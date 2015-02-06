/******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/

package net.enilink.komma.edit.ui.action;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import net.enilink.komma.common.util.Log;
import net.enilink.komma.common.util.StringStatics;
import net.enilink.komma.common.util.Trace;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.internal.EditUIDebugOptions;
import net.enilink.komma.edit.ui.internal.EditUIStatusCodes;
import net.enilink.komma.edit.ui.util.IPartSelector;
import net.enilink.komma.edit.ui.util.PartListenerAdapter;
import net.enilink.komma.edit.ui.util.StatusLineUtil;

/**
 * The abstract parent of all concrete action handlers that execute commands.
 * Logging and exception handling are done in a uniform way in the
 * <code>run()</code> method. Concrete subclasses must provide a definition of
 * the <code>doRun()</code> method to gather any required input and execute a
 * command. As an implementer of the <code>IRepeatableAction</code> interface,
 * this class implements the <code>isRepeatable()</code> method to return
 * <code>true</code> if it is enabled, and implements the <code>repeat()</code>
 * method to run itself. Subclasses that aren't repeatable or require special
 * repeat behavior must override the default implementations of these interface
 * methods.
 * 
 * This action handler supports life cycle methods by implementing the
 * <code>IDisposableAction</code> interface. Therefore, clients need to call the
 * <code>init()</code> method to initialize the action, and the
 * <code>dispose()</code> method when the action is no longer needed.
 * 
 * @author khussey
 */
public abstract class AbstractActionHandler extends Action implements
		IDisposableAction, IActionWithProgress, ISelectionChangedListener,
		IOperationHistoryListener, IPropertyListener {

	/**
	 * Flag to indicate whether or not this action has been set up.
	 */
	private boolean setup;

	/**
	 * My disposed state.
	 */
	private boolean disposed;

	/**
	 * The workbench part to which this action handler applies.
	 */
	private IWorkbenchPart workbenchPart;

	/**
	 * The workbench page this action is associated to
	 */
	private IWorkbenchPage workbenchPage;

	/**
	 * The part listener of this action
	 */
	private IPartListener partListener;

	/**
	 * Selects workbench parts that match the part criteria with which I was
	 * contributed. I will refresh myself when the selection changes on parts
	 * that match this criteria, and when such parts are activated.
	 */
	private IPartSelector partSelector;

	/**
	 * A static selection this action operates on.
	 */
	private ISelection selection;

	/**
	 * Constructs a new action handler for the specified workbench part.
	 * 
	 * @param workbenchPart
	 *            The workbench part to which this action handler applies.
	 */
	protected AbstractActionHandler(IWorkbenchPart workbenchPart) {
		setWorkbenchPart(workbenchPart);
		if (workbenchPart != null) {
			this.workbenchPage = workbenchPart.getSite().getPage();
		}
	}

	/**
	 * Constructs a new action handler that gets its workbench part by listening
	 * to the given workbench page
	 * 
	 * @param workbenchPage
	 *            The workbench page associated with this action handler
	 */
	protected AbstractActionHandler(final IWorkbenchPage workbenchPage) {
		this.workbenchPage = workbenchPage;
		if (workbenchPage != null) {
			this.partListener = new PartListenerAdapter() {
				/**
				 * Listens to part activation and updates the active workbench
				 */
				public void partActivated(IWorkbenchPart part) {
					setWorkbenchPart(part);
					if (part != null && contributedToPart(part)
							&& !isDisposed())
						refresh();
				}

				/**
				 * Listens to part deactivation and disables the action Could be
				 * improved to only consider toolbar actions
				 */
				public void partDeactivated(IWorkbenchPart part) {
					if (part != null && contributedToPart(part)) {
						setEnabled(false);
					}
				}
			};
			workbenchPage.addPartListener(partListener);
		}
	}

	/**
	 * Constructs a new action handler for the specified workbench part.
	 * 
	 * @param workbenchPart
	 *            The workbench part to which this action handler applies.
	 */
	protected AbstractActionHandler(IWorkbenchPart workbenchPart,
			ISelection selection) {
		setWorkbenchPart(workbenchPart);
		if (workbenchPart != null) {
			this.workbenchPage = workbenchPart.getSite().getPage();
		}
		this.selection = selection;
	}

	/**
	 * The basic implementation sets the workbenchpart if not already set and
	 * refreshes the action if the current part is not null.
	 * <P>
	 * Any subclass that overrided this method should ensure that the disposed
	 * state of this action is maintained by calling
	 * <code>setDisposed(false)</code> or calling <code>super.init()</code>.
	 */
	public void init() {
		setDisposed(false);
		if (getWorkbenchPart() == null && getWorkbenchPage() != null) {
			setWorkbenchPart(getWorkbenchPage().getActivePart());
		}
		refresh();
	}

	/**
	 * Default implementation of dispose. Any subclass that overrided this
	 * method should ensure that the disposed state of this action is maintained
	 * by calling <code>setDisposed(true)</code> or calling
	 * <code>super.dispose()</code>.
	 */
	public void dispose() {
		setWorkbenchPart(null);
		if (partListener != null && workbenchPage != null) {
			workbenchPage.removePartListener(partListener);
			workbenchPage = null;
			partListener = null;
		}
		setDisposed(true);
	}

	/**
	 * Sets the current workbencgPart
	 * 
	 * @param workbenchPart
	 *            The current workbenchPart
	 */
	protected void setWorkbenchPart(IWorkbenchPart workbenchPart) {
		if (getWorkbenchPart() == workbenchPart)
			return;

		if (getWorkbenchPart() != null) {
			if (isSelectionListener()) {
				ISelectionProvider provider = getWorkbenchPart().getSite()
						.getSelectionProvider();
				if (provider != null) {
					provider.removeSelectionChangedListener(this);
				}
			}
			if (isPropertyListener()) {
				getWorkbenchPart().removePropertyListener(this);
			}
			if (isOperationHistoryListener()) {
				getOperationHistory().removeOperationHistoryListener(this);
			}
		}

		this.workbenchPart = workbenchPart;

		if (workbenchPart != null) {

			if (contributedToPart(workbenchPart)) {

				if (isSelectionListener()) {
					ISelectionProvider provider = getWorkbenchPart().getSite()
							.getSelectionProvider();
					if (provider != null) {
						provider.addSelectionChangedListener(this);
					}
				}
				if (isPropertyListener()) {
					getWorkbenchPart().addPropertyListener(this);
				}
				if (isOperationHistoryListener()) {
					getOperationHistory().addOperationHistoryListener(this);
				}
			}
		}
	}

	/**
	 * Answers whether or not I am contributed to <code>part</code>.
	 * 
	 * @param part
	 *            the workbench part to be tested
	 * @return <code>true</code> if I am contributed to this part,
	 *         <code>false</code> otherwise.
	 */
	protected boolean contributedToPart(IWorkbenchPart part) {
		return partSelector == null || partSelector.selects(part);
	}

	/**
	 * Sets my part selector. If my current workbench part doesn't match the
	 * part selector, I stop listening to selection, property and operation
	 * history changes on that part.
	 * 
	 * @param partSelector
	 *            my new part selector
	 */
	public final void setPartSelector(IPartSelector partSelector) {
		if (this.partSelector == partSelector) {
			return;
		}

		this.partSelector = partSelector;
		IWorkbenchPart part = getWorkbenchPart();

		if (part != null && !partSelector.selects(part)) {
			if (isSelectionListener()) {
				ISelectionProvider provider = getWorkbenchPart().getSite()
						.getSelectionProvider();
				if (provider != null) {
					provider.removeSelectionChangedListener(this);
				}
			}
			if (isPropertyListener()) {
				getWorkbenchPart().removePropertyListener(this);
			}
			if (isOperationHistoryListener()) {
				getOperationHistory().removeOperationHistoryListener(this);
			}
		}
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
	 * Retrieves the action manager for this action handler from its workbench
	 * part.
	 * 
	 * @return The action manager for this action handler.
	 */
	protected ActionManager getActionManager() {
		ActionManager manager = null;
		IWorkbenchPart wbPart = getWorkbenchPart();
		if (wbPart != null) {
			manager = (ActionManager) wbPart.getAdapter(ActionManager.class);
		}

		return null == manager ? ActionManager.getDefault() : manager;
	}

	/**
	 * Returns the operation history for this action handler from its action
	 * manager.
	 * 
	 * @return the operation history
	 */
	protected IOperationHistory getOperationHistory() {
		return getActionManager().getOperationHistory();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		getActionManager().run(this);
	}

	/**
	 * Runs this action handler.
	 */
	public void run(IProgressMonitor progressMonitor) {
		if (isSetup() || !needsSetup()) {
			try {
				StatusLineUtil.outputErrorMessage(getWorkbenchPart(),
						StringStatics.BLANK);
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
	 * Runs this action handler, passing the triggering SWT event.
	 * 
	 * @param event
	 *            The SWT event which triggered this action being run.
	 */
	public void runWithEvent(Event event) {
		getActionManager().run(this);
	}

	/**
	 * Notifies this action handler that the selection has changed.
	 * 
	 * @param event
	 *            Event object describing the change.
	 */
	public final void selectionChanged(SelectionChangedEvent event) {
		refresh();
	}

	/**
	 * Property change event handler; does nothing by default. Subclasses should
	 * override if they are interested in handling property change events.
	 */
	public void propertyChanged(Object source, int propId) {
		// Do nothing by default
	}

	/**
	 * Retrieves the label for this action handler.
	 * 
	 * @return The label for this action handler.
	 */
	public String getLabel() {
		return getText();
	}

	/**
	 * Retrieves the current selection.
	 * 
	 * @return The current selection.
	 */
	protected ISelection getSelection() {
		if (this.selection != null) {
			return this.selection;
		}

		ISelection selection = null;
		ISelectionService selectionService = null;
		if (getWorkbenchPart() != null
				&& getWorkbenchPart().getSite().getWorkbenchWindow() != null) {
			selectionService = getWorkbenchPart().getSite()
					.getWorkbenchWindow().getSelectionService();
		}

		if (selectionService != null) {
			selection = selectionService.getSelection();
		}

		return (selection != null) ? selection : StructuredSelection.EMPTY;
	}

	/**
	 * Retrieves the current structured selection.
	 * 
	 * @return The current structured selection.
	 */
	protected IStructuredSelection getStructuredSelection() {
		ISelection selection = getSelection();
		return (selection instanceof StructuredSelection) ? (StructuredSelection) selection
				: StructuredSelection.EMPTY;
	}

	/**
	 * Retrieves a Boolean indicating whether this action handler can be run.
	 * 
	 * @return <code>true</code> if this action handler is enabled;
	 *         <code>false</code> otherwise.
	 */
	public boolean isRunnable() {
		return isEnabled();
	}

	/**
	 * Retrieves a Boolean indicating whether this action handler is interested
	 * in selection events.
	 * 
	 * @return <code>true</code> if this action handler is interested;
	 *         <code>false</code> otherwise.
	 */
	protected boolean isSelectionListener() {
		return false;
	}

	/**
	 * Answers whether of not this action handler is interested in property
	 * change events.
	 * <P>
	 * This default implementation always returns <code>false</code>. Subclasses
	 * must override if they are interested in property change events.
	 * 
	 * @return <code>true</code> if this action handler is interested;
	 *         <code>false</code> otherwise.
	 */
	protected boolean isPropertyListener() {
		return false;
	}

	/**
	 * Retrieves a Boolean indicating whether this action handler is interested
	 * in operation history changed events.
	 * 
	 * @return <code>true</code> if this action handler is interested;
	 *         <code>false</code> otherwise.
	 */
	protected boolean isOperationHistoryListener() {
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
			ErrorDialog.openError(display.getActiveShell(),
					removeMnemonics(getLabel()), null, status);

		} else {
			// we're not on the UI thread
			display.asyncExec(new Runnable() {

				public void run() {
					ErrorDialog.openError(display.getActiveShell(),
							removeMnemonics(getLabel()), null, status);
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
	 * Performs the actual work when this action handler is run. Subclasses must
	 * override this method to do some work.
	 * 
	 * @param progressMonitor
	 *            the progress monitor for tracking the progress of this action
	 *            when it is run.
	 */
	protected abstract void doRun(IProgressMonitor progressMonitor);

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.gmf.runtime.common.ui.action.IRepeatableAction#
	 * getWorkIndicatorType()
	 */
	public WorkIndicatorType getWorkIndicatorType() {
		return WorkIndicatorType.BUSY;
	}

	/**
	 * Returns the part listener
	 * 
	 * @return The part listener
	 */
	protected IPartListener getPartListener() {
		return partListener;
	}

	/**
	 * Returns the workbench page
	 * 
	 * @return The workbench page
	 */
	protected IWorkbenchPage getWorkbenchPage() {
		return workbenchPage;
	}

	/**
	 * Refreshes me if the history event has my workbench part's context, and
	 * the event is one of:
	 * <UL>
	 * <LI>{@link OperationHistoryEvent#UNDONE}</LI>
	 * <LI>{@link OperationHistoryEvent#REDONE}</LI>
	 * <LI>{@link OperationHistoryEvent#OPERATION_ADDED}</LI>
	 * <LI>{@link OperationHistoryEvent#OPERATION_CHANGED}</LI>
	 * <LI>{@link OperationHistoryEvent#OPERATION_NOT_OK}</LI>
	 * <LI>{@link OperationHistoryEvent#OPERATION_REMOVED}</LI>
	 * </UL>
	 * The other operation history events are ignored because they are
	 * intermediate events that will be followed by one of those listed above.
	 * We only want to refresh the action handler once for each change to the
	 * operation history.
	 */
	public void historyNotification(OperationHistoryEvent event) {
		int type = event.getEventType();
		if (type == OperationHistoryEvent.UNDONE
				|| type == OperationHistoryEvent.REDONE
				|| type == OperationHistoryEvent.DONE
				|| type == OperationHistoryEvent.OPERATION_ADDED
				|| type == OperationHistoryEvent.OPERATION_CHANGED
				|| type == OperationHistoryEvent.OPERATION_NOT_OK
				|| type == OperationHistoryEvent.OPERATION_REMOVED) {

			IUndoableOperation operation = event.getOperation();

			if (operation != null) {
				IUndoContext partContext = getUndoContext();

				if (partContext != null && operation.hasContext(partContext)) {
					refresh();
				}
			}
		}
	}

	/**
	 * Gets the undo context from my workbench part.
	 * 
	 * @return the undo context
	 */
	protected IUndoContext getUndoContext() {
		IWorkbenchPart part = getWorkbenchPart();

		if (part != null) {
			return (IUndoContext) part.getAdapter(IUndoContext.class);
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gmf.runtime.common.ui.action.IDisposableAction#isDisposed()
	 */
	public boolean isDisposed() {
		return disposed;
	}

	/**
	 * Sets my disposed state.
	 * 
	 * @param b
	 *            <code>true</code> if I am disposed, <code>false</code>
	 *            otherwise.
	 */
	protected void setDisposed(boolean b) {
		disposed = b;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gmf.runtime.common.ui.action.IRepeatableAction#setup()
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

}