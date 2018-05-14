package net.enilink.komma.edit.ui.dialogs;

import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * 
 * 
 *
 */
public class FilteredTreeAndListSelectionWidget {
	private FilteredTree filteredTree;

	private FilteredList filteredList;

	FilteredTreeAndListSelectionDialog filteredTreeAndListSelectionDialog;

	final IFilteredTreeAndListDescriptor descriptor;

	public FilteredTreeAndListSelectionWidget(
			IFilteredTreeAndListDescriptor descriptor,
			FilteredTreeAndListSelectionDialog filteredTreeAndListSelectionDialog,
			boolean multi) {

		this(descriptor, multi);
		this.filteredTreeAndListSelectionDialog = filteredTreeAndListSelectionDialog;
	}

	public FilteredTreeAndListSelectionWidget(
			final IFilteredTreeAndListDescriptor descriptor, boolean multi) {
		this.descriptor = descriptor;
		this.filteredTreeAndListSelectionDialog = null;

		filteredTree = new FilteredTree(new PatternFilter());
		filteredTree
				.setTreeContentProvider(descriptor.getTreeContentProvider());
		filteredTree.setTreeLabelProvider(descriptor.getLabelProvider());

		filteredList = new FilteredList(multi) {
			@Override
			protected ItemsFilter createFilter() {
				return new ItemsFilter() {
					@Override
					public boolean isConsistentItem(Object item) {
						return true;
					}

					@Override
					public boolean matchItem(Object item) {
						return matches(descriptor.getListItemName(item));
					}
				};
			}

			@Override
			protected void fillContentProvider(
					AbstractContentProvider contentProvider,
					ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
					throws CoreException {
				descriptor.fillListContentProvider(contentProvider,
						itemsFilter, progressMonitor);
			}

			@Override
			public String getElementName(Object item) {
				return descriptor.getListItemName(item);
			}

			@Override
			protected Comparator<Object> getItemsComparator() {
				return descriptor.getListItemsComparator();
			}

			@Override
			protected IStatus validateItem(Object item) {
				return descriptor.validateListItem(item);
			}

			@Override
			protected void updateStatus(IStatus status) {
				if (filteredTreeAndListSelectionDialog != null) {
					filteredTreeAndListSelectionDialog.updateStatus(status);
				}
			}
		};

		filteredList.setListLabelProvider(descriptor.getLabelProvider());
	}

	public FilteredList getFilteredList() {
		return filteredList;
	}

	public FilteredTree getFilteredTree() {
		return filteredTree;
	}

	public Composite createControl(Composite parent) {
		Composite content = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		content.setLayoutData(gd);

		GridLayout layout = new GridLayout(2, true);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		content.setLayout(layout);

		Control treeControl = filteredTree.createControl(content, SWT.BORDER
				| SWT.V_SCROLL | SWT.H_SCROLL);
		treeControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Control listControl = filteredList.createControl(content);
		listControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		return content;
	}

	public void show() {
		filteredTree.getViewer().setInput(descriptor.getTreeInput());
	}
}
