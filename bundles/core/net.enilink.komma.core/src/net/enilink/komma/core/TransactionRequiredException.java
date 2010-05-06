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
package net.enilink.komma.core;

/**
 * Thrown by the persistence provider when a transaction is required but is not
 * active.
 * 
 * @since Java Persistence 1.0
 */
public class TransactionRequiredException extends KommaException {

	/**
	 * Constructs a new <code>TransactionRequiredException</code> exception with
	 * <code>null</code> as its detail message.
	 */
	public TransactionRequiredException() {
		super();
	}

	/**
	 * Constructs a new <code>TransactionRequiredException</code> exception with
	 * the specified detail message.
	 * 
	 * @param message
	 *            the detail message.
	 */
	public TransactionRequiredException(String message) {
		super(message);
	}
}
