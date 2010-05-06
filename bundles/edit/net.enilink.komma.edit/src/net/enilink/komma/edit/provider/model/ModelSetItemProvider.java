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
 * $Id: ResourceSetItemProvider.java,v 1.8 2007/06/14 18:32:42 emerks Exp $
 */
package net.enilink.komma.edit.provider.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.provider.IEditingDomainItemProvider;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.edit.provider.IItemPropertyDescriptor;
import net.enilink.komma.edit.provider.IItemPropertySource;
import net.enilink.komma.edit.provider.IStructuredItemContentProvider;
import net.enilink.komma.edit.provider.ITreeItemContentProvider;
import net.enilink.komma.edit.provider.ItemProviderAdapter;
import net.enilink.komma.model.IModelSet;

/**
 * This is the item provider adapter for a {@link IModelSet} object.
 */
public class ModelSetItemProvider extends ItemProviderAdapter implements
		IEditingDomainItemProvider, IStructuredItemContentProvider,
		ITreeItemContentProvider, IItemLabelProvider, IItemPropertySource {
	/**
	 * This constructs an instance from a factory and a notifier.
	 */
	public ModelSetItemProvider(IAdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	/**
	 * This returns the property descriptors for the adapted class.
	 */
	@Override
	public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object) {
		if (itemPropertyDescriptors == null) {
			super.getPropertyDescriptors(object);
		}
		return itemPropertyDescriptors;
	}

	@Override
	public Collection<?> getChildren(Object object) {
		IModelSet modelSet = (IModelSet) object;
		return modelSet.getModels();
	}

	/**
	 * This specifies how to implement {@link #getChildren} and is used to
	 * deduce an appropriate feature for an
	 * {@link net.enilink.komma.edit.command.AddCommand},
	 * {@link net.enilink.komma.edit.command.RemoveCommand} or
	 * {@link net.enilink.komma.edit.command.MoveCommand} in
	 * {@link #createCommand(Object, IEditingDomain, Class, net.enilink.komma.edit.command.CommandParameter)
	 * createCommand}.
	 */
	@Override
	public Collection<? extends IProperty> getChildrenProperties(Object object) {
		if (childrenProperties == null) {
			super.getChildrenProperties(object);
			/*
			 * ResourceSet resourceSet = (ResourceSet)object;
			 * childrenFeatures.add
			 * (ResourcePackage.eINSTANCE.getResourceSet_Resources());
			 */
		}
		return childrenProperties;
	}

	/**
	 * This returns the parent of the ResourceSet.
	 */
	@Override
	public Object getParent(Object object) {
		return null;
	}

	/**
	 * This returns ResourceSet.gif.
	 */
	@Override
	public Object getImage(Object object) {
		return getResourceLocator().getImage("full/obj16/ModelSet");
	}

	/**
	 * This returns the label text for the adapted class.
	 */
	@Override
	public String getText(Object object) {
		return KommaEditPlugin.INSTANCE.getString("_UI_ModelSet_label");
	}

	@Override
	public Collection<?> getNewChildDescriptors(Object object,
			IEditingDomain editingDomain, Object sibling) {
		return Collections.emptyList();
	}

	/**
	 * This adds {@link net.enilink.komma.edit.command.CommandParameter}s
	 * describing the children that can be created under this object.
	 */
	@Override
	protected void collectNewChildDescriptors(
			Collection<Object> newChildDescriptors, Object object) {
		super.collectNewChildDescriptors(newChildDescriptors, object);
	}

	/**
	 * Return the resource locator for this item provider's resources.
	 */
	@Override
	public IResourceLocator getResourceLocator() {
		return KommaEditPlugin.INSTANCE;
	}
}
