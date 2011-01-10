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

import java.util.HashMap;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import net.enilink.commons.ui.editor.EditorForm;
import net.enilink.commons.ui.editor.EditorWidgetFactory;
import net.enilink.commons.ui.editor.IEditorForm;
import net.enilink.commons.ui.editor.IEditorPart;
import net.enilink.commons.ui.editor.PageBook;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.util.PartListener2Adapter;
import net.enilink.komma.model.IModelSet;

/**
 * 
 * @author Ken Wenzel
 */
public class SparqlView extends ViewPart {
	static final String NO_TYPE = ""; //$NON-NLS-1$

	EditorWidgetFactory widgetFactory;

	IPartListener2 partListener;

	PageBook pageBook;

	HashMap<IModelSet, QueryPage> modelSetToQueryPage = new HashMap<IModelSet, QueryPage>();

	class QueryPage {
		IModelSet modelSet;
		ISelectionService selectionService;
		EditorForm editorForm;

		QueryPage(IModelSet modelSet, ISelectionService selectionService) {
			this.modelSet = modelSet;
			this.selectionService = selectionService;
		}

		void createContents() {
			Composite page = pageBook.createPage(this);

			page.setLayout(new FillLayout());

			editorForm = new EditorForm(page, widgetFactory);

			final CTabFolder tabFolder = widgetFactory.createTabFolder(page,
					SWT.TOP);
			tabFolder.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					SparqlPart newPart = new SparqlPart();
					newPart.setInput(((IModelSet.Internal) modelSet)
							.getEntityManagerFactory());
					createTab(editorForm, tabFolder, newPart);
				}
			});

			final ISelectionListener listener = new ISelectionListener() {
				@Override
				public void selectionChanged(IWorkbenchPart part,
						ISelection selection) {
					setSelection(selection);
				}
			};

			if (selectionService != null) {
				selectionService.addSelectionListener(listener);
			}

			tabFolder.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					editorForm.dispose();

					if (selectionService != null) {
						selectionService.removeSelectionListener(listener);
					}
				}
			});
		}

		void activate() {
		}

		void setSelection(ISelection selection) {
			editorForm.setInput(selection);
		}

		void createTab(final IEditorForm editorForm,
				final CTabFolder tabFolder, SparqlPart part) {
			final CTabItem tabItem = new CTabItem(tabFolder, SWT.CLOSE);

			Integer maxTab = (Integer) tabFolder.getData("maxNr");
			maxTab = maxTab == null ? 1 : maxTab + 1;
			tabItem.setData("nr", maxTab);
			tabFolder.setData("maxNr", maxTab);

			tabItem.setText("Query " + maxTab);
			tabItem.setData(part);

			Composite tabContent = widgetFactory.createComposite(tabFolder);
			editorForm.addPart(part);
			part.createContents(tabContent);

			tabItem.setControl(tabContent);
			tabItem.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					if (tabItem.getData("nr")
							.equals(tabFolder.getData("maxNr"))) {
						if (tabFolder.getItemCount() > 0) {
							tabFolder.setData(
									"maxNr",
									tabFolder.getItem(
											tabFolder.getItemCount() - 1)
											.getData("nr"));
						} else {
							tabFolder.setData("maxNr", null);
						}
					}

					editorForm.removePart((IEditorPart) tabItem.getData());
					((IEditorPart) tabItem.getData()).dispose();
				}
			});
			tabFolder.setSelection(tabItem);
		}

		void deactivate() {

		}

	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		widgetFactory = new EditorWidgetFactory(parent.getDisplay());
		widgetFactory.adapt(parent);

		pageBook = widgetFactory.createPageBook(parent, SWT.NONE);
	}

	@Override
	public void dispose() {
		if (widgetFactory != null) {
			widgetFactory.dispose();
			widgetFactory = null;
		}
		if (partListener != null) {
			getViewSite().getPage().removePartListener(partListener);
			partListener = null;
		}
		super.dispose();
	}

	private void setWorkbenchPart(final IWorkbenchPart part) {
		IEditingDomainProvider provider = null;
		if (part instanceof IEditingDomainProvider) {
			provider = (IEditingDomainProvider) part;
		} else {
			provider = (IEditingDomainProvider) part
					.getAdapter(IEditingDomainProvider.class);
		}

		if (provider != null) {
			final IModelSet modelSet = provider.getEditingDomain()
					.getModelSet();
			QueryPage queryPage = null;
			if (modelSet != null) {
				queryPage = modelSetToQueryPage.get(modelSet);
				if (queryPage == null) {
					queryPage = new QueryPage(modelSet, part.getSite()
							.getWorkbenchWindow().getSelectionService());

					queryPage.createContents();

					modelSetToQueryPage.put(modelSet, queryPage);
				}
			}

			Control page = pageBook.getCurrentPage();
			if (page != null && page.getData("queryPage") != null
					&& page.getData("queryPage") != queryPage) {
				((QueryPage) page.getData("queryPage")).deactivate();
			}

			if (queryPage == null) {
				pageBook.showEmptyPage();
			} else {
				ISelectionProvider selectionProvider = part.getSite()
						.getSelectionProvider();
				if (selectionProvider != null) {
					queryPage.setSelection(selectionProvider.getSelection());
				}
				queryPage.activate();
				pageBook.showPage(queryPage);
			}
		}
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);

		this.partListener = new PartListener2Adapter() {
			@Override
			public void partActivated(IWorkbenchPartReference partRef) {
				IWorkbenchPart part = partRef.getPart(false);
				setWorkbenchPart(part);
			}

			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {
			}
		};
		site.getPage().addPartListener(partListener);
	}

	@Override
	public void saveState(IMemento memento) {
	}

	@Override
	public void setFocus() {

	}

	public void setInput(Object input) {

	}

}
