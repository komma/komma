/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.core;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Refreshable;
import javax.transaction.TransactionRequiredException;

import net.enilink.commons.iterator.IExtendedIterator;

public interface IKommaManager {
	/**
	 * Registers an {@link IEntityDecorator decorator}
	 * 
	 * @param decorator
	 *            the decorator
	 */
	void addDecorator(IEntityDecorator decorator);

	/**
	 * Add statements to this manager
	 * 
	 * @param statements
	 *            the statements to add
	 * @throws KommaException
	 *             thrown if there is an error while adding the statements
	 */
	void add(Iterable<? extends IStatement> statements);

	/**
	 * Clear the persistence context, causing all managed entities to become
	 * detached. Changes made to entities that have not been flushed to the
	 * database will not be persisted.
	 */
	void clear();

	/**
	 * Removes all namespace declarations from this manager.
	 */
	void clearNamespaces();

	/**
	 * Closes any transactions or connections in the manager.
	 * 
	 */
	void close();

	/**
	 * Close an Iterator created by iterate() immediately, instead of waiting
	 * until the iteration is complete or connection is closed.
	 * 
	 */
	void close(Iterator<?> iter);

	/**
	 * Check if the instance belongs to the current persistence context.
	 * 
	 * @param entity
	 * @return <code>true</code> if the instance belongs to the current
	 *         persistence context.
	 */
	boolean contains(Object entity);

	/**
	 * Returns the literal's value converted to a Java type.
	 * 
	 * @return the literal's value
	 */
	Object convertValue(ILiteral literal);

	/**
	 * Assigns <code>type</code> to a new anonymous entity.
	 * 
	 * @param concept
	 *            interface to be translated to rdf:type.
	 * @param concepts
	 *            additional interfaces to be translated to rdf:type.
	 * @return Java Bean representing the subject.
	 */
	<T> T create(Class<T> concept, Class<?>... concepts);

	/**
	 * Assigns <code>type</code> to a new anonymous entity.
	 * 
	 * @param concepts
	 *            the resources to be used for rdf:type.
	 * @return Java Bean representing the subject.
	 */
	IEntity create(IReference... concepts);

	/**
	 * Assigns <code>concept</code> to the named entity subject.
	 * 
	 * @param uri
	 *            URI of the entity.
	 * @param concept
	 *            interface to be translated to rdf:type.
	 * @param concepts
	 *            additional interfaces to be translated to rdf:type.
	 * 
	 * @return Java Bean representing the subject.
	 */
	<T> T createNamed(URI uri, Class<T> concept, Class<?>... concepts);

	/**
	 * Assigns <code>concept</code> to the named entity subject.
	 * 
	 * @param uri
	 *            URI of the entity.
	 * @param concepts
	 *            the resources to be used for rdf:type.
	 * 
	 * @return Java Bean representing the subject.
	 */
	IEntity createNamed(URI uri, IReference... concepts);

	/**
	 * Creates an ILiteral to hold a literal (value, type, language).
	 * 
	 * @param value
	 *            the literal's value.
	 * @param datatype
	 *            the literal's datatype.
	 * @param language
	 *            the literal's language.
	 * 
	 * @return Java Bean representing the literal.
	 */
	ILiteral createLiteral(Object value, URI datatype, String language);

	/**
	 * Creates an IKommaQuery to evaluate the query string.
	 * 
	 * @param query
	 *            rdf query in the configured language - default SPARQL.
	 * @return {@link IQuery}.
	 */
	IQuery<?> createQuery(String query);

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
	IQuery<?> createQuery(String query, String baseURI);

	/**
	 * Assigns <code>concept</code> to the given entity and return a new object
	 * reference that implements the given <code>concept</code>.
	 * 
	 * @param <T>
	 * @param entity
	 *            An existing entity retrieved from this manager.
	 * @param concept
	 *            interface to be translated to rdf:type.
	 * @param concepts
	 *            additional interfaces to be translated to rdf:type.
	 * @return Java Bean representing <code>entity</code> that implements
	 *         <code>concept</code>.
	 */
	<T> T designateEntity(Object entity, Class<T> concept, Class<?>... concepts);

	/**
	 * Retrieves the rdf:type, creates a Java Bean class and instantiates it.
	 * 
	 * @param reference
	 *            reference to the entity.
	 * @return JavaBean representing the subject.
	 */
	IEntity find(IReference reference);

	/**
	 * Retrieves the rdf:type, creates a Java Bean class by incorporating the
	 * given concepts and instantiates it.
	 * 
	 * @param uri
	 *            URI of the entity.
	 * @return JavaBean representing the subject.
	 */
	<T> T find(URI uri, Class<T> concept, Class<?>... concepts);

	/**
	 * Creates an iteration of entities that implement this <code>role</code>.
	 * 
	 * @param role
	 *            concept or behaviour to be translated to one or more
	 *            rdf:types.
	 * @return IExtendedIterator entities that implement role.
	 */
	<T> IExtendedIterator<T> findAll(Class<T> role);

	/**
	 * Creates a Java Bean class without inserting any statements.
	 * 
	 * @param uri
	 *            URI of the entity.
	 * @return JavaBean representing the subject.
	 */
	<T> T findRestricted(URI uri, Class<T> concept, Class<?>... concepts);

	/**
	 * Synchronize the persistence context to the underlying database.
	 * 
	 * @throws TransactionRequiredException
	 *             if there is no transaction
	 * @throws PersistenceException
	 *             if the flush fails
	 */
	void flush();

	/**
	 * Return the factory for the entity manager.
	 * 
	 * @return IKommaManagerFactory instance
	 * @throws IllegalStateException
	 *             if the entity manager has been closed.
	 */
	IKommaManagerFactory getFactory();

	/**
	 * Get the flush mode that applies to all objects contained in the
	 * persistence context.
	 * 
	 * @return flushMode
	 */
	FlushModeType getFlushMode();

	/**
	 * Return the inferencing capability of the underlying store.
	 * 
	 * @return {@link InferencingCapability} The inferencing capability.
	 */
	InferencingCapability getInferencing();

	/**
	 * Locale this manager was created with.
	 * 
	 * @return Locale or null
	 */
	Locale getLocale();

	/**
	 * Get the current lock mode for the entity instance.
	 * 
	 * @param entity
	 * @return lock mode
	 * @throws TransactionRequiredException
	 *             if there is no transaction
	 * @throws IllegalArgumentException
	 *             if the instance is not a managed entity and a transaction is
	 *             active
	 */
	LockModeType getLockMode(Object entity);

	/**
	 * Return the entity manager factory for the entity manager.
	 * 
	 * @return IKommaManagerFactory instance
	 * @throws IllegalStateException
	 *             if the entity manager has been closed.
	 */
	IKommaManagerFactory getManagerFactory();

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
	 * Gets the prefix that is associated with the specified namespace uri, if
	 * any.
	 * 
	 * @param namespace
	 *            A namespace uri.
	 * @return The prefix that is associated with the specified namespace uri,
	 *         or <tt>null</tt> if there is no such prefix.
	 */
	String getPrefix(URI namespace);

	/**
	 * Returns an iterator of all declared namespaces. Each Namespace object
	 * consists of a prefix and a namespace uri.
	 * 
	 * @return An iterator containing {@link INamespace} objects.
	 */
	IExtendedIterator<INamespace> getNamespaces();

	/**
	 * Get the properties and associated values that are in effect for the
	 * entity manager. Changing the contents of the map does not change the
	 * configuration in effect.
	 */
	Map<String, Object> getProperties();

	/**
	 * Get the names of the properties that are supported for use with the
	 * entity manager. These correspond to properties and hints that may be
	 * passed to the methods of the EntityManager interface that take a
	 * properties argument or used with the PersistenceContext annotation. These
	 * properties include all standard entity manager hints and properties as
	 * well as vendor-specific ones supported by the provider. These properties
	 * may or may not currently be in effect.
	 * 
	 * @return property names
	 */
	Set<String> getSupportedProperties();

	/**
	 * Returns the resource-level transaction object. The EntityTransaction
	 * instance may be used serially to begin and commit multiple transactions.
	 * 
	 * @return EntityTransaction instance
	 */
	IKommaTransaction getTransaction();

	/**
	 * Returns {@code true} if the {@link IEntityDecorator decorator} is already
	 * registered
	 * 
	 * @param decorator
	 *            the decorator
	 */
	boolean hasDecorator(IEntityDecorator decorator);

	/**
	 * If this manager currently has an open connection to the repository.
	 * 
	 * @return <code>true</code> if the connection is open.
	 */
	boolean isOpen();

	/**
	 * Indicate to the EntityManager that a transaction is active. This method
	 * should be called on an application managed EntityManager that was created
	 * outside the scope of the active transaction to associate it with the
	 * current transaction.
	 * 
	 * @throws TransactionRequiredException
	 *             if there is no transaction.
	 */
	void joinTransaction();

	/**
	 * Lock an entity instance that is contained in the persistence context with
	 * the specified lock mode type. If a pessimistic lock mode type is
	 * specified and the entity contains a version attribute, the persistence
	 * provider must also perform optimistic version checks when obtaining the
	 * database lock. If these checks fail, the OptimisticLockException will be
	 * thrown. If the lock mode type is pessimistic and the entity instance is
	 * found but cannot be locked: - the PessimisticLockException will be thrown
	 * if the database locking failure causes transaction-level rollback. - the
	 * LockTimeoutException will be thrown if the database locking failure
	 * causes only statement-level rollback
	 * 
	 * @param entity
	 * @param lockMode
	 * @throws IllegalArgumentException
	 *             if the instance is not an entity or is a detached entity
	 * @throws TransactionRequiredException
	 *             if there is no transaction
	 * @throws EntityNotFoundException
	 *             if the entity does not exist in the database when pessimistic
	 *             locking is performed
	 * @throws OptimisticLockException
	 *             if the optimistic version check fails
	 * @throws PessimisticLockException
	 *             if pessimistic locking fails and the transaction is rolled
	 *             back
	 * @throws LockTimeoutException
	 *             if pessimistic locking fails and only the statement is rolled
	 *             back
	 * @throws PersistenceException
	 *             if an unsupported lock call is made
	 */
	void lock(Object entity, LockModeType lockMode);

	/**
	 * Lock an entity instance that is contained in the persistence context with
	 * the specified lock mode type and with specified properties. If a
	 * pessimistic lock mode type is specified and the entity contains a version
	 * attribute, the persistence provider must also perform optimistic version
	 * checks when obtaining the database lock. If these checks fail, the
	 * OptimisticLockException will be thrown. If the lock mode type is
	 * pessimistic and the entity instance is found but cannot be locked: - the
	 * PessimisticLockException will be thrown if the database locking failure
	 * causes transaction-level rollback. - the LockTimeoutException will be
	 * thrown if the database locking failure causes only statement-level
	 * rollback If a vendor-specific property or hint is not recognized, it is
	 * silently ignored. Portable applications should not rely on the standard
	 * timeout hint. Depending on the database in use and the locking mechanisms
	 * used by the provider, the hint may or may not be observed.
	 * 
	 * @param entity
	 * @param lockMode
	 * @param properties
	 *            standard and vendor-specific properties and hints
	 * @throws IllegalArgumentException
	 *             if the instance is not an entity or is a detached entity
	 * @throws TransactionRequiredException
	 *             if there is no transaction
	 * @throws EntityNotFoundException
	 *             if the entity does not exist in the database when pessimistic
	 *             locking is performed
	 * @throws OptimisticLockException
	 *             if the optimistic version check fails
	 * @throws PessimisticLockException
	 *             if pessimistic locking fails and the transaction is rolled
	 *             back
	 * @throws LockTimeoutException
	 *             if pessimistic locking fails and only the statement is rolled
	 *             back
	 * @throws PersistenceException
	 *             if an unsupported lock call is made
	 */
	void lock(Object entity, LockModeType lockMode,
			Map<String, Object> properties);

	/**
	 * Copies all non-null values from bean into an entity managed by this
	 * manager. If <code>bean</code> implements {@link IEntity} its URI will be
	 * used to look up the managed entity, otherwise a new anonymous entity will
	 * be created.
	 * 
	 * @param <T>
	 * @param bean
	 *            with values that should be merged
	 * @return managed entity it was merged with
	 */
	<T> T merge(T bean);

	/**
	 * Make an instance managed and persistent.
	 * 
	 * @param entity
	 * @throws EntityExistsException
	 *             if the entity already exists. (If the entity already exists,
	 *             the EntityExistsException may be thrown when the persist
	 *             operation is invoked, or the EntityExistsException or another
	 *             PersistenceException may be thrown at flush or commit time.)
	 * @throws IllegalArgumentException
	 *             if the instance is not an entity
	 * @throws TransactionRequiredException
	 *             if invoked on a container-managed entity manager of type
	 *             PersistenceContextType.TRANSACTION and there is no
	 *             transaction.
	 */
	void persist(Object entity);

	/**
	 * If <code>entity</code> implements Refreshable, its method
	 * {@link Refreshable#refresh()} will be called. This call instructs
	 * entities that their property values may have changed and they should
	 * reload them as needed.
	 * 
	 * @param entity
	 */
	void refresh(Object entity);

	/**
	 * Refresh the state of the instance from the database, overwriting changes
	 * made to the entity, if any, and lock it with respect to given lock mode
	 * type. If the lock mode type is pessimistic and the entity instance is
	 * found but cannot be locked: - the PessimisticLockException will be thrown
	 * if the database locking failure causes transaction-level rollback. - the
	 * LockTimeoutException will be thrown if the database locking failure
	 * causes only statement-level rollback.
	 * 
	 * @param entity
	 * @param lockMode
	 * @throws IllegalArgumentException
	 *             if the instance is not an entity or the entity is not managed
	 * @throws TransactionRequiredException
	 *             if there is no transaction
	 * @throws EntityNotFoundException
	 *             if the entity no longer exists in the database
	 * @throws PessimisticLockException
	 *             if pessimistic locking fails and the transaction is rolled
	 *             back
	 * @throws LockTimeoutException
	 *             if pessimistic locking fails and only the statement is rolled
	 *             back
	 * @throws PersistenceException
	 *             if an unsupported lock call is made
	 */
	void refresh(Object entity, LockModeType lockMode);

	/**
	 * Refresh the state of the instance from the database, overwriting changes
	 * made to the entity, if any, and lock it with respect to given lock mode
	 * type and with specified properties. If the lock mode type is pessimistic
	 * and the entity instance is found but cannot be locked: - the
	 * PessimisticLockException will be thrown if the database locking failure
	 * causes transaction-level rollback. - the LockTimeoutException will be
	 * thrown if the database locking failure causes only statement-level
	 * rollback If a vendor-specific property or hint is not recognized, it is
	 * silently ignored. Portable applications should not rely on the standard
	 * timeout hint. Depending on the database in use and the locking mechanisms
	 * used by the provider, the hint may or may not be observed.
	 * 
	 * @param entity
	 * @param lockMode
	 * @param properties
	 *            standard and vendor-specific properties and hints
	 * @throws IllegalArgumentException
	 *             if the instance is not an entity or the entity is not managed
	 * @throws TransactionRequiredException
	 *             if there is no transaction
	 * @throws EntityNotFoundException
	 *             if the entity no longer exists in the database
	 * @throws PessimisticLockException
	 *             if pessimistic locking fails and the transaction is rolled
	 *             back
	 * @throws LockTimeoutException
	 *             if pessimistic locking fails and only the statement is rolled
	 *             back
	 * @throws PersistenceException
	 *             if an unsupported lock call is made
	 */
	void refresh(Object entity, LockModeType lockMode,
			Map<String, Object> properties);

	/**
	 * Refresh the state of the instance from the database, using the specified
	 * properties, and overwriting changes made to the entity, if any. If a
	 * vendor-specific property or hint is not recognized, it is silently
	 * ignored.
	 * 
	 * @param entity
	 * @param properties
	 *            standard and vendor-specific properties
	 * @throws IllegalArgumentException
	 *             if the instance is not an entity or the entity is not managed
	 * @throws TransactionRequiredException
	 *             if invoked on a container-managed entity manager of type
	 *             PersistenceContextType.TRANSACTION and there is no
	 *             transaction.
	 * @throws EntityNotFoundException
	 *             if the entity no longer exists in the database
	 */
	void refresh(Object entity, Map<String, Object> properties);

	/**
	 * Removes the given entity or subject and all implementing roles. It is the
	 * responsibility of the caller to ensure this <code>entity</code> or any
	 * other object referencing it are no longer used and any object that may
	 * have cached a value containing this is refreshed.
	 * 
	 * @param entity
	 *            to be removed from the pool and repository.
	 */
	void remove(Object entity);

	/**
	 * Remove statements from this manager
	 * 
	 * @param statement
	 *            the statement to remove
	 * @throws KommaException
	 *             thrown if there is an error while removing the statements
	 */
	void remove(Iterable<? extends IStatement> statements);

	/**
	 * Unregisters an {@link IEntityDecorator decorator}
	 * 
	 * @param decorator
	 *            the decorator
	 */
	void removeDecorator(IEntityDecorator decorator);

	/**
	 * Removes the <code>concept</code> designation from this
	 * <code>entity</code>.
	 * 
	 * @param entity
	 *            An existing entity retrieved from this manager.
	 * @param concepts
	 *            interface to be translated to rdf:type.
	 */
	void removeDesignation(Object entity, Class<?>... concepts);

	/**
	 * Removes a namespace declaration by removing the association between a
	 * prefix and a namespace name.
	 * 
	 * @param prefix
	 *            The namespace prefix of which the assocation with a namespace
	 *            name is to be removed.
	 */
	void removeNamespace(String prefix);

	/**
	 * Removes all the references to the given <code>entity</code> and replaces
	 * them with references to the new <code>qname</code>. It is the
	 * responsibility of the caller to ensure that any object references to this
	 * resource are replaced with the returned object. Previous referenced
	 * objects must no longer be used and any cached values must be refreshed.
	 * 
	 * @param entity
	 *            current Entity to be renamed
	 * @param uri
	 *            new qualified name of the entity
	 * @return <code>entity</code> with the given <code>qname</code>.
	 */
	<T> T rename(T entity, URI uri);

	/**
	 * Set the flush mode that applies to all objects contained in the
	 * persistence context.
	 * 
	 * @param flushMode
	 */
	void setFlushMode(FlushModeType flushMode);

	/**
	 * Sets the prefix for a namespace.
	 * 
	 * @param prefix
	 *            The new prefix.
	 * @param name
	 *            The namespace name that the prefix maps to.
	 */
	void setNamespace(String prefix, URI uri);

	/**
	 * Set an entity manager property. If a vendor-specific property is not
	 * recognized, it is silently ignored.
	 * 
	 * @param propertyName
	 * @param value
	 * @throws IllegalArgumentException
	 *             if the second argument is not valid for the implementation
	 */
	void setProperty(String propertyName, Object value);
}
