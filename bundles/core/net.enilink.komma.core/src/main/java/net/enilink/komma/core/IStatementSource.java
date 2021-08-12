package net.enilink.komma.core;

import net.enilink.commons.iterator.IExtendedIterator;

/**
 * Interface for sources that contain (potentially also inferred) statements.
 */
public interface IStatementSource {
	/**
	 * Returns <code>true</code> if at least one statement exists with the given
	 * subject, predicate, and object. Null parameters represent wildcards.
	 *
	 * @param subject         the subject to match, or null for a wildcard
	 * @param predicate       the predicate to match, or null for a wildcard
	 * @param object          the object to match, or null for a wildcard
	 * @param includeInferred Controls if inferred statements should be included to compute
	 *                        the results or not.
	 * @param contexts        The context(s) where to get the data.
	 * @return <code>true</code> if at least one matching statement exists, else
	 * <code>false</code>.
	 * @throws KommaException thrown if there is an error while getting the statements
	 */
	boolean hasMatch(IReference subject, IReference predicate, IValue object,
					 boolean includeInferred, IReference... contexts);

	/**
	 * Returns all the statements with the given subject, predicate, and object.
	 * Null parameters represent wildcards.
	 *
	 * @param subject         the subject to match, or null for a wildcard
	 * @param predicate       the predicate to match, or null for a wildcard
	 * @param object          the object to match, or null for a wildcard
	 * @param includeInferred Controls if inferred statements should be included to compute
	 *                        the results or not.
	 * @param contexts        The context(s) where to get the data.
	 * @return an {@link IExtendedIterator} of matching statements.
	 * @throws KommaException thrown if there is an error while getting the statements
	 */
	IExtendedIterator<IStatement> match(IReference subject,
										IReference predicate, IValue object, boolean includeInferred,
										IReference... contexts);
}
