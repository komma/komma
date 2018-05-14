package net.enilink.komma.edit.ui.provider;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.edit.provider.IViewerNotification;

public class LazyAdapterFactoryContentProvider extends
		AdapterFactoryContentProvider implements ILazyContentProvider, ILazyTreeContentProvider {
	protected Object input;
	protected Map<Object, Object[]> parentToChildren = new WeakHashMap<Object, Object[]>();

	public LazyAdapterFactoryContentProvider(IAdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.input = newInput;
		parentToChildren.clear();
		super.inputChanged(viewer, oldInput, newInput);
		if (viewer instanceof TableViewer) {
			if (newInput instanceof Object[]) {
				((TableViewer) viewer).setItemCount(((Object[])newInput).length);
			} else {
				((TableViewer) viewer).setItemCount(0);
			}
		}
	}

	@Override
	public void notifyChanged(
			Collection<? extends IViewerNotification> notifications) {
		// TODO recursively refresh affected elements instead of clearing the
		// whole table
		parentToChildren.clear();
		super.notifyChanged(notifications);
	}
	
	@Override
	public void updateElement(int index) {
		Object[] children = getChildren(input);
		if (children != null && index < children.length) {
			Object child = children[index];
			// ensure that uninitialized instances (obtained e.g. with
			// evaluateRestricted) are completely initialized
			child = ((IEntity) child).getEntityManager().find((IReference) child);
			((TableViewer) viewer).replace(child, index);
		}
	}

	@Override
	public void updateElement(Object parent, int index) {
		Object[] children = getChildren(parent);
		if (children != null && index < children.length) {
			Object child = children[index];
			// ensure that uninitialized instances (obtained e.g. with
			// evaluateRestricted) are completely initialized
			child = ((IEntity) child).getEntityManager().find((IReference) child);
			((TreeViewer) viewer).replace(parent, index, child);
			Object[] childrenOfChild = parentToChildren.get(child);
			if (childrenOfChild == null) {
				boolean hasChildren = super.hasChildren(child);
				if (!hasChildren) {
					parentToChildren.put(child, new Object[0]);
				}
				((TreeViewer) viewer).setHasChildren(child, hasChildren);
			} else {
				((TreeViewer) viewer).setChildCount(child, childrenOfChild.length);
			}
		}
	}

	/**
	 * This method may be overridden to allow filtering of the returned elements.
	 */
	protected Object[] internalGetChildren(Object element) {
		return super.getChildren(element);
	}

	@Override
	public Object[] getChildren(Object element) {
		Object[] children;
		if (element instanceof Object[]) {
			children = (Object[]) element;
		} else {
			children = parentToChildren.get(element);
			if (children == null) {
				children = internalGetChildren(element);
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
