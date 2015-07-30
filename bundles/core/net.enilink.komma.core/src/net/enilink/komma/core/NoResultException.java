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

import javax.management.Query;

/**
 * Thrown by the persistence provider when {@link Query#getSingleResult
 * getSingleResult()} is executed on a query and there is no result to return.
 * This exception will not cause the current transaction, if one is active, to
 * be marked for rollback.
 * 
 * @see Query#getSingleResult()
 * @see Query#getTypedSingleResult()
 * 
 * @since Java Persistence 1.0
 */
public class NoResultException extends KommaException {

	/**
	 * Constructs a new <code>NoResultException</code> exception with
	 * <code>null</code> as its detail message.
	 */
	public NoResultException() {
		super();
	}

	/**
	 * Constructs a new <code>NoResultException</code> exception with the
	 * specified detail message.
	 * 
	 * @param message
	 *            the detail message.
	 */
	public NoResultException(String message) {
		super(message);
	}
}
