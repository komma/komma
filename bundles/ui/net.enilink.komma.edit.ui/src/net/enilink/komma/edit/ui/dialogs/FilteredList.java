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
package net.enilink.komma.edit.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SearchPattern;
import org.eclipse.ui.progress.UIJob;

import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.internal.IEditUIImages;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;

/**
 * Shows a list of items to the user with a text entry field for a string
 * pattern used to filter the list of items.
 */
public abstract class FilteredList {
	private static final String SHOW_STATUS_LINE = "ShowStatusLine"; //$NON-NLS-1$

	private static final String HISTORY_SETTINGS = "History"; //$NON-NLS-1$

	Composite composite;

	/**
	 * Represents an empty selection in the pattern input field (used only for
	 * initial pattern).
	 */
	public static final int NONE = 0;

	/**
	 * Pattern input field selection where caret is at the beginning (used only
	 * for initial pattern).
	 */
	public static final int CARET_BEGINNING = 1;

	/**
	 * Represents a full selection in the pattern input field (used only for
	 * initial pattern).
	 */
	public static final int FULL_SELECTION = 2;

	private Text pattern;

	private TableViewer list;

	private DetailsContentViewer details;

	/**
	 * It is a duplicate of a field in the CLabel class in DetailsContentViewer.
	 * It is maintained, because the <code>setDetailsLabelProvider()</code>
	 * could be called before content area is created.
	 */
	private ILabelProvider detailsLabelProvider;

	private ItemsListLabelProvider itemsListLabelProvider;

	private MenuManager menuManager;

	private boolean multi;

	private ToolBar toolBar;

	private ToolItem toolItem;

	private Label progressLabel;

	private ToggleStatusLineAction toggleStatusLineAction;

	private RemoveHistoryItemAction removeHistoryItemAction;

	private ActionContributionItem removeHistoryActionContributionItem;

	private IStatus status;

	private RefreshCacheJob refreshCacheJob;

	private RefreshProgressMessageJob refreshProgressMessageJob;

	private Object[] lastSelection;

	private ContentProvider contentProvider;

	private FilterHistoryJob filterHistoryJob;

	private FilterJob filterJob;

	private ItemsFilter filter;

	private List<Object> lastCompletedResult;

	private ItemsFilter lastCompletedFilter;

	private String initialPatternText;

	private int selectionMode;

	private ItemsListSeparator itemsListSeparator;

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private boolean refreshWithLastSelection = false;

	private Object preSelected;

	/**
	 * Creates a new instance of the class.
	 * 
	 * @param multi
	 *            indicates whether dialog allows to select more than one
	 *            position in its list of items
	 */
	public FilteredList(boolean multi) {
		this.multi = multi;
	}

	/**
	 * Creates a new instance of the class. Created dialog won't allow to select
	 * more than one item.
	 * 
	 */
	public FilteredList() {
		this(false);
	}

	/**
	 * Adds viewer filter to the dialog items list.
	 * 
	 * @param filter
	 *            the new filter
	 */
	protected void addListFilter(ViewerFilter filter) {
		contentProvider.addFilter(filter);
	}

	/**
	 * Sets a new label provider for items in the list.
	 * 
	 * @param listLabelProvider
	 *            the label provider for items in the list
	 */
	public void setListLabelProvider(ILabelProvider listLabelProvider) {
		getItemsListLabelProvider().setProvider(listLabelProvider);
	}

	/**
	 * Returns the label decorator for selected items in the list.
	 * 
	 * @return the label decorator for selected items in the list
	 */
	private ILabelDecorator getListSelectionLabelDecorator() {
		return getItemsListLabelProvider().getSelectionDecorator();
	}

	/**
	 * Sets the label decorator for selected items in the list.
	 * 
	 * @param listSelectionLabelDecorator
	 *            the label decorator for selected items in the list
	 */
	public void setListSelectionLabelDecorator(
			ILabelDecorator listSelectionLabelDecorator) {
		getItemsListLabelProvider().setSelectionDecorator(
				listSelectionLabelDecorator);
	}

	/**
	 * Returns the item list label provider.
	 * 
	 * @return the item list label provider
	 */
	private ItemsListLabelProvider getItemsListLabelProvider() {
		if (itemsListLabelProvider == null) {
			itemsListLabelProvider = new ItemsListLabelProvider(
					new LabelProvider(), null);
		}
		return itemsListLabelProvider;
	}

	/**
	 * Sets label provider for the details field.
	 * 
	 * For a single selection, the element sent to
	 * {@link ILabelProvider#getImage(Object)} and
	 * {@link ILabelProvider#getText(Object)} is the selected object, for
	 * multiple selection a {@link String} with amount of selected items is the
	 * element.
	 * 
	 * @see #getSelectedItems() getSelectedItems() can be used to retrieve
	 *      selected items and get the items count.
	 * 
	 * @param detailsLabelProvider
	 *            the label provider for the details field
	 */
	public void setDetailsLabelProvider(ILabelProvider detailsLabelProvider) {
		this.detailsLabelProvider = detailsLabelProvider;
		if (details != null) {
			details.setLabelProvider(detailsLabelProvider);
		}
	}

	private ILabelProvider getDetailsLabelProvider() {
		if (detailsLabelProvider == null) {
			detailsLabelProvider = new LabelProvider();
		}
		return detailsLabelProvider;
	}

	private void createPatternAndMenu(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		pattern = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		pattern.setLayoutData(gd);

		createViewMenu(composite);
		composite.setLayoutData(gd);
	}

	private void createLabels(Composite parent) {
		Composite labels = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		labels.setLayout(layout);

		Label listLabel = new Label(labels, SWT.NONE);
		listLabel
				.setText(DialogMessages.FilteredItemsSelectionDialog_listLabel);

		// TODO find equivalent for RAP
		// listLabel.addTraverseListener(new TraverseListener() {
		// public void keyTraversed(TraverseEvent e) {
		// if (e.detail == SWT.TRAVERSE_MNEMONIC && e.doit) {
		// e.detail = SWT.TRAVERSE_NONE;
		// list.getTable().setFocus();
		// }
		// }
		// });

		GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		listLabel.setLayoutData(gd);

		progressLabel = new Label(labels, SWT.RIGHT);
		progressLabel.setLayoutData(gd);

		labels.setLayoutData(gd);
	}

	private void createViewMenu(Composite parent) {
		toolBar = new ToolBar(parent, SWT.FLAT);
		toolItem = new ToolItem(toolBar, SWT.PUSH, 0);

		GridData data = new GridData();
		data.horizontalAlignment = GridData.END;
		toolBar.setLayoutData(data);

		toolBar.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				showViewMenu();
			}
		});
		toolItem.setImage(ExtendedImageRegistry.getInstance().getImage(
				KommaEditUIPlugin.INSTANCE.getImage(IEditUIImages.VIEW_MENU)));
		toolItem.setToolTipText(DialogMessages.FilteredItemsSelectionDialog_menu);
		toolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showViewMenu();
			}
		});

		menuManager = new MenuManager();

		fillViewMenu(menuManager);
	}

	/**
	 * Fills the menu of the dialog.
	 * 
	 * @param menuManager
	 *            the menu manager
	 */
	protected void fillViewMenu(IMenuManager menuManager) {
		toggleStatusLineAction = new ToggleStatusLineAction();
		menuManager.add(toggleStatusLineAction);
	}

	private void showViewMenu() {
		Menu menu = menuManager.createContextMenu(composite.getShell());
		Rectangle bounds = toolItem.getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = toolBar.toDisplay(topLeft);
		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);
	}

	private void createPopupMenu() {
		removeHistoryItemAction = new RemoveHistoryItemAction();
		removeHistoryActionContributionItem = new ActionContributionItem(
				removeHistoryItemAction);

		MenuManager manager = new MenuManager();
		manager.add(removeHistoryActionContributionItem);
		manager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				List<?> selectedElements = ((StructuredSelection) list
						.getSelection()).toList();

				manager.remove(removeHistoryActionContributionItem);

				for (Object item : selectedElements) {
					if (item instanceof ItemsListSeparator
							|| !isHistoryElement(item)) {
						return;
					}
				}

				if (selectedElements.size() > 0) {
					removeHistoryItemAction
							.setText(DialogMessages.FilteredItemsSelectionDialog_removeItemsFromHistoryAction);

					manager.add(removeHistoryActionContributionItem);

				}
			}
		});

		Menu menu = manager.createContextMenu(composite.getShell());
		list.getTable().setMenu(menu);
	}

	public Control createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		composite.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				filterJob.cancel();
				refreshCacheJob.cancel();
				refreshProgressMessageJob.cancel();
			}
		});

		filterHistoryJob = new FilterHistoryJob();
		filterJob = new FilterJob();
		contentProvider = new ContentProvider();
		refreshCacheJob = new RefreshCacheJob();
		refreshProgressMessageJob = new RefreshProgressMessageJob();
		itemsListSeparator = new ItemsListSeparator(
				DialogMessages.FilteredItemsSelectionDialog_separatorLabel);
		selectionMode = NONE;

		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		composite.setLayout(gridLayout);
		createPatternAndMenu(composite);
		createLabels(composite);

		list = new TableViewer(composite, (multi ? SWT.MULTI : SWT.SINGLE)
				| SWT.BORDER | SWT.V_SCROLL | SWT.VIRTUAL);
		list.setContentProvider(contentProvider);
		list.setLabelProvider(getItemsListLabelProvider());
		list.setInput(new Object[0]);
		list.setItemCount(contentProvider.getElements(null).length);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		list.getTable().setLayoutData(gd);

		createPopupMenu();

		pattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				applyFilter();
			}
		});

		pattern.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					if (list.getTable().getItemCount() > 0) {
						list.getTable().setFocus();
					}
				}
			}
		});

		list.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection selection = (StructuredSelection) event
						.getSelection();
				handleSelected(selection);
			}
		});

		list.getTable().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {

				if (e.keyCode == SWT.DEL) {

					List<?> selectedElements = ((StructuredSelection) list
							.getSelection()).toList();

					boolean isSelectedHistory = true;

					for (Object item : selectedElements) {
						if (item instanceof ItemsListSeparator
								|| !isHistoryElement(item)) {
							isSelectedHistory = false;
							break;
						}
					}
					if (isSelectedHistory)
						removeSelectedItems(selectedElements);

				}

				if (e.keyCode == SWT.ARROW_UP && (e.stateMask & SWT.SHIFT) != 0
						&& (e.stateMask & SWT.CTRL) != 0) {
					StructuredSelection selection = (StructuredSelection) list
							.getSelection();

					if (selection.size() == 1) {
						Object element = selection.getFirstElement();
						if (element.equals(list.getElementAt(0))) {
							pattern.setFocus();
						}
						if (list.getElementAt(list.getTable()
								.getSelectionIndex() - 1) instanceof ItemsListSeparator)
							list.getTable().setSelection(
									list.getTable().getSelectionIndex() - 1);
						list.getTable().notifyListeners(SWT.Selection,
								new Event());

					}
				}

				if (e.keyCode == SWT.ARROW_DOWN
						&& (e.stateMask & SWT.SHIFT) != 0
						&& (e.stateMask & SWT.CTRL) != 0) {

					if (list.getElementAt(list.getTable().getSelectionIndex() + 1) instanceof ItemsListSeparator)
						list.getTable().setSelection(
								list.getTable().getSelectionIndex() + 1);
					list.getTable().notifyListeners(SWT.Selection, new Event());
				}

			}
		});

		details = new DetailsContentViewer(composite, SWT.BORDER | SWT.FLAT);
		details.setVisible(toggleStatusLineAction.isChecked());
		details.setContentProvider(new NullContentProvider());
		details.setLabelProvider(getDetailsLabelProvider());

		if (initialPatternText != null) {
			pattern.setText(initialPatternText);
		}

		switch (selectionMode) {
		case CARET_BEGINNING:
			pattern.setSelection(0, 0);
			break;
		case FULL_SELECTION:
			pattern.setSelection(0, initialPatternText.length());
			break;
		}

		// apply filter even if pattern is empty (display history)
		applyFilter();
		pattern.setFocus();

		return composite;
	}

	public void addSelectionChangedListener(
			ISelectionChangedListener changedListener) {
		list.addSelectionChangedListener(changedListener);

	}

	public void setPreSelected(Object preSelected) {
		this.preSelected = preSelected;
	}

	public void selectEntry(Object o) {
		StructuredSelection selection = new StructuredSelection(o);
		list.setSelection(selection);
	}

	/**
	 * Refreshes the details field according to the current selection in the
	 * items list.
	 */
	private void refreshDetails() {
		StructuredSelection selection = getSelectedItems();

		switch (selection.size()) {
		case 0:
			details.setInput(null);
			break;
		case 1:
			details.setInput(selection.getFirstElement());
			break;
		default:
			details.setInput(NLS.bind(
					DialogMessages.FilteredItemsSelectionDialog_nItemsSelected,
					new Integer(selection.size())));
			break;
		}

	}

	/**
	 * Handle selection in the items list by updating labels of selected and
	 * unselected items and refresh the details field using the selection.
	 * 
	 * @param selection
	 *            the new selection
	 */
	protected void handleSelected(StructuredSelection selection) {
		IStatus status = new Status(IStatus.OK, PlatformUI.PLUGIN_ID,
				IStatus.OK, EMPTY_STRING, null);

		if (selection.size() == 0) {
			status = new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID,
					IStatus.ERROR, EMPTY_STRING, null);

			if (lastSelection != null
					&& getListSelectionLabelDecorator() != null) {
				list.update(lastSelection, null);
			}

			lastSelection = null;

		} else {
			status = new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID,
					IStatus.ERROR, EMPTY_STRING, null);

			List<?> items = selection.toList();

			Object item = null;
			IStatus tempStatus = null;

			for (Object o : items) {
				if (o instanceof ItemsListSeparator) {
					continue;
				}

				item = o;
				tempStatus = validateItem(item);

				if (tempStatus.isOK()) {
					status = new Status(IStatus.OK, PlatformUI.PLUGIN_ID,
							IStatus.OK, EMPTY_STRING, null);
				} else {
					status = tempStatus;
					// if any selected element is not valid status is set to
					// ERROR
					break;
				}
			}

			if (lastSelection != null
					&& getListSelectionLabelDecorator() != null) {
				list.update(lastSelection, null);
			}

			if (getListSelectionLabelDecorator() != null) {
				list.update(items.toArray(), null);
			}

			lastSelection = items.toArray();
		}

		refreshDetails();
		updateStatus(status);
	}

	abstract protected void updateStatus(IStatus status);

	/**
	 * Refreshes the dialog - has to be called in UI thread.
	 */
	public void refresh() {
		if (list != null && !list.getTable().isDisposed()) {

			List<?> lastRefreshSelection = ((StructuredSelection) list
					.getSelection()).toList();

			list.setItemCount(contentProvider.getElements(null).length);
			list.refresh();

			if (list.getTable().getItemCount() > 0) {
				// preserve previous selection
				if (refreshWithLastSelection && lastRefreshSelection != null
						&& lastRefreshSelection.size() > 0) {
					list.setSelection(new StructuredSelection(
							lastRefreshSelection));
				} else {
					refreshWithLastSelection = true;

					list.getTable().setSelection(0);

					list.getTable().notifyListeners(SWT.Selection, new Event());
				}
			} else {
				list.setSelection(StructuredSelection.EMPTY);
			}

		}

		scheduleProgressMessageRefresh();
	}

	/**
	 * Updates the progress label.
	 * 
	 * @deprecated
	 */
	public void updateProgressLabel() {
		scheduleProgressMessageRefresh();
	}

	/**
	 * Notifies the content provider - fires filtering of content provider
	 * elements. During the filtering, a separator between history and workspace
	 * matches is added.
	 * <p>
	 * This is a long running operation and should be called in a job.
	 * 
	 * @param checkDuplicates
	 *            <code>true</code> if data concerning elements duplication
	 *            should be computed - it takes much more time than the standard
	 *            filtering
	 * @param monitor
	 *            a progress monitor or <code>null</code> if no monitor is
	 *            available
	 */
	public void reloadCache(boolean checkDuplicates, IProgressMonitor monitor) {
		if (list != null && !list.getTable().isDisposed()
				&& contentProvider != null) {
			contentProvider.reloadCache(checkDuplicates, monitor);
		}
	}

	/**
	 * Schedule refresh job.
	 */
	public void scheduleRefresh() {
		composite.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				refreshCacheJob.cancelAll();
				refreshCacheJob.schedule();
			}
		});
	}

	public void clearAndRefresh() {
		lastCompletedFilter = null;
		filter = null;
		applyFilter();
	}

	/**
	 * Schedules progress message refresh.
	 */
	public void scheduleProgressMessageRefresh() {
		if (filterJob.getState() != Job.RUNNING
				&& refreshProgressMessageJob.getState() != Job.RUNNING)
			refreshProgressMessageJob.scheduleProgressRefresh(null);
	}

	public IStructuredSelection getSelection() {
		List<?> selectedElements = ((IStructuredSelection) list.getSelection())
				.toList();

		List<Object> objectsToReturn = new ArrayList<Object>();

		for (Object item : selectedElements) {
			if (!(item instanceof ItemsListSeparator)) {
				accessedHistoryItem(item);
				objectsToReturn.add(item);
			}
		}

		return new StructuredSelection(objectsToReturn);
	}

	/**
	 * Sets the initial pattern used by the filter. This text is copied into the
	 * selection input on the dialog. A full selection is used in the pattern
	 * input field.
	 * 
	 * @param text
	 *            initial pattern for the filter
	 * @see FilteredList#FULL_SELECTION
	 */
	public void setInitialPattern(String text) {
		setInitialPattern(text, FULL_SELECTION);
	}

	/**
	 * Sets the initial pattern used by the filter. This text is copied into the
	 * selection input on the dialog. The <code>selectionMode</code> is used to
	 * choose selection type for the input field.
	 * 
	 * @param text
	 *            initial pattern for the filter
	 * @param selectionMode
	 *            one of: {@link FilteredList#NONE},
	 *            {@link FilteredList#CARET_BEGINNING},
	 *            {@link FilteredList#FULL_SELECTION}
	 */
	public void setInitialPattern(String text, int selectionMode) {
		this.initialPatternText = text;
		this.selectionMode = selectionMode;
	}

	/**
	 * Gets initial pattern.
	 * 
	 * @return initial pattern, or <code>null</code> if initial pattern is not
	 *         set
	 */
	protected String getInitialPattern() {
		return this.initialPatternText;
	}

	/**
	 * Returns the current selection.
	 * 
	 * @return the current selection
	 */
	protected StructuredSelection getSelectedItems() {
		StructuredSelection selection = (StructuredSelection) list
				.getSelection();

		List<?> selectedItems = selection.toList();
		Object itemToRemove = null;

		for (Object item : selectedItems) {
			if (item instanceof ItemsListSeparator) {
				itemToRemove = item;
				break;
			}
		}

		if (itemToRemove == null)
			return new StructuredSelection(selectedItems);
		// Create a new selection without the collision
		List<Object> newItems = new ArrayList<Object>(selectedItems);
		newItems.remove(itemToRemove);
		return new StructuredSelection(newItems);

	}

	public Control getControl() {
		return composite;
	}

	/**
	 * Validates the item. When items on the items list are selected or
	 * deselected, it validates each item in the selection and the dialog status
	 * depends on all validations.
	 * 
	 * @param item
	 *            an item to be checked
	 * @return status of the dialog to be set
	 */
	protected abstract IStatus validateItem(Object item);

	/**
	 * Creates an instance of a filter.
	 * 
	 * @return a filter for items on the items list. Can be <code>null</code>,
	 *         no filtering will be applied then, causing no item to be shown in
	 *         the list.
	 */
	protected abstract ItemsFilter createFilter();

	/**
	 * Applies the filter created by <code>createFilter()</code> method to the
	 * items list. When new filter is different than previous one it will cause
	 * refiltering.
	 */
	protected void applyFilter() {
		ItemsFilter newFilter = createFilter();

		// don't apply filtering for patterns which mean the same, for example:
		// *a**b and ***a*b
		if (filter != null && filter.equalsFilter(newFilter)) {
			return;
		}

		filterHistoryJob.cancel();
		filterJob.cancel();

		this.filter = newFilter;

		if (this.filter != null) {
			filterHistoryJob.schedule();
		}
	}

	/**
	 * Returns comparator to sort items inside content provider. Returned object
	 * will be probably created as an anonymous class. Parameters passed to the
	 * <code>compare(java.lang.Object, java.lang.Object)</code> are going to be
	 * the same type as the one used in the content provider.
	 * 
	 * @return comparator to sort items content provider
	 */
	protected abstract Comparator<Object> getItemsComparator();

	/**
	 * Fills the content provider with matching items.
	 * 
	 * @param contentProvider
	 *            collector to add items to.
	 *            {@link FilteredList.AbstractContentProvider#add(Object, FilteredList.ItemsFilter)}
	 *            only adds items that pass the given <code>itemsFilter</code>.
	 * @param itemsFilter
	 *            the items filter
	 * @param progressMonitor
	 *            must be used to report search progress. The state of this
	 *            progress monitor reflects the state of the filtering process.
	 * @throws CoreException
	 */
	protected abstract void fillContentProvider(
			AbstractContentProvider contentProvider, ItemsFilter itemsFilter,
			IProgressMonitor progressMonitor) throws CoreException;

	/**
	 * Removes selected items from history.
	 * 
	 * @param items
	 *            items to be removed
	 */
	private void removeSelectedItems(List<?> items) {
		for (Object item : items) {
			removeHistoryItem(item);
		}
		refreshWithLastSelection = false;
		contentProvider.refresh();
	}

	/**
	 * Removes an item from history.
	 * 
	 * @param item
	 *            an item to remove
	 * @return removed item
	 */
	protected Object removeHistoryItem(Object item) {
		return contentProvider.removeHistoryElement(item);
	}

	/**
	 * Adds item to history.
	 * 
	 * @param item
	 *            the item to be added
	 */
	protected void accessedHistoryItem(Object item) {
		contentProvider.addHistoryElement(item);
	}

	/**
	 * Returns a history comparator.
	 * 
	 * @return decorated comparator
	 */
	private Comparator<Object> getHistoryComparator() {
		return new HistoryComparator();
	}

	/**
	 * Returns the history of selected elements.
	 * 
	 * @return history of selected elements, or <code>null</code> if it is not
	 *         set
	 */
	protected SelectionHistory getSelectionHistory() {
		return this.contentProvider.getSelectionHistory();
	}

	/**
	 * Sets new history.
	 * 
	 * @param selectionHistory
	 *            the history
	 */
	protected void setSelectionHistory(SelectionHistory selectionHistory) {
		if (this.contentProvider != null)
			this.contentProvider.setSelectionHistory(selectionHistory);
	}

	/**
	 * Indicates whether the given item is a history item.
	 * 
	 * @param item
	 *            the item to be investigated
	 * @return <code>true</code> if the given item exists in history,
	 *         <code>false</code> otherwise
	 */
	public boolean isHistoryElement(Object item) {
		return this.contentProvider.isHistoryElement(item);
	}

	/**
	 * Indicates whether the given item is a duplicate.
	 * 
	 * @param item
	 *            the item to be investigated
	 * @return <code>true</code> if the item is duplicate, <code>false</code>
	 *         otherwise
	 */
	public boolean isDuplicateElement(Object item) {
		return this.contentProvider.isDuplicateElement(item);
	}

	/**
	 * Sets separator label
	 * 
	 * @param separatorLabel
	 *            the label showed on separator
	 */
	public void setSeparatorLabel(String separatorLabel) {
		this.itemsListSeparator = new ItemsListSeparator(separatorLabel);
	}

	/**
	 * Returns name for then given object.
	 * 
	 * @param item
	 *            an object from the content provider. Subclasses should pay
	 *            attention to the passed argument. They should either only pass
	 *            objects of a known type (one used in content provider) or make
	 *            sure that passed parameter is the expected one (by type
	 *            checking like <code>instanceof</code> inside the method).
	 * @return name of the given item
	 */
	public abstract String getElementName(Object item);

	private class ToggleStatusLineAction extends Action {

		/**
		 * Creates a new instance of the class.
		 */
		public ToggleStatusLineAction() {
			super(
					DialogMessages.FilteredItemsSelectionDialog_toggleStatusAction,
					IAction.AS_CHECK_BOX);
		}

		public void run() {
			details.setVisible(isChecked());
		}
	}

	/**
	 * Only refreshes UI on the basis of an already sorted and filtered set of
	 * items.
	 * <p>
	 * Standard invocation scenario:
	 * <ol>
	 * <li>filtering job (<code>FilterJob</code> class extending
	 * <code>Job</code> class)</li>
	 * <li>cache refresh without checking for duplicates (
	 * <code>RefreshCacheJob</code> class extending <code>Job</code> class)</li>
	 * <li>UI refresh (<code>RefreshJob</code> class extending
	 * <code>UIJob</code> class)</li>
	 * <li>cache refresh with checking for duplicates
	 * (<cod>CacheRefreshJob</code> class extending <code>Job</code> class)</li>
	 * <li>UI refresh (<code>RefreshJob</code> class extending
	 * <code>UIJob</code> class)</li>
	 * </ol>
	 * The scenario is rather complicated, but it had to be applied, because:
	 * <ul>
	 * <li>refreshing cache is rather a long action and cannot be run in the UI
	 * - cannot be run in a UIJob</li>
	 * <li>refreshing cache checking for duplicates is twice as long as
	 * refreshing cache without checking for duplicates; results of the search
	 * could be displayed earlier</li>
	 * <li>refreshing the UI have to be run in a UIJob</li>
	 * </ul>
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.FilterJob
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.RefreshJob
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.RefreshCacheJob
	 */
	private class RefreshJob extends UIJob {

		/**
		 * Creates a new instance of the class.
		 */
		public RefreshJob() {
			super(composite.getDisplay(),
					DialogMessages.FilteredItemsSelectionDialog_refreshJob);
			setSystem(true);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime
		 * .IProgressMonitor)
		 */
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (monitor.isCanceled())
				return new Status(IStatus.OK, KommaEditUIPlugin.PLUGIN_ID,
						IStatus.OK, EMPTY_STRING, null);

			if (FilteredList.this != null) {
				FilteredList.this.refresh();
			}

			return new Status(IStatus.OK, PlatformUI.PLUGIN_ID, IStatus.OK,
					EMPTY_STRING, null);
		}

	}

	/**
	 * Refreshes the progress message cyclically with 500 milliseconds delay.
	 * <code>RefreshProgressMessageJob</code> is strictly connected with
	 * <code>GranualProgressMonitor</code> and use it to to get progress message
	 * and to decide about break of cyclical refresh.
	 */
	private class RefreshProgressMessageJob extends UIJob {

		private GranualProgressMonitor progressMonitor;

		/**
		 * Creates a new instance of the class.
		 */
		public RefreshProgressMessageJob() {
			super(
					composite.getDisplay(),
					DialogMessages.FilteredItemsSelectionDialog_progressRefreshJob);
			setSystem(true);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime
		 * .IProgressMonitor)
		 */
		public IStatus runInUIThread(IProgressMonitor monitor) {

			if (!progressLabel.isDisposed())
				progressLabel.setText(progressMonitor != null ? progressMonitor
						.getMessage() : EMPTY_STRING);

			if (progressMonitor == null || progressMonitor.isDone()) {
				return new Status(IStatus.CANCEL, PlatformUI.PLUGIN_ID,
						IStatus.CANCEL, EMPTY_STRING, null);
			}

			// Schedule cyclical with 500 milliseconds delay
			schedule(500);

			return new Status(IStatus.OK, PlatformUI.PLUGIN_ID, IStatus.OK,
					EMPTY_STRING, null);
		}

		/**
		 * Schedule progress refresh job.
		 * 
		 * @param progressMonitor
		 *            used during refresh progress label
		 */
		public void scheduleProgressRefresh(
				GranualProgressMonitor progressMonitor) {
			this.progressMonitor = progressMonitor;
			// Schedule with initial delay to avoid flickering when the user
			// types quickly
			schedule(200);
		}

	}

	/**
	 * A job responsible for computing filtered items list presented using
	 * <code>RefreshJob</code>.
	 * 
	 * @see RefreshJob
	 * 
	 */
	private class RefreshCacheJob extends Job {

		private RefreshJob refreshJob = new RefreshJob();

		/**
		 * Creates a new instance of the class.
		 */
		public RefreshCacheJob() {
			super(DialogMessages.FilteredItemsSelectionDialog_cacheRefreshJob);
			setSystem(true);
		}

		/**
		 * Stops the job and all sub-jobs.
		 */
		public void cancelAll() {
			cancel();
			refreshJob.cancel();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seeorg.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
		 * IProgressMonitor)
		 */
		protected IStatus run(IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return new Status(IStatus.CANCEL, KommaEditUIPlugin.PLUGIN_ID,
						IStatus.CANCEL, EMPTY_STRING, null);
			}

			if (FilteredList.this != null) {
				GranualProgressMonitor wrappedMonitor = new GranualProgressMonitor(
						monitor);
				FilteredList.this.reloadCache(true, wrappedMonitor);
			}

			if (!monitor.isCanceled()) {
				refreshJob.schedule();
			}

			return new Status(IStatus.OK, PlatformUI.PLUGIN_ID, IStatus.OK,
					EMPTY_STRING, null);

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.runtime.jobs.Job#canceling()
		 */
		protected void canceling() {
			super.canceling();
			contentProvider.stopReloadingCache();
		}

	}

	private class RemoveHistoryItemAction extends Action {

		/**
		 * Creates a new instance of the class.
		 */
		public RemoveHistoryItemAction() {
			super(
					DialogMessages.FilteredItemsSelectionDialog_removeItemsFromHistoryAction);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			List<?> selectedElements = ((StructuredSelection) list
					.getSelection()).toList();
			removeSelectedItems(selectedElements);
		}
	}

	private class ItemsListLabelProvider extends LabelProvider implements
			IColorProvider, IFontProvider, ILabelProviderListener {
		private ILabelProvider provider;

		private ILabelDecorator selectionDecorator;

		// Need to keep our own list of listeners
		private ListenerList listeners = new ListenerList();

		/**
		 * Creates a new instance of the class.
		 * 
		 * @param provider
		 *            the label provider for all items, not <code>null</code>
		 * @param selectionDecorator
		 *            the decorator for selected items, can be <code>null</code>
		 */
		public ItemsListLabelProvider(ILabelProvider provider,
				ILabelDecorator selectionDecorator) {
			Assert.isNotNull(provider);
			this.provider = provider;
			this.selectionDecorator = selectionDecorator;

			provider.addListener(this);

			if (selectionDecorator != null) {
				selectionDecorator.addListener(this);
			}
		}

		/**
		 * Sets new selection decorator.
		 * 
		 * @param newSelectionDecorator
		 *            new label decorator for selected items in the list
		 */
		public void setSelectionDecorator(ILabelDecorator newSelectionDecorator) {
			if (selectionDecorator != null) {
				selectionDecorator.removeListener(this);
				selectionDecorator.dispose();
			}

			selectionDecorator = newSelectionDecorator;

			if (selectionDecorator != null) {
				selectionDecorator.addListener(this);
			}
		}

		/**
		 * Gets selection decorator.
		 * 
		 * @return the label decorator for selected items in the list
		 */
		public ILabelDecorator getSelectionDecorator() {
			return selectionDecorator;
		}

		/**
		 * Sets new label provider.
		 * 
		 * @param newProvider
		 *            new label provider for items in the list, not
		 *            <code>null</code>
		 */
		public void setProvider(ILabelProvider newProvider) {
			Assert.isNotNull(newProvider);
			provider.removeListener(this);
			provider.dispose();

			provider = newProvider;

			if (provider != null) {
				provider.addListener(this);
			}
		}

		/**
		 * Gets the label provider.
		 * 
		 * @return the label provider for items in the list
		 */
		public ILabelProvider getProvider() {
			return provider;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			if (element instanceof ItemsListSeparator) {
				return ExtendedImageRegistry.getInstance().getImage(
						KommaEditUIPlugin.INSTANCE
								.getImage(IEditUIImages.SEPARATOR));
			}

			return provider.getImage(element);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if (element instanceof ItemsListSeparator) {
				return getSeparatorLabel(((ItemsListSeparator) element)
						.getName());
			}

			String str = provider.getText(element);

			if (selectionDecorator != null && element != null) {

				// ((StructuredSelection)list.getSelection()).toList().contains(element))
				// cannot be used - virtual tables produce cycles in
				// update item - get selection invocation scenarios

				int[] selectionIndices = list.getTable().getSelectionIndices();
				List<?> elements = Arrays.asList(contentProvider
						.getElements(null));
				for (int i = 0; i < selectionIndices.length; i++) {
					if (elements.size() > selectionIndices[i]
							&& element
									.equals(elements.get(selectionIndices[i]))) {
						str = selectionDecorator.decorateText(str, element);
						break;
					}
				}
			}
			return str;
		}

		private String getSeparatorLabel(String separatorLabel) {
			Rectangle rect = list.getTable().getBounds();

			int borderWidth = list.getTable().computeTrim(0, 0, 0, 0).width;

			int imageWidth = ExtendedImageRegistry
					.getInstance()
					.getImage(
							KommaEditUIPlugin.INSTANCE
									.getImage(IEditUIImages.VIEW_MENU))
					.getBounds().width;

			int width = rect.width - borderWidth - imageWidth;

			GC gc = new GC(list.getTable());
			gc.setFont(list.getTable().getFont());

			int fSeparatorWidth = gc.textExtent("-").x;
			int fMessageLength = gc.textExtent(separatorLabel).x;

			gc.dispose();

			StringBuffer dashes = new StringBuffer();
			int chars = (((width - fMessageLength) / fSeparatorWidth) / 2) - 2;
			for (int i = 0; i < chars; i++) {
				dashes.append('-');
			}

			StringBuffer result = new StringBuffer();
			result.append(dashes);
			result.append(" " + separatorLabel + " "); //$NON-NLS-1$//$NON-NLS-2$
			result.append(dashes);
			return result.toString().trim();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse
		 * .jface.viewers.ILabelProviderListener)
		 */
		public void addListener(ILabelProviderListener listener) {
			listeners.add(listener);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
		 */
		public void dispose() {
			provider.removeListener(this);
			provider.dispose();

			if (selectionDecorator != null) {
				selectionDecorator.removeListener(this);
				selectionDecorator.dispose();
			}

			super.dispose();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java
		 * .lang.Object, java.lang.String)
		 */
		public boolean isLabelProperty(Object element, String property) {
			if (provider.isLabelProperty(element, property)) {
				return true;
			}
			if (selectionDecorator != null
					&& selectionDecorator.isLabelProperty(element, property)) {
				return true;
			}
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse
		 * .jface.viewers.ILabelProviderListener)
		 */
		public void removeListener(ILabelProviderListener listener) {
			listeners.remove(listener);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.
		 * Object)
		 */
		public Color getBackground(Object element) {
			if (element instanceof ItemsListSeparator) {
				return null;
			}
			if (provider instanceof IColorProvider) {
				return ((IColorProvider) provider).getBackground(element);
			}
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.
		 * Object)
		 */
		public Color getForeground(Object element) {
			if (element instanceof ItemsListSeparator) {
				return Display.getCurrent().getSystemColor(
						SWT.COLOR_WIDGET_NORMAL_SHADOW);
			}
			if (provider instanceof IColorProvider) {
				return ((IColorProvider) provider).getForeground(element);
			}
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
		 */
		public Font getFont(Object element) {
			if (element instanceof ItemsListSeparator) {
				return null;
			}
			if (provider instanceof IFontProvider) {
				return ((IFontProvider) provider).getFont(element);
			}
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.ILabelProviderListener#labelProviderChanged
		 * (org.eclipse.jface.viewers.LabelProviderChangedEvent)
		 */
		public void labelProviderChanged(LabelProviderChangedEvent event) {
			Object[] l = listeners.getListeners();
			for (int i = 0; i < listeners.size(); i++) {
				((ILabelProviderListener) l[i]).labelProviderChanged(event);
			}
		}
	}

	/**
	 * Used in ItemsListContentProvider, separates history and non-history
	 * items.
	 */
	private class ItemsListSeparator {

		private String name;

		/**
		 * Creates a new instance of the class.
		 * 
		 * @param name
		 *            the name of the separator
		 */
		public ItemsListSeparator(String name) {
			this.name = name;
		}

		/**
		 * Returns the name of this separator.
		 * 
		 * @return the name of the separator
		 */
		public String getName() {
			return name;
		}
	}

	/**
	 * GranualProgressMonitor is used for monitoring progress of filtering
	 * process. It is used by <code>RefreshProgressMessageJob</code> to refresh
	 * progress message. State of this monitor illustrates state of filtering or
	 * cache refreshing process.
	 * 
	 * @see GranualProgressMonitor#internalWorked(double)
	 */
	private class GranualProgressMonitor extends ProgressMonitorWrapper {

		private String name;

		private String subName;

		private int totalWork;

		private double worked;

		private boolean done;

		/**
		 * Creates instance of <code>GranualProgressMonitor</code>.
		 * 
		 * @param monitor
		 *            progress to be wrapped
		 */
		public GranualProgressMonitor(IProgressMonitor monitor) {
			super(monitor);
		}

		/**
		 * Checks if filtering has been done
		 * 
		 * @return true if filtering work has been done false in other way
		 */
		public boolean isDone() {
			return done;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.runtime.ProgressMonitorWrapper#setTaskName(java.
		 * lang.String)
		 */
		public void setTaskName(String name) {
			super.setTaskName(name);
			this.name = name;
			this.subName = null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.runtime.ProgressMonitorWrapper#subTask(java.lang
		 * .String)
		 */
		public void subTask(String name) {
			super.subTask(name);
			this.subName = name;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.runtime.ProgressMonitorWrapper#beginTask(java.lang
		 * .String, int)
		 */
		public void beginTask(String name, int totalWork) {
			super.beginTask(name, totalWork);
			if (this.name == null)
				this.name = name;
			this.totalWork = totalWork;
			refreshProgressMessageJob.scheduleProgressRefresh(this);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#worked(int)
		 */
		public void worked(int work) {
			super.worked(work);
			internalWorked(work);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#done()
		 */
		public void done() {
			done = true;
			super.done();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.runtime.ProgressMonitorWrapper#setCanceled(boolean)
		 */
		public void setCanceled(boolean b) {
			done = b;
			super.setCanceled(b);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.runtime.ProgressMonitorWrapper#internalWorked(double
		 * )
		 */
		public void internalWorked(double work) {
			worked = worked + work;
		}

		private String getMessage() {
			if (done)
				return ""; //$NON-NLS-1$

			String message;

			if (name == null) {
				message = subName == null ? "" : subName; //$NON-NLS-1$
			} else {
				message = subName == null ? name
						: NLS.bind(
								DialogMessages.FilteredItemsSelectionDialog_subtaskProgressMessage,
								new Object[] { name, subName });
			}
			if (totalWork == 0)
				return message;

			return NLS
					.bind(DialogMessages.FilteredItemsSelectionDialog_taskProgressMessage,
							new Object[] {
									message,
									new Integer(
											(int) ((worked * 100) / totalWork)) });

		}

	}

	/**
	 * Filters items history and schedule filter job.
	 */
	private class FilterHistoryJob extends Job {

		/**
		 * Filter used during the filtering process.
		 */
		private ItemsFilter itemsFilter;

		/**
		 * Creates new instance of receiver.
		 */
		public FilterHistoryJob() {
			super(DialogMessages.FilteredItemsSelectionDialog_jobLabel);
			setSystem(true);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seeorg.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
		 * IProgressMonitor)
		 */
		protected IStatus run(IProgressMonitor monitor) {
			this.itemsFilter = filter;

			contentProvider.reset();

			refreshWithLastSelection = false;

			contentProvider.addHistoryItems(itemsFilter);

			if (!(lastCompletedFilter != null && lastCompletedFilter
					.isSubFilter(this.itemsFilter)))
				contentProvider.refresh();

			filterJob.schedule();

			return Status.OK_STATUS;
		}

	}

	/**
	 * Filters items in indicated set and history. During filtering, it
	 * refreshes the dialog (progress monitor and elements list).
	 * 
	 * Depending on the filter, <code>FilterJob</code> decides which kind of
	 * search will be run inside <code>filterContent</code>. If the last
	 * filtering is done (last completed filter), is not null, and the new
	 * filter is a sub-filter (
	 * {@link FilteredList.ItemsFilter#isSubFilter(FilteredList.ItemsFilter)})
	 * of the last, then <code>FilterJob</code> only filters in the cache. If it
	 * is the first filtering or the new filter isn't a sub-filter of the last
	 * one, a full search is run.
	 */
	private class FilterJob extends Job {

		/**
		 * Filter used during the filtering process.
		 */
		protected ItemsFilter itemsFilter;

		/**
		 * Creates new instance of FilterJob
		 */
		public FilterJob() {
			super(DialogMessages.FilteredItemsSelectionDialog_jobLabel);
			setSystem(true);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seeorg.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
		 * IProgressMonitor)
		 */
		protected final IStatus run(IProgressMonitor parent) {
			GranualProgressMonitor monitor = new GranualProgressMonitor(parent);
			return doRun(monitor);
		}

		/**
		 * Executes job using the given filtering progress monitor. A hook for
		 * subclasses.
		 * 
		 * @param monitor
		 *            progress monitor
		 * @return result of the execution
		 */
		protected IStatus doRun(GranualProgressMonitor monitor) {
			try {
				internalRun(monitor);
			} catch (CoreException e) {
				cancel();
				return new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID,
						IStatus.ERROR,
						DialogMessages.FilteredItemsSelectionDialog_jobError, e);
			}
			return Status.OK_STATUS;
		}

		/**
		 * Main method for the job.
		 * 
		 * @param monitor
		 * @throws CoreException
		 */
		private void internalRun(GranualProgressMonitor monitor)
				throws CoreException {
			try {
				if (monitor.isCanceled())
					return;

				this.itemsFilter = filter;

				// if (filter.getPattern().length() != 0) {
				filterContent(monitor);
				// }

				if (monitor.isCanceled())
					return;

				contentProvider.refresh();
			} finally {
				monitor.done();
			}
		}

		/**
		 * Filters items.
		 * 
		 * @param monitor
		 *            for monitoring progress
		 * @throws CoreException
		 */
		protected void filterContent(GranualProgressMonitor monitor)
				throws CoreException {

			if (lastCompletedFilter != null
					&& lastCompletedFilter.isSubFilter(this.itemsFilter)) {

				int length = lastCompletedResult.size() / 500;
				monitor.beginTask(
						DialogMessages.FilteredItemsSelectionDialog_cacheSearchJob_taskName,
						length);

				for (int pos = 0; pos < lastCompletedResult.size(); pos++) {

					Object item = lastCompletedResult.get(pos);
					if (monitor.isCanceled())
						break;
					contentProvider.add(item, itemsFilter);

					if ((pos % 500) == 0) {
						monitor.worked(1);
					}
				}

			} else {

				lastCompletedFilter = null;
				lastCompletedResult = null;

				SubProgressMonitor subMonitor = null;
				if (monitor != null) {
					monitor.beginTask(
							DialogMessages.FilteredItemsSelectionDialog_searchJob_taskName,
							100);
					subMonitor = new SubProgressMonitor(monitor, 95);

				}

				fillContentProvider(contentProvider, itemsFilter, subMonitor);

				if (monitor != null && !monitor.isCanceled()) {
					monitor.worked(2);
					contentProvider.rememberResult(itemsFilter);
					monitor.worked(3);
				}
			}

		}

	}

	/**
	 * History stores a list of key, object pairs. The list is bounded at a
	 * certain size. If the list exceeds this size the oldest element is removed
	 * from the list. An element can be added/renewed with a call to
	 * <code>accessed(Object)</code>.
	 * <p>
	 * The history can be stored to/loaded from an XML file.
	 */
	protected static abstract class SelectionHistory {

		private static final String DEFAULT_ROOT_NODE_NAME = "historyRootNode"; //$NON-NLS-1$

		private static final String DEFAULT_INFO_NODE_NAME = "infoNode"; //$NON-NLS-1$

		private static final int MAX_HISTORY_SIZE = 60;

		private final List<Object> historyList;

		private final String rootNodeName;

		private final String infoNodeName;

		private SelectionHistory(String rootNodeName, String infoNodeName) {

			historyList = Collections
					.synchronizedList(new LinkedList<Object>() {

						private static final long serialVersionUID = 0L;

						/*
						 * (non-Javadoc)
						 * 
						 * @see java.util.LinkedList#add(java.lang.Object)
						 */
						public boolean add(Object arg0) {
							if (this.size() > MAX_HISTORY_SIZE)
								this.removeFirst();
							if (!this.contains(arg0))
								return super.add(arg0);
							return false;
						}

					});

			this.rootNodeName = rootNodeName;
			this.infoNodeName = infoNodeName;
		}

		/**
		 * Creates new instance of <code>SelectionHistory</code>.
		 */
		public SelectionHistory() {
			this(DEFAULT_ROOT_NODE_NAME, DEFAULT_INFO_NODE_NAME);
		}

		/**
		 * Adds object to history.
		 * 
		 * @param object
		 *            the item to be added to the history
		 */
		public synchronized void accessed(Object object) {
			historyList.add(object);
		}

		/**
		 * Returns <code>true</code> if history contains object.
		 * 
		 * @param object
		 *            the item for which check will be executed
		 * @return <code>true</code> if history contains object
		 *         <code>false</code> in other way
		 */
		public synchronized boolean contains(Object object) {
			return historyList.contains(object);
		}

		/**
		 * Returns <code>true</code> if history is empty.
		 * 
		 * @return <code>true</code> if history is empty
		 */
		public synchronized boolean isEmpty() {
			return historyList.isEmpty();
		}

		/**
		 * Remove element from history.
		 * 
		 * @param element
		 *            to remove form the history
		 * @return <code>true</code> if this list contained the specified
		 *         element
		 */
		public synchronized boolean remove(Object element) {
			return historyList.remove(element);
		}

		/**
		 * Load history elements from memento.
		 * 
		 * @param memento
		 *            memento from which the history will be retrieved
		 */
		public void load(IMemento memento) {

			IMemento historyMemento = memento.getChild(rootNodeName);

			if (historyMemento == null) {
				return;
			}

			IMemento[] mementoElements = historyMemento
					.getChildren(infoNodeName);
			for (int i = 0; i < mementoElements.length; ++i) {
				IMemento mementoElement = mementoElements[i];
				Object object = restoreItemFromMemento(mementoElement);
				if (object != null) {
					historyList.add(object);
				}
			}
		}

		/**
		 * Save history elements to memento.
		 * 
		 * @param memento
		 *            memento to which the history will be added
		 */
		public void save(IMemento memento) {

			IMemento historyMemento = memento.createChild(rootNodeName);

			Object[] items = getHistoryItems();
			for (int i = 0; i < items.length; i++) {
				Object item = items[i];
				IMemento elementMemento = historyMemento
						.createChild(infoNodeName);
				storeItemToMemento(item, elementMemento);
			}

		}

		/**
		 * Gets array of history items.
		 * 
		 * @return array of history elements
		 */
		public synchronized Object[] getHistoryItems() {
			return historyList.toArray();
		}

		/**
		 * Creates an object using given memento.
		 * 
		 * @param memento
		 *            memento used for creating new object
		 * 
		 * @return the restored object
		 */
		protected abstract Object restoreItemFromMemento(IMemento memento);

		/**
		 * Store object in <code>IMemento</code>.
		 * 
		 * @param item
		 *            the item to store
		 * @param memento
		 *            the memento to store to
		 */
		protected abstract void storeItemToMemento(Object item, IMemento memento);

	}

	/**
	 * Filters elements using SearchPattern by comparing the names of items with
	 * the filter pattern.
	 */
	public abstract class ItemsFilter {

		protected SearchPattern patternMatcher;

		/**
		 * Creates new instance of ItemsFilter.
		 */
		public ItemsFilter() {
			this(new SearchPattern());
		}

		/**
		 * Creates new instance of ItemsFilter.
		 * 
		 * @param searchPattern
		 *            the pattern to be used when filtering
		 */
		public ItemsFilter(SearchPattern searchPattern) {
			patternMatcher = searchPattern;
			String stringPattern = ""; //$NON-NLS-1$
			if (pattern != null && !pattern.getText().equals("*")) { //$NON-NLS-1$
				stringPattern = pattern.getText();
			}
			patternMatcher.setPattern(stringPattern);
		}

		/**
		 * Check if the given filter is a sub-filter of current filter. The
		 * default implementation checks if the <code>SearchPattern</code> from
		 * the current filter is a sub-pattern of the one from the provided
		 * filter.
		 * 
		 * @param filter
		 *            the filter to be checked, or <code>null</code>
		 * @return <code>true</code> if the given filter is sub-filter of the
		 *         current, <code>false</code> if the given filter isn't a
		 *         sub-filter or is <code>null</code>
		 * 
		 * @see org.eclipse.ui.dialogs.SearchPattern#isSubPattern(org.eclipse.ui.dialogs.SearchPattern)
		 */
		public boolean isSubFilter(ItemsFilter filter) {
			if (filter != null) {
				return this.patternMatcher.isSubPattern(filter.patternMatcher);
			}
			return false;
		}

		/**
		 * Checks whether the provided filter is equal to the current filter.
		 * The default implementation checks if <code>SearchPattern</code> from
		 * current filter is equal to the one from provided filter.
		 * 
		 * @param filter
		 *            filter to be checked, or <code>null</code>
		 * @return <code>true</code> if the given filter is equal to current
		 *         filter, <code>false</code> if given filter isn't equal to
		 *         current one or if it is <code>null</code>
		 * 
		 * @see org.eclipse.ui.dialogs.SearchPattern#equalsPattern(org.eclipse.ui.dialogs.SearchPattern)
		 */
		public boolean equalsFilter(ItemsFilter filter) {
			if (filter != null
					&& filter.patternMatcher.equalsPattern(this.patternMatcher)) {
				return true;
			}
			return false;
		}

		/**
		 * Checks whether the pattern's match rule is camel case.
		 * 
		 * @return <code>true</code> if pattern's match rule is camel case,
		 *         <code>false</code> otherwise
		 */
		public boolean isCamelCasePattern() {
			return patternMatcher.getMatchRule() == SearchPattern.RULE_CAMELCASE_MATCH;
		}

		/**
		 * Returns the pattern string.
		 * 
		 * @return pattern for this filter
		 * 
		 * @see SearchPattern#getPattern()
		 */
		public String getPattern() {
			return patternMatcher.getPattern();
		}

		/**
		 * Returns the rule to apply for matching keys.
		 * 
		 * @return match rule
		 * 
		 * @see SearchPattern#getMatchRule()
		 */
		public int getMatchRule() {
			return patternMatcher.getMatchRule();
		}

		/**
		 * Matches text with filter.
		 * 
		 * @param text
		 * @return <code>true</code> if text matches with filter pattern,
		 *         <code>false</code> otherwise
		 */
		protected boolean matches(String text) {
			return patternMatcher.matches(text);
		}

		/**
		 * General method for matching raw name pattern. Checks whether current
		 * pattern is prefix of name provided item.
		 * 
		 * @param item
		 *            item to check
		 * @return <code>true</code> if current pattern is a prefix of name
		 *         provided item, <code>false</code> if item's name is shorter
		 *         than prefix or sequences of characters don't match.
		 */
		public boolean matchesRawNamePattern(Object item) {
			String prefix = patternMatcher.getPattern();
			String text = getElementName(item);

			if (text == null)
				return false;

			int textLength = text.length();
			int prefixLength = prefix.length();
			if (textLength < prefixLength) {
				return false;
			}
			for (int i = prefixLength - 1; i >= 0; i--) {
				if (Character.toLowerCase(prefix.charAt(i)) != Character
						.toLowerCase(text.charAt(i)))
					return false;
			}
			return true;
		}

		/**
		 * Matches an item against filter conditions.
		 * 
		 * @param item
		 * @return <code>true<code> if item matches against filter conditions, <code>false</code>
		 *         otherwise
		 */
		public abstract boolean matchItem(Object item);

		/**
		 * Checks consistency of an item. Item is inconsistent if was changed or
		 * removed.
		 * 
		 * @param item
		 * @return <code>true</code> if item is consistent, <code>false</code>
		 *         if item is inconsistent
		 */
		public abstract boolean isConsistentItem(Object item);

	}

	/**
	 * An interface to content providers for
	 * <code>FilterItemsSelectionDialog</code>.
	 */
	public static abstract class AbstractContentProvider {
		/**
		 * Adds the item to the content provider iff the filter matches the
		 * item. Otherwise does nothing.
		 * 
		 * @param item
		 *            the item to add
		 * @param itemsFilter
		 *            the filter
		 * 
		 * @see FilteredList.ItemsFilter#matchItem(Object)
		 */
		public abstract void add(Object item, ItemsFilter itemsFilter);
	}

	/**
	 * Collects filtered elements. Contains one synchronized, sorted set for
	 * collecting filtered elements. All collected elements are sorted using
	 * comparator. Comparator is returned by getElementComparator() method.
	 * Implementation of <code>ItemsFilter</code> is used to filter elements.
	 * The key function of filter used in to filtering is
	 * <code>matchElement(Object item)</code>.
	 * <p>
	 * The <code>ContentProvider</code> class also provides item filtering
	 * methods. The filtering has been moved from the standard TableView
	 * <code>getFilteredItems()</code> method to content provider, because
	 * <code>ILazyContentProvider</code> and virtual tables are used. This class
	 * is responsible for adding a separator below history items and marking
	 * each items as duplicate if its name repeats more than once on the
	 * filtered list.
	 */
	private class ContentProvider extends AbstractContentProvider implements
			IStructuredContentProvider, ILazyContentProvider {

		private SelectionHistory selectionHistory;

		/**
		 * Raw result of the searching (unsorted, unfiltered).
		 * <p>
		 * Standard object flow:
		 * <code>items -> lastSortedItems -> lastFilteredItems</code>
		 */
		private Set<Object> items;

		/**
		 * Items that are duplicates.
		 */
		private Set<Object> duplicates;

		/**
		 * List of <code>ViewerFilter</code>s to be used during filtering
		 */
		private List<ViewerFilter> filters;

		/**
		 * Result of the last filtering.
		 * <p>
		 * Standard object flow:
		 * <code>items -> lastSortedItems -> lastFilteredItems</code>
		 */
		private List<Object> lastFilteredItems;

		/**
		 * Result of the last sorting.
		 * <p>
		 * Standard object flow:
		 * <code>items -> lastSortedItems -> lastFilteredItems</code>
		 */
		private List<Object> lastSortedItems;

		/**
		 * Used for <code>getFilteredItems()</code> method canceling (when the
		 * job that invoked the method was canceled).
		 * <p>
		 * Method canceling could be based (only) on monitor canceling
		 * unfortunately sometimes the method <code>getFilteredElements()</code>
		 * could be run with a null monitor, the <code>reset</code> flag have to
		 * be left intact.
		 */
		private boolean reset;

		/**
		 * Creates new instance of <code>ContentProvider</code>.
		 * 
		 * @param selectionHistory
		 */
		public ContentProvider(SelectionHistory selectionHistory) {
			this.selectionHistory = selectionHistory;
		}

		/**
		 * Creates new instance of <code>ContentProvider</code>.
		 */
		public ContentProvider() {
			this.items = Collections.synchronizedSet(new HashSet<Object>(2048));
			this.duplicates = Collections.synchronizedSet(new HashSet<Object>(
					256));
			this.lastFilteredItems = new ArrayList<Object>();
			this.lastSortedItems = Collections
					.synchronizedList(new ArrayList<Object>(2048));
		}

		/**
		 * Sets selection history.
		 * 
		 * @param selectionHistory
		 *            The selectionHistory to set.
		 */
		public void setSelectionHistory(SelectionHistory selectionHistory) {
			this.selectionHistory = selectionHistory;
		}

		/**
		 * @return Returns the selectionHistory.
		 */
		public SelectionHistory getSelectionHistory() {
			return selectionHistory;
		}

		/**
		 * Removes all content items and resets progress message.
		 */
		public void reset() {
			reset = true;
			this.items.clear();
			this.duplicates.clear();
			this.lastSortedItems.clear();
		}

		/**
		 * Stops reloading cache - <code>getFilteredItems()</code> method.
		 */
		public void stopReloadingCache() {
			reset = true;
		}

		/**
		 * Adds filtered item.
		 * 
		 * @param item
		 * @param itemsFilter
		 */
		public void add(Object item, ItemsFilter itemsFilter) {
			if (itemsFilter == filter) {
				if (itemsFilter != null) {
					if (itemsFilter.matchItem(item)) {
						this.items.add(item);
					}
				} else {
					this.items.add(item);
				}
			}
		}

		/**
		 * Add all history items to <code>contentProvider</code>.
		 * 
		 * @param itemsFilter
		 */
		public void addHistoryItems(ItemsFilter itemsFilter) {
			if (this.selectionHistory != null) {
				Object[] items = this.selectionHistory.getHistoryItems();
				for (int i = 0; i < items.length; i++) {
					Object item = items[i];
					if (itemsFilter == filter) {
						if (itemsFilter != null) {
							if (itemsFilter.matchItem(item)) {
								if (itemsFilter.isConsistentItem(item)) {
									this.items.add(item);
								} else {
									this.selectionHistory.remove(item);
								}
							}
						}
					}
				}
			}
		}

		/**
		 * Refresh dialog.
		 */
		public void refresh() {
			scheduleRefresh();
		}

		/**
		 * Removes items from history and refreshes the view.
		 * 
		 * @param item
		 *            to remove
		 * 
		 * @return removed item
		 */
		public Object removeHistoryElement(Object item) {
			if (this.selectionHistory != null)
				this.selectionHistory.remove(item);
			if (filter == null || filter.getPattern().length() == 0) {
				items.remove(item);
				duplicates.remove(item);
				this.lastSortedItems.remove(item);
			}

			synchronized (lastSortedItems) {
				Collections.sort(lastSortedItems, getHistoryComparator());
			}
			return item;
		}

		/**
		 * Adds item to history and refresh view.
		 * 
		 * @param item
		 *            to add
		 */
		public void addHistoryElement(Object item) {
			if (this.selectionHistory != null)
				this.selectionHistory.accessed(item);
			if (filter == null || !filter.matchItem(item)) {
				this.items.remove(item);
				this.duplicates.remove(item);
				this.lastSortedItems.remove(item);
			}
			synchronized (lastSortedItems) {
				Collections.sort(lastSortedItems, getHistoryComparator());
			}
			this.refresh();
		}

		/**
		 * @param item
		 * @return <code>true</code> if given item is part of the history
		 */
		public boolean isHistoryElement(Object item) {
			if (this.selectionHistory != null) {
				return this.selectionHistory.contains(item);
			}
			return false;
		}

		/**
		 * Sets/unsets given item as duplicate.
		 * 
		 * @param item
		 *            item to change
		 * 
		 * @param isDuplicate
		 *            duplicate flag
		 */
		public void setDuplicateElement(Object item, boolean isDuplicate) {
			if (this.items.contains(item)) {
				if (isDuplicate)
					this.duplicates.add(item);
				else
					this.duplicates.remove(item);
			}
		}

		/**
		 * Indicates whether given item is a duplicate.
		 * 
		 * @param item
		 *            item to check
		 * @return <code>true</code> if item is duplicate
		 */
		public boolean isDuplicateElement(Object item) {
			return duplicates.contains(item);
		}

		/**
		 * Load history from memento.
		 * 
		 * @param memento
		 *            memento from which the history will be retrieved
		 */
		public void loadHistory(IMemento memento) {
			if (this.selectionHistory != null)
				this.selectionHistory.load(memento);
		}

		/**
		 * Save history to memento.
		 * 
		 * @param memento
		 *            memento to which the history will be added
		 */
		public void saveHistory(IMemento memento) {
			if (this.selectionHistory != null)
				this.selectionHistory.save(memento);
		}

		/**
		 * Gets sorted items.
		 * 
		 * @return sorted items
		 */
		private Object[] getSortedItems() {
			if (lastSortedItems.size() != items.size()) {
				synchronized (lastSortedItems) {
					lastSortedItems.clear();
					lastSortedItems.addAll(items);
					Collections.sort(lastSortedItems, getHistoryComparator());
				}
			}
			return lastSortedItems.toArray();
		}

		/**
		 * Remember result of filtering.
		 * 
		 * @param itemsFilter
		 */
		public void rememberResult(ItemsFilter itemsFilter) {
			List<Object> itemsList = Collections.synchronizedList(Arrays
					.asList(getSortedItems()));
			// synchronization
			if (itemsFilter == filter) {
				lastCompletedFilter = itemsFilter;
				lastCompletedResult = itemsList;
			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.IStructuredContentProvider#getElements(
		 * java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return lastFilteredItems.toArray();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse
		 * .jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.ILazyContentProvider#updateElement(int)
		 */
		public void updateElement(int index) {

			FilteredList.this.list.replace(
					(lastFilteredItems.size() > index) ? lastFilteredItems
							.get(index) : null, index);

		}

		/**
		 * Main method responsible for getting the filtered items and checking
		 * for duplicates. It is based on the
		 * {@link ContentProvider#getFilteredItems(Object, IProgressMonitor)}.
		 * 
		 * @param checkDuplicates
		 *            <code>true</code> if data concerning elements duplication
		 *            should be computed - it takes much more time than standard
		 *            filtering
		 * 
		 * @param monitor
		 *            progress monitor
		 */
		public void reloadCache(boolean checkDuplicates,
				IProgressMonitor monitor) {

			reset = false;

			if (monitor != null) {
				// the work is divided into two actions of the same length
				int totalWork = checkDuplicates ? 200 : 100;

				monitor.beginTask(
						DialogMessages.FilteredItemsSelectionDialog_cacheRefreshJob,
						totalWork);
			}

			// the TableViewer's root (the input) is treated as parent

			lastFilteredItems = Arrays.asList(getFilteredItems(list.getInput(),
					monitor != null ? new SubProgressMonitor(monitor, 100)
							: null));

			if (reset || (monitor != null && monitor.isCanceled())) {
				if (monitor != null)
					monitor.done();
				return;
			}

			if (checkDuplicates) {
				checkDuplicates(monitor);
			}
			if (monitor != null)
				monitor.done();
		}

		private void checkDuplicates(IProgressMonitor monitor) {
			synchronized (lastFilteredItems) {
				IProgressMonitor subMonitor = null;
				int reportEvery = lastFilteredItems.size() / 20;
				if (monitor != null) {
					subMonitor = new SubProgressMonitor(monitor, 100);
					subMonitor
							.beginTask(
									DialogMessages.FilteredItemsSelectionDialog_cacheRefreshJob_checkDuplicates,
									5);
				}
				HashMap<Object, Object> helperMap = new HashMap<Object, Object>();
				for (int i = 0; i < lastFilteredItems.size(); i++) {
					if (reset
							|| (subMonitor != null && subMonitor.isCanceled()))
						return;
					Object item = lastFilteredItems.get(i);

					if (!(item instanceof ItemsListSeparator)) {
						Object previousItem = helperMap.put(
								getElementName(item), item);
						if (previousItem != null) {
							setDuplicateElement(previousItem, true);
							setDuplicateElement(item, true);
						} else {
							setDuplicateElement(item, false);
						}
					}

					if (subMonitor != null && reportEvery != 0
							&& (i + 1) % reportEvery == 0)
						subMonitor.worked(1);
				}
				helperMap.clear();
			}
		}

		/**
		 * Returns an array of items filtered using the provided
		 * <code>ViewerFilter</code>s with a separator added.
		 * 
		 * @param parent
		 *            the parent
		 * @param monitor
		 *            progress monitor, can be <code>null</code>
		 * @return an array of filtered items
		 */
		protected Object[] getFilteredItems(Object parent,
				IProgressMonitor monitor) {
			int ticks = 100;

			if (monitor != null) {
				monitor.beginTask(
						DialogMessages.FilteredItemsSelectionDialog_cacheRefreshJob_getFilteredElements,
						ticks);
				if (filters != null) {
					ticks /= (filters.size() + 2);
				} else {
					ticks /= 2;
				}
			}

			// get already sorted array
			Object[] filteredElements = getSortedItems();

			if (monitor != null) {
				monitor.worked(ticks);
			}

			// filter the elements using provided ViewerFilters
			if (filters != null && filteredElements != null) {
				for (ViewerFilter f : filters) {
					filteredElements = f.filter(list, parent, filteredElements);
					if (monitor != null)
						monitor.worked(ticks);
				}
			}

			if (filteredElements == null || monitor.isCanceled()) {
				if (monitor != null)
					monitor.done();
				return new Object[0];
			}

			ArrayList<Object> preparedElements = new ArrayList<Object>();
			boolean hasHistory = false;

			if (filteredElements.length > 0) {
				if (isHistoryElement(filteredElements[0])) {
					hasHistory = true;
				}
			}

			int reportEvery = filteredElements.length / ticks;

			// add separator
			for (int i = 0; i < filteredElements.length; i++) {
				Object item = filteredElements[i];

				if (hasHistory && !isHistoryElement(item)) {
					preparedElements.add(itemsListSeparator);
					hasHistory = false;
				}

				preparedElements.add(item);

				if (monitor != null && reportEvery != 0
						&& ((i + 1) % reportEvery == 0))
					monitor.worked(1);
			}

			if (monitor != null)
				monitor.done();

			return preparedElements.toArray();
		}

		/**
		 * Adds a filter to this content provider. For an example usage of such
		 * filters look at the project <code>org.eclipse.ui.ide</code>, class
		 * 
		 * <code>org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog.CustomWorkingSetFilter</code>
		 * .
		 * 
		 * 
		 * @param filter
		 *            the filter to be added
		 */
		public void addFilter(ViewerFilter filter) {
			if (filters == null) {
				filters = new ArrayList<ViewerFilter>();
			}
			filters.add(filter);
			// currently filters are only added when dialog is restored
			// if it is changed, refreshing the whole TableViewer should be
			// added
		}

	}

	/**
	 * A content provider that does nothing.
	 */
	private class NullContentProvider implements IContentProvider {

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse
		 * .jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	/**
	 * DetailsContentViewer objects are wrappers for labels.
	 * DetailsContentViewer provides means to change label's image and text when
	 * the attached LabelProvider is updated.
	 */
	private class DetailsContentViewer extends ContentViewer {

		private CLabel label;

		/**
		 * Unfortunately, it was impossible to delegate displaying border to
		 * label. The <code>ViewForm</code> is used because <code>CLabel</code>
		 * displays shadow when border is present.
		 */
		private ViewForm viewForm;

		/**
		 * Constructs a new instance of this class given its parent and a style
		 * value describing its behavior and appearance.
		 * 
		 * @param parent
		 *            the parent component
		 * @param style
		 *            SWT style bits
		 */
		public DetailsContentViewer(Composite parent, int style) {
			viewForm = new ViewForm(parent, style);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			viewForm.setLayoutData(gd);
			label = new CLabel(viewForm, SWT.FLAT);
			label.setFont(parent.getFont());
			viewForm.setContent(label);
			hookControl(label);
		}

		/**
		 * Shows/hides the content viewer.
		 * 
		 * @param visible
		 *            if the content viewer should be visible.
		 */
		public void setVisible(boolean visible) {
			GridData gd = (GridData) viewForm.getLayoutData();
			gd.exclude = !visible;
			viewForm.getParent().layout();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.Viewer#inputChanged(java.lang.Object,
		 * java.lang.Object)
		 */
		protected void inputChanged(Object input, Object oldInput) {
			if (oldInput == null) {
				if (input == null) {
					return;
				}
				refresh();
				return;
			}

			refresh();

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.ContentViewer#handleLabelProviderChanged
		 * (org.eclipse.jface.viewers.LabelProviderChangedEvent)
		 */
		protected void handleLabelProviderChanged(
				LabelProviderChangedEvent event) {
			if (event != null) {
				refresh(event.getElements());
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.Viewer#getControl()
		 */
		public Control getControl() {
			return label;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.Viewer#getSelection()
		 */
		public ISelection getSelection() {
			// not supported
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.Viewer#refresh()
		 */
		public void refresh() {
			Object input = this.getInput();
			if (input != null) {
				ILabelProvider labelProvider = (ILabelProvider) getLabelProvider();
				doRefresh(labelProvider.getText(input),
						labelProvider.getImage(input));
			} else {
				doRefresh(null, null);
			}
		}

		/**
		 * Sets the given text and image to the label.
		 * 
		 * @param text
		 *            the new text or null
		 * @param image
		 *            the new image
		 */
		private void doRefresh(String text, Image image) {
			label.setText(text);
			label.setImage(image);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers
		 * .ISelection, boolean)
		 */
		public void setSelection(ISelection selection, boolean reveal) {
			// not supported
		}

		/**
		 * Refreshes the label if currently chosen element is on the list.
		 * 
		 * @param objs
		 *            list of changed object
		 */
		private void refresh(Object[] objs) {
			if (objs == null || getInput() == null) {
				return;
			}
			Object input = getInput();
			for (int i = 0; i < objs.length; i++) {
				if (objs[i].equals(input)) {
					refresh();
					break;
				}
			}
		}
	}

	/**
	 * Compares items using camel case method.
	 */
	private class CamelCaseComparator implements Comparator<Object> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object o1, Object o2) {
			int leftCategory = getCamelCaseCategory(o1);
			int rightCategory = getCamelCaseCategory(o2);
			if (leftCategory < rightCategory)
				return -1;
			if (leftCategory > rightCategory)
				return +1;
			return getItemsComparator().compare(o1, o2);
		}

		private int getCamelCaseCategory(Object item) {
			if (filter == null)
				return 0;
			if (!filter.isCamelCasePattern())
				return 0;
			return filter.matchesRawNamePattern(item) ? 0 : 1;
		}
	}

	/**
	 * Compares items according to the history.
	 */
	private class HistoryComparator implements Comparator<Object> {

		private CamelCaseComparator camelCaseComparator;

		/**
		 * 
		 */
		public HistoryComparator() {
			this.camelCaseComparator = new CamelCaseComparator();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object o1, Object o2) {
			if ((isHistoryElement(o1) && isHistoryElement(o2))
					|| (!isHistoryElement(o1) && !isHistoryElement(o2)))
				return this.camelCaseComparator.compare(o1, o2);

			if (isHistoryElement(o1))
				return -2;
			if (isHistoryElement(o2))
				return +2;

			return 0;
		}

	}

	/**
	 * Get the control where the search pattern is entered. Any filtering should
	 * be done using an {@link ItemsFilter}. This control should only be
	 * accessed for listeners that wish to handle events that do not affect
	 * filtering such as custom traversal.
	 * 
	 * @return Control or <code>null</code> if the pattern control has not been
	 *         created.
	 */
	public Control getPatternControl() {
		return pattern;
	}

	public void restoreState(IMemento memento) {
		boolean toggleStatusLine = Boolean.TRUE.equals(memento
				.getBoolean(SHOW_STATUS_LINE));

		toggleStatusLineAction.setChecked(toggleStatusLine);
		details.setVisible(toggleStatusLine);

		IMemento historyMemento = memento.getChild(HISTORY_SETTINGS);
		if (historyMemento != null) {
			contentProvider.loadHistory(memento);
		}
	}

	public void saveState(IMemento memento) {
		memento.putBoolean(SHOW_STATUS_LINE, toggleStatusLineAction.isChecked());

		IMemento historyMemento = memento.createChild(HISTORY_SETTINGS);
		this.contentProvider.saveHistory(historyMemento);
	}
}
