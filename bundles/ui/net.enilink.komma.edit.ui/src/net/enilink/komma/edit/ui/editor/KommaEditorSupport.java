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
import net.enilink.komma.common.util.BasicDiagnostic;
import net.enilink.komma.common.util.Diagnostic;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.command.EditingDomainCommandStack;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.provider.AdapterFactoryItemDelegator;
import net.enilink.komma.edit.provider.ComposedAdapterFactory;
import net.enilink.komma.edit.provider.ReflectiveItemProviderAdapterFactory;
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
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelSetModule;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.model.base.IURIMapRuleSet;
import net.enilink.komma.model.base.SimpleURIMapRule;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.komma.model.validation.IValidator;

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
import org.eclipse.jface.dialogs.IPageChangeProvider;
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
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetPage;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * This is a base class for a multi-page model editor.
 */
public abstract class KommaEditorSupport<E extends ISupportedEditor> implements
		IEditingDomainProvider, IMenuListener, IAdaptable {
	protected class EditorSelectionProvider extends
			PostSelectionProviderAdapter {
		/**
		 * This keeps track of the selection of the editor as a whole.
		 * 
		 */
		protected ISelection editorSelection = StructuredSelection.EMPTY;

		/**
		 * This listens to which ever viewer is active.
		 */
		protected ISelectionChangedListener selectionChangedListener;

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

		public ISelectionProvider getSelectionProvider() {
			return selectionProvider;
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
							if (editorSelection == null
									|| !editorSelection
											.equals(selectionChangedEvent
													.getSelection())) {
								setSelection(selectionChangedEvent
										.getSelection());
							}
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
		 * This sets the selection into whichever viewer is active.
		 */
		public void setSelectionToViewer(Collection<?> collection) {
			final Collection<?> theSelection = collection;
			// Make sure it's okay.
			if (theSelection != null && !theSelection.isEmpty()) {
				// I don't know if this should be run this deferred
				// because we might have to give the editor a chance to process
				// the viewer update events
				// and hence to update the views first.
				Runnable runnable = new Runnable() {
					public void run() {
						// Try to select the items in the current content viewer
						// of the editor.
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
	};

	/**
	 * Resources that have been changed since last activation.
	 * 
	 */
	protected Collection<IModel> changedModels = new ArrayList<IModel>();

	protected ICommandStackListener commandStackListener;

	/**
	 * This is the content outline page.
	 * 
	 */
	protected IContentOutlinePage contentOutlinePage;

	/**
	 * This is a kludge...
	 * 
	 */
	protected IStatusLineManager contentOutlineStatusLineManager;

	/**
	 * This is the content outline page's viewer.
	 * 
	 */
	protected TreeViewer contentOutlineViewer;

	/**
	 * This keeps track of the editing domain that is used to track all changes
	 * to the model.
	 * 
	 */
	private AdapterFactoryEditingDomain editingDomain;

	protected E editor;

	protected EditorSelectionProvider editorSelectionProvider = new EditorSelectionProvider();

	/**
	 * The MarkerHelper is responsible for creating workspace resource markers
	 * presented in Eclipse's Problems View.
	 * 
	 */
	protected MarkerHelper markerHelper = new EditUIMarkerHelper();

	protected IModel model;

	protected IModelSet modelSet;

	protected boolean disposeModelSet = true;

	protected boolean saveAllModels = true;

	/**
	 * Map to store the diagnostic associated with a resource.
	 * 
	 */
	protected Map<IModel, Diagnostic> modelToDiagnosticMap = new LinkedHashMap<IModel, Diagnostic>();

	protected IOperationHistory operationHistory;

	protected ComposedAdapterFactory ownedAdapterFactory;

	@Inject
	protected Injector injector;

	/**
	 * This listens for when the outline becomes active.
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
				if (provider != null
						&& getEditingDomain() != null
						&& getEditingDomain().equals(
								provider.getEditingDomain())) {
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

	protected INotificationListener<INotification> modifiedListener = new INotificationListener<INotification>() {
		@Override
		public NotificationFilter<INotification> getFilter() {
			return null;
		}

		@Override
		public void notifyChanged(
				Collection<? extends INotification> notifications) {
			for (INotification notification : notifications) {
				if (notification instanceof IStatementNotification) {
					if (MODELS.PROPERTY_MODIFIED
							.equals(((IStatementNotification) notification)
									.getPredicate())
							&& model.equals(notification.getSubject())) {
						editor.getSite().getShell().getDisplay()
								.asyncExec(new Runnable() {
									@Override
									public void run() {
										editor.firePropertyChange(IEditorPart.PROP_DIRTY);
									}
								});
					}
				}
			}
		}
	};

	/**
	 * Adapter used to update the problem indication when models are demand
	 * loaded.
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
						IReference model = ((IStatementNotification) notification)
								.getModelSet()
								.getMetaDataManager()
								.find(((IStatementNotification) notification)
										.getSubject());
						if (model instanceof IModel) {
							Diagnostic diagnostic = analyzeModelProblems(
									(IModel) model, null);
							if (diagnostic.getSeverity() != Diagnostic.OK) {
								modelToDiagnosticMap.put((IModel) model,
										diagnostic);
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
		}
	};

	/**
	 * This is the property sheet page support.
	 * 
	 */
	protected IPropertySheetPageSupport propertySheetPageSupport;

	/**
	 * Resources that have been removed since last activation.
	 * 
	 */
	protected Collection<IModel> removedModels = new ArrayList<IModel>();

	/**
	 * This listens for workspace changes.
	 * 
	 */
	protected IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta delta = event.getDelta();
			try {
				class ResourceDeltaVisitor implements IResourceDeltaVisitor {
					protected Collection<IModel> changedModels = new ArrayList<IModel>();
					protected Collection<IModel> removedModels = new ArrayList<IModel>();
					final IModelSet modelSet = KommaEditorSupport.this.modelSet;

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
								IModel model = modelSet.getModel(URIs
										.createPlatformResourceURI(delta
												.getFullPath().toString(),
												false), false);
								if (model != null
										// no need to keep track of changes to
										// models the editors don't care about
										&& (saveAllModels || model
												.equals(KommaEditorSupport.this.model))) {
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

				modelSet.getUnitOfWork().begin();
				try {
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
				} finally {
					modelSet.getUnitOfWork().end();
				}
			} catch (CoreException exception) {
				KommaEditUIPlugin.INSTANCE.log(exception);
			}
		}
	};

	/**
	 * Resources that have been saved.
	 * 
	 */
	protected Collection<IModel> savedModels = new ArrayList<IModel>();

	/**
	 * Controls whether the problem indication should be updated.
	 * 
	 */
	protected boolean updateProblemIndication = true;

	public KommaEditorSupport(E editor) {
		this.editor = editor;
	}

	/**
	 * Returns a diagnostic describing the errors and warnings listed in the
	 * resource and the specified exception (if any).
	 * 
	 */
	public Diagnostic analyzeModelProblems(IModel model, Exception exception) {
		if (!model.getErrors().isEmpty() || !model.getWarnings().isEmpty()) {
			BasicDiagnostic basicDiagnostic = new BasicDiagnostic(
					Diagnostic.ERROR, KommaEditUIPlugin.PLUGIN_ID, 0,
					getString("_UI_CreateModelError_message", model.getURI()),
					new Object[] { model, exception });
			basicDiagnostic.merge(ModelUtil.computeDiagnostic(model, true));
			return basicDiagnostic;
		} else if (exception != null) {
			return new BasicDiagnostic(Diagnostic.ERROR,
					KommaEditUIPlugin.PLUGIN_ID, 0, getString(
							"_UI_CreateModelError_message", model.getURI()),
					new Object[] { model, exception });
		} else {
			return Diagnostic.OK_INSTANCE;
		}
	}

	/**
	 * This creates a context menu for the viewer and adds a listener as well
	 * registering the menu for extension.
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
		editor.getSite().registerContextMenu("net.enilink.komma.edit.ui.menu",
				contextMenu, menuSelectionProvider);

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
				new EditingDomainViewerDropAdapter(getEditingDomain(), viewer));
	}

	/**
	 * This is the method called to load a resource into the editing domain's
	 * model set based on the editor's input.
	 */
	public void createModel() {
		URI resourceURI = EditUIUtil.getURI(editor.getEditorInput());
		if (resourceURI.scheme() == null
				|| !resourceURI.scheme().toLowerCase().startsWith("http")) {
			IURIConverter uriConverter = modelSet.getURIConverter();
			try (InputStream in = uriConverter.createInputStream(resourceURI)) {
				String ontology = ModelUtil.findOntology(in, resourceURI
						.toString(), ModelUtil.mimeType(ModelUtil
						.contentDescription(uriConverter, resourceURI)));
				if (ontology != null) {
					modelSet.getURIConverter()
							.getURIMapRules()
							.addRule(
									new SimpleURIMapRule(ontology, resourceURI
											.toString()));
					resourceURI = URIs.createURI(ontology);
				}
			} catch (Exception e) {
				KommaEditUIPlugin.INSTANCE.log(e);
			}
		}

		Exception exception = null;
		try {
			model = modelSet.getModel(resourceURI, true);
		} catch (Exception e) {
			exception = e;
			model = modelSet.createModel(resourceURI);
		}

		Diagnostic diagnostic = analyzeModelProblems(model, exception);
		if (diagnostic.getSeverity() != Diagnostic.OK) {
			modelToDiagnosticMap.put(model, diagnostic);
		}
		modelSet.addMetaDataListener(problemIndicationListener);
		modelSet.addMetaDataListener(modifiedListener);
	}

	protected IModelSet createModelSet() {
		KommaModule module = ModelPlugin.createModelSetModule(getClass()
				.getClassLoader());
		IModelSetFactory factory = Guice.createInjector(
				new ModelSetModule(module)).getInstance(IModelSetFactory.class);
		IModelSet modelSet = factory.createModelSet(URIs
				.createURI(MODELS.NAMESPACE +
				// "MemoryModelSet" //
						"OwlimModelSet" //
				));
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

	public void dispose() {
		updateProblemIndication = false;

		ResourcesPlugin.getWorkspace().removeResourceChangeListener(
				resourceChangeListener);

		editor.getSite().getPage().removePartListener(partListener);

		if (getEditingDomain() != null) {
			getEditingDomain().getCommandStack().removeCommandStackListener(
					commandStackListener);
			modelSet.removeMetaDataListener(modifiedListener);
			modelSet.removeMetaDataListener(problemIndicationListener);
			setEditingDomain(null);
		}

		if (editorSelectionProvider != null) {
			// ensure that selection listener is removed
			editorSelectionProvider.setSelectionProvider(null);
			editorSelectionProvider = null;
		}

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

		if (disposeModelSet && modelSet != null) {
			modelSet.dispose();
			modelSet = null;
		}
	}

	protected boolean saveModel(IModel model, Map<Object, Object> saveOptions) {
		if (model.isModified() && !getEditingDomain().isReadOnly(model)) {
			try {
				model.save(saveOptions);
				savedModels.add(model);
				return true;
			} catch (Exception exception) {
				modelToDiagnosticMap.put(model,
						analyzeModelProblems(model, exception));
			}
		}
		return false;
	}

	/**
	 * This is for implementing {@link IEditorPart} and simply saves the model
	 * file.
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
				try {
					modelSet.getUnitOfWork().begin();
					if (saveAllModels) {
						for (IModel model : modelSet.getModels()) {
							saveModel(model, saveOptions);
						}
					} else {
						saveModel(model, saveOptions);
					}
				} finally {
					modelSet.getUnitOfWork().end();
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
			((BasicCommandStack) getEditingDomain().getCommandStack())
					.saveIsDone();
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
				doSaveAs(URIs.createPlatformResourceURI(file.getFullPath()
						.toString(), true), EclipseUtil.createEditorInput(file));
			}
		}
	}

	protected void doSaveAs(URI uri, IEditorInput editorInput) {
		URI oldResourceURI = EditUIUtil.getURI(editor.getEditorInput());
		URI oldModelURI = model.getURI();
		if (oldModelURI.isPlatformResource()
				&& oldModelURI.equals(oldResourceURI)) {
			// rename model
			model.setURI(uri);
		}

		// update mapping rules
		IURIMapRuleSet mapRules = modelSet.getURIConverter().getURIMapRules();
		mapRules.removeRule(new SimpleURIMapRule(oldModelURI.toString(),
				oldResourceURI.toString()));
		mapRules.addRule(new SimpleURIMapRule(model.getURI().toString(), uri
				.toString()));

		// mark model as modified
		model.setModified(true);
		editor.setInputWithNotify(editorInput);
		editor.setPartName(editorInput.getName());
		IProgressMonitor progressMonitor = getActionBars()
				.getStatusLineManager() != null ? getActionBars()
				.getStatusLineManager().getProgressMonitor()
				: new NullProgressMonitor();
		doSave(progressMonitor);
	}

	public EditingDomainActionBarContributor getActionBarContributor() {
		return (EditingDomainActionBarContributor) editor.getEditorSite()
				.getActionBarContributor();
	}

	public IActionBars getActionBars() {
		return getActionBarContributor().getActionBars();
	}

	/**
	 * This is how the framework determines which interfaces we implement.
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
		return null;
	}

	public IAdapterFactory getAdapterFactory() {
		return ((AdapterFactoryEditingDomain) getEditingDomain())
				.getAdapterFactory();
	}

	/**
	 * This accesses a cached version of the content outliner.
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
					contentOutlineViewer
							.setContentProvider(new AdapterFactoryContentProvider(
									getAdapterFactory()));
					contentOutlineViewer
							.setLabelProvider(new AdapterFactoryLabelProvider(
									getAdapterFactory()));
					contentOutlineViewer.setInput(modelSet);

					// Make sure our popups work.
					createContextMenuFor(contentOutlineViewer);

					if (!modelSet.getModels().isEmpty()) {
						// Select the root object in the view.
						contentOutlineViewer.setSelection(
								new StructuredSelection(modelSet.getModels()
										.iterator().next()), true);
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
	 * 
	 */
	public AdapterFactoryEditingDomain getEditingDomain() {
		return editingDomain;
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

	public IModel getModel() {
		return model;
	}

	protected abstract IResourceLocator getResourceLocator();

	protected IPath getSaveAsPath() {
		return null;
	}

	/**
	 * This looks up a string in the plugin's plugin.properties file.
	 * 
	 */
	public String getString(String key) {
		return getResourceLocator().getString(key);
	}

	/**
	 * This looks up a string in plugin.properties, making a substitution.
	 * 
	 */
	public String getString(String key, Object s1) {
		return getResourceLocator().getString(key, new Object[] { s1 });
	}

	public void gotoMarker(IMarker marker) {
		try {
			if (marker.getType().equals(IValidator.MARKER)) {
				String uriAttribute = marker.getAttribute(
						IValidator.URI_ATTRIBUTE, null);
				if (uriAttribute != null) {
					URI uri = URIs.createURI(uriAttribute);
					IObject object = modelSet.getObject(uri, true);
					if (object != null) {
						editorSelectionProvider
								.setSelectionToViewer(Collections
										.singleton(getEditingDomain()
												.getWrapper(object)));
					}
				}
			}
		} catch (CoreException exception) {
			KommaEditUIPlugin.INSTANCE.log(exception);
		}
	}

	/**
	 * Handles activation of the editor or it's associated views.
	 * 
	 */
	protected void handleActivate() {
		handlePageChange(editor instanceof IPageChangeProvider ? ((IPageChangeProvider) editor)
				.getSelectedPage() : null);

		// Recompute the read only state.
		if (getEditingDomain().getModelToReadOnlyMap() != null) {
			getEditingDomain().getModelToReadOnlyMap().clear();

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
	 * Handles what to do with changed models on activation.
	 * 
	 */
	protected void handleChangedModels() {
		if (!changedModels.isEmpty() && (!isDirty() || handleDirtyConflict())) {
			if (isDirty()) {
				changedModels.addAll(modelSet.getModels());
			}
			getEditingDomain().getCommandStack().flush();

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
	 * views.
	 * 
	 */
	public void handleContentOutlineSelection(ISelection selection) {

	}

	/**
	 * Shows a dialog that asks if conflicting changes should be discarded.
	 * 
	 */
	protected boolean handleDirtyConflict() {
		return MessageDialog.openQuestion(editor.getSite().getShell(),
				getString("_UI_FileConflict_label"),
				getString("_WARN_FileConflict"));
	}

	public void handlePageChange(Object activeEditor) {
		if (activeEditor instanceof ISelectionProvider) {
			editorSelectionProvider
					.setSelectionProvider((ISelectionProvider) activeEditor);
		}
		if (contentOutlinePage != null) {
			handleContentOutlineSelection(contentOutlinePage.getSelection());
		}
	}

	/**
	 * This is called during startup.
	 * 
	 */
	public void init() {
		initializeEditingDomain();
		editor.getSite().setSelectionProvider(editorSelectionProvider);
		editor.getSite().getPage().addPartListener(partListener);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
	}

	protected ComposedAdapterFactory createDefaultAdapterFactory() {
		ComposedAdapterFactory adapterFactory = new ComposedAdapterFactory(
				ComposedAdapterFactory.IDescriptor.IRegistry.INSTANCE) {
			/**
			 * Default adapter factory for all namespaces
			 */
			class DefaultItemProviderAdapterFactory extends
					ReflectiveItemProviderAdapterFactory {
				public DefaultItemProviderAdapterFactory() {
					super(KommaEditPlugin.getPlugin());
				}

				@Override
				public Object adapt(Object object, Object type) {
					if (object instanceof IClass) {
						// do not override the adapter for classes
						return null;
					}
					return super.adapt(object, type);
				}

				public boolean isFactoryForType(Object type) {
					// support any namespace
					return type instanceof URI || supportedTypes.contains(type);
				}

				@Override
				protected Collection<IClass> getTypes(Object object) {
					return object instanceof net.enilink.komma.em.concepts.IResource ? super
							.getTypes(object) : Collections
							.<IClass> emptyList();
				}
			}

			DefaultItemProviderAdapterFactory defaultAdapterFactory;
			{
				defaultAdapterFactory = new DefaultItemProviderAdapterFactory();
				defaultAdapterFactory.setParentAdapterFactory(this);
			}

			@Inject
			protected void setInjector(Injector injector) {
				injector.injectMembers(defaultAdapterFactory);
			}

			@Override
			protected IAdapterFactory getDefaultAdapterFactory(Object type) {
				// provide a default adapter factory as fallback if no
				// specific adapter factory was found
				return defaultAdapterFactory;
			}
		};
		if (injector != null) {
			injector.injectMembers(adapterFactory);
		}
		return adapterFactory;
	}

	/**
	 * This sets up the editing domain for the model editor.
	 * 
	 */
	protected void initializeEditingDomain() {
		// Create the editing domain with a special command stack.
		modelSet = createModelSet();
		initializeModelSet(modelSet);

		setEditingDomain(getExistingEditingDomain(modelSet));
		if (getEditingDomain() == null) {
			// Create an adapter factory that yields item providers.
			ownedAdapterFactory = createDefaultAdapterFactory();
			// Create the command stack that will notify this editor as commands
			// are executed.
			EditingDomainCommandStack commandStack = new EditingDomainCommandStack();
			setEditingDomain(new AdapterFactoryEditingDomain(
					ownedAdapterFactory, commandStack, modelSet));
			commandStack.setEditingDomain(getEditingDomain());
			getEditingDomain().setModelToReadOnlyMap(
					new java.util.WeakHashMap<IModel, Boolean>());
		}

		// Add a listener to set the most recent command's affected objects to
		// be the selection of the viewer with focus.
		commandStackListener = new ICommandStackListener() {
			public void commandStackChanged(final EventObject event) {
				editor.getEditorSite().getShell().getDisplay()
						.asyncExec(new Runnable() {
							public void run() {
								editor.firePropertyChange(IEditorPart.PROP_DIRTY);

								// Try to select the affected objects.
								ICommand mostRecentCommand = ((ICommandStack) event
										.getSource()).getMostRecentCommand();
								if (mostRecentCommand != null
										&& !mostRecentCommand
												.getAffectedObjects().isEmpty()) {
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
		getEditingDomain().getCommandStack().addCommandStackListener(
				commandStackListener);
		propertySheetPageSupport = createPropertySheetPageSupport();
	}

	protected void initializeModelSet(IModelSet modelSet) {
	}

	/**
	 * This is for implementing {@link IEditorPart} and simply tests the command
	 * stack.
	 * 
	 */
	public boolean isDirty() {
		return getEditingDomain() != null
				&& (((BasicCommandStack) getEditingDomain().getCommandStack())
						.isSaveNeeded() || getModel().isModified());
	}

	/**
	 * This always returns true because it is not currently supported.
	 * 
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * This implements {@link org.eclipse.jface.action.IMenuListener} to help
	 * fill the context menus with contributions from the Edit menu.
	 * 
	 */
	public void menuAboutToShow(IMenuManager menuManager) {
		IEditorActionBarContributor actionBarContributor = editor
				.getEditorSite().getActionBarContributor();
		if (actionBarContributor instanceof IMenuListener) {
			((IMenuListener) actionBarContributor).menuAboutToShow(menuManager);
		}
	}

	protected void setEditingDomain(AdapterFactoryEditingDomain editingDomain) {
		this.editingDomain = editingDomain;
	}

	public void setSelectionProvider(ISelectionProvider selectionProvider) {
		editorSelectionProvider.setSelectionProvider(selectionProvider);
	}

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
	 * Returns whether the outline view should be presented to the user.
	 * 
	 */
	protected boolean showOutlineView() {
		return true;
	}

	/**
	 * Updates the problems indication with the information described in the
	 * specified diagnostic.
	 * 
	 */
	public void updateProblemIndication() {
		if (updateProblemIndication) {
			BasicDiagnostic diagnostic = new BasicDiagnostic(Diagnostic.OK,
					"net.enilink.komma.edit.ui.editor", 0, null,
					new Object[] { modelSet });
			for (Diagnostic childDiagnostic : modelToDiagnosticMap.values()) {
				if (childDiagnostic.getSeverity() != Diagnostic.OK) {
					diagnostic.add(childDiagnostic);
				}
			}
			if (markerHelper.hasMarkers(modelSet)) {
				markerHelper.deleteMarkers(modelSet);
			}
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
