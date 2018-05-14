package net.enilink.komma.edit.ui.editor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

public interface ISupportedEditor extends IEditorPart {
	void firePropertyChange(final int propertyId);

	void setInputWithNotify(IEditorInput input);

	void setPartName(String partName);
}
