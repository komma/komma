/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.eclipse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.content.IContentDescription;

import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.base.URIHandler;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

public class PlatformResourceURIHandler extends URIHandler {
	/**
	 * The platform resource map.
	 * 
	 * @see #getPlatformResourceMap
	 */
	private static Map<String, URI> platformResourceMap;

	/**
	 * An output stream that transfers its contents to an {@link IFile} upon
	 * closing.
	 */
	public static class PlatformResourceOutputStream extends
			ByteArrayOutputStream {
		protected IFile file;
		protected boolean force;
		protected boolean keepHistory;
		protected IProgressMonitor progressMonitor;
		protected boolean previouslyFlushed;

		public PlatformResourceOutputStream(IFile file, boolean force,
				boolean keepHistory, IProgressMonitor progressMonitor) {
			this.file = file;
			this.force = force;
			this.keepHistory = keepHistory;
			this.progressMonitor = progressMonitor;
		}

		protected void createContainer(IContainer container) throws IOException {
			if (!container.exists()) {
				if (container.getType() == IResource.FOLDER) {
					createContainer(container.getParent());
					try {
						((IFolder) container).create(force, keepHistory,
								progressMonitor);
					} catch (CoreException exception) {
						throw new IModel.IOWrappedException(exception);
					}
				}
			}
		}

		@Override
		public void close() throws IOException {
			flush();
			super.close();
		}

		@Override
		public void flush() throws IOException {
			super.flush();

			if (previouslyFlushed) {
				if (count == 0) {
					return;
				}
			} else {
				createContainer(file.getParent());
			}

			byte[] contents = toByteArray();
			InputStream inputStream = new ByteArrayInputStream(contents, 0,
					contents.length);

			try {
				if (previouslyFlushed) {
					file.appendContents(inputStream, force, false,
							progressMonitor);
				} else if (!file.exists()) {
					file.create(inputStream, false, null);
					previouslyFlushed = true;
				} else {
					if (!file.isSynchronized(IResource.DEPTH_ONE)) {
						file.refreshLocal(IResource.DEPTH_ONE, progressMonitor);
					}
					file.setContents(inputStream, force, keepHistory,
							progressMonitor);
					previouslyFlushed = true;
				}
				reset();
			} catch (CoreException exception) {
				throw new IModel.IOWrappedException(exception);
			}
		}
	}

	/**
	 * Isolated Eclipse workbench utilities.
	 */
	public static class WorkbenchHelper {
		/**
		 * Creates an output stream for the given {@link IFile} path.
		 * <p>
		 * This implementation uses a
		 * {@link PlatformResourceURIHandler.PlatformResourceOutputStream}.
		 * </p>
		 * 
		 * @return an open output stream.
		 * @exception IOException
		 *                if there is a problem obtaining an open output stream.
		 * @see IWorkspaceRoot#getFile(org.eclipse.core.runtime.IPath)
		 * @see PlatformResourceURIHandler.PlatformResourceOutputStream
		 * @see IFile#setContents(InputStream, boolean, boolean,
		 *      IProgressMonitor)
		 */
		public static OutputStream createPlatformResourceOutputStream(
				String platformResourcePath, Map<?, ?> options)
				throws IOException {
			IFile file = getWorkspaceRoot().getFile(
					new Path(platformResourcePath));
			@SuppressWarnings("unchecked")
			final Map<Object, Object> response = options == null ? null
					: (Map<Object, Object>) options
							.get(IURIConverter.OPTION_RESPONSE);
			return new PlatformResourceOutputStream(file, false, true, null) {
				@Override
				public void close() throws IOException {
					try {
						super.close();
					} finally {
						if (response != null) {
							response.put(
									IURIConverter.RESPONSE_TIME_STAMP_PROPERTY,
									file.getLocalTimeStamp());
						}
					}
				}
			};
		}

		/**
		 * Creates an input stream for the given {@link IFile} path.
		 * <p>
		 * This implementation uses {@link IFile#getContents()
		 * IFile.getContents}.
		 * </p>
		 * 
		 * @return an open input stream.
		 * @see IWorkspaceRoot#getFile(org.eclipse.core.runtime.IPath)
		 * @see IFile#getContents()
		 * @exception IOException
		 *                if there is a problem obtaining an open input stream.
		 */
		public static InputStream createPlatformResourceInputStream(
				String platformResourcePath, Map<?, ?> options)
				throws IOException {
			IFile file = getWorkspaceRoot().getFile(
					new Path(platformResourcePath));
			try {
				if (!file.isSynchronized(IResource.DEPTH_ONE)) {
					file.refreshLocal(IResource.DEPTH_ONE, null);
				}
				InputStream result = file.getContents();
				if (options != null) {
					@SuppressWarnings("unchecked")
					Map<Object, Object> response = (Map<Object, Object>) options
							.get(IURIConverter.OPTION_RESPONSE);
					if (response != null) {
						response.put(
								IURIConverter.RESPONSE_TIME_STAMP_PROPERTY,
								file.getLocalTimeStamp());
					}
				}
				return result;
			} catch (CoreException exception) {
				throw new IModel.IOWrappedException(exception);
			}
		}

		public static void delete(String platformResourcePath, Map<?, ?> options)
				throws IOException {
			IFile file = getWorkspaceRoot().getFile(
					new Path(platformResourcePath));
			try {
				file.delete(true, null);
			} catch (CoreException exception) {
				throw new IModel.IOWrappedException(exception);
			}
		}

		public static boolean exists(String platformResourcePath,
				Map<?, ?> options) {
			IResource resource = getWorkspaceRoot().findMember(
					new Path(platformResourcePath));
			return resource != null;
		}

		public static Map<String, ?> attributes(String platformResourcePath,
				Map<?, ?> options) {
			IResource resource = getWorkspaceRoot().findMember(
					new Path(platformResourcePath));
			Map<String, Object> result = new HashMap<String, Object>();
			if (resource != null) {
				@SuppressWarnings("unchecked")
				Set<String> requestedAttributes = options == null ? null
						: (Set<String>) options
								.get(IURIConverter.OPTION_REQUESTED_ATTRIBUTES);

				if (requestedAttributes == null
						|| requestedAttributes
								.contains(IURIConverter.ATTRIBUTE_TIME_STAMP)) {
					result.put(IURIConverter.ATTRIBUTE_TIME_STAMP, resource
							.getLocalTimeStamp());
				}
				ResourceAttributes resourceAttributes = null;
				if (requestedAttributes == null
						|| requestedAttributes
								.contains(IURIConverter.ATTRIBUTE_READ_ONLY)) {
					resourceAttributes = resource.getResourceAttributes();
					result.put(IURIConverter.ATTRIBUTE_READ_ONLY,
							resourceAttributes.isReadOnly());
				}
				if (requestedAttributes == null
						|| requestedAttributes
								.contains(IURIConverter.ATTRIBUTE_ARCHIVE)) {
					if (resourceAttributes == null) {
						resourceAttributes = resource.getResourceAttributes();
					}
					result.put(IURIConverter.ATTRIBUTE_ARCHIVE,
							resourceAttributes.isArchive());
				}
				if (requestedAttributes == null
						|| requestedAttributes
								.contains(IURIConverter.ATTRIBUTE_EXECUTABLE)) {
					if (resourceAttributes == null) {
						resourceAttributes = resource.getResourceAttributes();
					}
					result.put(IURIConverter.ATTRIBUTE_EXECUTABLE,
							resourceAttributes.isExecutable());
				}
				if (requestedAttributes == null
						|| requestedAttributes
								.contains(IURIConverter.ATTRIBUTE_HIDDEN)) {
					if (resourceAttributes == null) {
						resourceAttributes = resource.getResourceAttributes();
					}
					result.put(IURIConverter.ATTRIBUTE_HIDDEN,
							resourceAttributes.isHidden());
				}
				if (requestedAttributes == null
						|| requestedAttributes
								.contains(IURIConverter.ATTRIBUTE_DIRECTORY)) {
					if (resourceAttributes == null) {
						resourceAttributes = resource.getResourceAttributes();
					}
					result.put(IURIConverter.ATTRIBUTE_DIRECTORY,
							resource instanceof IContainer);
				}
			}
			return result;
		}

		public static void updateAttributes(String platformResourcePath,
				Map<String, ?> attributes, Map<?, ?> options)
				throws IOException {
			IResource resource = getWorkspaceRoot().findMember(
					new Path(platformResourcePath));
			if (resource == null) {
				throw new FileNotFoundException("The resource "
						+ platformResourcePath + " does not exist");
			} else {
				try {
					Long timeStamp = (Long) attributes
							.get(IURIConverter.ATTRIBUTE_TIME_STAMP);
					if (timeStamp != null) {
						resource.setLocalTimeStamp(timeStamp);
					}

					ResourceAttributes resourceAttributes = null;
					Boolean readOnly = (Boolean) attributes
							.get(IURIConverter.ATTRIBUTE_READ_ONLY);
					if (readOnly != null) {
						resourceAttributes = resource.getResourceAttributes();
						resourceAttributes.setReadOnly(readOnly);
					}
					Boolean archive = (Boolean) attributes
							.get(IURIConverter.ATTRIBUTE_ARCHIVE);
					if (archive != null) {
						if (resourceAttributes == null) {
							resourceAttributes = resource
									.getResourceAttributes();
						}
						resourceAttributes.setArchive(archive);
					}
					Boolean executable = (Boolean) attributes
							.get(IURIConverter.ATTRIBUTE_EXECUTABLE);
					if (executable != null) {
						if (resourceAttributes == null) {
							resourceAttributes = resource
									.getResourceAttributes();
						}
						resourceAttributes.setExecutable(executable);
					}
					Boolean hidden = (Boolean) attributes
							.get(IURIConverter.ATTRIBUTE_HIDDEN);
					if (hidden != null) {
						if (resourceAttributes == null) {
							resourceAttributes = resource
									.getResourceAttributes();
						}
						resourceAttributes.setHidden(hidden);
					}

					if (resourceAttributes != null) {
						resource.setResourceAttributes(resourceAttributes);
					}
				} catch (CoreException exception) {
					throw new IModel.IOWrappedException(exception);
				}
			}
		}

		public static IContentDescription getContentDescription(
				String platformResourcePath, Map<?, ?> options)
				throws IOException {
			IFile file = getWorkspaceRoot().getFile(
					new Path(platformResourcePath));
			try {
				return file.getContentDescription();
			} catch (CoreException exception) {
				throw new IModel.IOWrappedException(exception);
			}
		}

		public static IWorkspaceRoot getWorkspaceRoot() {
			return ModelPlugin.getWorkspaceRoot();
		}
	}

	@Override
	public boolean canHandle(URI uri) {
		return uri.isPlatform();
	}

	/**
	 * Creates an output stream for the platform resource path and returns it.
	 * <p>
	 * This implementation does one of two things, depending on the runtime
	 * environment. If there is an Eclipse workspace, it delegates to
	 * {@link WorkbenchHelper#createPlatformResourceOutputStream
	 * WorkbenchHelper.createPlatformResourceOutputStream}, which gives the
	 * expected Eclipse behaviour. Otherwise, the
	 * {@link EcorePlugin#resolvePlatformResourcePath resolved} URI is delegated
	 * to {@link #createOutputStream createOutputStream} for recursive
	 * processing.
	 * 
	 * @return an open output stream.
	 * @exception IOException
	 *                if there is a problem obtaining an open output stream or a
	 *                valid interpretation of the path.
	 * @see EcorePlugin#resolvePlatformResourcePath(String)
	 */
	@Override
	public OutputStream createOutputStream(URI uri, Map<?, ?> options)
			throws IOException {
		String platformResourcePath = uri.toPlatformString(true);
		if (WorkbenchHelper.getWorkspaceRoot() != null) {
			return WorkbenchHelper.createPlatformResourceOutputStream(
					platformResourcePath, options);
		} else {
			URI resolvedLocation = resolvePlatformResourcePath(platformResourcePath);
			if (resolvedLocation != null) {
				return ((IURIConverter) options
						.get(IURIConverter.OPTION_URI_CONVERTER))
						.createOutputStream(resolvedLocation, options);
			}

			throw new IOException("The path '" + platformResourcePath
					+ "' is unmapped");
		}
	}

	/**
	 * Creates an input stream for the platform resource path and returns it.
	 * <p>
	 * This implementation does one of two things, depending on the runtime
	 * environment. If there is an Eclipse workspace, it delegates to
	 * {@link WorkbenchHelper#createPlatformResourceInputStream
	 * WorkbenchHelper.createPlatformResourceInputStream}, which gives the
	 * expected Eclipse behaviour. Otherwise, the
	 * {@link EcorePlugin#resolvePlatformResourcePath resolved} URI is delegated
	 * to {@link #createInputStream createInputStream} for recursive processing.
	 * 
	 * @return an open input stream.
	 * @exception IOException
	 *                if there is a problem obtaining an open input stream or a
	 *                valid interpretation of the path.
	 * @see EcorePlugin#resolvePlatformResourcePath(String)
	 */
	@Override
	public InputStream createInputStream(URI uri, Map<?, ?> options)
			throws IOException {
		String platformResourcePath = uri.toPlatformString(true);
		if (WorkbenchHelper.getWorkspaceRoot() != null) {
			return WorkbenchHelper.createPlatformResourceInputStream(
					platformResourcePath, options);
		} else {
			URI resolvedLocation = resolvePlatformResourcePath(platformResourcePath);
			if (resolvedLocation != null) {
				return getURIConverter(options).createInputStream(
						resolvedLocation, options);
			}

			throw new IOException("The path '" + platformResourcePath
					+ "' is unmapped");
		}
	}

	@Override
	public boolean exists(URI uri, Map<?, ?> options) {
		String platformResourcePath = uri.toPlatformString(true);
		if (WorkbenchHelper.getWorkspaceRoot() != null) {
			return WorkbenchHelper.exists(platformResourcePath, options);
		} else {
			URI resolvedLocation = resolvePlatformResourcePath(platformResourcePath);
			return resolvedLocation != null
					&& getURIConverter(options).exists(resolvedLocation,
							options);
		}
	}

	@Override
	public Map<String, ?> getAttributes(URI uri, Map<?, ?> options) {
		String platformResourcePath = uri.toPlatformString(true);
		if (WorkbenchHelper.getWorkspaceRoot() != null) {
			return WorkbenchHelper.attributes(platformResourcePath, options);
		} else {
			URI resolvedLocation = resolvePlatformResourcePath(platformResourcePath);
			return resolvedLocation == null ? Collections
					.<String, Object> emptyMap() : getURIConverter(options)
					.getAttributes(resolvedLocation, options);
		}
	}

	@Override
	public void setAttributes(URI uri, Map<String, ?> attributes,
			Map<?, ?> options) throws IOException {
		String platformResourcePath = uri.toPlatformString(true);
		if (WorkbenchHelper.getWorkspaceRoot() != null) {
			WorkbenchHelper.updateAttributes(platformResourcePath, attributes,
					options);
		} else {
			URI resolvedLocation = resolvePlatformResourcePath(platformResourcePath);
			if (resolvedLocation != null) {
				getURIConverter(options).setAttributes(resolvedLocation,
						attributes, options);
			} else {
				throw new IOException("The platform resource path '"
						+ platformResourcePath + "' does not resolve");
			}
		}
	}

	/**
	 * Returns the platform resource map.
	 * <p>
	 * This map is from {@link String} to {@link URI}. It is the logical
	 * equivalent of the map implied by an {@link IWorkspaceRoot}: I.e., each
	 * entry in the map corresponds to an
	 * {@link org.eclipse.core.resources.IProject} that has a
	 * {@link org.eclipse.core.resources.IResource#getName name} and a location
	 * {@link org.eclipse.core.resources.IResource#getLocation location}; the
	 * name is the key and the location, interpreted as a
	 * {@link URI#createFileURI file URI}, is the value. This map is used to
	 * {@link #resolvePlatformResourcePath resolve} a platform resource path,
	 * and thereby supports relocatable projects in a manner that is
	 * transparently the same as an Eclipse workspace.
	 * </p>
	 * 
	 * @return the platform resource map.
	 * @see #resolvePlatformResourcePath
	 */
	public static Map<String, URI> getPlatformResourceMap() {
		if (platformResourceMap == null) {
			platformResourceMap = new HashMap<String, URI>();
		}
		return platformResourceMap;
	}

	/**
	 * Resolves a platform resource path of the form
	 * <code>"/project/path"</code> against the platform resource map.
	 * <p>
	 * The first segment of the path, i.e., the <em>project name</em>, is used
	 * to get a URI from the {@link #getPlatformResourceMap() map}. If a URI
	 * results, the remaining segments are {@link URI#resolve(URI) resolved}
	 * against it and that is the result. Otherwise, the result is
	 * <code>null</code>. For example, given this mapping
	 * 
	 * <pre>
	 * EcoreUtil.getPlatformResourceMap().put(&quot;project&quot;,
	 * 		URI.createURI(&quot;file:///C:/location/&quot;));
	 * </pre>
	 * 
	 * the following transformation would result:
	 * 
	 * <pre>
	 *  /project/directory/file
	 *    -&gt;
	 *  file:///C:/location/directory/file
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @return the resolved URI or <code>null</code>.
	 */
	public static URI resolvePlatformResourcePath(String platformResourcePath) {
		if (platformResourceMap != null) {
			int index = platformResourcePath.indexOf("/", 1);
			String rootContainerName = platformResourcePath.substring(1, index);
			String relativeName = platformResourcePath.substring(index + 1);
			URI rootContainerLocation = getPlatformResourceMap().get(
					rootContainerName);
			if (rootContainerLocation != null) {
				return URIImpl.createURI(relativeName).resolve(
						rootContainerLocation);
			}
		}
		return null;
	}
}
