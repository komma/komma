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
 * $Id: AdapterFactoryContentProvider.java,v 1.12 2008/05/07 19:08:40 emerks Exp $
 */
package net.enilink.komma.edit.ui.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.INotifier;
import net.enilink.komma.common.notify.NotificationFilter;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.provider.IItemPropertySource;
import net.enilink.komma.edit.provider.IStructuredItemContentProvider;
import net.enilink.komma.edit.provider.ITreeItemContentProvider;
import net.enilink.komma.edit.provider.IViewerNotification;
import net.enilink.komma.edit.provider.ViewerNotification;
import net.enilink.komma.model.IModelAware;

/**
 * This content provider wraps an AdapterFactory and it delegates its JFace
 * provider interfaces to corresponding adapter-implemented item provider
 * interfaces. All method calls to the various structured content provider
 * interfaces are delegated to interfaces implemented by the adapters generated
 * by the AdapterFactory.
 * {@link org.eclipse.jface.viewers.IStructuredContentProvider} is delegated to
 * {@link IStructuredItemContentProvider}; {@link ITreeContentProvider} is
 * delegated to {@link ITreeItemContentProvider}; and
 * {@link IPropertySourceProvider} to {@link IItemPropertySource}.
 */
public class AdapterFactoryContentProvider implements ITreeContentProvider,
		IPropertySourceProvider, INotificationListener<IViewerNotification> {
	/**
	 * This keeps track of the one factory we are using. Use a
	 * {@link org.eclipse.emf.edit.provider.ComposedAdapterFactory} if adapters
	 * from more the one factory are involved in the model.
	 */
	protected IAdapterFactory adapterFactory;

	/**
	 * This keeps track of the one viewer using this content provider.
	 */
	protected Viewer viewer;

	/**
	 * This is used to queue viewer notifications and refresh viewers based on
	 * them.
	 * 
	 */
	protected ViewerRefresh viewerRefresh;

	private static final Class<?> IStructuredItemContentProviderClass = IStructuredItemContentProvider.class;
	private static final Class<?> ITreeItemContentProviderClass = ITreeItemContentProvider.class;
	private static final Class<?> IItemPropertySourceClass = IItemPropertySource.class;

	/**
	 * This constructs an instance that wraps this factory. The factory should
	 * yield adapters that implement the various IItemContentProvider
	 * interfaces. If the adapter factory is an {@link IChangeNotifier}, a
	 * listener is added to it, so it's important to call {@link #dispose()}.
	 */
	@SuppressWarnings("unchecked")
	public AdapterFactoryContentProvider(IAdapterFactory adapterFactory) {
		this.adapterFactory = adapterFactory;

		if (adapterFactory instanceof INotifier) {
			((INotifier<IViewerNotification>) adapterFactory).addListener(this);
		}
	}

	/**
	 * This sets the wrapped factory. If the adapter factory is an
	 * {@link IChangeNotifier}, a listener is added to it, so it's important to
	 * call {@link #dispose()}.
	 */
	@SuppressWarnings("unchecked")
	public void setAdapterFactory(IAdapterFactory adapterFactory) {
		if (this.adapterFactory instanceof INotifier) {
			((INotifier<IViewerNotification>) this.adapterFactory)
					.removeListener(this);
		}

		if (adapterFactory instanceof INotifier) {
			((INotifier<IViewerNotification>) adapterFactory).addListener(this);
		}

		this.adapterFactory = adapterFactory;
	}

	/**
	 * This returns the wrapped factory.
	 */
	public IAdapterFactory getAdapterFactory() {
		return adapterFactory;
	}

	/**
	 * The given Viewer will start (oldInput == null) or stop (newInput == null)
	 * listening for domain events.
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// If there was no old input, then we must be providing content for this
		// part for the first time...
		this.viewer = viewer;
	}

	/**
	 * This implements
	 * {@link org.eclipse.jface.viewers.IStructuredContentProvider}.getElements
	 * to forward the call to an object that implements
	 * {@link org.eclipse.emf.edit.provider.IStructuredItemContentProvider#getElements
	 * IStructuredItemContentProvider.getElements}.
	 */
	public Object[] getElements(Object object) {
		if (object instanceof Object[]) {
			return (Object[]) object;
		}
		// Get the adapter from the factory.
		IStructuredItemContentProvider structuredItemContentProvider = (IStructuredItemContentProvider) adapterFactory
				.adapt(object, IStructuredItemContentProviderClass);

		// Either delegate the call or return nothing.
		return (structuredItemContentProvider != null ? structuredItemContentProvider
				.getElements(object) : Collections.EMPTY_LIST).toArray();
	}

	/**
	 * This implements {@link org.eclipse.jface.viewers.ITreeContentProvider}
	 * .getChildren to forward the call to an object that implements
	 * {@link org.eclipse.emf.edit.provider.ITreeItemContentProvider#getChildren
	 * ITreeItemContentProvider.getChildren}.
	 */
	public Object[] getChildren(Object object) {
		// Get the adapter from the factory.
		ITreeItemContentProvider treeItemContentProvider = (ITreeItemContentProvider) adapterFactory
				.adapt(object, ITreeItemContentProviderClass);

		// Either delegate the call or return nothing.
		return (treeItemContentProvider != null ? treeItemContentProvider
				.getChildren(object) : Collections.EMPTY_LIST).toArray();
	}

	/**
	 * This implements {@link org.eclipse.jface.viewers.ITreeContentProvider}
	 * .hasChildren to forward the call to an object that implements
	 * {@link org.eclipse.emf.edit.provider.ITreeItemContentProvider#hasChildren
	 * ITreeItemContentProvider.hasChildren}.
	 */
	public boolean hasChildren(Object object) {
		// Get the adapter from the factory.
		ITreeItemContentProvider treeItemContentProvider = (ITreeItemContentProvider) adapterFactory
				.adapt(object, ITreeItemContentProviderClass);

		// Either delegate the call or return nothing.
		return treeItemContentProvider != null
				&& treeItemContentProvider.hasChildren(object);
	}

	/**
	 * This implements {@link org.eclipse.jface.viewers.ITreeContentProvider}
	 * .getParent to forward the call to an object that implements
	 * {@link org.eclipse.emf.edit.provider.ITreeItemContentProvider#getParent
	 * ITreeItemContentProvider.getParent}.
	 */
	public Object getParent(Object object) {
		// Get the adapter from the factory.
		ITreeItemContentProvider treeItemContentProvider = (ITreeItemContentProvider) adapterFactory
				.adapt(object, ITreeItemContentProviderClass);

		// Either delegate the call or return nothing.
		return treeItemContentProvider != null ? treeItemContentProvider
				.getParent(object) : null;
	}

	/**
	 * This discards the content provider and removes this as a listener to the
	 * {@link #adapterFactory}.
	 */
	@SuppressWarnings("unchecked")
	public void dispose() {
		if (adapterFactory instanceof INotifier) {
			((INotifier<IViewerNotification>) adapterFactory)
					.removeListener(this);
		}
		viewer = null;
	}

	/**
	 * This implements
	 * {@link org.eclipse.ui.views.properties.IPropertySourceProvider}
	 * .getPropertySource to forward the call to an object that implements
	 * {@link org.eclipse.emf.edit.provider.IItemPropertySource}.
	 */
	public IPropertySource getPropertySource(Object object) {
		if (object instanceof IPropertySource) {
			return (IPropertySource) object;
		} else {
			IItemPropertySource itemPropertySource = (IItemPropertySource) adapterFactory
					.adapt(object, IItemPropertySourceClass);

			return itemPropertySource != null ? createPropertySource(object,
					itemPropertySource) : null;
		}
	}

	protected IPropertySource createPropertySource(Object object,
			IItemPropertySource itemPropertySource) {
		return new PropertySource(object, adapterFactory, itemPropertySource);
	}

	@Override
	public void notifyChanged(
			Collection<? extends IViewerNotification> notifications) {
		if (viewer != null && viewer.getControl() != null
				&& !viewer.getControl().isDisposed()) {
			// If the notification is an IViewerNotification, it specifies how
			// ViewerRefresh should behave. Otherwise fall
			// back to NotifyChangedToViewerRefresh, which determines how to
			// refresh the viewer directly from the model
			// notification.

			boolean executeRefresh = false;
			for (IViewerNotification notification : notifications) {
				if (viewerRefresh == null) {
					viewerRefresh = new ViewerRefresh(viewer);
				}

				executeRefresh |= viewerRefresh
						.addNotification((IViewerNotification) notification);
			}

			if (executeRefresh) {
				viewer.getControl().getDisplay().asyncExec(viewerRefresh);
			}
		}
	}

	/**
	 * A runnable class that efficiently updates a
	 * {@link org.eclipse.jface.viewers.Viewer} via standard APIs, based on
	 * queued {@link org.eclipse.emf.edit.provider.IViewerNotification}s from
	 * the model's item providers.
	 */
	public static class ViewerRefresh implements Runnable {
		Viewer viewer;
		List<IViewerNotification> notifications;

		public ViewerRefresh(Viewer viewer) {
			this.viewer = viewer;
		}

		/**
		 * Adds a viewer notification to the queue that will be processed by
		 * this <code>ViewerRefresh</code>. Duplicative notifications will not
		 * be queued.
		 * 
		 * @param notification
		 *            the notification to add to the queue
		 * @return whether the queue has been made non-empty, which would
		 *         indicate that the <code>ViewerRefresh</code> needs to be
		 *         {@link Display#asyncExec scheduled} on the event queue
		 */
		public synchronized boolean addNotification(
				IViewerNotification notification) {
			if (notifications == null) {
				notifications = new ArrayList<IViewerNotification>();
			}

			if (notifications.isEmpty()) {
				notifications.add(notification);
				return true;
			}

			if (viewer instanceof StructuredViewer) {
				for (Iterator<IViewerNotification> i = notifications.iterator(); i
						.hasNext() && notification != null;) {
					IViewerNotification old = i.next();
					IViewerNotification merged = merge(old, notification);
					if (merged == old) {
						notification = null;
					} else if (merged != null) {
						notification = merged;
						i.remove();
					}
				}
				if (notification != null) {
					notifications.add(notification);
				}
			}
			return false;
		}

		/**
		 * Compares two notifications and, if duplicative, returns a single
		 * notification that does the work of both. Note: this gives priority to
		 * a content refresh on the whole viewer over a content refresh or label
		 * update on a specific element; however, it doesn't use parent-child
		 * relationships to determine if refreshes on non-equal elements are
		 * duplicative.
		 * 
		 * @return a single notification that is equivalent to the two
		 *         parameters, or null if they are non-duplicative
		 */
		protected IViewerNotification merge(IViewerNotification n1,
				IViewerNotification n2) {
			// This implements the following order of preference:
			// 1. full refresh and update
			// 2. full refresh (add update if necessary)
			// 3. refresh element with update
			// 4. refresh element (if necessary)
			// 5. update element
			//
			if (n1.getElement() == null && n1.isLabelUpdate()) {
				return n1;
			} else if (n2.getElement() == null && n2.isLabelUpdate()) {
				return n2;
			} else if (n1.getElement() == null) {
				if (n2.isLabelUpdate()) {
					n1 = new ViewerNotification();
				}
				return n1;
			} else if (n2.getElement() == null) {
				if (n1.isLabelUpdate()) {
					n2 = new ViewerNotification();
				}
				return n2;
			} else if (n1.getElement().equals(n2.getElement())) {
				if (n1.isContentRefresh() && n1.isLabelUpdate()) {
					return n1;
				} else if (n2.isContentRefresh() && n2.isLabelUpdate()) {
					return n2;
				} else if (n1.isContentRefresh()) {
					if (n2.isLabelUpdate()) {
						n1 = new ViewerNotification(n1.getElement(), true, true);
					}
					return n1;
				} else if (n2.isContentRefresh()) {
					if (n1.isLabelUpdate()) {
						n2 = new ViewerNotification(n2.getElement(), true, true);
					}
					return n2;
				} else if (n1.isLabelUpdate()) {
					return n1;
				} else // n2.isLabelUpdate()
				{
					return n2;
				}
			}
			return null;
		}

		public void run() {
			if (viewer != null && viewer.getControl() != null
					&& !viewer.getControl().isDisposed()) {
				List<IViewerNotification> current;

				synchronized (this) {
					current = notifications;
					notifications = null;
				}

				if (current != null) {
					for (IViewerNotification viewerNotification : current) {
						refresh(viewerNotification);
					}
				}
			}
		}

		protected void refresh(IViewerNotification notification) {
			Object element = notification.getElement();
			if (viewer instanceof StructuredViewer) {
				StructuredViewer structuredViewer = (StructuredViewer) viewer;

				ISelection selection = structuredViewer.getSelection();
				boolean isStaleSelection = AdapterFactoryEditingDomain
						.isStale(selection);
				if (isStaleSelection) {
					viewer.setSelection(StructuredSelection.EMPTY);
				}

				if (element != null) {
					// the following test may also be done with an
					// IElementComparer on the viewer
					Widget widget = structuredViewer.testFindItem(element);
					if (widget != null) {
						// <FIX> for
						// https://bugs.eclipse.org/bugs/show_bug.cgi?id=389482
						if (viewer instanceof TreeViewer
								&& (viewer.getControl().getStyle() & SWT.VIRTUAL) != 0) {
							if (widget != null && !widget.isDisposed()) {
								// force widget to be refreshed
								((TreeItem) widget).getChecked();
							}
						}
						// </FIX>

						Object oldElement = widget.getData();
						// ensure that old and new element are contained within
						// the same model or in no model at all
						if (oldElement == null
								|| element instanceof IModelAware
								&& oldElement instanceof IModelAware
								&& ((IModelAware) element).getModel().equals(
										((IModelAware) oldElement).getModel())
								|| !(element instanceof IModelAware || oldElement instanceof IModelAware)) {
							if (notification.isContentRefresh()) {
								structuredViewer.refresh(element,
										notification.isLabelUpdate());
							} else if (notification.isLabelUpdate()) {
								structuredViewer.update(element, null);
							}
						}
					}
				} else {
					structuredViewer.refresh(notification.isLabelUpdate());
				}

				if (isStaleSelection) {
					Object object = structuredViewer.getInput();
					IEditingDomain editingDomain = AdapterFactoryEditingDomain
							.getEditingDomainFor(object);
					if (editingDomain == null) {
						for (Object child : ((IStructuredContentProvider) structuredViewer
								.getContentProvider()).getElements(object)) {
							editingDomain = AdapterFactoryEditingDomain
									.getEditingDomainFor(child);
							if (editingDomain != null) {
								break;
							}
						}
					}
					// TODO handle stale selections
					// if (editingDomain instanceof AdapterFactoryEditingDomain)
					// {
					// structuredViewer
					// .setSelection(
					// new StructuredSelection(
					// ((AdapterFactoryEditingDomain) editingDomain)
					// .resolve(((IStructuredSelection) selection)
					// .toList())),
					// true);
					// }
				}
			} else {
				viewer.refresh();
			}
		}
	}

	@Override
	public NotificationFilter<IViewerNotification> getFilter() {
		return null;
	}
}
