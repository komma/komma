package net.enilink.komma.owl.editor.internal.individuals;

import java.util.List;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.edit.provider.ISearchableItemProvider;
import net.enilink.komma.edit.provider.SparqlSearchableItemProvider;
import net.enilink.komma.edit.ui.provider.LazyAdapterFactoryContentProvider;
import net.enilink.komma.model.IObject;
import net.enilink.komma.util.ISparqlConstants;

public class InstanceTreePart extends IndividualsPart {
	static String QUERY_INSTANCES = ISparqlConstants.PREFIX
			+ "SELECT DISTINCT ?r WHERE {"
			+ "?r a ?c . OPTIONAL {?other komma:contains ?r} FILTER (!bound(?other)) }";

	class ContentProvider extends LazyAdapterFactoryContentProvider implements
			ISearchableItemProvider {
		public ContentProvider(IAdapterFactory adapterFactory) {
			super(adapterFactory);
		}

		public IExtendedIterator<?> find(Object expression, Object parent,
				int limit) {
			SparqlSearchableItemProvider searchableProvider = new SparqlSearchableItemProvider() {
				@Override
				protected String getQueryFindPatterns(Object parent) {
					return "[a ?parent; komma:hasDescendant ?s]";
				}
			};
			return searchableProvider.find(expression, currentInput, 20);
		}
	}

	protected IClass currentInput;

	@Override
	protected StructuredViewer createViewer(Composite parent) {
		Tree tree = getWidgetFactory().createTree(parent,
				SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL);

		TreeViewer viewer = new TreeViewer(tree);
		viewer.setUseHashlookup(true);

		return viewer;
	}

	@Override
	protected void adapterFactoryChanged() {
		super.adapterFactoryChanged();

		getViewer()
				.setContentProvider(new ContentProvider(getAdapterFactory()));
	}

	@Override
	protected void setInputToViewer(StructuredViewer viewer, IClass input) {
		currentInput = input;
		if (input == null) {
			viewer.setInput(null);
		} else {
			List<IObject> instances = input.getEntityManager()
					.createQuery(QUERY_INSTANCES).setIncludeInferred(true)
					.setParameter("c", input).evaluate(IObject.class).toList();

			viewer.setInput(instances.toArray());
		}
	}
}
