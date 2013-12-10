package net.enilink.komma.owl.editor.instances;

import net.enilink.komma.edit.ui.views.AbstractEditingDomainView;

public class InstancesView extends AbstractEditingDomainView {
	public InstancesView() {
		setEditPart(new InstancesPart());
	}
}