package net.enilink.komma.edit.ui.editor;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

/**
 * An adapter for IPostSelectionProvider
 */
public abstract class PostSelectionProviderAdapter implements
		IPostSelectionProvider {

	/**
	 * Registered selection changed listeners (element type:
	 * <code>ISelectionChangedListener</code>).
	 */
	private ListenerList listeners = new ListenerList();

	/**
	 * Registered post selection changed listeners.
	 */
	private ListenerList postListeners = new ListenerList();

	/*
	 * (non-Javadoc) Method declared on <code>ISelectionProvider</code>.
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	/**
	 * Adds a listener for post selection changes in this multi page selection
	 * provider.
	 * 
	 * @param listener
	 *            a selection changed listener
	 */
	public void addPostSelectionChangedListener(
			ISelectionChangedListener listener) {
		postListeners.add(listener);
	}

	/**
	 * Notifies all registered selection changed listeners that the editor's
	 * selection has changed. Only listeners registered at the time this method
	 * is called are notified.
	 * 
	 * @param event
	 *            the selection changed event
	 */
	public void fireSelectionChanged(final SelectionChangedEvent event) {
		Object[] listeners = this.listeners.getListeners();
		fireEventChange(event, listeners);
	}

	/**
	 * Notifies all post selection changed listeners that the editor's selection
	 * has changed.
	 * 
	 * @param event
	 *            the event to propogate.
	 */
	public void firePostSelectionChanged(final SelectionChangedEvent event) {
		Object[] listeners = postListeners.getListeners();
		fireEventChange(event, listeners);
	}

	private void fireEventChange(final SelectionChangedEvent event,
			Object[] listeners) {
		for (int i = 0; i < listeners.length; ++i) {
			final ISelectionChangedListener l = (ISelectionChangedListener) listeners[i];
			SafeRunner.run(new SafeRunnable() {
				public void run() {
					l.selectionChanged(event);
				}
			});
		}
	}

	/*
	 * (non-JavaDoc) Method declaed on <code>ISelectionProvider</code>.
	 */
	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Removes a listener for post selection changes in this multi page
	 * selection provider.
	 * 
	 * @param listener
	 *            a selection changed listener
	 */
	public void removePostSelectionChangedListener(
			ISelectionChangedListener listener) {
		postListeners.remove(listener);
	}
}
