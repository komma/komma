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

import java.util.List;


/**
 * A list that supports move.
 */
public interface IList<E> extends List<E> {
	/**
	 * Moves the object to the new position, if is in the list.
	 * 
	 * @param newPosition
	 *            the position of the object after the move.
	 * @param object
	 *            the object to move.
	 */
	void move(int newPosition, E object);

	/**
	 * Moves the object from the old position to the new position.
	 * 
	 * @param newPosition
	 *            the position of the object after the move.
	 * @param oldPosition
	 *            the position of the object before the move.
	 * @return the moved object.
	 */
	E move(int newPosition, int oldPosition);
}
