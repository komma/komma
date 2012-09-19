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
			left = layout.createFolder("left", IPageLayout.LEFT, 0.35f,
					editorArea);
			layout.setEditorAreaVisible(false);
		}
		left.addView(OWLViews.ID_CLASSES);
		left.addView(OWLViews.ID_OBJECTPROPERTIES);
		left.addView(OWLViews.ID_DATATYPEPROPERTIES);
		left.addView(OWLViews.ID_OTHERPROPERTIES);
		left.addView(IPageLayout.ID_OUTLINE);

		IFolderLayout bottomLeft = layout.createFolder("bottomLeft",
				IPageLayout.BOTTOM, 0.50f, "left");
		bottomLeft.addView(OWLViews.ID_INSTANCES);
		bottomLeft.addView(OWLViews.ID_INSTANCETREE);
		bottomLeft.addView(OWLViews.ID_INSTANCETABLE);

		IFolderLayout bottom = layout.createFolder("bottom",
				IPageLayout.BOTTOM, 0.50f, editorArea);
		if (!CommonsUi.IS_RAP_RUNNING) {
			bottom.addView(IPageLayout.ID_PROP_SHEET);
		}
		bottom.addView(EditUIViews.ID_DETAILS);
		bottom.addView(OWLViews.ID_IMPORTS);
		bottom.addView(OWLViews.ID_NAMESPACES);

		if (CommonsUi.IS_RAP_RUNNING) {
			layout.addFastView("net.enilink.rap.workbench.modelsView");
		}
	}
}
