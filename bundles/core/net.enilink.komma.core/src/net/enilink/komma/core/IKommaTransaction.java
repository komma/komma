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

public interface IKommaTransaction {
    /**
     * Start a resource transaction.
     * 
     * @throws IllegalStateException
     *             if isActive() is true.
     */
    public void begin();

    /**
     * Commit the current resource transaction, writing any 
     * unflushed changes to the database.  
     * 
     * @throws IllegalStateException
     *             if isActive() is false.
     * @throws RollbackException
     *             if the commit fails.
     */
    public void commit();

    /**
     * Roll back the current resource transaction. 
     * 
     * @throws IllegalStateException
     *             if isActive() is false.
     * @throws PersistenceException
     *             if an unexpected error condition is encountered.
     */
    public void rollback();

    /**
     * Mark the current resource transaction so that the only 
     * possible outcome of the transaction is for the transaction 
     * to be rolled back. 
     * 
     * @throws IllegalStateException
     *             if isActive() is false.
     */
    public void setRollbackOnly();

    /**
     * Determine whether the current resource transaction has been 
     * marked for rollback.
     * 
     * @throws IllegalStateException
     *             if isActive() is false.
     */
    public boolean getRollbackOnly();

    /**
     * Indicate whether a resource transaction is in progress.
     * 
     * @throws PersistenceException
     *             if an unexpected error condition is encountered.
     */
    public boolean isActive();
}