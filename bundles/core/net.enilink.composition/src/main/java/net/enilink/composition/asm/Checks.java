/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.composition.asm;

import net.enilink.composition.exceptions.CompositionException;

/**
 * Utility methods for general assertions.
 */
public final class Checks {

	private Checks() {
	}

	/**
	 * Throws a {@link CompositionException} if the given condition is not met.
	 * 
	 * @param condition
	 *            the condition
	 * @param errorMessageFormat
	 *            the error message format
	 * @param errorMessageArgs
	 *            the error message arguments
	 */
	public static void ensure(boolean condition, String errorMessageFormat,
			Object... errorMessageArgs) {
		if (!condition) {
			throw new CompositionException(errorMessageFormat, errorMessageArgs);
		}
	}

	/**
	 * Throws a {@link CompositionException} if the given condition is not met.
	 * 
	 * @param condition
	 *            the condition
	 * @param errorMessage
	 *            the error message
	 */
	public static void ensure(boolean condition, String errorMessage) {
		if (!condition) {
			throw new CompositionException(errorMessage);
		}
	}

}