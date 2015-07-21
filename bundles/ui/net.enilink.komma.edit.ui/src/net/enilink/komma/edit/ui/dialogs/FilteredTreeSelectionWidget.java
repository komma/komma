package net.enilink.komma.edit.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class FilteredTreeSelectionWidget {

	private FilteredTree filteredTree;

	private IFilteredTreeDescriptor descriptor;

	public FilteredTreeSelectionWidget(IFilteredTreeDescriptor descriptor) {
		filteredTree = new FilteredTree(new PatternFilter());
		this.descriptor = descriptor;
	}

	public Composite createControl(Composite parent) {
		Composite content = new Composite(parent, SWT.NONE);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		content.setLayoutData(gd);

		GridLayout layout = new GridLayout(1, true);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		content.setLayout(layout);

		Control treeControl = filteredTree.createControl(content, SWT.BORDER
				| SWT.V_SCROLL | SWT.H_SCROLL);
		treeControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		filteredTree
				.setTreeContentProvider(descriptor.getTreeContentProvider());
		filteredTree.setTreeLabelProvider(descriptor.getLabelProvider());

		return content;
	}

	public FilteredTree getFilteredTree() {
		return filteredTree;
	}

	public void show() {
		filteredTree.getViewer().setInput(descriptor.getTreeInput());
		filteredTree.getViewer().refresh();
	}
}
