package net.enilink.komma.owl.editor.rcp;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.enilink.commons.ui.editor.EditorForm;
import net.enilink.commons.ui.editor.IEditorPart;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIs;
import net.enilink.komma.edit.command.EditingDomainCommandStack;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.provider.ComposedAdapterFactory;
import net.enilink.komma.edit.ui.editor.IPropertySheetPageSupport;
import net.enilink.komma.edit.ui.editor.KommaMultiPageEditor;
import net.enilink.komma.edit.ui.editor.KommaMultiPageEditorSupport;
import net.enilink.komma.edit.ui.rcp.editor.TabbedPropertySheetPageSupport;
import net.enilink.komma.edit.ui.views.IViewerMenuSupport;
import net.enilink.komma.edit.ui.views.SelectionProviderAdapter;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelSetModule;
import net.enilink.komma.owl.editor.OWLEditorPlugin;
import net.enilink.komma.owl.editor.classes.ClassesPart;
import net.enilink.komma.owl.editor.ontology.OntologyPart;
import net.enilink.komma.owl.editor.properties.DatatypePropertiesPart;
import net.enilink.komma.owl.editor.properties.ObjectPropertiesPart;
import net.enilink.komma.owl.editor.properties.OtherPropertiesPart;
import net.enilink.komma.workbench.IProjectModelSet;
import net.enilink.komma.workbench.ProjectModelSetSupport;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.dialogs.SaveAsDialog;

import com.google.inject.Guice;

/**
 * A basic OWL editor.
 */
public class OWLEditor extends KommaMultiPageEditor implements
		IViewerMenuSupport {
	static protected class Shared {
		IModelSet modelSet;
		Set<Object> openEditors = Collections
				.newSetFromMap(new WeakHashMap<Object, Boolean>());
		ComposedAdapterFactory adapterFactory;
	}

	protected static QualifiedName PROPERTY_SHARED = new QualifiedName(
			OWLEditor.class.getName(), "shared");

	protected EditorForm form;
	protected SelectionProviderAdapter formSelectionProvider = new SelectionProviderAdapter();

	protected IProject project;
	protected Shared shared;

	protected void addPage(String label, IEditorPart editPart) {
		Composite control = form.getWidgetFactory().createComposite(
				form.getBody());
		control.setLayout(new FillLayout());
		control.setData("editPart", editPart);

		editPart.initialize(form);
		editPart.createContents(control);
		editPart.setInput(getEditorSupport().getModel());
		editPart.refresh();
		setPageText(addPage(control), label);
	}

	@Override
	protected void createPages() {
		final boolean[] internalChange = { false };
		form = new EditorForm(getContainer()) {
			@Override
			public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
				if (IEditingDomainProvider.class.equals(adapter)) {
					return OWLEditor.this;
				} else if (IViewerMenuSupport.class.equals(adapter)) {
					return OWLEditor.this;
				}
				return super.getAdapter(adapter);
			}

			@Override
			public void fireSelectionChanged(IEditorPart firingPart,
					ISelection selection) {
				try {
					internalChange[0] = true;
					formSelectionProvider.setSelection(selection);
				} finally {
					internalChange[0] = false;
				}
			}
		};
		formSelectionProvider
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						if (internalChange[0]) {
							return;
						}
						Object selected = ((IStructuredSelection) event
								.getSelection()).getFirstElement();
						// allow arbitrary selections to be adapted to IValue
						// objects
						if (selected != null && !(selected instanceof IValue)) {
							Object adapter = Platform.getAdapterManager()
									.getAdapter(selected, IValue.class);
							if (adapter != null) {
								selected = adapter;
							}
						}
						if (selected != null) {
							IEditorPart editPart = (IEditorPart) getControl(
									getActivePage()).getData("editPart");
							if (editPart != null
									&& editPart.setEditorInput(selected)) {
								form.refreshStale();
							}
						}
					}
				});

		try {
			// Creates the model from the editor input
			getEditorSupport().createModel();

			addPage("Ontology", new OntologyPart());
			addPage("Classes", new ClassesPart());
			addPage("ObjectProperties", new ObjectPropertiesPart());
			addPage("DatatypeProperties", new DatatypePropertiesPart());
			addPage("other Properties", new OtherPropertiesPart());

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
	protected KommaMultiPageEditorSupport<? extends KommaMultiPageEditor> createEditorSupport() {
		return new KommaMultiPageEditorSupport<KommaMultiPageEditor>(this) {
			{
				saveAllModels = false;
				disposeModelSet = false;
			}

			@Override
			public void handlePageChange(Object activeEditor) {
				super.handlePageChange(activeEditor);
				editorSelectionProvider
						.setSelectionProvider(formSelectionProvider);
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
				if (getEditorInput() instanceof IFileEditorInput) {
					project = ((IFileEditorInput) getEditorInput()).getFile()
							.getProject();
				} else {
					project = (IProject) getEditorInput().getAdapter(
							IProject.class);
				}
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
						.createModelSet(MODELS.NAMESPACE_URI
								.appendLocalPart("MemoryModelSet"),
								// uses automatically OWLIM if available
								MODELS.NAMESPACE_URI
										.appendLocalPart("OwlimModelSet"),
								URIs.createURI(MODELS.NAMESPACE
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