package net.enilink.komma.owl.editor.internal.individuals;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.model.IObject;
import net.enilink.komma.util.ISparqlConstants;

public class InstanceTreePart extends IndividualsPart {
	static String QUERY_INSTANCES = ISparqlConstants.PREFIX
			+ "SELECT DISTINCT ?r WHERE {"
			+ "?r a ?c . OPTIONAL {?other komma:contains ?r} FILTER (!bound(?other)) }";

	private class ContentProvider extends AdapterFactoryContentProvider
			implements ILazyTreeContentProvider {
		Map<Object, Object[]> parentToChildren = new WeakHashMap<Object, Object[]>();

		public ContentProvider(IAdapterFactory adapterFactory) {
			super(adapterFactory);
		}

		@Override
		public Object[] getElements(Object object) {
			if (object instanceof Object[]) {
				return (Object[]) object;
			}
			return super.getElements(object);
		}

		@Override
		public void updateElement(Object parent, int index) {
			Object[] children;
			if (parent instanceof Object[]) {
				children = (Object[]) parent;
			} else {
				children = parentToChildren.get(parent);
			}

			if (children != null && index < children.length) {
				((TreeViewer) viewer).replace(parent, index, children[index]);
				((TreeViewer) viewer).setHasChildren(parent, true);
			}
		}

		@Override
		public void updateChildCount(Object element, int currentChildCount) {
			Object[] children;
			if (element instanceof Object[]) {
				children = (Object[]) element;
			} else {
				parentToChildren.remove(element);

				children = super.getChildren(element);
				parentToChildren.put(element, children);
			}

			if (children.length != currentChildCount) {
				((TreeViewer) viewer).setChildCount(element, children.length);
			}
		}
	}

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
		if (input == null) {
			viewer.setInput(null);
		} else {
			List<IObject> instances = input.getKommaManager()
					.createQuery(QUERY_INSTANCES).setIncludeInferred(true)
					.setParameter("c", input).evaluate(IObject.class).toList();

			viewer.setInput(instances.toArray());
		}
	}
}
