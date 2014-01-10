package net.enilink.komma.owl.editor.rcp;

import net.enilink.commons.ui.editor.EditorForm;
import net.enilink.commons.ui.editor.FormPart;
import net.enilink.commons.ui.editor.IEditorPart;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.edit.ui.editor.IPropertySheetPageSupport;
import net.enilink.komma.edit.ui.editor.KommaEditorSupport;
import net.enilink.komma.edit.ui.editor.KommaFormEditor;
import net.enilink.komma.edit.ui.rcp.editor.TabbedPropertySheetPageSupport;
import net.enilink.komma.edit.ui.views.IViewerMenuSupport;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelSetModule;
import net.enilink.komma.owl.editor.OWLEditorPlugin;
import net.enilink.komma.owl.editor.classes.ClassesPart;
import net.enilink.komma.owl.editor.internal.KommaFormPage;
import net.enilink.komma.owl.editor.ontology.OntologyPart;
import net.enilink.komma.owl.editor.properties.DatatypePropertiesPart;
import net.enilink.komma.owl.editor.properties.ObjectPropertiesPart;
import net.enilink.komma.owl.editor.properties.OtherPropertiesPart;
import net.enilink.komma.workbench.IProjectModelSet;
import net.enilink.komma.workbench.ProjectModelSetSupport;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import com.google.inject.Guice;

/**
 * A basic OWL editor.
 */
public class OWLEditor extends KommaFormEditor implements IViewerMenuSupport {
	static class EditorPartPage extends KommaFormPage {
		IEditorPart editorPart;

		EditorPartPage(FormEditor editor, String id, String title,
				IEditorPart editorPart) {
			super(editor, id, title);
			this.editorPart = editorPart;
		}

		protected void createFormContent(IManagedForm managedForm) {
			ScrolledForm form = managedForm.getForm();
			form.setText(getTitle());

			EditorForm editorForm = getEditorForm();
			managedForm.addPart(new FormPart(editorForm));

			Composite body = form.getBody();
			body.setLayout(new FillLayout());

			editorPart.initialize(editorForm);
			editorPart.createContents(body);
			editorPart.setInput(getEditor().getAdapter(IModel.class));
			editorPart.refresh();
		}
	}

	protected IFormPage ontologyPage;
	protected IFormPage classesPage;
	protected IFormPage objectPropertiesPage;
	protected IFormPage otherPropertiesPage;
	protected IFormPage datatypePropertiesPage;

	public OWLEditor() {
		ontologyPage = new EditorPartPage(this, "ontology", "Ontology",
				new OntologyPart());
		classesPage = new EditorPartPage(this, "classes", "Classes",
				new ClassesPart());
		objectPropertiesPage = new EditorPartPage(this, "objectProperties",
				"ObjectProperties", new ObjectPropertiesPart());
		otherPropertiesPage = new EditorPartPage(this, "otherProperties",
				"other Properties", new OtherPropertiesPart());
		datatypePropertiesPage = new EditorPartPage(this, "datatypeProperties",
				"DatatypeProperties", new DatatypePropertiesPart());
	}

	protected void addPages() {
		try {
			// Creates the model from the editor input
			getEditorSupport().createModel();

			addPage(ontologyPage);
			addPage(classesPage);
			addPage(objectPropertiesPage);
			addPage(datatypePropertiesPage);
			addPage(otherPropertiesPage);

			getSite().getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					getEditorSupport().updateProblemIndication();
				}
			});
		} catch (Exception e) {
			OWLEditorPlugin.INSTANCE.log(e);
		}
	}

	@Override
	protected KommaEditorSupport<? extends KommaFormEditor> createEditorSupport() {
		return new KommaEditorSupport<KommaFormEditor>(this) {
			@Override
			protected IResourceLocator getResourceLocator() {
				return OWLEditorPlugin.INSTANCE;
			}

			protected IModelSet createModelSet() {
				KommaModule module = ModelPlugin
						.createModelSetModule(getClass().getClassLoader());
				module.addConcept(IProjectModelSet.class);
				module.addBehaviour(ProjectModelSetSupport.class);

				IModelSetFactory factory = Guice.createInjector(
						new ModelSetModule(module)).getInstance(
						IModelSetFactory.class);

				IModelSet modelSet = factory
						.createModelSet(
								URIImpl.createURI(MODELS.NAMESPACE +
								// "MemoryModelSet" //
										"OwlimModelSet" //
								),
								URIImpl.createURI(MODELS.NAMESPACE
										+ "ProjectModelSet"));

				if (modelSet instanceof IProjectModelSet
						&& getEditorInput() instanceof IFileEditorInput) {
					((IProjectModelSet) modelSet)
							.setProject(((IFileEditorInput) getEditorInput())
									.getFile().getProject());
				}

				return modelSet;
			}

			@Override
			protected IPropertySheetPageSupport createPropertySheetPageSupport() {
				return new TabbedPropertySheetPageSupport();
			}

			protected IPath getSaveAsPath() {
				SaveAsDialog saveAsDialog = new SaveAsDialog(editor.getSite()
						.getShell());
				saveAsDialog.open();
				return saveAsDialog.getResult();
			}
		};
	}

	@Override
	public Object getAdapter(Class key) {
		if (key == IModel.class) {
			return getEditorSupport().getModel();
		}
		return super.getAdapter(key);
	}

	@Override
	public void createContextMenuFor(StructuredViewer viewer) {
		getEditorSupport().createContextMenuFor(viewer);
	}
}
