/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.ui.progress;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitorWithBlocking;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Display;

/**
 * An wrapper around a progress monitor which, unless overridden, forwards
 * <code>IProgressMonitor</code> and <code>IProgressMonitorWithBlocking</code>
 * methods to the wrapped progress monitor and runs this methods in the display
 * thread.
 * <p>
 * This class can be used without OSGi running.
 * </p>
 * <p>
 * Clients may subclass.
 * </p>
 */
public class UiProgressMonitorWrapper implements IProgressMonitor,
		IProgressMonitorWithBlocking {

	/** The wrapped progress monitor. */
	private IProgressMonitor progressMonitor;
	private Display display;

	/**
	 * Creates a new wrapper around the given monitor.
	 * 
	 * @param monitor
	 *            the progress monitor to forward to
	 */
	public UiProgressMonitorWrapper(IProgressMonitor monitor, Display display) {
		Assert.isNotNull(monitor);
		Assert.isNotNull(display);
		progressMonitor = monitor;
		this.display = display;
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code> method forwards to
	 * the wrapped progress monitor. Clients may override this method to do
	 * additional processing.
	 * 
	 * @see IProgressMonitor#beginTask(String, int)
	 */
	public void beginTask(final String name, final int totalWork) {
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					progressMonitor.beginTask(name, totalWork);
				}
			});
		}
	}

	/**
	 * This implementation of a <code>IProgressMonitorWithBlocking</code> method
	 * forwards to the wrapped progress monitor. Clients may override this
	 * method to do additional processing.
	 * 
	 * @see IProgressMonitorWithBlocking#clearBlocked()
	 * @since 3.0
	 */
	public void clearBlocked() {
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (progressMonitor instanceof IProgressMonitorWithBlocking)
						((IProgressMonitorWithBlocking) progressMonitor)
								.clearBlocked();
				}
			});
		}
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code> method forwards to
	 * the wrapped progress monitor. Clients may override this method to do
	 * additional processing.
	 * 
	 * @see IProgressMonitor#done()
	 */
	public void done() {
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					progressMonitor.done();
				}
			});
		}
	}

	/**
	 * Returns the wrapped progress monitor.
	 * 
	 * @return the wrapped progress monitor
	 */
	public IProgressMonitor getWrappedProgressMonitor() {
		return progressMonitor;
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code> method forwards to
	 * the wrapped progress monitor. Clients may override this method to do
	 * additional processing.
	 * 
	 * @see IProgressMonitor#internalWorked(double)
	 */
	public void internalWorked(final double work) {
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					progressMonitor.internalWorked(work);
				}
			});
		}
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code> method forwards to
	 * the wrapped progress monitor. Clients may override this method to do
	 * additional processing.
	 * 
	 * @see IProgressMonitor#isCanceled()
	 */
	public boolean isCanceled() {
		final boolean[] canceled = { false };
		if (!display.isDisposed()) {
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					canceled[0] = progressMonitor.isCanceled();
				}
			});
		}

		return canceled[0];
	}

	/**
	 * This implementation of a <code>IProgressMonitorWithBlocking</code> method
	 * forwards to the wrapped progress monitor. Clients may override this
	 * method to do additional processing.
	 * 
	 * @see IProgressMonitorWithBlocking#setBlocked(IStatus)
	 * @since 3.0
	 */
	public void setBlocked(final IStatus reason) {
		if (!display.isDisposed()) {
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					if (progressMonitor instanceof IProgressMonitorWithBlocking)
						((IProgressMonitorWithBlocking) progressMonitor)
								.setBlocked(reason);
				}
			});
		}
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code> method forwards to
	 * the wrapped progress monitor. Clients may override this method to do
	 * additional processing.
	 * 
	 * @see IProgressMonitor#setCanceled(boolean)
	 */
	public void setCanceled(final boolean b) {
		if (!display.isDisposed()) {
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					progressMonitor.setCanceled(b);
				}
			});
		}
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code> method forwards to
	 * the wrapped progress monitor. Clients may override this method to do
	 * additional processing.
	 * 
	 * @see IProgressMonitor#setTaskName(String)
	 */
	public void setTaskName(final String name) {
		if (!display.isDisposed()) {
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					progressMonitor.setTaskName(name);
				}
			});
		}
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code> method forwards to
	 * the wrapped progress monitor. Clients may override this method to do
	 * additional processing.
	 * 
	 * @see IProgressMonitor#subTask(String)
	 */
	public void subTask(final String name) {
		if (!display.isDisposed()) {
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					progressMonitor.subTask(name);
				}
			});
		}
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code> method forwards to
	 * the wrapped progress monitor. Clients may override this method to do
	 * additional processing.
	 * 
	 * @see IProgressMonitor#worked(int)
	 */
	public void worked(final int work) {
		if (!display.isDisposed()) {
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					progressMonitor.worked(work);
				}
			});
		}
	}
}
