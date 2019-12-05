package net.enilink.komma.owl.editor.rcp;

import net.enilink.commons.ui.editor.EditorForm;
import net.enilink.commons.ui.editor.IEditorPart;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.IValue;
import net.enilink.komma.edit.command.EditingDomainCommandStack;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.editor.IPropertySheetPageSupport;
import net.enilink.komma.edit.ui.editor.KommaMultiPageEditor;
import net.enilink.komma.edit.ui.editor.KommaMultiPageEditorSupport;
import net.enilink.komma.edit.ui.rcp.editor.TabbedPropertySheetPageSupport;
import net.enilink.komma.edit.ui.rcp.project.ProjectModelSetManager;
import net.enilink.komma.edit.ui.views.IViewerMenuSupport;
import net.enilink.komma.edit.ui.views.SelectionProviderAdapter;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.owl.editor.OWLEditorPlugin;
import net.enilink.komma.owl.editor.classes.ClassesPart;
import net.enilink.komma.owl.editor.ontology.OntologyPart;
import net.enilink.komma.owl.editor.properties.DatatypePropertiesPart;
import net.enilink.komma.owl.editor.properties.ObjectPropertiesPart;
import net.enilink.komma.owl.editor.properties.OtherPropertiesPart;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.dialogs.SaveAsDialog;

/**
 * A basic OWL editor.
 */
public class OWLEditor extends KommaMultiPageEditor implements
		IViewerMenuSupport {
	protected EditorForm form;
	protected SelectionProviderAdapter formSelectionProvider = new SelectionProviderAdapter();

	protected IProject project;
	protected ProjectModelSetManager modelSetManager;

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
				// set up an editor-local editing domain with own command stack
				EditingDomainCommandStack commandStack = new EditingDomainCommandStack();
				editingDomain = new AdapterFactoryEditingDomain(
						modelSetManager.getAdapterFactory(), commandStack,
						modelSet) {
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
				modelSetManager = ProjectModelSetManager.getSharedInstance(project);
				modelSetManager.addClient(OWLEditor.this);
				return modelSetManager.getModelSet();
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
				if (modelSetManager != null) {
					modelSetManager.removeClient(OWLEditor.this);
					modelSet = null;
				}
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
	public void createContextMenuFor(StructuredViewer viewer, Control menuParent, IWorkbenchPartSite partSite) {
		getEditorSupport().createContextMenuFor(viewer, menuParent, partSite);
	}
}