package net.enilink.komma.core;

import net.enilink.commons.iterator.IExtendedIterator;

/**
 * Represents the result of a SPARQL ask query.
 * <p>
 * The underlying iterator of this object does always
 * return exactly one element which is either <code>true</code>
 * or <code>false</code>.
 */
public interface IBooleanResult extends IExtendedIterator<Boolean> {
	/**
	 * Returns the result value as primitive boolean.
	 * 
	 * @return The result value
	 */
	boolean asBoolean();
}
