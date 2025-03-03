package net.enilink.komma.core.visitor;

import net.enilink.komma.core.IStatement;

public interface IDataVisitor<T> {
	default T visitBegin() {
		return null;
	}

	default T visitEnd() {
		return null;
	}

	T visitStatement(IStatement stmt);
}