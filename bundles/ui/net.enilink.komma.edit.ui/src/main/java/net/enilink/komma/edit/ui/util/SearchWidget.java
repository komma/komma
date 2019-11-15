/*******************************************************************************
 * Copyright (c) 2011 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.ui.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import net.enilink.komma.edit.provider.ISearchableItemProvider;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.internal.IEditUIImages;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;

/**
 * A simple control that provides a text widget for searching in viewers.
 */
public class SearchWidget {
	protected ContentViewer viewer;

	/**
	 * The search text widget to be used by this tree. This value may be
	 * <code>null</code> if there is no search widget, or if the controls have
	 * not yet been created.
	 */
	protected Text searchText;

	/**
	 * The control representing the search button for the search text entry.
	 * This value may be <code>null</code> if no such button exists, or if the
	 * controls have not yet been created.
	 */
	protected ToolBarManager searchToolBar;

	/**
	 * The Composite on which the search controls are created. This is used to
	 * set the background color of the search controls to match the surrounding
	 * controls.
	 */
	protected Composite searchComposite;

	/**
	 * The text to initially show in the search text control.
	 */
	protected String initialText = ""; //$NON-NLS-1$

	/**
	 * Create the controls. Subclasses should override.
	 * 
	 * @param parent
	 * @param treeStyle
	 */
	public Control createControl(Composite parent) {
		searchComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		searchComposite.setLayout(layout);
		searchComposite.setFont(parent.getFont());

		createSearchControls(searchComposite);
		setInitialText("Search...");

		return searchComposite;
	}

	public Control getControl() {
		return searchComposite;
	}

	/**
	 * Create the search controls. By default, a text and corresponding tool bar
	 * button that executes the search is created. Subclasses may override.
	 * 
	 * @param parent
	 *            parent <code>Composite</code> of the search controls
	 * @return the <code>Composite</code> that contains the search controls
	 */
	protected Composite createSearchControls(Composite parent) {
		createSearchText(parent);
		createSearchButton(parent);
		if (searchToolBar != null) {
			searchToolBar.update(false);
		}
		return parent;
	}

	/**
	 * Creates the search text and adds listeners. This method calls
	 * {@link #doCreateSearchText(Composite)} to create the text control.
	 * Subclasses should override {@link #doCreateSearchText(Composite)} instead
	 * of overriding this method.
	 * 
	 * @param parent
	 *            <code>Composite</code> of the search text
	 */
	protected void createSearchText(Composite parent) {
		searchText = doCreateSearchText(parent);
		searchText.addFocusListener(new FocusAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt
			 * .events.FocusEvent)
			 */
			public void focusGained(FocusEvent e) {
				/*
				 * Running in an asyncExec because the selectAll() does not
				 * appear to work when using mouse to give focus to text.
				 */
				Display display = searchText.getDisplay();
				display.asyncExec(new Runnable() {
					public void run() {
						if (!searchText.isDisposed()) {
							if (getInitialText().equals(
									searchText.getText().trim())) {
								searchText.selectAll();
							}
						}
					}
				});
			}
		});

		searchText.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				doSearch(searchText.getText());
			}
		});
		searchText.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
				false));

		// content proposals for searching
		class ObjectProposal extends ContentProposal {
			Object object;

			ObjectProposal(Object object) {
				super("");
				this.object = object;
			}
		}

		ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(
				searchText, new TextContentAdapter(),
				new IContentProposalProvider() {
					@Override
					public IContentProposal[] getProposals(String contents,
							int position) {
						Collection<Object> results = findElements(contents);
						List<IContentProposal> proposals = new ArrayList<IContentProposal>();
						for (Object result : results) {
							proposals.add(new ObjectProposal(result));
						}
						return proposals.toArray(new IContentProposal[proposals
								.size()]);
					}
				}, null, null);
		proposalAdapter.setAutoActivationDelay(750);
		proposalAdapter
				.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_IGNORE);
		proposalAdapter
				.addContentProposalListener(new IContentProposalListener() {
					@Override
					public void proposalAccepted(IContentProposal proposal) {
						viewer.setSelection(new StructuredSelection(
								((ObjectProposal) proposal).object), true);
					}
				});

		proposalAdapter.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				ILabelProvider labelProvider = getLabelProvider();
				return labelProvider != null ? labelProvider
						.getText(((ObjectProposal) element).object) : super
						.getText(element);
			}

			@Override
			public Image getImage(Object element) {
				ILabelProvider labelProvider = getLabelProvider();
				return labelProvider != null ? labelProvider
						.getImage(((ObjectProposal) element).object) : super
						.getImage(element);
			}
		});
	}

	/**
	 * Creates the text control for entering the search text. Subclasses may
	 * override.
	 * 
	 * @param parent
	 *            the parent composite
	 * @return the text widget
	 */
	protected Text doCreateSearchText(Composite parent) {
		return new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.SEARCH
				| SWT.CANCEL);
	}

	/**
	 * Set the background for the widgets that support the search text area.
	 * 
	 * @param background
	 *            background <code>Color</code> to set
	 */
	public void setBackground(Color background) {
		if (searchComposite != null) {
			searchComposite.setBackground(background);
		}
		if (searchToolBar != null && searchToolBar.getControl() != null) {
			searchToolBar.getControl().setBackground(background);
		}
	}

	/**
	 * Create the button that clears the text.
	 * 
	 * @param parent
	 *            parent <code>Composite</code> of toolbar button
	 */
	private void createSearchButton(Composite parent) {
		searchToolBar = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
		searchToolBar.createControl(parent);

		IAction searchAction = new Action("", IAction.AS_PUSH_BUTTON) {//$NON-NLS-1$
			public void run() {
				doSearch(searchText.getText());
			}
		};
		searchAction.setImageDescriptor(ExtendedImageRegistry.getInstance()
				.getImageDescriptor(
						KommaEditUIPlugin.INSTANCE
								.getImage(IEditUIImages.SEARCH)));

		searchAction.setToolTipText("Search");
		// clearTextAction.setImageDescriptor(JFaceResources
		// .getImageRegistry().getDescriptor(
		// ISharedImages.IMG_ETOOL_CLEAR));
		// clearTextAction.setDisabledImageDescriptor(JFaceResources
		// .getImageRegistry().getDescriptor(
		// ISharedImages.));

		searchToolBar.add(searchAction);
	}

	/**
	 * Clears the text in the search text widget.
	 */
	protected void clearText() {
		setSearchText(""); //$NON-NLS-1$
	}

	/**
	 * Set the text in the search control.
	 * 
	 * @param string
	 */
	protected void setSearchText(String string) {
		if (searchText != null) {
			searchText.setText(string);
			selectAll();
		}
	}

	/**
	 * Get the search text for the receiver, if it was created. Otherwise return
	 * <code>null</code>.
	 * 
	 * @return the search Text, or null if it was not created
	 */
	public Text getSearchControl() {
		return searchText;
	}

	/**
	 * Convenience method to return the text of the search control. If the text
	 * widget is not created, then null is returned.
	 * 
	 * @return String in the text, or null if the text does not exist
	 */
	protected String getSearchString() {
		return searchText != null ? searchText.getText() : null;
	}

	/**
	 * Set the text that will be shown until the first focus. A default value is
	 * provided, so this method only need be called if overriding the default
	 * initial text is desired.
	 * 
	 * @param text
	 *            initial text to appear in text field
	 */
	public void setInitialText(String text) {
		initialText = text;
		setSearchText(initialText);
	}

	/**
	 * Select all text in the search text field.
	 * 
	 */
	protected void selectAll() {
		if (searchText != null) {
			searchText.selectAll();
		}
	}

	/**
	 * Get the initial text for the receiver.
	 * 
	 * @return String
	 */
	protected String getInitialText() {
		return initialText;
	}

	protected Collection<Object> findElements(String pattern) {
		IContentProvider provider = viewer.getContentProvider();
		Object input = viewer.getInput();

		Collection<Object> results = new LinkedHashSet<Object>();
		if (provider instanceof ISearchableItemProvider) {
			results.addAll(((ISearchableItemProvider) provider).find(pattern,
					input, 20).toList());
		} else if (provider instanceof AdapterFactoryContentProvider) {
			Object[] elements;
			if (input instanceof Object[]) {
				elements = (Object[]) input;
			} else {
				elements = new Object[] { input };
			}
			for (Object element : elements) {
				ISearchableItemProvider searchableProvider = (ISearchableItemProvider) ((AdapterFactoryContentProvider) provider)
						.getAdapterFactory().adapt(element,
								ISearchableItemProvider.class);
				if (searchableProvider != null) {
					results.addAll(searchableProvider
							.find(pattern, element, 20).toList());
				}
			}
		}
		return results;
	}

	protected ILabelProvider getLabelProvider() {
		return viewer == null ? null : (ILabelProvider) viewer
				.getLabelProvider();
	}

	protected void doSearch(String pattern) {
		Collection<Object> results = findElements(pattern);

		if (!results.isEmpty()) {
			Object selected = null;
			if (results.size() == 1) {
				selected = results.iterator().next();
			} else if (results.size() > 1) {
				ElementListSelectionDialog selectionDialog = new ElementListSelectionDialog(
						getControl().getShell(), getLabelProvider());
				selectionDialog.setHelpAvailable(false);
				selectionDialog.setElements(results.toArray(new Object[results
						.size()]));
				if (selectionDialog.open() == Window.OK) {
					selected = selectionDialog.getFirstResult();
				}
			}
			if (selected != null) {
				viewer.setSelection(new StructuredSelection(selected), true);
			}
		}
	}

	public void setViewer(ContentViewer viewer) {
		this.viewer = viewer;
	}
}
