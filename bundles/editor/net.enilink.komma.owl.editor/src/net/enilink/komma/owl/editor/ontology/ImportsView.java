package net.enilink.komma.owl.editor.ontology;

import net.enilink.komma.edit.ui.views.AbstractEditingDomainView;

public class ImportsView extends AbstractEditingDomainView {

	public ImportsView() {
		setEditPart(new ImportsPart());
	}
}