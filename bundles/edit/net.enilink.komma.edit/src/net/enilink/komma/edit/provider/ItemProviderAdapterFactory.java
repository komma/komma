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
 * $Id: ReflectiveItemProviderAdapterFactory.java,v 1.4 2006/12/28 06:48:53 marcelop Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.enilink.commons.util.Pair;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationBroadcaster;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.NotificationSupport;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.URI;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;

/**
 * This is the factory that is used to provide the interfaces needed to support
 * Viewers reflectively.
 */
public abstract class ItemProviderAdapterFactory<T> extends AdapterFactory
		implements IComposeableAdapterFactory, IDisposable,
		INotificationBroadcaster<INotification> {
	private Set<URI> namespaceURIs;

	protected Map<Pair<IModel, Collection<? extends T>>, Object> providers = new HashMap<Pair<IModel, Collection<? extends T>>, Object>();

	protected IResourceLocator resourceLocator;

	protected NotificationSupport<INotification> notificationSupport = new NotificationSupport<INotification>();

	protected ComposedAdapterFactory parentAdapterFactory;

	protected Collection<Object> supportedTypes = new ArrayList<Object>();

	public ItemProviderAdapterFactory(IResourceLocator resourceLocator,
			URI... namespaceURIs) {
		this.namespaceURIs = new HashSet<URI>(Arrays.asList(namespaceURIs));
		this.resourceLocator = resourceLocator;

		supportedTypes.add(IEditingDomainItemProvider.class);
		supportedTypes.add(IStructuredItemContentProvider.class);
		supportedTypes.add(ITreeItemContentProvider.class);
		supportedTypes.add(IItemColorProvider.class);
		supportedTypes.add(IItemFontProvider.class);
		supportedTypes.add(IItemLabelProvider.class);
		supportedTypes.add(IItemPropertySource.class);
		supportedTypes.add(ITableItemLabelProvider.class);
		supportedTypes.add(ISearchableItemProvider.class);
	}

	@Override
	public Object adapt(Object object, Object type) {
		return doAdapt(object, this);
	}

	protected Object doAdapt(Object object, Object type) {
		return super.adapt(object, type);
	}

	public void addListener(
			INotificationListener<INotification> notificationListener) {
		notificationSupport.addListener(notificationListener);
	}

	@Override
	protected Object createAdapter(Object object, Object type) {
		Collection<T> types = getTypes(object);
		Pair<IModel, Collection<? extends T>> key = new Pair<IModel, Collection<? extends T>>(
				object instanceof IObject ? ((IObject) object).getModel()
						: null, types);

		Object provider;
		if ((provider = providers.get(key)) == null) {
			provider = createItemProvider(object, types, type);
			providers.put(key, provider);
		}
		return provider;
	}

	protected abstract Collection<T> getTypes(Object object);

	protected abstract Object createItemProvider(Object object,
			Collection<T> types, Object providerType);

	public void dispose() {
		for (Object provider : providers.values()) {
			if (provider instanceof IDisposable) {
				((IDisposable) provider).dispose();
			}
		}
		providers.clear();
	}

	public void fireNotifications(
			Collection<? extends INotification> notifications) {
		notificationSupport.fireNotifications(notifications);

		if (parentAdapterFactory != null) {
			parentAdapterFactory.fireNotifications(notifications);
		}
	}

	public IComposeableAdapterFactory getRootAdapterFactory() {
		return parentAdapterFactory == null ? this : parentAdapterFactory
				.getRootAdapterFactory();
	}

	@Override
	public boolean isFactoryForType(Object type) {
		return (type instanceof URI && namespaceURIs.contains(type))
				|| supportedTypes.contains(type)
				|| super.isFactoryForType(type);
	}

	public void removeListener(
			INotificationListener<INotification> notificationListener) {
		notificationSupport.removeListener(notificationListener);
	}

	public void setParentAdapterFactory(
			ComposedAdapterFactory parentAdapterFactory) {
		this.parentAdapterFactory = parentAdapterFactory;
	}
}
