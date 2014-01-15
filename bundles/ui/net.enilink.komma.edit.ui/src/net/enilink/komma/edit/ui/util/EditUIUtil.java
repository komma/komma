/**
 * <copyright>
 *
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: EditUIUtil.java,v 1.8 2008/05/23 21:49:17 davidms Exp $
 */

package net.enilink.komma.edit.ui.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.common.ui.EclipseUtil;
import net.enilink.komma.common.ui.URIEditorInput;
import net.enilink.komma.common.util.UniqueExtensibleList;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.base.ExtensibleURIConverter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class EditUIUtil {
	/**
	 * Opens the default editor for the resource. This method only works if the
	 * model's URI is a platform resource URI.
	 */
	public static boolean openEditor(IReference resource)
			throws PartInitException {
		URI uri = resource.getURI();
		if (uri != null) {
			URI normalizedURI = uri;
			if (resource instanceof IObject) {
				normalizedURI = ((IObject) resource).getModel().getModelSet()
						.getURIConverter().normalize(uri);
			}
			IEditorInput editorInput = null;
			if (normalizedURI.isPlatformResource()) {
				String path = normalizedURI.toPlatformString(true);
				IResource workspaceResource = ResourcesPlugin.getWorkspace()
						.getRoot().findMember(new Path(path));
				if (workspaceResource instanceof IFile) {
					editorInput = EclipseUtil
							.createEditorInput((IFile) workspaceResource);
				}
			}
			if (editorInput == null) {
				editorInput = new URIEditorInput(uri);
			}
			IEditorDescriptor editorDesc = getDefaultEditor(normalizedURI
					.toString());
			if (editorDesc != null) {
				IWorkbenchPage page = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage();
				IEditorPart editorPart = page.openEditor(editorInput,
						editorDesc.getId());
				return editorPart != null;
			}
		}
		return false;
	}

	public static URI getURI(IEditorInput editorInput) {
		URI result = null;
		if (AbstractKommaPlugin.IS_ECLIPSE_RUNNING) {
			result = EclipseUtil.getURI(editorInput);
		}
		if (result == null) {
			if (editorInput instanceof URIEditorInput) {
				result = ((URIEditorInput) editorInput).getURI().trimFragment();
			} else {
				result = URIImpl.createURI(editorInput.getName());
			}
		}
		return result;
	}

	/**
	 * Returns the default editor for a given file name. This method is like
	 * {@link IEditorRegistry#getDefaultEditor(String)}, but it will not return
	 * <code>null</code> unless all applicable content types have no associated
	 * editor.
	 * 
	 * @param fileName
	 *            the file name in the system
	 * @return the descriptor of the default editor, or <code>null</code> if not
	 *         found
	 */
	public static IEditorDescriptor getDefaultEditor(String fileName) {
		return fileName != null && fileName.length() != 0 ? getDefaultEditor(
				fileName,
				Platform.getContentTypeManager().findContentTypesFor(fileName))
				: null;
	}

	/**
	 * Returns the default editor for a given contents and file name.
	 * <p>
	 * If a file name is not provided, the entire content type registry will be
	 * queried. For performance reasons, it is highly recommended to provide a
	 * file name if available.
	 * 
	 * @param contents
	 *            an input stream
	 * @param fileName
	 *            the file name associated to the contents, or <code>null</code>
	 * @return the descriptor of the default editor, or <code>null</code> if not
	 *         found
	 */
	public static IEditorDescriptor getDefaultEditor(InputStream contents,
			String fileName) {
		if (contents != null) {
			try {
				return getDefaultEditor(
						fileName,
						Platform.getContentTypeManager().findContentTypesFor(
								contents, fileName));
			} catch (IOException e) {
				KommaEditUIPlugin.INSTANCE.log(e);
			}
		}
		return getDefaultEditor(fileName);
	}

	private static IEditorDescriptor getDefaultEditor(String fileName,
			IContentType[] contentTypes) {
		IEditorRegistry editorRegistry = PlatformUI.getWorkbench()
				.getEditorRegistry();

		if (contentTypes.length == 0) {
			return editorRegistry.getDefaultEditor(fileName, null);
		}

		IEditorDescriptor result = null;
		for (int i = 0; result == null && i < contentTypes.length; i++) {
			result = editorRegistry.getDefaultEditor(fileName, contentTypes[i]);
		}
		return result;
	}

	/**
	 * Returns the default editor for a given URI. This method actually attempts
	 * to open an input stream for the URI and uses its contents, along with the
	 * filename (the URI's last segment), to obtain appropriate content types.
	 * <p>
	 * If a URI converter is specified, it is used to open the stream.
	 * Otherwise, the global default {@link URIConverter#INSTANCE instance} is
	 * used.
	 * 
	 * @param uri
	 *            a URI
	 * @param uriConverter
	 *            URI converter from which to obtain an input stream, or
	 *            <code>null</code>
	 * @return the descriptor of the default editor, or <code>null</code> if not
	 *         found
	 */
	public static IEditorDescriptor getDefaultEditor(URI uri,
			IURIConverter uriConverter) {
		String fileName = URIImpl.decode(uri.lastSegment());
		if (uriConverter == null) {
			uriConverter = new ExtensibleURIConverter();
		}
		InputStream stream = null;

		try {
			stream = uriConverter.createInputStream(uri);
			return getDefaultEditor(stream, fileName);
		} catch (IOException e) {
			KommaEditUIPlugin.INSTANCE.log(e);
			return getDefaultEditor(fileName);
		} finally {
			close(stream);
		}
	}

	private static void close(InputStream stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				KommaEditUIPlugin.INSTANCE.log(e);
			}
		}
	}

	/**
	 * Returns the editors for a given file name. This method is like
	 * {@link IEditorRegistry#getEditors(String)}, but its result will include
	 * editors for all applicable content types.
	 * 
	 * @param fileName
	 *            the file name in the system
	 * @param defaultsOnly
	 *            if <code>true</code>, only the default editor for each content
	 *            type will be included in the result
	 * @return the descriptors of the editors
	 */
	public static IEditorDescriptor[] getEditors(String fileName,
			boolean defaultsOnly) {
		return fileName != null && fileName.length() != 0 ? getEditors(
				fileName,
				Platform.getContentTypeManager().findContentTypesFor(fileName),
				defaultsOnly) : new IEditorDescriptor[0];
	}

	/**
	 * Returns the editors for a given contents and file name.
	 * <p>
	 * If a file name is not provided, the entire content type registry will be
	 * queried. For performance reasons, it is highly recommended to provide a
	 * file name if available.
	 * 
	 * @param contents
	 *            an input stream
	 * @param fileName
	 *            the file name associated to the contents, or <code>null</code>
	 * @param defaultsOnly
	 *            if <code>true</code>, only the default editor for each content
	 *            type will be included in the result
	 * @return the descriptors of the editors
	 */
	public static IEditorDescriptor[] getEditors(InputStream contents,
			String fileName, boolean defaultsOnly) {
		if (contents != null) {
			try {
				return getEditors(fileName, Platform.getContentTypeManager()
						.findContentTypesFor(contents, fileName), defaultsOnly);
			} catch (IOException e) {
				KommaEditUIPlugin.INSTANCE.log(e);
			}
		}
		return getEditors(fileName, defaultsOnly);
	}

	private static IEditorDescriptor[] getEditors(String fileName,
			IContentType[] contentTypes, boolean defaultsOnly) {
		IEditorRegistry editorRegistry = PlatformUI.getWorkbench()
				.getEditorRegistry();

		if (contentTypes.length == 0) {
			return editorRegistry.getEditors(fileName, null);
		}

		List<IEditorDescriptor> result = new UniqueExtensibleList<IEditorDescriptor>();
		for (IContentType contentType : contentTypes) {
			if (defaultsOnly) {
				IEditorDescriptor editor = editorRegistry.getDefaultEditor(
						fileName, contentType);
				if (editor != null) {
					result.add(editor);
				}
			} else {
				result.addAll(Arrays.asList(editorRegistry.getEditors(fileName,
						contentType)));
			}
		}
		return result.toArray(new IEditorDescriptor[result.size()]);
	}

	/**
	 * Returns the editors for a given URI. This method actually attempts to
	 * open an input stream for the URI and uses its contents, along with the
	 * filename (the URI's last segment), to obtain appropriate content types.
	 * <p>
	 * If a URI converter is specified, it is used to open the stream.
	 * Otherwise, the global default {@link URIConverter#INSTANCE instance} is
	 * used.
	 * 
	 * @param uri
	 *            a URI
	 * @param uriConverter
	 *            a URI converter from which to obtain an input stream, or
	 *            <code>null</code>
	 * @param defaultsOnly
	 *            if <code>true</code>, only the default editor for each content
	 *            type will be included in the result
	 * @return the descriptors of the editors
	 */
	public static IEditorDescriptor[] getEditors(URI uri,
			IURIConverter uriConverter, boolean defaultsOnly) {
		String fileName = URIImpl.decode(uri.lastSegment());
		if (uriConverter == null) {
			uriConverter = new ExtensibleURIConverter();
		}
		InputStream stream = null;

		try {
			stream = uriConverter.createInputStream(uri);
			return getEditors(stream, fileName, defaultsOnly);
		} catch (IOException e) {
			KommaEditUIPlugin.INSTANCE.log(e);
			return getEditors(fileName, defaultsOnly);
		} finally {
			close(stream);
		}
	}

	/**
	 * Creates a corresponding error status object for the given
	 * <code>exception</code>.
	 * 
	 * @param exception
	 *            The exception for which an error status object should be
	 *            created.
	 * @return The error status.
	 */
	public static IStatus createErrorStatus(Exception exception) {
		return new Status(Status.ERROR, KommaEditUIPlugin.PLUGIN_ID,
				exception.getMessage(), exception);
	}
}
