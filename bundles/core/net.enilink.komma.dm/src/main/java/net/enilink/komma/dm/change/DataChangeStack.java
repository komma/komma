/*
 * Copyright James Leigh (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.dm.change;

import java.util.List;
import java.util.Stack;

import net.enilink.komma.dm.IDataManager;

/**
 * Tracks changes to an {@link IDataManager} and allows those changes to be
 * reversed.
 * 
 */
public class DataChangeStack extends DataChangeTracker implements
		IDataChangeStack {
	private boolean trackChanges = true;

	private int saveLocation = 0;

	private int undoLimit = 0;
	private Stack<List<IDataChange>> undoStack = new Stack<List<IDataChange>>(),
			redoStack = new Stack<List<IDataChange>>();

	public void flush() {
		redoStack.clear();
		undoStack.clear();
		saveLocation = 0;
	}

	public int getUndoLimit() {
		return undoLimit;
	}

	@Override
	protected void handleChanges(List<IDataChange> committed) {
		if (trackChanges) {
			pushChanges(committed);
		}
		super.handleChanges(committed);
	}

	public boolean isDirty() {
		return undoStack.size() != saveLocation;
	}

	public void markSaveLocation() {
		saveLocation = undoStack.size();
	}

	public void pushChanges(List<IDataChange> changes) {
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

	private void redo(List<IDataChange> changes, IDataManager dm) {
		for (IDataChange change : changes) {
			change.redo(dm);
		}
	}

	public void redo(IDataManager dm) {
		try {
			trackChanges = false;
			synchronized (activeDataManagers) {
				boolean active = dm.getTransaction().isActive();
				if (!active) {
					dm.getTransaction().begin();
				}
				if (!redoStack.isEmpty()) {
					redo(redoStack.pop(), dm);
				}
				List<IDataChange> activeChanges = activeDataManagers.get(dm);
				if (activeChanges != null) {
					redo(activeChanges, dm);
				}
				if (!active) {
					dm.getTransaction().commit();
				}
			}
		} finally {
			trackChanges = true;
		}
	}

	public void setUndoLimit(int undoLimit) {
		this.undoLimit = undoLimit;
	}

	private void undo(List<IDataChange> changes, IDataManager dm) {
		for (int i = changes.size() - 1; i >= 0; i--) {
			IDataChange change = changes.get(i);
			change.undo(dm);
		}
	}

	public void undo(IDataManager dm) {
		try {
			trackChanges = false;
			synchronized (activeDataManagers) {
				boolean active = dm.getTransaction().isActive();
				if (!active) {
					dm.getTransaction().begin();
				}
				List<IDataChange> activeChanges = activeDataManagers.get(dm);
				if (activeChanges != null) {
					undo(activeChanges, dm);
				}
				if (!undoStack.isEmpty()) {
					List<IDataChange> changes = undoStack.pop();
					undo(changes, dm);
					redoStack.add(changes);
				}
				if (!active) {
					dm.getTransaction().commit();
				}
			}
		} finally {
			trackChanges = true;
		}
	}
}
