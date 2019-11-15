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

import java.util.Collection;

/**
 * A <code>BasicEList</code> that allows only {@link #isUnique unique} elements.
 */
public class UniqueExtensibleList<E> extends ExtensibleList<E> {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates an empty instance with no initial capacity.
	 */
	public UniqueExtensibleList() {
		super();
	}

	/**
	 * Creates an empty instance with the given capacity.
	 * 
	 * @param initialCapacity
	 *            the initial capacity of the list before it must grow.
	 * @exception IllegalArgumentException
	 *                if the <code>initialCapacity</code> is negative.
	 */
	public UniqueExtensibleList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates an instance that is a copy of the collection, with duplicates
	 * removed.
	 * 
	 * @param collection
	 *            the initial contents of the list.
	 */
	public UniqueExtensibleList(Collection<? extends E> collection) {
		super(collection.size());
		addAll(collection);
	}

	/**
	 * Returns <code>true</code> because this list requires uniqueness.
	 * 
	 * @return <code>true</code>.
	 */
	@Override
	protected boolean isUnique() {
		return true;
	}

	/**
	 * A <code>UniqueEList</code> that {@link #useEquals uses} <code>==</code>
	 * instead of <code>equals</code> to compare members.
	 */
	public static class FastCompare<E> extends UniqueExtensibleList<E> {
		private static final long serialVersionUID = 1L;

		/**
		 * Creates an empty instance with no initial capacity.
		 */
		public FastCompare() {
			super();
		}

		/**
		 * Creates an empty instance with the given capacity.
		 * 
		 * @param initialCapacity
		 *            the initial capacity of the list before it must grow.
		 * @exception IllegalArgumentException
		 *                if the <code>initialCapacity</code> is negative.
		 */
		public FastCompare(int initialCapacity) {
			super(initialCapacity);
		}

		/**
		 * Creates an instance that is a copy of the collection, with duplicates
		 * removed.
		 * 
		 * @param collection
		 *            the initial contents of the list.
		 */
		public FastCompare(Collection<? extends E> collection) {
			super(collection.size());
			addAll(collection);
		}

		/**
		 * Returns <code>false</code> because this list uses <code>==</code>.
		 * 
		 * @return <code>false</code>.
		 */
		@Override
		protected boolean useEquals() {
			return false;
		}
	}
}
