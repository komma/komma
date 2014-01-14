package net.enilink.komma.edit.ui.properties.internal.parts;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;

import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.properties.PropertyEditingHelper;
import net.enilink.komma.edit.properties.PropertyEditingHelper.Type;
import net.enilink.komma.edit.ui.celleditor.PropertyCellEditingSupport;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Statement;

class ValueEditingSupport extends PropertyCellEditingSupport {
	private IEditingDomain editingDomain;
	private boolean createNew;

	public ValueEditingSupport(TreeViewer viewer) {
		this(viewer, PropertyEditingHelper.Type.VALUE);
	}

	public ValueEditingSupport(final TreeViewer viewer,
			PropertyEditingHelper.Type type) {
		super(viewer, type, (type == Type.VALUE ? SWT.MULTI | SWT.WRAP
				| SWT.V_SCROLL : SWT.SINGLE));
	}

	@Override
	protected boolean canEdit(Object element) {
		if (element instanceof StatementNode
				&& ((StatementNode) element).isInverse()) {
			return false;
		}
		boolean expandedNode = element instanceof PropertyNode
				&& ((AbstractTreeViewer) getViewer()).getExpandedState(element);
		createNew = expandedNode
				|| element instanceof PropertyNode
				&& (((PropertyNode) element).isCreateNewStatementOnEdit() || ((PropertyNode) element)
						.isIncomplete());
		if (type == PropertyEditingHelper.Type.PROPERTY && expandedNode) {
			return false;
		}
		return super.canEdit(element);
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		if (createNew) {
			((PropertyNode) unwrap(element)).setCreateNewStatementOnEdit(true);
			getViewer().update(element, null);
		}
		return super.getCellEditor(element);
	}

	@Override
	protected IEditingDomain getEditingDomain() {
		return editingDomain;
	}

	public void setEditingDomain(IEditingDomain editingDomain) {
		this.editingDomain = editingDomain;
	}

	@Override
	protected IStatement getStatement(Object element) {
		if (createNew) {
			return new Statement(((PropertyNode) element).getResource(),
					((PropertyNode) element).getProperty(), null);
		}
		return ((StatementNode) element).getStatement();
	}

	@Override
	protected void editorClosed(Object element) {
		if (createNew) {
			// ensure that initial state is restored
			((PropertyNode) element).setCreateNewStatementOnEdit(false);
			getViewer().update(element, null);
		}
	}

	@Override
	protected void setProperty(Object element, IProperty property) {
		final PropertyNode node = (PropertyNode) element;
		node.setProperty(property);
		getViewer().update(node, null);
		getViewer().getControl().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				// TODO find a more generic way to
				// do this
				getViewer().editElement(node, 1);
			}
		});
		((PropertyTreeContentProvider) getViewer().getContentProvider())
				.registerPropertyNode(node);
	}

	@Override
	protected void setEditStatus(Object element, IStatus status, Object value) {
		StatementNode node = (StatementNode) element;
		node.setStatus(status);
		node.setEditorValue(status.isOK() ? null : value);
		// a new value was directly added to the property node
		if (status.isOK() && node instanceof PropertyNode) {
			((PropertyNode) node).refreshChildren();
		}
		getViewer().refresh(node);
	}

	@Override
	protected Object getValue(Object element) {
		Object value = ((StatementNode) unwrap(element)).getEditorValue();
		return value != null ? value : super.getValue(element);
	}
}
