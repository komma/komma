/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.internal.model.Messages;
import net.enilink.komma.internal.model.extensions.ContentFactoriesRegistryReader;
import net.enilink.komma.internal.model.extensions.ContentHandlerRegistryReader;
import net.enilink.komma.internal.model.extensions.ExtensionFactoriesRegistryReader;
import net.enilink.komma.internal.model.extensions.ProtocolFactoriesRegistryReader;
import net.enilink.komma.internal.model.extensions.URIMappingRegistryReader;
import net.enilink.komma.model.base.ContentHandlerRegistry;
import net.enilink.komma.model.base.IURIMapRuleSet;
import net.enilink.komma.model.base.ModelFactoryRegistry;
import net.enilink.komma.model.base.URIMapRuleSet;
import net.enilink.komma.model.validation.Diagnostician;
import net.enilink.komma.model.validation.IValidator;
import net.enilink.komma.model.validation.ValidatorRegistry;
import net.enilink.komma.core.KommaModule;

/**
 * The activator class controls the plug-in life cycle
 */
public class ModelCore extends AbstractKommaPlugin {
	private IValidator.Registry validatorRegistry = new ValidatorRegistry();

	private IContentHandler.Registry contentHandlerRegistry = new ContentHandlerRegistry();

	private IModel.Factory.Registry modelFactoryRegistry = new ModelFactoryRegistry();

	private IURIMapRuleSet uriMap = new URIMapRuleSet();

	// The plug-in ID
	public static final String PLUGIN_ID = "net.enilink.komma.model";

	private static final ModelCore INSTANCE = new ModelCore();

	/**
	 * The constructor
	 */
	public ModelCore() {
		super(new IResourceLocator[] {});
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static ModelCore getDefault() {
		return INSTANCE;
	}

	public static void logErrorMessage(String message) {
		getDefault().log(
				new Status(IStatus.ERROR, PLUGIN_ID,
						IModelStatusConstants.INTERNAL_ERROR, message, null));
	}

	public static void logErrorStatus(String message, IStatus status) {
		if (status == null) {
			logErrorMessage(message);
			return;
		}
		MultiStatus multi = new MultiStatus(PLUGIN_ID,
				IModelStatusConstants.INTERNAL_ERROR, message, null);
		multi.add(status);
		getDefault().log(multi);
	}

	public static void log(Throwable e) {
		getDefault().log(
				new Status(IStatus.ERROR, PLUGIN_ID,
						IModelStatusConstants.INTERNAL_ERROR,
						Messages.ModelCore_internal_error, e));
	}

	@Override
	public IResourceLocator getBundleResourceLocator() {
		return plugin;
	}

	public IContentHandler.Registry getContentHandlerRegistry() {
		return contentHandlerRegistry;
	}

	public IModel.Factory.Registry getModelFactoryRegistry() {
		return modelFactoryRegistry;
	}

	public IURIMapRuleSet getURIMap() {
		return uriMap;
	}

	public IValidator.Registry getValidatorRegistry() {
		return validatorRegistry;
	}

	private Diagnostician diagnosticion;

	public IValidator getDefaultValidator() {
		if (diagnosticion == null) {
			diagnosticion = new Diagnostician(getValidatorRegistry());
		}
		return diagnosticion;
	}

	/**
	 * The plugin singleton
	 */
	private static Implementation plugin;

	/**
	 * The workspace root.
	 * 
	 * @see #getWorkspaceRoot
	 */
	private static IWorkspaceRoot workspaceRoot;

	/**
	 * Returns the workspace root, or <code>null</code>, if the runtime
	 * environment is stand-alone.
	 * 
	 * @return the workspace root, or <code>null</code>.
	 */
	public static IWorkspaceRoot getWorkspaceRoot() {
		return workspaceRoot;
	}

	/**
	 * A plugin implementation that handles Ecore plugin registration.
	 * 
	 * @see #startup()
	 */
	static public class Implementation extends EclipsePlugin {
		/**
		 * Creates the singleton instance.
		 */
		public Implementation() {
			plugin = this;
		}

		/**
		 * Starts up this plugin by reading some extensions and populating the
		 * relevant registries.
		 * <p>
		 * The {@link org.eclipse.emf.ecore.EPackage.Registry#INSTANCE global}
		 * package registry is populated by plugin registration of the form:
		 * 
		 * <pre>
		 *  &lt;extension point=&quot;org.eclipse.emf.ecore.generated_package&quot; &gt;
		 *      &lt;package uri=&quot;http://www.example.org/abc/Abc.ecore&quot; class=&quot;org.example.abc.AbcPackage&quot;/&gt;
		 *  &lt;extension&gt;
		 *</pre>
		 * 
		 * </p>
		 * The URI is arbitrary but an absolute URI is recommended. Provision
		 * for access to the serialized model via <code>"http:"</code> is
		 * encouraged.
		 * <p>
		 * The
		 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory.Registry#INSTANCE
		 * global} resource factory registry's
		 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory.Registry#getExtensionToFactoryMap()
		 * extension} map is populated by plugin registration of the form:
		 * 
		 * <pre>
		 *  &lt;extension point=&quot;org.eclipse.emf.ecore.extension_parser&quot;&gt;
		 *      &lt;parser type=&quot;abc&quot; class=&quot;org.example.abc.util.AbcResourceFactoryImpl&quot;/&gt;
		 *  &lt;extension&gt;
		 *</pre>
		 * 
		 * </p>
		 * <p>
		 * The
		 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory.Registry#INSTANCE
		 * global} resource factory registry's
		 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory.Registry#getProtocolToFactoryMap()
		 * protocol} map is populated by plugin registration of the form:
		 * 
		 * <pre>
		 *  &lt;extension point=&quot;org.eclipse.emf.ecore.protocol_parser&quot; &gt;
		 *      &lt;parser protocolName=&quot;abc&quot; class=&quot;org.example.abc.util.AbcResourceFactoryImpl&quot;/&gt;
		 *  &lt;extension&gt;
		 *</pre>
		 * 
		 * </p>
		 * <p>
		 * The {@link org.eclipse.emf.ecore.resource.URIConverter#URI_MAP
		 * global} URI map is populated by plugin registration of the form:
		 * 
		 * <pre>
		 *  &lt;extension point=&quot;org.eclipse.emf.ecore.uri_mapping&quot; &gt;
		 *      &lt;mapping source=&quot;//special/&quot; target=&quot;special/&quot;/&gt;
		 *  &lt;extension&gt;
		 *</pre>
		 * 
		 * If the target is relative, it is resolved against the plugin's
		 * installed location, resulting in a URI of the form:
		 * 
		 * <pre>
		 *  platform:/plugin/plugin-name_1.2.3/...
		 *</pre>
		 * 
		 * The above registration would map
		 * 
		 * <pre>
		 * // special/a/b.c
		 *</pre>
		 * 
		 * to
		 * 
		 * <pre>
		 *  platform:/plugin/plugin-name_1.2.3/special/a/b.c
		 *</pre>
		 * 
		 * </p>
		 * 
		 * @throws Exception
		 *             if there is a show stopping problem.
		 */
		@Override
		public void start(BundleContext context) throws Exception {
			super.start(context);

			if (IS_RESOURCES_BUNDLE_AVAILABLE) {
				workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			}

			IModel.Factory.Registry modelFactoryRegistry = ModelCore
					.getDefault().getModelFactoryRegistry();
			new ExtensionFactoriesRegistryReader(modelFactoryRegistry)
					.readRegistry();
			new ProtocolFactoriesRegistryReader(modelFactoryRegistry)
					.readRegistry();
			new ContentFactoriesRegistryReader(modelFactoryRegistry)
					.readRegistry();
			new ContentHandlerRegistryReader(ModelCore.getDefault()
					.getContentHandlerRegistry()).readRegistry();
			new URIMappingRegistryReader(ModelCore.getDefault().getURIMap())
					.readRegistry();
		}
	}

	/**
	 * Returns models which are registered as individual ones
	 * 
	 * @return base models
	 */
	public static Collection<ModelDescription> getBaseModels() {
		List<ModelDescription> descriptions = new ArrayList<ModelDescription>();

		IExtensionPoint extensionPoint = Platform.getExtensionRegistry()
				.getExtensionPoint(PLUGIN_ID, "models");
		if (extensionPoint != null) {
			// Loop through the config elements.
			for (IConfigurationElement configElement : extensionPoint
					.getConfigurationElements()) {
				ModelDescription modelDescription = new ModelDescription(
						configElement);

				if (modelDescription.getNamespace() == null) {
					logErrorMessage("Attribute namespace required for modelDescription: "
							+ configElement);
					continue;
				}

				descriptions.add(modelDescription);
			}
		}

		return descriptions;
	}

	public static KommaModule createModelSetModule(ClassLoader classLoader) {
		KommaModule module = new KommaModule(classLoader);

		for (Class<?> behaviourClass : ModelCore.getModelBehaviours()) {
			if (behaviourClass.isInterface()) {
				module.addConcept(behaviourClass);
			} else {
				module.addBehaviour(behaviourClass);
			}
		}

		return module;
	}

	/**
	 * Returns models which are registered as individual ones
	 * 
	 * @return base models
	 */
	public static Collection<? extends Class<?>> getModelBehaviours() {
		List<Class<?>> behaviours = new ArrayList<Class<?>>();

		if (Platform.getExtensionRegistry() != null) {
			IExtensionPoint extensionPoint = Platform.getExtensionRegistry()
					.getExtensionPoint(PLUGIN_ID, "modelBehaviours");
			if (extensionPoint != null) {
				// Loop through the config elements.
				for (IConfigurationElement configElement : extensionPoint
						.getConfigurationElements()) {
					String className = configElement.getAttribute("class");

					if (className == null) {
						logErrorMessage("Attribute class required for modelBehaviours: "
								+ configElement);
						continue;
					}

					Class<?> behaviourClass;
					try {
						if (IS_ECLIPSE_RUNNING) {
							Bundle bundle = Platform.getBundle(configElement
									.getContributor().getName());

							behaviourClass = bundle.loadClass(className);
						} else {
							behaviourClass = Class.forName(className);
						}
						behaviours.add(behaviourClass);
					} catch (ClassNotFoundException e) {
						log(e);
					}
				}
			}
		}

		return behaviours;
	}
}
