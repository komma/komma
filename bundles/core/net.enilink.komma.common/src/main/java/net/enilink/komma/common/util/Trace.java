/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.common.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;

/**
 * A utility for tracing debug information. Provides a simple interface for
 * filtering and generating trace output.
 * 
 * @author khussey
 * @canBeSeenBy %partners
 */
public class Trace {

	/**
	 * private constructor for the static class.
	 */
	private Trace() {
		super();
	}

	/**
	 * String containing an open parenthesis.
	 * 
	 */
	protected static final String PARENTHESIS_OPEN = "("; //$NON-NLS-1$

	/**
	 * String containing a close parenthesis.
	 * 
	 */
	protected static final String PARENTHESIS_CLOSE = ")"; //$NON-NLS-1$

	/**
	 * Prefix for tracing the changing of values.
	 * 
	 */
	protected static final String PREFIX_CHANGING = "CHANGING "; //$NON-NLS-1$

	/**
	 * Prefix for tracing the catching of throwables.
	 * 
	 */
	protected static final String PREFIX_CATCHING = "CAUGHT "; //$NON-NLS-1$

	/**
	 * Prefix for tracing the throwing of throwables.
	 * 
	 */
	protected static final String PREFIX_THROWING = "THROWN "; //$NON-NLS-1$

	/**
	 * Prefix for tracing the entering of methods.
	 * 
	 */
	protected static final String PREFIX_ENTERING = "ENTERING "; //$NON-NLS-1$

	/**
	 * Prefix for tracing the exiting of methods.
	 * 
	 */
	protected static final String PREFIX_EXITING = "EXITING "; //$NON-NLS-1$

	/**
	 * Separator for methods.
	 * 
	 */
	protected static final String SEPARATOR_METHOD = "#"; //$NON-NLS-1$

	/**
	 * Separator for parameters.
	 * 
	 */
	protected static final String SEPARATOR_PARAMETER = ", "; //$NON-NLS-1$

	/**
	 * Separator for return values.
	 * 
	 */
	protected static final String SEPARATOR_RETURN = ":"; //$NON-NLS-1$

	/**
	 * Separator containing a space.
	 * 
	 */
	protected static final String SEPARATOR_SPACE = " "; //$NON-NLS-1$

	/**
	 * Label indicating old value.
	 * 
	 */
	protected static final String LABEL_OLD_VALUE = "old="; //$NON-NLS-1$

	/**
	 * Label indicating new value.
	 * 
	 */
	protected static final String LABEL_NEW_VALUE = "new="; //$NON-NLS-1$

	/**
	 * The cached debug options (for optimization).
	 */
	private static final Map<String, Boolean> cachedOptions = new HashMap<String, Boolean>();

	/**
	 * Retrieves a Boolean value indicating whether tracing is enabled for the
	 * specified plug-in.
	 * 
	 * @return Whether tracing is enabled for the plug-in.
	 * @param plugin
	 *            The plug-in for which to determine trace enablement.
	 * 
	 */
	protected static boolean shouldTrace(Plugin plugin) {
		return plugin != null && plugin.isDebugging();
	}

	/**
	 * Retrieves a Boolean value indicating whether tracing is enabled for the
	 * specified debug option of the specified plug-in.
	 * 
	 * @return Whether tracing is enabled for the debug option of the plug-in.
	 * @param plugin
	 *            The plug-in for which to determine trace enablement.
	 * @param option
	 *            The debug option for which to determine trace enablement.
	 * 
	 */
	public static boolean shouldTrace(Plugin plugin, String option) {
		if (shouldTrace(plugin)) {
			Boolean value = null;

			synchronized (cachedOptions) {
				value = cachedOptions.get(option);

				if (null == value) {
					value = Boolean.valueOf(Platform.getDebugOption(option));

					cachedOptions.put(option, value);
				}
			}

			return value.booleanValue();
		}

		return false;
	}

	/**
	 * Retrieves a textual representation of the specified argument.
	 * 
	 * @return A textual representation of the specified argument.
	 * @param argument
	 *            The argument for which to retrieve a textual representation.
	 * 
	 */
	protected static String getArgumentString(Object argument) {
		return String.valueOf(argument);
	}

	/**
	 * Retrieves a textual representation of the specified arguments.
	 * 
	 * @return A textual representation of the specified arguments.
	 * @param arguments
	 *            The arguments for which to retrieve a textual representation.
	 * 
	 */
	protected static String getArgumentsString(Object[] arguments) {
		StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < arguments.length; i++) {
			buffer.append(getArgumentString(arguments[i]));

			if (i < arguments.length - 1) {
				buffer.append(SEPARATOR_PARAMETER);
			}
		}

		return buffer.toString();
	}

	/**
	 * Traces the specified message.
	 * 
	 * @param message
	 *            The message to be traced.
	 * 
	 */
	protected static void trace(String message) {
		System.out.println(message);
	}

	/**
	 * Traces the specified message from the specified plug-in.
	 * 
	 * @param plugin
	 *            The plug-in from which to trace.
	 * @param message
	 *            The message to be traced.
	 * 
	 */
	public static void trace(Plugin plugin, String message) {
		if (shouldTrace(plugin)) {
			trace(message);
		}
	}

	/**
	 * Traces the specified message from the specified plug-in for the specified
	 * debug option.
	 * 
	 * @param plugin
	 *            The plug-in from which to trace.
	 * @param option
	 *            The debug option for which to trace.
	 * @param message
	 *            The message to be traced.
	 * 
	 */
	public static void trace(Plugin plugin, String option, String message) {
		if (shouldTrace(plugin, option)) {
			trace(message);
		}
	}

	/**
	 * Traces the changing of a value.
	 * 
	 * @param plugin
	 *            The plug-in from which to trace.
	 * @param option
	 *            The debug option for which to trace.
	 * @param valueDescription
	 *            The description of the value which is changing.
	 * @param oldValue
	 *            The old value.
	 * @param newValue
	 *            The new value.
	 */
	public static void changing(Plugin plugin, String option,
			String valueDescription, Object oldValue, Object newValue) {

		if (shouldTrace(plugin, option)) {
			trace(PREFIX_CHANGING + valueDescription + SEPARATOR_SPACE
					+ LABEL_OLD_VALUE + getArgumentString(oldValue)
					+ SEPARATOR_PARAMETER + LABEL_NEW_VALUE
					+ getArgumentString(newValue));
		}
	}

	/**
	 * 
	 * @param plugin
	 *            The plug-in from which to trace.
	 * @param option
	 *            The debug option for which to trace.
	 * @param clazz
	 *            The class in which the value is changing.
	 * @param methodName
	 *            The name of the method in which the value is changing.
	 * @param valueDescription
	 *            The description of the value which is changing.
	 * @param oldValue
	 *            The old value.
	 * @param newValue
	 *            The new value.
	 */
	public static void changing(Plugin plugin, String option,  Class<?> clazz,
			String methodName, String valueDescription, Object oldValue,
			Object newValue) {

		if (shouldTrace(plugin, option)) {
			trace(PREFIX_CHANGING + valueDescription + SEPARATOR_SPACE
					+ LABEL_OLD_VALUE + getArgumentString(oldValue)
					+ SEPARATOR_PARAMETER + LABEL_NEW_VALUE
					+ getArgumentString(newValue) + SEPARATOR_SPACE
					+ PARENTHESIS_OPEN + clazz.getName() + SEPARATOR_METHOD
					+ methodName + PARENTHESIS_CLOSE);
		}
	}

	/**
	 * Traces the catching of the specified throwable in the specified method of
	 * the specified class.
	 * 
	 * @param plugin
	 *            The plug-in from which to trace.
	 * @param option
	 *            The debug option for which to trace.
	 * @param clazz
	 *            The class in which the throwable is being caught.
	 * @param methodName
	 *            The name of the method in which the throwable is being caught.
	 * @param throwable
	 *            The throwable that is being caught.
	 * 
	 */
	public static void catching(Plugin plugin, String option, Class<?> clazz,
			String methodName, Throwable throwable) {
		if (shouldTrace(plugin, option)) {
			trace(PREFIX_CATCHING + throwable.getMessage() + SEPARATOR_SPACE
					+ PARENTHESIS_OPEN + clazz.getName() + SEPARATOR_METHOD
					+ methodName + PARENTHESIS_CLOSE);

			throwable.printStackTrace(System.err);
		}
	}

	/**
	 * Traces the throwing of the specified throwable from the specified method
	 * of the specified class.
	 * 
	 * @param plugin
	 *            The plug-in from which to trace.
	 * @param option
	 *            The debug option for which to trace.
	 * @param clazz
	 *            The class from which the throwable is being thrown.
	 * @param methodName
	 *            The name of the method from which the throwable is being
	 *            thrown.
	 * @param throwable
	 *            The throwable that is being thrown.
	 * 
	 */
	public static void throwing(Plugin plugin, String option,  Class<?> clazz,
			String methodName, Throwable throwable) {

		if (shouldTrace(plugin, option)) {

			trace(PREFIX_THROWING + throwable.getMessage() + SEPARATOR_SPACE
					+ PARENTHESIS_OPEN + clazz.getName() + SEPARATOR_METHOD
					+ methodName + PARENTHESIS_CLOSE);

			throwable.printStackTrace(System.err);
		}
	}

	/**
	 * Traces the entering into the specified method of the specified class.
	 * 
	 * @param plugin
	 *            The plug-in from which to trace.
	 * @param option
	 *            The debug option for which to trace.
	 * @param clazz
	 *            The class whose method is being entered.
	 * @param methodName
	 *            The name of method that is being entered.
	 * 
	 */
	public static void entering(Plugin plugin, String option,  Class<?> clazz,
			String methodName) {
		if (shouldTrace(plugin, option)) {
			trace(PREFIX_ENTERING + clazz.getName() + SEPARATOR_METHOD
					+ methodName);
		}
	}

	/**
	 * Traces the entering into the specified method of the specified class,
	 * with the specified parameter.
	 * 
	 * @param plugin
	 *            The plug-in from which to trace.
	 * @param option
	 *            The debug option for which to trace.
	 * @param clazz
	 *            The class whose method is being entered.
	 * @param methodName
	 *            The name of method that is being entered.
	 * @param parameter
	 *            The parameter to the method being entered.
	 * 
	 */
	public static void entering(Plugin plugin, String option,  Class<?> clazz,
			String methodName, Object parameter) {
		if (shouldTrace(plugin, option)) {
			trace(PREFIX_ENTERING + clazz.getName() + SEPARATOR_METHOD
					+ methodName + PARENTHESIS_OPEN
					+ getArgumentString(parameter) + PARENTHESIS_CLOSE);
		}
	}

	/**
	 * Traces the entering into the specified method of the specified class,
	 * with the specified parameters.
	 * 
	 * @param plugin
	 *            The plug-in from which to trace.
	 * @param option
	 *            The debug option for which to trace.
	 * @param clazz
	 *            The class whose method is being entered.
	 * @param methodName
	 *            The name of method that is being entered.
	 * @param parameters
	 *            The parameters to the method being entered.
	 * 
	 */
	public static void entering(Plugin plugin, String option,  Class<?> clazz,
			String methodName, Object[] parameters) {
		if (shouldTrace(plugin, option)) {

			trace(PREFIX_ENTERING + clazz.getName() + SEPARATOR_METHOD
					+ methodName + PARENTHESIS_OPEN
					+ getArgumentsString(parameters) + PARENTHESIS_CLOSE);
		}
	}

	/**
	 * Traces the exiting from the specified method of the specified class.
	 * 
	 * @param plugin
	 *            The plug-in from which to trace.
	 * @param option
	 *            The debug option for which to trace.
	 * @param clazz
	 *            The class whose method is being exited.
	 * @param methodName
	 *            The name of method that is being exited.
	 * 
	 */
	public static void exiting(Plugin plugin, String option,  Class<?> clazz,
			String methodName) {
		if (shouldTrace(plugin, option)) {

			trace(PREFIX_EXITING + clazz.getName() + SEPARATOR_METHOD
					+ methodName);
		}
	}

	/**
	 * Traces the exiting from the specified method of the specified class, with
	 * the specified return value.
	 * 
	 * @param plugin
	 *            The plug-in from which to trace.
	 * @param option
	 *            The debug option for which to trace.
	 * @param clazz
	 *            The class whose method is being exited.
	 * @param methodName
	 *            The name of method that is being exited.
	 * @param returnValue
	 *            The return value of the method being exited.
	 * 
	 */
	public static void exiting(Plugin plugin, String option,  Class<?> clazz,
			String methodName, Object returnValue) {

		if (shouldTrace(plugin, option)) {

			trace(PREFIX_EXITING + clazz.getName() + SEPARATOR_METHOD
					+ methodName + SEPARATOR_RETURN
					+ getArgumentString(returnValue));
		}
	}

}
