package net.enilink.komma.owl.editor.internal.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import net.enilink.commons.ui.editor.EditorForm;
import net.enilink.commons.ui.editor.FormPart;
import net.enilink.komma.owl.editor.IModelProvider;
import net.enilink.komma.owl.editor.internal.KommaFormPage;

/**
 * Extends FormPage that is itself an extension of EditorPart
 * 
 * @author Ken Wenzel
 * 
 */
public class DatatypePropertiesPage extends KommaFormPage {
	protected DatatypePropertiesPart part;

	public DatatypePropertiesPage(FormEditor editor) {
		super(editor, "DatatypeProperties", "DatatypeProperties");
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		form.setText("DatatypeProperties");

		EditorForm editorForm = getEditorForm();
		managedForm.addPart(new FormPart(editorForm));

		Composite body = form.getBody();
		body.setLayout(new GridLayout(1, false));

		Composite composite = editorForm.getWidgetFactory().createComposite(
				body);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		part = new DatatypePropertiesPart();
		part.initialize(editorForm);
		part.createContents(composite);
		part.setInput(((IModelProvider) getEditor()).getModel());
		part.refresh();
	}
}
