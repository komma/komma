/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.change;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.change.DataChangeTracker;
import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.core.KommaException;

public class ChangeDescription implements IChangeDescription {
	private LinkedList<IDataChange> changes = new LinkedList<IDataChange>();
	private IModelSet.Internal modelSet;

	public ChangeDescription(IModelSet modelSet) {
		this.modelSet = (IModelSet.Internal) modelSet;
	}

	public void add(IDataChange change) {
		changes.add(change);
	}

	public List<IDataChange> getChanges() {
		return changes;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
		try {
			IDataManager dm = null;
			try {
				dm = modelSet.getDataManagerFactory().get();

				DataChangeTracker changeTracker = (DataChangeTracker) modelSet
						.getDataChangeTracker();
				boolean isTrackingChanges = changeTracker.isEnabled();
				if (isTrackingChanges) {
					changeTracker.setEnabled(false);
				}

				dm.getTransaction().begin();
				for (ListIterator<IDataChange> it = changes
						.listIterator(changes.size()); it.hasPrevious();) {
					it.previous().redo(dm);
				}

				dm.getTransaction().commit();
				changeTracker.setEnabled(isTrackingChanges);
			} catch (Throwable e) {
				if (dm != null && dm.getTransaction().isActive()) {
					dm.getTransaction().rollback();
				}
				throw e;
			} finally {
				if (dm != null) {
					dm.close();
				}
			}
		} catch (Throwable e) {
			throw new KommaException(e);
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
		try {
			IDataManager dm = null;
			try {
				dm = modelSet.getDataManagerFactory().get();

				DataChangeTracker changeTracker = (DataChangeTracker) modelSet
						.getDataChangeTracker();
				boolean isTrackingChanges = changeTracker.isEnabled();
				if (isTrackingChanges) {
					changeTracker.setEnabled(false);
				}

				dm.getTransaction().begin();
				for (ListIterator<IDataChange> it = changes
						.listIterator(changes.size()); it.hasPrevious();) {
					it.previous().undo(dm);
				}

				dm.getTransaction().commit();
				changeTracker.setEnabled(isTrackingChanges);
			} catch (Throwable e) {
				if (dm != null && dm.getTransaction().isActive()) {
					dm.getTransaction().rollback();
				}
				throw e;
			} finally {
				if (dm != null) {
					dm.close();
				}
			}
		} catch (Throwable e) {
			throw new KommaException(e);
		}

		return Status.OK_STATUS;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return changes.isEmpty();
	}
}
