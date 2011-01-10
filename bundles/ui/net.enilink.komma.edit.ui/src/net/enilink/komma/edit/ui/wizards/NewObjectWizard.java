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
package net.enilink.komma.edit.ui.wizards;

import java.util.regex.Pattern;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;

import net.enilink.komma.model.IModel;

abstract public class NewObjectWizard extends Wizard {
	protected ObjectTypeSelectionPage selectionPage;

	protected ObjectNamePage objectNamePage;

	protected Composite containerComposite;

	protected Object treeInput;

	protected ILabelProvider treeLabelProvider;

	protected ITreeContentProvider treeContentProvider;

	protected IModel model;

	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);

		containerComposite = pageContainer;
	}

	public NewObjectWizard(IModel model) {
		this(model, null, null, null);
	}

	public NewObjectWizard(IModel model, Object treeInput,
			ILabelProvider treeLabelProvider,
			ITreeContentProvider treeContentProvider) {
		this.model = model;
		this.treeInput = treeInput;
		this.treeLabelProvider = treeLabelProvider;
		this.treeContentProvider = treeContentProvider;

		createPages();
	}

	protected boolean showTypePage() {
		return treeContentProvider != null;
	}

	protected boolean showNamePage() {
		return true;
	}

	protected void createPages() {
		if (showTypePage()) {
			selectionPage = new ObjectTypeSelectionPage("Select Resource Type",
					treeInput, treeLabelProvider, treeContentProvider);
		}

		if (showNamePage()) {
			objectNamePage = new ObjectNamePage() {
				@Override
				protected void validate() {
					String name = getObjectName();

					String errorMsg = null;
					if (name.length() == 0) {
						errorMsg = "Name may not be empty.";
					} else {
						Pattern namePattern = Pattern.compile("(\\w|[.])+");
						if (!namePattern.matcher(name).matches()) {
							errorMsg = "Invalid name.";
						} else if (model
								.getManager()
								.createQuery(
										"SELECT ?subj WHERE { ?subj ?pred ?obj . }")
								.setParameter(
										"subj",
										model.getURI().namespace()
												.appendFragment(name))
								.evaluate().hasNext()) {
							errorMsg = "An entity with the same name is already present in this model.";
						}
					}

					setPageComplete(errorMsg == null);
					setErrorMessage(errorMsg);
				}
			};
		}
	}

	public void addPages() {
		if (selectionPage != null) {
			addPage(selectionPage);
		}
		if (objectNamePage != null) {
			addPage(objectNamePage);
		}
	}

	public boolean canFinish() {
		return objectNamePage.isPageComplete()
				&& getContainer().getCurrentPage() == objectNamePage;
	}

	public IWizardPage getNextPage(IWizardPage page) {
		if (page == selectionPage) {
			return objectNamePage;
		}
		return null;
	}

	public IWizardPage getPreviousPage(IWizardPage page) {
		if (page != null) {
			if (page == objectNamePage) {
				return selectionPage;
			}
			return objectNamePage;
		}
		return null;
	}

	public Object[] getObjectTypes() {
		return selectionPage.getTypes();
	}

	public String getObjectName() {
		return objectNamePage.getObjectName();
	}

	public void setObjectTypes(Object[] types) {
		if (selectionPage != null) {
			selectionPage.setTypes(types);
		}
	}
}
