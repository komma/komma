package net.enilink.komma.dm;

import java.util.Set;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.InferencingCapability;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.URI;

/**
 * Interface to an RDF data manager providing a set of methods to query and
 * modify the underlying data.
 */
public interface IDataManager {
	/**
	 * Add statements to this data manager
	 * 
	 * @param statements
	 *            the statements to add
	 * @throws KommaException
	 *             thrown if there is an error while adding the statements
	 */
	IDataManager add(Iterable<? extends IStatement> statements);

	/**
	 * Removes all namespace declarations from this manager.
	 */
	IDataManager clearNamespaces();

	/**
	 * Closes this data manager and all open connections. All subsequent
	 * operations on a disconnected data source will throw
	 * IllegalStateExceptions.
	 */
	void close();

	/**
	 * Creates an IKommaQuery to evaluate the query string.
	 * 
	 * @param query
	 *            rdf query in the configured language - default SPARQL.
	 * @param baseURI
	 *            base URI for relative URIs or <code>null</code> if the query
	 *            does not contain relative URIs
	 * @return {@link IQuery}.
	 */
	<R> IDataManagerQuery<R> createQuery(String query, String baseURI);

	/**
	 * Returns all asserted statements with the given subject, predicate, and
	 * object. Null parameters represent wildcards.
	 * 
	 * @param subject
	 *            the subject to match, or null for a wildcard
	 * @param predicate
	 *            the predicate to match, or null for a wildcard
	 * @param object
	 *            the object to match, or null for a wildcard
	 * @return an {@link IExtendedIterator} of matching statements.
	 * @throws KommaException
	 *             thrown if there is an error while getting the statements
	 */
	IExtendedIterator<IStatement> matchAsserted(IReference subject,
			IReference predicate, IValue object);

	/**
	 * Return the inferencing capability of the underlying store.
	 * 
	 * @return {@link InferencingCapability} The inferencing capability.
	 */
	InferencingCapability getInferencing();

	/**
	 * Gets the namespace that is associated with the specified prefix, if any.
	 * 
	 * @param prefix
	 *            A namespace prefix.
	 * @return The namespace name that is associated with the specified prefix,
	 *         or <tt>null</tt> if there is no such namespace.
	 */
	URI getNamespace(String prefix);

	/**
	 * Returns an iterator of all declared namespaces. Each Namespace object
	 * consists of a prefix and a namespace uri.
	 * 
	 * @return An iterator containing {@link INamespace} objects.
	 */
	IExtendedIterator<INamespace> getNamespaces();

	/**
	 * Returns the resource-level transaction object. The {@link ITransaction}
	 * instance may be used serially to begin and commit multiple transactions.
	 * 
	 * @return IKommaTransaction instance
	 */
	ITransaction getTransaction();

	/**
	 * Returns <code>true</code> if at least one statement exists with the given
	 * subject, predicate, and object. Null parameters represent wildcards.
	 * 
	 * @param subject
	 *            the subject to match, or null for a wildcard
	 * @param predicate
	 *            the predicate to match, or null for a wildcard
	 * @param object
	 *            the object to match, or null for a wildcard
	 * @return <code>true</code> if at least one matching statement exists, else
	 *         <code>false</code>.
	 * @throws KommaException
	 *             thrown if there is an error while getting the statements
	 */
	boolean hasMatch(IReference subject, IReference predicate, IValue object);

	/**
	 * Returns whether or not there is an open connection to this data source
	 * 
	 * @return true if there is a connection, false otherwise.
	 */
	boolean isOpen();

	/**
	 * Returns all the statements with the given subject, predicate, and object.
	 * Null parameters represent wildcards.
	 * 
	 * @param subject
	 *            the subject to match, or null for a wildcard
	 * @param predicate
	 *            the predicate to match, or null for a wildcard
	 * @param object
	 *            the object to match, or null for a wildcard
	 * @return an {@link IExtendedIterator} of matching statements.
	 * @throws KommaException
	 *             thrown if there is an error while getting the statements
	 */
	IExtendedIterator<IStatement> match(IReference subject,
			IReference predicate, IValue object);

	/**
	 * Returns a new data manager specific blank node reference.
	 */
	IReference newBlankNode();

	/**
	 * Remove statements from this data manager
	 * 
	 * @param statement
	 *            the statement to remove
	 * @throws KommaException
	 *             thrown if there is an error while removing the statements
	 */
	IDataManager remove(Iterable<? extends IStatement> statements);

	/**
	 * Removes a namespace declaration by removing the association between a
	 * prefix and a namespace name.
	 * 
	 * @param prefix
	 *            The namespace prefix of which the assocation with a namespace
	 *            name is to be removed.
	 */
	IDataManager removeNamespace(String prefix);

	/**
	 * Specifies a set of contexts which are used for adding and removing statements.
	 * 
	 * @param modifyContexts
	 */
	IDataManager setModifyContexts(Set<URI> modifyContexts);

	/**
	 * Sets the prefix for a namespace.
	 * 
	 * @param prefix
	 *            The new prefix.
	 * @param name
	 *            The namespace name that the prefix maps to.
	 */
	IDataManager setNamespace(String prefix, URI uri);

	/**
	 * Specifies a set of contexts which are used for reading statements.
	 * 
	 * @param readContexts
	 */
	IDataManager setReadContexts(Set<URI> readContexts);

	IDataManager setIncludeInferred(boolean includeInferred);

	boolean getIncludeInferred();
}
