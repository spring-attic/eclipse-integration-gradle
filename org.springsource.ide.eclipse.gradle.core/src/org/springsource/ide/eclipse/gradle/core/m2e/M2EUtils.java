package org.springsource.ide.eclipse.gradle.core.m2e;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectManager;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.GradleModuleVersion;
import org.gradle.tooling.model.UnsupportedMethodException;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.ProjectOpenCloseListener;

@SuppressWarnings("restriction")
public class M2EUtils {

	public interface IM2EUtils {
		IProject getMavenProject(ExternalDependency dep);
		boolean isInstalled();
		void addMavenOpenCloseListener(ProjectOpenCloseListener openCloseListener);
	}

	public static class DefaultImplementation implements IM2EUtils {
		
		public IProject getMavenProject(String groupId, String artifactId, String version) {
			MavenProjectManager mvnProjects = MavenPluginActivator.getDefault().getMavenProjectManager();
			IMavenProjectFacade mvnProject = mvnProjects.getMavenProject(groupId, artifactId, version);
			if (mvnProject!=null) {
				return mvnProject.getProject();
			}
			return null;
		}

		public IProject getMavenProject(ExternalDependency dep) {
			try { 
				GradleModuleVersion mv = dep.getGradleModuleVersion();
				if (mv!=null) {
					return getMavenProject(mv.getGroup(), mv.getName(), mv.getVersion());
				} else {
					System.out.println(dep.getFile());
				}
			} catch (UnsupportedMethodException e) {
				//Expected, if project is using older Gradle version pre 1.1.
				//Ingore and move on.
			} catch (Throwable e) {
				//Maybe a version of Gradle that doesn't provide this information
				GradleCore.log(e);
			}
			return null;
		}

		public boolean isInstalled() {
			//The fact that we are using this implementation implies that M2E is installed.
			return true;
		}

		@Override
		public void addMavenOpenCloseListener(final ProjectOpenCloseListener openCloseListener) {
			MavenProjectManager mvnProjects = MavenPluginActivator.getDefault().getMavenProjectManager();
			IMavenProjectChangedListener adapter = new IMavenProjectChangedListener() {
				public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
					for (MavenProjectChangedEvent e : events) {
						switch (e.getKind()) {
						case MavenProjectChangedEvent.KIND_ADDED:
							openCloseListener.projectOpened(e.getMavenProject().getProject());
							break;
						case MavenProjectChangedEvent.KIND_REMOVED:
							openCloseListener.projectClosed(e.getOldMavenProject().getProject());
							break;
						default:
							break;
						}
					}
				}
			};
			mvnProjects.addMavenProjectChangedListener(adapter);
		}
	}

	/**
	 * NullImplementation is a 'dummy' implementation which does nothing.
	 * This is used if m2e is not installed. We use m2e to find maven projects in the workspace.
	 */
	public static class NullImplementation implements IM2EUtils {
		public IProject getMavenProject(ExternalDependency dep) {
			return null;
		}

		public boolean isInstalled() {
			return false;
		}

		public void addMavenOpenCloseListener(ProjectOpenCloseListener openCloseListener) {
		}
	}


	private static IM2EUtils impl;

	public static IProject getMavenProject(ExternalDependency dep) {
		return implementation().getMavenProject(dep);
	}

	private static synchronized IM2EUtils implementation() {
		if (impl==null) {
			impl = createImplementation();
		}
		return impl;
	}

	private static IM2EUtils createImplementation() {
		try {
			Class.forName("org.eclipse.m2e.core.internal.MavenPluginActivator");
			Class.forName("org.eclipse.m2e.core.internal.project.registry.MavenProjectManager");
			Class.forName("org.eclipse.m2e.core.project.IMavenProjectFacade");
			Class.forName("org.apache.maven.artifact.repository.MavenArtifactRepository");
			return new DefaultImplementation();
		} catch (Throwable e) {
			return new NullImplementation();
		}
	}

	public static boolean isInstalled() {
		return implementation().isInstalled();
	}

	public static void addMavenProjectListener(ProjectOpenCloseListener openCloseListener) {
		implementation().addMavenOpenCloseListener(openCloseListener);
	}
	
}
