package net.enilink.komma.edit.ui.editor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

public interface ISupportedEditor extends IEditorPart {
	Composite getContainer();

	int getPageCount();

	void setPageText(int pageIndex, String text);

	void firePropertyChange(final int propertyId);

	void setInputWithNotify(IEditorInput input);

	void setPartName(String partName);

	IEditorPart getEditor(int pageIndex);

	void setActivePage(int pageIndex);
}
