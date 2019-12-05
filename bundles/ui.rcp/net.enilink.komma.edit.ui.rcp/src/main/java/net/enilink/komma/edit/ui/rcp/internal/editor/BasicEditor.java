package net.enilink.komma.edit.ui.rcp.internal.editor;

import net.enilink.komma.common.ui.ViewerPane;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIs;
import net.enilink.komma.edit.ui.celleditor.AdapterFactoryTreeEditor;
import net.enilink.komma.edit.ui.editor.IPropertySheetPageSupport;
import net.enilink.komma.edit.ui.editor.KommaMultiPageEditor;
import net.enilink.komma.edit.ui.editor.KommaMultiPageEditorSupport;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.rcp.KommaEditUIRCP;
import net.enilink.komma.edit.ui.rcp.editor.TabbedPropertySheetPageSupport;
import net.enilink.komma.edit.ui.views.IViewerMenuSupport;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelSetModule;
import net.enilink.komma.workbench.IProjectModelSet;
import net.enilink.komma.workbench.ProjectModelSetSupport;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPartSite;

import com.google.inject.Guice;

/**
 * This is an example of a basic model editor.
 * 
 */
public class BasicEditor extends KommaMultiPageEditor implements
		IViewerMenuSupport {
	/**
	 * This is the viewer that shadows the selection in the content outline. The
	 * parent relation must be correctly defined for this to work.
	 * 
	 */
	protected TreeViewer selectionViewer;

	/**
	 * This creates a model editor.
	 */
	public BasicEditor() {
		super();
	}

	@Override
	protected KommaMultiPageEditorSupport<? extends KommaMultiPageEditor> createEditorSupport() {
		return new KommaMultiPageEditorSupport<BasicEditor>(this) {
			@Override
			protected IResourceLocator getResourceLocator() {
				return KommaEditUIRCP.INSTANCE;
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
								URIs.createURI(MODELS.NAMESPACE +
								// "MemoryModelSet" //
										"OwlimModelSet" //
								),
								URIs.createURI(MODELS.NAMESPACE
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
		};
	}

	/**
	 * This is the method used by the framework to install your own controls. //
	 */
	@Override
	public void createPages() {
		// Creates the model from the editor input
		getEditorSupport().createModel();

		// Only creates the other pages if there is something that can be edited
		if (!getEditingDomain().getModelSet().getModels().isEmpty()) {
			// Create a page for the selection tree view.
			{
				ViewerPane viewerPane = new ViewerPane(getSite().getPage(),
						BasicEditor.this) {
					@Override
					public Viewer createViewer(Composite composite) {
						Tree tree = new Tree(composite, SWT.MULTI);
						TreeViewer newTreeViewer = new TreeViewer(tree);
						return newTreeViewer;
					}

					@Override
					public void requestActivation() {
						super.requestActivation();
						getEditorSupport().setSelectionProvider(getViewer());
					}
				};
				viewerPane.createControl(getContainer());

				selectionViewer = (TreeViewer) viewerPane.getViewer();
				selectionViewer
						.setContentProvider(new AdapterFactoryContentProvider(
								getEditorSupport().getAdapterFactory()));

				selectionViewer
						.setLabelProvider(new AdapterFactoryLabelProvider(
								getEditorSupport().getAdapterFactory()));
				selectionViewer.setInput(getEditorSupport().getEditingDomain()
						.getModelSet());
				selectionViewer.setSelection(new StructuredSelection(
						getEditorSupport().getEditingDomain().getModelSet()
								.getModels().iterator().next()), true);
				viewerPane.setTitle(getEditorSupport().getEditingDomain()
						.getModelSet());

				new AdapterFactoryTreeEditor(selectionViewer.getTree(),
						getEditorSupport().getAdapterFactory());

				getEditorSupport().createContextMenuFor(selectionViewer, getContainer(), getSite());
				int pageIndex = addPage(viewerPane.getControl());
				setPageText(pageIndex,
						getEditorSupport().getString("_UI_SelectionPage_label"));
			}

			getSite().getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					setActivePage(0);
				}
			});
		}

		// Ensures that this editor will only display the page's tab
		// area if there are more than one page
		getContainer().addControlListener(new ControlAdapter() {
			boolean guard = false;

			@Override
			public void controlResized(ControlEvent event) {
				if (!guard) {
					guard = true;
					getEditorSupport().hideTabs();
					guard = false;
				}
			}
		});

		getSite().getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				getEditorSupport().updateProblemIndication();
			}
		});
	}

	@Override
	public void createContextMenuFor(StructuredViewer viewer, Control menuParent, IWorkbenchPartSite partSite) {
		getEditorSupport().createContextMenuFor(viewer, menuParent, partSite);
	}

	@Override
	public Object getAdapter(Class key) {
		if (key == IModel.class) {
			return getEditorSupport().getModel();
		}
		return super.getAdapter(key);
	}
}
