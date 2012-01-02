package net.enilink.komma.edit.ui.views;

import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.IContributedContentsView;
import org.eclipse.ui.part.ViewPart;

import net.enilink.commons.ui.editor.EditorForm;
import net.enilink.commons.ui.editor.IEditorPart;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.util.PartListener2Adapter;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IValue;

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
			if (part != null && part.equals(partRef.getPart(false))) {
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
				if (IToolBarManager.class.equals(adapter)) {
					return getViewSite().getActionBars().getToolBarManager();
				}
				if (IEditingDomainProvider.class.equals(adapter)) {
					return editingDomainProvider;
				} else if (IViewerMenuSupport.class.equals(adapter)) {
					if (part instanceof IViewerMenuSupport) {
						return (IViewerMenuSupport) part;
					} else if (part != null) {
						return part.getAdapter(IViewerMenuSupport.class);
					}
				}
				return delegatedGetAdapter(adapter);
			}
		};
		editorForm.addPart(editPart);

		editPart.createContents(parent);

		installSelectionProvider();

		IWorkbenchPart editor = getSite().getPage().getActiveEditor();
		if (editor != null) {
			setWorkbenchPart(editor);
		}

		this.listener = new Listener();
		getSite().getPage().addPartListener(listener);
		getSite().getPage().addSelectionListener(listener);
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
				&& lastProvider.getEditingDomain().equals(
						editingDomainProvider.getEditingDomain())) {
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

		IModel model = null;
		if (part != null) {
			model = (IModel) part.getAdapter(IModel.class);
			if (domainChanged && model == null && editingDomainProvider != null) {
				Set<IModel> models = editingDomainProvider.getEditingDomain()
						.getModelSet().getModels();
				if (!models.isEmpty()) {
					model = models.iterator().next();
				}
			}
		}

		boolean changed = domainChanged;
		if (domainChanged || model != null) {
			changed |= this.model != model || this.model != null
					&& !this.model.equals(model);
			this.model = model;
		}

		if (changed && editPart != null) {
			selectionProvider.setInternalSelection(StructuredSelection.EMPTY);

			if (active) {
				editorForm.setInput(null);
				editPart.setInput(this.model);
				editorForm.refreshStale();
			} else {
				setInputOnActivate = true;
			}
		}
	}
}
