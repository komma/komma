package net.enilink.komma.edit.ui.editor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.operations.DefaultOperationHistory;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetPage;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.BasicCommandStack;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.ICommandStack;
import net.enilink.komma.common.command.ICommandStackListener;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.NotificationFilter;
import net.enilink.komma.common.ui.EclipseUtil;
import net.enilink.komma.common.ui.MarkerHelper;
import net.enilink.komma.common.ui.editor.ProblemEditorPart;
import net.enilink.komma.common.util.BasicDiagnostic;
import net.enilink.komma.common.util.Diagnostic;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.edit.command.EditingDomainCommandStack;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.provider.AdapterFactoryItemDelegator;
import net.enilink.komma.edit.provider.ComposedAdapterFactory;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.action.EditingDomainActionBarContributor;
import net.enilink.komma.edit.ui.dnd.EditingDomainViewerDropAdapter;
import net.enilink.komma.edit.ui.dnd.LocalTransfer;
import net.enilink.komma.edit.ui.dnd.ViewerDragAdapter;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.provider.UnwrappingSelectionProvider;
import net.enilink.komma.edit.ui.util.EditUIMarkerHelper;
import net.enilink.komma.edit.ui.util.EditUIUtil;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.model.base.ModelSetFactory;
import net.enilink.komma.model.base.SimpleURIMapRule;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.komma.model.validation.IValidator;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

/**
 * This is a base class for a multi-page model editor.
 */
public abstract class KommaEditorSupport<E extends MultiPageEditorPart & ISupportedEditor>
		implements IEditingDomainProvider, IMenuListener, IAdaptable {
	class EditorSelectionProvider extends PostSelectionProviderAdapter {
		/**
		 * This listens to which ever viewer is active.
		 */
		protected ISelectionChangedListener selectionChangedListener;

		/**
		 * This keeps track of the selection of the editor as a whole.
		 * 
		 */
		protected ISelection editorSelection = StructuredSelection.EMPTY;

		/**
		 * This keeps track of the active selection provider.
		 * 
		 */
		private ISelectionProvider selectionProvider;

		/**
		 * This implements {@link org.eclipse.jface.viewers.ISelectionProvider}
		 * to return this editor's overall selection.
		 */
		@Override
		public ISelection getSelection() {
			return editorSelection;
		}

		/**
		 * This implements {@link org.eclipse.jface.viewers.ISelectionProvider}
		 * to set this editor's overall selection. Calling this result will
		 * notify the listeners.
		 */
		@Override
		public void setSelection(ISelection selection) {
			editorSelection = selection;
			fireSelectionChanged(new SelectionChangedEvent(this, selection));
			setStatusLineManager(selection);
		}

		/**
		 * Sets the current selection provider.
		 */
		public void setSelectionProvider(ISelectionProvider selectionProvider) {
			// If it is changing...
			if (this.selectionProvider != selectionProvider) {
				if (selectionChangedListener == null) {
					// Create the listener on demand.
					selectionChangedListener = new ISelectionChangedListener() {
						// This just notifies those things that are affected by
						// the section.
						public void selectionChanged(
								SelectionChangedEvent selectionChangedEvent) {
							setSelection(selectionChangedEvent.getSelection());
						}
					};
				}

				// Stop listening to the old one.
				if (this.selectionProvider != null) {
					this.selectionProvider
							.removeSelectionChangedListener(selectionChangedListener);
				}

				// Start listening to the new one.
				if (selectionProvider != null) {
					selectionProvider
							.addSelectionChangedListener(selectionChangedListener);
				}

				// Remember it.
				this.selectionProvider = selectionProvider;

				// Set the editors selection based on the current viewer's
				// selection.
				setSelection(selectionProvider == null ? StructuredSelection.EMPTY
						: selectionProvider.getSelection());
			}
		}

		/**
		 * This sets the selection into whichever viewer is active. <!--
		 * begin-user-doc --> <!-- end-user-doc -->
		 * 
		 */
		public void setSelectionToViewer(Collection<?> collection) {
			final Collection<?> theSelection = collection;
			// Make sure it's okay.
			//
			if (theSelection != null && !theSelection.isEmpty()) {
				// I don't know if this should be run this deferred
				// because we might have to give the editor a chance to process
				// the
				// viewer update events
				// and hence to update the views first.
				Runnable runnable = new Runnable() {
					public void run() {
						// Try to select the items in the current content viewer
						// of
						// the editor.
						//
						if (getSelectionProvider() != null) {
							((ISelectionProvider) getSelectionProvider())
									.setSelection(new StructuredSelection(
											theSelection.toArray()));
						}
					}
				};
				runnable.run();
			}
		}

		public ISelectionProvider getSelectionProvider() {
			return selectionProvider;
		}
	};

	/**
	 * Resources that have been changed since last activation. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	protected Collection<IModel> changedModels = new ArrayList<IModel>();

	/**
	 * This is the content outline page. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 */
	protected IContentOutlinePage contentOutlinePage;

	/**
	 * This is a kludge... <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	protected IStatusLineManager contentOutlineStatusLineManager;

	/**
	 * This is the content outline page's viewer. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 */
	protected TreeViewer contentOutlineViewer;

	/**
	 * This keeps track of the editing domain that is used to track all changes
	 * to the model. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	protected AdapterFactoryEditingDomain editingDomain;

	protected ComposedAdapterFactory ownedAdapterFactory;

	protected E editor;

	protected ICommandStackListener commandStackListener;

	protected EditorSelectionProvider editorSelectionProvider = new EditorSelectionProvider();

	/**
	 * The MarkerHelper is responsible for creating workspace resource markers
	 * presented in Eclipse's Problems View. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 */
	protected MarkerHelper markerHelper = new EditUIMarkerHelper();

	protected IModel model;

	protected IModelSet modelSet;

	/**
	 * Map to store the diagnostic associated with a resource. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	protected Map<IModel, Diagnostic> modelToDiagnosticMap = new LinkedHashMap<IModel, Diagnostic>();

	protected IOperationHistory operationHistory;

	/**
	 * This listens for when the outline becomes active <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 */
	protected IPartListener partListener = new IPartListener() {
		public void partActivated(IWorkbenchPart p) {
			if (p instanceof ContentOutline) {
				if (((ContentOutline) p).getCurrentPage() == contentOutlinePage) {
					getActionBarContributor().setActiveEditor(editor);

					editorSelectionProvider
							.setSelectionProvider(contentOutlineViewer);
				}
			} else if (p instanceof PropertySheet) {
				if (((PropertySheet) p).getCurrentPage() == propertySheetPageSupport) {
					getActionBarContributor().setActiveEditor(editor);

					// handleActivate();
				}
			} else if (p == editor) {
				handleActivate();
			} else {
				// support associated views
				IEditingDomainProvider provider = (IEditingDomainProvider) p
						.getAdapter(IEditingDomainProvider.class);
				if (provider != null && editingDomain != null
						&& editingDomain.equals(provider.getEditingDomain())) {
					getActionBarContributor().setActiveEditor(editor);

					editorSelectionProvider.setSelectionProvider(p.getSite()
							.getSelectionProvider());
				}
			}
		}

		public void partBroughtToTop(IWorkbenchPart p) {
			// Ignore.
		}

		public void partClosed(IWorkbenchPart p) {
			// Ignore.
		}

		public void partDeactivated(IWorkbenchPart p) {
			// Ignore.
		}

		public void partOpened(IWorkbenchPart p) {
			// Ignore.
		}
	};

	/**
	 * Adapter used to update the problem indication when resources are demanded
	 * loaded. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	protected INotificationListener<INotification> problemIndicationListener = new INotificationListener<INotification>() {
		Set<URI> properties = new HashSet<URI>(Arrays.asList(
				MODELS.PROPERTY_LOADED, MODELS.PROPERTY_ERROR,
				MODELS.PROPERTY_WARNING));

		@Override
		public NotificationFilter<INotification> getFilter() {
			return null;
		}

		@Override
		public void notifyChanged(
				Collection<? extends INotification> notifications) {
			for (INotification notification : notifications) {
				if (notification instanceof IStatementNotification) {
					if (properties
							.contains(((IStatementNotification) notification)
									.getPredicate().getURI())) {
						IModel model = (IModel) ((IStatementNotification) notification)
								.getModelSet()
								.getMetaDataManager()
								.find(((IStatementNotification) notification)
										.getSubject());
						Diagnostic diagnostic = analyzeModelProblems(model,
								null);
						if (diagnostic.getSeverity() != Diagnostic.OK) {
							modelToDiagnosticMap.put(model, diagnostic);
						} else {
							modelToDiagnosticMap.remove(model);
						}

						if (updateProblemIndication) {
							editor.getSite().getShell().getDisplay()
									.asyncExec(new Runnable() {
										public void run() {
											updateProblemIndication();
										}
									});
						}
						break;
					}
				}
			}
		}
	};

	/**
	 * This is the property sheet page support.
	 * 
	 */
	protected IPropertySheetPageSupport propertySheetPageSupport;

	/**
	 * Resources that have been removed since last activation. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	protected Collection<IModel> removedModels = new ArrayList<IModel>();

	/**
	 * This listens for workspace changes. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 */
	protected IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta delta = event.getDelta();
			try {
				class ResourceDeltaVisitor implements IResourceDeltaVisitor {
					protected Collection<IModel> changedModels = new ArrayList<IModel>();
					protected IModelSet modelSet = editingDomain.getModelSet();
					protected Collection<IModel> removedModels = new ArrayList<IModel>();

					public Collection<IModel> getChangedModels() {
						return changedModels;
					}

					public Collection<IModel> getRemovedModels() {
						return removedModels;
					}

					public boolean visit(IResourceDelta delta) {
						if (delta.getResource().getType() == IResource.FILE) {
							if (delta.getKind() == IResourceDelta.REMOVED
									|| delta.getKind() == IResourceDelta.CHANGED
									&& delta.getFlags() != IResourceDelta.MARKERS) {
								IModel model = modelSet.getModel(URIImpl
										.createURI(delta.getFullPath()
												.toString()), false);
								if (model != null) {
									if (delta.getKind() == IResourceDelta.REMOVED) {
										removedModels.add(model);
									} else if (!savedModels.remove(model)) {
										changedModels.add(model);
									}
								}
							}
						}

						return true;
					}
				}

				ResourceDeltaVisitor visitor = new ResourceDeltaVisitor();
				delta.accept(visitor);

				if (!visitor.getRemovedModels().isEmpty()) {
					removedModels.addAll(visitor.getRemovedModels());
					if (!isDirty()) {
						editor.getSite().getShell().getDisplay()
								.asyncExec(new Runnable() {
									public void run() {
										editor.getSite().getPage()
												.closeEditor(editor, false);
									}
								});
					}
				}

				if (!visitor.getChangedModels().isEmpty()) {
					changedModels.addAll(visitor.getChangedModels());
					if (editor.getSite().getPage().getActiveEditor() == KommaEditorSupport.this) {
						editor.getSite().getShell().getDisplay()
								.asyncExec(new Runnable() {
									public void run() {
										handleActivate();
									}
								});
					}
				}
			} catch (CoreException exception) {
				KommaEditUIPlugin.INSTANCE.log(exception);
			}
		}
	};

	/**
	 * Resources that have been saved. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 */
	protected Collection<IModel> savedModels = new ArrayList<IModel>();

	/**
	 * Controls whether the problem indication should be updated. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	protected boolean updateProblemIndication = true;

	public KommaEditorSupport(E editor) {
		this.editor = editor;
	}

	/**
	 * Returns a diagnostic describing the errors and warnings listed in the
	 * resource and the specified exception (if any). <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 */
	public Diagnostic analyzeModelProblems(IModel model, Exception exception) {
		if (!model.getErrors().isEmpty() || !model.getWarnings().isEmpty()) {
			BasicDiagnostic basicDiagnostic = new BasicDiagnostic(
					Diagnostic.ERROR, KommaEditUIPlugin.PLUGIN_ID, 0,
					getString("_UI_CreateModelError_message", model.getURI()),
					new Object[] { exception == null ? (Object) model
							: exception });
			basicDiagnostic.merge(ModelUtil.computeDiagnostic(model, true));
			return basicDiagnostic;
		} else if (exception != null) {
			return new BasicDiagnostic(Diagnostic.ERROR,
					KommaEditUIPlugin.PLUGIN_ID, 0, getString(
							"_UI_CreateModelError_message", model.getURI()),
					new Object[] { exception });
		} else {
			return Diagnostic.OK_INSTANCE;
		}
	}

	/**
	 * This creates a context menu for the viewer and adds a listener as well
	 * registering the menu for extension. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 */
	public void createContextMenuFor(StructuredViewer viewer) {
		Menu menu = viewer.getControl().getMenu();
		if (menu != null && !menu.isDisposed()) {
			menu.dispose();
			viewer.getControl().setMenu(null);
		}

		MenuManager contextMenu = new MenuManager("#PopUp");
		contextMenu.add(new Separator("additions"));
		contextMenu.setRemoveAllWhenShown(true);
		contextMenu.addMenuListener(this);
		menu = contextMenu.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);

		ISelectionProvider menuSelectionProvider = new UnwrappingSelectionProvider(
				viewer);
		editor.getSite()
				.registerContextMenu(contextMenu, menuSelectionProvider);
		editor.getSite().registerContextMenu(
				"net.enilink.komma.edit.ui.menu", contextMenu,
				menuSelectionProvider);

		int dndOperations = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;
		Transfer[] transfers = new Transfer[] { LocalTransfer.getInstance() };

		DragSource source = (DragSource) viewer.getControl().getData(
				DND.DRAG_SOURCE_KEY);
		if (source != null && !source.isDisposed()) {
			source.dispose();
		}
		viewer.addDragSupport(dndOperations, transfers, new ViewerDragAdapter(
				viewer));

		DropTarget target = (DropTarget) viewer.getControl().getData(
				DND.DROP_TARGET_KEY);
		if (target != null && !target.isDisposed()) {
			target.dispose();
		}
		viewer.addDropSupport(dndOperations, transfers,
				new EditingDomainViewerDropAdapter(editingDomain, viewer));
	}

	/**
	 * This is the method called to load a resource into the editing domain's
	 * model set based on the editor's input.
	 */
	public void createModel() {
		URI resourceURI = EditUIUtil.getURI(editor.getEditorInput());

		if (resourceURI.scheme() == null
				|| !resourceURI.scheme().toLowerCase().startsWith("http")) {
			InputStream in = null;
			try {
				in = editingDomain.getModelSet().getURIConverter()
						.createInputStream(resourceURI);

				String ontology = ModelUtil.findOntology(in,
						resourceURI.toString());
				if (ontology != null) {
					editingDomain
							.getModelSet()
							.getURIConverter()
							.getURIMapRules()
							.addRule(
									new SimpleURIMapRule(ontology, resourceURI
											.toString()));

					resourceURI = URIImpl.createURI(ontology);
				}
			} catch (Exception e) {
				KommaEditUIPlugin.INSTANCE.log(e);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						KommaEditUIPlugin.INSTANCE.log(e);
					}
				}
			}

		}

		Exception exception = null;
		try {
			// Load the model through the editing domain.
			model = editingDomain.getModelSet().getModel(resourceURI, true);
		} catch (Exception e) {
			exception = e;
			model = editingDomain.getModelSet().getModel(resourceURI, false);
		}

		Diagnostic diagnostic = analyzeModelProblems(model, exception);
		if (diagnostic.getSeverity() != Diagnostic.OK) {
			modelToDiagnosticMap.put(model,
					analyzeModelProblems(model, exception));
		}

		model.getManager();

		editingDomain.getModelSet().addMetaDataListener(
				problemIndicationListener);
	}

	protected IModelSet createModelSet() {
		KommaModule module = ModelCore.createModelSetModule(getClass()
				.getClassLoader());

		IModelSet modelSet = new ModelSetFactory(module,
				URIImpl.createURI(MODELS.NAMESPACE +
				// "MemoryModelSet" //
						"OwlimModelSet" //
				)).createModelSet();
		return modelSet;
	}

	protected IPropertySheetPageSupport createPropertySheetPageSupport() {
		// return default PropertySheetPage
		return new IPropertySheetPageSupport() {
			IPropertySheetPage propertySheetPage;

			@Override
			public void dispose() {
				if (propertySheetPage != null) {
					propertySheetPage.dispose();
					propertySheetPage = null;
				}
			}

			@Override
			public IPropertySheetPage getPage() {
				if (propertySheetPage == null
						|| propertySheetPage.getControl().isDisposed()) {
					propertySheetPage = new PropertySheetPage();
				}
				return propertySheetPage;
			}

			@Override
			public void refresh() {
			}
		};
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public void dispose() {
		updateProblemIndication = false;

		ResourcesPlugin.getWorkspace().removeResourceChangeListener(
				resourceChangeListener);

		editor.getSite().getPage().removePartListener(partListener);

		editingDomain.getCommandStack().removeCommandStackListener(
				commandStackListener);

		editingDomain.getModelSet().removeMetaDataListener(
				problemIndicationListener);

		if (ownedAdapterFactory != null) {
			ownedAdapterFactory.dispose();
			ownedAdapterFactory = null;
		}

		if (propertySheetPageSupport != null) {
			propertySheetPageSupport.dispose();
		}

		if (contentOutlinePage != null) {
			contentOutlinePage.dispose();
		}
	}

	/**
	 * This is for implementing {@link IEditorPart} and simply saves the model
	 * file. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public void doSave(IProgressMonitor progressMonitor) {
		// Save only resources that have actually changed.
		final Map<Object, Object> saveOptions = new HashMap<Object, Object>();
		saveOptions.put(IModel.OPTION_SAVE_ONLY_IF_CHANGED,
				IModel.OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER);

		IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
			// This is the method that gets invoked when the operation runs.
			@Override
			public void run(IProgressMonitor monitor) {
				// Save the resources to the file system.
				for (IModel model : editingDomain.getModelSet().getModels()) {
					if (model.isModified() && !editingDomain.isReadOnly(model)) {
						try {
							model.save(saveOptions);
							savedModels.add(model);
						} catch (Exception exception) {
							modelToDiagnosticMap.put(model,
									analyzeModelProblems(model, exception));
						}
					}
				}
			}
		};

		// Do the work within an operation because this is a long running
		// activity that modifies the workbench.
		saveRunnable = EclipseUtil.createWorkspaceModifyOperation(saveRunnable);

		updateProblemIndication = false;
		try {
			// This runs the options, and shows progress.
			new ProgressMonitorDialog(editor.getSite().getShell()).run(true,
					false, saveRunnable);

			// Refresh the necessary state.
			((BasicCommandStack) editingDomain.getCommandStack()).saveIsDone();
			editor.firePropertyChange(IEditorPart.PROP_DIRTY);
		} catch (Exception exception) {
			// Something went wrong that shouldn't.
			KommaEditUIPlugin.INSTANCE.log(exception);
		}
		updateProblemIndication = true;
		updateProblemIndication();
	}

	/**
	 * This also changes the editor's input.
	 */
	public void doSaveAs() {
		IPath path = getSaveAsPath();
		if (path != null) {
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			if (file != null) {
				doSaveAs(URIImpl.createPlatformResourceURI(file.getFullPath()
						.toString(), true), EclipseUtil.createEditorInput(file));
			}
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	protected void doSaveAs(URI uri, IEditorInput editorInput) {
		editingDomain.getModelSet().getModels().iterator().next().setURI(uri);
		editor.setInputWithNotify(editorInput);
		editor.setPartName(editorInput.getName());
		IProgressMonitor progressMonitor = getActionBars()
				.getStatusLineManager() != null ? getActionBars()
				.getStatusLineManager().getProgressMonitor()
				: new NullProgressMonitor();
		doSave(progressMonitor);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public EditingDomainActionBarContributor getActionBarContributor() {
		return (EditingDomainActionBarContributor) editor.getEditorSite()
				.getActionBarContributor();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public IActionBars getActionBars() {
		return getActionBarContributor().getActionBars();
	}

	/**
	 * This is how the framework determines which interfaces we implement. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class key) {
		if (key.equals(IContentOutlinePage.class)) {
			return showOutlineView() ? getContentOutlinePage() : null;
		} else if (key.equals(IPropertySheetPage.class)) {
			return propertySheetPageSupport != null ? propertySheetPageSupport
					.getPage() : null;
		} else if (key.equals(IEditingDomainProvider.class)) {
			return this;
		} else if (key.equals(IOperationHistory.class)) {
			if (operationHistory == null) {
				operationHistory = new DefaultOperationHistory();
			}
			return operationHistory;
		}
		// else if (key.equals(IGotoMarker.class)) {
		// return this;
		// }
		return null;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public IAdapterFactory getAdapterFactory() {
		return ((AdapterFactoryEditingDomain) editingDomain)
				.getAdapterFactory();
	}

	/**
	 * This accesses a cached version of the content outliner. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public IContentOutlinePage getContentOutlinePage() {
		if (contentOutlinePage == null) {
			// The content outline is just a tree.
			class BasicContentOutlinePage extends ContentOutlinePage {
				@Override
				public void createControl(Composite parent) {
					super.createControl(parent);
					contentOutlineViewer = getTreeViewer();
					contentOutlineViewer.addSelectionChangedListener(this);

					// Set up the tree viewer.
					//
					contentOutlineViewer
							.setContentProvider(new AdapterFactoryContentProvider(
									getAdapterFactory()));
					contentOutlineViewer
							.setLabelProvider(new AdapterFactoryLabelProvider(
									getAdapterFactory()));
					contentOutlineViewer.setInput(editingDomain.getModelSet());

					// Make sure our popups work.
					createContextMenuFor(contentOutlineViewer);

					if (!editingDomain.getModelSet().getModels().isEmpty()) {
						// Select the root object in the view.
						contentOutlineViewer.setSelection(
								new StructuredSelection(editingDomain
										.getModelSet().getModels().iterator()
										.next()), true);
					}
				}

				@Override
				public void makeContributions(IMenuManager menuManager,
						IToolBarManager toolBarManager,
						IStatusLineManager statusLineManager) {
					super.makeContributions(menuManager, toolBarManager,
							statusLineManager);
					contentOutlineStatusLineManager = statusLineManager;
				}

				@Override
				public void setActionBars(IActionBars actionBars) {
					super.setActionBars(actionBars);
					getActionBarContributor().shareGlobalActions(this,
							actionBars);
				}
			}

			contentOutlinePage = new BasicContentOutlinePage();

			// Listen to selection so that we can handle it in a special way.
			contentOutlinePage
					.addSelectionChangedListener(new ISelectionChangedListener() {
						// This ensures that we handle selections correctly.
						public void selectionChanged(SelectionChangedEvent event) {
							handleContentOutlineSelection(event.getSelection());
						}
					});
		}

		return contentOutlinePage;
	}

	/**
	 * This returns the editing domain as required by the
	 * {@link IEditingDomainProvider} interface. This is important for
	 * implementing the static methods of {@link AdapterFactoryEditingDomain}
	 * and for supporting {@link org.eclipse.emf.edit.ui.action.CommandAction}.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public IEditingDomain getEditingDomain() {
		return editingDomain;
	}

	public IModel getModel() {
		return model;
	}

	protected abstract IResourceLocator getResourceLocator();

	protected IPath getSaveAsPath() {
		// SaveAsDialog saveAsDialog = new SaveAsDialog(editor.getSite()
		// .getShell());
		// saveAsDialog.open();
		// IPath path = saveAsDialog.getResult();
		return null;
	}

	/**
	 * This looks up a string in the plugin's plugin.properties file. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public String getString(String key) {
		return getResourceLocator().getString(key);
	}

	/**
	 * This looks up a string in plugin.properties, making a substitution. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public String getString(String key, Object s1) {
		return getResourceLocator().getString(key, new Object[] { s1 });
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public void gotoMarker(IMarker marker) {
		try {
			if (marker.getType().equals(IValidator.MARKER)) {
				String uriAttribute = marker.getAttribute(
						IValidator.URI_ATTRIBUTE, null);
				if (uriAttribute != null) {
					URI uri = URIImpl.createURI(uriAttribute);
					IObject object = editingDomain.getModelSet().getObject(uri,
							true);
					if (object != null) {
						editorSelectionProvider
								.setSelectionToViewer(Collections
										.singleton(editingDomain
												.getWrapper(object)));
					}
				}
			}
		} catch (CoreException exception) {
			KommaEditUIPlugin.INSTANCE.log(exception);
		}
	}

	/**
	 * Handles activation of the editor or it's associated views. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	protected void handleActivate() {
		handlePageChange();

		// Recompute the read only state.
		if (editingDomain.getModelToReadOnlyMap() != null) {
			editingDomain.getModelToReadOnlyMap().clear();

			// Refresh any actions that may become enabled or disabled.
			editorSelectionProvider.setSelection(editorSelectionProvider
					.getSelection());
		}

		if (!removedModels.isEmpty()) {
			if (handleDirtyConflict()) {
				editor.getSite().getPage().closeEditor(editor, false);
			} else {
				removedModels.clear();
				changedModels.clear();
				savedModels.clear();
			}
		} else if (!changedModels.isEmpty()) {
			changedModels.removeAll(savedModels);
			handleChangedModels();
			changedModels.clear();
			savedModels.clear();
		}
	}

	/**
	 * Handles what to do with changed models on activation. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 */
	protected void handleChangedModels() {
		if (!changedModels.isEmpty() && (!isDirty() || handleDirtyConflict())) {
			if (isDirty()) {
				changedModels.addAll(editingDomain.getModelSet().getModels());
			}
			editingDomain.getCommandStack().flush();

			updateProblemIndication = false;
			for (IModel model : changedModels) {
				if (model.isLoaded()) {
					model.unload();
					try {
						model.load(Collections.EMPTY_MAP);
					} catch (IOException exception) {
						if (!modelToDiagnosticMap.containsKey(model)) {
							modelToDiagnosticMap.put(model,
									analyzeModelProblems(model, exception));
						}
					}
				}
			}

			if (AdapterFactoryEditingDomain.isStale(editorSelectionProvider
					.getSelection())) {
				editorSelectionProvider.setSelection(StructuredSelection.EMPTY);
			}

			updateProblemIndication = true;
			updateProblemIndication();
		}
	}

	/**
	 * This deals with how we want selection in the outliner to affect the other
	 * views. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public void handleContentOutlineSelection(ISelection selection) {

	}

	/**
	 * Shows a dialog that asks if conflicting changes should be discarded. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	protected boolean handleDirtyConflict() {
		return MessageDialog.openQuestion(editor.getSite().getShell(),
				getString("_UI_FileConflict_label"),
				getString("_WARN_FileConflict"));
	}

	/**
	 * If there is just one page in the multi-page editor part, this hides the
	 * single tab at the bottom. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public void hideTabs() {
		if (editor.getPageCount() <= 1) {
			editor.setPageText(0, "");
			if (editor.getContainer() instanceof CTabFolder) {
				((CTabFolder) editor.getContainer()).setTabHeight(1);
				Point point = editor.getContainer().getSize();
				editor.getContainer().setSize(point.x, point.y + 6);
			}
		}
	}

	/**
	 * This is called during startup. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 */
	public void init() {
		initializeEditingDomain();

		editor.getSite().setSelectionProvider(editorSelectionProvider);

		editor.getSite().getPage().addPartListener(partListener);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
	}

	/**
	 * This sets up the editing domain for the model editor. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 */
	protected void initializeEditingDomain() {
		// Create the editing domain with a special command stack.
		modelSet = createModelSet();
		initializeModelSet(modelSet);

		editingDomain = getExistingEditingDomain(modelSet);

		if (editingDomain == null) {
			// Create an adapter factory that yields item providers.
			ownedAdapterFactory = new ComposedAdapterFactory(
					ComposedAdapterFactory.IDescriptor.IRegistry.INSTANCE);

			// Create the command stack that will notify this editor as commands
			// are
			// executed.
			EditingDomainCommandStack commandStack = new EditingDomainCommandStack();

			editingDomain = new AdapterFactoryEditingDomain(
					ownedAdapterFactory, commandStack, modelSet);
			commandStack.setEditingDomain(editingDomain);
			editingDomain
					.setModelToReadOnlyMap(new java.util.WeakHashMap<IModel, Boolean>());
		}

		// Add a listener to set the most recent command's affected objects to
		// be the selection of the viewer with focus.

		commandStackListener = new ICommandStackListener() {
			public void commandStackChanged(final EventObject event) {
				editor.getContainer().getDisplay().asyncExec(new Runnable() {
					public void run() {
						editor.firePropertyChange(IEditorPart.PROP_DIRTY);

						// Try to select the affected objects.
						ICommand mostRecentCommand = ((ICommandStack) event
								.getSource()).getMostRecentCommand();
						if (mostRecentCommand != null) {
							editorSelectionProvider
									.setSelectionToViewer(mostRecentCommand
											.getAffectedObjects());
						}
						if (propertySheetPageSupport != null) {
							propertySheetPageSupport.refresh();
						}
					}
				});
			}
		};
		editingDomain.getCommandStack().addCommandStackListener(
				commandStackListener);

		propertySheetPageSupport = createPropertySheetPageSupport();
	}

	protected void initializeModelSet(IModelSet modelSet) {
	}

	protected AdapterFactoryEditingDomain getExistingEditingDomain(
			IModelSet modelSet) {
		IEditingDomainProvider provider = (IEditingDomainProvider) modelSet
				.adapters().getAdapter(IEditingDomainProvider.class);

		if (provider != null) {
			return (AdapterFactoryEditingDomain) provider.getEditingDomain();
		}

		return null;
	}

	/**
	 * This is for implementing {@link IEditorPart} and simply tests the command
	 * stack. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public boolean isDirty() {
		return ((BasicCommandStack) editingDomain.getCommandStack())
				.isSaveNeeded();
	}

	/**
	 * This always returns true because it is not currently supported. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * This implements {@link org.eclipse.jface.action.IMenuListener} to help
	 * fill the context menus with contributions from the Edit menu. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public void menuAboutToShow(IMenuManager menuManager) {
		((IMenuListener) editor.getEditorSite().getActionBarContributor())
				.menuAboutToShow(menuManager);
	}

	public void handlePageChange() {
		Object activeEditor = editor.getSelectedPage();
		if (activeEditor instanceof ISelectionProvider) {
			editorSelectionProvider
					.setSelectionProvider((ISelectionProvider) activeEditor);
		}
		if (contentOutlinePage != null) {
			handleContentOutlineSelection(contentOutlinePage.getSelection());
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public void setStatusLineManager(ISelection selection) {
		IStatusLineManager statusLineManager = editorSelectionProvider
				.getSelectionProvider() != null
				&& editorSelectionProvider.getSelectionProvider() == contentOutlineViewer ? contentOutlineStatusLineManager
				: getActionBars().getStatusLineManager();

		if (statusLineManager != null) {
			if (selection instanceof IStructuredSelection) {
				Collection<?> collection = ((IStructuredSelection) selection)
						.toList();
				switch (collection.size()) {
				case 0: {
					statusLineManager
							.setMessage(getString("_UI_NoObjectSelected"));
					break;
				}
				case 1: {
					String text = new AdapterFactoryItemDelegator(
							getAdapterFactory()).getText(collection.iterator()
							.next());
					statusLineManager.setMessage(getString(
							"_UI_SingleObjectSelected", text));
					break;
				}
				default: {
					statusLineManager.setMessage(getString(
							"_UI_MultiObjectSelected",
							Integer.toString(collection.size())));
					break;
				}
				}
			} else {
				statusLineManager.setMessage("");
			}
		}
	}

	/**
	 * Returns whether the outline view should be presented to the user. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	protected boolean showOutlineView() {
		return true;
	}

	/**
	 * If there is more than one page in the multi-page editor part, this shows
	 * the tabs at the bottom. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public void showTabs() {
		if (editor.getPageCount() > 1) {
			editor.setPageText(0, getString("_UI_SelectionPage_label"));
			if (editor.getContainer() instanceof CTabFolder) {
				((CTabFolder) editor.getContainer()).setTabHeight(SWT.DEFAULT);
				Point point = editor.getContainer().getSize();
				editor.getContainer().setSize(point.x, point.y - 6);
			}
		}
	}

	/**
	 * Updates the problems indication with the information described in the
	 * specified diagnostic. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 */
	public void updateProblemIndication() {
		if (updateProblemIndication) {
			BasicDiagnostic diagnostic = new BasicDiagnostic(Diagnostic.OK,
					"net.enilink.komma.edit.ui.editor", 0, null,
					new Object[] { editingDomain.getModelSet() });
			for (Diagnostic childDiagnostic : modelToDiagnosticMap.values()) {
				if (childDiagnostic.getSeverity() != Diagnostic.OK) {
					diagnostic.add(childDiagnostic);
				}
			}

			int lastEditorPage = editor.getPageCount() - 1;
			if (lastEditorPage >= 0
					&& editor.getEditor(lastEditorPage) instanceof ProblemEditorPart) {
				((ProblemEditorPart) editor.getEditor(lastEditorPage))
						.setDiagnostic(diagnostic);
				if (diagnostic.getSeverity() != Diagnostic.OK) {
					editor.setActivePage(lastEditorPage);
				}
			} else if (diagnostic.getSeverity() != Diagnostic.OK) {
				ProblemEditorPart problemEditorPart = new ProblemEditorPart();
				problemEditorPart.setDiagnostic(diagnostic);
				problemEditorPart.setMarkerHelper(markerHelper);
				try {
					editor.addPage(++lastEditorPage, problemEditorPart,
							editor.getEditorInput());
					editor.setPageText(lastEditorPage,
							problemEditorPart.getPartName());
					editor.setActivePage(lastEditorPage);
					showTabs();
				} catch (PartInitException exception) {
					KommaEditUIPlugin.INSTANCE.log(exception);
				}
			}

			if (markerHelper.hasMarkers(editingDomain.getModelSet())) {
				markerHelper.deleteMarkers(editingDomain.getModelSet());
				if (diagnostic.getSeverity() != Diagnostic.OK) {
					try {
						markerHelper.createMarkers(diagnostic);
					} catch (CoreException exception) {
						KommaEditUIPlugin.INSTANCE.log(exception);
					}
				}
			}
		}
	}
}
