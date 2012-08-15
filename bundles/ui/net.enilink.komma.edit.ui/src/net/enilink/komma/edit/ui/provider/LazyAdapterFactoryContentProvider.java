package net.enilink.komma.edit.ui.provider;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import net.enilink.komma.common.adapter.IAdapterFactory;

public class LazyAdapterFactoryContentProvider extends
		AdapterFactoryContentProvider implements ILazyTreeContentProvider {
	protected Map<Object, Object[]> parentToChildren = new WeakHashMap<Object, Object[]>();

	public LazyAdapterFactoryContentProvider(IAdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		parentToChildren.clear();
		super.inputChanged(viewer, oldInput, newInput);
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
		Object[] children = null;
		if (parent instanceof Object[]) {
			children = (Object[]) parent;
		} else if (parent != null) {
			children = parentToChildren.get(parent);
			if (children == null) {
				updateChildCount(parent, -1);
				children = parentToChildren.get(parent);
			}
		}

		if (children != null && index < children.length) {
			Object child = children[index];
			((TreeViewer) viewer).replace(parent, index, child);
			((TreeViewer) viewer).setHasChildren(child,
					super.hasChildren(child));
		}
	}

	@Override
	public void updateChildCount(Object element, int currentChildCount) {
		Object[] children;
		if (element instanceof Object[]) {
			children = (Object[]) element;
		} else {
			children = getChildren(element);
			parentToChildren.put(element, children);
		}
		if (children.length != currentChildCount) {
			((TreeViewer) viewer).setChildCount(element, children.length);
		}
	}
}
