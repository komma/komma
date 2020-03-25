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
 * $Id: EMFEditPlugin.java,v 1.9 2008/01/29 21:13:13 emerks Exp $
 */
package net.enilink.komma.edit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import net.enilink.commons.util.extensions.RegistryReader;
import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.URI;
import net.enilink.komma.edit.provider.ComposedAdapterFactory;
import net.enilink.komma.edit.provider.IChildCreationExtender;
import net.enilink.komma.internal.model.extensions.KommaRegistryReader;
import net.enilink.komma.internal.model.extensions.KommaRegistryReader.PluginClassDescriptor;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.RegistryFactory;

/**
 * The <b>Plugin</b> for the model EMF.Edit library. EMF must run within an
 * Eclipse workbench, within a headless Eclipse workspace, or just stand-alone
 * as part of some other application. To support this, all resource access
 * should be directed to the resource locator, which can redirect the service as
 * appropriate to the runtime. During stand-alone invocation no plugin
 * initialization takes place. In this case, emf.edit.resources.jar must be on
 * the CLASSPATH.
 * 
 * @see #INSTANCE
 */
public final class KommaEditPlugin extends AbstractKommaPlugin {
	public static final String PLUGIN_ID = "net.enilink.komma.edit";

	/**
	 * The singleton instance of the plugin.
	 */
	public static final KommaEditPlugin INSTANCE = new KommaEditPlugin();

	/**
	 * The one instance of this class.
	 */
	private static Implementation plugin;

	/**
	 * Creates the singleton instance.
	 */
	private KommaEditPlugin() {
		super(new IResourceLocator[] {});
	}

	/*
	 * Javadoc copied from base class.
	 */
	@Override
	public IResourceLocator getBundleResourceLocator() {
		return plugin;
	}

	/**
	 * The singleton instance of an
	 * {@link ComposedAdapterFactory.IDescriptor.IRegistry item provider adapter
	 * factory registry}.
	 */
	private static ComposedAdapterFactory.IDescriptor.IRegistry.Impl composedAdapterFactoryDescriptorRegistry;

	/**
	 * Returns a populated instance of an
	 * {@link ComposedAdapterFactory.IDescriptor.IRegistry item provider adapter
	 * factory registry}.
	 * 
	 * @return a populated instance of an item provider adapter factory
	 *         registry.
	 */
	public static ComposedAdapterFactory.IDescriptor.IRegistry getComposedAdapterFactoryDescriptorRegistry() {
		if (composedAdapterFactoryDescriptorRegistry == null) {
			composedAdapterFactoryDescriptorRegistry = new ComposedAdapterFactory.IDescriptor.IRegistry.Impl(
					null) {
				private static final long serialVersionUID = 1L;

				@Override
				public ComposedAdapterFactory.IDescriptor delegatedGetDescriptor(
						Collection<?> types) {
					List<Object> stringTypes = new ArrayList<Object>(
							types.size());
					for (Object key : types) {
						if (key instanceof URI) {
							stringTypes.add(((URI) key).toString());
						} else if (key instanceof Class<?>) {
							stringTypes.add(((Class<?>) key).getName());
						} else {
							return null;
						}
					}
					ComposedAdapterFactory.IDescriptor descriptor = (ComposedAdapterFactory.IDescriptor) get(stringTypes);
					if (descriptor != null) {
						put(types, descriptor);
						return descriptor;
					}

					return super.delegatedGetDescriptor(types);
				}
			};
			if (INSTANCE.getBundleResourceLocator() instanceof EclipsePlugin) {
				RegistryReader registryReader = new KommaRegistryReader(
						RegistryFactory.getRegistry(),
						INSTANCE.getSymbolicName(),
						"itemProviderAdapterFactories") {
					@Override
					protected boolean readElement(
							IConfigurationElement element, boolean add) {
						if (element.getName().equals("factory")) {
							String namespaceURI = element.getAttribute("uri");
							String className = element.getAttribute("class");
							String supportedTypes = element
									.getAttribute("supportedTypes");
							if (namespaceURI == null) {
								logMissingAttribute(element, "uri");
							} else if (className == null) {
								logMissingAttribute(element, "class");
							} else if (supportedTypes == null) {
								logMissingAttribute(element, "supportedTypes");
							}

							class PluginAdapterFactoryDescriptor extends
									PluginClassDescriptor implements
									ComposedAdapterFactory.IDescriptor {
								public PluginAdapterFactoryDescriptor(
										IConfigurationElement element,
										String attributeName) {
									super(element, attributeName);
								}

								public IAdapterFactory createAdapterFactory() {
									return (IAdapterFactory) createInstance();
								}
							}

							String[] namespaceURIs = namespaceURI.split("\\s");
							for (StringTokenizer stringTokenizer = new StringTokenizer(
									supportedTypes); stringTokenizer
									.hasMoreTokens();) {
								String supportedType = stringTokenizer
										.nextToken();
								for (String namespace : namespaceURIs) {
									List<Object> key = new ArrayList<Object>();
									key.add(namespace);
									key.add(supportedType);
									if (add) {
										composedAdapterFactoryDescriptorRegistry
												.put(key,
														new PluginAdapterFactoryDescriptor(
																element,
																"class"));
									} else {
										composedAdapterFactoryDescriptorRegistry
												.remove(key);
									}
								}
							}

							return true;
						}
						return false;
					}
				};
				registryReader.readRegistry();
			}
		}
		return composedAdapterFactoryDescriptorRegistry;
	}

	/**
	 * The singleton instance of a
	 * {@link IChildCreationExtender.IDescriptor.IRegistry child creation extender
	 * registry}.
	 */
	private static IChildCreationExtender.IDescriptor.IRegistry.Impl childCreationExtenderDescriptorRegistry;

	/**
	 * Returns a populated instance of a
	 * {@link IChildCreationExtender.IDescriptor.IRegistry child creation extender
	 * registry}.
	 * 
	 * @return a populated instance of child creation extender registry.
	 */
	public static IChildCreationExtender.IDescriptor.IRegistry getChildCreationExtenderDescriptorRegistry() {
		if (childCreationExtenderDescriptorRegistry == null) {
			childCreationExtenderDescriptorRegistry = new IChildCreationExtender.IDescriptor.IRegistry.Impl(
					null) {
				private static final long serialVersionUID = 1L;

				@Override
				public Collection<IChildCreationExtender.IDescriptor> delegatedGetDescriptors(
						String namespace) {
					Collection<IChildCreationExtender.IDescriptor> descriptors = get(namespace);
					return descriptors != null ? descriptors : super
							.delegatedGetDescriptors(namespace);
				}
			};
			if (INSTANCE.getBundleResourceLocator() instanceof EclipsePlugin) {
				RegistryReader registryReader = new RegistryReader(
						RegistryFactory.getRegistry(),
						INSTANCE.getSymbolicName(), "childCreationExtenders") {
					@Override
					protected boolean readElement(
							IConfigurationElement element, boolean add) {
						if (element.getName().equals("extender")) {
							String packageURI = element.getAttribute("uri");
							String className = element.getAttribute("class");
							if (packageURI == null) {
								logMissingAttribute(element, "uri");
							} else if (className == null) {
								logMissingAttribute(element, "class");
							}

							class PluginChildCreationExtenderDescriptor extends
									PluginClassDescriptor implements
									IChildCreationExtender.IDescriptor {
								public PluginChildCreationExtenderDescriptor(
										IConfigurationElement element,
										String attributeName) {
									super(element, attributeName);
								}

								public IChildCreationExtender createChildCreationExtender() {
									return (IChildCreationExtender) createInstance();
								}

								public boolean matches(
										IConfigurationElement element) {
									return element.getContributor().equals(
											this.element.getContributor());
								}
							}

							Collection<IChildCreationExtender.IDescriptor> collection = childCreationExtenderDescriptorRegistry
									.get(packageURI);
							if (add) {
								if (collection == null) {
									childCreationExtenderDescriptorRegistry
											.put(packageURI,
													collection = new ArrayList<IChildCreationExtender.IDescriptor>());
								}

								collection
										.add(new PluginChildCreationExtenderDescriptor(
												element, "class"));
							} else if (collection != null) {
								for (IChildCreationExtender.IDescriptor descriptor : collection) {
									if (descriptor instanceof PluginChildCreationExtenderDescriptor
											&& ((PluginChildCreationExtenderDescriptor) descriptor)
													.matches(element)) {
										collection.remove(descriptor);
										break;
									}
								}
							}

							return true;
						}
						return false;
					}
				};
				registryReader.readRegistry();
			}
		}
		return childCreationExtenderDescriptorRegistry;
	}

	/**
	 * Returns the singleton instance of the Eclipse plugin.
	 * 
	 * @return the singleton instance.
	 */
	public static Implementation getPlugin() {
		return plugin;
	}

	/**
	 * The actual implementation of the Eclipse <b>Plugin</b>.
	 */
	public static class Implementation extends EclipsePlugin {
		/**
		 * Creates an instance.
		 */
		public Implementation() {
			super();

			// Remember the static instance.
			//
			plugin = this;
		}
	}
}
