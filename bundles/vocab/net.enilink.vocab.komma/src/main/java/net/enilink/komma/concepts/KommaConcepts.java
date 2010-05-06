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
package net.enilink.komma.concepts;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.common.util.IResourceLocator;

/**
 * The activator class controls the plug-in life cycle
 */
public class KommaConcepts extends AbstractKommaPlugin {
	// The plug-in ID
	public static final String PLUGIN_ID = "net.enilink.komma.concepts";

	private static final KommaConcepts INSTANCE = new KommaConcepts();

	/**
	 * The constructor
	 */
	public KommaConcepts() {
		super(new IResourceLocator[] {});
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static KommaConcepts getDefault() {
		return INSTANCE;
	}

	public static void logErrorMessage(String message) {
		getDefault()
				.log(new Status(IStatus.ERROR, PLUGIN_ID, 0, message, null));
	}

	public static void logErrorStatus(String message, IStatus status) {
		if (status == null) {
			logErrorMessage(message);
			return;
		}
		MultiStatus multi = new MultiStatus(PLUGIN_ID, 0, message, null);
		multi.add(status);
		getDefault().log(multi);
	}

	public static void log(Throwable e) {
		getDefault().log(
				new Status(IStatus.ERROR, PLUGIN_ID, 0, "Internal error", e));
	}

	@Override
	public IResourceLocator getBundleResourceLocator() {
		return plugin;
	}

	/**
	 * The plugin singleton
	 */
	private static Implementation plugin;

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
		 * {@link org.eclipse.emf.ecore.resource.Resource.Factory.Registry#INSTANCE
		 * global} resource factory registry's
		 * {@link org.eclipse.emf.ecore.resource.Resource.Factory.Registry#getExtensionToFactoryMap()
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
		 * {@link org.eclipse.emf.ecore.resource.Resource.Factory.Registry#INSTANCE
		 * global} resource factory registry's
		 * {@link org.eclipse.emf.ecore.resource.Resource.Factory.Registry#getProtocolToFactoryMap()
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

		}
	}
}
