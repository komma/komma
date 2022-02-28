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

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.ui.editor.AbstractEditorPart;
import net.enilink.commons.ui.editor.EditorWidgetFactory;
import net.enilink.commons.ui.editor.PageBook;
import net.enilink.commons.ui.jface.viewers.CComboViewer;
import net.enilink.commons.ui.progress.ProgressDistributor;
import net.enilink.commons.ui.progress.UiProgressMonitorWrapper;
import net.enilink.commons.util.Pair;
import net.enilink.commons.util.extensions.RegistryReader;
import net.enilink.komma.core.*;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelAware;
import net.enilink.komma.sparql.ui.SparqlUI;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SparqlPart extends AbstractEditorPart {
	private class LoadResultsJob extends FinishInUIJob {
		ProgressDistributor progressDistributor;
		String[] columnNames;
		List<Object[]> data;
		IEntityManagerFactory managerFactory;
		String sparql;
		List<IEntity> selected;
		Set<INamespace> namespaces;

		private LoadResultsJob(IEntityManagerFactory managerFactory,
				List<IEntity> selected, Set<INamespace> namespaces,
				String sparql) {
			super("Evaluating SPARQL"); //$NON-NLS-1$
			this.managerFactory = managerFactory;
			this.selected = selected;
			this.namespaces = namespaces;
			this.sparql = sparql;
		}

		@Override
		public void finishInUI(IStatus status) {
			if (status.isOK()) {
				resultArea.setData(namespaces, columnNames, data);
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
			for (IEntity entity : selected) {
				if (entity instanceof IModel) {
					models.add((IModel) entity);
				} else if (entity instanceof IModelAware) {
					models.add(((IModelAware) entity).getModel());
				}
			}
			IUnitOfWork uow;
			IEntityManager managerForQuery;
			if (models.size() != 1) {
				if (this.managerFactory == null) {
					return Status.CANCEL_STATUS;
				}
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
				if (selected.size() > 0) {
					int i = 0;
					for (Object entity : selected) {
						if (i == 0) {
							query.setParameter("selected", entity);
						}
						query.setParameter("selected" + (++i), entity);
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
						Object value = result.next();

						Object[] row = new Object[columnNames.length];
						for (int i = 0; i < columnNames.length; i++) {
							row[i] = value instanceof IBindings<?> ? ((IBindings<?>) value)
									.get(columnNames[i]) : value;
						}
						data.add(row);
					}
					result.close();
				} else if (result instanceof IBooleanResult) {
					columnNames = new String[] { "result" };
					data = new ArrayList<Object[]>();
					data.add(new Object[] { ((IBooleanResult) result)
							.asBoolean() });
				} else if (result instanceof IGraphResult) {
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

		private void setData(Set<INamespace> namespaces, String[] columnNames,
				Collection<Object[]> data) {
			statusLine.setInfo(data.size());
			for (ResultViewerWrapper wrapper : resultViewers) {
				wrapper.setData(namespaces, columnNames, data);
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
			Set<INamespace> namespaces;
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

			void setData(Set<INamespace> namespaces, String[] columnNames,
					Collection<Object[]> data) {
				dataChanged = true;
				this.namespaces = namespaces;
				this.columnNames = columnNames;
				this.data = data;
				applyData();
			}

			private void applyData() {
				if (composite != null
						&& resultViewerContent.getCurrentPage() == composite) {
					viewer.setData(namespaces, columnNames, data);
					dataChanged = false;
				}
			}
		}

	}

	ResultArea resultArea;
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
		final CCombo combo = getWidgetFactory().createCCombo(queryComposite);
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
		combo.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
			}

			@Override
			public void focusGained(FocusEvent e) {
				combo.removeAll();
				for (INamespace namespace : getContextNamespaces()) {
					comboViewer.add(new Pair<String, String>(namespace
							.getPrefix(), namespace.getURI().toString()));
				}
			}
		});

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
		queryText.append("select ?p ?o\nwhere {\n\t?selected ?p ?o\n}\n");

		MenuManager menuManager = new MenuManager();
		menuManager.add(new Action("Format") {
			@Override
			public void run() {
				// TODO implement format action
			}
		});
		queryText.setMenu(menuManager.createContextMenu(queryText));

		// TODO implement simple content proposals without parser
//		ContentProposalAdapter proposalAdapter = ContentProposals
//				.enableContentProposal(queryText, JFaceProposalProvider
//						.wrap(new ParboiledProposalProvider(Parboiled
//								.createParser(Sparql11Parser.class).Query(),
//								new SparqlProposals())), null);
//		proposalAdapter.setAutoActivationDelay(1000);
//		proposalAdapter.setPopupSize(new Point(200, 120));

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

	protected Set<INamespace> getContextNamespaces() {
		Set<IEntityManager> managers = new HashSet<>();
		for (IEntity entity : getSelectedEntities()) {
			managers.add(entity.getEntityManager());
		}
		Set<INamespace> namespaces = new HashSet<>();
		if (managers.isEmpty()) {
			IEntityManagerFactory managerFactory = (IEntityManagerFactory) getForm()
					.getAdapter(IEntityManagerFactory.class);
			if (managerFactory != null) {
				try (IEntityManager manager = managerFactory.get()) {
					namespaces.addAll(manager.getNamespaces().toList());
				}
			}
		} else {
			for (IEntityManager manager : managers) {
				namespaces.addAll(manager.getNamespaces().toList());
			}
		}
		return namespaces;
	}

	protected List<IEntity> getSelectedEntities() {
		List<IEntity> selectedEntities = new ArrayList<>();
		Object formInput = getForm().getInput();
		if (formInput instanceof IStructuredSelection) {
			for (Object selected : ((IStructuredSelection) formInput).toList()) {
				if (selected instanceof IEntity) {
					selectedEntities.add((IEntity) selected);
				}
			}
		}
		return selectedEntities;
	}

	public void loadResultData(String sparql) {
		if (loadJob != null) {
			loadJob.cancel();
		}
		resultArea.startLoading();

		// add common prefix declaration to query
		Set<String> prefixes = new HashSet<>();
		Matcher prefixMatcher = Pattern.compile("prefix\\s+([^:]+)\\s*:",
				Pattern.CASE_INSENSITIVE).matcher(sparql);
		while (prefixMatcher.find()) {
			prefixes.add(prefixMatcher.group(1));
		}
		StringBuilder sb = new StringBuilder();
		Set<INamespace> namespaces = getContextNamespaces();
		for (INamespace namespace : namespaces) {
			if (!prefixes.contains(namespace.getPrefix())) {
				sb.append("PREFIX ").append(namespace.getPrefix())
						.append(": <").append(namespace.getURI()).append(">\n");
			}
		}
		sb.append(sparql);

		IEntityManagerFactory managerFactory = (IEntityManagerFactory) getForm()
				.getAdapter(IEntityManagerFactory.class);
		loadJob = new LoadResultsJob(managerFactory, getSelectedEntities(),
				namespaces, sb.toString());
		loadJob.setUser(true);
		loadJob.schedule();
	}
}