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
package net.enilink.komma.core;

/**
 * This interface is used to gain manual control over the unit of work. This is
 * mostly to do work in non-request, non-transactional threads. Or where more
 * fine-grained control over the unit of work is required. Starting and ending a
 * unit of work directly corresponds to opening and closing an
 * {@code IEntityManager}respectively.
 * <p>
 * The Unit of Work referred to by UnitOfWork will always be local to the
 * calling thread. Be careful to end() in a finally block.
 * 
 */
public interface IUnitOfWork {

	/**
	 * Starts a Unit Of Work. Underneath, causes a session to the data layer to
	 * be opened. If there is already one open, the invocation will do nothing.
	 * In this way, you can define arbitrary units-of-work that nest within one
	 * another safely.
	 * 
	 * Transaction semantics are not affected.
	 */
	void begin();

	/**
	 * Declares an end to the current Unit of Work. Underneath, causes any open
	 * session to the data layer to close. If there is no Unit of work open,
	 * then the call returns silently. You can safely invoke end() repeatedly.
	 * <p>
	 * Transaction semantics are not affected.
	 */
	void end();
}
