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

package net.enilink.komma.edit.ui.commands;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import net.enilink.komma.common.util.StringStatics;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.internal.EditUIStatusCodes;
import net.enilink.komma.edit.ui.util.StatusLineUtil;

public abstract class AbstractHandler extends
		org.eclipse.core.commands.AbstractHandler {
	/**
	 * Enumerated type for work indicator type
	 */
	public static enum WorkIndicatorType {
		/** No work indicator. */
		NONE("None"),

		/** Busy work indicator. */
		BUSY("Busy"),

		/** Progress monitor work indicator. */
		PROGRESS_MONITOR("Progress Monitor"),

		/** Cancelable progress monitor work indicator. */
		CANCELABLE_PROGRESS_MONITOR("Cancelable Progress Monitor");

		String name;

		/**
		 * Constructor for WorkIndicatorType.
		 * 
		 * @param name
		 *            The name for the WorkIndicatorType
		 */
		WorkIndicatorType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	/**
	 * Runs the specified action.
	 * 
	 * @exception UnsupportedOperationException
	 *                If the action cannot be run.
	 * @exception RuntimeException
	 *                if any exception or error occurs while running the action
	 */
	private void doExecute(final ExecutionEvent event)
			throws ExecutionException {
		WorkIndicatorType type = getWorkIndicatorType();
		switch (type) {
		case PROGRESS_MONITOR:
			runInProgressMonitorDialog(event, false);
			break;
		case CANCELABLE_PROGRESS_MONITOR:
			runInProgressMonitorDialog(event, true);
			break;
		case BUSY:
			// display hourglass cursor
			BusyIndicator.showWhile(null, new Runnable() {
				public void run() {
					try {
						execute(event, new NullProgressMonitor());
					} catch (ExecutionException e) {
						throw new RuntimeException(e);
					}
				}
			});
			break;
		default:
			execute(event, new NullProgressMonitor());
		}
	}

	/**
	 * Executes this handler.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			StatusLineUtil.outputErrorMessage(getWorkbenchPart(event),
					StringStatics.BLANK);
			doExecute(event);
		} catch (Exception e) {
			handle(event, e);
		}
		return null;
	}

	/**
	 * Performs the actual work when this action handler is run. Subclasses must
	 * override this method to do some work.
	 * 
	 * @param progressMonitor
	 *            the progress monitor for tracking the progress of this action
	 *            when it is run.
	 */
	abstract public void execute(ExecutionEvent event, IProgressMonitor monitor)
			throws ExecutionException;

	private Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null && PlatformUI.isWorkbenchRunning()) {
			display = PlatformUI.getWorkbench().getDisplay();
		}
		return display;
	}

	/**
	 * Retrieves the label for this action handler.
	 * 
	 * @return The label for this action handler.
	 */
	public String getLabel(ExecutionEvent event) {
		try {
			return event.getCommand().getName();
		} catch (NotDefinedException e) {
			return null;
		}
	}

	/**
	 * Returns the editing domain which is associated with the current active
	 * workbench part.
	 */
	protected IEditingDomain getEditingDomain(ExecutionEvent event)
			throws ExecutionException {
		IWorkbenchPart wbPart = getWorkbenchPart(event);

		IEditingDomainProvider provider = null;
		if (wbPart != null) {
			provider = (IEditingDomainProvider) wbPart
					.getAdapter(IEditingDomainProvider.class);
		}

		return provider != null ? provider.getEditingDomain() : null;
	}

	/**
	 * Returns the editing domain which is associated with the current active
	 * workbench part.
	 * 
	 * @throws ExecutionException
	 *             if no editing domain was found for the current execution
	 *             context
	 */
	protected IEditingDomain getEditingDomainChecked(ExecutionEvent event)
			throws ExecutionException {
		IEditingDomain editingDomain = getEditingDomain(event);
		if (editingDomain == null) {
			throw new ExecutionException(
					"No editing domain found for the current execution context.");
		}
		return editingDomain;
	}

	/**
	 * Returns the operation history for this action handler from its action
	 * manager.
	 * 
	 * @return the operation history
	 */
	protected IOperationHistory getOperationHistory(ExecutionEvent event)
			throws ExecutionException {
		IOperationHistory history = null;
		IWorkbenchPart wbPart = getWorkbenchPart(event);
		if (wbPart != null) {
			history = (IOperationHistory) wbPart
					.getAdapter(IOperationHistory.class);
		}

		return null == history ? OperationHistoryFactory.getOperationHistory()
				: history;
	}

	/**
	 * Retrieves the current selection.
	 * 
	 * @return The current selection.
	 */
	protected ISelection getSelection(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			return (IStructuredSelection) selection;
		}
		return StructuredSelection.EMPTY;
	}

	/**
	 * Retrieves the current structured selection.
	 * 
	 * @return The current structured selection.
	 */
	protected IStructuredSelection getStructuredSelection(ExecutionEvent event) {
		ISelection selection = getSelection(event);
		return (selection instanceof StructuredSelection) ? (StructuredSelection) selection
				: StructuredSelection.EMPTY;
	}

	/**
	 * Gets the undo context from my workbench part.
	 * 
	 * @return the undo context
	 */
	protected IUndoContext getUndoContext(ExecutionEvent event)
			throws ExecutionException {
		IWorkbenchPart part = getWorkbenchPart(event);

		if (part != null) {
			return (IUndoContext) part.getAdapter(IUndoContext.class);
		}

		return null;
	}

	/**
	 * Retrieves the value of the <code>workbenchPart</code> instance variable.
	 * 
	 * @return The value of the <code>workbenchPart</code> instance variable.
	 */
	protected final IWorkbenchPart getWorkbenchPart(ExecutionEvent event)
			throws ExecutionException {
		return HandlerUtil.getActivePartChecked(event);
	}

	/**
	 * Gets type of work indicator (progress monitor, hourglass, or none).
	 * 
	 * @return type of work indicator
	 */
	public WorkIndicatorType getWorkIndicatorType() {
		return WorkIndicatorType.BUSY;
	}

	/**
	 * Handles the specified exception.
	 * 
	 * @param event
	 * 
	 * @param exception
	 *            The exception to be handled.
	 */
	protected void handle(ExecutionEvent event, Exception exception) {
		IStatus status = new Status(IStatus.ERROR, KommaEditUIPlugin.PLUGIN_ID,
				EditUIStatusCodes.ACTION_FAILURE, String.valueOf(exception
						.getMessage()), exception);

		KommaEditUIPlugin.getPlugin().log(status);
		openErrorDialog(event, status);
	}

	/**
	 * Opens an error dialog for the specified status object.
	 * 
	 * @param event
	 * 
	 * @param status
	 *            The status object for which to open an error dialog.
	 * 
	 */
	protected void openErrorDialog(final ExecutionEvent event,
			final IStatus status) {
		final Display display = getDisplay();

		if (display.getThread() == Thread.currentThread()) {
			// we're already on the UI thread
			ErrorDialog.openError(display.getActiveShell(), Action
					.removeMnemonics(getLabel(event)), null, status);
		} else {
			// we're not on the UI thread
			display.asyncExec(new Runnable() {
				public void run() {
					ErrorDialog.openError(display.getActiveShell(), Action
							.removeMnemonics(getLabel(event)), null, status);
				}
			});
		}
	}

	// /**
	// * Refreshes me if the history event has my workbench part's context, and
	// * the event is one of:
	// * <UL>
	// * <LI>{@link OperationHistoryEvent#UNDONE}</LI>
	// * <LI>{@link OperationHistoryEvent#REDONE}</LI>
	// * <LI>{@link OperationHistoryEvent#OPERATION_ADDED}</LI>
	// * <LI>{@link OperationHistoryEvent#OPERATION_CHANGED}</LI>
	// * <LI>{@link OperationHistoryEvent#OPERATION_NOT_OK}</LI>
	// * <LI>{@link OperationHistoryEvent#OPERATION_REMOVED}</LI>
	// * </UL>
	// * The other operation history events are ignored because they are
	// * intermediate events that will be followed by one of those listed above.
	// * We only want to refresh the action handler once for each change to the
	// * operation history.
	// */
	// public void historyNotification(OperationHistoryEvent event) {
	// int type = event.getEventType();
	// if (type == OperationHistoryEvent.UNDONE
	// || type == OperationHistoryEvent.REDONE
	// || type == OperationHistoryEvent.DONE
	// || type == OperationHistoryEvent.OPERATION_ADDED
	// || type == OperationHistoryEvent.OPERATION_CHANGED
	// || type == OperationHistoryEvent.OPERATION_NOT_OK
	// || type == OperationHistoryEvent.OPERATION_REMOVED) {
	//
	// IUndoableOperation operation = event.getOperation();
	//
	// if (operation != null) {
	// IUndoContext partContext = getUndoContext(event);
	//
	// if (partContext != null && operation.hasContext(partContext)) {
	// refresh();
	// }
	// }
	// }
	// }

	/**
	 * Runs <code>action</code> in the context of a progress monitor dialog. The
	 * action runs in the same thread as the dialog. The cancel button on the
	 * dialog is enabled if <code>cancelable</code> is <code>true</code>.
	 * 
	 * @param act
	 *            the action to repeat
	 * @param cancelable
	 *            <code>true</code> if the progress monitor should have an
	 *            enabled cancel button, <code>false</code> otherwise.
	 * 
	 * @exception RuntimeException
	 *                if any exception or error occurs while running the action
	 */
	private void runInProgressMonitorDialog(final ExecutionEvent event,
			boolean cancelable) {
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
				try {
					execute(event, monitor);
				} catch (ExecutionException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		runInProgressMonitorDialog(runnable, cancelable);
	}

	/**
	 * Runs <code>runnable</code> in a progress monitor dialog. The runnable
	 * runs in the same thread as the dialog. The cancel button on the dialog is
	 * enabled if <code>cancelable</code> is <code>true</code>.
	 * 
	 * @param runnable
	 *            the runnable to run in the context of the progress dialog
	 * @param cancelable
	 *            <code>true</code> if the progress monitor should have an
	 *            enabled cancel button, <code>false</code> otherwise.
	 * 
	 * @exception RuntimeException
	 *                if any exception or error occurs while running the
	 *                runnable
	 */
	private void runInProgressMonitorDialog(IRunnableWithProgress runnable,
			boolean cancelable) {

		try {
			if (System.getProperty("RUN_PROGRESS_IN_UI_HACK") != null) { //$NON-NLS-1$
				new ProgressMonitorDialog(null)
						.run(false, cancelable, runnable);
			} else {
				new ProgressMonitorDialog(null).run(true, cancelable, runnable);
			}
		} catch (InvocationTargetException ite) {
			KommaEditUIPlugin.getPlugin().log(new Status(IStatus.ERROR, KommaEditUIPlugin.PLUGIN_ID,
					EditUIStatusCodes.SERVICE_FAILURE, "run", ite)); //$NON-NLS-1$

			RuntimeException cre = new RuntimeException(ite.getTargetException());
			throw cre;
		} catch (InterruptedException ie) {
		}
	}
}