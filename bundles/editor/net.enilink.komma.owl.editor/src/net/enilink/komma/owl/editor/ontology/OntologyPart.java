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
package net.enilink.komma.owl.editor.ontology;

import java.util.ArrayList;
import java.util.List;

import net.enilink.commons.iterator.Filter;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.base.IURIMapRule;
import net.enilink.komma.model.base.IURIMapRuleSet;
import net.enilink.komma.model.base.SimpleURIMapRule;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Extends FormPage that is itself an extension of EditorPart
 * 
 */
public class OntologyPart extends AbstractEditingDomainPart {
	protected Text uriText;
	protected ControlDecoration uriTextError;
	protected Button changeUri;
	protected ImportsPart importsPart;
	protected NamespacesPart namespacePart;

	protected IModel model;

	@Override
	public void createContents(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		getWidgetFactory().paintBordersFor(parent);

		createNameComposite(parent);

		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		importsPart = new ImportsPart();
		addPart(importsPart);
		importsPart.createContents(createSection(sashForm, "Imports"));

		namespacePart = new NamespacesPart();
		addPart(namespacePart);
		namespacePart.createContents(createSection(sashForm, "Namespaces"));
	}

	private void createNameComposite(Composite parent) {
		Composite composite = getWidgetFactory().createComposite(parent);
		composite.setLayout(new GridLayout(2, false));
		composite
				.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		uriText = getWidgetFactory().createText(composite, "", SWT.SINGLE);
		uriText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		uriTextError = new ControlDecoration(uriText, SWT.LEFT | SWT.TOP);
		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
		uriTextError.setImage(fieldDecoration.getImage());
		uriTextError.hide();
		uriText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				URI uri = null;
				String text = uriText.getText().trim();
				if (!text.isEmpty()) {
					try {
						uri = URIImpl.createURI(uriText.getText().trim());
						uriTextError.hide();
					} catch (IllegalArgumentException iae) {
						uriTextError.setDescriptionText("Invalid URI.");
						uriTextError.show();
					}
				}
				changeUri.setEnabled(model != null && uri != null
						&& !uri.equals(model.getURI()));
			}
		});

		changeUri = getWidgetFactory().createButton(composite, "Rename",
				SWT.NONE);
		changeUri.setEnabled(false);
		changeUri.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (model != null) {
					try {
						getEditingDomain().getCommandStack().execute(
								new SimpleCommand() {
									URI oldUri = model.getURI();
									URI newUri = URIImpl.createURI(uriText
											.getText().trim());

									IURIMapRuleSet ruleSet = model
											.getModelSet().getURIConverter()
											.getURIMapRules();
									List<IURIMapRule> oldRules = ruleSet
											.iterator()
											.filterKeep(
													new Filter<IURIMapRule>() {
														public boolean accept(
																IURIMapRule o) {
															return o instanceof SimpleURIMapRule
																	&& ((SimpleURIMapRule) o)
																			.getPattern()
																			.equals(oldUri
																					.toString());
														};
													}).toList();
									List<IURIMapRule> newRules = new ArrayList<>();
									{
										for (IURIMapRule rule : oldRules) {
											newRules.add(new SimpleURIMapRule(
													rule.getPriority(), newUri
															.toString(),
													((SimpleURIMapRule) rule)
															.getReplacement()));
										}
									}

									protected CommandResult doRedoWithResult(
											IProgressMonitor progressMonitor,
											IAdaptable info)
											throws ExecutionException {
										uriText.setText(newUri.toString());
										return doExecuteWithResult(
												progressMonitor, info);
									}

									protected CommandResult doUndoWithResult(
											IProgressMonitor progressMonitor,
											IAdaptable info)
											throws ExecutionException {
										uriText.setText(oldUri.toString());
										model.setURI(oldUri);
										// update mapping rules
										for (IURIMapRule rule : newRules) {
											ruleSet.removeRule(rule);
										}
										for (IURIMapRule rule : oldRules) {
											ruleSet.addRule(rule);
										}
										return CommandResult
												.newOKCommandResult();
									}

									@Override
									protected CommandResult doExecuteWithResult(
											IProgressMonitor progressMonitor,
											IAdaptable info)
											throws ExecutionException {
										model.setURI(newUri);
										// update mapping rules
										for (IURIMapRule rule : oldRules) {
											ruleSet.removeRule(rule);
										}
										for (IURIMapRule rule : newRules) {
											ruleSet.addRule(rule);
										}
										changeUri.setEnabled(false);
										return CommandResult
												.newOKCommandResult();
									}
								}, null, null);
					} catch (ExecutionException ee) {
						MessageDialog.openError(getShell(),
								"Rename of model failed.", ee.getMessage());
					}

				}
			}
		});
	}

	private Composite createSection(Composite parent, String name) {
		Section section = getWidgetFactory().createSection(parent,
				Section.TITLE_BAR | Section.EXPANDED);
		section.setText(name);
		Composite client = getWidgetFactory()
				.createComposite(section, SWT.NONE);
		section.setClient(client);
		return client;
	}

	@Override
	public void refresh() {
		uriText.setText(model != null ? model.getURI().toString() : "");
		uriTextError.hide();
		changeUri.setEnabled(false);
		refreshParts();
		super.refresh();
	}

	@Override
	public void setInput(Object input) {
		model = input instanceof IModel ? (IModel) input : null;
		importsPart.setInput(input);
		namespacePart.setInput(input);
	}
}
