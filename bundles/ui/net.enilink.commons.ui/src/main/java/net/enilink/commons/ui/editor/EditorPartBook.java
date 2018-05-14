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
package net.enilink.commons.ui.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;

/**
 * This managed form part handles the 'details' portion of the 'master/details'
 * block. It has a page book that manages pages of details registered for the
 * current selection.
 * <p>
 * By default, details part accepts any number of pages. If dynamic page
 * provider is registered, this number may be excessive. To avoid running out of
 * steam (by creating a large number of pages with widgets on each), maximum
 * number of pages can be set to some reasonable value (e.g. 10). When this
 * number is reached, old pages (those created first) will be removed and
 * disposed as new ones are added. If the disposed pages are needed again after
 * that, they will be created again.
 */
public class EditorPartBook extends AbstractEditorPart implements
		ISelectionChangedListener {
	private Composite pageBook;

	private IStructuredSelection lastSelection, currentSelection;
	private Map<Object, PartBag> parts;
	private IEditorPartProvider partProvider;
	private int partLimit = Integer.MAX_VALUE;
	private boolean commitIfDirty;
	private int style = SWT.NONE;

	private static class PartBag {
		private static int counter;
		private int ticket;
		private IEditorPart part;
		private boolean fixed;

		public PartBag(IEditorPart part, boolean fixed) {
			this.part = part;
			this.fixed = fixed;
			this.ticket = ++counter;
		}

		public int getTicket() {
			return ticket;
		}

		public IEditorPart getPart() {
			return part;
		}

		public void dispose() {
			part.dispose();
			part = null;
		}

		public boolean isDisposed() {
			return part == null;
		}

		public boolean isFixed() {
			return fixed;
		}

		public static int getCurrentTicket() {
			return counter;
		}
	}

	/**
	 * Creates a details part by wrapping the provided page book.
	 * 
	 * @param mform
	 *            the parent form
	 * @param scrolledPageBook
	 *            the page book to wrap
	 */
	public EditorPartBook(boolean commitIfDirty) {
		this(commitIfDirty, SWT.V_SCROLL | SWT.H_SCROLL);
	}

	public EditorPartBook(boolean commitIfDirty, int style) {
		this.commitIfDirty = commitIfDirty;
		this.style = style;
		parts = new HashMap<Object, PartBag>();
	}

	/**
	 * Registers the details part to be used for all the objects of the provided
	 * object class.
	 * 
	 * @param objectClass
	 *            an object of type 'java.lang.Class' to be used as a key for
	 *            the provided page
	 * @param page
	 *            the page to show for objects of the provided object class
	 */
	public void registerPart(Class<?> objectClass, IEditorPart page) {
		registerPart(objectClass, page, true);
	}

	private void registerPart(Object objectClass, IEditorPart page,
			boolean fixed) {
		parts.put(objectClass, new PartBag(page, fixed));
		page.initialize(getForm());
	}

	/**
	 * Sets the dynamic page provider. The dynamic provider can return different
	 * pages for objects of the same class based on their state.
	 * 
	 * @param provider
	 *            the provider to use
	 */
	public void setPartProvider(IEditorPartProvider provider) {
		this.partProvider = provider;
	}

	/**
	 * Commits the part by committing the current page.
	 * 
	 * @param onSave
	 *            <code>true</code> if commit is requested as a result of the
	 *            'save' action, <code>false</code> otherwise.
	 */
	public void commit(boolean onSave) {
		IEditorPart page = getCurrentPart();
		if (page != null) {
			commitPart(currentSelection, page, false);
		}
	}

	/**
	 * Returns the current page visible in the part.
	 * 
	 * @return the current page
	 */
	public IEditorPart getCurrentPart() {
		Control control = pageBook instanceof ScrolledPageBook ? ((ScrolledPageBook) pageBook)
				.getCurrentPage()
				: ((PageBook) pageBook).getCurrentPage();
		if (control != null) {
			Object data = control.getData();
			if (data instanceof IEditorPart)
				return (IEditorPart) data;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IFormPart#dispose()
	 */
	public void dispose() {
		for (PartBag pageBag : parts.values()) {
			pageBag.dispose();
		}
	}

	/**
	 * Tests if the currently visible page is dirty.
	 * 
	 * @return <code>true</code> if the page is dirty, <code>false</code>
	 *         otherwise.
	 */
	public boolean isDirty() {
		IEditorPart page = getCurrentPart();
		if (page != null) {
			return page.isDirty();
		}
		return false;
	}

	/**
	 * Tests if the currently visible page is stale and needs refreshing.
	 * 
	 * @return <code>true</code> if the page is stale, <code>false</code>
	 *         otherwise.
	 */
	public boolean isStale() {
		IEditorPart page = getCurrentPart();
		if (page != null) {
			return page.isStale();
		}
		return false;
	}

	/**
	 * Refreshes the current page.
	 */
	public void refresh() {
		IEditorPart page = getCurrentPart();
		if (page != null) {
			page.refresh();
		}
	}

	/**
	 * Sets the focus to the currently visible page.
	 */
	public boolean setFocus() {
		IEditorPart page = getCurrentPart();
		if (page != null) {
			page.setFocus();
			return true;
		}
		return false;
	}

	public Control getControl() {
		return pageBook;
	}

	public boolean setEditorInput(Object input) {
		return false;
	}

	public void selectionChanged(SelectionChangedEvent event) {
		lastSelection = currentSelection;

		selectionChanged(event.getSelection());
	}

	public void selectionChanged(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			currentSelection = (IStructuredSelection) selection;
		} else {
			currentSelection = null;
		}
		update();
	}

	private void update() {
		Object key = null;
		if (currentSelection != null && !currentSelection.isEmpty()) {
			Object firstElement = currentSelection.getFirstElement();
			if (firstElement != null) {
				key = getKey(firstElement);
			}
		}
		showPart(key);
	}

	private Object getKey(Object object) {
		if (partProvider != null) {
			Object key = partProvider.getPartKey(object);
			if (key != null) {
				return key;
			}
		}
		return object.getClass();
	}

	private void showPart(final Object key) {
		checkLimit();
		final IEditorPart oldPart = getCurrentPart();
		if (key != null) {
			PartBag pageBag = (PartBag) parts.get(key);
			IEditorPart part = pageBag != null ? pageBag.getPart() : null;
			if (part == null) {
				// try to get the page dynamically from the provider
				if (partProvider != null) {
					part = partProvider.getPart(key);
					if (part != null) {
						registerPart(key, part, false);
					}
				}
			}
			if (part != null) {
				final IEditorPart fpart = part;
				BusyIndicator.showWhile(pageBook.getDisplay(), new Runnable() {
					public void run() {
						if (!hasPage(key)) {
							Composite parent = createPage(key);
							fpart.createContents(parent);
							parent.setData(fpart);
						}
						// commit the current part
						if (commitIfDirty && oldPart != null
								&& oldPart.isDirty()) {
							commitPart(lastSelection, oldPart, false);
						}
						fpart.setInput(currentSelection.getFirstElement());
						if (oldPart != null) {
							oldPart.deactivate();
						}
						fpart.activate();
						// refresh the new part
						refreshPart(fpart);
						showPage(key);
					}
				});
				return;
			}
		}
		// If we are switching from an old page to nothing,
		// don't loose data
		if (commitIfDirty && oldPart != null && oldPart.isDirty()) {
			commitPart(lastSelection, oldPart, false);
		}
		if (oldPart != null) {
			oldPart.deactivate();
		}
		showEmptyPage();
	}

	private Composite createPage(Object key) {
		if (pageBook instanceof ScrolledPageBook) {
			return ((ScrolledPageBook) pageBook).createPage(key);
		}
		return ((PageBook) pageBook).createPage(key);
	}

	private boolean hasPage(Object key) {
		if (pageBook instanceof ScrolledPageBook) {
			return ((ScrolledPageBook) pageBook).hasPage(key);
		}
		return ((PageBook) pageBook).hasPage(key);
	}

	private void showEmptyPage() {
		if (pageBook instanceof ScrolledPageBook) {
			((ScrolledPageBook) pageBook).showEmptyPage();
		} else {
			((PageBook) pageBook).showEmptyPage();
		}

	}

	private void showPage(Object key) {
		if (pageBook instanceof ScrolledPageBook) {
			((ScrolledPageBook) pageBook).showPage(key);
		} else {
			((PageBook) pageBook).showPage(key);
		}
	}

	private void removePage(Object key, boolean showEmptyPage) {
		if (pageBook instanceof ScrolledPageBook) {
			((ScrolledPageBook) pageBook).removePage(key, showEmptyPage);
		} else {
			((PageBook) pageBook).removePage(key, showEmptyPage);
		}
	}

	protected void refreshPart(IEditorPart detailsPart) {
		detailsPart.refresh();
	}

	protected void commitPart(ISelection selection, IEditorPart detailsPart,
			boolean onSave) {
		detailsPart.commit(onSave);
	}

	private void checkLimit() {
		if (parts.size() <= getPartLimit())
			return;
		// overflow
		int currentTicket = PartBag.getCurrentTicket();
		int cutoffTicket = currentTicket - getPartLimit();
		for (Map.Entry<Object, PartBag> entry : parts.entrySet()) {
			Object key = entry.getKey();
			PartBag pageBag = entry.getValue();
			if (pageBag.getTicket() <= cutoffTicket) {
				// candidate - see if it is active and not fixed
				if (!pageBag.isFixed()
						&& !pageBag.getPart().equals(getCurrentPart())) {
					// drop it
					pageBag.dispose();
					parts.remove(key);
					removePage(key, false);
				}
			}
		}
	}

	/**
	 * Returns the maximum number of pages that should be maintained in this
	 * part. When an attempt is made to add more pages, old pages are removed
	 * and disposed based on the order of creation (the oldest pages are
	 * removed). The exception is made for the page that should otherwise be
	 * disposed but is currently active.
	 * 
	 * @return maximum number of pages for this part
	 */
	public int getPartLimit() {
		return partLimit;
	}

	/**
	 * Sets the page limit for this part.
	 * 
	 * @see #getPartLimit()
	 * @param partLimit
	 *            the maximum number of pages that should be maintained in this
	 *            part.
	 */
	public void setPartLimit(int partLimit) {
		this.partLimit = partLimit;
		checkLimit();
	}

	@Override
	public void createContents(Composite parent) {
		if ((style & (SWT.V_SCROLL | SWT.H_SCROLL)) != 0) {
			this.pageBook = getWidgetFactory().createScrolledPageBook(parent,
					style);
		} else {
			this.pageBook = getWidgetFactory().createPageBook(parent, style);
		}
	}

	@Override
	public void setInput(Object input) {
		if (input != null) {
			selectionChanged(new StructuredSelection(input));
		} else {
			selectionChanged(StructuredSelection.EMPTY);
		}
	}
}
