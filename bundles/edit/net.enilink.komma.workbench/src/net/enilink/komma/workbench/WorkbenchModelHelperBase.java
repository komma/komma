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
 *  $$RCSfile: WorkbenchResourceHelperBase.java,v $$
 *  $$Revision: 1.5 $$  $$Date: 2006/08/09 15:40:22 $$ 
 */
package net.enilink.komma.workbench;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import net.enilink.komma.common.util.WrappedException;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.workbench.internal.KommaWorkbenchContextFactory;
import net.enilink.komma.workbench.internal.KommaWorkbenchPlugin;

/**
 * Workbench resource helper
 * 
 * @since 1.0.0
 */
public class WorkbenchModelHelperBase {

	/**
	 * Everything is static, don't know why there is an instance here.
	 */
	public static final WorkbenchModelHelperBase INSTANCE = new WorkbenchModelHelperBase();

	/**
	 * Return an existing context base on <code>aProject</code>.
	 * 
	 * @param aProject
	 * @return the context base for the project or <code>null</code> if none.
	 * 
	 * @since 1.0.0
	 */
	public static KommaWorkbenchContextBase getKommaContext(IProject aProject) {
		return KommaWorkbenchContextFactory.INSTANCE.getKommaContext(aProject);
	}

	/**
	 * Get the IFile for the URI. The URI must be a workbench project style URI.
	 * 
	 * @param uri
	 *            The URI MUST be either a
	 *            "<b>platform:/resource/</b>project-name/...." type URI or it
	 *            must be of type "project-name/...". This method will only
	 *            return resources that are workbench project resources. Any
	 *            other type of URI will cause <code>null</code> to be returned.
	 * @return the IFile if the URI is a project form, <code>null</code> if not
	 *         a project form, OR the project doesn't exist. The IFile returned
	 *         doesn't necessarily exist. Use {@link IFile#exists()} to test
	 *         that.
	 * 
	 * @since 1.2.0
	 */
	public static IFile getIFile(URI uri) {
		IProject project = getProject(uri);
		if (project != null) {
			IPath path;
			if (isPlatformModelURI(uri)) {
				// Need to get the path and remove the first two segments
				// (/resource/project name/).
				path = new Path(URIImpl.decode(uri.path()))
						.removeFirstSegments(2);
			} else {
				// Need to get the path and remove the first segment (/project
				// name/).
				path = new Path(URIImpl.decode(uri.path()))
						.removeFirstSegments(1);
			}
			return project.getFile(path);
		} else
			return null;
	}

	/**
	 * Check for a cached model for the given URI, if none is found, create a
	 * new model for with the URI against the given model set.
	 * 
	 * @param uri
	 * @param set
	 * @return resource or <code>null</code> if set was <code>null</code>.
	 * 
	 * @since 1.0.0
	 */
	public static IModel getExistingOrCreateResource(URIImpl uri, IModelSet set) {
		if (set != null) {
			IModel model = set.getModel(uri, false);
			if (model == null) {
				model = set.createModel(uri);
			}
			return model;
		}

		return null;
	}

	/**
	 * Return a new or existing context base on <code>aProject</code>. Allow the
	 * <code>contributor</code> to contribute to the new or existing nature
	 * prior to returning.
	 * 
	 * @param project
	 * @param contributor
	 * @return the context base for the project.
	 * 
	 * @since 1.0.0
	 */
	public static KommaWorkbenchContextBase createKommaContext(
			IProject project, IKommaContextContributor contributor) {
		return KommaWorkbenchContextFactory.INSTANCE.createKommaContext(
				project, contributor);
	}

	/**
	 * Does the passed URI have the form platform:/resource/... ?
	 * 
	 * @param uri
	 * @return <code>true</code> if it is a platform resource protocol.
	 * 
	 * @since 1.0.0
	 */
	public static boolean isPlatformModelURI(URI uri) {
		return KommaWorkbenchPlugin.PLATFORM_PROTOCOL.equals(uri.scheme())
				&& KommaWorkbenchPlugin.PLATFORM_RESOURCE
						.equals(uri.segment(0));
	}

	/**
	 * This api is used if you create a new MOF resource and you want to add it
	 * to the correct ResourceSet. In order to do that, we need the IProject
	 * that you want aResource to be cached within as well as the IPath which is
	 * the full path of the location of the new Resource.
	 * 
	 * @param project
	 * @param aResource
	 * @param fullPath
	 * @return <code>true</code> if resource was cached.
	 * 
	 * @since 1.0.0
	 */
	// public static boolean cacheResource(IProject project, Resource aResource,
	// IPath fullPath) {
	// if (project == null || aResource == null || !project.isAccessible())
	// return false;
	// IModelSet set = getModelSet(project);
	// if (set != null) {
	// URI converted = set.getURIConverter().normalize(aResource.getURI());
	// if (converted != aResource.getURI())
	// aResource.setURI(converted);
	// return set.getResources().add(aResource);
	// }
	// return false;
	// }

	/**
	 * Get the path of the project resource relative to the workspace or
	 * relative to the list of containers in this project.
	 * 
	 * @param aResource
	 * @return path
	 * 
	 * @since 1.0.0
	 */
	public static String getActualProjectRelativeURI(IResource aResource) {
		if (aResource == null || !aResource.isAccessible())
			return null;
		IProject project = aResource.getProject();
		IPath path = getPathInProject(project, aResource.getFullPath());
		return path.makeRelative().toString();
	}

	/**
	 * Return an IPath that can be used to load a Resource using the
	 * <code>fullPath</code>. This will be a project relative path.
	 * 
	 * @param project
	 * @param fullPath
	 * @return path
	 * 
	 * @since 1.0.0
	 */
	public static IPath getPathInProject(IProject project, IPath fullPath) {
		List<?> containers = getProjectURIConverterContainers(project);
		if (!containers.isEmpty())
			return getPathFromContainers(containers, fullPath);
		return fullPath;
	}

	protected static List<?> getProjectURIConverterContainers(IProject project) {
		KommaWorkbenchContextBase nature = createKommaContext(project, null);
		if (nature != null) {
			IWorkbenchURIConverter conv = (IWorkbenchURIConverter) nature
					.getURIConverter();
			if (conv != null)
				return conv.getInputContainers();
		}
		return Collections.EMPTY_LIST;
	}

	/**
	 * If this path is contained within one of the listed containers, then
	 * return the path relative to the container.
	 * 
	 * @param containers
	 * @param fullPath
	 * @return path relative to a container, or unchanged path if not in a
	 *         container.
	 * 
	 * @since 1.0.0
	 */
	public static IPath getPathFromContainers(List<?> containers, IPath fullPath) {
		IContainer container = null;
		IPath result;
		int size = containers.size();
		int matching = -1;
		IPath containerPath;
		for (int i = 0; i < size; i++) {
			container = (IContainer) containers.get(i);
			containerPath = container.getFullPath();
			matching = fullPath.matchingFirstSegments(containerPath);
			if (matching > 0 && matching == containerPath.segmentCount()) {
				result = fullPath.removeFirstSegments(matching);
				result = result.makeRelative();
				return result;
			}
		}
		return fullPath;
	}

	/**
	 * Return true if the <code>uri</code> has its container segments visible
	 * from the input containers for the <code>project</code>.
	 * 
	 * @param project
	 * @param uri
	 * @return <code>true</code> if the uri is visible from the input
	 *         containers.
	 * 
	 * @since 1.0.0
	 */
	public static boolean hasContainerStructure(IProject project, URIImpl uri) {
		if (project != null && uri != null) {
			IPath path = new Path(uri.toString());
			List<?> containers = getProjectURIConverterContainers(project);
			int segmentCount = path.segmentCount();
			IPath containerPath = segmentCount > 1 ? path.removeLastSegments(1)
					: null;
			IContainer container = null;
			for (int i = 0; i < containers.size(); i++) {
				container = (IContainer) containers.get(i);
				if (!container.isAccessible())
					continue;
				if (segmentCount == 1) {
					if (container == project)
						return true;
				} else if (containerPath != null) {
					IFolder folder = container.getFolder(containerPath);
					if (folder != null && folder.isAccessible())
						return true;
				}
			}
		}
		return false;
	}

	/*
	 * Get the project for the uri if the uri is a valid workbench project
	 * format uri. null otherwise.
	 */
	private static IProject getProject(URI uri) {
		String projectName;
		if (isPlatformModelURI(uri))
			projectName = uri.segment(1);
		else if (uri.scheme() == null) {
			projectName = new Path(uri.path()).segment(0); // assume project
			// name is first in
			// the URI
		} else
			return null;
		IProject project = getWorkspace().getRoot().getProject(
				URIImpl.decode(projectName));
		if (project != null && project.isAccessible())
			return project;
		else
			return null;
	}

	/**
	 * Get the workspace. (just use {@link ResourcesPlugin#getWorkspace()}).
	 * 
	 * @return
	 * 
	 * @since 1.0.0
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Get the project associated with the resource set.
	 * 
	 * @param set
	 * @return project or <code>null</code> if resource set not associated with
	 *         a project.
	 * 
	 * @since 1.0.0
	 */
	public static IProject getProject(IModelSet set) {
		if (set != null) {
			if (set instanceof IProjectModelSet) {
				return ((IProjectModelSet) set).getProject();
			}
		}
		return null;
	}

	protected static boolean isRegisteredURIMapping(String href) {
		if (href != null) {
			String file = href;
			int index = href.indexOf('#');
			if (index > -1)
				file = href.substring(0, index);
			return ModelPlugin.getDefault().getURIMap().map(
					URIImpl.createURI(file)) != null;
		}
		return false;
	}

	/**
	 * Return true if the WrappedException is actually a Resource Not Found.
	 * 
	 * @param wrappedEx
	 * @return <code>true</code> is exception wrappers a resource not found.
	 * @since 1.0.0
	 */
	public static boolean isResourceNotFound(WrappedException wrappedEx) {
		Exception excep = wrappedEx.exception();
		while (excep instanceof WrappedException) {
			excep = ((WrappedException) excep).exception();
		}
		return primIsResourceNotFound(excep);
	}

	private static boolean primIsResourceNotFound(Throwable excep) {
		if (excep instanceof CoreException) {
			IStatus status = ((CoreException) excep).getStatus();
			return status.getCode() == IResourceStatus.RESOURCE_NOT_FOUND
					&& ResourcesPlugin.PI_RESOURCES.equals(status.getPlugin());
		}
		return false;
	}

	/**
	 * Return true if the WrappedException is actually a Resource Not Found.
	 * 
	 * @param wrappedEx
	 * @return <code>true</code> is exception wrappers a resource not found.
	 * @since 1.0.0
	 */
	public static boolean isResourceNotFound(IModel.IOWrappedException wrappedEx) {
		return primIsResourceNotFound(wrappedEx.getCause());
	}

	/**
	 * Return a URI represenation of the platformURI without the leading
	 * "platform:/resource/" if present.
	 * 
	 * @param platformURI
	 * @return uri
	 * @since 1.0.0
	 */
	public static URI getNonPlatformURI(URIImpl platformURI) {
		if (isPlatformModelURI(platformURI)) {
			String uriString = primGetNonPlatformURIString(platformURI);
			return URIImpl.createURI(uriString);
		}
		return platformURI;
	}

	/**
	 * Return a String represenation of the platformURI without the leading
	 * "platform:/resource/" if present.
	 * 
	 * @param platformURI
	 * @return
	 * @since 1.0.0
	 */
	public static String getNonPlatformURIString(URIImpl platformURI) {
		if (isPlatformModelURI(platformURI)) {
			return primGetNonPlatformURIString(platformURI);
		}
		return platformURI.toString();
	}

	/*
	 * Remove "platform:/resource/" from the front of the platformURI and return
	 * the remaining String.
	 */
	private static String primGetNonPlatformURIString(URIImpl platformURI) {
		String uriString = platformURI.toString();
		// "platform:/resource/" is 19 characters.
		return uriString.substring(19, uriString.length());
	}

	/**
	 * Does the passed URI have the form platform:/plugin/... ?
	 * 
	 * @param uri
	 * @return <code>true</code> if uri is platform plugin protocol.
	 * 
	 * @since 1.0.0
	 */
	public static boolean isPlatformPluginResourceURI(URI uri) {
		return KommaWorkbenchPlugin.PLATFORM_PROTOCOL.equals(uri.scheme())
				&& KommaWorkbenchPlugin.PLATFORM_PLUGIN.equals(uri.segment(0));
	}

}