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
import net.enilink.komma.edit.command.EditingDomainCommandStack;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.provider.ComposedAdapterFactory;
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
	static protected class Shared {
		IModelSet modelSet;
		Set<Object> openEditors = Collections
				.newSetFromMap(new WeakHashMap<Object, Boolean>());
		ComposedAdapterFactory adapterFactory;
	}

	protected static QualifiedName PROPERTY_SHARED = new QualifiedName(
			OWLEditor.class.getName(), "shared");

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
	protected Shared shared;

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

			protected AdapterFactoryEditingDomain getExistingEditingDomain(
					IModelSet modelSet) {
				AdapterFactoryEditingDomain editingDomain = super
						.getExistingEditingDomain(modelSet);
				if (shared.adapterFactory == null) {
					shared.adapterFactory = createDefaultAdapterFactory();
				}

				// set up an editor-local editing domain with own command stack
				EditingDomainCommandStack commandStack = new EditingDomainCommandStack();
				editingDomain = new AdapterFactoryEditingDomain(
						shared.adapterFactory, commandStack, modelSet) {
					protected void registerDomainProviderAdapter() {
						// do not register this editing domain as adapter
					}
				};
				commandStack.setEditingDomain(editingDomain);
				editingDomain
						.setModelToReadOnlyMap(new java.util.WeakHashMap<IModel, Boolean>());
				return editingDomain;
			}

			protected IModelSet createModelSet() {
				project = getEditorInput() instanceof IFileEditorInput ? ((IFileEditorInput) getEditorInput())
						.getFile().getProject() : null;
				IModelSet modelSet = null;
				try {
					shared = (Shared) project
							.getSessionProperty(PROPERTY_SHARED);
				} catch (CoreException e) {
					// ignore
				}
				if (shared == null) {
					shared = new Shared();
					try {
						project.setSessionProperty(PROPERTY_SHARED, shared);
					} catch (CoreException e) {
						// ignore
					}
				}
				shared.openEditors.add(OWLEditor.this);

				// use shared model set
				if (shared.modelSet != null) {
					return shared.modelSet;
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
				if (shared != null) {
					shared.modelSet = modelSet;
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
				if (shared != null) {
					shared.openEditors.remove(OWLEditor.this);
					if (modelSet != null && shared.openEditors.isEmpty()) {
						// dipose shared adapter factory
						if (shared.adapterFactory != null) {
							shared.adapterFactory.dispose();
							shared.adapterFactory = null;
						}
						modelSet.dispose();
						modelSet = null;
						// remove shared properties from project
						try {
							project.setSessionProperty(PROPERTY_SHARED, null);
						} catch (CoreException e) {
							// ignore
						}
					}
				}
				shared = null;
			}
		};
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class key) {
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
