package net.enilink.komma.edit.ui.wizards;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import net.enilink.komma.core.URI;

public class ObjectNamespaceSelectionPage extends WizardPage {
	protected ILabelProvider labelProvider;
	protected IContentProvider contentProvider;
	protected Object uris;
	
	protected ListViewer listViewer;
	
	protected Object[] types;
	
	public ObjectNamespaceSelectionPage(String name,List<URI> nsUris,ILabelProvider labelProvider){
		this(name,nsUris,labelProvider,new ArrayContentProvider());
	}
	
	public ObjectNamespaceSelectionPage(String name,Object nsUris,ILabelProvider labelProvider,IContentProvider contentProvider){
		super(name);
		
		this.uris = nsUris;
		this.labelProvider = labelProvider;
		this.contentProvider = contentProvider;
		
		setTitle("New resource");
		setDescription("Select resource namespace");
		
		setPageComplete(false);
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		
		composite.setLayout(new GridLayout(1,false));
		org.eclipse.swt.widgets.List list = new org.eclipse.swt.widgets.List(composite,SWT.BORDER);
		
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		list.setLayoutData(gridData);
		
		listViewer = new ListViewer(list);
		listViewer.setLabelProvider(labelProvider);
		listViewer.setContentProvider(contentProvider);
		listViewer.setInput(uris);
		
		listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				IStructuredSelection selection = (IStructuredSelection) e
						.getSelection();
				types = selection.toArray();
				setPageComplete(!selection.isEmpty());
			}
		});
		
		selectTypes();

		setControl(composite);
	}
	
	protected Object[] getTypes() {
		return types;
	}

	protected void setTypes(Object[] types) {
		this.types = types;
		selectTypes();
	}

	private void selectTypes() {
		if (types != null && listViewer != null) {
			listViewer.setSelection(new StructuredSelection(types), true);
		}
	}

}
