package net.enilink.komma.edit.ui.provider;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

public abstract class UnwrappingLabelProvider implements ILabelProvider,
		ITableLabelProvider {
	private IBaseLabelProvider delegate;

	public UnwrappingLabelProvider(IBaseLabelProvider delegate) {
		this.delegate = delegate;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		delegate.addListener(listener);
	}

	@Override
	public void dispose() {
		delegate.dispose();
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (delegate instanceof ITableLabelProvider) {
			return ((ITableLabelProvider) delegate).getColumnImage(
					unwrap(element), columnIndex);
		}
		return getImage(element);
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (delegate instanceof ITableLabelProvider) {
			return ((ITableLabelProvider) delegate).getColumnText(
					unwrap(element), columnIndex);
		}
		return getText(element);
	}

	@Override
	public Image getImage(Object element) {
		if (delegate instanceof ILabelProvider) {
			return ((ILabelProvider) delegate).getImage(unwrap(element));
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		if (delegate instanceof ILabelProvider) {
			return ((ILabelProvider) delegate).getText(unwrap(element));
		}
		return null;
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return delegate.isLabelProperty(unwrap(element), property);
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		delegate.removeListener(listener);
	}

	protected abstract Object unwrap(Object element);
}
