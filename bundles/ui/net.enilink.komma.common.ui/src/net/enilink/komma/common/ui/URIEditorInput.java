/**
 * <copyright>
 *
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
 * $Id: URIEditorInput.java,v 1.6 2007/10/02 16:13:53 emerks Exp $
 */
package net.enilink.komma.common.ui;

import java.io.File;
import java.lang.reflect.Constructor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.osgi.framework.Bundle;

import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

/**
 * An implementation of an {@link org.eclipse.ui.IEditorInput} to wrap a
 * {@link URI}.
 */
public class URIEditorInput implements IEditorInput, IPersistableElement {
	private URI uri;
	private String name;

	public URIEditorInput(URI uri) {
		this.uri = uri;
	}

	public URIEditorInput(URI uri, String name) {
		this.uri = uri;
		this.name = name;
	}

	public URIEditorInput(IMemento memento) {
		loadState(memento);
	}

	@Override
	public int hashCode() {
		return uri.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof URIEditorInput
				&& uri.equals(((URIEditorInput) o).getURI());
	}

	/**
	 * @return the uri
	 */
	public URI getURI() {
		return uri;
	}

	/**
	 * Returns <b>true</b> only if the URI represents a file and if this file
	 * exists.
	 * 
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	public boolean exists() {
		if (getURI().isFile()) {
			return new File(getURI().toFileString()).exists();
		} else {
			if (AbstractKommaPlugin.IS_RESOURCES_BUNDLE_AVAILABLE) {
				return EclipseUtil.exists(uri);
			} else {
				return false;
			}
		}
	}

	/**
	 * Returns the <i>toString</i> value of the associated URI.
	 * 
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	public String getName() {
		if (name == null) {
			URI uri = getURI();
			return URIImpl.decode(uri.isHierarchical()
					&& uri.lastSegment() != null ? uri.lastSegment() : uri
					.toString());
		} else {
			return name;
		}
	}

	public String getToolTipText() {
		return getURI().toString();
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public IPersistableElement getPersistable() {
		return this;
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		if (AbstractKommaPlugin.IS_RESOURCES_BUNDLE_AVAILABLE) {
			Object result = EclipseUtil.getAdapter(adapter, uri);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	public String getFactoryId() {
		return URIEditorInputFactory.ID;
	}

	protected String getBundleSymbolicName() {
		return CommonUIPlugin.getPlugin().getSymbolicName();
	}

	protected static final String BUNDLE_TAG = "bundle";
	protected static final String CLASS_TAG = "class";
	protected static final String URI_TAG = "uri";
	protected static final String NAME_TAG = "name";

	static URIEditorInput create(IMemento memento) {
		String bundleSymbolicName = memento.getString(BUNDLE_TAG);
		String className = memento.getString(CLASS_TAG);
		try {
			Bundle bundle = Platform.getBundle(bundleSymbolicName);
			Class<?> theClass = bundle.loadClass(className);
			Constructor<?> constructor = theClass
					.getConstructor(IMemento.class);
			return (URIEditorInput) constructor.newInstance(memento);
		} catch (Exception exception) {
			CommonUIPlugin.INSTANCE.log(exception);
			return new URIEditorInput(memento);
		}
	}

	public void saveState(IMemento memento) {
		memento.putString(BUNDLE_TAG, getBundleSymbolicName());
		memento.putString(CLASS_TAG, getClass().getName());
		memento.putString(URI_TAG, uri.toString());
		memento.putString(NAME_TAG, name);
	}

	protected void loadState(IMemento memento) {
		uri = URIImpl.createURI(memento.getString(URI_TAG));
		name = memento.getString(NAME_TAG);
	}

	protected static class EclipseUtil {
		public static Object getAdapter(Class<?> adapter, URI uri) {
			if ((adapter == IFile.class || adapter == IResource.class)
					&& uri.isPlatformResource()) {
				return ResourcesPlugin.getWorkspace().getRoot()
						.getFile(new Path(uri.toPlatformString(true)));
			} else {
				return null;
			}
		}

		public static boolean exists(URI uri) {
			if (uri.isPlatformResource()) {
				return ResourcesPlugin.getWorkspace().getRoot()
						.getFile(new Path(uri.toPlatformString(true))).exists();
			} else {
				return false;
			}
		}
	}
}