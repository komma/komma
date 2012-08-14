package net.enilink.komma.util;

import java.util.ArrayList;
import java.util.List;

import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.core.IUnitOfWork;

public class UnitOfWork implements IUnitOfWork {
	private ThreadLocal<Integer> countLocal = new ThreadLocal<Integer>();
	private ThreadLocal<List<IDataManager>> trackedManagersLocal = new ThreadLocal<List<IDataManager>>();

	public void addManager(IDataManager manager) {
		List<IDataManager> trackedManagers = trackedManagersLocal.get();
		if (trackedManagers == null) {
			trackedManagers = new ArrayList<IDataManager>();
			trackedManagersLocal.set(trackedManagers);
		}
		trackedManagers.add(manager);
	}

	@Override
	public void begin() {
		Integer count = countLocal.get();
		countLocal.set(count == null ? 1 : count + 1);
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
			List<IDataManager> trackedManagers = trackedManagersLocal.get();
			if (trackedManagers != null) {
				for (IDataManager manager : trackedManagers) {
					if (manager.isOpen()) {
						manager.close();
					}
				}
				trackedManagers.clear();
			}
			trackedManagersLocal.remove();
			countLocal.remove();
		}
	}

	/**
	 * Tests if this unit of work is currently active.
	 */
	public boolean isActive() {
		return countLocal.get() != null;
	}
}