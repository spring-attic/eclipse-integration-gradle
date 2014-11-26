package org.springsource.ide.eclipse.gradle.core.m2e;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
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
		void addOpenCloseListener(ProjectOpenCloseListener openCloseListener);
		void removeOpenCloseListener(ProjectOpenCloseListener openCloseListener);
		int countOpenCloseListeners();
	}

	public static class DefaultImplementation implements IM2EUtils {
		
		private ListenerList openCloseListeners;
		private IMavenProjectChangedListener mvnProjectListener;
		
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

		private synchronized void ensureMavenProjectChangedListener() {
			if (mvnProjectListener==null) {
				MavenProjectManager mvnProjects = MavenPluginActivator.getDefault().getMavenProjectManager();
				mvnProjectListener = new IMavenProjectChangedListener() {
					public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
						for (MavenProjectChangedEvent e : events) {
							switch (e.getKind()) {
							case MavenProjectChangedEvent.KIND_ADDED:
								fireOpenEvent(e.getMavenProject().getProject());
								break;
							case MavenProjectChangedEvent.KIND_REMOVED:
								fireCloseEvent(e.getOldMavenProject().getProject());
								break;
							default:
								break;
							}
						}
					}
				};
				mvnProjects.addMavenProjectChangedListener(mvnProjectListener);
			}
		}
		
		@Override
		public synchronized void addOpenCloseListener(final ProjectOpenCloseListener l) {
			ensureMavenProjectChangedListener();
			if (openCloseListeners==null) {
				openCloseListeners = new ListenerList();
			}
			openCloseListeners.add(l);
		}
		
		@Override
		public void removeOpenCloseListener(ProjectOpenCloseListener l) {
			if (openCloseListeners!=null) {
				openCloseListeners.remove(l);
			}
		}
		
		private void fireOpenEvent(IProject project) {
			ListenerList listeners = openCloseListeners;
			if (listeners!=null) {
				for (Object l : listeners.getListeners()) {
					((ProjectOpenCloseListener)l).projectOpened(project);
				}
			}
		}
		
		private void fireCloseEvent(IProject project) {
			ListenerList listeners = openCloseListeners;
			if (listeners!=null) {
				for (Object l : listeners.getListeners()) {
					((ProjectOpenCloseListener)l).projectClosed(project);
				}
			}
		}

		@Override
		public int countOpenCloseListeners() {
			ListenerList listeners = openCloseListeners;
			if (listeners!=null) {
				return listeners.getListeners().length;
			}
			return 0;
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

		public void addOpenCloseListener(ProjectOpenCloseListener openCloseListener) {
		}

		public void removeOpenCloseListener(ProjectOpenCloseListener openCloseListener) {
		}

		public int countOpenCloseListeners() {
			return 0;
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

	public static void addOpenCloseListener(ProjectOpenCloseListener openCloseListener) {
		implementation().addOpenCloseListener(openCloseListener);
	}

	public static void removeOpenCloseListener(ProjectOpenCloseListener openCloseListener) {
		implementation().removeOpenCloseListener(openCloseListener);
	}

	public static int countOpenCloseListeners() {
		return implementation().countOpenCloseListeners();
	}
	
}
