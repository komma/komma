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
package net.enilink.komma.common.internal;

/**
 * A list of status codes for this plug-in.
 * 
 * @author khussey
 * 
 */
public final class CommonStatusCodes {

	/**
	 * This class should not be instantiated since it is a static constant
	 * class.
	 * 
	 */
	private CommonStatusCodes() {
		/* private constructor */
	}

	/**
	 * Status code indicating that everything is OK.
	 */
	public static final int OK = 0;

	/**
	 * Status code indicating that an error occurred during plug-in start-up.
	 */
	public static final int PLUGIN_STARTUP_FAILURE = 1;

	/**
	 * Status code indicating that an error occurred during plug-in shut-down.
	 */
	public static final int PLUGIN_SHUTDOWN_FAILURE = 2;

	/**
	 * Status code indicating that an error occurred with internationalization.
	 */
	public static final int L10N_FAILURE = 3;

	/**
	 * Status code indicating that an error occurred with a command.
	 */
	public static final int COMMAND_FAILURE = 4;

	/**
	 * Status code indicating that an error occurred with a service.
	 */
	public static final int SERVICE_FAILURE = 5;

	/**
	 * Status code indicating that an operation was cancelled.
	 */
	public static final int CANCELLED = 6;

	/**
	 * Status code indicating that an operation was rolled back due to live
	 * validation errors.
	 */
	public static final int VALIDATION_FAILURE = 7;

	/**
	 * Encoding failure.
	 */
	public static final int ENCODING_FAILURE = 8;

	/**
	 * Error status code indicating that the recovery of a failed execution also
	 * failed.
	 */
	public static final int EXECUTE_RECOVERY_FAILED = 20;

	/**
	 * Error status code indicating that the recovery of a failed undo also
	 * failed.
	 */
	public static final int UNDO_RECOVERY_FAILED = 21;

	/**
	 * Error status code indicating that the recovery of a failed redo also
	 * failed.
	 */
	public static final int REDO_RECOVERY_FAILED = 22;

	/**
	 * Error status code indicating that an unexpected error occurred.
	 */
	public static final int INTERNAL_ERROR = 10001;

}
