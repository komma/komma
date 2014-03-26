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

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * The activator class controls the plug-in life cycle
 */
public class LoggingPlugin extends Plugin {
	static {
		System.setProperty("org.jboss.logging.provider", "slf4j");
	}

	// The plug-in ID
	public static final String PLUGIN_ID = "net.enilink.commons.logging";

	// The shared instance
	private static LoggingPlugin plugin;

	private static boolean initialized = false;

	/**
	 * The constructor
	 */
	public LoggingPlugin() {
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		init();
	}

	public static void init() {
		if (!initialized) {

			// assume SLF4J is bound to logback in the current environment
			LoggerContext context = (LoggerContext) LoggerFactory
					.getILoggerFactory();

			try {
				JoranConfigurator configurator = new JoranConfigurator();
				configurator.setContext(context);
				// Call context.reset() to clear any previous configuration,
				// e.g. default configuration. For multi-step configuration,
				// omit calling context.reset().
				context.reset();

				String fileName = "logback.xml";
				// try to load config file from workspace; otherwise the default
				// from this plugin will be used
				Location instanceLocation = Platform.isRunning() ? Platform
						.getInstanceLocation() : null;
				File configFile = null;
				if (instanceLocation != null) {
					configFile = new File(instanceLocation.getURL().getPath()
							+ fileName);
				}
				if (configFile != null && configFile.exists()) {
					// use file from workspace
					configurator.doConfigure(configFile);
				} else {
					// use file from classpath
					configurator.doConfigure(LoggingPlugin.class
							.getResource(fileName));
				}
			} catch (JoranException je) {
				// StatusPrinter will handle this
			}
			StatusPrinter.printInCaseOfErrorsOrWarnings(context);
			if ("true"
					.equals(System
							.getProperty("net.enilink.commons.logging.captureSystemOut"))) {
				// redirect system output output to log.info()
				System.setOut(new PrintStream(System.out) {
					private Logger log = LoggerFactory.getLogger(System.class);

					public void print(String message) {
						log.info(message);
					}
				});

				// redirect system error output to log.error()
				System.setErr(new PrintStream(System.err) {
					private Logger log = LoggerFactory.getLogger(System.class);

					public void print(String message) {
						log.error(message);
					}
				});
			}
			initialized = true;
		}
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static LoggingPlugin getDefault() {
		return plugin;
	}
}
