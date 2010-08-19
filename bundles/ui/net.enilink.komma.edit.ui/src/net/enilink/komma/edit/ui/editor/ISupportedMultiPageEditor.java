package net.enilink.komma.edit.ui.editor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;

public interface ISupportedMultiPageEditor extends ISupportedEditor {
	Composite getContainer();

	int getPageCount();

	void setPageText(int pageIndex, String text);

	IEditorPart getEditor(int pageIndex);

	void setActivePage(int pageIndex);
}
