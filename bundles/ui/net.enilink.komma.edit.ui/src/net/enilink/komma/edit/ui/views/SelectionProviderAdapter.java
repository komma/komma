package net.enilink.komma.edit.ui.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

public class SelectionProviderAdapter implements ISelectionProvider {
	List<ISelectionChangedListener> listeners = new ArrayList<ISelectionChangedListener>();

	ISelection currentSelection = StructuredSelection.EMPTY;

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	public ISelection getSelection() {
		return currentSelection;
	}

	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		listeners.remove(listener);
	}

	public void setSelection(ISelection selection) {
		currentSelection = selection;
		final SelectionChangedEvent e = new SelectionChangedEvent(this,
				selection);
		ISelectionChangedListener[] listenersArray = listeners
				.toArray(new ISelectionChangedListener[listeners.size()]);

		for (int i = 0; i < listenersArray.length; i++) {
			final ISelectionChangedListener l = listenersArray[i];
			SafeRunner.run(new SafeRunnable() {
				public void run() {
					l.selectionChanged(e);
				}
			});
		}
	}

}