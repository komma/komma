package net.enilink.komma.edit.ui.editor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

public interface ISupportedMultiPageEditor extends ISupportedEditor {
	void addPage(int index, IEditorPart editor, IEditorInput input) throws PartInitException;
	
	Composite getContainer();

	int getPageCount();

	void setPageText(int pageIndex, String text);

	IEditorPart getEditor(int pageIndex);

	void setActivePage(int pageIndex);
}
