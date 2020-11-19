/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.security.auth.Refreshable;
import javax.transaction.TransactionRequiredException;

import net.enilink.commons.iterator.IExtendedIterator;

public interface IEntityManager extends AutoCloseable {
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
	 * Add statements to this manager, optionally ignoring statements from
	 * imported models when checking for duplicates.
	 * <p>
	 * The default behaviour for add() is to take these into account to avoid
	 * inserting statements that are already present from imports.
	 * 
	 * @param statements
	 *            the statements to add
	 * @param ignoreImports
	 *            wether to ignore statements from imported models
	 * @throws KommaException
	 *             thrown if there is an error while adding the statements
	 */
	void add(Iterable<? extends IStatement> statements, boolean ignoreImports);

	/**
	 * Registers an {@link IEntityDecorator decorator}
	 * 
	 * @param decorator
	 *            the decorator
	 */
	void addDecorator(IEntityDecorator decorator);

	/**
	 * Assigns the concepts to the given entity and returns a new object
	 * reference that implements the first <code>concept</code>.
	 * 
	 * @param <T>
	 *            The resulting entity type.
	 * @param entity
	 *            An existing entity.
	 * @param concept
	 *            interface to be translated to rdf:type.
	 * @param concepts
	 *            additional interfaces to be translated to rdf:type.
	 * @return Java Bean representing <code>entity</code> that implements
	 *         <code>concept</code>.
	 */
	<T> T assignTypes(Object entity, Class<T> concept, Class<?>... concepts);

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
	 * Converts a Java value to an {@link ILiteral} with the given
	 * <code>datatype</code>.
	 * 
	 * @param value
	 *            the Java value that should be converted to an literal
	 * @param datatype
	 *            the datatype of the new literal.
	 * 
	 * @return Object representing the literal.
	 */
	ILiteral createLiteral(Object value, URI datatype);

	/**
	 * Creates an ILiteral to hold a literal (label, type, language).
	 * 
	 * @param label
	 *            the literal's label.
	 * @param datatype
	 *            the literal's datatype.
	 * @param language
	 *            the literal's language.
	 * 
	 * @return Object representing the literal.
	 */
	ILiteral createLiteral(String label, URI datatype, String language);

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
	 * Creates an {@link IQuery} to evaluate the query string against asserted
	 * and inferred statements.
	 * 
	 * @param query
	 *            rdf query in the configured language - default SPARQL.
	 * @return {@link IQuery}.
	 */
	IQuery<?> createQuery(String query);

	/**
	 * Creates an {@link IQuery} to evaluate the query string against asserted
	 * and inferred statements.
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
	 * Creates an {@link IQuery} to evaluate the query string.
	 * 
	 * @param query
	 *            rdf query in the configured language - default SPARQL.
	 * @param includeInferred
	 *            Controls if inferred statements should be included to compute
	 *            the results or not.
	 * @return {@link IQuery}.
	 */
	IQuery<?> createQuery(String query, boolean includeInferred);

	/**
	 * Creates an {@link IQuery} to evaluate the query string.
	 * 
	 * @param query
	 *            RDF query in the configured language - default SPARQL.
	 * @param baseURI
	 *            base URI for relative URIs or <code>null</code> if the query
	 *            does not contain relative URIs
	 * @param includeInferred
	 *            Controls if inferred statements should be included to compute
	 *            the results or not.
	 * @return {@link IQuery}.
	 */
	IQuery<?> createQuery(String query, String baseURI, boolean includeInferred);

	/**
	 * Creates a new anonymous reference.
	 * 
	 * @return A new anonymous reference object.
	 */
	IReference createReference();

	/**
	 * Creates a new anonymous reference with the given <code>id</code>.
	 * 
	 * @param id
	 *            the id of the new reference or <code>null</code>
	 * @return A new reference object.
	 */
	IReference createReference(String id);

	/**
	 * Creates an {@link IUpdate} to evaluate the update string.
	 * 
	 * @param update
	 *            RDF update in the configured language - default SPARQL.
	 * @param baseURI
	 *            base URI for relative URIs or <code>null</code> if the update
	 *            does not contain relative URIs
	 * @param includeInferred
	 *            Controls if inferred statements should be included to compute
	 *            the results or not.
	 * @return {@link IUpdate}.
	 */
	IUpdate createUpdate(String update, String baseURI, boolean includeInferred);

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
	<T> T find(IReference uri, Class<T> concept, Class<?>... concepts);

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
	 * @param reference
	 *            Reference of the entity.
	 * @param concept
	 *            The primary type of the resulting bean.
	 * @param conepts
	 *            Additional types.
	 * @return JavaBean representing the subject.
	 */
	<T> T findRestricted(IReference reference, Class<T> concept,
			Class<?>... concepts);

	/**
	 * Return the factory for the entity manager.
	 * 
	 * @return IEntityManagerFactory instance
	 * @throws IllegalStateException
	 *             if the entity manager has been closed.
	 */
	IEntityManagerFactory getFactory();

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
	 * Get the properties and associated values that are in effect for the
	 * entity manager. Changing the contents of the map does not change the
	 * configuration in effect.
	 */
	Map<String, Object> getProperties();

	/**
	 * Returns the resource-level transaction object. The {@link ITransaction}
	 * instance may be used serially to begin and commit multiple transactions.
	 * 
	 * @return IKommaTransaction instance
	 */
	ITransaction getTransaction();

	/**
	 * Returns {@code true} if the {@link IEntityDecorator decorator} is already
	 * registered
	 * 
	 * @param decorator
	 *            the decorator
	 */
	boolean hasDecorator(IEntityDecorator decorator);

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
	boolean hasMatch(IReference subject, IReference predicate, Object object);

	/**
	 * Returns <code>true</code> if at least one asserted statement exists with
	 * the given subject, predicate, and object. Null parameters represent
	 * wildcards.
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
	boolean hasMatchAsserted(IReference subject, IReference predicate,
			Object object);

	/**
	 * If this manager currently has an open connection to the repository.
	 * 
	 * @return <code>true</code> if the connection is open.
	 */
	boolean isOpen();

	/**
	 * Indicate to the {@link IEntityManager} that a transaction is active. This
	 * method should be called on an application managed entity manager that was
	 * created outside the scope of the active transaction to associate it with
	 * the current transaction.
	 * 
	 * @throws TransactionRequiredException
	 *             if there is no transaction.
	 */
	void joinTransaction();

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
			IReference predicate, Object object);

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
	 * If <code>entity</code> implements Refreshable, its method
	 * {@link Refreshable#refresh()} will be called. This call instructs
	 * entities that their property values may have changed and they should
	 * reload them as needed.
	 * 
	 * @param entity
	 */
	void refresh(Object entity);

	/**
	 * Remove statements from this manager
	 * 
	 * @param statement
	 *            the statements to remove
	 * @throws KommaException
	 *             thrown if there is an error while removing the statements
	 */
	void remove(Iterable<? extends IStatementPattern> statements);

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
	 * Removes the given entity or subject while also recursively removing all
	 * referenced resources. It is the responsibility of the caller to ensure
	 * this <code>entity</code> or any other object referencing it are no longer
	 * used and any object that may have cached a value containing this is
	 * refreshed.
	 * 
	 * @param entity
	 *            to be removed from the pool and repository.
	 * 
	 */
	void removeRecursive(Object entity, boolean anonymousOnly);

	/**
	 * Unregisters an {@link IEntityDecorator decorator}
	 * 
	 * @param decorator
	 *            the decorator
	 */
	void removeDecorator(IEntityDecorator decorator);

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
	 * Removes the concepts from the given <code>entity</code>.
	 * 
	 * @param entity
	 *            An existing entity.
	 * @param concepts
	 *            interfaces to be translated to rdf:type.
	 */
	void removeTypes(Object entity, Class<?>... concepts);

	/**
	 * Returns a list of roles that are registered for the given
	 * <code>type</code>.
	 * 
	 * @param type
	 *            The type which roles should be looked up.
	 * @return A list of registered roles
	 */
	Collection<Class<?>> rolesForType(URI type);

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

	/**
	 * Returns the {@link IValue} converted to a Java type.
	 * 
	 * @return the converted value
	 */
	Object toInstance(IValue value);

	/**
	 * Returns the value converted to a Java type and initialized with data from
	 * <code>graph</code>.
	 * 
	 * @return the converted value
	 */
	Object toInstance(Object value, Class<?> type, IGraph graph);

	/**
	 * Checks if this entity manager has a mapping for the given
	 * <code>role</code>
	 * 
	 * @param role
	 *            Role class
	 * @return <code>true</code> if this entity manager has a mapping for the
	 *         given role, else <code>false</code>.
	 */
	boolean supportsRole(Class<?> role);

	/**
	 * Returns the Java object converted to an {@link IValue}.
	 * 
	 * @return the converted object value
	 */
	IValue toValue(Object instance);
}
