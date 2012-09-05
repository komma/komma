package net.enilink.komma.edit.ui.provider;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.edit.provider.IViewerNotification;

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
	public void notifyChanged(
			Collection<? extends IViewerNotification> notifications) {
		parentToChildren.clear();
		super.notifyChanged(notifications);
	}

	@Override
	public void updateElement(Object parent, int index) {
		Object[] children = getChildren(parent);
		if (children != null && index < children.length) {
			Object child = children[index];
			((TreeViewer) viewer).replace(parent, index, child);
			Object[] childrenOfChild = parentToChildren.get(child);
			if (childrenOfChild == null) {
				boolean hasChildren = super.hasChildren(child);
				if (!hasChildren) {
					parentToChildren.put(child, new Object[0]);
				}
				((TreeViewer) viewer).setHasChildren(child, hasChildren);
			}
		}
	}

	@Override
	public Object[] getChildren(Object element) {
		Object[] children;
		if (element instanceof Object[]) {
			children = (Object[]) element;
		} else {
			children = parentToChildren.get(element);
			if (children == null) {
				children = super.getChildren(element);
				parentToChildren.put(element, children);
				((TreeViewer) viewer).setChildCount(element, children.length);
			}
		}
		return children;
	}

	@Override
	public void updateChildCount(Object element, int currentChildCount) {
		Object[] children = getChildren(element);
		if (children.length != currentChildCount) {
			((TreeViewer) viewer).setChildCount(element, children.length);
		}
	}
}
