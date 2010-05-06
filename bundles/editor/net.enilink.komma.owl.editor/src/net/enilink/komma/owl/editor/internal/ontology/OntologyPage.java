package net.enilink.komma.owl.editor.internal.ontology;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
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
public class OntologyPage extends KommaFormPage {
	protected OntologyPart ontologyPart;

	public OntologyPage(FormEditor editor) {
		super(editor, "ontology", "Ontology");
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		form.setText("Ontology");

		EditorForm editorForm = getEditorForm();
		managedForm.addPart(new FormPart(editorForm));

		Composite body = form.getBody();
		FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
		fillLayout.marginHeight = fillLayout.marginWidth = 5;
		body.setLayout(fillLayout);

		Composite composite = editorForm.getWidgetFactory().createComposite(
				body);

		ontologyPart = new OntologyPart();
		ontologyPart.initialize(editorForm);
		ontologyPart.createContents(composite);
		ontologyPart.setInput(((IModelProvider) getEditor()).getModel());
		ontologyPart.refresh();
	}
}
