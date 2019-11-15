/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: Disposable.java,v 1.3 2006/12/28 06:48:53 marcelop Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.Collection;
import java.util.HashSet;

/**
 * This implements {@link IDisposable} as a set {@link IDisposable}s that can in turn be
 * disposed.
 */
public class Disposable extends HashSet<Object> implements IDisposable {
	private static final long serialVersionUID = 1L;

	/**
	 * This creates an empty instance.
	 */
	public Disposable() {
		super();
	}

	/**
	 * This creates an instance with containing all the given disposables.
	 */
	public Disposable(Collection<?> disposables) {
		super(disposables);
	}

	/**
	 * This is called to dispose the disposables.
	 */
	public void dispose() {
		for (Object object : this) {
			IDisposable disposable = (IDisposable) object;
			disposable.dispose();
		}
		clear();
	}

	@Override
	public boolean add(Object object) {
		if (object instanceof IDisposable) {
			return super.add(object);
		} else {
			return false;
		}
	}

	@Override
	public boolean addAll(Collection<?> collection) {
		boolean result = false;
		for (Object object : collection) {
			if (add(object)) {
				result = true;
			}
		}
		return result;
	}
}
