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
 * $Id: AdapterFactoryTreeIterator.java,v 1.3 2006/12/28 06:48:53 marcelop Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.Collections;
import java.util.Iterator;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.util.AbstractTreeIterator;

/**
 * This implements a tree iterator that iterates over an object, it's children,
 * their children, and so on, use the {@link ITreeItemContentProvider}s produced
 * by an adapter factory.
 */
public class AdapterFactoryTreeIterator<E> extends AbstractTreeIterator<E> {
	private static final long serialVersionUID = 1L;

	/**
	 * This keeps track of the factory used to generate the
	 * ITreeItemContentProvider adapters.
	 */
	protected IAdapterFactory adapterFactory;

	/**
	 * This constructs tree iterator that iterates over an object, it's
	 * children, their children, and so on.
	 */
	public AdapterFactoryTreeIterator(IAdapterFactory adapterFactory, E object) {
		super(object);
		this.adapterFactory = adapterFactory;
	}

	/**
	 * This constructs tree iterator that iterates over an object (but only if
	 * includeRoot is true), it's children, their children, and so on.
	 */
	public AdapterFactoryTreeIterator(IAdapterFactory adapterFactory,
			Object object, boolean includeRoot) {
		super(object, includeRoot);
		this.adapterFactory = adapterFactory;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Iterator<E> getChildren(Object o) {
		ITreeItemContentProvider treeItemContentProvider = (ITreeItemContentProvider) adapterFactory
				.adapt(o, ITreeItemContentProvider.class);
		return treeItemContentProvider != null ? (Iterator<E>) treeItemContentProvider
				.getChildren(o).iterator()
				: Collections.<E> emptyList().iterator();
	}
}
