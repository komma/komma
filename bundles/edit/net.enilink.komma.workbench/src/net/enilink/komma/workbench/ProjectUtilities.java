/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 *  $$RCSfile: ProjectUtilities.java,v $$
 *  $$Revision: 1.4 $$  $$Date: 2005/05/11 19:01:24 $$ 
 */

package net.enilink.komma.workbench;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IObject;
import net.enilink.komma.workbench.internal.KommaWorkbenchPlugin;

/**
 * KOMMA Workbench Project Utilities.
 * 
 * @since 1.0.0
 */

public class ProjectUtilities {
	private static final Logger log = LoggerFactory
			.getLogger(ProjectUtilities.class);

	private ProjectUtilities() {
	}

	/**
	 * Add the nature id to the project ahead of all other nature ids.
	 * 
	 * @param proj
	 * @param natureId
	 * @throws CoreException
	 * 
	 * @since 1.0.0
	 */
	public static void addNatureToProject(IProject proj, String natureId)
			throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures = description.getNatureIds();
		String[] newNatures = new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 1, prevNatures.length);
		newNatures[0] = natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, null);
	}

	/**
	 * Add the nature id after all of the other nature ids for the project.
	 * 
	 * @param proj
	 * @param natureId
	 * @throws CoreException
	 * 
	 * @since 1.0.0
	 */
	public static void addNatureToProjectLast(IProject proj, String natureId)
			throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures = description.getNatureIds();
		String[] newNatures = new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length] = natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, null);
	}

	/**
	 * Remove the nature id from the project.
	 * 
	 * @param project
	 * @param natureId
	 * @throws CoreException
	 * 
	 * @since 1.0.0
	 */
	public static void removeNatureFromProject(IProject project, String natureId)
			throws CoreException {
		IProjectDescription description = project.getDescription();
		String[] prevNatures = description.getNatureIds();
		int size = prevNatures.length;
		int newsize = 0;
		String[] newNatures = new String[size];
		boolean matchfound = false;
		for (int i = 0; i < size; i++) {
			if (prevNatures[i].equals(natureId)) {
				matchfound = true;
				continue;
			} else
				newNatures[newsize++] = prevNatures[i];
		}
		if (!matchfound)
			throw new CoreException(
					new Status(
							IStatus.ERROR,
							KommaWorkbenchPlugin.PLUGIN_ID,
							0,
							"The nature id " + natureId + " does not exist on the project " + project.getName(), null)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else {
			String[] temp = newNatures;
			newNatures = new String[newsize];
			System.arraycopy(temp, 0, newNatures, 0, newsize);
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		}
	}

	/**
	 * Add the list of projects to end of the "referenced projects" list from
	 * the project's description.
	 * 
	 * @param project
	 * @param toBeAddedProjectsList
	 * @throws CoreException
	 * 
	 * @since 1.0.0
	 */
	public static void addReferenceProjects(IProject project,
			List<IProject> toBeAddedProjectsList) throws CoreException {
		IProjectDescription description = project.getDescription();
		IProject[] projects = description.getReferencedProjects();

		List<IProject> projectsList = new ArrayList<IProject>(
				Arrays.asList(projects));
		projectsList.addAll(toBeAddedProjectsList);

		description.setReferencedProjects(projectsList
				.toArray(new IProject[projectsList.size()]));
		project.setDescription(description, null);
	}

	/**
	 * Add the single project to the end of the "referenced projects" list from
	 * the project's description.
	 * 
	 * @param project
	 * @param projectToBeAdded
	 * @throws CoreException
	 * 
	 * @since 1.0.0
	 */
	public static void addReferenceProjects(IProject project,
			IProject projectToBeAdded) throws CoreException {
		IProjectDescription description = project.getDescription();
		IProject[] projects = description.getReferencedProjects();

		List<IProject> projectsList = new ArrayList<IProject>(
				Arrays.asList(projects));
		projectsList.add(projectToBeAdded);

		description.setReferencedProjects(projectsList
				.toArray(new IProject[projectsList.size()]));
		project.setDescription(description, null);
	}

	/**
	 * Force a an immediate build of the project.
	 * 
	 * @param project
	 * @param progressMonitor
	 * 
	 * @since 1.0.0
	 */
	public static void forceAutoBuild(IProject project,
			IProgressMonitor progressMonitor) {
		try {
			project.build(IncrementalProjectBuilder.FULL_BUILD, progressMonitor);
		} catch (CoreException ce) {
			log.error("Building project failed", ce);
		}
	}

	/**
	 * Return if auto build is turned on.
	 * 
	 * @return <code>true</code> if auto build is turned on.
	 * 
	 * @since 1.0.0
	 */
	public static boolean getCurrentAutoBuildSetting() {

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription wd = workspace.getDescription();
		return wd.isAutoBuilding();
	}

	/**
	 * Get the project associated with the given object.
	 * 
	 * @param object
	 *            may be an
	 * 
	 *            <code>IProject, IResource, IAdaptable (to an IProject), EObject (gets IProject if object is in a ProjectResourceSet</code>
	 *            .
	 * @param natureId
	 *            if <code>null</code> then returns project. If not
	 *            <code>null</code> then returns project only if project has
	 *            this nature id.
	 * @return project associated with the object or <code>null</code> if not
	 *         found.
	 * 
	 * @since 1.0.0
	 */
	public static IProject getProject(Object object, String natureId) {
		IProject result = getProject(object);
		if (natureId == null)
			return result;
		if (result != null && result.isAccessible() && natureId != null)
			try {
				if (result.hasNature(natureId)) {
					return result;
				}
			} catch (CoreException e) {
				log.error("Determining project nature failed", e);
			}
		return null;
	}

	/**
	 * Get the project associated with the given object.
	 * 
	 * @param object
	 *            may be an
	 * 
	 *            <code>IProject, IResource, IAdaptable (to an IProject), EObject (gets IProject if object is in a ProjectResourceSet</code>
	 *            .
	 * @return project associated with the object or <code>null</code> if not
	 *         found.
	 * 
	 * @since 1.0.0
	 */
	public static IProject getProject(Object object) {
		IProject result = null;

		if (object instanceof IProject)
			result = (IProject) object;
		else if (object instanceof IResource)
			result = ((IResource) object).getProject();
		else if (object instanceof IAdaptable)
			result = (IProject) ((IAdaptable) object)
					.getAdapter(IProject.class);
		else if (object instanceof IObject)
			result = getProject((IObject) object);

		return result;
	}

	/**
	 * Get the project associated with the given EObject. (If in a
	 * ProjectResourceSet, then the project from that resource set).
	 * 
	 * @param aRefObject
	 * @return project if associated or <code>null</code> if not found.
	 * 
	 * @since 1.0.0
	 */
	public static IProject getProject(IObject aRefObject) {
		if (aRefObject != null) {
			IModel model = aRefObject.getModel();
			return getProject(model);
		}
		return null;
	}

	/**
	 * Get the project associated with the given Resource. (If in a
	 * ProjectResourceSet, then the project from that resource set).
	 * 
	 * @param resource
	 * @return project if associated or <code>null</code> if not found.
	 * 
	 * @since 1.0.0
	 */
	public static IProject getProject(IModel model) {
		IModelSet set = model == null ? null : model.getModelSet();
		if (set instanceof IProjectModelSet) {
			return ((IProjectModelSet) set).getProject();
		}
		return null;
	}

	/**
	 * Remove the list of projects from the list of "referenced projects" in the
	 * project's description.
	 * 
	 * @param project
	 * @param toBeRemovedProjectList
	 * @throws org.eclipse.core.runtime.CoreException
	 * 
	 * @since 1.0.0
	 */
	public static void removeReferenceProjects(IProject project,
			List<IProject> toBeRemovedProjectList)
			throws org.eclipse.core.runtime.CoreException {
		IProjectDescription description = project.getDescription();
		IProject[] projects = description.getReferencedProjects();

		List<IProject> projectsList = new ArrayList<IProject>(
				Arrays.asList(projects));
		projectsList.removeAll(toBeRemovedProjectList);

		description.setReferencedProjects(projectsList
				.toArray(new IProject[projectsList.size()]));
		project.setDescription(description, null);
	}

	/**
	 * Remove the project from the list of "referenced projects" in the
	 * description for the given project.
	 * 
	 * @param project
	 * @param toBeRemovedProject
	 * @throws org.eclipse.core.runtime.CoreException
	 * 
	 * @since 1.0.0
	 */
	public static void removeReferenceProjects(IProject project,
			IProject toBeRemovedProject)
			throws org.eclipse.core.runtime.CoreException {
		IProjectDescription description = project.getDescription();
		IProject[] projects = description.getReferencedProjects();

		List<IProject> projectsList = new ArrayList<IProject>(
				Arrays.asList(projects));
		projectsList.remove(toBeRemovedProject);

		description.setReferencedProjects(projectsList
				.toArray(new IProject[projectsList.size()]));
		project.setDescription(description, null);
	}

	/**
	 * Set the auto-building state.
	 * 
	 * @param aBoolean
	 *            <code>true</code> to turn auto-building on.
	 * 
	 * @since 1.0.0
	 */
	public static void turnAutoBuildOn(boolean aBoolean) {
		try {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceDescription wd = workspace.getDescription();
			wd.setAutoBuilding(aBoolean);
			workspace.setDescription(wd);
		} catch (CoreException ce) {
			log.error("Failed to change auto build state", ce);

		}
	}

	/**
	 * Adds a builder to the build spec for the given project.
	 * 
	 * @param builderID
	 *            The id of the builder.
	 * @param project
	 *            Project to add to.
	 * @return whether the builder id was actually added (it may have already
	 *         existed)
	 * @throws CoreException
	 * @since 1.0.0
	 */
	public static boolean addToBuildSpec(String builderID, IProject project)
			throws CoreException {
		return addToBuildSpecBefore(builderID, null, project);
	}

	/**
	 * Adds a builder to the build spec for the given project, immediately
	 * before the specified successor builder.
	 * 
	 * @param builderID
	 *            The id of the builder.
	 * @param successorID
	 *            The id to put the builder before.
	 * @return whether the builder id was actually added (it may have already
	 *         existed)
	 * @throws CoreException
	 * @since 1.0.0
	 */
	public static boolean addToBuildSpecBefore(String builderID,
			String successorID, IProject project) throws CoreException {
		IProjectDescription description = project.getDescription();
		ICommand[] commands = description.getBuildSpec();
		boolean found = false;
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(builderID)) {
				found = true;
				break;
			}
		}
		if (!found) {
			boolean successorFound = false;
			ICommand command = description.newCommand();
			command.setBuilderName(builderID);
			ICommand[] newCommands = new ICommand[commands.length + 1];
			for (int j = 0, index = 0; j < commands.length; j++, index++) {
				if (successorID != null
						&& commands[j].getBuilderName().equals(successorID)) {
					successorFound = true;
					newCommands[index++] = command;
				}
				newCommands[index] = commands[j];
			}
			if (!successorFound)
				newCommands[newCommands.length - 1] = command;
			description.setBuildSpec(newCommands);
			project.setDescription(description, null);
		}
		return !found;
	}

	/**
	 * Remove the builder from the build spec.
	 * 
	 * @param builderID
	 *            The id of the builder.
	 * @param project
	 *            Project to remove from.
	 * @return boolean if the builder id was found and removed
	 * @throws CoreException
	 * @since 1.0.0
	 */
	public static boolean removeFromBuildSpec(String builderID, IProject project)
			throws CoreException {
		IProjectDescription description = project.getDescription();
		ICommand[] commands = description.getBuildSpec();
		boolean found = false;
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(builderID)) {
				found = true;
				break;
			}
		}
		if (found) {
			ICommand[] newCommands = new ICommand[commands.length - 1];
			int newCount = 0;
			for (int i = 0; i < commands.length; ++i) {
				if (!(commands[i].getBuilderName().equals(builderID))) {
					// Add the existng to the new array
					newCommands[newCount] = commands[i];
					newCount++;
				}
			}
			description.setBuildSpec(newCommands);
			project.setDescription(description, null);
		}
		return found;

	}

	/**
	 * Ensure the container is not read-only.
	 * <p>
	 * For Linux, a Resource cannot be created in a ReadOnly folder. This is
	 * only necessary for new files.
	 * 
	 * @param resource
	 *            workspace resource to make read/write
	 * @since 1.0.0
	 */
	public static void ensureContainerNotReadOnly(IResource resource) {
		if (resource != null && !resource.exists()) { // it must be new
			IContainer container = resource.getParent();
			ResourceAttributes attr = container.getResourceAttributes();
			while (attr != null && !attr.isReadOnly()) {
				container = container.getParent();
				if (container == null)
					break;
				attr = container.getResourceAttributes();
			}
			if (container != null && attr != null)
				attr.setReadOnly(false);
		}
	}

	/**
	 * Get projects from primary nature.
	 * 
	 * @param natureID
	 * @return All projects that have the given nature id as the first nature
	 *         id.
	 * 
	 * @since 1.0.0
	 */
	public static IProject[] getProjectsForPrimaryNature(String natureID) {
		IProject[] projectsWithNature = new IProject[] {};
		List<IProject> result = new ArrayList<IProject>();
		IProject[] projects = getAllProjects();
		for (int i = 0; i < projects.length; i++) {
			if (isProjectPrimaryNature(projects[i], natureID))
				result.add(projects[i]);
		}
		return result.toArray(projectsWithNature);
	}

	/**
	 * Get all projects in the workspace
	 * 
	 * @return all workspace projects
	 * 
	 * @since 1.0.0
	 */
	public static IProject[] getAllProjects() {
		return ResourcesPlugin.getWorkspace().getRoot().getProjects();
	}

	/**
	 * Is this nature id the primary nature id for the project
	 * 
	 * @param project
	 * @param natureID
	 * @return <code>true</code> if first nature id for the project.
	 * 
	 * @since 1.0.0
	 */
	public static boolean isProjectPrimaryNature(IProject project,
			String natureID) {
		String[] natures = null;
		try {
			natures = project.getDescription().getNatureIds();
		} catch (Exception e1) {
		}
		return (natures != null && natures.length > 0 && natures[0]
				.equals(natureID));
	}

	protected static IPath createPath(IProject p, String defaultSourceName) {
		IPath path = new Path(p.getName());
		path = path.append(defaultSourceName);
		path = path.makeAbsolute();
		return path;
	}

	/**
	 * Strip off a leading "/" from each project name in the array, if it has
	 * one.
	 * 
	 * @param projecNames
	 * @return array of project names with all leading '/' removed.
	 * 
	 * @since 1.0.0
	 */
	public static String[] getProjectNamesWithoutForwardSlash(
			String[] projecNames) {
		String[] projNames = new String[projecNames.length];
		List<String> temp = java.util.Arrays.asList(projecNames);
		for (int i = 0; i < temp.size(); i++) {
			String name = (temp.get(i));
			if (name.startsWith("/")) { //$NON-NLS-1$
				projNames[i] = name.substring(1, name.length());
			} else {
				projNames[i] = name;
			}
		}
		return projNames;
	}

	/**
	 * List of all files in the project.
	 * <p>
	 * Note: A more efficient way to do this is to use
	 * {@link IResource#accept(org.eclipse.core.resources.IResourceProxyVisitor, int)}
	 * 
	 * @param 1.0.0
	 * @return list of files in the project
	 * 
	 * @see IResource#accept(org.eclipse.core.resources.IResourceProxyVisitor,
	 *      int)
	 * @since 1.0.0
	 */
	public static List<IResource> getAllProjectFiles(IProject project) {
		List<IResource> result = new ArrayList<IResource>();
		if (project == null)
			return result;
		try {
			result = collectFiles(project.members(), result);
		} catch (CoreException e) {
		}
		return result;
	}

	private static List<IResource> collectFiles(IResource[] members,
			List<IResource> result) throws CoreException {
		// recursively collect files for the given members
		for (int i = 0; i < members.length; i++) {
			IResource res = members[i];
			if (res instanceof IFolder) {
				collectFiles(((IFolder) res).members(), result);
			} else if (res instanceof IFile) {
				result.add(res);
			}
		}
		return result;
	}

	/**
	 * Get the project.
	 * 
	 * @param projectName
	 * @return a IProject given the projectName
	 * @since 1.0.0
	 */
	public static IProject getProject(String projectName) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
	}

	/**
	 * Return whether the given builder name is attached to the project.
	 * 
	 * @param project
	 * @param builderName
	 * @return <code>true</code> if builder name is attached to the project.
	 * 
	 * @since 1.0.0
	 */
	public static boolean hasBuilder(IProject project, String builderName) {
		try {
			ICommand[] builders = project.getDescription().getBuildSpec();
			for (int i = 0; i < builders.length; i++) {
				ICommand builder = builders[i];
				if (builder != null) {
					if (builder.getBuilderName().equals(builderName))
						return true;
				}
			}
		} catch (Exception e) {
		}
		return false;
	}
}