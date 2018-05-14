package net.enilink.komma.owl.editor.instances;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.Statement;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.properties.EditingHelper.Type;
import net.enilink.komma.edit.provider.ISearchableItemProvider;
import net.enilink.komma.edit.provider.SparqlSearchableItemProvider;
import net.enilink.komma.edit.ui.celleditor.PropertyCellEditingSupport;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.provider.LazyAdapterFactoryContentProvider;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.em.util.ISparqlConstants;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelUtil;
import net.enilink.vocab.rdfs.RDFS;

public class InstanceTablePart extends InstancesPart {
	static final int LIMIT = 10000;

	class ContentProvider extends LazyAdapterFactoryContentProvider implements ISearchableItemProvider {
		Object[] elements;

		public ContentProvider(IAdapterFactory adapterFactory) {
			super(adapterFactory);
		}

		public IExtendedIterator<?> find(Object expression, Object parent, int limit) {
			SparqlSearchableItemProvider searchableProvider = new SparqlSearchableItemProvider() {
				@Override
				protected String getQueryFindPatterns(Object parent) {
					return "?s a ?parent . ";
				}
			};
			return searchableProvider.find(expression, currentInput, 20);
		}
	}

	class LabelProvider extends AdapterFactoryLabelProvider implements ITableLabelProvider {
		public LabelProvider(IAdapterFactory adapterFactory) {
			super(adapterFactory);
		}

		@Override
		public String getColumnText(Object object, int columnIndex) {
			if (properties != null && columnIndex < properties.size()) {
				IProperty property = properties.get(columnIndex);
				IResource resource = (IResource) object;
				if (property == null) {
					// return the object name
					if (!rdfsLabelShown) {
						return super.getColumnText(object, columnIndex);
					} else {
						return ModelUtil.getPName(object);
					}
				}
				// access all values to fill the property set cache
				Object[] values = resource.getAsSet(property).toArray();
				if (values.length > 0) {
					return super.getText(values[0]);
				}
				return null;
			}
			return super.getColumnText(object, columnIndex);
		}

		@Override
		public Image getColumnImage(Object object, int columnIndex) {
			return null;
		}
	}

	class Comparator extends ViewerComparator {
		int column;
		int direction = SWT.UP;

		int getDirection() {
			return direction;
		}

		void setColumn(int column) {
			if (column == this.column) {
				// Same column as last sort; toggle the direction
				direction = direction == SWT.DOWN ? SWT.UP : SWT.DOWN;
			} else {
				// New column; do an ascending sort
				this.column = column;
				direction = SWT.UP;
			}
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			ITableLabelProvider labelProvider = (ITableLabelProvider) ((ContentViewer) viewer).getLabelProvider();
			String l1 = labelProvider.getColumnText(e1, column);
			String l2 = labelProvider.getColumnText(e2, column);
			int result = (l1 == null ? "" : l1).compareToIgnoreCase(l2 == null ? "" : l2);
			return direction == SWT.DOWN ? -result : result;
		}
	}

	protected LabelProvider labelProvider;
	protected IClass currentInput;
	protected boolean sortAscending = true;
	protected IReference sortProperty = null;
	protected List<IProperty> properties;
	protected boolean rdfsLabelShown;

	@Override
	protected StructuredViewer createViewer(Composite parent) {
		Table table = getWidgetFactory().createTable(parent,
				SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
		TableViewer viewer = new TableViewer(table);
		viewer.getTable().setHeaderVisible(true);
		viewer.setUseHashlookup(true);
		return viewer;
	}

	@Override
	protected void adapterFactoryChanged() {
		getViewer().setContentProvider(new ContentProvider(getAdapterFactory()));
		if (labelProvider != null) {
			labelProvider.dispose();
		}
		labelProvider = new LabelProvider(getAdapterFactory());
		createContextMenuFor(getViewer());
	}

	private SelectionAdapter getSelectionAdapter(final TableColumn column, final int index) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableViewer tableViewer = (TableViewer) getViewer();
				Table table = tableViewer.getTable();
				table.setSortColumn(column);
				int dir = table.getSortDirection();
				dir = dir == SWT.UP ? dir = SWT.DOWN : SWT.UP;
				table.setSortDirection(dir);
				sortAscending = dir == SWT.UP;
				IProperty p = properties.get(index);
				sortProperty = p == null ? null : p.getReference();
				refresh();
			}
		};
	}

	@Override
	protected void setInputToViewer(StructuredViewer viewer, IClass input) {
		currentInput = input;
		if (input == null) {
			viewer.setInput(null);
		} else {
			TableViewer tableViewer = (TableViewer) viewer;
			Table table = tableViewer.getTable();
			for (TableColumn column : table.getColumns()) {
				column.dispose();
			}
			properties = new ArrayList<>();
			// null for the object itself
			properties.add(null);
			properties.addAll(input.getEntityManager()
					.createQuery(ISparqlConstants.PREFIX + "SELECT DISTINCT ?p WHERE { ?r a ?c; ?p ?o . "
							+ "filter (?p != rdf:type && not exists { ?r ?otherP ?o . ?otherP rdfs:subPropertyOf ?p filter (?otherP != ?p) }) }")
					.setParameter("c", input).evaluate(IProperty.class).toList());
			int col = 0;
			rdfsLabelShown = false;
			TableColumn sortColumn = null;
			for (final IProperty property : properties) {
				rdfsLabelShown |= RDFS.PROPERTY_LABEL.equals(property);
				TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
				TableColumn column = viewerColumn.getColumn();
				column.setText(property == null ? "Instance" : labelProvider.getText(property));
				column.setResizable(true);
				column.setMoveable(true);
				column.addSelectionListener(getSelectionAdapter(column, col++));
				column.pack();
				if (property == sortProperty || property != null && property.equals(sortProperty)) {
					sortColumn = column;
				}
				viewerColumn.setLabelProvider(new CellLabelProvider() {
					@Override
					public void update(ViewerCell cell) {
						int columnIndex = cell.getColumnIndex();
						cell.setText(labelProvider.getColumnText(cell.getElement(), columnIndex));
					}
				});
				if (property != null) {
					viewerColumn.setEditingSupport(new PropertyCellEditingSupport(((ColumnViewer) viewer), Type.VALUE,
							SWT.MULTI | SWT.WRAP | SWT.V_SCROLL) {
						@Override
						protected IStatement getStatement(Object element) {
							List<IValue> values = ((IResource) element).getPropertyValues(property, false).toList();
							return new Statement((IEntity) element, property, !values.isEmpty() ? values.get(0) : null);
						}

						@Override
						protected IEditingDomain getEditingDomain() {
							return InstanceTablePart.this.getEditingDomain();
						}
					});
				}
			}
			if (sortColumn != null) {
				table.setSortColumn(sortColumn);
				table.setSortDirection(sortAscending ? SWT.UP : SWT.DOWN);
			}

			boolean sortByProperty = sortProperty != null && properties.contains(sortProperty);
			String queryStr;
			if (sortByProperty) {
				queryStr = ISparqlConstants.PREFIX + "SELECT DISTINCT ?r WHERE { ?r a ?c; ?p ?o } order by "
						+ (sortAscending ? "?o" : "desc(?o)") + " limit " + LIMIT;
			} else {
				queryStr = ISparqlConstants.PREFIX + "SELECT DISTINCT ?r WHERE { ?r a ?c } order by "
						+ (sortAscending || sortProperty != null ? "?r" : "desc(?r)") + " limit " + LIMIT;
			}
			IQuery<?> query = input.getEntityManager().createQuery(queryStr).setParameter("c", input);
			if (sortByProperty) {
				query.setParameter("p", sortProperty);
			}
			List<IObject> instances = query.evaluateRestricted(IObject.class).toList();
			viewer.setInput(instances.toArray());
		}
	}
}
