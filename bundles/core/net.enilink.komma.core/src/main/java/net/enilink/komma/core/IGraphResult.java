package net.enilink.komma.core;

import net.enilink.commons.iterator.IExtendedIterator;

/**
 * Represents the result of a SPARQL construct query.
 * <p>
 * The result elements are individual statements. 
 */
public interface IGraphResult extends IExtendedIterator<IStatement> {

}
