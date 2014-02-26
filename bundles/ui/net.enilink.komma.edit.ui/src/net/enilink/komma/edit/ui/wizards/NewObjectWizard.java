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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import net.enilink.komma.model.IModel;
import net.enilink.komma.parser.manchester.ManchesterSyntaxParser;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.QName;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

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
				ManchesterSyntaxParser rdfParser = Parboiled
						.createParser(ManchesterSyntaxParser.class);

				@Override
				protected URI validate(String nameText) {
					URI name = null;

					String errorMsg = null;
					if (nameText.length() == 0) {
						errorMsg = "Name may not be empty.";
					} else {
						ParsingResult<Object> result = new ReportingParseRunner<Object>(
								rdfParser.IriRef()).run(nameText);

						if (result.hasErrors()) {
							errorMsg = "Invalid name.";
						} else {
							if (result.resultValue instanceof IriRef) {
								try {
									name = URIImpl
											.createURI(((IriRef) result.resultValue)
													.getIri());
									if (name.isRelative()) {
										name = null;
										throw new IllegalArgumentException(
												"Relative IRIs are not supported.");
									}
								} catch (IllegalArgumentException e) {
									errorMsg = "Invalid IRI.";
								}
							} else {
								String prefix = ((QName) result.resultValue)
										.getPrefix();
								String localPart = ((QName) result.resultValue)
										.getLocalPart();
								URI ns;
								if (prefix == null
										|| prefix.trim().length() == 0) {
									ns = model.getURI();
								} else {
									ns = model.getManager()
											.getNamespace(prefix);
								}
								if (ns != null) {
									name = ns.appendLocalPart(localPart);
								} else {
									errorMsg = "Unknown prefix";
								}
							}
							if (name != null
									&& model.getManager()
											.createQuery(
													"ASK { ?subj ?pred ?obj }",
													false)
											.setParameter("subj", name)
											.getBooleanResult()) {
								errorMsg = "An entity with the same name is already present in this model.";
							}
						}
					}

					setPageComplete(errorMsg == null);
					setErrorMessage(errorMsg);

					return name;
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
		if (objectNamePage == null) {
			return selectionPage.isPageComplete();
		}
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

	public URI getObjectName() {
		return objectNamePage.getObjectName();
	}

	public void setObjectTypes(Object[] types) {
		if (selectionPage != null) {
			selectionPage.setTypes(types);
		}
	}
}
