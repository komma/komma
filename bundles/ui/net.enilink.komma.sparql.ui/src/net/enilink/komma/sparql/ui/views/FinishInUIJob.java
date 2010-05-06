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
package net.enilink.komma.sparql.ui.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public abstract class FinishInUIJob extends Job {
	protected FinishInUIJob(String name) {
		super(name);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final IStatus[] result = new IStatus[1];
		try {
			result[0] = runAsync(monitor);
			return result[0];
		} finally {
			if (PlatformUI.isWorkbenchRunning()) {
				Display display = PlatformUI.getWorkbench().getDisplay();
				if (display != null) {
					display.syncExec(new Runnable() {
						public void run() {
							finishInUI(result[0]);
						}
					});
				}
			}
		}
	}

	protected void runAsyncInUI(Runnable runnable) {
		if (PlatformUI.isWorkbenchRunning()) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			if (display != null) {
				display.asyncExec(runnable);
			}
		}
	}

	protected void runSyncInUI(Runnable runnable) {
		if (PlatformUI.isWorkbenchRunning()) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			if (display != null) {
				display.syncExec(runnable);
			}
		}
	}

	public abstract IStatus runAsync(IProgressMonitor monitor);

	/**
	 * Finishes the job. Called unconditionally, check the status before doing
	 * anything.
	 * 
	 * @param status
	 *            The status as set by runAsync. Needs to be checked.
	 */
	public abstract void finishInUI(IStatus status);
}
