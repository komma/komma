package net.enilink.komma.edit.ui.properties.internal.context;

import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

public class PropertiesContext implements IPropertiesContext {
	CopyOnWriteArraySet<IPropertyChangeListener> listeners = new CopyOnWriteArraySet<IPropertyChangeListener>();
	boolean excludeInferred, excludeAnonymous;

	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		listeners.add(listener);
	}

	public void setExcludeInferred(boolean includeInferred) {
		if (this.excludeInferred != includeInferred) {
			boolean oldValue = this.excludeInferred;
			this.excludeInferred = includeInferred;

			firePropertyChanged(new PropertyChangeEvent(this,
					"excludeInference", oldValue, this.excludeInferred));
		}
	}

	public void setExcludeAnonymous(boolean excludeAnonymous) {
		if (this.excludeAnonymous != excludeAnonymous) {
			boolean oldValue = this.excludeAnonymous;
			this.excludeAnonymous = excludeAnonymous;

			firePropertyChanged(new PropertyChangeEvent(this,
					"excludeAnonymous", oldValue, this.excludeAnonymous));
		}
	}

	@Override
	public boolean excludeAnonymous() {
		return excludeAnonymous;
	}

	@Override
	public boolean excludeInferred() {
		return excludeInferred;
	}

	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		listeners.remove(listener);
	}

	void firePropertyChanged(PropertyChangeEvent event) {
		for (IPropertyChangeListener listener : listeners) {
			listener.propertyChange(event);
		}
	}
}
