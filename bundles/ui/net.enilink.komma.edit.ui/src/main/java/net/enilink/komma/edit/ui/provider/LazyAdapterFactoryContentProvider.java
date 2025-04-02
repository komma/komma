package net.enilink.komma.edit.ui.provider;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.edit.provider.IViewerNotification;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;

public class LazyAdapterFactoryContentProvider extends
		AdapterFactoryContentProvider implements ILazyContentProvider, ILazyTreeContentProvider {
	protected Object input;
	protected Map<Object, Object[]> parentToChildren = new WeakHashMap<Object, Object[]>();

	// used as a hack to fix performance issues with RAP
	// see https://github.com/eclipse-rap/org.eclipse.rap/issues/286
	static Field styleField;
	{
		try {
			styleField = Widget.class.getDeclaredField("style");
			styleField.setAccessible(true);
		} catch (NoSuchFieldException e) {
			styleField = null;
		}
	}

	public LazyAdapterFactoryContentProvider(IAdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.input = newInput;
		parentToChildren.clear();
		super.inputChanged(viewer, oldInput, newInput);
		if (viewer instanceof TableViewer) {
			// enforce update of element count
			getChildren(input);
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
		boolean updateCount = false;
		if (element instanceof Object[]) {
			children = (Object[]) element;
			updateCount = true;
		} else {
			children = parentToChildren.get(element);
			if (children == null) {
				children = internalGetChildren(element);
				parentToChildren.put(element, children);
				updateCount = true;
			}
		}
		if (updateCount) {
			if (viewer instanceof TreeViewer) {
				// only update child counts for tree viewers
				((TreeViewer) viewer).setChildCount(element, children.length);
			} else if (viewer instanceof TableViewer) {
				// set item count if input element is updated
				var table = ((TableViewer) viewer).getTable();

				int style = table.getStyle();
				boolean resetStyle = false;
				try {
					// this is a hack to fix performance issues with RAP
					// by prevent scrollbar updates for each destroyed item
					if (styleField != null && (style & SWT.NO_SCROLL) == 0) {
						try {
							styleField.set(table, table.getStyle() | SWT.NO_SCROLL);
							resetStyle = true;
						} catch (IllegalAccessException e) {
							// ignore
						}
					}
					table.setItemCount(children.length);
				} finally {
					if (resetStyle) {
						try {
							styleField.set(table, style);
						} catch (IllegalAccessException e) {
							// ignore
						}
					}
				}
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
