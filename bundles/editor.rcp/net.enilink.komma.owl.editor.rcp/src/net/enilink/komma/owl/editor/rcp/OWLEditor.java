package net.enilink.komma.owl.editor.rcp;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
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
	protected static QualifiedName PROPERTY_MODELSET = new QualifiedName(
			OWLEditor.class.getName(), "modelSet");
	protected static QualifiedName PROPERTY_OPENEDITORS = new QualifiedName(
			OWLEditor.class.getName(), "openEditors");

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

	protected IProject project;
	protected Set<Object> openEditors;

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
			{
				saveAllModels = false;
				disposeModelSet = false;
			}

			@Override
			protected IResourceLocator getResourceLocator() {
				return OWLEditorPlugin.INSTANCE;
			}

			@SuppressWarnings("unchecked")
			protected IModelSet createModelSet() {
				project = getEditorInput() instanceof IFileEditorInput ? ((IFileEditorInput) getEditorInput())
						.getFile().getProject() : null;
				IModelSet modelSet = null;
				try {
					modelSet = (IModelSet) project
							.getSessionProperty(PROPERTY_MODELSET);
					openEditors = (Set<Object>) project
							.getSessionProperty(PROPERTY_OPENEDITORS);
					if (openEditors == null) {
						openEditors = Collections
								.newSetFromMap(new WeakHashMap<Object, Boolean>());
						project.setSessionProperty(PROPERTY_OPENEDITORS,
								openEditors);
					}
				} catch (CoreException e) {
					// ignore
				}
				if (openEditors != null) {
					openEditors.add(OWLEditor.this);
				}
				// use shared model set
				if (modelSet != null) {
					return modelSet;
				}

				KommaModule module = ModelPlugin
						.createModelSetModule(getClass().getClassLoader());
				module.addConcept(IProjectModelSet.class);
				module.addBehaviour(ProjectModelSetSupport.class);

				IModelSetFactory factory = Guice.createInjector(
						new ModelSetModule(module)).getInstance(
						IModelSetFactory.class);

				modelSet = factory
						.createModelSet(
								URIImpl.createURI(MODELS.NAMESPACE +
								// "MemoryModelSet" //
										"OwlimModelSet" //
								),
								URIImpl.createURI(MODELS.NAMESPACE
										+ "ProjectModelSet"));
				if (modelSet instanceof IProjectModelSet && project != null) {
					((IProjectModelSet) modelSet).setProject(project);
				}
				// share model set via session property
				try {
					project.setSessionProperty(PROPERTY_MODELSET, modelSet);
					project.setSessionProperty(PROPERTY_OPENEDITORS,
							openEditors);
				} catch (CoreException e) {
					// ignore
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

			@Override
			public void dispose() {
				super.dispose();
				if (openEditors != null) {
					openEditors.remove(OWLEditor.this);
				}
				if (modelSet != null
						&& (openEditors == null || openEditors.isEmpty())) {
					modelSet.dispose();
					modelSet = null;
					// remove shared properties from project
					try {
						project.setSessionProperty(PROPERTY_MODELSET, null);
						project.setSessionProperty(PROPERTY_OPENEDITORS, null);
					} catch (CoreException e) {
						// ignore
					}
				}
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
