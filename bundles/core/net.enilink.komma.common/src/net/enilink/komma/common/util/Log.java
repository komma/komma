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
package net.enilink.komma.common.util;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

import net.enilink.komma.common.CommonPlugin;
import net.enilink.komma.common.internal.CommonDebugOptions;
import net.enilink.komma.common.internal.CommonStatusCodes;

/**
 * A utility for logging errors, warnings, and information. Provides a simple
 * interface for generating logs based on status objects.
 * 
 * @author khussey
 * @canBeSeenBy %partners
 */
public class Log {

	/**
	 * private constructor for the static class.
	 */
	protected Log() {
		super();
	}

	/**
	 * The default log listener for this log utility.
	 */
	private static ILog log;
	static {
		if (CommonPlugin.getPlugin() != null) {
			log = CommonPlugin.getPlugin().getLog();
		} else {
			log = new ILog() {
				public void addLogListener(ILogListener listener) {
				}

				public void log(final IStatus status) {
					System.out.println(status);
				}

				public void removeLogListener(ILogListener listener) {
				}

				@Override
				public Bundle getBundle() {
					return null;
				}
			};
		}
	}

	/**
	 * Retrieves the default log listener for this log utility.
	 * 
	 * @return The default log listener for this log utility.
	 */
	protected static ILog getDefault() {
		return log;
	}

	/**
	 * Responds to a log request for the specified plug-in based on the
	 * specified status object. Statuses with severity of error or warning
	 * result in the generation of a platform log for the specified plug-in; all
	 * log requests are forward to the default log listener.
	 * 
	 * @param plugin
	 *            The plug-in for which to generate a log.
	 * @param status
	 *            The status object on which to base the log.
	 * 
	 */
	public static void log(Plugin plugin, IStatus status) {
		switch (status.getSeverity()) {
		case IStatus.ERROR:
		case IStatus.WARNING:
			getDefault().log(status);
			break;
		default:
			/*
			 * The Eclipse logging facility does not have the concept of logging
			 * levels. Anything logged to the plugins logger will automatically
			 * be logged to the .log file. The Common Logging implementation
			 * provides the common log file (CommonBaseEvents.log) with the
			 * associated extension point and preference page but is not related
			 * to the .log file. Since we do not want to Eclipse log poluted
			 * with "plug-in started" messages, we do not log these.
			 * 
			 * getDefault().log(status);
			 */
			break;
		}
	}

	/**
	 * Generates a log for the specified plug-in, with the specified severity,
	 * status code, and message.
	 * 
	 * @param plugin
	 *            The plug-in for which to generate a log.
	 * @param severity
	 *            The severity of the log.
	 * @param code
	 *            The status code for the log.
	 * @param message
	 *            The message for the log.
	 * 
	 */
	public static void log(Plugin plugin, int severity, int code, String message) {
		log(plugin, severity, code, message, null);
	}

	/**
	 * Generates a log for the specified plug-in, with the specified severity,
	 * status code, message, and throwable.
	 * 
	 * @param plugin
	 *            The plug-in for which to generate a log.
	 * @param severity
	 *            The severity of the log.
	 * @param code
	 *            The status code for the log.
	 * @param message
	 *            The message for the log.
	 * @param throwable
	 *            The throwable for the log.
	 * 
	 */
	public static void log(Plugin plugin, int severity, int code,
			String message, Throwable throwable) {
		//
		// Status ctor requires a non-null message
		String msg = message == null ? "" //$NON-NLS-1$
				: message;

		try {
			log(plugin, new Status(severity, plugin.getBundle()
					.getSymbolicName(), code, msg, throwable));
		} catch (IllegalArgumentException iae) {
			Trace.catching(CommonPlugin.getPlugin(),
					CommonDebugOptions.EXCEPTIONS_CATCHING, Log.getDefault()
							.getClass(), "log", iae); //$NON-NLS-1$
			Log.error(CommonPlugin.getPlugin(),
					CommonStatusCodes.SERVICE_FAILURE, "log", iae); //$NON-NLS-1$
		}
	}

	/**
	 * Generates an error log for the specified plug-in, with the specified
	 * status code and message.
	 * 
	 * @param plugin
	 *            The plug-in for which to generate a log.
	 * @param code
	 *            The status code for the log.
	 * @param message
	 *            The message for the log.
	 * 
	 */
	public static void error(Plugin plugin, int code, String message) {
		error(plugin, code, message, null);
	}

	/**
	 * Generates an error log for the specified plug-in, with the specified
	 * status code, message, and throwable.
	 * 
	 * @param plugin
	 *            The plug-in for which to generate a log.
	 * @param code
	 *            The status code for the log.
	 * @param message
	 *            The message for the log.
	 * @param throwable
	 *            The throwable for the log.
	 * 
	 */
	public static void error(Plugin plugin, int code, String message,
			Throwable throwable) {
		log(plugin, IStatus.ERROR, code, message, throwable);
	}

	/**
	 * Generates a warning log for the specified plug-in, with the specified
	 * status code and message.
	 * 
	 * @param plugin
	 *            The plug-in for which to generate a log.
	 * @param code
	 *            The status code for the log.
	 * @param message
	 *            The message for the log.
	 * 
	 */
	public static void warning(Plugin plugin, int code, String message) {
		warning(plugin, code, message, null);
	}

	/**
	 * Generates a warning log for the specified plug-in, with the specified
	 * status code, message, and throwable.
	 * 
	 * @param plugin
	 *            The plug-in for which to generate a log.
	 * @param code
	 *            The status code for the log.
	 * @param message
	 *            The message for the log.
	 * @param throwable
	 *            The throwable for the log.
	 * 
	 */
	public static void warning(Plugin plugin, int code, String message,
			Throwable throwable) {

		log(plugin, IStatus.WARNING, code, message, throwable);
	}

	/**
	 * Generates an information log for the specified plug-in, with the
	 * specified status code and message.
	 * 
	 * @param plugin
	 *            The plug-in for which to generate a log.
	 * @param code
	 *            The status code for the log.
	 * @param message
	 *            The message for the log.
	 * 
	 */
	public static void info(Plugin plugin, int code, String message) {
		info(plugin, code, message, null);
	}

	/**
	 * Generates an information log for the specified plug-in, with the
	 * specified status code, message, and throwable.
	 * 
	 * @param plugin
	 *            The plug-in for which to generate a log.
	 * @param code
	 *            The status code for the log.
	 * @param message
	 *            The message for the log.
	 * @param throwable
	 *            The throwable for the log.
	 * 
	 */
	public static void info(Plugin plugin, int code, String message,
			Throwable throwable) {

		log(plugin, IStatus.INFO, code, message, throwable);
	}
}