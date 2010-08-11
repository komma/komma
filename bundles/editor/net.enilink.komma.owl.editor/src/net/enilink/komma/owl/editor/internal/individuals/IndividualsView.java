package net.enilink.komma.owl.editor.internal.individuals;

import net.enilink.komma.edit.ui.views.AbstractEditingDomainView;

public class IndividualsView extends AbstractEditingDomainView {
	public IndividualsView() {
		setEditPart(new IndividualsPart());
	}
}