package net.enilink.komma.edit.ui.rcp.editor;

import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import net.enilink.komma.edit.ui.editor.IPropertySheetPageSupport;

public class TabbedPropertySheetPageSupport implements
		IPropertySheetPageSupport, ITabbedPropertySheetPageContributor {
	private String contributorId;
	private TabbedPropertySheetPage propertySheetPage;

	public TabbedPropertySheetPageSupport() {
		this("net.enilink.komma.edit.ui");
	}

	public TabbedPropertySheetPageSupport(String contributorId) {
		this.contributorId = contributorId;
	}

	@Override
	public void dispose() {
		if (propertySheetPage != null) {
			propertySheetPage.dispose();
			propertySheetPage = null;
		}
	}

	@Override
	public String getContributorId() {
		return contributorId;
	}

	@Override
	public IPropertySheetPage getPage() {
		if (propertySheetPage == null
				|| propertySheetPage.getControl().isDisposed()) {
			propertySheetPage = new TabbedPropertySheetPage(this);
		}
		return propertySheetPage;
	}

	@Override
	public void refresh() {
		if (propertySheetPage != null
				&& !propertySheetPage.getControl().isDisposed()
				&& propertySheetPage.getCurrentTab() != null) {
			propertySheetPage.refresh();
		}
	}
}
