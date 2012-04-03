package net.enilink.komma.model.base;

import net.enilink.komma.common.util.WrappedException;
import net.enilink.komma.model.IModel;

class DiagnosticWrappedException extends WrappedException implements
		IModel.IDiagnostic {
	private static final long serialVersionUID = 1L;

	private String location;

	public DiagnosticWrappedException(String location, Exception exception) {
		super(exception);
		this.location = location;
	}

	public int getColumn() {
		return 0;
	}

	public int getLine() {
		return 0;
	}

	public String getLocation() {
		return location;
	}
}
