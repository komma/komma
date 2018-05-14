/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 */
package net.enilink.komma.edit.domain;

/**
 * This is the interface specified by an object that is able to yield its
 * {@link IEditingDomain}. See
 * {@link AdapterFactoryEditingDomain#getEditingDomainFor(Object)
 * AdapterFactoryEditingDomain.getEditingDomainFor} for one use of this.
 */
public interface IEditingDomainProvider {
	/**
	 * This returns the editing domain.
	 */
	public IEditingDomain getEditingDomain();
}
