/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.core;

public interface ITransaction {
	/**
	 * Start a resource transaction.
	 * 
	 * @throws IllegalStateException
	 *             if isActive() is true.
	 */
	void begin();

	/**
	 * Commit the current resource transaction, writing any unflushed changes to
	 * the database.
	 * 
	 * @throws IllegalStateException
	 *             if isActive() is false.
	 * @throws RollbackException
	 *             if the commit fails.
	 */
	void commit();

	/**
	 * Roll back the current resource transaction.
	 * 
	 * @throws IllegalStateException
	 *             if isActive() is false.
	 * @throws PersistenceException
	 *             if an unexpected error condition is encountered.
	 */
	void rollback();

	/**
	 * Mark the current resource transaction so that the only possible outcome
	 * of the transaction is for the transaction to be rolled back.
	 * 
	 * @throws IllegalStateException
	 *             if isActive() is false.
	 */
	void setRollbackOnly();

	/**
	 * Determine whether the current resource transaction has been marked for
	 * rollback.
	 * 
	 * @throws IllegalStateException
	 *             if isActive() is false.
	 */
	boolean getRollbackOnly();

	/**
	 * Indicate whether a resource transaction is in progress.
	 * 
	 * @throws PersistenceException
	 *             if an unexpected error condition is encountered.
	 */
	boolean isActive();
}