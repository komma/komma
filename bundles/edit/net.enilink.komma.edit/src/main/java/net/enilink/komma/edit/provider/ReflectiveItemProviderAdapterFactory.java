/**
 * <copyright>
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
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
 * $Id: ReflectiveItemProviderAdapterFactory.java,v 1.4 2006/12/28 06:48:53 marcelop Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.Collection;
import java.util.Collections;

import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.URI;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IResource;

/**
 * This is the factory that is used to provide the interfaces needed to support
 * Viewers reflectively.
 */
public class ReflectiveItemProviderAdapterFactory extends
		ItemProviderAdapterFactory<IClass> {
	public ReflectiveItemProviderAdapterFactory(
			IResourceLocator resourceLocator, URI... namespaceURIs) {
		super(resourceLocator, namespaceURIs);
	}

	protected Object createItemProvider(Object object,
			Collection<IClass> types, Object providerType) {
		return new ReflectiveItemProvider(this, resourceLocator, types);
	}

	protected Collection<IClass> getTypes(Object object) {
		return object instanceof IResource ? ((IResource) object)
				.getDirectNamedClasses().toSet() : Collections
				.<IClass> emptySet();
	}
}
