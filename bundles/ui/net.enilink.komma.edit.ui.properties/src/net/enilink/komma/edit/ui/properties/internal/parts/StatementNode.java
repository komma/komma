package net.enilink.komma.edit.ui.properties.internal.parts;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;

public abstract class StatementNode {
	protected boolean inverse;
	protected IStatus status = Status.OK_STATUS;
	protected Object editorValue;

	public StatementNode(boolean inverse) {
		this.inverse = inverse;
	}

	public Object getEditorValue() {
		return editorValue;
	}

	public void setEditorValue(Object editorValue) {
		this.editorValue = editorValue;
	}

	public IReference getResource() {
		if (getStatement() == null) {
			return null;
		}
		return inverse ? (IReference) getStatement().getObject()
				: getStatement().getSubject();
	}

	public abstract IStatement getStatement();

	public Object getValue() {
		if (getStatement() == null) {
			return null;
		}
		return inverse ? (IReference) getStatement().getSubject()
				: getStatement().getObject();
	}

	public boolean isInverse() {
		return inverse;
	}

	public IStatus getStatus() {
		return status;
	}

	public void setStatus(IStatus status) {
		this.status = status;
	}
}