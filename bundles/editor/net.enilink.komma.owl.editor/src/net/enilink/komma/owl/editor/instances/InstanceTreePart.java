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
import net.enilink.vocab.rdf.RDF;

public class InstanceTreePart extends InstancesPart {
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
					if (RDF.TYPE_PROPERTY.equals(currentInput)
							|| currentInput.getRdfsSubClassOf().contains(
									RDF.TYPE_PROPERTY)) {
						return "?s rdfs:subPropertyOf [ a ?parent ] . ";
					}
					return "[ a ?parent; komma:child* ?s ] . ";
				}
			};
			return searchableProvider.find(expression, currentInput, 20);
		}
	}

	protected IClass currentInput;

	@Override
	protected StructuredViewer createViewer(Composite parent) {
		Tree tree = getWidgetFactory().createTree(parent,
				SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL | SWT.MULTI);
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

	protected String instancesQuery() {
		StringBuilder sb = new StringBuilder(ISparqlConstants.PREFIX)
				.append("SELECT DISTINCT ?r WHERE { ?r a ?c FILTER NOT EXISTS { ");
		if (RDF.TYPE_PROPERTY.equals(currentInput)
				|| currentInput.getRdfsSubClassOf().contains(RDF.TYPE_PROPERTY)) {
			sb.append("?other a ?c . ?r rdfs:subPropertyOf ?other FILTER (?r != ?other)");
		} else {
			sb.append("?other a ?c; komma:child ?r FILTER (?r != ?other)");
		}
		sb.append(" }} ORDER BY ?r");
		return sb.toString();
	}

	@Override
	protected void setInputToViewer(StructuredViewer viewer, IClass input) {
		currentInput = input;
		if (input == null) {
			viewer.setInput(null);
		} else {
			List<IObject> instances = input.getEntityManager()
					.createQuery(instancesQuery()).setParameter("c", input)
					.evaluate(IObject.class).toList();
			viewer.setInput(instances.toArray());
		}
	}
}
