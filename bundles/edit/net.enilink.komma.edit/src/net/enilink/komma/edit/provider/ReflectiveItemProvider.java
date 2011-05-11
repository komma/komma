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
 * $Id: ReflectiveItemProvider.java,v 1.22 2008/05/25 17:27:40 emerks Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.komma.core.IReference;

/**
 * This adapter implementation provides reflective support that emulates the
 * behavior of a default generated item provider.
 */
public class ReflectiveItemProvider extends ItemProviderAdapter implements
		IEditingDomainItemProvider, IStructuredItemContentProvider,
		ITreeItemContentProvider, IItemLabelProvider, IItemPropertySource,
		ISearchableItemProvider {

	protected IResourceLocator resourceLocator;
	protected Collection<IClass> supportedTypes;

	public ReflectiveItemProvider(IAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, Collection<IClass> supportedTypes) {
		super(adapterFactory);
		this.resourceLocator = resourceLocator;
		this.supportedTypes = supportedTypes;
	}

	public IResourceLocator getResourceLocator() {
		return resourceLocator;
	}

	/**
	 * This handles model notifications by calling {@link #updateChildren} to
	 * update any cached children and by creating a viewer notification, which
	 * it passes to {@link #fireNotifyChanged}.
	 */
	@Override
	public void notifyChanged(Collection<? extends INotification> notifications) {
		super.notifyChanged(notifications);

		Collection<IViewerNotification> viewerNotifications = null;
		for (INotification notification : notifications) {
			if (notification instanceof IStatementNotification) {
				Collection<IViewerNotification> newViewerNotifications;
				if ("label".equals(((IStatementNotification) notification)
						.getPredicate().getURI().localPart())) {
					newViewerNotifications = addViewerNotifications(
							viewerNotifications,
							(IStatementNotification) notification, false, true);

				} else {
					newViewerNotifications = addViewerNotifications(
							viewerNotifications,
							(IStatementNotification) notification, true, true);
				}
				if (newViewerNotifications != null) {
					viewerNotifications = newViewerNotifications;
				}
			}
		}

		if (viewerNotifications != null) {
			fireNotifications(viewerNotifications);
			return;
		}
	}

	protected Collection<IViewerNotification> createViewerNotificationList() {
		return new ArrayList<IViewerNotification>();
	}

	protected Collection<IViewerNotification> addViewerNotifications(
			Collection<IViewerNotification> viewerNotifications,
			IStatementNotification notification, boolean contentRefresh,
			boolean labelUpdate) {
		Object element = notification.getSubject();

		IObject object;
		if (element instanceof IObject) {
			object = (IObject) element;
		} else if (element instanceof IReference) {
			object = resolveReference((IReference) element);
		} else {
			return null;
		}

		if (object != null) {
			object.refresh(notification.getPredicate());

			if (viewerNotifications == null) {
				viewerNotifications = createViewerNotificationList();
			}

			viewerNotifications.add(new ViewerNotification(object,
					contentRefresh, labelUpdate));
		}
		return viewerNotifications;
	}

	/**
	 * This should not be implemented as anonymous inner class to prevent
	 * referencing the item provider instance.
	 * 
	 * Example: Storing an instance of this class in a global image registry can
	 * lead to memory leaks if item provider instances are referenced.
	 */
	static class ComposedCreateChildImage extends ComposedImage {
		public ComposedCreateChildImage(Collection<?> images) {
			super(images);
		}

		@Override
		public List<Point> getDrawPoints(Size size) {
			List<Point> result = super.getDrawPoints(size);
			result.get(1).x = size.width - 7;
			return result;
		}
	}

	@Override
	public Object getCreateChildImage(Object owner, Object property,
			Object childDescription, Collection<?> selection) {
		if (childDescription instanceof ChildDescriptor) {
			childDescription = ((ChildDescriptor) childDescription).getValue();
		}

		IProperty resolvedProperty = (IProperty) ((IObject) owner).getModel()
				.resolve((IReference) property);

		String name = "full/ctool16/Create"
				+ getTypes(owner).iterator().next().getURI().localPart() + "_"
				+ resolvedProperty.getURI().localPart();

		Object childType = childDescription instanceof Collection<?> ? ((Collection<?>) childDescription)
				.iterator().next() : childDescription;

		if (childType instanceof net.enilink.vocab.rdfs.Class
				&& ((IObject) childType).getURI() != null) {
			name += "_" + ((IObject) childType).getURI().localPart();
		}

		try {
			return getResourceLocator().getImage(name);
		} catch (Exception e) {
			List<Object> images = new ArrayList<Object>();
			IItemLabelProvider itemLabelProvider = (IItemLabelProvider) ((IComposeableAdapterFactory) adapterFactory)
					.getRootAdapterFactory().adapt(childType,
							IItemLabelProvider.class);

			Object itemImage = itemLabelProvider != null ? itemLabelProvider
					.getImage(childType) : null;
			if (itemImage != null) {
				images.add(itemLabelProvider.getImage(childType));
				images.add(KommaEditPlugin.INSTANCE
						.getImage("full/ovr16/CreateChild"));
				return new ComposedCreateChildImage(images);
			}
		}
		return super.getCreateChildImage(owner, property, childType, selection);
	}

	@Override
	public Object getImage(Object object) {
		for (IClass type : getTypes(object)) {
			try {
				return overlayImage(
						object,
						getResourceLocator().getImage(
								"full/obj16/" + type.getURI().localPart()));
			} catch (Exception missing) {
				// ignore
			}
		}

		try {
			// try default image from provided resource locator
			return overlayImage(object,
					getResourceLocator().getImage("full/obj16/Item"));
		} catch (Exception missing2) {
			// fall back to image provided by edit plugin
			return KommaEditPlugin.INSTANCE.getImage("full/obj16/Item");
		}
	}

	protected Collection<? extends IClass> getTypes(Object object) {
		return supportedTypes;
	}

	public String getText(Object object) {
		return ModelUtil.getLabel(object);
	}

	protected ISearchableItemProvider getSearchableItemProvider() {
		return new SparqlSearchableItemProvider();
	}

	@Override
	public IExtendedIterator<?> find(Object expression, Object parent, int limit) {
		return getSearchableItemProvider().find(expression, parent, limit);
	}
}
