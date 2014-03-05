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
import net.enilink.komma.core.URIs;
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

	protected Button uriUseDefault;
	protected Text uriField;

	protected WizardConfigureOntologyPage(String pageName,
			IStructuredSelection selection) {
		super(pageName, selection);
	}

	/**
	 * The "advanced" section is not used for the link options but rather for
	 * additional settings for the ontology creation (URI and format).
	 */
	@Override
	public void createAdvancedControls(Composite parent) {
		Composite formatGroup = new Composite(parent, SWT.NONE);
		GridLayout formatlayout = new GridLayout(2, false);
		formatlayout.marginWidth = 0;
		formatGroup.setLayout(formatlayout);
		formatGroup
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		formatGroup.setFont(parent.getFont());

		Label formatLabel = new Label(formatGroup, SWT.NONE);
		formatLabel.setText("File format:");

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

		// set the default extension here, as this is called from super()
		setFileExtension("rdf");

		// get the list of writeable content-types from the plugin registry
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
					&& "true".equalsIgnoreCase(String.valueOf(desc
							.getProperty(hasWriter)))) {
				supportedTypes.add(contentType);
				if (contentType.isAssociatedWith("dummy." + getFileExtension())) {
					// preselect entry for the default extension (see above)
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

		Composite uriGroup = new Composite(parent, SWT.NONE);
		GridLayout uriLayout = new GridLayout();
		uriLayout.marginWidth = 0;
		uriGroup.setLayout(uriLayout);
		uriGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		uriGroup.setFont(parent.getFont());

		Label uriLabel = new Label(uriGroup, SWT.NONE);
		uriLabel.setText("Enter the URI you wish to use:");

		uriField = new Text(uriGroup, SWT.BORDER);
		uriField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		// checkbox to use default URI (platform:/resource/$path)
		uriUseDefault = new Button(uriGroup, SWT.CHECK);
		uriUseDefault.setSelection(true);
		uriUseDefault.setText("Use a default URI");
		uriUseDefault.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// enable/disable(+reset) URI text input
				if (uriUseDefault.getSelection()) {
					uriField.setEnabled(false);
					updateURI();
				} else {
					uriField.setEnabled(true);
				}

			}
		});
		uriUseDefault.notifyListeners(SWT.Selection, new Event());
	}

	/** Return the selected URI. */
	public URI getURI() {
		return URIs.createURI(uriField.getText());
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
				getURI().toString(), //
				getFormat().getDefaultDescription().getProperty(mimeType)
						.toString(), //
				getFormat().getDefaultCharset());
		visitor.visitBegin();
		visitor.visitNamespace(new Namespace("", getURI().trimFragment()
				.appendLocalPart("")));
		visitor.visitStatement(new Statement(getURI().trimFragment(),
				RDF.PROPERTY_TYPE, OWL.TYPE_ONTOLOGY));
		visitor.visitEnd();
		return new ByteArrayInputStream(baos.toByteArray());
	}

	/** Also trigger modification of the URI, if appropriate. */
	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		updateURI();
	}

	/**
	 * Update the URI field upon changes to the filename or format when the
	 * "use default" option is selected.
	 */
	protected void updateURI() {
		if (uriUseDefault != null && uriUseDefault.getSelection()
				&& uriField != null && !getFileName().isEmpty()) {
			IPath targetPath = getContainerFullPath().append(getFileName());
			uriField.setText(URIs.createPlatformResourceURI(
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

		// format selection and non-empty URI are needed
		return (formatField.getSelectionIndex() != -1 && !uriField.getText()
				.isEmpty());
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
