/*
 * Copyright James Leigh (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.repository.change;

import java.util.List;
import java.util.Stack;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.event.NotifyingRepository;
import org.openrdf.store.StoreException;


/**
 * Tracks changes to a RepositoryConnection and allows those changes to be
 * reversed. There is a performance hit when this object is attached to a
 * RepositoryConnection. This object depends on {@link NotifyingRepository} to
 * be on the stack. If the repository does not support or have enabled read
 * uncommitted, the {@link StatementRealizerRepository} must be a delegate of
 * {@link NotifyingRepository}.
 * 
 * @author James Leigh
 * @author Ken Wenzel
 * 
 */
public class RepositoryChangeStack extends RepositoryChangeTracker implements
		IRepositoryChangeStack {
	private boolean trackChanges = true;

	private int saveLocation = 0;

	private int undoLimit = 0;
	private Stack<List<IRepositoryChange>> undoStack = new Stack<List<IRepositoryChange>>(),
			redoStack = new Stack<List<IRepositoryChange>>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.change.IRepositoryChangeStack#flush()
	 */
	public void flush() {
		redoStack.clear();
		undoStack.clear();
		saveLocation = 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.change.IRepositoryChangeStack#getUndoLimit()
	 */
	public int getUndoLimit() {
		return undoLimit;
	}

	@Override
	protected void handleChanges(List<IRepositoryChange> committed) {
		if (trackChanges) {
			pushChanges(committed);
		}
		super.handleChanges(committed);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.change.IRepositoryChangeStack#isDirty()
	 */
	public boolean isDirty() {
		return undoStack.size() != saveLocation;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.change.IRepositoryChangeStack#markSaveLocation()
	 */
	public void markSaveLocation() {
		saveLocation = undoStack.size();
	}

	public void pushChanges(List<IRepositoryChange> changes) {
		if (getUndoLimit() > 0) {
			while (undoStack.size() >= getUndoLimit()) {
				undoStack.remove(0);
				if (saveLocation > -1)
					saveLocation--;
			}
		}
		if (saveLocation > undoStack.size())
			saveLocation = -1; // The save point was somewhere in the redo stack
		undoStack.push(changes);
	}

	private void redo(List<IRepositoryChange> changes, RepositoryConnection conn)
			throws StoreException {
		for (IRepositoryChange change : changes) {
			change.redo(conn);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.komma.change.IRepositoryChangeStack#redo(org.openrdf.repository
	 * .RepositoryConnection)
	 */
	public void redo(RepositoryConnection conn) throws StoreException {
		if (exception != null) {
			throw exception;
		}
		try {
			trackChanges = false;
			synchronized (activeConnections) {
				boolean autoCommit = conn.isAutoCommit();
				if (autoCommit) {
					conn.begin();
				}
				if (!redoStack.isEmpty()) {
					redo(redoStack.pop(), conn);
				}
				List<IRepositoryChange> activeChanges = activeConnections
						.get(conn);
				if (activeChanges != null) {
					redo(activeChanges, conn);
				}
				if (autoCommit) {
					conn.commit();
				}
			}
		} finally {
			trackChanges = true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.change.IRepositoryChangeStack#setUndoLimit(int)
	 */
	public void setUndoLimit(int undoLimit) {
		this.undoLimit = undoLimit;
	}

	private void undo(List<IRepositoryChange> changes, RepositoryConnection conn)
			throws StoreException {
		for (int i = changes.size() - 1; i >= 0; i--) {
			IRepositoryChange change = changes.get(i);
			change.undo(conn);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.komma.change.IRepositoryChangeStack#undo(org.openrdf.repository
	 * .RepositoryConnection)
	 */
	public void undo(RepositoryConnection conn) throws StoreException {
		if (exception != null) {
			throw exception;
		}

		try {
			trackChanges = false;
			synchronized (activeConnections) {
				boolean autoCommit = conn.isAutoCommit();
				if (autoCommit) {
					conn.begin();
				}
				List<IRepositoryChange> activeChanges = activeConnections
						.get(conn);
				if (activeChanges != null) {
					undo(activeChanges, conn);
				}
				if (!undoStack.isEmpty()) {
					List<IRepositoryChange> changes = undoStack.pop();
					undo(changes, conn);
					redoStack.add(changes);
				}
				if (autoCommit) {
					conn.commit();
				}
			}
		} finally {
			trackChanges = true;
		}
	}
}
