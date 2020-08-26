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

import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.URI;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.internal.change.AddChange;
import net.enilink.komma.dm.internal.change.NamespaceChange;
import net.enilink.komma.dm.internal.change.RemoveChange;

/**
 * Tracks changes to an {@link IDataManager} by implementing the interface
 * {@link IDataChangeSupport}.
 * 
 */
public class DataChangeSupport implements IDataChangeSupport {
	static class Options {
		volatile Boolean enabled;
		volatile Mode mode;
	}

	protected Map<IDataManager, List<IDataChange>> activeDataManagers = new HashMap<>();
	protected WeakHashMap<IDataManager, Options> dataManagerOptions = new WeakHashMap<>();

	private CopyOnWriteArraySet<IDataChangeListener> listeners = new CopyOnWriteArraySet<>();
	private CopyOnWriteArraySet<IDataChangeListener> internalListeners = new CopyOnWriteArraySet<>();

	private volatile boolean defaultEnabled = true;
	private ThreadLocal<Boolean> perThreadEnabled = new ThreadLocal<>();

	private volatile Mode defaultMode = Mode.VERIFY_NONE;
	private ThreadLocal<Mode> perThreadMode = new ThreadLocal<>();

	@Override
	public void add(IDataManager dm, IStatement stmt) {
		addChange(dm, new AddChange(stmt));
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

	public void addInternalChangeListener(IDataChangeListener changeListener) {
		internalListeners.add(changeListener);
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

	private <K, T> List<T> ensureList(Map<K, List<T>> map, K key) {
		List<T> list = map.get(key);
		if (list == null) {
			list = new ArrayList<T>();
			map.put(key, list);
		}
		return list;
	}

	@Override
	public boolean getDefaultEnabled() {
		return defaultEnabled;
	}

	@Override
	public Mode getDefaultMode() {
		return defaultMode;
	}

	@Override
	public Mode getMode(IDataManager dm) {
		Mode perThreadModeValue = perThreadMode.get();
		if (perThreadModeValue != null) {
			return perThreadModeValue;
		} else {
			synchronized (dataManagerOptions) {
				Options options = dataManagerOptions.get(dm);
				return options == null || options.mode == null ? defaultMode : options.mode;
			}
		}
	}

	protected void handleChanges(List<IDataChange> committed) {
		notifyListeners(committed);
	}

	@Override
	public boolean isEnabled(IDataManager dm) {
		Boolean perThreadEnabledValue = perThreadEnabled.get();
		if (perThreadEnabledValue != null) {
			return perThreadEnabledValue.booleanValue();
		} else {
			synchronized (dataManagerOptions) {
				Options options = dataManagerOptions.get(dm);
				return options == null || options.enabled == null ? defaultEnabled : options.enabled;
			}
		}
	}

	protected void notifyListeners(List<IDataChange> changes) {
		for (IDataChangeListener internalChangeListener : internalListeners) {
			internalChangeListener.dataChanged(changes);
		}
		for (IDataChangeListener changeListener : listeners) {
			changeListener.dataChanged(changes);
		}
	}

	@Override
	public void remove(IDataManager dm, IStatement stmt) {
		addChange(dm, new RemoveChange(stmt));
	}

	@Override
	public void removeChangeListener(IDataChangeListener changeListener) {
		listeners.remove(changeListener);
	}

	public void removeInternalChangeListener(IDataChangeListener changeListener) {
		internalListeners.remove(changeListener);
	}

	@Override
	public void removeNamespace(IDataManager dm, String prefix, URI namespace) {
		IDataChange change = new NamespaceChange(prefix, namespace,
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

	@Override
	public void setDefaultEnabled(boolean enabled) {
		this.defaultEnabled = enabled;
	}

	@Override
	public void setDefaultMode(Mode mode) {
		this.defaultMode = mode;
	}

	public void setEnabled(IDataManager dm, boolean enabled) {
		if (dm == null) {
			perThreadEnabled.set(enabled);
		} else {
			synchronized (dataManagerOptions) {
				Options options = dataManagerOptions.get(dm);
				if (options == null) {
					options = new Options();
					dataManagerOptions.put(dm, options);
				}
				options.enabled = enabled;
			}
		}
	}

	@Override
	public void setMode(IDataManager dm, Mode mode) {
		if (dm == null) {
			perThreadMode.set(mode);
		} else {
			synchronized (dataManagerOptions) {
				Options options = dataManagerOptions.get(dm);
				if (options == null) {
					options = new Options();
					dataManagerOptions.put(dm, options);
				}
				options.mode = mode;
			}
		}
	}

	@Override
	public void setNamespace(IDataManager dm, String prefix, URI oldNS,
			URI newNS) {
		IDataChange change = new NamespaceChange(prefix, oldNS, newNS);
		addChange(dm, change);
	}
}
