/*
 * Copyright James Leigh (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.dm.change;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.internal.change.AddChange;
import net.enilink.komma.dm.internal.change.OverrideNamespaceChange;
import net.enilink.komma.dm.internal.change.RemoveChange;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.URI;

/**
 * Tracks changes to an {@link IDataManager} by implementing the interface
 * {@link IDataChangeSupport}.
 * 
 */
public class DataChangeTracker implements IDataChangeSupport,
		IDataChangeTracker {
	private final static IReference[] NULL_CONTEXT = { null };
	protected Map<IDataManager, List<IDataChange>> activeDataManagers = new HashMap<IDataManager, List<IDataChange>>();
	protected WeakHashMap<IDataManager, Object> disabledDataManagers = new WeakHashMap<IDataManager, Object>();

	private CopyOnWriteArraySet<IDataChangeListener> listeners = new CopyOnWriteArraySet<IDataChangeListener>();

	@Override
	public void add(IDataManager dm, IReference subj, IReference pred,
			IValue obj, IReference... contexts) {
		if (contexts == null || contexts.length == 0) {
			contexts = NULL_CONTEXT;
		}
		for (IReference context : contexts) {
			addChange(dm, new AddChange(subj, pred, obj, context));
		}
	}

	private void addChange(IDataManager dm, IDataChange change) {
		synchronized (activeDataManagers) {
			ensureList(activeDataManagers, dm).add(change);
		}
	}

	@Override
	public void addChangeListener(IDataChangeListener changeListener) {
		listeners.add(changeListener);
	}

	@Override
	public void close(IDataManager dm) {
		synchronized (activeDataManagers) {
			activeDataManagers.remove(dm);
		}
	}

	@Override
	public void commit(IDataManager dm) {
		List<IDataChange> committed = null;
		synchronized (activeDataManagers) {
			List<IDataChange> changes = activeDataManagers.get(dm);
			if (changes != null && !changes.isEmpty()) {
				committed = new ArrayList<IDataChange>(changes);
				changes.clear();
			}
		}
		if (committed != null) {
			handleChanges(committed);
		}
	}

	@Override
	public void dataChanged(List<IDataChange> changes) {
		handleChanges(changes);
	}

	private <K, T> List<T> ensureList(Map<K, List<T>> map, K key) {
		List<T> list = map.get(key);
		if (list == null) {
			list = new ArrayList<T>();
			map.put(key, list);
		}
		return list;
	}

	protected void handleChanges(List<IDataChange> committed) {
		notifyListeners(committed);
	}

	@Override
	public boolean isEnabled(IDataManager dm) {
		synchronized (disabledDataManagers) {
			return disabledDataManagers.get(dm) == null;
		}
	}

	protected void notifyListeners(List<IDataChange> changes) {
		for (IDataChangeListener changeListener : listeners) {
			changeListener.dataChanged(changes);
		}
	}

	@Override
	public void remove(IDataManager dm, IReference subj, IReference pred,
			IValue obj, IReference... contexts) {
		if (contexts == null || contexts.length == 0) {
			contexts = NULL_CONTEXT;
		}
		for (IReference context : contexts) {
			addChange(dm, new RemoveChange(subj, pred, obj, context));
		}
	}

	@Override
	public void removeChangeListener(IDataChangeListener changeListener) {
		listeners.remove(changeListener);
	}

	@Override
	public void removeNamespace(IDataManager dm, String prefix, URI namespace) {
		IDataChange change = new OverrideNamespaceChange(prefix, namespace,
				null);
		addChange(dm, change);
	}

	@Override
	public void rollback(IDataManager dm) {
		synchronized (activeDataManagers) {
			List<IDataChange> changes = activeDataManagers.get(dm);
			if (changes != null) {
				changes.clear();
			}
		}
	}

	public void setEnabled(IDataManager dm, boolean enabled) {
		synchronized (disabledDataManagers) {
			if (enabled) {
				disabledDataManagers.remove(dm);
			} else {
				disabledDataManagers.put(dm, true);
			}
		}
	}

	@Override
	public void setNamespace(IDataManager dm, String prefix, URI oldNS,
			URI newNS) {
		IDataChange change = new OverrideNamespaceChange(prefix, oldNS, newNS);
		addChange(dm, change);
	}
}
