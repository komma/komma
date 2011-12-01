package net.enilink.komma.owl.editor;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import net.enilink.commons.ui.CommonsUi;
import net.enilink.komma.edit.ui.properties.EditUIViews;

public class OWLPerspective implements IPerspectiveFactory {
	@Override
	public void createInitialLayout(IPageLayout layout) {
		// Get the editor area.
		String editorArea = layout.getEditorArea();

		if (!CommonsUi.IS_RAP_RUNNING) {
			IFolderLayout topLeft = layout.createFolder("topLeft",
					IPageLayout.LEFT, 0.25f, editorArea);
			initTopLeftFolder(topLeft);
			// Top left: Resource Navigator view and Bookmarks view placeholder
			topLeft.addView(IPageLayout.ID_RES_NAV);
			topLeft.addPlaceholder(IPageLayout.ID_BOOKMARKS);
		}

		// Bottom left: Outline view and Property Sheet view
		IFolderLayout left;
		if (!CommonsUi.IS_RAP_RUNNING) {
			left = layout.createFolder("left", IPageLayout.BOTTOM, 0.50f,
					"topLeft");
		} else {
			left = layout.createFolder("left", IPageLayout.LEFT, 0.25f,
					editorArea);
		}
		left.addView(OWLViews.ID_CLASSES);
		left.addView(OWLViews.ID_OBJECTPROPERTIES);
		left.addView(OWLViews.ID_DATATYPEPROPERTIES);
		left.addView(OWLViews.ID_OTHERPROPERTIES);
		left.addView(IPageLayout.ID_OUTLINE);

		layout.addView(OWLViews.ID_INDIVIDUALS, IPageLayout.BOTTOM, 0.5f,
				"left");

		// Bottom right: Task List view
		IFolderLayout bottom = layout.createFolder("bottom",
				IPageLayout.BOTTOM, 0.50f, editorArea);
		bottom.addView(IPageLayout.ID_PROP_SHEET);
		bottom.addView(OWLViews.ID_IMPORTS);
		bottom.addView(OWLViews.ID_NAMESPACES);
		bottom.addView(EditUIViews.ID_DETAILS);
	}

	protected void initTopLeftFolder(IFolderLayout topLeft) {
		if (!CommonsUi.IS_RAP_RUNNING) {
			// Top left: Resource Navigator view and Bookmarks view placeholder
			topLeft.addView(IPageLayout.ID_RES_NAV);
			topLeft.addPlaceholder(IPageLayout.ID_BOOKMARKS);
		}
	}
}
