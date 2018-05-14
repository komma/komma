/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.ui.editor;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * This class implements the 'master/details' UI pattern suitable for inclusion
 * in a form. The block consists of two parts: 'master' and 'details' in a sash
 * form that allows users to change the relative ratio on the page. The master
 * part needs to be created by the users of this class. The details part is
 * created by the block.
 * <p>
 * The master part is responsible for adding itself as a form part and firing
 * selection events. The details part catches the selection events and tries to
 * load a page registered to handle the selected object(s). The page shows the
 * details of the selected object(s) and allows users to edit them.
 * <p>
 * Details pages can be registered statically using 'registerPage' or
 * dynamically through the use of 'IElementDetailsPartProvider' in case where
 * different pages need to be shown for objects of the same type depending on
 * their state.
 * <p>
 * Subclasses are required to implement abstract methods of this class. Master
 * part must be created and at least one details page should be registered in
 * order to show details of the objects selected in the master part. Tool bar
 * actions can be optionally added to the tool bar manager.
 */
public abstract class EditorMasterDetailsBlock extends AbstractEditorPart
		implements ISelectionChangedListener {
	/**
	 * Details part created by the block. No attempt should be made to access
	 * this field inside <code>createMasterPart</code> because it has not been
	 * created yet and will be <code>null</code>.
	 */
	protected EditorPartBook detailsPartBook;

	/**
	 * The form that is the parent of both master and details part. The form
	 * allows users to change the ratio between the two parts.
	 */
	protected CSashForm sashForm;

	static final int DRAGGER_SIZE = 40;

	/**
	 * Creates the content of the master/details block inside the managed form.
	 * This method should be called as late as possible inside the parent part.
	 * 
	 * @param managedForm
	 *            the managed form to create the block in
	 */
	public void createContents(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
		parent.setLayout(layout);
		sashForm = new CSashForm(parent, SWT.NULL, getWidgetFactory());
		getWidgetFactory().adapt(sashForm, false, false);
		sashForm.setMenu(parent.getMenu());
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createMasterPart(sashForm);
		createDetailsPart(sashForm);
	}

	/**
	 * Implement this method to create a master part in the provided parent.
	 * Typical master parts are section parts that contain tree or table viewer.
	 * 
	 * @param parent
	 *            the parent composite
	 */
	protected abstract void createMasterPart(Composite parent);

	/**
	 * Implement this method to statically register parts for the expected
	 * object types. This mechanism can be used when there is 1-&gt;1 mapping
	 * between object classes and details pages.
	 * 
	 * @param detailsMainPart
	 *            the details part
	 */
	protected abstract void registerParts(EditorPartBook detailsMainPart);

	private void createDetailsPart(Composite parent) {
		detailsPartBook = new EditorPartBook(true) {
			@Override
			protected void commitPart(ISelection selection,
					IEditorPart detailsPart, boolean onSave) {
				super.commitPart(selection, detailsPart, onSave);
				detailsCommitted(selection);
			}

			@Override
			protected void refreshPart(IEditorPart detailsPart) {
				super.refreshPart(detailsPart);
				detailsRefreshed(detailsPart);
			}
		};
		initialize(detailsPartBook);
		detailsPartBook.createContents(parent);
		registerParts(detailsPartBook);
	}

	public void selectionChanged(SelectionChangedEvent event) {
		detailsPartBook.selectionChanged(event);
	}

	protected void detailsCommitted(ISelection selection) {

	}

	protected void detailsRefreshed(IEditorPart detailsPart) {

	}

	@Override
	public void commit(boolean onSave) {
		if (detailsPartBook.isDirty()) {
			detailsPartBook.commit(onSave);
		}
		super.commit(onSave);
	}

	@Override
	public boolean isDirty() {
		return detailsPartBook.isDirty() || super.isDirty();
	}

	@Override
	public boolean isStale() {
		return detailsPartBook.isStale() || super.isStale();
	}

	@Override
	public void refresh() {
		detailsPartBook.refresh();
		super.refresh();
	}

	@Override
	public void dispose() {
		if (detailsPartBook != null) {
			detailsPartBook.dispose();
		}
		super.dispose();
	}
}
