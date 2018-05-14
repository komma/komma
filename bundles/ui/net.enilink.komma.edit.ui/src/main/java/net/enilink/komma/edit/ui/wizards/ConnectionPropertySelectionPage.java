package net.enilink.komma.edit.ui.wizards;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.IReference;

public class ConnectionPropertySelectionPage extends WizardPage {
	protected Object input;
	protected ITreeContentProvider contentProvider;
	protected ILabelProvider labelProvider;
	protected TreeViewer treeViewer;
	protected IReference selectedType;
	private List<ViewerFilter> preAddedFilters = Collections.EMPTY_LIST;

	public IReference validate(Object[] selection) {
		return null;
	}

	public ConnectionPropertySelectionPage(String name, Object treeInput,
			ITreeContentProvider contentProvider, ILabelProvider labelProvider) {
		super(name);

		setTitle("New Connection");
		setDescription("Select connection type");

		this.input = (treeInput instanceof IExtendedIterator) ? ((IExtendedIterator<?>) treeInput)
				.toList() : treeInput;
		this.contentProvider = contentProvider;
		this.labelProvider = labelProvider;

		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);

		composite.setLayout(new GridLayout(1, false));
		Tree tree = new Tree(composite, SWT.BORDER);

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tree.setLayoutData(gridData);

		treeViewer = new TreeViewer(tree);
		treeViewer.setLabelProvider(labelProvider);
		treeViewer.setContentProvider(contentProvider);
		treeViewer.setInput(input);

		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				IStructuredSelection selection = (IStructuredSelection) e
						.getSelection();
				selectedType = validate(selection.toArray());
				setPageComplete(!selection.isEmpty());
			}
		});

		treeViewer.setSorter(new PropertyConnectionSorter());

		for (ViewerFilter filter : preAddedFilters) {
			treeViewer.addFilter(filter);
		}

		setControl(composite);
	}

	public void addFilter(ViewerFilter filter) {
		if (treeViewer != null) {
			treeViewer.addFilter(filter);
		} else {
			if (preAddedFilters == Collections.EMPTY_LIST) {
				preAddedFilters = new LinkedList<ViewerFilter>();
			}

			preAddedFilters.add(filter);
		}
	}

	public void setInput(Object input) {
		this.input = input;
		if (treeViewer != null) {
			treeViewer.setInput(input);
		}
	}

	protected IReference getType() {
		return selectedType;
	}
}
