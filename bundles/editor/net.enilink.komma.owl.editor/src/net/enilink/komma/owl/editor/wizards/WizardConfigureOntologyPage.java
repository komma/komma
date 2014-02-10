/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Fedorov <Alexander.Fedorov@borland.com>
 *     		- Bug 172000 [Wizards] WizardNewFileCreationPage should support overwriting existing resources
 *     Fraunhofer IWU - extended basic wizard page for ontology creation
 *******************************************************************************/
package net.enilink.komma.owl.editor.wizards;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.enilink.komma.core.Namespace;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.core.visitor.IDataAndNamespacesVisitor;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelUtil;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

/**
 * Wizard page to configure settings for ontology files.
 */
public class WizardConfigureOntologyPage extends WizardNewFileCreationPage {

	protected Combo formatField;
	protected ComboViewer formatViewer;

	protected Button namespaceDefault;
	protected Text namespaceField;

	protected WizardConfigureOntologyPage(String pageName,
			IStructuredSelection selection) {
		super(pageName, selection);
	}

	/**
	 * The "advanced" section is not used for the link options but rather for
	 * additional settings for the ontology creation (namespace and format).
	 */
	@Override
	public void createAdvancedControls(Composite parent) {
		Composite formatGroup = new Composite(parent, SWT.NONE);
		GridLayout formatlayout = new GridLayout();
		formatlayout.marginWidth = 0;
		formatlayout.marginHeight = 0;
		formatGroup.setLayout(formatlayout);
		formatGroup
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		formatGroup.setFont(parent.getFont());

		Label formatLabel = new Label(formatGroup, SWT.NONE);
		formatLabel.setText("Select the file format: ");

		formatField = new Combo(formatGroup, SWT.BORDER);
		formatViewer = new ComboViewer(formatField);
		formatViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
			}

			@SuppressWarnings("unchecked")
			@Override
			public Object[] getElements(Object input) {
				return ((List<IContentType>) input)
						.toArray(new IContentType[0]);
			}
		});
		formatViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((IContentType) element).getName();
			}
		});

		// set this here, doing it in the constructor is too late
		setFileExtension("rdf");
		QualifiedName mimeType = new QualifiedName(ModelPlugin.PLUGIN_ID,
				"mimeType");
		QualifiedName hasWriter = new QualifiedName(ModelPlugin.PLUGIN_ID,
				"hasWriter");
		List<IContentType> supportedTypes = new ArrayList<IContentType>();
		IContentType defaultSelection = null;
		for (IContentType contentType : Platform.getContentTypeManager()
				.getAllContentTypes()) {
			IContentDescription desc = contentType.getDefaultDescription();
			// use those registered from the model plugin w/ mimeType property
			if (desc.getProperty(mimeType) != null
					&& Boolean.parseBoolean((String) desc.getProperty(hasWriter))) {
				supportedTypes.add(contentType);
				if (contentType.isAssociatedWith("dummy." + getFileExtension())) {
					// default-select entry for the given extension (see above)
					defaultSelection = contentType;
				}
			}
		}
		formatViewer.setInput(supportedTypes);
		if (defaultSelection != null) {
			formatViewer
					.setSelection(new StructuredSelection(defaultSelection));
		}

		// handle changes to the format (also changes the extension)
		formatViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						IContentType contentType = (IContentType) formatViewer
								.getElementAt(formatField.getSelectionIndex());
						setFileExtension(contentType
								.getFileSpecs(IContentType.FILE_EXTENSION_SPEC)[0]);
					}
				});

		Composite namespaceGroup = new Composite(parent, SWT.NONE);
		GridLayout namespaceLayout = new GridLayout();
		namespaceLayout.marginWidth = 0;
		namespaceLayout.marginHeight = 0;
		namespaceGroup.setLayout(namespaceLayout);
		namespaceGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		namespaceGroup.setFont(parent.getFont());

		Label namespaceLabel = new Label(namespaceGroup, SWT.NONE);
		namespaceLabel.setText("Enter the namespace for your new Ontology: ");

		namespaceField = new Text(namespaceGroup, SWT.BORDER);
		namespaceField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));

		// check box for default namespace (platform:/resource/$path)
		namespaceDefault = new Button(namespaceGroup, SWT.CHECK);
		namespaceDefault.setSelection(true);
		namespaceDefault.setText("Use a default namespace");
		namespaceDefault.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// enable/disable(+reset) namespace text input
				if (namespaceDefault.getSelection()) {
					namespaceField.setEnabled(false);
					updateNamespace();
				} else {
					namespaceField.setEnabled(true);
				}

			}
		});
		namespaceDefault.notifyListeners(SWT.Selection, new Event());
	}

	/** Return the selected namespace. */
	public URI getNamespace() {
		return URIImpl.createURI(namespaceField.getText());
	}

	/** Return the selected format (content-type). */
	public IContentType getFormat() {
		return (IContentType) formatViewer.getElementAt(formatField
				.getSelectionIndex());
	}

	/**
	 * Create the actual file contents using the settings in the wizard.
	 */
	@Override
	protected InputStream getInitialContents() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		QualifiedName mimeType = new QualifiedName(ModelPlugin.PLUGIN_ID,
				"mimeType");
		IDataAndNamespacesVisitor<Void> visitor = ModelUtil.writeData(baos,
				getNamespace().toString(), //
				getFormat().getDefaultDescription().getProperty(mimeType)
						.toString(), //
				getFormat().getDefaultCharset());
		visitor.visitBegin();
		visitor.visitNamespace(new Namespace("", getNamespace().trimFragment()
				.appendLocalPart("")));
		visitor.visitStatement(new Statement(getNamespace().trimFragment(),
				RDF.PROPERTY_TYPE, OWL.TYPE_ONTOLOGY));
		visitor.visitEnd();
		return new ByteArrayInputStream(baos.toByteArray());
	}

	/** Also trigger modification of the namespace, if appropriate. */
	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		updateNamespace();
	}

	/**
	 * Update the namespace field upon changes to the filename or format when
	 * the "use default" option is selected.
	 */
	protected void updateNamespace() {
		if (namespaceDefault != null && namespaceDefault.getSelection()
				&& namespaceField != null && !getFileName().isEmpty()) {
			IPath targetPath = getContainerFullPath().append(getFileName());
			namespaceField.setText(URIImpl.createPlatformResourceURI(
					targetPath.toString(), true).toString());
		}
	}

	/**
	 * Handle changes to the file extension here by also changing it in the
	 * concatenated filename and report both (new extension and complete name,
	 * in that order) towards the basic wizard components (they don't handle
	 * dynamic changes well).
	 */
	@Override
	public void setFileExtension(String extension) {
		String oldextension = getFileExtension();
		String filename = getFileName();
		// strip the old extension off; append the new one
		super.setFileExtension(extension);
		if (oldextension != null && filename.endsWith("." + oldextension)) {
			filename = filename.replace("." + oldextension, "." + extension);
			setFileName(filename);
		}
	}

	@Override
	protected boolean validatePage() {
		boolean isValid = super.validatePage();
		if (!isValid) {
			return false;
		}

		// format selection and namespace setting needed
		return (formatField.getSelectionIndex() != -1 && !namespaceField
				.getText().isEmpty());
	}

	/**
	 * This extended page does not use the original "advanced" section with link
	 * options. This therefore simply returns OK_STATUS.
	 */
	@Override
	protected IStatus validateLinkedResource() {
		return Status.OK_STATUS;
	}

	/**
	 * This extended page does not use the original "advanced" section with link
	 * options. Not overriding this would cause an NPE; simply do nothing here.
	 */
	@Override
	protected void createLinkTarget() {
		// do nothing, still needed to avoid an NPE
	}
}
