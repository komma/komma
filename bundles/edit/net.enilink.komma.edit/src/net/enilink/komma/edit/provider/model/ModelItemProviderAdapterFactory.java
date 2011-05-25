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
 */
package net.enilink.komma.edit.provider.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.MissingResourceException;

import net.enilink.komma.common.util.DelegatingResourceLocator;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.edit.provider.ReflectiveItemProvider;
import net.enilink.komma.edit.provider.ReflectiveItemProviderAdapterFactory;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;

public class ModelItemProviderAdapterFactory extends
		ReflectiveItemProviderAdapterFactory {
	public ModelItemProviderAdapterFactory() {
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
		}, MODELS.NAMESPACE_URI);
	}

	@Override
	public Object adapt(Object object, Object type) {
		if (object instanceof IClass) {
			return doAdapt(object, type);
		}
		return doAdapt(object, this);
	}

	@Override
	protected Object createAdapter(Object object, Object type) {
		// in the case of instantiated model sets and models
		if (object instanceof IModel) {
			return createModelAdapter();
		} else if (object instanceof IModelSet) {
			return createModelSetAdapter();
		}

		// in the case of editors for model set containers and model sets
		return super.createAdapter(object, type);
	}

	@Override
	protected Object createItemProvider(Object object,
			Collection<IClass> types, Object providerType) {
		if (object instanceof IClass) {
			if (IItemLabelProvider.class.equals(providerType)) {
				// override label provider for classes
				return new ReflectiveItemProvider(this, resourceLocator, types) {
					@Override
					protected Collection<? extends IClass> getTypes(
							Object object) {
						return Arrays.asList((IClass) object);
					}

					@Override
					public boolean isAdapterForType(Object type) {
						return IItemLabelProvider.class.equals(type);
					}
				};
			}
			return null;
		}
		return super.createItemProvider(object, types, providerType);
	}

	/**
	 * This creates an adapter for a
	 * {@link org.eclipse.emf.ecore.resource.Resource}.
	 * 
	 */
	public Object createModelAdapter() {
		return new ModelItemProvider(this);
	}

	/**
	 * This creates an adapter for a
	 * {@link org.eclipse.emf.ecore.resource.ResourceSet}.
	 * 
	 */
	public Object createModelSetAdapter() {
		return new ModelSetItemProvider(this);
	}

	public void dispose() {
	}
}
