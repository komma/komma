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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
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

	private static final String LOGBACK_CONFIG_FILE = "logback.xml";

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

	public static synchronized void init() {
		if (!initialized) {
			if (LoggerFactory.getILoggerFactory() instanceof LoggerContext) {
				// SLF4J is bound to logback in the current environment
				LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
				loadConfiguration(context);
				StatusPrinter.printInCaseOfErrorsOrWarnings(context);
				if (isTrue("net.enilink.logger.captureSystemOut")) {
					redirectStdoutToLogInfo();
					redirectStdoutToLogError();
				}
			}
			initialized = true;
		}
	}

	private static void loadConfiguration(LoggerContext context) {
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			// Call context.reset() to clear any previous configuration,
			// e.g. default configuration. For multi-step configuration,
			// omit calling context.reset().
			context.reset();

			InputStream config = getLoggingFileFromWorkspace();
			if (config == null) {
				config = getLoggingFileFromInstallationPath();
			}
			if (config == null) {
				config = getLoggingFileFromClasspath();
			}
			if (config != null) {
				configurator.doConfigure(config);
			}

		} catch (JoranException je) {
			// StatusPrinter will handle this
		}
	}

	private static boolean isTrue(String property) {
		return "true".equals(System.getProperty(property));
	}

	private static void redirectStdoutToLogError() {
		System.setErr(new PrintStream(System.err) {
			private Logger log = LoggerFactory.getLogger(System.class);

			public void print(String message) {
				log.error(message);
			}
		});
	}

	private static void redirectStdoutToLogInfo() {
		System.setOut(new PrintStream(System.out) {
			private Logger log = LoggerFactory.getLogger(System.class);

			public void print(String message) {
				log.info(message);
			}
		});
	}

	private static InputStream getLoggingFileFromClasspath() {
		return LoggingPlugin.class.getResourceAsStream(LOGBACK_CONFIG_FILE);
	}

	private static InputStream getLoggingFileFromWorkspace() {
		if (Platform.isRunning()) {
			String path = Platform.getInstanceLocation().getURL().getPath() + LOGBACK_CONFIG_FILE;
			try {
				return new FileInputStream(path);
			} catch (FileNotFoundException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	private static InputStream getLoggingFileFromInstallationPath() {
		if (Platform.isRunning()) {
			String path = Platform.getInstallLocation().getURL().getPath() + LOGBACK_CONFIG_FILE;
			try {
				return new FileInputStream(path);
			} catch (FileNotFoundException e) {
				return null;
			}
		} else {
			return null;
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
