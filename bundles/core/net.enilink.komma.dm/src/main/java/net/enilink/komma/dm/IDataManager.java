package net.enilink.komma.dm;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.*;

/**
 * Interface to an RDF data manager providing a set of methods to query and
 * modify the underlying data.
 */
public interface IDataManager extends IStatementSource, AutoCloseable {
	/**
	 * Add statements to this data manager
	 * 
	 * @param statements
	 *            The statements to add.
	 * @param readContexts
	 *            The context(s) where to check if data is already existing
	 *            before adding.
	 * @param addContexts
	 *            The context(s) where to add the data.
	 * @throws KommaException
	 *             thrown if there is an error while adding the statements
	 */
	IDataManager add(Iterable<? extends IStatement> statements,
			IReference[] readContexts, IReference... addContexts);

	/**
	 * Add statements to this data manager
	 * 
	 * @param statements
	 *            The statements to add.
	 * @param contexts
	 *            The context(s) where to add the data.
	 * @throws KommaException
	 *             thrown if there is an error while adding the statements
	 */
	IDataManager add(Iterable<? extends IStatement> statements,
			IReference... contexts);

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
	 * Creates an {@link IDataManagerQuery} to evaluate the query string.
	 * 
	 * @param query
	 *            RDF query in the configured language - default SPARQL.
	 * @param baseURI
	 *            base URI for relative URIs or <code>null</code> if the query
	 *            does not contain relative URIs
	 * @param includeInferred
	 *            Controls if inferred statements should be included to compute
	 *            the results or not.
	 * @param contexts
	 *            The context(s) where to read and modify data.
	 * @return {@link IDataManagerQuery}.
	 */
	<R> IDataManagerQuery<R> createQuery(String query, String baseURI,
			boolean includeInferred, IReference... contexts);

	/**
	 * Creates an {@link IDataManagerUpdate} to evaluate the update string.
	 * 
	 * @param update
	 *            RDF update in the configured language - default SPARQL.
	 * @param baseURI
	 *            base URI for relative URIs or <code>null</code> if the update
	 *            does not contain relative URIs
	 * @param includeInferred
	 *            Controls if inferred statements should be included to compute
	 *            the results or not.
	 * @param readContexts
	 *            The context(s) where to read existing data.
	 * @param modifyContexts
	 *            The context(s) where to modify the data.
	 * @return {@link IDataManagerUpdate}.
	 */
	IDataManagerUpdate createUpdate(String update, String baseURI,
			boolean includeInferred, IReference[] readContexts,
			IReference... modifyContexts);

	/**
	 * Creates an {@link IDataManagerUpdate} to evaluate the update string.
	 * 
	 * @param update
	 *            RDF update in the configured language - default SPARQL.
	 * @param baseURI
	 *            base URI for relative URIs or <code>null</code> if the update
	 *            does not contain relative URIs
	 * @param includeInferred
	 *            Controls if inferred statements should be included to compute
	 *            the results or not.
	 * @param contexts
	 *            The context(s) where to get or modify the data.
	 * @return {@link IDataManagerUpdate}.
	 */
	IDataManagerUpdate createUpdate(String update, String baseURI,
			boolean includeInferred, IReference... contexts);

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
	 * Returns whether or not there is an open connection to this data source
	 * 
	 * @return true if there is a connection, false otherwise.
	 */
	boolean isOpen();

	/**
	 * Returns a new data manager specific blank node reference.
	 */
	IReference blankNode();

	/**
	 * Returns a data manager specific blank node reference with the given
	 * <code>id</code>.
	 */
	IReference blankNode(String id);

	/**
	 * Remove statements from this data manager
	 * 
	 * @param statements
	 *            The statements to remove.
	 * @param contexts
	 *            The context(s) where to remove the data.
	 * @throws KommaException
	 *             thrown if there is an error while removing the statements
	 */
	IDataManager remove(Iterable<? extends IStatementPattern> statements,
			IReference... contexts);

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
	 * Sets the prefix for a namespace.
	 * 
	 * @param prefix
	 *            The new prefix.
	 * @param name
	 *            The namespace name that the prefix maps to.
	 */
	IDataManager setNamespace(String prefix, URI uri);
}
