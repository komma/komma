/**
 * <copyright>
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 */
package net.enilink.komma.edit.provider.komma;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.MissingResourceException;
import java.util.Set;

import net.enilink.komma.common.util.DelegatingResourceLocator;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.provider.ItemProviderAdapterFactory;
import net.enilink.vocab.komma.KOMMA;

public class KommaItemProviderAdapterFactory extends
		ItemProviderAdapterFactory<URI> {
	static Set<URI> properties = new HashSet<>(
			Arrays.asList(KOMMA.PROPERTY_ROOTPROPERTY));

	public KommaItemProviderAdapterFactory() {
		super(new DelegatingResourceLocator() {

			@Override
			protected Object delegatedGetImage(String key)
					throws MissingResourceException {
				// try png as fall-back
				return getPrimaryResourceLocator().getImage(key + ".png");
			}

			@Override
			protected IResourceLocator[] getDelegateResourceLocators() {
				return new IResourceLocator[0];
			}

			@Override
			protected IResourceLocator getPrimaryResourceLocator() {
				return KommaEditPlugin.INSTANCE;
			}
		}, KOMMA.NAMESPACE_URI);
	}

	@Override
	protected Object doAdapt(Object object, Object type) {
		if (object instanceof IReference && properties.contains(object)) {
			return super.doAdapt(object, type);
		}
		return null;
	}

	public void dispose() {
	}

	@Override
	protected Collection<URI> getTypes(Object object) {
		return Arrays.asList(((IReference) object).getURI());
	}

	@Override
	protected Object createItemProvider(Object object, Collection<URI> types,
			Object providerType) {
		return new KommaRootPropertyItemProvider(this, resourceLocator, types);
	}
}
