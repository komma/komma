package net.enilink.komma.owl.editor.rcp;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.IFileEditorInput;

import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.edit.ui.editor.IPropertySheetPageSupport;
import net.enilink.komma.edit.ui.editor.KommaEditorSupport;
import net.enilink.komma.edit.ui.editor.KommaFormEditor;
import net.enilink.komma.edit.ui.rcp.editor.TabbedPropertySheetPageSupport;
import net.enilink.komma.edit.ui.views.IViewerMenuSupport;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.model.base.ModelSetFactory;
import net.enilink.komma.owl.editor.IModelProvider;
import net.enilink.komma.owl.editor.OWLEditorPlugin;
import net.enilink.komma.owl.editor.internal.classes.ClassesPage;
import net.enilink.komma.owl.editor.internal.ontology.OntologyPage;
import net.enilink.komma.owl.editor.internal.properties.DatatypePropertiesPage;
import net.enilink.komma.owl.editor.internal.properties.ObjectPropertiesPage;
import net.enilink.komma.owl.editor.internal.properties.OtherPropertiesPage;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.workbench.IProjectModelSet;
import net.enilink.komma.workbench.ProjectModelSetSupport;

/**
 * This is the form editor acting as container for all involved editors
 * 
 * @author Ken Wenzel
 */
public class OWLEditor extends KommaFormEditor implements IViewerMenuSupport,
		IModelProvider {
	protected OntologyPage ontologyPage;
	protected ClassesPage classesPage;
	protected ObjectPropertiesPage objectPropertiesPage;
	protected OtherPropertiesPage otherPropertiesPage;
	protected DatatypePropertiesPage datatypePropertiesPage;

	public OWLEditor() {
		ontologyPage = new OntologyPage(this);
		classesPage = new ClassesPage(this);
		objectPropertiesPage = new ObjectPropertiesPage(this);
		otherPropertiesPage = new OtherPropertiesPage(this);
		datatypePropertiesPage = new DatatypePropertiesPage(this);
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
				KommaModule module = ModelCore.createModelSetModule(getClass()
						.getClassLoader());
				module.addConcept(IProjectModelSet.class);
				module.addBehaviour(ProjectModelSetSupport.class);

				IModelSet modelSet = new ModelSetFactory(module, URIImpl
						.createURI(MODELS.NAMESPACE +
						// "MemoryModelSet" //
								"OwlimModelSet" //
						), URIImpl.createURI(MODELS.NAMESPACE
						+ "ProjectModelSet") //
				).createModelSet();

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

	public IModel getModel() {
		return getEditorSupport().getModel();
	}

	@Override
	public void createContextMenuFor(StructuredViewer viewer) {
		getEditorSupport().createContextMenuFor(viewer);
	}
}
