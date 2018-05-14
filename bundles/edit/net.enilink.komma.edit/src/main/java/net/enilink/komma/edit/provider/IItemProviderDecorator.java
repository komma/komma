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
 * $Id: IItemProviderDecorator.java,v 1.2 2005/06/08 06:17:05 nickb Exp $
 */
package net.enilink.komma.edit.provider;

import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotifier;

/**
 * This is implemented by an item provider that decorates another item provider.
 */
public interface IItemProviderDecorator {
	/**
	 * This returns the decorated item provider, which must implement the
	 * {@link org.eclipse.emf.edit.provider.IChangeNotifier} interface.
	 */
	INotifier<INotification> getDecoratedItemProvider();

	/**
	 * This set the decorated item provider, which must implement the
	 * {@link org.eclipse.emf.edit.provider.IChangeNotifier} interface.
	 */
	void setDecoratedItemProvider(INotifier<INotification> decoratedItemProvider);
}
