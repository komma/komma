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
 * $Id: DecoratorAdapterFactory.java,v 1.4 2007/03/23 17:37:21 marcelop Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.Collection;
import java.util.HashMap;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotifier;
import net.enilink.komma.common.notify.NotificationSupport;

/**
 * This abstract class provides support for creating
 * {@link IItemProviderDecorator}s for the adapters created by another
 * {@link IAdapterFactory}.
 */
public abstract class DecoratorAdapterFactory extends
		NotificationSupport<INotification> implements IAdapterFactory,
		IComposeableAdapterFactory, IDisposable {
	protected HashMap<Object, IItemProviderDecorator> itemProviderDecorators = new HashMap<Object, IItemProviderDecorator>();

	/**
	 * This keeps the
	 * {@link net.enilink.komma.IAdapterFactory.common.notify.AdapterFactory}
	 * being decorated.
	 */
	protected IAdapterFactory decoratedAdapterFactory;

	/**
	 * This is used to implement the {@link IComposeableAdapterFactory}
	 * interface.
	 */
	protected ComposedAdapterFactory parentAdapterFactory;

	/**
	 * This creates an instance that decorates the adapters from the given
	 * adapter factory.
	 */
	public DecoratorAdapterFactory(IAdapterFactory decoratedAdapterFactory) {
		this.decoratedAdapterFactory = decoratedAdapterFactory;
	}

	/**
	 * This just delegates to the {@link #decoratedAdapterFactory}.
	 */
	public boolean isFactoryForType(Object type) {
		return decoratedAdapterFactory.isFactoryForType(type);
	}

	/**
	 * This returns the adapter factory whose adapters are being decorated.
	 */
	public IAdapterFactory getDecoratedAdapterFactory() {
		return decoratedAdapterFactory;
	}

	/**
	 * This sets the adapter factory whose adapters will be decorated.
	 */
	public void setDecoratedAdapterFactory(
			IAdapterFactory decoratedAdapterFactory) {
		this.decoratedAdapterFactory = decoratedAdapterFactory;
	}

	/**
	 * This is called when a new decorator is needed by
	 * {@link #adapt(Object,Object)}.
	 */
	protected abstract IItemProviderDecorator createItemProviderDecorator(
			Object target, Object Type);

	/**
	 * All adapter creation is delegated to this method, which yields decorated
	 * item providers. It hooks up the decorators created by
	 * {@link #createItemProviderDecorator} to the adapters returned by
	 * {@link #decoratedAdapterFactory}.
	 */
	@SuppressWarnings("unchecked")
	public Object adapt(Object target, Object type) {
		Object adapter = decoratedAdapterFactory.adapt(target, type);
		if (adapter instanceof INotifier) {
			IItemProviderDecorator itemProviderDecorator = itemProviderDecorators
					.get(adapter);
			if (itemProviderDecorator == null) {
				itemProviderDecorator = createItemProviderDecorator(target,
						type);
				itemProviderDecorators.put(adapter, itemProviderDecorator);
				itemProviderDecorator
						.setDecoratedItemProvider((INotifier) adapter);
			}

			return itemProviderDecorator;
		}

		return adapter;
	}

	/**
	 * This returns the root adapter factory that delegates to this factory.
	 */
	public IComposeableAdapterFactory getRootAdapterFactory() {
		return parentAdapterFactory == null ? this : parentAdapterFactory
				.getRootAdapterFactory();
	}

	/**
	 * This sets the direct parent adapter factory into which this factory is
	 * composed.
	 */
	public void setParentAdapterFactory(
			ComposedAdapterFactory parentAdapterFactory) {
		this.parentAdapterFactory = parentAdapterFactory;
	}

	public void fireNotifications(
			Collection<? extends INotification> notifications) {
		super.fireNotifications(notifications);

		if (parentAdapterFactory != null) {
			parentAdapterFactory.fireNotifications(notifications);
		}
	}

	public void dispose() {
		for (Object object : itemProviderDecorators.values()) {
			if (object instanceof IDisposable) {
				((IDisposable) object).dispose();
			}
		}
	}
}
