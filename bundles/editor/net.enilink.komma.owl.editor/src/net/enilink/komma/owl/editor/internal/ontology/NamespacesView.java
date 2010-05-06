package net.enilink.komma.owl.editor.internal.ontology;

import net.enilink.komma.edit.ui.views.AbstractEditingDomainView;

public class NamespacesView extends AbstractEditingDomainView {
	public NamespacesView() {
		setEditPart(new NamespacesPart());
	}
}