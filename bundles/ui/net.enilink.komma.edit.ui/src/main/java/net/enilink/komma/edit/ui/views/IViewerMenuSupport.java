package net.enilink.komma.edit.ui.views;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPartSite;

public interface IViewerMenuSupport {
	void createContextMenuFor(StructuredViewer viewer, Control menuParent, IWorkbenchPartSite partSite);
}
