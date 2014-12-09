package io.pivotal.tooling.plugin.eclipse;

import io.pivotal.tooling.model.eclipse.StsEclipseProject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.result.ArtifactResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.component.DefaultModuleComponentIdentifier;
import org.gradle.api.internal.artifacts.component.DefaultModuleComponentSelector;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.ProjectPublication;
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.ProjectPublicationRegistry;
import org.gradle.api.internal.artifacts.result.DefaultResolvedArtifactResult;
import org.gradle.api.specs.Specs;
import org.gradle.language.base.artifact.SourcesArtifact;
import org.gradle.language.java.artifact.JavadocArtifact;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.internal.resolver.model.IdeLocalFileDependency;
import org.gradle.plugins.ide.internal.tooling.EclipseModelBuilder;
import org.gradle.plugins.ide.internal.tooling.GradleProjectBuilder;
import org.gradle.plugins.ide.internal.tooling.eclipse.DefaultEclipseProject;
import org.gradle.plugins.ide.internal.tooling.eclipse.DefaultEclipseProjectDependency;
import org.gradle.runtime.jvm.JvmLibrary;
import org.gradle.tooling.internal.gradle.DefaultGradleModuleVersion;
import org.gradle.tooling.internal.gradle.DefaultGradleProject;
import org.gradle.tooling.model.GradleModuleVersion;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.gradle.tooling.provider.model.ToolingModelBuilder;

class StsEclipseProjectModelBuilder implements ToolingModelBuilder {
    private static final String PROJECT_EXTERNAL_CONF = "projectExternal";

    private DefaultStsEclipseProject result, root;
    private Project currentProject;

    private GradleProjectBuilder gradleProjectBuilder = new GradleProjectBuilder();
    private DefaultGradleProject<?> rootGradleProject;
    private ProjectPublicationRegistry publicationRegistry;

    private EclipseModelBuilder eclipseModelBuilder = new EclipseModelBuilder(gradleProjectBuilder);

    private Map<String, GradleModuleVersion> moduleVersionByProjectPath = new HashMap<String, GradleModuleVersion>();
    private Map<String, DefaultStsEclipseExternalDependency> externalEquivalentByProjectPath = new HashMap<String, DefaultStsEclipseExternalDependency>();

	private Map<String, DefaultStsEclipseProject> projectByPath = new HashMap<String, DefaultStsEclipseProject>();

    public StsEclipseProjectModelBuilder(ProjectPublicationRegistry publicationRegistry) {
        this.publicationRegistry = publicationRegistry;
    }

    @Override
    public boolean canBuild(String modelName) {
        return modelName.equals(StsEclipseProject.class.getName());
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        currentProject = project;
        rootGradleProject = gradleProjectBuilder.buildAll(project);
        buildHierarchy(project.getRootProject());
        buildProjectDependencies(root);
        return result;
    }

    /**
     * @param project
     * @return - A list of all binary dependencies, including transitives of both
     * binary dependencies and project dependencies
     */
    @SuppressWarnings("unchecked")
	private List<DefaultStsEclipseExternalDependency> buildExternalDependencies(Project project) {
        Map<String, DefaultStsEclipseExternalDependency> externalDependenciesById = new HashMap<String, DefaultStsEclipseExternalDependency>();

        EclipseModel eclipseModel = project.getExtensions().getByType(EclipseModel.class);

        for (Configuration conf : eclipseModel.getClasspath().getPlusConfigurations()) {
            List<ComponentIdentifier> binaryDependencies = new ArrayList<ComponentIdentifier>();
            for (DependencyResult dep : conf.getIncoming().getResolutionResult().getAllDependencies()) {
                if (dep instanceof ResolvedDependencyResult && dep.getRequested() instanceof DefaultModuleComponentSelector)
                    binaryDependencies.add(((ResolvedDependencyResult) dep).getSelected().getId());
            }

            List<String> binaryDependenciesAsStrings = new ArrayList<String>();
            for (ComponentIdentifier binaryDependency : binaryDependencies)
                binaryDependenciesAsStrings.add(binaryDependency.toString());

            Set<ComponentArtifactsResult> artifactsResults = project.getDependencies().createArtifactResolutionQuery()
                    .forComponents(binaryDependencies)
                    .withArtifacts(JvmLibrary.class, SourcesArtifact.class, JavadocArtifact.class)
                    .execute()
                    .getResolvedComponents();

            for (ResolvedArtifact artifact : conf.getResolvedConfiguration().getLenientConfiguration().getArtifacts(Specs.SATISFIES_ALL)) {
                ModuleVersionIdentifier id = artifact.getModuleVersion().getId();
                if (binaryDependenciesAsStrings.contains(id.toString())) {
                    externalDependenciesById.put(id.toString(), new DefaultStsEclipseExternalDependency()
                            .setFile(artifact.getFile())
                            .setModuleVersion(id));
                }
            }

            for (ComponentArtifactsResult artifactResult : artifactsResults) {
                DefaultStsEclipseExternalDependency externalDependency = externalDependenciesById.get(artifactResult.getId().toString());
                for (ArtifactResult sourcesResult : artifactResult.getArtifacts(SourcesArtifact.class)) {
                    if (sourcesResult instanceof DefaultResolvedArtifactResult)
                        externalDependency.setSource(((DefaultResolvedArtifactResult) sourcesResult).getFile());
                }
                for (ArtifactResult javadocResult : artifactResult.getArtifacts(JavadocArtifact.class)) {
                    if (javadocResult instanceof DefaultResolvedArtifactResult)
                        externalDependency.setJavadoc(((DefaultResolvedArtifactResult) javadocResult).getFile());
                }
            }
        }

        // this grabs "local file dependencies" (e.g. gradleApi(), localGroovy())
        for(Configuration conf : eclipseModel.getClasspath().getPlusConfigurations()) {
            for (Dependency dependency : conf.getAllDependencies()) {
                if (dependency instanceof SelfResolvingDependency && !(dependency instanceof ProjectDependency)) {
                    Set<File> resolvedFiles = ((SelfResolvingDependency) dependency).resolve();

                    for (File resolvedFile : resolvedFiles) {
                        // these type of dependencies do not have a module version identifier, the file path is unique enough
                        externalDependenciesById.put(resolvedFile.getAbsolutePath(), new DefaultStsEclipseExternalDependency()
                                .setFile(resolvedFile));
                    }
                }
            }
        }

        for(Configuration conf: eclipseModel.getClasspath().getMinusConfigurations()) {
            for (ResolvedArtifact artifact : conf.getResolvedConfiguration().getLenientConfiguration().getArtifacts(Specs.SATISFIES_ALL)) {
                externalDependenciesById.remove(artifact.getModuleVersion().getId().toString());
            }
        }

        // must create new list because Map.values() is not Serializable
        return new ArrayList<DefaultStsEclipseExternalDependency>(externalDependenciesById.values());
    }

    private void resolveExternalDependencyEquivalent(Project project) {
        String group = project.getGroup().toString(), name = project.getName();

        EclipseToolingModelPluginExtension ext = (EclipseToolingModelPluginExtension) project.getExtensions().getByName("eclipseToolingModel");

        Configuration projectExternal = project.getConfigurations().create(PROJECT_EXTERNAL_CONF);
        projectExternal.getDependencies().add(new DefaultExternalModuleDependency(group,
                name, ext.getEquivalentBinaryVersion()).setTransitive(false));

        DefaultStsEclipseExternalDependency externalDependency = new DefaultStsEclipseExternalDependency();

        for (ResolvedArtifact resolvedArtifact : projectExternal.getResolvedConfiguration().getLenientConfiguration().getArtifacts(Specs.SATISFIES_ALL)) {
            externalDependency.setModuleVersion(new DefaultModuleVersionIdentifier(group, name,
                    resolvedArtifact.getModuleVersion().getId().getVersion()));
            externalDependency.setFile(resolvedArtifact.getFile());
        }

        if(externalDependency.getFile() == null)
            return; // unable to find a binary equivalent for this project

        @SuppressWarnings("unchecked")
		Set<ComponentArtifactsResult> artifactsResults = project.getDependencies().createArtifactResolutionQuery()
                .forComponents(new DefaultModuleComponentIdentifier(group, name, externalDependency.getGradleModuleVersion().getVersion()))
                .withArtifacts(JvmLibrary.class, SourcesArtifact.class, JavadocArtifact.class)
                .execute()
                .getResolvedComponents();

        for (ComponentArtifactsResult artifactResult : artifactsResults) {
            for (ArtifactResult sourcesResult : artifactResult.getArtifacts(SourcesArtifact.class)) {
                if(sourcesResult instanceof DefaultResolvedArtifactResult)
                    externalDependency.setSource(((DefaultResolvedArtifactResult) sourcesResult).getFile());
            }
            for (ArtifactResult javadocResult : artifactResult.getArtifacts(JavadocArtifact.class)) {
                if(javadocResult instanceof DefaultResolvedArtifactResult)
                    externalDependency.setJavadoc(((DefaultResolvedArtifactResult) javadocResult).getFile());
            }
        }
        System.out.println("externalEq: "+project.getPath() + " => " + externalDependency.getFile());
        externalEquivalentByProjectPath.put(project.getPath(), externalDependency);
    }

    private DefaultStsEclipseProject buildHierarchy(Project project) {
        DefaultStsEclipseProject eclipseProject = getProject(project.getPath());

        if (project == project.getRootProject())
            root = eclipseProject;

        List<DefaultStsEclipseProject> children = new ArrayList<DefaultStsEclipseProject>();
        for (Project child : project.getChildProjects().values())
            children.add(buildHierarchy(child));

        moduleVersionByProjectPath.put(project.getPath(), new DefaultGradleModuleVersion(new DefaultModuleVersionIdentifier(project.getGroup().toString(),
                project.getName(), project.getVersion().toString())));
        resolveExternalDependencyEquivalent(project);

        DefaultEclipseProject defaultEclipseProject = eclipseModelBuilder.buildAll(HierarchicalEclipseProject.class.getName(), project);

        List<DefaultGradleModuleVersion> publications = new ArrayList<DefaultGradleModuleVersion>();
        
        if(publicationRegistry != null) {
	        for (ProjectPublication publication : publicationRegistry.getPublications(project.getPath()))
	            publications.add(new DefaultGradleModuleVersion(publication.getId()));
        }

        eclipseProject
                .setHierarchicalEclipseProject(defaultEclipseProject)
                .setGradleProject(rootGradleProject.findByPath(project.getPath()))
                .setChildren(children)
                .setClasspath(buildExternalDependencies(project))
                .setPlugins(plugins(project))
                .setRoot(root)
                .setPublications(publications);

        for (DefaultStsEclipseProject child : children)
            child.setParent(eclipseProject);

        if (project == currentProject)
            result = eclipseProject;

        return eclipseProject;
    }

    private DefaultStsEclipseProject getProject(String path) {
	    	DefaultStsEclipseProject existing = projectByPath.get(path);
	    	if (existing==null) {
	    		projectByPath.put(path, existing=new DefaultStsEclipseProject());
	    	}
	    	return existing;
	}
    
    private Set<DefaultStsEclipseProjectDependency> buildProjectDependencies(DefaultStsEclipseProject eclipseProject) {
    		Set<DefaultStsEclipseProjectDependency> pDeps = eclipseProject.getProjectDependencies();

	    	if (pDeps == null) {
	    		eclipseProject.setProjectDependencies(pDeps = new HashSet<DefaultStsEclipseProjectDependency>());
	    		for (DefaultEclipseProjectDependency projectDependency : eclipseProject.getHierarchicalEclipseProject().getProjectDependencies()) {
	    			// in case of a cycle, we never want to add ourselves as a project dependency
	    			if(!eclipseProject.getHierarchicalEclipseProject().getPath().equals(":" + projectDependency.getPath())) {
	    				pDeps.add(newProjectDep(projectDependency));	
	    			}
	    		}
	    		//Add transitives as well
	    		for (DefaultEclipseProjectDependency projectDependency : eclipseProject.getHierarchicalEclipseProject().getProjectDependencies()) {
	    			if(!eclipseProject.getName().equals(projectDependency.getTargetProject().getName())) {
	    				for (DefaultStsEclipseProjectDependency transitive : buildProjectDependencies(getProject(projectDependency.getTargetProject().getPath()))) {
	    					if(!eclipseProject.getHierarchicalEclipseProject().getPath().equals(":" + transitive.getPath())) {
			    				pDeps.add(transitive);	
			    			}
					}
	    			}
	    		}
	    	}
	
	    	//Ensure that all projects in the hierarchy get built:
	    	for (DefaultStsEclipseProject child : eclipseProject.getChildren())
	    		buildProjectDependencies(child);
	    	
	    	return pDeps;
    }
    
    private Map<String, DefaultStsEclipseProjectDependency> projectDependendencyCache = new HashMap<String, DefaultStsEclipseProjectDependency>();
    
	private DefaultStsEclipseProjectDependency newProjectDep(DefaultEclipseProjectDependency projectDependency) {
        String targetPath = projectDependency.getTargetProject().getPath();
		DefaultStsEclipseProjectDependency dep = projectDependendencyCache.get(targetPath);
		if (dep==null) {
			projectDependendencyCache.put(targetPath, dep = new DefaultStsEclipseProjectDependency(
			    projectDependency,
			    moduleVersionByProjectPath.get(targetPath),
			    externalEquivalentByProjectPath.get(targetPath)
			));
		}
		return dep;
	}

    private static List<String> plugins(Project project) {
        List<String> plugins = new ArrayList<String>();
        for(Plugin<?> plugin : project.getPlugins())
            plugins.add(plugin.getClass().getName());
        return plugins;
    }
}
