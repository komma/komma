package net.enilink.komma.edit.ui.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.enilink.commons.ui.editor.EditorForm;
import net.enilink.commons.ui.editor.IEditorPart;
import net.enilink.komma.core.IValue;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.util.PartListener2Adapter;
import net.enilink.komma.model.IModel;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IContributedContentsView;
import org.eclipse.ui.part.ViewPart;

public class AbstractEditingDomainView extends ViewPart implements
		IContributedContentsView {
	private class Listener extends PartListener2Adapter implements
			ISelectionListener {
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			if (AbstractEditingDomainView.this == part) {
				if (!active) {
					active = true;

					if (setInputOnActivate) {
						editorForm.setInput(null);
						editPart.setInput(model);
						editorForm.refreshStale();

						setInputOnActivate = false;
					}
					if (selectionOnActivate != null) {
						if (editorForm.setInput(selectionOnActivate)) {
							editorForm.refreshStale();
						}
						selectionOnActivate = null;
					}
				}
			} else if (part != null) {
				setWorkbenchPart(part);
			}
		}

		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			if (AbstractEditingDomainView.this == part && !active) {
				partActivated(partRef);
			}
		}

		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
			if (part != null && part.equals(partRef.getPart(false))
					&& !PlatformUI.getWorkbench().isClosing()) {
				setWorkbenchPart(null);
			}
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partHidden(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			if (AbstractEditingDomainView.this == part) {
				active = false;
			}
		}

		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (part != AbstractEditingDomainView.this
					&& selection instanceof IStructuredSelection
					&& !selection.equals(selectionProvider.getSelection())) {
				Object selected = ((IStructuredSelection) selection)
						.getFirstElement();

				// allow arbitrary selections to be adapted to IValue objects
				if (selected != null && !(selected instanceof IValue)) {
					Object adapter = Platform.getAdapterManager().getAdapter(
							selected, IValue.class);
					if (adapter != null) {
						selected = adapter;
					}
				}

				if (selected != null) {
					if (active) {
						if (editorForm.setInput(selected)) {
							editorForm.refreshStale();
						}
					} else {
						selectionOnActivate = selected;
					}
				}
			}
		}
	}

	private class SelectionProvider extends SelectionProviderAdapter {
		public void setInternalSelection(ISelection selection) {
			// set selection without propagating to other parts
			super.setSelection(selection);
		}

		@Override
		public void setSelection(ISelection selection) {
			// try to propagate selection to interested parts
			if (selection instanceof IStructuredSelection) {
				Object selected = ((IStructuredSelection) selection)
						.getFirstElement();
				if (editorForm.setInput(selected)) {
					// input was accepted, selection changed events are
					// automatically triggered by the respective part
					return;
				}
			}
			// input was not accepted by any part, so we can't do anything
		}
	}

	private boolean active = true;
	private IEditingDomainProvider editingDomainProvider;

	private EditorForm editorForm;

	private AbstractEditingDomainPart editPart;

	private Listener listener;

	protected IModel model;

	protected IWorkbenchPart part;

	private Object selectionOnActivate;

	private SelectionProvider selectionProvider = new SelectionProvider();

	private boolean setInputOnActivate;

	private List<String> redirectedActionIds = new ArrayList<>();

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		editorForm = new EditorForm(parent) {
			@Override
			public void fireSelectionChanged(IEditorPart firingPart,
					final ISelection selection) {
				selectionProvider.setInternalSelection(selection);
				super.fireSelectionChanged(firingPart, selection);
			}

			@Override
			@SuppressWarnings("rawtypes")
			public Object getAdapter(Class adapter) {
				if (IWorkbenchPartSite.class.equals(adapter)
						|| IViewSite.class.equals(adapter)) {
					return getViewSite();
				}
				if (IToolBarManager.class.equals(adapter)) {
					return getViewSite().getActionBars().getToolBarManager();
				}
				if (IEditingDomainProvider.class.equals(adapter)) {
					return editingDomainProvider;
				} else if (IViewerMenuSupport.class.equals(adapter)) {
					return new IViewerMenuSupport() {
						@Override
						public void createContextMenuFor(StructuredViewer viewer, Control menuParent, IWorkbenchPartSite partSite) {
							IViewerMenuSupport delegate = null;
							if (part instanceof IViewerMenuSupport) {
								delegate = (IViewerMenuSupport) part;
							} else if (part != null) {
								delegate = part.getAdapter(IViewerMenuSupport.class);
							}
							if (delegate != null) {
								// use own site to register context menu
								delegate.createContextMenuFor(viewer, menuParent, getSite());
							}
						}
					};
				}
				return delegatedGetAdapter(adapter);
			}
		};
		editorForm.addPart(editPart);
		
		editorForm.getWidgetFactory().adapt(parent);
		editPart.createContents(parent);

		installSelectionProvider();

		this.listener = new Listener();
		getSite().getPage().addPartListener(listener);
		getSite().getPage().addSelectionListener(listener);

		IWorkbenchPart editor = getSite().getPage().getActiveEditor();
		if (editor != null) {
			setWorkbenchPart(editor);
			// on creation of this part, make sure it is made aware of the
			// current selection to avoid it being empty
			if (this.model != null) {
				this.listener.selectionChanged(editor, editor.getSite()
						.getSelectionProvider().getSelection());
			}
		}

		// redirect to editor actions
		List<ActionFactory> actionFactories = Arrays.asList(
				ActionFactory.DELETE, ActionFactory.CUT, ActionFactory.COPY,
				ActionFactory.PASTE, ActionFactory.UNDO, ActionFactory.REDO);
		IActionBars myBars = getViewSite().getActionBars();
		for (ActionFactory af : actionFactories) {
			if (myBars.getGlobalActionHandler(af.getId()) == null) {
				redirectedActionIds.add(af.getId());
			}
		}
	}

	protected Object delegatedGetAdapter(Class<?> adapter) {
		return null;
	}

	@Override
	public void dispose() {
		if (editorForm != null) {
			editorForm.dispose();
			editorForm = null;
		}
		if (listener != null) {
			getSite().getPage().removeSelectionListener(listener);
			getSite().getPage().removePartListener(listener);
			listener = null;
		}
		super.dispose();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		Object result = null;
		if (IEditingDomainProvider.class.equals(adapter)) {
			return editingDomainProvider;
		}
		if (IContributedContentsView.class.equals(adapter)) {
			return this;
		}
		return result == null ? super.getAdapter(adapter) : result;
	}

	@Override
	public IWorkbenchPart getContributingPart() {
		return part;
	}

	public AbstractEditingDomainPart getEditPart() {
		return editPart;
	}

	protected void installSelectionProvider() {
		getSite().setSelectionProvider(selectionProvider);
	}

	protected boolean setEditingDomainProvider(
			IEditingDomainProvider editingDomainProvider, IWorkbenchPart part) {
		IEditingDomainProvider lastProvider = this.editingDomainProvider;
		if (editingDomainProvider == null) {
			this.part = null;
			this.editingDomainProvider = null;
		} else if (lastProvider != null
				&& lastProvider.getEditingDomain() != null
				&& lastProvider.getEditingDomain().equals(
						editingDomainProvider.getEditingDomain())) {
			if (part instanceof org.eclipse.ui.IEditorPart) {
				// prefer editors over other parts since they are able to
				// provide additional functionality like IViewerMenuSupport
				this.part = part;
			}
			return false;
		} else {
			this.part = part;
			this.editingDomainProvider = editingDomainProvider;
		}
		return true;
	}

	public void setEditPart(AbstractEditingDomainPart viewPart) {
		this.editPart = viewPart;
	}

	@Override
	public void setFocus() {
		editorForm.setFocus();
	}

	protected void setWorkbenchPart(final IWorkbenchPart part) {
		boolean domainChanged = false;
		if (part == null) {
			domainChanged = setEditingDomainProvider(null, null);
		} else if (part instanceof IEditingDomainProvider) {
			domainChanged = setEditingDomainProvider(
					(IEditingDomainProvider) part, part);
		} else {
			IEditingDomainProvider provider = (IEditingDomainProvider) part
					.getAdapter(IEditingDomainProvider.class);
			if (provider != null) {
				domainChanged = setEditingDomainProvider(provider, part);
			}
		}

		IModel model = part != null ? (IModel) part.getAdapter(IModel.class)
				: null;
		boolean changed = domainChanged;
		if (domainChanged || model != null) {
			changed |= this.model != model || this.model != null
					&& !this.model.equals(model);
			this.model = model;
		}

		if (changed) {
			// redirect unbound global actions to editor actions
			IActionBars myBars = getViewSite().getActionBars();
			if (part != null && part.getSite() instanceof IEditorSite) {
				IActionBars editorBars = ((IEditorSite) part.getSite())
						.getActionBars();
				for (String id : redirectedActionIds) {
					myBars.setGlobalActionHandler(id,
							editorBars.getGlobalActionHandler(id));
				}
			}
			myBars.updateActionBars();
		}

		if (changed && editPart != null) {
			selectionProvider.setInternalSelection(StructuredSelection.EMPTY);

			if (active || this.model == null) {
				editorForm.setInput(null);
				editPart.setInput(this.model);
				editorForm.refreshStale();
			} else {
				setInputOnActivate = true;
			}
		}
	}
}
