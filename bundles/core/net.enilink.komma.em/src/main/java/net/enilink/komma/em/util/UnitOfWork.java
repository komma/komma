package net.enilink.komma.em.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.enilink.komma.core.IUnitOfWork;

public class UnitOfWork implements IUnitOfWork {
	private ThreadLocal<Integer> countLocal = new ThreadLocal<>();
	private ThreadLocal<List<AutoCloseable>> trackedCloseablesLocal = new ThreadLocal<>();

	// track client threads for debugging purposes
	private Set<Thread> clientThreads = Collections
			.newSetFromMap(new ConcurrentHashMap<Thread, Boolean>());
	private final boolean debug;

	public UnitOfWork() {
		this(false);
	}

	public UnitOfWork(boolean debug) {
		this.debug = debug;
	}

	public void addCloseable(AutoCloseable closeable) {
		List<AutoCloseable> trackedCloseables = trackedCloseablesLocal.get();
		if (trackedCloseables == null) {
			trackedCloseables = new ArrayList<>();
			trackedCloseablesLocal.set(trackedCloseables);
		}
		trackedCloseables.add(closeable);
	}

	@Override
	public void begin() {
		Integer count = countLocal.get();
		countLocal.set(count == null ? 1 : count + 1);
		if (debug) {
			clientThreads.add(Thread.currentThread());
		}
	}

	@Override
	public void end() {
		Integer count = countLocal.get();
		if (count == null) {
			return;
		}
		int newCount = count - 1;
		countLocal.set(newCount);
		if (newCount <= 0) {
			List<AutoCloseable> trackedCloseables = trackedCloseablesLocal
					.get();
			if (trackedCloseables != null) {
				for (AutoCloseable closeable : trackedCloseables) {
					try {
						closeable.close();
					} catch (Exception e) {
						// ignore
					}
				}
				trackedCloseables.clear();
			}
			trackedCloseablesLocal.remove();
			countLocal.remove();

			if (debug) {
				clientThreads.remove(Thread.currentThread());
			}
		}
	}

	/**
	 * Tests if this unit of work is currently active.
	 */
	public boolean isActive() {
		return countLocal.get() != null;
	}

	public Collection<Thread> getClientThreads() {
		return Collections.unmodifiableCollection(clientThreads);
	}
}