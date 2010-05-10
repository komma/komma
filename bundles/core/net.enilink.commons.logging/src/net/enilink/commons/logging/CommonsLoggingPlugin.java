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
package net.enilink.commons.logging;

import java.io.File;
import java.io.PrintStream;

import org.apache.log4j.PropertyConfigurator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class CommonsLoggingPlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.enilink.commons.logging";

	// The shared instance
	private static CommonsLoggingPlugin plugin;

	/**
	 * The constructor
	 */
	public CommonsLoggingPlugin() {
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

		setupLoggingSystem();
	}

	private void setupLoggingSystem() {
		// try to load log config from workspace;
		// otherwise the default from this plugin will be used
		Location instanceLocation = Platform.getInstanceLocation();
		if (instanceLocation != null) {
			File file = new File(instanceLocation.getURL().getPath()
					+ "log4j.properties");
			if (file.exists()) {
				PropertyConfigurator.configureAndWatch(file.getAbsolutePath(),
						30000);
			} else {
				System.out
						.println("no log4j.properties found in workspace; using default from classpath");
				System.out.println("workspace location: "
						+ file.getAbsolutePath());
			}
		}

		// redirect system output output to log.info()
		PrintStream printStreamSystemOut = new PrintStream(System.out) {
			private Logger log = LoggerFactory.getLogger(System.class);

			public void print(String message) {
				log.info(message);
			}
		};

		System.setOut(printStreamSystemOut);

		// redirect system error output to log.error()
		PrintStream printStreamSystemErr = new PrintStream(System.err) {
			private Logger log = LoggerFactory.getLogger(System.class);

			public void print(String message) {
				log.error(message);
			}
		};

		System.setErr(printStreamSystemErr);
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
	public static CommonsLoggingPlugin getDefault() {
		return plugin;
	}
}
