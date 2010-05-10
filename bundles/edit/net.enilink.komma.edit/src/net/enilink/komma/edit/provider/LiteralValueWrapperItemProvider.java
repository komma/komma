/**
 * <copyright> 
 *
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
 * $Id: AttributeValueWrapperItemProvider.java,v 1.4 2006/12/28 06:48:54 marcelop Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.Collections;
import java.util.List;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.command.CopyCommand;
import net.enilink.komma.edit.command.SetCommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.core.IReference;

/**
 * A wrapper implementation for simple attribute values.
 */
public class LiteralValueWrapperItemProvider extends WrapperItemProvider
		implements IStructuredItemContentProvider, ITreeItemContentProvider,
		IItemLabelProvider, IItemPropertySource, IEditingDomainItemProvider {
	/**
	 * The resource locator from the owner's item provider.
	 */
	protected IResourceLocator resourceLocator;

	/**
	 * The single property descriptor for the value is cached here as a
	 * singleton list.
	 */
	protected List<IItemPropertyDescriptor> propertyDescriptors;

	/**
	 * Creates an instance for a single-valued attribute.
	 */
	public LiteralValueWrapperItemProvider(Object value, IObject owner,
			IReference property, IAdapterFactory adapterFactory,
			IResourceLocator resourceLocator) {
		super(value, owner, property, CommandParameter.NO_INDEX,
				adapterFactory);
		this.resourceLocator = resourceLocator;
	}

	/**
	 * Creates an instance for a value within a multi-valued attribute.
	 */
	public LiteralValueWrapperItemProvider(Object value, IObject owner,
			IReference property, int index, IAdapterFactory adapterFactory,
			IResourceLocator resourceLocator) {
		super(value, owner, property, index, adapterFactory);
		this.resourceLocator = resourceLocator;
	}

	/**
	 * If non-null, the value is converted to a string, using the type of its
	 * attribute and the appropriate factory.
	 */
	@Override
	public String getText(Object object) {
		return value != null ? ModelUtil.getLabel(value) : "null";
	}

	/**
	 * Creates, caches and returns an item property descriptor for the value.
	 */
	@Override
	public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object) {
		if (propertyDescriptors == null) {
			propertyDescriptors = Collections
					.<IItemPropertyDescriptor> singletonList(new WrapperItemPropertyDescriptor(
							resourceLocator, property));
		}
		return propertyDescriptors;
	}

	/**
	 * Returns a wrapped set command that returns as its affected object the
	 * replacement wrapper for the value.
	 */
	@Override
	protected ICommand createSetCommand(IEditingDomain domain, Object owner,
			Object feature, Object value, int index) {
		return new ReplacementAffectedObjectCommand(SetCommand.create(domain,
				this.owner, this.property, value, this.index));
	}

	/**
	 * Returns a {@link WrapperItemProvider.SimpleCopyCommand} that copies the
	 * value by converting it into a string and back, using the factory methods.
	 */
	@Override
	protected ICommand createCopyCommand(IEditingDomain domain, Object owner,
			CopyCommand.Helper helper) {
		return new SimpleCopyCommand(domain) {
			@Override
			public IWrapperItemProvider copy() {
				Object valueCopy = null;

				if (value != null) {
//					KommaUtil.convertToType(((IObject)getOwner()).getModel(), , typeUri)
				}
				return new LiteralValueWrapperItemProvider(valueCopy,
						(IObject) LiteralValueWrapperItemProvider.this.owner,
						(IReference) property, index, adapterFactory, resourceLocator);
			}
		};
	}
}
