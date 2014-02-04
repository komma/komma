package net.enilink.komma.owl.editor.instances;

import java.util.List;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.edit.provider.ISearchableItemProvider;
import net.enilink.komma.edit.provider.SparqlSearchableItemProvider;
import net.enilink.komma.edit.ui.provider.LazyAdapterFactoryContentProvider;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.util.ISparqlConstants;
import net.enilink.komma.model.IObject;

public class InstanceTreePart extends InstancesPart {
	static String QUERY_INSTANCES = ISparqlConstants.PREFIX
	// +
	// "CONSTRUCT {?r a <komma:Result> . ?r a ?t} WHERE {?r a ?c FILTER NOT EXISTS {[a ?c; komma:child ?r]} OPTIONAL {?r a ?t FILTER isIRI(?t)}}";
			+ "SELECT DISTINCT ?r WHERE {?r a ?c FILTER NOT EXISTS {[ a ?c; komma:child ?r ]}}";

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
					return "?instance a ?parent . ?instance komma:descendant ?s . ";
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
					.createQuery(QUERY_INSTANCES).setParameter("c", input)
					.evaluate(IObject.class).toList();

			viewer.setInput(instances.toArray());
		}
	}
}
