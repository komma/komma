/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.ui.provider.reflective;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.NotificationFilter;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Statement;

public abstract class ModelContentProvider implements IContentProvider {
	private INotificationListener<INotification> listener = new INotificationListener<INotification>() {
		NotificationFilter<INotification> filter = NotificationFilter
				.instanceOf(IStatementNotification.class);

		private IStatement transformChange(IStatementNotification change) {
			IResource subj = (IResource) model.resolve(change.getSubject());
			IEntity pred = (IEntity) model.resolve(change.getPredicate());
			Object obj = transformStatementObjects
					&& change.getObject() instanceof IReference ? model
					.resolve((IReference) change.getObject()) : change
					.getObject();

			return new Statement(subj, pred, obj);
		}

		@Override
		public NotificationFilter<INotification> getFilter() {
			return filter;
		}

		@Override
		public void notifyChanged(
				Collection<? extends INotification> notifications) {
			Collection<Runnable> runnables = new ArrayList<Runnable>();
			try {
				for (INotification notification : notifications) {
					IStatementNotification stmtNotification = (IStatementNotification) notification;

					boolean processPendingChanges = true;
					if (stmtNotification.isAdd()) {
						processPendingChanges = addedStatement(
								transformChange(stmtNotification), runnables);
					} else {
						processPendingChanges = removedStatement(
								transformChange(stmtNotification), runnables);
					}
					if (!processPendingChanges) {
						break;
					}
				}
			} catch (Exception e) {
				KommaEditUIPlugin.INSTANCE.log(e);
			} finally {
				executeRunnables(runnables);
			}

		}
	};

	protected boolean executedFullRefresh = false;
	private Viewer viewer;
	protected IModel model;
	protected boolean listenerRegistered = false;
	protected boolean registerListener = false;
	protected boolean transformStatementObjects = true;

	private Collection<Runnable> pendingUpdates;

	public void dispose() {
		unregisterListener();
		model = null;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		unregisterListener();
		this.viewer = viewer;

		if (newInput instanceof IModel) {
			model = (IModel) newInput;
		} else if (newInput instanceof IObject) {
			model = ((IObject) newInput).getModel();
		} else {
			model = null;
		}

		internalInputChanged(viewer, oldInput, newInput);
		registerListener = shouldRegisterListener(viewer);
		registerListener();
	}

	protected void setTransformStatementObjects(
			boolean transformStatementObjects) {
		this.transformStatementObjects = transformStatementObjects;
	}

	protected void internalInputChanged(Viewer viewer, Object oldInput,
			Object newInput) {

	}

	abstract protected boolean shouldRegisterListener(Viewer viewer);

	public void registerListener() {
		unregisterListener();
		if (!listenerRegistered && registerListener && model != null) {
			listenerRegistered = true;
			model.getModelSet().addListener(listener);
		}
	}

	public void unregisterListener() {
		if (listenerRegistered && model != null) {
			model.getModelSet().removeListener(listener);
			listenerRegistered = false;
		}
	}

	abstract protected boolean addedStatement(IStatement stmt,
			Collection<Runnable> runnables);

	abstract protected boolean removedStatement(IStatement stmt,
			Collection<Runnable> runnables);

	protected void updatedViewer() {
	}

	protected final void executeRunnables(final Collection<Runnable> runnables) {
		// now post all collected runnables
		Control ctrl = viewer.getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
			// Are we in the UIThread? If so spin it until we are done
			if (ctrl.getDisplay().getThread() == Thread.currentThread()) {
				runUpdates(runnables);
			} else {
				synchronized (this) {
					if (pendingUpdates == null) {
						pendingUpdates = runnables;
					} else {
						pendingUpdates.addAll(runnables);
					}
				}
				ctrl.getDisplay().asyncExec(new Runnable() {
					public void run() {
						runPendingUpdates();
					}
				});
			}
		}
	}

	/**
	 * Run all of the runnables that are the widget updates. Must be called in
	 * the display thread.
	 */
	public void runPendingUpdates() {
		Collection<Runnable> pendingUpdates;
		synchronized (this) {
			pendingUpdates = this.pendingUpdates;
			this.pendingUpdates = null;
		}
		if (pendingUpdates != null && viewer != null) {
			Control control = viewer.getControl();
			if (control != null && !control.isDisposed()) {
				runUpdates(pendingUpdates);
			}
		}
	}

	private void runUpdates(Collection<Runnable> runnables) {
		for (Runnable runnable : runnables) {
			runnable.run();
			if (executedFullRefresh) {
				executedFullRefresh = false;
				break;
			}
		}
		if (!runnables.isEmpty()) {
			updatedViewer();
		}
	}

	protected void postRefresh(final Collection<Runnable> runnables) {
		// remove all previous update operations
		runnables.clear();

		runnables.add(new Runnable() {
			public void run() {
				((StructuredViewer) viewer).refresh();

				// make sure no other update operations are executed after this
				// one
				executedFullRefresh = true;
			}
		});
	}

	protected void postRefresh(final List<?> toRefresh,
			final boolean updateLabels, Collection<Runnable> runnables) {
		assert viewer instanceof StructuredViewer : "This method may only be called for structured viewers";

		runnables.add(new Runnable() {
			public void run() {
				for (Iterator<?> iter = toRefresh.iterator(); iter.hasNext();) {
					((StructuredViewer) viewer).refresh(iter.next(),
							updateLabels);
				}
			}
		});
	}

	protected void postAdd(final Object parent, final Object element,
			Collection<Runnable> runnables) {
		assert viewer instanceof TreeViewer : "This method may only be called for tree viewers";

		runnables.add(new Runnable() {
			public void run() {
				Widget[] items = ((TreeViewer) viewer).testFindItems(element);
				for (int i = 0; i < items.length; i++) {
					Widget item = items[i];
					if (item instanceof TreeItem && !item.isDisposed()) {
						TreeItem parentItem = ((TreeItem) item).getParentItem();
						if (parentItem != null && !parentItem.isDisposed()
								&& parent.equals(parentItem.getData())) {
							return; // no add, element already added (most
							// likely by a refresh)
						}
					}
				}
				((TreeViewer) viewer).add(parent, element);
			}
		});
	}

	protected void postRemove(final Object element,
			Collection<Runnable> runnables) {
		runnables.add(new Runnable() {
			public void run() {
				if (viewer instanceof TableViewer) {
					((TableViewer) viewer).remove(element);
				} else if (viewer instanceof TreeViewer) {
					((TreeViewer) viewer).remove(element);
				}
			}
		});
	}

	/**
	 * Returns the associated viewer of this content provider
	 * 
	 * @return the viewer
	 */
	protected Viewer getViewer() {
		return viewer;
	}
}