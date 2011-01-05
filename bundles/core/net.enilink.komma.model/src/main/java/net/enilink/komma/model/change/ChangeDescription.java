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
import org.openrdf.repository.event.base.NotifyingRepositoryConnectionWrapper;

import net.enilink.komma.ds.change.IDataSourceChange;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.core.KommaException;

public class ChangeDescription implements IChangeDescription {
	private LinkedList<IDataSourceChange> changes = new LinkedList<IDataSourceChange>();
	private IModelSet.Internal modelSet;

	public ChangeDescription(IModelSet modelSet) {
		this.modelSet = (IModelSet.Internal) modelSet;
	}

	public void add(IDataSourceChange change) {
		changes.add(change);
	}

	public List<IDataSourceChange> getChanges() {
		return changes;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
		try {
			NotifyingRepositoryConnectionWrapper connection = null;
			try {
				connection = (NotifyingRepositoryConnectionWrapper) modelSet
						.getSharedRepositoyConnection();
				boolean isReportDeltas = connection.reportDeltas();
				if (isReportDeltas) {
					connection.setReportDeltas(false);
				}

				boolean autoCommit = connection.isAutoCommit();
				if (autoCommit) {
					connection.begin();
				}

				for (ListIterator<IDataSourceChange> it = changes
						.listIterator(changes.size()); it.hasPrevious();) {
					it.previous().redo(connection);
				}

				if (autoCommit) {
					connection.commit();
				}
				connection.setReportDeltas(isReportDeltas);
			} catch (Throwable e) {
				if (connection != null) {
					connection.rollback();
				}
				throw e;
			}
		} catch (Throwable e) {
			throw new KommaException(e);
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
		try {
			NotifyingRepositoryConnectionWrapper connection = null;
			try {
				connection = (NotifyingRepositoryConnectionWrapper) modelSet
						.getSharedRepositoyConnection();
				boolean isReportDeltas = connection.reportDeltas();
				if (isReportDeltas) {
					connection.setReportDeltas(false);
				}

				boolean autoCommit = connection.isAutoCommit();
				if (autoCommit) {
					connection.begin();
				}

				for (ListIterator<IDataSourceChange> it = changes
						.listIterator(changes.size()); it.hasPrevious();) {
					it.previous().undo(connection);
				}

				if (autoCommit) {
					connection.commit();
				}
				connection.setReportDeltas(isReportDeltas);
			} catch (Throwable e) {
				if (connection != null) {
					connection.rollback();
				}
				throw e;
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
