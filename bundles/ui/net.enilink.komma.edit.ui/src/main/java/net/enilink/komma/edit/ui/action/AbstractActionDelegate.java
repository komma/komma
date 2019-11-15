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

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import net.enilink.komma.common.util.Log;
import net.enilink.komma.common.util.Trace;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.internal.EditUIDebugOptions;
import net.enilink.komma.edit.ui.internal.EditUIStatusCodes;

/**
 * The abstract parent of all concrete action delegates that execute commands.
 * Logging and exception handling are done in a uniform way in the
 * <code>run()</code> method. Concrete subclasses must provide a definition of
 * the <code>doRun()</code> method to gather any required input and execute a
 * command. As an implementer of the <code>IRepeatableAction</code> interface,
 * this class implements the <code>isRepeatable()</code> method to return
 * <code>true</code> if its plug-in action is enabled, and implements the
 * <code>repeat()</code> method to run the delegate's action. Subclasses that
 * aren't repeatable or require special repeat behavior must override the
 * default implementations of these interface methods.
 * <p>
 * This class provides definitions for the methods that are found in four of the
 * five action delegate interfaces in Eclipse. Hence, in most cases, adding a
 * new action delegate is simply a matter of subclassing this class and
 * declaring that the new class implements the desired action delegate
 * interface.
 * 
 * @author khussey
 * 
 * @see org.eclipse.ui.IActionDelegate
 * @see org.eclipse.ui.IEditorActionDelegate
 * @see org.eclipse.ui.IObjectActionDelegate
 * @see org.eclipse.ui.IViewActionDelegate
 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate
 * @see org.eclipse.ui.IActionDelegate2
 */
public abstract class AbstractActionDelegate implements IPartListener,
		IActionWithProgress {

	/**
	 * Flag to indicate whether or not this action has been set up.
	 */
	private boolean setup;

	/**
	 * The action for which this is a delegate.
	 */
	private IAction action = null;

	/**
	 * The workbench part to which this action delegate applies.
	 */
	private IWorkbenchPart workbenchPart = null;

	/**
	 * The workbench window to which this action delegate applies.
	 */
	private IWorkbenchWindow workbenchWindow = null;

	/**
	 * Constructs a new action delegate.
	 */
	protected AbstractActionDelegate() {
		super();
		setSetup(false);
	}

	/**
	 * Retrieves the value of the <code>action</code> instance variable.
	 * 
	 * @return The value of the <code>action</code> instance variable.
	 */
	protected final IAction getAction() {
		return action;
	}

	/**
	 * Sets the <code>action</code> instance variable to the specified value.
	 * 
	 * @param action
	 *            The new value for the <code>action</code> instance variable.
	 */
	protected final void setAction(IAction action) {
		this.action = action;
	}

	/**
	 * Retrieves the value of the <code>workbenchPart</code> instance variable.
	 * 
	 * @return The value of the <code>workbenchPart</code> instance variable.
	 */
	protected final IWorkbenchPart getWorkbenchPart() {
		return workbenchPart;
	}

	/**
	 * Sets the <code>workbenchPart</code> instance variable to the specified
	 * value.
	 * 
	 * @param workbenchPart
	 *            The new value for the <code>workbenchPart</code> instance
	 *            variable.
	 */
	protected void setWorkbenchPart(IWorkbenchPart workbenchPart) {
		this.workbenchPart = workbenchPart;
	}

	/**
	 * Retrieves the value of the <code>workbenchWindow</code> instance
	 * variable.
	 * 
	 * @return The value of the <code>workbenchWindow</code> instance variable.
	 */
	protected final IWorkbenchWindow getWorkbenchWindow() {
		return workbenchWindow;
	}

	/**
	 * Sets the <code>workbenchWindow</code> instance variable to the specified
	 * value.
	 * 
	 * @param workbenchWindow
	 *            The new value for the <code>workbenchWindow</code> instance
	 *            variable.
	 */
	protected final void setWorkbenchWindow(IWorkbenchWindow workbenchWindow) {
		this.workbenchWindow = workbenchWindow;
	}

	/**
	 * Retrieves the action manager for this action delegate from its workbench
	 * part.
	 * 
	 * @return The action manager for this action delegate.
	 */
	protected ActionManager getActionManager() {
		IWorkbenchPart wbp = getWorkbenchPart();
		if (wbp != null) {
			ActionManager manager = (ActionManager) wbp
					.getAdapter(ActionManager.class);
			if (manager != null) {
				return manager;
			}
		}
		return ActionManager.getDefault();
	}

	/**
	 * Gets the operation history for this action delegate from its action
	 * manager.
	 * 
	 * @return the operation history
	 */
	protected IOperationHistory getOperationHistory() {
		return getActionManager().getOperationHistory();
	}

	/**
	 * Retrieves the current selection.
	 * 
	 * @return The current selection.
	 */
	protected ISelection getSelection() {
		ISelection selection = null;
		IWorkbenchPart wbp = getWorkbenchPart();
		if (wbp != null) {
			IWorkbenchPartSite wbps = wbp.getSite();
			if (wbps != null) {
				ISelectionProvider selectionProvider = wbps
						.getSelectionProvider();
				if (selectionProvider != null) {
					selection = selectionProvider.getSelection();
					if (selection != null)
						return selection;
				}
			}
		}
		return StructuredSelection.EMPTY;
	}

	/**
	 * Retrieves the current structured selection.
	 * 
	 * @return The current structured selection.
	 */
	protected IStructuredSelection getStructuredSelection() {
		IStructuredSelection selection = null;
		IWorkbenchPart wbp = getWorkbenchPart();
		if (wbp != null) {
			ISelectionProvider selectionProvider = wbp.getSite()
					.getSelectionProvider();
			if (selectionProvider != null
					&& selectionProvider.getSelection() instanceof IStructuredSelection) {
				selection = (IStructuredSelection) selectionProvider
						.getSelection();
				if (selection != null)
					return selection;
			}
		}
		return StructuredSelection.EMPTY;
	}

	/**
	 * Performs this action. This method is called when the delegating action
	 * has been triggered.
	 * 
	 * @param act
	 *            The action proxy that handles the presentation portion of the
	 *            action.
	 */
	public void run(IAction act) {
		getActionManager().run(this);
	}

	/**
	 * Notifies this action delegate that the selection in the workbench has
	 * changed.
	 * 
	 * @param act
	 *            The action proxy that handles presentation portion of the
	 *            action.
	 * @param selection
	 *            The current selection, or <code>null</code> if there is no
	 *            selection.
	 */
	public void selectionChanged(IAction act, ISelection selection) {
		setAction(act);
	}

	/**
	 * Sets the active editor for this action delegate.
	 * 
	 * @param action
	 *            The action proxy that handles presentation portion of the
	 *            action.
	 * @param targetEditor
	 *            The new editor target.
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		setAction(action);
		setWorkbenchPart(targetEditor);
	}

	/**
	 * Sets the active part for this delegate. The active part is commonly used
	 * to get a working context for the action, such as the shell for any dialog
	 * which is needed.
	 * 
	 * @param action
	 *            The action proxy that handles presentation portion of the
	 *            action.
	 * @param targetPart
	 *            The new part target.
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		setAction(action);
		setWorkbenchPart(targetPart);
	}

	/**
	 * Notifies this action delegate that the given part has been activated.
	 * 
	 * @param part
	 *            The part that was activated.
	 */
	public void partActivated(IWorkbenchPart part) {
		setWorkbenchPart(part);
	}

	/**
	 * Notifies this action delegate that the given part has been brought to the
	 * top.
	 * 
	 * @param part
	 *            The part that was surfaced.
	 */
	public void partBroughtToTop(IWorkbenchPart part) {
		/* not implemented */
	}

	/**
	 * Notifies this action delegate that the given part has been closed.
	 * 
	 * @param part
	 *            The part that was closed.
	 */
	public void partClosed(IWorkbenchPart part) {
		if (getWorkbenchPart() == part) {
			setWorkbenchPart(null);
		}
	}

	/**
	 * Notifies this action delegate that the given part has been deactivated.
	 * 
	 * @param part
	 *            The part that was deactivated.
	 */
	public void partDeactivated(IWorkbenchPart part) {
		/* method not implemented */
	}

	/**
	 * Notifies this action delegate that the given part has been opened.
	 * 
	 * @param part
	 *            The part that was opened.
	 */
	public void partOpened(IWorkbenchPart part) {
		/* method not implemented */
	}

	/**
	 * Initializes this action delegate with the view it will work in.
	 * 
	 * @param view
	 *            The view that provides the context for this delegate.
	 */
	public void init(IViewPart view) {
		setWorkbenchPart(view);
	}

	/**
	 * Disposes this action delegate.
	 */
	public void dispose() {
		if (null != getWorkbenchWindow()) {
			getWorkbenchWindow().getPartService().removePartListener(this);
		}
		setWorkbenchPart(null);
		setWorkbenchWindow(null);
		setAction(null);
	}

	/**
	 * Initializes this action delegate with the workbench window it will work
	 * in.
	 * 
	 * @param window
	 *            The window that provides the context for this delegate.
	 */
	public void init(IWorkbenchWindow window) {
		setWorkbenchWindow(window);

		if (null != window.getActivePage()) {
			setWorkbenchPart(window.getActivePage().getActivePart());
		}
		window.getPartService().addPartListener(this);
	}

	/**
	 * Retrieves the label for this action delegate.
	 * 
	 * @return The label for this action delegate.
	 */
	public String getLabel() {
		return getAction().getText();
	}

	/**
	 * Retrieves a Boolean indicating whether this action delegate can be run.
	 * 
	 * @return <code>true</code> if the action for this delegate is enabled;
	 *         <code>false</code> otherwise.
	 */
	public boolean isRunnable() {
		return getAction().isEnabled();
	}

	/**
	 * @inheritDoc
	 */
	public void refresh() {
		/* method not implemented */
	}

	/**
	 * Runs this action delegate.
	 */
	public void run(IProgressMonitor progressMonitor) {
		if (isSetup() || !needsSetup()) {
			try {
				doRun(progressMonitor);
			} catch (Exception e) {
				handle(e);
			}
			setSetup(false);
		} else {
			throw new IllegalStateException(
					"action must be setup before it is run"); //$NON-NLS-1$
		}
	}

	/**
	 * Answers whether or not this action should be setup before it is run.
	 * Subclasses should override if they provide vital behaviour in the setup
	 * method.
	 * 
	 * @return <code>true</code> if the action has a setup, <code>false</code>
	 *         otherwise.
	 */
	protected boolean needsSetup() {
		return false;
	}

	/**
	 * Handles the specified exception.
	 * 
	 * @param exception
	 *            The exception to be handled.
	 */
	protected void handle(Exception exception) {
		Trace.catching(KommaEditUIPlugin.getPlugin(),
				EditUIDebugOptions.EXCEPTIONS_CATCHING, getClass(),
				"handle", exception); //$NON-NLS-1$

		IStatus status = new Status(IStatus.ERROR, KommaEditUIPlugin.PLUGIN_ID,
				EditUIStatusCodes.ACTION_FAILURE, String.valueOf(exception
						.getMessage()), exception);

		Log.log(KommaEditUIPlugin.getPlugin(), status);
		openErrorDialog(status);
	}

	/**
	 * Opens an error dialog for the specified status object.
	 * 
	 * @param status
	 *            The status object for which to open an error dialog.
	 * 
	 */
	protected void openErrorDialog(final IStatus status) {
		final Display display = getDisplay();

		if (display.getThread() == Thread.currentThread()) {
			// we're already on the UI thread
			ErrorDialog.openError(display.getActiveShell(), Action
					.removeMnemonics(getLabel()), null, status);
		} else {
			// we're not on the UI thread
			display.asyncExec(new Runnable() {
				public void run() {
					ErrorDialog.openError(display.getActiveShell(), Action
							.removeMnemonics(getLabel()), null, status);
				}
			});
		}
	}

	private Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null && PlatformUI.isWorkbenchRunning()) {
			display = PlatformUI.getWorkbench().getDisplay();
		}
		return display;
	}

	/**
	 * Performs the actual work when this action delegate is run. Subclasses
	 * must override this method to do some work.
	 * 
	 * @param progressMonitor
	 *            A progress monitor for tracking the progress of the action's
	 *            execution.
	 */
	protected abstract void doRun(IProgressMonitor progressMonitor);

	/**
	 * @see org.eclipse.gmf.runtime.common.ui.action.IActionWithProgress#getWorkIndicatorType()
	 */
	public WorkIndicatorType getWorkIndicatorType() {
		return WorkIndicatorType.BUSY;
	}

	/**
	 * @see org.eclipse.gmf.runtime.common.ui.action.IActionWithProgress#setup()
	 */
	public boolean setup() {
		setSetup(true);
		return true;
	}

	/**
	 * Returns the setup state of this action.
	 * 
	 * @return <code>true</code> if the action has been setup,
	 *         <code>false</code> otherwise.
	 */
	public boolean isSetup() {
		return setup;
	}

	/**
	 * Sets the setup state of this action.
	 * 
	 * @param setup
	 *            <code>true</code> if the action has been setup,
	 *            <code>false</code> otherwise.
	 */
	protected void setSetup(boolean setup) {
		this.setup = setup;
	}

	/*
	 * 
	 * mgoyal: Fixing the memory leak caused in subclasses that implement
	 * IEditorActionDelegate. This will provide the functionality to also
	 * implement IActionDelegate2 and get notified of Lifecycle events.
	 */

	/**
	 * Allows the action delegate to initialize itself after being created by
	 * the proxy action. This lifecycle method is called after the action
	 * delegate has been created and before any other method of the action
	 * delegate is called.
	 * 
	 * @param anAction
	 *            the proxy action that handles the presentation portion of the
	 *            action.
	 */
	public void init(IAction anAction) {
		setAction(anAction);
	}

	/**
	 * Performs this action, passing the SWT event which triggered it. This
	 * method is called by the proxy action when the action has been triggered.
	 * Implement this method to do the actual work.
	 * <p>
	 * <b>Note:</b> This method is called instead of <code>run(IAction)</code>.
	 * </p>
	 * 
	 * @param anAction
	 *            the action proxy that handles the presentation portion of the
	 *            action
	 * @param event
	 *            the SWT event which triggered this action being run
	 */
	public void runWithEvent(IAction anAction, Event event) {
		run(anAction);
	}

}
