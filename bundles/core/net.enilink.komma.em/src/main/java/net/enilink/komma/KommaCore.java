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
package net.enilink.komma;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.internal.Messages;

/**
 * The activator class controls the plug-in life cycle
 */
public class KommaCore extends AbstractKommaPlugin {
	// The plug-in ID
	public static final String PLUGIN_ID = "net.enilink.komma";

	private static final KommaCore INSTANCE = new KommaCore();

	/**
	 * The constructor
	 */
	public KommaCore() {
		super(new IResourceLocator[] {});
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static KommaCore getDefault() {
		return INSTANCE;
	}

	public static void logErrorMessage(String message) {
		getDefault().log(
				new Status(IStatus.ERROR, PLUGIN_ID,
						IKommaStatusConstants.INTERNAL_ERROR, message, null));
	}

	public static void logErrorStatus(String message, IStatus status) {
		if (status == null) {
			logErrorMessage(message);
			return;
		}
		MultiStatus multi = new MultiStatus(PLUGIN_ID,
				IKommaStatusConstants.INTERNAL_ERROR, message, null);
		multi.add(status);
		getDefault().log(multi);
	}

	public static void log(Throwable e) {
		getDefault().log(
				new Status(IStatus.ERROR, PLUGIN_ID,
						IKommaStatusConstants.INTERNAL_ERROR,
						Messages.KommaCore_internal_error, e));
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
	}
}
