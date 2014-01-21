package net.enilink.komma.owl.editor.internal;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import net.enilink.commons.ui.editor.EditorForm;
import net.enilink.commons.ui.editor.FormPart;
import net.enilink.commons.ui.editor.IEditorPart;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.views.IViewerMenuSupport;
import net.enilink.komma.owl.editor.OWLEditorPlugin;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;

public abstract class KommaFormPage extends FormPage implements
		ISelectionProvider {
	private EditorForm form;
	private ISelection currentSelection;
	private Set<ISelectionChangedListener> listeners = new CopyOnWriteArraySet<ISelectionChangedListener>();

	public KommaFormPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	public KommaFormPage(String id, String title) {
		super(id, title);
	}

	protected EditorForm getEditorForm() {
		if (form == null) {
			IManagedForm managedForm = getManagedForm();
			form = new EditorForm(managedForm.getForm(),
					managedForm.getToolkit()) {
				@Override
				public Object getAdapter(Class adapter) {
					if (IEditingDomainProvider.class.equals(adapter)) {
						return (IEditingDomainProvider) getEditor();
					} else if (IViewerMenuSupport.class.equals(adapter)) {
						return (IViewerMenuSupport) getEditor();
					}
					Object result = KommaFormPage.this.getAdapter(adapter);
					return result != null ? result : super.getAdapter(adapter);
				}

				@Override
				public void fireSelectionChanged(IEditorPart firingPart,
						ISelection selection) {
					currentSelection = selection;

					KommaFormPage.this.fireSelectionChanged(currentSelection);
				}
			};
			managedForm.addPart(new FormPart(form));
		}
		return form;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	protected void fireSelectionChanged(ISelection selection) {
		final SelectionChangedEvent e = new SelectionChangedEvent(this,
				selection);
		for (final ISelectionChangedListener listener : listeners) {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					OWLEditorPlugin.INSTANCE.log(exception);
				}

				@Override
				public void run() throws Exception {
					listener.selectionChanged(e);
				}
			});
		}
	}

	@Override
	public ISelection getSelection() {
		return currentSelection != null ? currentSelection
				: StructuredSelection.EMPTY;
	}

	@Override
	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		Object input = selection;
		if (input instanceof IStructuredSelection) {
			input = ((IStructuredSelection) input).getFirstElement();
		}
		form.setInput(input);
	}

}
