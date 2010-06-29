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
 * $Id: ComposedAdapterFactory.java,v 1.11 2008/01/29 21:13:13 emerks Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.enilink.komma.KommaCore;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.NotificationSupport;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.core.URI;

/**
 * This provides support for composing several factories for different models
 * into a single factory serving the union of the model objects.
 */
public class ComposedAdapterFactory extends NotificationSupport<INotification>
		implements IComposeableAdapterFactory, IDisposable {
	/**
	 * A descriptor can create an adapter factory. They are used as the values
	 * in a {@link IDescriptor.IRegistry registry}.
	 */
	public interface IDescriptor {
		/**
		 * A registry is an index that takes a collection of keys, typically a
		 * pair consisting of an EPackage or java.lang.Package, and a
		 * java.lang.Class, and maps it to a {@link IDescriptor descriptor}.
		 */
		interface IRegistry {
			/**
			 * A simple registry implementation that supports delegation.
			 */
			class Impl extends HashMap<Collection<?>, Object> implements
					IRegistry {
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

				/**
				 * This is called when local lookup fails.
				 */
				protected IDescriptor delegatedGetDescriptor(Collection<?> types) {
					if (delegateRegistry != null) {
						return delegateRegistry.getDescriptor(types);
					}

					return null;
				}

				public IDescriptor getDescriptor(Collection<?> types) {
					IDescriptor descriptor = (IDescriptor) get(types);
					return descriptor == null ? delegatedGetDescriptor(types)
							: descriptor;
				}
			}

			/**
			 * The global registry typically populated by plugin registration.
			 */
			IRegistry INSTANCE = net.enilink.komma.edit.KommaEditPlugin
					.getComposedAdapterFactoryDescriptorRegistry();

			/**
			 * Returns descriptor that can create a factory for the types.
			 * 
			 * @param types
			 *            collections of keys, typically a pair consisting of an
			 *            EPackage or java.lang.Package, and a java.lang.Class.
			 * @return a descriptor that can create a factory for the types.
			 */
			IDescriptor getDescriptor(Collection<?> types);
		}

		/**
		 * Creates an adapter factory.
		 * 
		 * @return a new adapter factory.
		 */
		IAdapterFactory createAdapterFactory();
	}

	/**
	 * This keeps track of all the
	 * {@link net.enilink.komma.IAdapterFactory.common.notify.AdapterFactory}
	 * delegates.
	 */
	protected List<IAdapterFactory> adapterFactories = new ArrayList<IAdapterFactory>();

	/**
	 * This is used to demand create adapter factories from a registry.
	 */
	protected IDescriptor.IRegistry adapterFactoryDescriptorRegistry;

	/**
	 * This is used to implement the {@link IComposeableAdapterFactory}
	 * interface.
	 */
	protected ComposedAdapterFactory parentAdapterFactory;

	public ComposedAdapterFactory() {
		super();
	}

	public ComposedAdapterFactory(
			Collection<? extends IAdapterFactory> adapterFactories) {
		for (IAdapterFactory adapterFactory : adapterFactories) {
			addAdapterFactory(adapterFactory);
		}
	}

	public ComposedAdapterFactory(IAdapterFactory adapterFactory) {
		addAdapterFactory(adapterFactory);
	}

	public ComposedAdapterFactory(IAdapterFactory[] adapterFactories) {
		for (int i = 0; i < adapterFactories.length; ++i) {
			addAdapterFactory(adapterFactories[i]);
		}
	}

	public ComposedAdapterFactory(
			IDescriptor.IRegistry adapterFactoryDescriptorRegistry) {
		this.adapterFactoryDescriptorRegistry = adapterFactoryDescriptorRegistry;
	}

	public Object adapt(Object target, Object type) {
		Object adapter = internalAdapt(target, type);

		if (!(type instanceof Class<?>)
				|| ((Class<?>) type).isInstance(adapter)) {
			return adapter;
		}

		return null;
	}

	protected Object adaptEntityClass(IResource target, Object type,
			Collection<Object> typesCache, Set<URI> seenNamespaces,
			Collection<IClass> seenClasses, Collection<? extends IClass> classes) {
		Object result = null;

		for (IClass resourceClass : classes) {
			if (!seenClasses.add(resourceClass)) {
				continue;
			}

			URI name = resourceClass.getURI();
			if (name == null) {
				continue;
			}
			URI namespace;
			try {
				namespace = name.namespace();
			} catch (Exception e) {
				KommaCore.log(e);
				continue;
			}
			if (seenNamespaces.add(namespace)) {
				typesCache.add(namespace);
				if (type != null) {
					typesCache.add(type);
				}
				IAdapterFactory delegateAdapterFactory = getFactoryForTypes(typesCache);
				if (delegateAdapterFactory != null) {
					result = delegateAdapterFactory.adapt(target, type);
				}
				if (result != null) {
					return result;
				}

				typesCache.clear();
			}
		}

		for (final IClass resourceClass : classes) {
			result = adaptEntityClass(target, type, typesCache, seenNamespaces,
					seenClasses, resourceClass.getDirectNamedSuperClasses()
							.toList());
			if (result != null) {
				break;
			}
		}

		return result;
	}

	protected Object adaptJavaClass(Object target, Object type,
			Collection<Object> failedPackages, Class<?> javaClass) {
		Object result = null;

		Package javaPackage = javaClass.getPackage();
		if (failedPackages.add(javaPackage)) {
			Collection<Object> types = new ArrayList<Object>();
			types.add(javaPackage);
			if (type != null) {
				types.add(type);
			}
			IAdapterFactory delegateAdapterFactory = getFactoryForTypes(types);
			if (delegateAdapterFactory != null) {
				result = delegateAdapterFactory.adapt(target, type);
			}
		}

		if (result == null) {
			Class<?> superclass = javaClass.getSuperclass();
			if (superclass != null) {
				result = adaptJavaClass(target, type, failedPackages, javaClass
						.getSuperclass());
			}
			if (result == null) {
				Class<?>[] interfaces = javaClass.getInterfaces();
				for (int i = 0; i < interfaces.length; ++i) {
					result = adaptJavaClass(target, type, failedPackages,
							interfaces[i]);
					if (result != null) {
						break;
					}
				}
			}
		}

		return result;
	}

	public void addAdapterFactory(IAdapterFactory adapterFactory) {
		if (!adapterFactories.contains(adapterFactory)) {
			adapterFactories.add(adapterFactory);
			if (adapterFactory instanceof IComposeableAdapterFactory) {
				((IComposeableAdapterFactory) adapterFactory)
						.setParentAdapterFactory(this);
			}
		}
	}

	protected IAdapterFactory delegatedGetFactoryForTypes(Collection<?> types) {
		return null;
	}

	public void dispose() {
		for (Object factory : adapterFactories) {
			if (factory instanceof IDisposable) {
				((IDisposable) factory).dispose();
			}
		}
	}

	@Override
	public void fireNotifications(
			Collection<? extends INotification> notifications) {
		super.fireNotifications(notifications);

		if (parentAdapterFactory != null) {
			parentAdapterFactory.fireNotifications(notifications);
		}
	}

	public IAdapterFactory getFactoryForTypes(Collection<?> types) {
		IAdapterFactory result = null;

		FactoryLoop: for (IAdapterFactory factory : adapterFactories) {
			if (factory instanceof ComposedAdapterFactory) {
				IAdapterFactory candidate = ((ComposedAdapterFactory) factory)
						.getFactoryForTypes(types);
				if (candidate != null) {
					return candidate;
				}
			} else {
				for (Object type : types) {
					if (!factory.isFactoryForType(type)) {
						continue FactoryLoop;
					}
				}
				return factory;
			}
		}

		if (adapterFactoryDescriptorRegistry != null) {
			IDescriptor descriptor = adapterFactoryDescriptorRegistry
					.getDescriptor(types);
			if (descriptor != null) {
				result = descriptor.createAdapterFactory();
				addAdapterFactory(result);
			}
		}

		return result == null ? delegatedGetFactoryForTypes(types) : result;
	}

	/**
	 * This returns the root adapter factory that delegates to this factory.
	 */
	public IComposeableAdapterFactory getRootAdapterFactory() {
		return parentAdapterFactory == null ? this : parentAdapterFactory
				.getRootAdapterFactory();
	}

	public void insertAdapterFactory(IAdapterFactory adapterFactory) {
		if (!adapterFactories.contains(adapterFactory)) {
			adapterFactories.add(0, adapterFactory);
			if (adapterFactory instanceof IComposeableAdapterFactory) {
				((IComposeableAdapterFactory) adapterFactory)
						.setParentAdapterFactory(this);
			}
		}
	}

	protected Object internalAdapt(Object target, Object type) {
		Object result = null;

		if (target instanceof IClass) {
			result = adaptEntityClass((IResource) target, type,
					new ArrayList<Object>(), new HashSet<URI>(),
					new HashSet<IClass>(), Arrays.asList((IClass) target));
		}

		if (result != null) {
			return result;
		}

		if (target instanceof IResource) {
			IResource resource = (IResource) target;
			result = adaptEntityClass(resource, type, new ArrayList<Object>(),
					new HashSet<URI>(), new HashSet<IClass>(), resource
							.getDirectNamedClasses().toList());
		}

		if (result != null) {
			return result;
		}

		if (target != null) {
			result = adaptJavaClass(target, type, new HashSet<Object>(), target
					.getClass());
		}
		return result == null ? target : result;
	}

	public boolean isFactoryForType(Object type) {
		for (IAdapterFactory adapterFactory : adapterFactories) {
			if (adapterFactory.isFactoryForType(type)) {
				return true;
			}
		}

		return false;
	}

	public void removeAdapterFactory(IAdapterFactory adapterFactory) {
		if (adapterFactories.contains(adapterFactory)) {
			adapterFactories.remove(adapterFactory);
			if (adapterFactory instanceof IComposeableAdapterFactory) {
				((IComposeableAdapterFactory) adapterFactory)
						.setParentAdapterFactory(null);
			}
		}
	}

	/**
	 * This sets the direct parent adapter factory into which this factory is
	 * composed.
	 */
	public void setParentAdapterFactory(
			ComposedAdapterFactory parentAdapterFactory) {
		this.parentAdapterFactory = parentAdapterFactory;
	}
}
