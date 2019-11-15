/**
 * <copyright> 
 *
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
 * $Id: IChildCreationExtender.java,v 1.1 2008/01/29 21:13:13 emerks Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.IEditingDomain;

/**
 * An interface used by objects that can extend the results of
 * {@link IEditingDomainItemProvider#getNewChildDescriptors(Object, IEditingDomain, Object)
 * IEditingDomainItemProvider.getNewChildDescriptors}.
 */
public interface IChildCreationExtender {
	/**
	 * Returns a collection of objects describing the children that can be added
	 * under the given object in the editing domain.
	 */
	Collection<?> getNewChildDescriptors(Object object,
			IEditingDomain editingDomain);

	/**
	 * Returns a resource locator than can locate resources related to the child
	 * descriptors.
	 */
	IResourceLocator getResourceLocator();

	/**
	 * A descriptor can create a child creation extender. They are used as the
	 * values in a {@link IDescriptor.IRegistry registry}.
	 */
	interface IDescriptor {
		/**
		 * Creates a child creation extender.
		 * 
		 * @return a new child creation extender.
		 */
		IChildCreationExtender createChildCreationExtender();

		/**
		 * A registry is an index that takes a namespace and maps it to a
		 * collection of {@link IDescriptor descriptor}s.
		 */
		interface IRegistry {
			/**
			 * The global registry typically populated by plugin registration.
			 */
			IRegistry INSTANCE = KommaEditPlugin
					.getChildCreationExtenderDescriptorRegistry();

			/**
			 * Returns collection of descriptors that can create a child
			 * creation extenders.
			 * 
			 * @param namespace
			 *            a key which will typically be the namespace of the
			 *            package for which to create child creation extenders.
			 * @return a collection of descriptors that can create a child
			 *         creation extender.
			 */
			Collection<IDescriptor> getDescriptors(String namespace);

			/**
			 * A simple registry implementation that supports delegation.
			 */
			class Impl extends HashMap<String, Collection<IDescriptor>>
					implements IRegistry {
				private static final long serialVersionUID = 1L;

				/**
				 * The delegate registry should lookup fail locally.
				 */
				protected IRegistry delegateRegistry;

				/**
				 * Creates an instance.
				 * 
				 * @param delegateRegistry
				 *            <code>null</code> or a registration that should
				 *            act as the delegate.
				 */
				public Impl(IRegistry delegateRegistry) {
					this.delegateRegistry = delegateRegistry;
				}

				public Collection<IDescriptor> getDescriptors(String namespace) {
					Collection<IDescriptor> descriptor = get(namespace);
					return descriptor == null ? delegatedGetDescriptors(namespace)
							: descriptor;
				}

				/**
				 * This is called when local lookup fails.
				 */
				protected Collection<IDescriptor> delegatedGetDescriptors(
						String namespace) {
					if (delegateRegistry != null) {
						return delegateRegistry.getDescriptors(namespace);
					}

					return Collections.emptyList();
				}
			}
		}
	}
}