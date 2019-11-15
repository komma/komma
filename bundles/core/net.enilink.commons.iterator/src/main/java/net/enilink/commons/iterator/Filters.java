/*******************************************************************************
 * Copyright (c) 2009, 2015 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.iterator;

/**
 * Companion class for {@link Filter}s.
 * 
 */
public class Filters {
	private Filters() {
	}

	/**
	 * A Filter that accepts everything it's offered.
	 */
	private static final Filter<?> ANY = new Filter<Object>() {
		public final boolean accept(Object o) {
			return true;
		}
	};

	/**
	 * Create a Filter that accepts everything it's offered.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Filter<T> any() {
		return (Filter<T>) ANY;
	}
}
