package net.enilink.komma.edit.ui.editor;

import org.eclipse.ui.views.properties.IPropertySheetPage;

public interface IPropertySheetPageSupport {
	IPropertySheetPage getPage();
	
	void refresh();
	
	void dispose();
}
