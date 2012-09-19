package net.enilink.komma.owl.editor.internal.instances;

import net.enilink.komma.edit.ui.views.AbstractEditingDomainView;

public class InstanceTableView extends AbstractEditingDomainView {
	public InstanceTableView() {
		setEditPart(new InstanceTablePart());
	}
}