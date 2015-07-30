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
 * Thrown by the persistence provider when the 
 * {@link EntityTransaction#commit() EntityTransaction.commit()} fails.
 *
 * @see javax.persistence.EntityTransaction#commit()
 *
 * @since Java Persistence 1.0
 */
public class RollbackException extends KommaException {
	
        /** 
         * Constructs a new <code>RollbackException</code> exception 
         * with <code>null</code> as its detail message.
         */
	public RollbackException() {
		super();
	}

        /** 
         * Constructs a new <code>RollbackException</code> exception 
         * with the specified detail message.
         * @param   message   the detail message.
         */
	public RollbackException(String message) {
		super(message);
	}

        /** 
         * Constructs a new <code>RollbackException</code> exception 
         * with the specified detail message and cause.
         * @param   message   the detail message.
         * @param   cause     the cause.
         */
	public RollbackException(String message, Throwable cause) {
		super(message, cause);
	}
	
        /** 
         * Constructs a new <code>RollbackException</code> exception 
         * with the specified cause.
         * @param   cause     the cause.
         */
	public RollbackException(Throwable cause) {
		super(cause);
	}
}


