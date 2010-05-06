package net.enilink.komma.owl.editor.internal.classes;

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
public class ClassesPage extends KommaFormPage {
	protected ClassesPart classesPart;

	public ClassesPage(FormEditor editor) {
		super(editor, "classes", "Classes");
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		form.setText("Classes");

		EditorForm editorForm = getEditorForm();
		managedForm.addPart(new FormPart(editorForm));

		Composite body = form.getBody();
		body.setLayout(new GridLayout(1, false));

		Composite composite = editorForm.getWidgetFactory().createComposite(
				body);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		classesPart = new ClassesPart();
		classesPart.initialize(editorForm);
		classesPart.createContents(composite);
		classesPart.setInput(((IModelProvider) getEditor()).getModel());
		classesPart.refresh();
	}
}
