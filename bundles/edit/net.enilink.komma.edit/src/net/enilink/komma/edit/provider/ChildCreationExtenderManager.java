/**
 * <copyright> 
 *
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
 * $Id: ChildCreationExtenderManager.java,v 1.1 2008/01/29 21:13:13 emerks Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.ArrayList;
import java.util.List;

import net.enilink.komma.common.util.DelegatingResourceLocator;
import net.enilink.komma.common.util.ExtensibleList;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.edit.domain.IEditingDomain;

/**
 * A child creation extension manager encapsulates a
 * {@link #getChildCreationExtenders() list of child creation extenders} for a
 * particular package. The list is automatically populated based on
 * {@link IChildCreationExtender.Descriptor.Registry#getDescriptors(String)
 * registered extensions} for the package. The manager also acts as resource
 * locator that loads resources from either the primary resource locator or one
 * of the extenders.
 */
public class ChildCreationExtenderManager extends DelegatingResourceLocator {
	/**
	 * The primary resource locator.
	 */
	protected IResourceLocator primaryResourceLocator;

	/**
	 * A key in the {@link IChildCreationExtender.Descriptor.Registry child
	 * creation extender registry} which will typically be the namespace of the
	 * package being extended.
	 */
	protected String namespace;

	/**
	 * A specialized child creation extender list implementation that provides
	 * efficient access to the resource locators of the extenders.
	 */
	private static class ChildCreationExtenderList extends
			ExtensibleList<IChildCreationExtender> {
		private static final long serialVersionUID = 1L;

		/**
		 * An empty array.
		 */
		private static final IResourceLocator[] NO_RESOURCE_LOCATORS = new IResourceLocator[0];

		/**
		 * A cache of the resource locators of the extenders.
		 */
		protected IResourceLocator[] delegateResourceLocators = NO_RESOURCE_LOCATORS;

		/**
		 * The modCount the last time the delegateResourceLocators were
		 * computed.
		 */
		protected int expectedModCount = modCount;

		/**
		 * Creates an empty new instance.
		 */
		public ChildCreationExtenderList() {
			super();
		}

		/**
		 * This list may not contain null.
		 */
		@Override
		protected boolean canContainNull() {
			return false;
		}

		/**
		 * This list may only contain instances of
		 * {@link IChildCreationExtender}.
		 */
		@Override
		protected Object[] newData(int capacity) {
			return new IChildCreationExtender[capacity];
		}

		/**
		 * Returns an efficiently computed array of the delegate resource
		 * locators.
		 */
		protected IResourceLocator[] getDelegateResourceLocators() {
			if (expectedModCount != modCount) {
				expectedModCount = modCount;
				if (size == 0) {
					delegateResourceLocators = NO_RESOURCE_LOCATORS;
				} else {
					IChildCreationExtender[] childCreationExtenders = (IChildCreationExtender[]) data;
					delegateResourceLocators = new IResourceLocator[size];
					for (int i = 0; i < size; ++i) {
						delegateResourceLocators[i] = childCreationExtenders[i]
								.getResourceLocator();
					}
				}
			}
			return delegateResourceLocators;
		}
	}

	/**
	 * The list of child creation extenders.
	 */
	protected ChildCreationExtenderList childCreationExtenders;

	/**
	 * Creates an instance for the given primary resource locator to manage the
	 * extensions for the given namespace.
	 * 
	 * @param primaryResourceLocator
	 *            the resource locator for finding basic resources.
	 * @param namespace
	 *            a key in the
	 *            {@link IChildCreationExtender.Descriptor.Registry child
	 *            creation extender registry} which will typically be the
	 *            namespace of the package whose extensions are being managed.
	 */
	public ChildCreationExtenderManager(
			IResourceLocator primaryResourceLocator, String namespace) {
		this.primaryResourceLocator = primaryResourceLocator;
		this.namespace = namespace;
	}

	@Override
	protected IResourceLocator[] getDelegateResourceLocators() {
		getChildCreationExtenders();
		return childCreationExtenders.getDelegateResourceLocators();
	}

	@Override
	protected IResourceLocator getPrimaryResourceLocator() {
		return primaryResourceLocator;
	}

	/**
	 * Returns a modifiable list of the child creation extenders.
	 * 
	 * @return a modifiable list of the child creation extenders.
	 */
	public List<IChildCreationExtender> getChildCreationExtenders() {
		if (childCreationExtenders == null) {
			childCreationExtenders = new ChildCreationExtenderList();
			for (IChildCreationExtender.Descriptor descriptor : IChildCreationExtender.Descriptor.Registry.INSTANCE
					.getDescriptors(namespace)) {
				childCreationExtenders.add(descriptor
						.createChildCreationExtender());
			}
		}
		return childCreationExtenders;
	}

	/**
	 * Returns the list of new child descriptors accumulated from each of the
	 * child creation extenders.
	 * 
	 * @param object
	 *            the owner object of the descriptors.
	 * @param editingDomain
	 *            the domain for the descriptors.
	 * @return the list of new child descriptors accumulated from each of the
	 *         child creation extenders.
	 */
	public List<?> getNewChildDescriptors(Object object,
			IEditingDomain editingDomain) {
		List<Object> result = new ArrayList<Object>();
		for (IChildCreationExtender childCreationExtender : getChildCreationExtenders()) {
			result.addAll(childCreationExtender.getNewChildDescriptors(object,
					editingDomain));
		}
		return result;
	}
}
