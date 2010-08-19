package net.enilink.komma.edit.ui.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;

import net.enilink.komma.common.ui.editor.ProblemEditorPart;
import net.enilink.komma.common.util.BasicDiagnostic;
import net.enilink.komma.common.util.Diagnostic;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;

public abstract class KommaMultiPageEditorSupport<E extends MultiPageEditorPart & ISupportedMultiPageEditor>
		extends KommaEditorSupport<E> {
	public KommaMultiPageEditorSupport(E editor) {
		super(editor);
	}

	/**
	 * If there is just one page in the multi-page editor part, this hides the
	 * single tab at the bottom.
	 */
	public void hideTabs() {
		if (editor.getPageCount() <= 1) {
			editor.setPageText(0, "");
			if (editor.getContainer() instanceof CTabFolder) {
				((CTabFolder) editor.getContainer()).setTabHeight(1);
				Point point = editor.getContainer().getSize();
				editor.getContainer().setSize(point.x, point.y + 6);
			}
		}
	}

	/**
	 * If there is more than one page in the multi-page editor part, this shows
	 * the tabs at the bottom.
	 * 
	 */
	public void showTabs() {
		if (editor.getPageCount() > 1) {
			editor.setPageText(0, getString("_UI_SelectionPage_label"));
			if (editor.getContainer() instanceof CTabFolder) {
				((CTabFolder) editor.getContainer()).setTabHeight(SWT.DEFAULT);
				Point point = editor.getContainer().getSize();
				editor.getContainer().setSize(point.x, point.y - 6);
			}
		}
	}

	/**
	 * Updates the problems indication with the information described in the
	 * specified diagnostic.
	 * 
	 */
	public void updateProblemIndication() {
		if (updateProblemIndication) {
			BasicDiagnostic diagnostic = new BasicDiagnostic(Diagnostic.OK,
					"net.enilink.komma.edit.ui.editor", 0, null,
					new Object[] { getEditingDomain().getModelSet() });
			for (Diagnostic childDiagnostic : modelToDiagnosticMap.values()) {
				if (childDiagnostic.getSeverity() != Diagnostic.OK) {
					diagnostic.add(childDiagnostic);
				}
			}

			int lastEditorPage = editor.getPageCount() - 1;
			if (lastEditorPage >= 0
					&& editor.getEditor(lastEditorPage) instanceof ProblemEditorPart) {
				((ProblemEditorPart) editor.getEditor(lastEditorPage))
						.setDiagnostic(diagnostic);
				if (diagnostic.getSeverity() != Diagnostic.OK) {
					editor.setActivePage(lastEditorPage);
				}
			} else if (diagnostic.getSeverity() != Diagnostic.OK) {
				ProblemEditorPart problemEditorPart = new ProblemEditorPart();
				problemEditorPart.setDiagnostic(diagnostic);
				problemEditorPart.setMarkerHelper(markerHelper);
				try {
					editor.addPage(++lastEditorPage, problemEditorPart,
							editor.getEditorInput());
					editor.setPageText(lastEditorPage,
							problemEditorPart.getPartName());
					editor.setActivePage(lastEditorPage);
					showTabs();
				} catch (PartInitException exception) {
					KommaEditUIPlugin.INSTANCE.log(exception);
				}
			}

			if (markerHelper.hasMarkers(getEditingDomain().getModelSet())) {
				markerHelper.deleteMarkers(getEditingDomain().getModelSet());
				if (diagnostic.getSeverity() != Diagnostic.OK) {
					try {
						markerHelper.createMarkers(diagnostic);
					} catch (CoreException exception) {
						KommaEditUIPlugin.INSTANCE.log(exception);
					}
				}
			}
		}
	}
}
