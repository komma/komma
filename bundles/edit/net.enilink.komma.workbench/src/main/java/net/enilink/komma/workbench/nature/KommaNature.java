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
 *  $$RCSfile: EMFNature.java,v $$
 *  $$Revision: 1.4 $$  $$Date: 2005/02/15 23:04:14 $$ 
 */
package net.enilink.komma.workbench.nature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import net.enilink.komma.workbench.IKommaContextContributor;
import net.enilink.komma.workbench.IWorkbenchURIConverter;
import net.enilink.komma.workbench.KommaWorkbenchContextBase;
import net.enilink.komma.workbench.ProjectUtilities;
import net.enilink.komma.workbench.WorkbenchModelHelperBase;
import net.enilink.komma.workbench.internal.nature.KommaNatureRegistry;

/**
 * The base KommaNature.
 * <p>
 * This is expected to be subclassed by clients, but there are default
 * subclasses available.
 * </p>
 * 
 * @since 1.0.0
 */
public abstract class KommaNature implements IProjectNature,
		IKommaContextContributor {

	/**
	 * Add the nature id to the project.
	 * 
	 * @param proj
	 * @param natureId
	 * @throws CoreException
	 * 
	 * @since 1.0.0
	 */
	protected static void addNatureToProject(IProject proj, String natureId)
			throws CoreException {
		ProjectUtilities.addNatureToProject(proj, natureId);
	}

	/**
	 * Return a list of nature ids based on the natures that have been
	 * configured for this project.
	 * 
	 * @return list of configured nature ids.
	 * @param project
	 */
	public static List<?> getRegisteredRuntimeIDs(IProject project) {
		List<String> result = null;
		String natureID = null;
		if (project != null && project.isAccessible()) {
			Iterator<?> it = KommaNatureRegistry.singleton().REGISTERED_NATURE_IDS
					.iterator();
			while (it.hasNext()) {
				natureID = (String) it.next();
				try {
					if (project.hasNature(natureID)) {
						if (result == null)
							result = new ArrayList<String>(2);
						result.add(natureID);
					}
				} catch (CoreException e) {
				}
			}
		}
		return result == null ? Collections.EMPTY_LIST : result;
	}

	/**
	 * Return a list of EMFNatures based on the natures that have been
	 * configured for this project.
	 * 
	 * @return List of EMFNatures
	 * @param project
	 * @return list of natures configured for the project.
	 * @since 1.0.0
	 */
	public static List<?> getRegisteredRuntimes(IProject project) {
		List<KommaNature> result = null;
		KommaNature nature = null;
		if (project != null && project.isAccessible()) {
			String natureID;
			Iterator<?> it = KommaNatureRegistry.singleton().REGISTERED_NATURE_IDS
					.iterator();
			while (it.hasNext()) {
				natureID = (String) it.next();
				try {
					nature = (KommaNature) project.getNature(natureID);
				} catch (CoreException e) {
				}
				if (nature != null) {
					if (result == null)
						result = new ArrayList<KommaNature>(2);
					result.add(nature);
				}
			}
		}
		return result == null ? Collections.EMPTY_LIST : result;
	}

	/**
	 * Return if the project has the given nature.
	 * 
	 * @param project
	 * @param natureId
	 * @return <code>true</code> if project has given nature
	 * 
	 * @since 1.0.0
	 */
	public static boolean hasRuntime(IProject project, String natureId) {
		if (project == null || !project.isAccessible())
			return false;
		try {
			return project.hasNature(natureId);
		} catch (CoreException e) {
			return false;
		}
	}

	/**
	 * Return if the project has any one of the possible given nature ids.
	 * 
	 * @param project
	 * @param possibleNatureIds
	 * @return <code>true</code> if at least one of the possible natures id is
	 *         configured for the project.
	 * 
	 * @since 1.0.0
	 */
	public static boolean hasRuntime(IProject project,
			String[] possibleNatureIds) {
		if (project != null) {
			for (int i = 0; i < possibleNatureIds.length; i++) {
				if (hasRuntime(project, possibleNatureIds[i]))
					return true;
			}
		}
		return false;
	}

	/**
	 * Register the given nature id as an EMFNature.
	 * 
	 * @param natureID
	 * 
	 * @since 1.0.0
	 */
	public static void registerNatureID(String natureID) {
		KommaNatureRegistry.singleton().REGISTERED_NATURE_IDS.add(natureID);
	}

	protected boolean hasConfigured = false;

	protected KommaWorkbenchContextBase kommaContext;

	protected IProject project;

	public KommaNature() {
		super();
	}

	/**
	 * Configures the project with this nature. This is called by
	 * <code>IProject.addNature</code> and should not be called directly by
	 * clients. The nature extension id is added to the list of natures on the
	 * project by <code>IProject.addNature</code>, and need not be added here.
	 * 
	 * <p>
	 * All subtypes must call super. The better way for subtypes is to override
	 * primConfigure instead.
	 * </p>
	 * 
	 * @throws CoreException
	 * @since 1.0.0
	 */
	public void configure() throws org.eclipse.core.runtime.CoreException {
		if (!hasConfigured) {
			hasConfigured = true;
			primConfigure();
		}
	}

	/**
	 * Create a folder relative to the project based on
	 * aProjectRelativePathString.
	 * 
	 * @param aProjectRelativePath
	 * @return
	 * @throws CoreException
	 * 
	 * @since 1.0.0
	 */
	public IFolder createFolder(IPath aProjectRelativePath)
			throws CoreException {
		if (aProjectRelativePath != null && !aProjectRelativePath.isEmpty()) {
			IFolder folder = getWorkspace().getRoot().getFolder(
					getProjectPath().append(aProjectRelativePath));
			if (!folder.exists()) {
				ProjectUtilities.ensureContainerNotReadOnly(folder);
				folder.create(true, true, null);
			}
			return folder;
		}
		return null;
	}

	/**
	 * Create a folder relative to the project based on
	 * aProjectRelativePathString.
	 * 
	 * @param aProjectRelativePathString
	 * @return
	 * @throws CoreException
	 * 
	 * @since 1.0.0
	 */
	public IFolder createFolder(String aProjectRelativePathString)
			throws CoreException {
		if (aProjectRelativePathString != null
				&& aProjectRelativePathString.length() > 0)
			return createFolder(new Path(aProjectRelativePathString));
		return null;
	}

	/**
	 * Create an EMF context for the project.
	 * 
	 * @throws CoreException
	 * 
	 * @since 1.0.0
	 */
	protected void createKommaContext() throws CoreException {
		WorkbenchModelHelperBase.createKommaContext(getProject(), this);
	}

	/**
	 * Removes this nature from the project, performing any required
	 * deconfiguration. This is called by <code>IProject.removeNature</code> and
	 * should not be called directly by clients. The nature id is removed from
	 * the list of natures on the project by <code>IProject.removeNature</code>,
	 * and need not be removed here.
	 * 
	 * @throws CoreException
	 * @since 1.0.0
	 */
	public void deconfigure() throws org.eclipse.core.runtime.CoreException {
		kommaContext = null;
	}

	/**
	 * Lazy initializer; for migration of existing workspaces where configure
	 * will never get called.
	 * 
	 * @return context base for the project.
	 * 
	 * @since 1.0.0
	 */
	protected KommaWorkbenchContextBase getKommaContextBase() {
		if (kommaContext == null) {
			try {
				createKommaContext();
			} catch (CoreException ex) {
				// Logger.getLogger().logError(ex);
			}
		}
		return kommaContext;
	}

	/**
	 * Returns the Komma root folder for the project. Defaults to the project.
	 * Subclasses can override.
	 * 
	 * @return Komma root folder for the project.
	 * 
	 * @since 1.0.0
	 */
	public IContainer getKommaRoot() {
		return getProject();
	}

	/**
	 * Return the nature's ID.
	 * 
	 * @return nature id
	 * 
	 * @since 1.0.0
	 */
	public abstract String getNatureID();

	/**
	 * Return the ID of the plugin that this nature is contained within.
	 * 
	 * @return
	 * 
	 * @since 1.0.0
	 */
	protected abstract String getPluginID();

	/**
	 * Returns the project to which this project nature applies.
	 * 
	 * @return the project handle
	 * @since 1.0.0
	 */
	public org.eclipse.core.resources.IProject getProject() {
		return project;
	}

	/**
	 * Return the full path of the project.
	 * 
	 * @return full project path (relative to workspace)
	 * @since 1.0.0
	 */
	public IPath getProjectPath() {
		return getProject().getFullPath();
	}

	/**
	 * Get WorkbenchURIConverter for this project.
	 * <p>
	 * This method assumes the URIConverter on the ResourceSet is the one that
	 * was created for the ResourceSet on behalf of this nature runtime.
	 * </p>
	 * 
	 * @return
	 * 
	 * @since 1.0.0
	 */
	protected IWorkbenchURIConverter getWorkbenchURIConverter() {
		return getKommaContextBase().getURIConverter();
	}

	public IWorkspace getWorkspace() {
		return getProject().getWorkspace();
	}

	/**
	 * Called from configure the first time configure is called on the nature.
	 * Default is do nothing. Subclasses should override and add in their own
	 * configuration.
	 * 
	 * @throws org.eclipse.core.runtime.CoreException
	 * 
	 * @since 1.0.0
	 */
	protected void primConfigure()
			throws org.eclipse.core.runtime.CoreException {

	}

	/**
	 * Sets the project to which this nature applies. Used when instantiating
	 * this project nature runtime. This is called by
	 * <code>IProject.addNature</code> and should not be called directly by
	 * clients.
	 * 
	 * @param project
	 *            the project to which this nature applies
	 * 
	 * @since 1.0.0
	 */
	public void setProject(org.eclipse.core.resources.IProject newProject) {
		project = newProject;
	}

}