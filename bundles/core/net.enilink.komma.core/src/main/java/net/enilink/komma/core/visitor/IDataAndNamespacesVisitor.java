package net.enilink.komma.core.visitor;

import net.enilink.komma.core.INamespace;

public interface IDataAndNamespacesVisitor<T> extends IDataVisitor<T> {
	T visitNamespace(INamespace namespace);
}
