/*
 * Copyright James Leigh (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.repository.change;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.event.NotifyingRepository;
import org.openrdf.repository.event.RepositoryConnectionListener;
import org.openrdf.store.StoreException;

import net.enilink.komma.internal.repository.change.AddChange;
import net.enilink.komma.internal.repository.change.OverrideNamespaceChange;
import net.enilink.komma.internal.repository.change.RemoveChange;

/**
 * Tracks changes to a RepositoryConnection. There is a performance hit when
 * this object is attached to a RepositoryConnection. This object depends on
 * {@link NotifyingRepository} to be on the stack. If the repository does not
 * support or have enabled read uncommitted, the
 * {@link StatementRealizerRepository} must be a delegate of
 * {@link NotifyingRepository}.
 * 
 * @author James Leigh
 * @author Ken Wenzel
 * 
 */
public class RepositoryChangeTracker implements RepositoryConnectionListener,
		IRepositoryChangeTracker {
	protected Map<RepositoryConnection, List<IRepositoryChange>> activeConnections = new HashMap<RepositoryConnection, List<IRepositoryChange>>();
	protected StoreException exception;
	private CopyOnWriteArraySet<IRepositoryChangeListener> listeners = new CopyOnWriteArraySet<IRepositoryChangeListener>();

	public void add(RepositoryConnection conn, Resource subj, URI pred,
			Value obj, Resource... contexts) {
		addChange(conn, new AddChange(subj, pred, obj, contexts));
	}

	private void addChange(RepositoryConnection conn, IRepositoryChange change) {
		synchronized (activeConnections) {
			ensureList(activeConnections, conn).add(change);

			// force commit if connection is in auto-commit mode
			try {
				if (conn.isAutoCommit()) {
					commit(conn);
				}
			} catch (StoreException e) {
				// ignore
			}
		}
	}

	@Override
	public void addChangeListener(IRepositoryChangeListener changeListener) {
		listeners.add(changeListener);
	}

	public void begin(RepositoryConnection conn) {
		// do nothing
	}

	public void clear(RepositoryConnection conn, Resource... contexts) {
		throw new UnsupportedOperationException(
				"RepositoryChangeStack is only supported when reporting deltas is enabled");
	}

	public void clearNamespaces(RepositoryConnection conn) {
		throw new UnsupportedOperationException(
				"RepositoryChangeStack is only supported when reporting deltas is enabled");
	}

	public void close(RepositoryConnection conn) {
		synchronized (activeConnections) {
			activeConnections.remove(conn);
		}
	}

	public void commit(RepositoryConnection conn) {
		synchronized (activeConnections) {
			List<IRepositoryChange> changes = activeConnections.get(conn);
			if (changes != null && !changes.isEmpty()) {
				List<IRepositoryChange> committed = new ArrayList<IRepositoryChange>(
						changes);

				handleChanges(committed);

				changes.clear();
			}
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

	protected void handleChanges(List<IRepositoryChange> committed) {
		notifyListeners(committed);
	}

	private void handleException(Exception e) {
		if (e instanceof StoreException) {
			exception = (StoreException) e;
		} else if (e.getCause() instanceof StoreException) {
			exception = (StoreException) e.getCause();
		} else {
			throw new AssertionError(e);
		}
	}

	protected void notifyListeners(Collection<IRepositoryChange> changes) {
		IRepositoryChange[] changesArray = changes
				.toArray(new IRepositoryChange[changes.size()]);
		for (IRepositoryChangeListener changeListener : listeners) {
			changeListener.repositoryChanged(changesArray);
		}
	}

	public void remove(RepositoryConnection conn, Resource subj, URI pred,
			Value obj, Resource... contexts) {
		if (subj == null || pred == null || obj == null || contexts == null
				|| contexts.length == 0) {
			throw new UnsupportedOperationException(
					"RepositoryChangeStack is only supported when reporting deltas is enabled");
		}
		addChange(conn, new RemoveChange(subj, pred, obj, contexts));
	}

	@Override
	public void removeChangeListener(IRepositoryChangeListener changeListener) {
		listeners.remove(changeListener);
	}

	public void removeNamespace(RepositoryConnection conn, String prefix) {
		try {
			String namespace = conn.getNamespace(prefix);
			IRepositoryChange change = new OverrideNamespaceChange(prefix,
					namespace, null);
			addChange(conn, change);
			commit(conn);
		} catch (StoreException e) {
			handleException(e);
		}
	}

	public void rollback(RepositoryConnection conn) {
		synchronized (activeConnections) {
			List<IRepositoryChange> changes = activeConnections.get(conn);
			if (changes != null) {
				changes.clear();
			}
		}
	}

	public void setNamespace(RepositoryConnection conn, String prefix,
			String name) {
		IRepositoryChange change = new OverrideNamespaceChange(prefix, null,
				name);
		addChange(conn, change);
		commit(conn);
	}
}
