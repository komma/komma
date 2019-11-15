/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.ui.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * PageBook is a class that is capable of stacking several composites (pages),
 * while showing one at a time.
 * 
 */
public class PageBook extends Composite {
	private Control currentPage;
	private Composite emptyPage;
	private org.eclipse.ui.part.PageBook pageBook;
	private Map<Object, Control> pages;

	/**
	 * Creates a new instance in the provided parent and with the provided
	 * style.
	 * 
	 * @param parent
	 *            the control parent
	 * @param style
	 *            the style to use
	 */
	public PageBook(Composite parent, int style) {
		super(parent, style);
		setLayout(new FillLayout());

		pageBook = new org.eclipse.ui.part.PageBook(this, SWT.NULL);
		pages = new HashMap<Object, Control>();
		this.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event e) {
				switch (e.detail) {
				case SWT.TRAVERSE_ESCAPE:
				case SWT.TRAVERSE_RETURN:
				case SWT.TRAVERSE_TAB_NEXT:
				case SWT.TRAVERSE_TAB_PREVIOUS:
					e.doit = true;
					break;
				}
			}
		});
	}

	private Composite createPage() {
		Composite page = new Composite(pageBook, SWT.NULL);
		page.setBackground(getBackground());
		page.setForeground(getForeground());
		page.setMenu(pageBook.getMenu());
		return page;
	}

	/**
	 * Creates a new page for the provided key. Use the returned composite to
	 * create children in it.
	 * 
	 * @param key
	 *            the page key
	 * @return the newly created page composite
	 */
	public Composite createPage(Object key) {
		Composite page = createPage();
		pages.put(key, page);
		return page;
	}

	/**
	 * Returns the page book container.
	 * 
	 * @return the page book container
	 */
	public Composite getContainer() {
		return pageBook;
	}

	/**
	 * Returns the page currently showing.
	 * 
	 * @return the current page
	 */
	public Control getCurrentPage() {
		return currentPage;
	}

	/**
	 * Tests if the page under the provided key is currently in the book.
	 * 
	 * @param key
	 *            the page key
	 * @return <code>true</code> if page exists, <code>false</code> otherwise.
	 */
	public boolean hasPage(Object key) {
		return pages.containsKey(key);
	}

	/**
	 * Registers a page under the privided key to be managed by the page book.
	 * The page must be a direct child of the page book container.
	 * 
	 * @param key
	 *            the page key
	 * @param page
	 *            the page composite to register
	 * @see #createPage(Object)
	 * @see #getContainer
	 */
	public void registerPage(Object key, Control page) {
		pages.put(key, page);
	}

	/**
	 * Removes the page under the provided key from the page book. Does nothing
	 * if page with that key does not exist.
	 * 
	 * @param key
	 *            the page key.
	 */
	public void removePage(Object key) {
		removePage(key, true);
	}

	/**
	 * Removes the page under the provided key from the page book. Does nothing
	 * if page with that key does not exist.
	 * 
	 * @param key
	 *            the page key.
	 * @param showEmptyPage
	 *            if <code>true</code>, shows the empty page after page removal.
	 */
	public void removePage(Object key, boolean showEmptyPage) {
		Control page = pages.get(key);
		if (page != null) {
			pages.remove(key);
			page.dispose();
			if (showEmptyPage)
				showEmptyPage();
		}
	}

	@Override
	public void setBackground(Color color) {
		pageBook.setBackground(color);
		super.setBackground(color);
	}

	/**
	 * Sets focus on the current page if shown.
	 */
	public boolean setFocus() {
		if (currentPage != null)
			return currentPage.setFocus();
		return super.setFocus();
	}

	@Override
	public void setForeground(Color color) {
		pageBook.setForeground(color);
		super.setForeground(color);
	}

	/**
	 * Shows a page with no children to be used if the desire is to not show any
	 * registered page.
	 */
	public void showEmptyPage() {
		if (emptyPage == null) {
			emptyPage = createPage();
			emptyPage.setLayout(new GridLayout());
		}
		pageBook.showPage(emptyPage);
		currentPage = emptyPage;
	}

	/**
	 * Shows the page with the provided key and hides the page previously
	 * showing. Does nothing if the page with that key does not exist.
	 * 
	 * @param key
	 *            the page key
	 */
	public void showPage(Object key) {
		Control page = pages.get(key);
		if (page != null) {
			pageBook.showPage(page);
			if (currentPage != null && currentPage != page) {
				// switching pages - force layout
				if (page instanceof Composite)
					((Composite) page).layout(false);
			}
			currentPage = page;
		} else {
			showEmptyPage();
		}
	}
}
