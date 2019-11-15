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
package net.enilink.komma.sparql.ui.views;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import net.enilink.commons.ui.editor.EditorForm;
import net.enilink.commons.ui.editor.EditorWidgetFactory;
import net.enilink.commons.ui.editor.IEditorForm;
import net.enilink.commons.ui.editor.IEditorPart;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.util.PartListener2Adapter;
import net.enilink.komma.model.IModelSet;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

/**
 * View for executing SPARQL queries.
 * 
 */
public class SparqlView extends ViewPart {
	Reference<IEditingDomainProvider> provider;

	EditorWidgetFactory widgetFactory;

	IPartListener2 partListener;

	QueryPart queryPart;

	class QueryPart {
		ISelectionService selectionService;
		EditorForm editorForm;

		CTabFolder tabFolder;
		CTabItem plusTab;

		QueryPart(ISelectionService selectionService) {
			this.selectionService = selectionService;
		}

		void addQueryTab() {
			plusTab.dispose();
			SparqlPart newPart = new SparqlPart();
			createTab(editorForm, tabFolder, newPart);
			plusTab = createPlusTab();
		}

		CTabItem createPlusTab() {
			CTabItem plusTab = new CTabItem(tabFolder, SWT.NONE);
			plusTab.setText("+");
			return plusTab;
		}

		void createContents(Composite parent) {
			parent.setLayout(new FillLayout());
			editorForm = new EditorForm(parent, widgetFactory) {
				public Object getAdapter(
						@SuppressWarnings("rawtypes") Class adapter) {
					if (IEntityManagerFactory.class.equals(adapter)
							&& provider != null) {
						IEditingDomainProvider p = provider.get();
						return p != null && p.getEditingDomain() != null ? ((IModelSet.Internal) p
								.getEditingDomain().getModelSet())
								.getEntityManagerFactory() : null;
					}
					return null;
				};
			};
			tabFolder = widgetFactory.createTabFolder(editorForm.getBody(),
					SWT.TOP);
			tabFolder.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					addQueryTab();
				}
			});
			plusTab = createPlusTab();
			tabFolder.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (e.item == plusTab) {
						e.doit = false;
						addQueryTab();
					}
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
	}

	@Override
	public void createPartControl(Composite parent) {
		widgetFactory = new EditorWidgetFactory(parent.getDisplay());
		widgetFactory.adapt(parent);
		queryPart = new QueryPart(getSite().getWorkbenchWindow()
				.getSelectionService());
		queryPart.createContents(parent);
	}

	@Override
	public void dispose() {
		if (widgetFactory != null) {
			widgetFactory.dispose();
			widgetFactory = null;
		}
		if (partListener != null) {
			getSite().getPage().removePartListener(partListener);
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
			this.provider = new WeakReference<>(provider);
			ISelectionProvider selectionProvider = part.getSite()
					.getSelectionProvider();
			if (selectionProvider != null) {
				queryPart.setSelection(selectionProvider.getSelection());
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
}
