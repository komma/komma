package net.enilink.komma.core;

import java.util.List;

import net.enilink.commons.iterator.IExtendedIterator;

public interface ITupleResult<T> extends IExtendedIterator<T> {
	List<String> getBindingNames();
}