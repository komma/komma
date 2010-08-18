package net.enilink.komma.edit.ui.views;

import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
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
			if (part != null) {
				setWorkbenchPart(part);
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
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (part != AbstractEditingDomainView.this
					&& selection instanceof IStructuredSelection) {
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

				if (editorForm.setInput(selected)) {
					editorForm.refreshStale();
				}
			}
		}
	}

	private AbstractEditingDomainPart editPart;

	private EditorForm editorForm;

	private Listener listener;

	private IEditingDomainProvider editingDomainProvider;

	private ISelectionProvider selectionProvider = new SelectionProviderAdapter();

	protected IWorkbenchPart part;

	protected IModel model;

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		editorForm = new EditorForm(parent) {
			@Override
			public void fireSelectionChanged(IEditorPart firingPart,
					final ISelection selection) {
				selectionProvider.setSelection(selection);
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
				} else if (IViewerMenuSupport.class.equals(adapter)
						&& part instanceof IViewerMenuSupport) {
					return (IViewerMenuSupport) part;
				}
				return null;
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

	protected void installSelectionProvider() {
		getSite().setSelectionProvider(selectionProvider);
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

	public AbstractEditingDomainPart getEditPart() {
		return editPart;
	}

	protected void setEditingDomainProvider(
			IEditingDomainProvider editingDomainProvider) {
		this.editingDomainProvider = editingDomainProvider;
		if (editingDomainProvider == null) {
			this.model = null;
		} else {
			Set<IModel> models = editingDomainProvider.getEditingDomain()
					.getModelSet().getModels();
			if (!models.isEmpty()) {
				this.model = models.iterator().next();
			}
		}
		if (editPart != null) {
			selectionProvider.setSelection(StructuredSelection.EMPTY);

			editorForm.setInput(null);
			editPart.setInput(this.model);
			editorForm.refreshStale();
		}
	}

	public void setEditPart(AbstractEditingDomainPart viewPart) {
		this.editPart = viewPart;
	}

	@Override
	public void setFocus() {
		editorForm.setFocus();
	}

	protected void setWorkbenchPart(final IWorkbenchPart part) {
		if (part == null) {
			this.part = null;
			setEditingDomainProvider(null);
		} else if (part instanceof IEditingDomainProvider) {
			this.part = part;
			setEditingDomainProvider((IEditingDomainProvider) part);
		} else if (part != null) {
			IEditingDomainProvider provider = (IEditingDomainProvider) part
					.getAdapter(IEditingDomainProvider.class);
			if (provider != null
					&& !provider.equals(this.editingDomainProvider)) {
				this.part = part;
				setEditingDomainProvider(provider);
			}
		}
	}

	@Override
	public IWorkbenchPart getContributingPart() {
		return part;
	}

}
