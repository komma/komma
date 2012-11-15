package net.enilink.komma.owl.editor.internal.instances;

import java.util.List;

import org.eclipse.jface.viewers.AbstractTableViewer;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
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
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.provider.ISearchableItemProvider;
import net.enilink.komma.edit.provider.SparqlSearchableItemProvider;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IReference;
import net.enilink.komma.util.ISparqlConstants;

public class InstanceTablePart extends InstancesPart {
	static final int LIMIT = 1000;
	static final String QUERY_INSTANCES = ISparqlConstants.PREFIX
			+ "SELECT DISTINCT ?r WHERE { ?r a ?c } limit " + LIMIT;

	class ContentProvider extends AdapterFactoryContentProvider implements
			ILazyContentProvider, ISearchableItemProvider {
		Object[] elements;

		public ContentProvider(IAdapterFactory adapterFactory) {
			super(adapterFactory);
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			elements = newInput instanceof Object[] ? (Object[]) newInput
					: null;
			super.inputChanged(viewer, oldInput, newInput);
			((AbstractTableViewer) viewer)
					.setItemCount(elements != null ? elements.length : 0);
		}

		public IExtendedIterator<?> find(Object expression, Object parent,
				int limit) {
			SparqlSearchableItemProvider searchableProvider = new SparqlSearchableItemProvider() {
				@Override
				protected String getQueryFindPatterns(Object parent) {
					return "?instance a ?parent . ";
				}
			};
			return searchableProvider.find(expression, currentInput, 20);
		}

		@Override
		public void updateElement(int index) {
			if (elements != null && index < elements.length) {
				((AbstractTableViewer) viewer).replace(currentInput
						.getEntityManager().find((IReference) elements[index]),
						index);
			}
		}
	}

	class LabelProvider extends AdapterFactoryLabelProvider implements
			ITableLabelProvider {
		public LabelProvider(IAdapterFactory adapterFactory) {
			super(adapterFactory);
		}

		@Override
		public String getColumnText(Object object, int columnIndex) {
			if (properties != null && columnIndex < properties.size()) {
				IProperty property = properties.get(columnIndex);
				IResource resource = (IResource) object;
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
			ITableLabelProvider labelProvider = (ITableLabelProvider) ((ContentViewer) viewer)
					.getLabelProvider();
			String l1 = labelProvider.getColumnText(e1, column);
			String l2 = labelProvider.getColumnText(e2, column);
			int result = (l1 == null ? "" : l1)
					.compareToIgnoreCase(l2 == null ? "" : l2);
			return direction == SWT.DOWN ? -result : result;
		}
	}

	protected IClass currentInput;
	protected List<IProperty> properties;
	protected Comparator comparator = new Comparator();

	@Override
	protected StructuredViewer createViewer(Composite parent) {
		Table table = getWidgetFactory().createTable(parent,
				SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL);
		TableViewer viewer = new TableViewer(table);
		viewer.getTable().setHeaderVisible(true);
		viewer.setUseHashlookup(true);
		return viewer;
	}

	@Override
	protected void adapterFactoryChanged() {
		getViewer()
				.setContentProvider(new ContentProvider(getAdapterFactory()));
		getViewer().setLabelProvider(new LabelProvider(getAdapterFactory()));
		createContextMenuFor(getViewer());
	}

	private SelectionAdapter getSelectionAdapter(final TableColumn column,
			final int index) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				comparator.setColumn(index);
				int dir = comparator.getDirection();
				TableViewer tableViewer = (TableViewer) getViewer();
				tableViewer.getTable().setSortColumn(column);
				tableViewer.getTable().setSortDirection(dir);
				Object[] elements = (Object[]) tableViewer.getInput();
				if (elements != null) {
					comparator.sort(tableViewer, elements);
				}
				tableViewer.refresh();
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

			IProperty sortProperty = null;
			TableColumn sortColumn = tableViewer.getTable().getSortColumn();
			for (TableColumn column : tableViewer.getTable().getColumns()) {
				if (column == sortColumn) {

				}
				column.dispose();
			}
			properties = input
					.getEntityManager()
					.createQuery(
							ISparqlConstants.PREFIX
									+ "SELECT DISTINCT ?p WHERE { "
									+ "{ select ?r where { ?r a ?c } limit "
									+ LIMIT
									+ " } ?r ?p ?o filter (isLiteral(?o) && not exists {?r ?otherP ?o . ?otherP rdfs:subPropertyOf ?p filter (?otherP != ?p)})}")
					.setParameter("c", input).evaluate(IProperty.class)
					.toList();

			int col = 0;
			for (IProperty property : properties) {
				TableColumn column = new TableColumn(tableViewer.getTable(),
						SWT.LEFT);
				column.setText(((ILabelProvider) viewer.getLabelProvider())
						.getText(property));
				column.setResizable(true);
				column.setMoveable(true);
				column.addSelectionListener(getSelectionAdapter(column, col++));
				column.pack();
			}
			List<IObject> instances = input.getEntityManager()
					.createQuery(QUERY_INSTANCES).setParameter("c", input)
					.evaluateRestricted(IObject.class).toList();
			viewer.setInput(instances.toArray());
		}
	}
}
