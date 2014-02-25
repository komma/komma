package net.enilink.komma.edit.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import net.enilink.komma.common.util.ICollector;

/**
 * Asynchronous {@link ICollector} implementation.
 * 
 * The job's {@link Job#run(IProgressMonitor)} method should execute the root
 * method that receives this collector.
 */
abstract public class CollectorJob<T> extends Job implements ICollector<T> {
	private Long begin;
	private boolean canceled;
	private final long delay = 200;

	protected volatile boolean done;

	private List<T> objects = new ArrayList<T>();

	public CollectorJob(String name) {
		super(name);
	}

	@Override
	public void add(Iterable<T> elements) {
		for (T element : elements) {
			add(element);
		}
	}

	@Override
	public void add(T element) {
		synchronized (objects) {
			objects.add(element);
		}
		if (begin == null) {
			begin = System.currentTimeMillis();
		} else if (System.currentTimeMillis() - begin >= delay) {
			// reset begin
			begin = System.currentTimeMillis();
			handleObjects();
		}
	}

	@Override
	protected void canceling() {
		super.canceling();
		canceled = true;
	}

	@Override
	public boolean cancelled() {
		return canceled;
	}

	/**
	 * The element collection is done. Clean up any temporary state.
	 * 
	 */
	protected void done() {
		done = true;
		handleObjects();
	}

	private void handleObjects() {
		boolean empty;
		synchronized (objects) {
			empty = objects.isEmpty();
		}
		if (!cancelled() && (!empty || done)) {
			final List<T> objectsSnapshot;
			synchronized (objects) {
				objectsSnapshot = new ArrayList<T>(objects);
				objects.clear();
			}
			handleObjects(objectsSnapshot);
		}
	}

	protected abstract void handleObjects(Collection<T> objects);
}