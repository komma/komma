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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.NotificationSupport;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IResource;

import com.google.inject.Inject;
import com.google.inject.Injector;

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
			class Impl extends ConcurrentHashMap<Collection<?>, Object>
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
			 *            namespace URI or java.lang.Package, and a
			 *            {@link IClass} or java.lang.Class.
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
	protected List<IAdapterFactory> adapterFactories = new CopyOnWriteArrayList<IAdapterFactory>();

	/**
	 * This is used to demand create adapter factories from a registry.
	 */
	protected IDescriptor.IRegistry adapterFactoryDescriptorRegistry;

	/**
	 * This is used to implement the {@link IComposeableAdapterFactory}
	 * interface.
	 */
	protected ComposedAdapterFactory parentAdapterFactory;

	@Inject
	protected Injector injector;

	public ComposedAdapterFactory() {
		super();
	}

	public ComposedAdapterFactory(
			Collection<? extends IAdapterFactory> adapterFactories) {
		for (IAdapterFactory adapterFactory : adapterFactories) {
			appendAdapterFactory(adapterFactory);
		}
	}

	public ComposedAdapterFactory(IAdapterFactory adapterFactory) {
		appendAdapterFactory(adapterFactory);
	}

	public ComposedAdapterFactory(IAdapterFactory[] adapterFactories) {
		for (int i = 0; i < adapterFactories.length; ++i) {
			appendAdapterFactory(adapterFactories[i]);
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

	protected Object adaptEntity(IResource target, Object type,
			Set<URI> seenNamespaces, final Collection<IClass> seenClasses,
			List<IClass> classes) {
		Collection<Object> typesCache = new ArrayList<Object>();

		if (seenClasses != null) {
			seenClasses.addAll(classes);
		}
		while (!classes.isEmpty()) {
			IClass resourceClass = classes.remove(0);

			URI name = resourceClass.getURI();
			if (name == null) {
				continue;
			}
			URI namespace;
			try {
				namespace = name.namespace();
			} catch (Exception e) {
				KommaEditPlugin.INSTANCE.log(e);
				continue;
			}
			if (seenNamespaces.add(namespace)) {
				typesCache.add(namespace);
				if (type != null) {
					typesCache.add(type);
				}
				IAdapterFactory delegateAdapterFactory = getFactoryForTypes(typesCache);
				if (delegateAdapterFactory != null) {
					Object result = delegateAdapterFactory.adapt(target, type);
					if (result != null) {
						return result;
					}
				}

				typesCache.clear();
			}

			if (seenClasses != null) {
				for (IClass superClass : resourceClass
						.getDirectNamedSuperClasses()) {
					if (!seenClasses.add(superClass)) {
						continue;
					}
					int index = Collections.binarySearch(classes, superClass,
							IResource.RANK_COMPARATOR);
					if (index < 0) {
						index = -(index + 1);
					}
					classes.add(index, superClass);
				}
			}
		}

		IAdapterFactory defaultAdapterFactory = getDefaultAdapterFactory(type);
		return defaultAdapterFactory != null ? defaultAdapterFactory.adapt(
				target, type) : null;
	}

	protected IAdapterFactory getDefaultAdapterFactory(Object type) {
		return null;
	}

	protected Object adaptJavaObject(Object target, Object type,
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
				result = adaptJavaObject(target, type, failedPackages,
						javaClass.getSuperclass());
			}
			if (result == null) {
				Class<?>[] interfaces = javaClass.getInterfaces();
				for (int i = 0; i < interfaces.length; ++i) {
					result = adaptJavaObject(target, type, failedPackages,
							interfaces[i]);
					if (result != null) {
						break;
					}
				}
			}
		}

		if (result == null) {
			IAdapterFactory defaultAdapterFactory = getDefaultAdapterFactory(type);
			if (defaultAdapterFactory != null) {
				result = defaultAdapterFactory.adapt(target, type);
			}
		}

		return result;
	}

	public void appendAdapterFactory(IAdapterFactory adapterFactory) {
		synchronized (adapterFactories) {
			// synchronize to ensure that insertion in combination with contains
			// is only executed once
			if (!adapterFactories.contains(adapterFactory)) {
				adapterFactories.add(adapterFactory);
				if (adapterFactory instanceof IComposeableAdapterFactory) {
					((IComposeableAdapterFactory) adapterFactory)
							.setParentAdapterFactory(this);
				}
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
				if (null != injector) {
					injector.injectMembers(result);
				}
				appendAdapterFactory(result);
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

	public void prependAdapterFactory(IAdapterFactory adapterFactory) {
		synchronized (adapterFactories) {
			// synchronize to ensure that insertion in combination with contains
			// is only executed once
			if (!adapterFactories.contains(adapterFactory)) {
				adapterFactories.add(0, adapterFactory);
				if (adapterFactory instanceof IComposeableAdapterFactory) {
					((IComposeableAdapterFactory) adapterFactory)
							.setParentAdapterFactory(this);
				}
			}
		}
	}

	protected Object internalAdapt(Object target, Object type) {
		Object result = null;

		if (target instanceof IReference) {
			// class or property
			URI uri = ((IReference) target).getURI();
			if (uri != null) {
				IAdapterFactory factory = getFactoryForTypes(Arrays.asList(
						uri.namespace(), type));
				if (factory != null) {
					result = factory.adapt(target, type);
				}
			}
		}

		if (result != null) {
			return result;
		}

		if (target instanceof IResource) {
			IResource resource = (IResource) target;
			result = adaptEntity(resource, type, new HashSet<URI>(),
					new HashSet<IClass>(), sort(resource
							.getDirectNamedClasses().toList()));
		}

		if (result != null) {
			return result;
		}

		if (target != null) {
			result = adaptJavaObject(target, type, new HashSet<Object>(),
					target.getClass());
		}
		return result == null ? target : result;
	}

	/**
	 * Sort classes according to their "semantic" level.
	 * 
	 * @param classes
	 * @return
	 */
	private List<IClass> sort(List<IClass> classes) {
		Collections.sort(classes, IResource.RANK_COMPARATOR);
		return classes;
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
