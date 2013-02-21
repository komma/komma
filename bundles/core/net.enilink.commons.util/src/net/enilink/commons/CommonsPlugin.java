/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import net.enilink.commons.extensions.RegistryFactoryHelper;
import net.enilink.commons.util.IOpener;

/**
 * The activator class controls the plug-in life cycle
 */
public class CommonsPlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.enilink.commons.util";

	// The shared instance
	private static CommonsPlugin plugin;

	/**
	 * The constructor
	 */
	public CommonsPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static CommonsPlugin getDefault() {
		return plugin;
	}

	public static boolean openResource(String openerId, Object root,
			Object resource) {
		IExtensionPoint extensionPoint = RegistryFactoryHelper.getRegistry()
				.getExtensionPoint(PLUGIN_ID, "openers");

		if (extensionPoint == null) {
			logErrorMessage("Unable to find opener. Extension point: openers not found"); //$NON-NLS-1$
			return false;
		}

		// Loop through the config elements.
		IConfigurationElement targetElement = null;
		IConfigurationElement[] configElements = extensionPoint
				.getConfigurationElements();
		for (int j = 0; j < configElements.length; j++) {
			String strId = configElements[j].getAttribute("id"); //$NON-NLS-1$
			if (openerId.equals(strId)) {
				targetElement = configElements[j];
				break;
			}
		}
		if (targetElement == null) {
			// log it since we cannot safely display a dialog.
			logErrorMessage("Unable to find opener: " + openerId); //$NON-NLS-1$
			return false;
		}

		IOpener opener;
		try {
			opener = (IOpener) targetElement.createExecutableExtension("class");

			if (opener.canOpen(root, resource)) {
				return opener.open(root, resource);
			}
		} catch (CoreException e) {
			log(e);
		}

		return false;
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, 0, message, null));
	}

	public static void logErrorStatus(String message, IStatus status) {
		if (status == null) {
			logErrorMessage(message);
			return;
		}
		MultiStatus multi = new MultiStatus(PLUGIN_ID, 0, message, null);
		multi.add(status);
		log(multi);
	}

	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, 0, "Internal error", e));
	}
}
