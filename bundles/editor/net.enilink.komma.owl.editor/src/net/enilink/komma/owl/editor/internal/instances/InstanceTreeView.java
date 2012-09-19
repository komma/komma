package net.enilink.komma.owl.editor.internal.instances;

import net.enilink.komma.edit.ui.views.AbstractEditingDomainView;

public class InstanceTreeView extends AbstractEditingDomainView {
	public InstanceTreeView() {
		setEditPart(new InstanceTreePart());
	}
}