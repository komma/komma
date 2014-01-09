/*******************************************************************************
 * Copyright (c) 2010 Angelo Zerr and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.util.internal.extensions;

import net.enilink.commons.util.CommonsUtilPlugin;

/**
 * Helper to debug None OSGi-env registry.
 */
public final class DebugHelper {

	private static final String OPTION_DEBUG = "net.enilink.commons.extensions.debug"; //$NON-NLS-1$

	public static boolean DEBUG = "true".equals(System
			.getProperty(OPTION_DEBUG));

	/**
	 * Log message.
	 * 
	 * @param message
	 */
	public static void log(String message) {
		log(message, 0);
	}

	/**
	 * Log message with indent.
	 * 
	 * @param message
	 * @param indent
	 */
	public static void log(String message, int indent) {
		System.out.println(createMessage(message, indent));
	}

	/**
	 * Log error.
	 * 
	 * @param error
	 */
	public static void logError(String message) {
		logError(message, 0);
	}

	/**
	 * Log error with indent.
	 * 
	 * @param message
	 * @param indent
	 */
	public static void logError(String message, int indent) {
		System.err.println(createMessage(message, indent));
	}

	/**
	 * Log error exception.
	 * 
	 * @param e
	 */
	public static void logError(Throwable e) {
		e.printStackTrace(System.err);
		System.err.println();
	}

	/**
	 * Create message.
	 * 
	 * @param message
	 * @param indent
	 * @return
	 */
	private static String createMessage(String message, int indent) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			s.append("\t");
		}
		s.append("[");
		s.append(CommonsUtilPlugin.PLUGIN_ID);
		s.append("] ");
		if (message != null) {
			s.append(message);
		}
		return s.toString();
	}
}
