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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.parboiled.Parboiled;

import net.enilink.commons.extensions.RegistryReader;
import net.enilink.commons.iterator.Filter;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.commons.ui.editor.AbstractEditorPart;
import net.enilink.commons.ui.editor.EditorWidgetFactory;
import net.enilink.commons.ui.editor.PageBook;
import net.enilink.commons.ui.jface.viewers.CComboViewer;
import net.enilink.commons.ui.progress.ProgressDistributor;
import net.enilink.commons.ui.progress.UiProgressMonitorWrapper;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.common.ui.assist.ContentProposals;
import net.enilink.komma.edit.ui.assist.ParboiledProposalProvider;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.model.ModelDescription;
import net.enilink.komma.parser.sparql.SparqlParser;
import net.enilink.komma.query.SparqlBuilder;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.IBooleanResult;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.IGraphResult;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.ITupleResult;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.sparql.ui.SparqlUI;
import net.enilink.komma.sparql.ui.assist.SparqlProposals;
import net.enilink.komma.util.Pair;

class SparqlPart extends AbstractEditorPart {
	private class LoadResultsJob extends FinishInUIJob {
		ProgressDistributor progressDistributor;
		String[] columnNames;
		List<Object[]> data;
		IEntityManagerFactory managerFactory;
		String sparql;
		List<IObject> selectedObjects;

		private LoadResultsJob(IEntityManagerFactory managerFactory,
				List<IObject> selectedObjects, String sparql) {
			super("Evaluating SPARQL"); //$NON-NLS-1$
			this.managerFactory = managerFactory;
			this.selectedObjects = selectedObjects;
			if (this.selectedObjects == null) {
				this.selectedObjects = Collections.emptyList();
			}
			this.sparql = sparql;
		}

		@Override
		public void finishInUI(IStatus status) {
			if (status.isOK()) {
				resultArea.setData(columnNames, data);
			} else {
				resultArea.setError(status);
			}
		}

		@Override
		protected void canceling() {
			if (progressDistributor != null) {
				progressDistributor.removeMonitor(uiProgressMonitor);
			}
		}

		@Override
		public IStatus runAsync(IProgressMonitor monitor) {
			progressDistributor = new ProgressDistributor();
			progressDistributor.addMonitor(monitor);
			progressDistributor.addMonitor(uiProgressMonitor);

			progressDistributor.beginTask("Loading results",
					IProgressMonitor.UNKNOWN);

			Set<IModel> models = new HashSet<IModel>();
			for (IObject selected : selectedObjects) {
				models.add(selected.getModel());
			}
			IUnitOfWork uow;
			IEntityManager managerForQuery;
			if (models.size() != 1) {
				uow = this.managerFactory.getUnitOfWork();
				uow.begin();
				managerForQuery = this.managerFactory.get();
			} else {
				IModel model = models.iterator().next();
				uow = model.getModelSet().getUnitOfWork();
				uow.begin();
				managerForQuery = model.getManager();
			}

			try {
				IQuery<?> query = managerForQuery.createQuery(sparql);
				query.setIncludeInferred(true);
				if (selectedObjects.size() > 0) {
					int i = 0;
					for (Object selected : selectedObjects) {
						if (i == 0) {
							query.setParameter("selected", selected);
						}
						query.setParameter("selected" + (++i), selected);
					}
				}
				IExtendedIterator<?> result = query.evaluate();
				if (result instanceof ITupleResult<?>) {
					columnNames = ((ITupleResult<?>) result).getBindingNames()
							.toArray(
									new String[((ITupleResult<?>) result)
											.getBindingNames().size()]);
					data = new ArrayList<Object[]>();
					while (result.hasNext()) {
						IBindings<?> bindings = (IBindings<?>) result.next();

						Object[] row = new Object[columnNames.length];
						for (int i = 0; i < columnNames.length; i++) {
							row[i] = bindings.get(columnNames[i]);
						}
						data.add(row);
					}
					result.close();
				} else if (query instanceof IBooleanResult) {
					columnNames = new String[] { "result" };
					data = new ArrayList<Object[]>();
					data.add(new Object[] { ((IBooleanResult) result)
							.asBoolean() });
				} else if (query instanceof IGraphResult) {
					columnNames = new String[] { "subject", "predicate",
							"object" };
					data = new ArrayList<Object[]>();
					while (result.hasNext()) {
						IStatement stmt = ((IGraphResult) result).next();

						data.add(new Object[] { stmt.getSubject(),
								stmt.getPredicate(), stmt.getObject() });
					}
					result.close();
				}

			} catch (Exception e) {
				return new Status(IStatus.ERROR, SparqlUI.PLUGIN_ID,
						"Error executing query", e);
			} finally {
				uow.end();
				progressDistributor.done();
			}

			return Status.OK_STATUS;
		}
	}

	class StatusLine {
		EditorWidgetFactory widgetFactory;
		Label rowsLabel;

		StatusLine(EditorWidgetFactory widgetFactory) {
			this.widgetFactory = widgetFactory;
		}

		void createContents(Composite parent) {
			parent.setLayout(new GridLayout(2, false));

			Composite labelComposite = widgetFactory.createComposite(parent);
			labelComposite.setLayout(new RowLayout());
			labelComposite.setLayoutData(new GridData(SWT.BEGINNING,
					SWT.DEFAULT, false, false));

			rowsLabel = widgetFactory.createLabel(labelComposite, ""); //$NON-NLS-1$
		}

		void setInfo(int rowCount) {
			rowsLabel.setText(rowCount + " " //$NON-NLS-1$
					+ (rowCount == 1 ? "result" : "results")); //$NON-NLS-1$ //$NON-NLS-2$
			rowsLabel.getParent().layout(true);
		}
	}

	LoadResultsJob loadJob;

	IProgressMonitor uiProgressMonitor;

	Composite parent;

	class ResultArea {
		PageBook pageBook, resultViewerContent;
		StatusLine statusLine;

		ResultViewerWrapper[] resultViewers;

		Composite progressComposite, dataComposite, errorComposite;

		ResultArea(Composite composite) {
			composite.setLayout(new FillLayout());

			pageBook = getWidgetFactory().createPageBook(composite, SWT.NONE);

			progressComposite = pageBook.createPage("progress");
			progressComposite.setLayout(new GridLayout(1, false));
			ProgressMonitorPart progressViewer = new ProgressMonitorPart(
					progressComposite, null);
			progressViewer.setData(EditorWidgetFactory.KEY_DRAW_BORDER,
					EditorWidgetFactory.TEXT_BORDER);
			progressViewer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
					true, true));

			uiProgressMonitor = new UiProgressMonitorWrapper(progressViewer,
					getShell().getDisplay());

			dataComposite = pageBook.createPage("data");
			dataComposite.setLayout(new GridLayout(1, false));

			List<IResultViewer> viewers = getResultViewers();
			resultViewers = new ResultViewerWrapper[viewers.size()];
			int i = 0;
			for (IResultViewer viewer : viewers) {
				resultViewers[i++] = new ResultViewerWrapper(viewer);
			}

			if (resultViewers.length > 1) {
				Composite tabsComposite = getWidgetFactory().createComposite(
						dataComposite);
				tabsComposite.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT,
						true, false));

				createTabs(tabsComposite);
			}

			resultViewerContent = getWidgetFactory().createPageBook(
					dataComposite, SWT.NONE);
			resultViewerContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
					true, true));

			Composite statusComposite = getWidgetFactory().createComposite(
					dataComposite);
			statusComposite.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT,
					true, false));
			statusLine = new StatusLine(getWidgetFactory());
			statusLine.createContents(statusComposite);

			pageBook.showEmptyPage();
		}

		public void setError(IStatus status) {
			pageBook.showEmptyPage();
		}

		private void createTabs(Composite composite) {
			composite.setLayout(new RowLayout());

			for (final ResultViewerWrapper wrapper : resultViewers) {
				wrapper.link = getWidgetFactory().createHyperlink(composite,
						wrapper.viewer.getName(), SWT.NONE);
				wrapper.link.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
						wrapper.show();
					}
				});
			}
		}

		void startLoading() {
			pageBook.showPage("progress");
		}

		List<IResultViewer> getResultViewers() {
			final List<IResultViewer> resultViewers = new ArrayList<IResultViewer>();
			RegistryReader reader = new RegistryReader(SparqlUI.PLUGIN_ID,
					"resultViewers") {
				@Override
				protected boolean readElement(IConfigurationElement element) {
					if (!"resultViewer".equals(element.getName())) {
						return false;
					}

					String className = element.getAttribute("class");
					if (className == null || className.trim().isEmpty()) {
						logMissingAttribute(element, "class");

						return true;
					}

					try {
						Object viewerObj = element
								.createExecutableExtension("class");

						if (!(viewerObj instanceof IResultViewer)) {
							logError(element,
									"Result viewer does not implement expected interface "
											+ IResultViewer.class.getName());
						} else {
							resultViewers.add((IResultViewer) viewerObj);
						}
					} catch (CoreException e) {
						logError(element, e.getMessage());
					}
					return true;
				}
			};
			reader.readRegistry();
			return resultViewers;
		}

		private void setData(String[] columnNames, Collection<Object[]> data) {
			statusLine.setInfo(data.size());

			for (ResultViewerWrapper wrapper : resultViewers) {
				wrapper.setData(columnNames, data);
			}
			if (resultViewerContent.getCurrentPage() == null
					&& resultViewers.length > 0) {
				resultViewers[0].show();
			}

			pageBook.showPage("data");
		}

		class ResultViewerWrapper {
			Composite composite;

			IResultViewer viewer;
			Hyperlink link;

			int limit;
			String[] columnNames;
			Collection<Object[]> data;

			boolean dataChanged = false;

			ResultViewerWrapper(IResultViewer viewer) {
				this.viewer = viewer;
			}

			void show() {
				if (!resultViewerContent.hasPage(viewer)) {
					composite = resultViewerContent.createPage(viewer);

					viewer.createContents(getWidgetFactory(), composite);
				}
				resultViewerContent.showPage(viewer);
				applyData();
			}

			void setData(String[] columnNames, Collection<Object[]> data) {
				dataChanged = true;

				this.columnNames = columnNames;
				this.data = data;

				applyData();
			}

			private void applyData() {
				if (viewer != null
						&& resultViewerContent.getCurrentPage() == composite) {
					viewer.setData(columnNames, data);

					dataChanged = false;
				}
			}
		}

	}

	ResultArea resultArea;
	IEntityManagerFactory managerFactory;
	Text queryText;

	@Override
	public void createContents(Composite parent) {
		this.parent = parent;

		parent.setLayout(new FillLayout());
		Section section = getWidgetFactory().createSection(parent,
				Section.TITLE_BAR | Section.EXPANDED);
		section.setText("SPARQL"); //$NON-NLS-1$

		getWidgetFactory().createCompositeSeparator(section);

		Composite client = getWidgetFactory().createComposite(section);
		getWidgetFactory().paintBordersFor(client);
		section.setClient(client);
		client.setLayout(new FillLayout());

		SashForm sash = new SashForm(client, SWT.VERTICAL);

		Composite queryComposite = getWidgetFactory().createComposite(sash);
		queryComposite.setLayout(new GridLayout(2, false));

		// Selection-Field for namespaces
		CCombo combo = getWidgetFactory().createCCombo(queryComposite);
		GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		combo.setLayoutData(gridData);

		final CComboViewer comboViewer = new CComboViewer(combo);
		comboViewer.setLabelProvider(new LabelProvider() {

			@Override
			public String getText(Object element) {
				return ((Pair<?, ?>) element).getFirst() + ": <"
						+ ((Pair<?, ?>) element).getSecond() + ">";
			}
		});

		IEntityManager manager = managerFactory.get();
		for (INamespace namespace : manager.getNamespaces()) {
			comboViewer.add(new Pair<String, String>(namespace.getPrefix(),
					namespace.getURI().toString()));
		}

		Hyperlink addPrefixLink = getWidgetFactory().createHyperlink(
				queryComposite, "Add", SWT.NONE);
		addPrefixLink.addHyperlinkListener(new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				Pair<?, ?> prefix = (Pair<?, ?>) ((IStructuredSelection) comboViewer
						.getSelection()).getFirstElement();
				if (prefix != null) {
					queryText.setText(("PREFIX " + prefix.getFirst() + ": <"
							+ prefix.getSecond() + ">\r\n" + queryText
							.getText()).replace("\r\n", "\n"));
				}
			}
		});

		queryText = getWidgetFactory().createText(queryComposite, "",
				SWT.MULTI | SWT.V_SCROLL);

		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		queryText.setLayoutData(gridData);

		// Add prefixes
		queryText.append("PREFIX owl: <" + OWL.NAMESPACE + ">\n");
		queryText.append("PREFIX rdf: <" + RDF.NAMESPACE + ">\n");
		queryText.append("PREFIX rdfs: <" + RDFS.NAMESPACE + ">\n");
		for (ModelDescription modelDescription : ModelCore.getBaseModels())
			queryText.append("PREFIX " + modelDescription.getPrefix() + ": <"
					+ modelDescription.getNamespace() + ">\n");

		queryText.append("\nselect ?p ?o\nwhere {\n\t?selected ?p ?o\n}\n");

		MenuManager menuManager = new MenuManager();
		menuManager.add(new Action("Format") {
			@Override
			public void run() {
				try {
					queryText.setText(new SparqlBuilder(queryText.getText())
							.toString());
				} catch (Exception e) {
					// ignore
				}
			}
		});
		queryText.setMenu(menuManager.createContextMenu(queryText));
		ContentProposals.enableContentProposal(
				queryText,
				new ParboiledProposalProvider(Parboiled.createParser(
						SparqlParser.class).Query(), new SparqlProposals()),
				null);

		Button button = getWidgetFactory().createButton(queryComposite, "Run",
				SWT.PUSH);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String sparql = queryText.getText();
				loadResultData(sparql);
			}
		});
		button.setLayoutData(new GridData(SWT.NONE, SWT.BOTTOM, false, false));

		Composite resultsComposite = getWidgetFactory().createComposite(sash);
		resultsComposite.setLayout(new FillLayout());

		resultArea = new ResultArea(resultsComposite);
		sash.setWeights(new int[] { 40, 60 });
	}

	@Override
	public void setInput(Object input) {
		this.managerFactory = (IEntityManagerFactory) input;
	}

	public void loadResultData(String sparql) {
		if (loadJob != null) {
			loadJob.cancel();
		}
		resultArea.startLoading();

		List<IObject> selectedObjects = Collections.emptyList();
		Object formInput = getForm().getInput();
		if (formInput instanceof IStructuredSelection) {
			@SuppressWarnings("unchecked")
			List<IObject> selected = (List<IObject>) WrappedIterator
					.create(((IStructuredSelection) formInput).toList()
							.iterator()).filterKeep(new Filter<Object>() {
						@Override
						public boolean accept(Object o) {
							return o instanceof IObject;
						}
					}).toList();
			selectedObjects = selected;
		}

		loadJob = new LoadResultsJob(managerFactory, selectedObjects, sparql);
		loadJob.setUser(true);
		loadJob.schedule();
	}
}