package net.enilink.komma.util;

import java.util.ArrayList;
import java.util.List;

import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IUnitOfWork;

public class UnitOfWork implements IUnitOfWork {
	private ThreadLocal<Integer> countLocal = new ThreadLocal<Integer>();
	private ThreadLocal<List<IEntityManager>> trackedManagersLocal = new ThreadLocal<List<IEntityManager>>();

	@Override
	public void begin() {
		Integer count = countLocal.get();
		countLocal.set(count == null ? 1 : count + 1);
	}

	public void addManager(IEntityManager manager) {
		List<IEntityManager> trackedManagers = trackedManagersLocal.get();
		if (trackedManagers == null) {
			trackedManagers = new ArrayList<IEntityManager>();
			trackedManagersLocal.set(trackedManagers);
		}
		trackedManagers.add(manager);
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
			List<IEntityManager> trackedManagers = trackedManagersLocal.get();
			if (trackedManagers != null) {
				for (IEntityManager manager : trackedManagers) {
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
}