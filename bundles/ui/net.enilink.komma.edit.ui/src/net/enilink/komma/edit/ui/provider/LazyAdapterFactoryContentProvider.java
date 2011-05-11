package net.enilink.komma.edit.ui.provider;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;

import net.enilink.komma.common.adapter.IAdapterFactory;

public class LazyAdapterFactoryContentProvider extends
		AdapterFactoryContentProvider implements ILazyTreeContentProvider {
	protected Map<Object, Object[]> parentToChildren = new WeakHashMap<Object, Object[]>();

	public LazyAdapterFactoryContentProvider(IAdapterFactory adapterFactory) {
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
			updateChildCount(children[index], -1);
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
