package net.enilink.komma.core.visitor;

import net.enilink.komma.core.IStatement;

public interface IDataVisitor<T> {
	T visitBegin();

	T visitEnd();

	T visitStatement(IStatement stmt);
}