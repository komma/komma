package net.enilink.komma.owl.editor.instances;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.edit.provider.ISearchableItemProvider;
import net.enilink.komma.edit.provider.IViewerNotification;
import net.enilink.komma.edit.provider.SparqlSearchableItemProvider;
import net.enilink.komma.edit.ui.provider.LazyAdapterFactoryContentProvider;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.util.ISparqlConstants;
import net.enilink.komma.model.IObject;
import net.enilink.vocab.rdf.RDF;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

public class InstanceTreePart extends InstancesPart {
	class ContentProvider extends InstancesPart.ContentProvider implements
			ISearchableItemProvider {
		public ContentProvider(IAdapterFactory adapterFactory) {
			super(adapterFactory);
		}

		public IExtendedIterator<?> find(Object expression, Object parent,
				int limit) {
			Object input = getViewer().getInput();
			SparqlSearchableItemProvider searchableProvider = new SparqlSearchableItemProvider() {
				@Override
				protected String getQueryFindPatterns(Object parent) {
					if (RDF.TYPE_PROPERTY.equals(input)
							|| (input instanceof IClass && ((IClass)input).getRdfsSubClassOf().contains(
									RDF.TYPE_PROPERTY))) {
						return "?s rdfs:subPropertyOf [ a ?parent ] . ";
					}
					return "[ a ?parent; komma:child* ?s ] . ";
				}
			};
			return searchableProvider.find(expression, input, 20);
		}
	}

	@Override
	protected StructuredViewer createViewer(Composite parent) {
		Tree tree = getWidgetFactory().createTree(parent,
				SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL | SWT.MULTI);
		TreeViewer viewer = new TreeViewer(tree);
		viewer.setUseHashlookup(true);
		return viewer;
	}

	protected IContentProvider createContentProvider() {
		return new ContentProvider(getAdapterFactory());
	}

	protected String instancesQuery(IClass input) {
		StringBuilder sb = new StringBuilder(ISparqlConstants.PREFIX)
				.append("SELECT DISTINCT ?r WHERE { { select ?type { ?type rdfs:subClassOf* ?c } } ?r a ?type FILTER NOT EXISTS { ");
		if (RDF.TYPE_PROPERTY.equals(input)
				|| input.getRdfsSubClassOf().contains(RDF.TYPE_PROPERTY)) {
			sb.append("?other a ?c . ?r rdfs:subPropertyOf ?other FILTER (?r != ?other)");
		} else {
			sb.append("?childProp rdfs:subPropertyOf* komma:child . ?other a ?type; ?childProp ?r FILTER (?r != ?other)");
		}
		sb.append(" }} ORDER BY ?r");
		return sb.toString();
	}
}
