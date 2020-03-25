/******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/

package net.enilink.komma.edit.ui.action;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.custom.BusyIndicator;

import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.internal.EditUIStatusCodes;

/**
 * Responsible for managing the running of actions. All
 * actions (delegates and handlers) channel their run requests through an action
 * manager. An action manager keeps track of the action that was last run and
 * fires events to interested listeners whenever an action is run.
 * 
 * @author khussey
 */
public class ActionManager {

	/**
	 * The empty string.
	 */
	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$
	/**
	 * A string containing only a space character.
	 */
	protected static final String SPACE = " "; //$NON-NLS-1$

	/**
	 * The default action manager.
	 */
	private static ActionManager actionManager = null;
    
    private final IOperationHistory operationHistory;

	/**
	 * The last action that was run.
	 */
	private IActionWithProgress action = null;

	/**
	 * The action manager change listeners.
	 */
	private final List<IActionManagerChangeListener> listeners =
		Collections.synchronizedList(new ArrayList<IActionManagerChangeListener>());
    
    /**
     * Intializes me with an operation history.
     * 
     * @param operationHistory The operation history for this action manager.
     */
    public ActionManager(IOperationHistory operationHistory) {
        super();

        assert null != operationHistory;
        this.operationHistory = operationHistory;
    }

	/**
	 * Retrieves the default action manager.
	 * 
	 * @return The default action manager.
	 */
	public static ActionManager getDefault() {
		if (null == actionManager) {
			actionManager = new ActionManager(OperationHistoryFactory.getOperationHistory());
		}

		return actionManager;
	}
    
    /**
     * Gets my operation history.
     * 
     * @return my operation history
     */
    public final IOperationHistory getOperationHistory() {
        return operationHistory;
    }
	
	/**
	 * Retrieves the value of the <code>action</code> instance variable.
	 * 
	 * @return The value of the <code>action</code> instance variable.
	 */
	protected final IActionWithProgress getAction() {
		return action;
	}

	/**
	 * Sets the <code>action</code> instance variable to the specified value.
	 * 
	 * @param action The new value for the <code>action</code> instance
	 *                variable.
	 */
	protected final void setAction(IActionWithProgress action) {
		this.action = action;
	}

	/**
	 * Retrieves the value of the <code>listeners</code> instance variable.
	 * 
	 * @return The value of the <code>listeners</code> instance varible.
	 */
	protected final List<IActionManagerChangeListener> getListeners() {
		return listeners;
	}

	/**
	 * Adds the specified listener to the list of action manager change
	 * listeners for this action manager.
	 * 
	 * @param listener The listener to be added.
	 */
	public void addActionManagerChangeListener(IActionManagerChangeListener listener) {
		assert null != listener;

		getListeners().add(listener);
	}

	/**
	 * Removes the specified listener from the list of action manager change
	 * listeners for this action manager.
	 * 
	 * @param listener The listener to be removed.
	 */
	public void removeActionManagerChangeListener(IActionManagerChangeListener listener) {
		assert null != listener;

		getListeners().remove(listener);
	}

	/**
	 * Notifies the listeners for this action manager that the specified
	 * event has occurred.
	 * 
	 * @param event The action manager change event to be fired.
	 */
	protected void fireActionManagerChange(ActionManagerChangeEvent event) {
		assert null != event;

		List<IActionManagerChangeListener> targets = null;
		synchronized (getListeners()) {
			targets = new ArrayList<IActionManagerChangeListener>(getListeners());
		}

		for (Iterator<IActionManagerChangeListener> i = targets.iterator(); i.hasNext();) {
			i.next().actionManagerChanged(
				event);
		}
	}

	/**
	 * Clears this action manager by discarding the last action that was run.
	 */
	public void clear() {
		setAction(null);

		fireActionManagerChange(new ActionManagerChangeEvent(this));
	}

	/**
	 * Runs the specified action.
	 * 
	 * @param theAction The action to be run.
	 * @exception UnsupportedOperationException If the action cannot be run.
	 * @exception RuntimeException if any exception or error occurs 
	 * 									   while running the action
	 */
	public void run(final IActionWithProgress theAction) {
		if (!theAction.isRunnable()) {
			UnsupportedOperationException uoe =
				new UnsupportedOperationException();
			throw uoe;
		}
		
		boolean setup = theAction.setup();
		if (!setup) {
			// The setup did not occur (e.g. the user cancelled
			// a dialog presented in the setup). Do not proceed.
			return;
		}

		IActionWithProgress.WorkIndicatorType type =
			theAction.getWorkIndicatorType();

		if (type == IActionWithProgress.WorkIndicatorType.PROGRESS_MONITOR) {
			runActionInProgressMonitorDialog(theAction, false);

		} else if (
			type
				== IActionWithProgress
					.WorkIndicatorType
					.CANCELABLE_PROGRESS_MONITOR) {
			runActionInProgressMonitorDialog(theAction, true);

		} else if (type == IActionWithProgress.WorkIndicatorType.BUSY) {
			// display hourglass cursor
			BusyIndicator.showWhile(null, new Runnable() {
				public void run() {
					theAction.run(new NullProgressMonitor());
				}
			});
		} else {
			theAction.run(new NullProgressMonitor());
		}

		setAction(theAction);

		fireActionManagerChange(new ActionManagerChangeEvent(this, theAction));
	}

	/**
	 * Runs <code>runnable</code> in a progress monitor dialog. The runnable runs in
	 * the same thread as the dialog. The cancel button on the dialog is enabled
	 * if <code>cancelable</code> is <code>true</code>. 
	 * 
	 * @param runnable the runnable to run in the context of the progress dialog
	 * @param cancelable <code>true</code> if the progress monitor should have
	 * 					  an enabled cancel button, <code>false</code> otherwise.
	 * 
	 * @exception RuntimeException if any exception or error occurs 
	 * 									   while running the runnable
	 */
	private void runInProgressMonitorDialog(
		IRunnableWithProgress runnable,
		boolean cancelable) {

		try {
			if (System.getProperty("RUN_PROGRESS_IN_UI_HACK") != null) { //$NON-NLS-1$
				new ProgressMonitorDialog(null).run(false, cancelable, runnable);
			} else {
				new ProgressMonitorDialog(null).run(true, cancelable, runnable);
			}

		} catch (InvocationTargetException ite) {
			KommaEditUIPlugin.getPlugin().log(new Status(IStatus.ERROR, KommaEditUIPlugin.PLUGIN_ID,
					EditUIStatusCodes.SERVICE_FAILURE, "run", ite));

			RuntimeException cre =
				new RuntimeException(ite.getTargetException());

			throw cre;
		} catch (InterruptedException ie) {
		}
	}

	/**
	 * Runs <code>action</code> in the context of a progress monitor dialog.
	 * The action runs in the same thread as the dialog. The cancel button on
	 * the dialog is enabled if <code>cancelable</code> is <code>true</code>. 
	 * 
	 * @param act the action to repeat
	 * @param cancelable <code>true</code> if the progress monitor should have
	 * 					  an enabled cancel button, <code>false</code> otherwise.
	 * 
	 * @exception RuntimeException if any exception or error occurs 
	 * 									   while running the action
	 */
	private void runActionInProgressMonitorDialog(
		final IActionWithProgress act,
		boolean cancelable) {

		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				act.run(monitor);
			}
		};
		runInProgressMonitorDialog(runnable, cancelable);
	}
}
