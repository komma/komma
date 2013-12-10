package net.enilink.komma.owl.editor.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import net.enilink.commons.ui.editor.EditorForm;
import net.enilink.commons.ui.editor.FormPart;
import net.enilink.komma.model.IModel;
import net.enilink.komma.owl.editor.internal.KommaFormPage;

/**
 * Extends FormPage that is itself an extension of EditorPart
 * 
 * @author Ken Wenzel
 * 
 */
public class OtherPropertiesPage extends KommaFormPage {
	protected OtherPropertiesPart part;

	public OtherPropertiesPage(FormEditor editor) {
		super(editor, "other Properties", "other Properties");
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		form.setText("other Properties");

		EditorForm editorForm = getEditorForm();
		managedForm.addPart(new FormPart(editorForm));

		Composite body = form.getBody();
		body.setLayout(new GridLayout(1, false));

		Composite composite = editorForm.getWidgetFactory().createComposite(
				body);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		part = new OtherPropertiesPart();
		part.initialize(editorForm);
		part.createContents(composite);
		part.setInput(getEditor().getAdapter(IModel.class));
		part.refresh();
	}
}
