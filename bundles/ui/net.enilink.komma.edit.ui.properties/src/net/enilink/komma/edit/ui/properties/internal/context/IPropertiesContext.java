package net.enilink.komma.edit.ui.properties.internal.context;

import org.eclipse.jface.util.IPropertyChangeListener;

public interface IPropertiesContext {
	void addPropertyChangeListener(IPropertyChangeListener listener);

	void removePropertyChangeListener(IPropertyChangeListener listener);

	boolean excludeAnonymous();

	boolean excludeInferred();
}
