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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.internal.EditUIStatusCodes;
import net.enilink.komma.edit.ui.util.PartListenerAdapter;

/**
 * A custom contribution item that goes into a workbenchpart's toolbar
 * 
 * @author melaasar
 */
public abstract class AbstractContributionItem extends ContributionItem
		implements ISelectionChangedListener, IOperationHistoryListener,
		IActionWithProgress {

	/**
	 * Flag to indicate whether or not this action has been set up.
	 */
	private boolean setup;

	// the part service
	private IWorkbenchPage workbenchPage;

	// the part listener
	private IPartListener partListener;

	// the current workbenchpart
	private IWorkbenchPart workbenchPart;

	// the item listener
	private Listener itemListener;

	// the item widget
	private Item item;

	// the control in case of a widget with a SWT.SEPARATOR style
	private Control control;

	// the control text
	private String label;

	// the enablement state of the item
	private boolean enabled = true;

	/**
	 * Creates a new WorkbenchPartContributionItem
	 * 
	 * @param workbenchPage
	 *            The workbench Page
	 */
	public AbstractContributionItem(IWorkbenchPage workbenchPage) {
		this(workbenchPage, null);
	}

	/**
	 * Creates a new WorkbenchPartContributionItem
	 * 
	 * @param workbenchPage
	 *            The workbench Page
	 * @param id
	 *            The id of the contribution item
	 */
	public AbstractContributionItem(IWorkbenchPage workbenchPage, String id) {
		super(id);
		assert null != workbenchPage : "workbenchPage is null"; //$NON-NLS-1$

		this.workbenchPage = workbenchPage;
		partListener = new PartListenerAdapter() {

			public void partActivated(IWorkbenchPart part) {
				setWorkbenchPart(part);
				update();
			}
		};
		itemListener = new Listener() {

			public void handleEvent(Event event) {
				AbstractContributionItem.this.handleWidgetEvent(event);
			}
		};
	}

	/**
	 * <code>init</code> is used to initialize the common part of filling this
	 * item in a contribution manager. The <code>dispose</code> method is later
	 * called to clean up what has been initialized in the <code>fill</code> and
	 * <code>init</code> methods
	 */
	protected void init() {
		if (getWorkbenchPart() == null)
			setWorkbenchPart(workbenchPage.getActivePart());
		workbenchPage.addPartListener(partListener);
	}

	/**
	 * Dispose should only clean up what was done in the <code>fill</code>
	 * methods It is not meant to clean up what was done in constructors
	 */
	public void dispose() {
		workbenchPage.removePartListener(partListener);
		setWorkbenchPart(null);
		item = null;
		control = null;
		super.dispose();
	}

	/**
	 * Gets the current workbench part.
	 * 
	 * @return The current workbench part.
	 */
	protected IWorkbenchPart getWorkbenchPart() {
		return workbenchPart;
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

	/**
	 * Gets the item control
	 * 
	 * @return The item control
	 */
	protected Control getControl() {
		return control;
	}

	/**
	 * Gets the item widget
	 * 
	 * @return The item widget
	 */
	protected Item getItem() {
		return item;
	}

	/**
	 * Gets the tool item widget
	 * 
	 * @return The tool item widget
	 */
	protected ToolItem getToolItem() {
		return item instanceof ToolItem ? (ToolItem) item : null;
	}

	/**
	 * Gets the tool item widget
	 * 
	 * @return The tool item widget
	 */
	protected MenuItem getMenuItem() {
		return item instanceof MenuItem ? (MenuItem) item : null;
	}

	/**
	 * Gets the control tooltip text
	 * 
	 * @return The control tooltip text
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param item
	 *            widget
	 */
	public void setItem(Item item) {
		this.item = item;
	}

	/**
	 * Sets the control label
	 * 
	 * @param label
	 *            The control label
	 */
	protected void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Sets the current workbench part
	 * 
	 * @param workbenchPart
	 *            The current workbench part
	 */
	protected void setWorkbenchPart(IWorkbenchPart workbenchPart) {
		if (getWorkbenchPart() != null) {
			if (isSelectionListener()) {
				ISelectionProvider provider = getWorkbenchPart().getSite()
						.getSelectionProvider();
				if (provider != null) {
					provider.removeSelectionChangedListener(this);
				}
			}
			if (isOperationHistoryListener()) {
				getOperationHistory().removeOperationHistoryListener(this);
			}
		}

		this.workbenchPart = workbenchPart;

		if (workbenchPart != null) {
			if (isSelectionListener()) {
				ISelectionProvider provider = getWorkbenchPart().getSite()
						.getSelectionProvider();
				if (provider != null) {
					provider.addSelectionChangedListener(this);
				}
			}
			if (isOperationHistoryListener()) {
				getOperationHistory().addOperationHistoryListener(this);
			}
		}
	}

	/**
	 * The control item implementation of this <code>IContributionItem</code>
	 * method calls the <code>createControl</code> framework method. Subclasses
	 * must implement <code>createControl</code> rather than overriding this
	 * method.
	 * 
	 * @param parent
	 *            The parent of the control to fill
	 */
	public final void fill(Composite parent) {
		init();
		control = createControl(parent);
		if (control != null) {
			update();
		}
		assert null != control : "The contribution item cannot fill in composites"; //$NON-NLS-1$
	}

	/**
	 * The control item implementation of this <code>IContributionItem</code>
	 * method throws an exception since controls cannot be added to menus.
	 * 
	 * @param parent
	 *            The menu
	 * @param index
	 *            Menu index
	 */
	public final void fill(Menu parent, int index) {
		init();
		MenuItem menuItem = createMenuItem(parent, index);
		if (menuItem != null) {
			menuItem.setData(this);
			menuItem.setText(getLabel());
			menuItem.addListener(SWT.Dispose, getItemListener());
			setItem(menuItem);
			update();
		}
		assert null != menuItem : "The contribution item cannot fill in menus"; //$NON-NLS-1$
	}

	/**
	 * The control item implementation of this <code>IContributionItem</code>
	 * method calls the <code>createControl</code> framework method to create a
	 * control under the given parent, and then creates a new tool item to hold
	 * it. Subclasses must implement <code>createControl</code> rather than
	 * overriding this method.
	 * 
	 * @param parent
	 *            The ToolBar to add the new control to
	 * @param index
	 *            Index
	 */
	public final void fill(ToolBar parent, int index) {
		init();
		ToolItem toolItem = createToolItem(parent, index);
		if (toolItem != null) {
			toolItem.setData(this);
			toolItem.setToolTipText(getLabel());
			toolItem.addListener(SWT.Dispose, getItemListener());
			setItem(toolItem);
			update();
		}
		assert null != toolItem : "The contribution item cannot fill in toolbars"; //$NON-NLS-1$
	}

	/**
	 * Creates the <code>ToolItem</code> with the given parent and index.
	 * 
	 * @param parent
	 *            The ToolBar to add the new control to
	 * @param index
	 *            Index
	 * @return <code>ToolItem</code> for specified <code>ToolBar</code> at
	 *         specifiec index
	 */
	protected ToolItem createToolItem(ToolBar parent, int index) {
		control = createControl(parent);
		if (control != null) {
			ToolItem anItem = new ToolItem(parent, SWT.SEPARATOR, index);
			anItem.setControl(control);
			anItem.setWidth(computeWidth(control));
			return anItem;
		}
		return null;
	}

	/**
	 * Creates the menuitem with the given parent and index.
	 * 
	 * @param parent
	 *            The Menu to add the new control to
	 * @param index
	 *            Index
	 * @return created <code>MenuItem</code>
	 */
	protected MenuItem createMenuItem(Menu parent, int index) {
		return null;
	}

	/**
	 * Creates the control of this contributor - override only if a custom
	 * control is needed.
	 * 
	 * @param parent
	 *            the parent <code>Composite</code>
	 * @return control for the specified parent <code>Composite</code>
	 */
	protected Control createControl(Composite parent) {
		return null;
	}

	/**
	 * Method is being called when there control created by subclasses is not
	 * null.
	 * 
	 * Computes the width of the given control which is being added to a tool
	 * bar. This is needed to determine the width of the tool bar item
	 * containing the given control.
	 * 
	 * @param cont
	 *            the control being added
	 * @return the width of the control
	 */
	protected int computeWidth(Control cont) {
		return cont.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x;
	}

	/**
	 * updates the properties of this contribution item Subclasses should call
	 * this method when an update is requested
	 * 
	 * This method is not intended to be overriden. Instead override the
	 * <code>refresh()</code> method
	 */
	public final void update() {
		if (getWorkbenchPart() == null)
			setWorkbenchPart(workbenchPage.getActivePart());
		if (getWorkbenchPart() != null) {
			refresh();
		}
	}

	/**
	 * refreshed the properties of this contribution item This method should not
	 * be called directly, instead <code>update</code> method should be called.
	 * 
	 * Subclasses could override this method to add to the refresh and at the
	 * end call <code>super.refresh()</code>
	 */
	public void refresh() {
		setEnabled(calculateEnabled());
		if (getControl() != null || getItem() != null)
			refreshItem();
	}

	/**
	 * Refreshes the item's GUI
	 */
	protected void refreshItem() {
		if (getControl() != null)
			getControl().setEnabled(isEnabled());
		else if (getToolItem() != null)
			getToolItem().setEnabled(isEnabled());
		else if (getMenuItem() != null)
			getMenuItem().setEnabled(isEnabled());
	}

	/**
	 * Calculates enablement of the widget. Subclasses must implement. The
	 * enablement will used every time the widget is refreshed. It is a
	 * resposcibility of the subclasses to call refresh() when it is
	 * appropriate.
	 * 
	 * @return boolean
	 */
	protected abstract boolean calculateEnabled();

	/**
	 * Method setEnabled.
	 * 
	 * @param enabled
	 */
	protected void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IContributionItem#isEnabled()
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Executes the given <code>ommand</code>.
	 * 
	 * @param command
	 *            <code>ICommand</code> to be executed
	 */
	protected void execute(ICommand command) {
		if (command == null || !command.canExecute())
			return;

		command.addContext(getUndoContext());

		try {
			getOperationHistory().execute(command, new NullProgressMonitor(),
					null);

		} catch (ExecutionException e) {
			KommaEditUIPlugin.getPlugin().log(new Status(IStatus.ERROR, KommaEditUIPlugin.PLUGIN_ID,
					EditUIStatusCodes.ACTION_FAILURE, e.getLocalizedMessage(), e));
		}
		return;
	}

	/**
	 * Retrieves the action manager for this action delegate from its workbench
	 * part.
	 * 
	 * @return The action manager for this action delegate.
	 */
	protected ActionManager getActionManager() {
		ActionManager manager = (ActionManager) getWorkbenchPart().getAdapter(
				ActionManager.class);

		return null == manager ? ActionManager.getDefault() : manager;
	}

	/**
	 * Returns the operation history for this contribution item from its action
	 * manager.
	 * 
	 * @return the operation history
	 */
	protected IOperationHistory getOperationHistory() {
		return getActionManager().getOperationHistory();
	}

	/**
	 * A generalized convinience method. Should be called by subclasses whenever
	 * run() must be ivoked (e.g. whenever a button is pushed)
	 * 
	 * @param event
	 *            an optional associated SWT event
	 */
	protected void runWithEvent(Event event) {
		getActionManager().run(this);
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

	/**
	 * Handles the specified exception.
	 * 
	 * @param exception
	 *            The exception to be handled.
	 */
	protected void handle(Exception exception) {
		IStatus status = new Status(IStatus.ERROR, KommaEditUIPlugin.PLUGIN_ID,
				EditUIStatusCodes.ACTION_FAILURE, String.valueOf(exception
						.getMessage()), exception);

		KommaEditUIPlugin.getPlugin().log(status);
		openErrorDialog(status);
	}

	/**
	 * Opens an error dialog for the specified status object.
	 * 
	 * @param status
	 *            The status object for which to open an error dialog.
	 * 
	 */
	protected void openErrorDialog(IStatus status) {
		ErrorDialog.openError(getWorkbenchPart().getSite().getShell(),
				getLabel(), null, status);
	}

	/**
	 * Handles an event from the widget (forwarded from nested listener).
	 * 
	 * @param e
	 *            <code>Event</code> to be handled by this method
	 */
	protected void handleWidgetEvent(Event e) {
		switch (e.type) {
		case SWT.Dispose:
			handleWidgetDispose(e);
			break;
		}
	}

	/**
	 * Handles a widget dispose event for the widget corresponding to this item.
	 * 
	 * @param e
	 *            widget dispose <code>Event</code>
	 */
	protected void handleWidgetDispose(Event e) {
		dispose();
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
	 * Retrieves a Boolean indicating whether this contribution item is
	 * interested in operation history changed events.
	 * 
	 * @return <code>true</code> if this action handler is interested;
	 *         <code>false</code> otherwise.
	 */
	protected boolean isOperationHistoryListener() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(
	 * org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public final void selectionChanged(SelectionChangedEvent event) {
		update();
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

				if (partContext != null && operation.hasContext(partContext)
						&& PlatformUI.isWorkbenchRunning()) {
					PlatformUI.getWorkbench().getDisplay().syncExec(
							new Runnable() {

								public void run() {
									update();
								}
							});
				}
			}
		}
	}

	/**
	 * Retrieves the current selection.
	 * 
	 * @return The current selection.
	 */
	protected ISelection getSelection() {
		ISelection selection = null;
		ISelectionProvider selectionProvider = getWorkbenchPart().getSite()
				.getSelectionProvider();

		if (selectionProvider != null) {
			selection = selectionProvider.getSelection();
		}

		return (selection != null) ? selection : StructuredSelection.EMPTY;
	}

	/**
	 * Retrieves the current structured selection.
	 * 
	 * @return <code>IStructuredSelection</code> for current selection
	 */
	protected IStructuredSelection getStructuredSelection() {
		IStructuredSelection selection = null;
		ISelectionProvider selectionProvider = getWorkbenchPart().getSite()
				.getSelectionProvider();

		if (selectionProvider != null
				&& selectionProvider.getSelection() instanceof IStructuredSelection) {
			selection = (IStructuredSelection) selectionProvider.getSelection();
		}
		return (selection != null) ? selection : StructuredSelection.EMPTY;
	}

	/**
	 * Returns the item listenr
	 * 
	 * @return The item listener
	 */
	protected Listener getItemListener() {
		return itemListener;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.gmf.runtime.common.ui.action.IRepeatableAction#
	 * getWorkIndicatorType()
	 */
	public WorkIndicatorType getWorkIndicatorType() {
		return WorkIndicatorType.BUSY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gmf.runtime.common.ui.action.IRepeatableAction#isRunnable()
	 */
	public boolean isRunnable() {
		return isEnabled();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gmf.runtime.common.ui.action.IRepeatableAction#run(org.eclipse
	 * .core.runtime.IProgressMonitor)
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