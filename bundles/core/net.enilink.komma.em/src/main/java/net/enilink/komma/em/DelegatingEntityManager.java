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
package net.enilink.komma.em;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IStatementPattern;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.IUpdate;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.InferencingCapability;
import net.enilink.komma.core.LockModeType;
import net.enilink.komma.core.URI;

public abstract class DelegatingEntityManager implements IEntityManager {
	@Override
	public void add(Iterable<? extends IStatement> statements) {
		getDelegate().add(statements);
	}

	@Override
	public void add(Iterable<? extends IStatement> statements, boolean ignoreImports) {
		getDelegate().add(statements, ignoreImports);
	}

	@Override
	public void addDecorator(IEntityDecorator decorator) {
		getDelegate().addDecorator(decorator);
	}

	@Override
	public void clear() {
		getDelegate().clear();
	}

	@Override
	public void clearNamespaces() {
		getDelegate().clearNamespaces();
	}

	@Override
	public void close() {
		getDelegate().close();
	}

	@Override
	public void close(Iterator<?> iter) {
		getDelegate().close(iter);
	}

	@Override
	public boolean contains(Object entity) {
		return getDelegate().contains(entity);
	}

	@Override
	public <T> T create(Class<T> concept, Class<?>... concepts) {
		return getDelegate().create(concept, concepts);
	}

	@Override
	public IEntity create(IReference... concepts) {
		return getDelegate().create(concepts);
	}

	@Override
	public ILiteral createLiteral(Object value, URI datatype) {
		return getDelegate().createLiteral(value, datatype);
	}

	@Override
	public ILiteral createLiteral(String label, URI datatype, String language) {
		return getDelegate().createLiteral(label, datatype, language);
	}

	@Override
	public <T> T createNamed(net.enilink.komma.core.URI uri, Class<T> concept,
			Class<?>... concepts) {
		return getDelegate().createNamed(uri, concept, concepts);
	}

	@Override
	public IEntity createNamed(net.enilink.komma.core.URI uri,
			IReference... concepts) {
		return getDelegate().createNamed(uri, concepts);
	}

	@Override
	public IQuery<?> createQuery(String query) {
		return getDelegate().createQuery(query);
	}

	@Override
	public IQuery<?> createQuery(String query, String baseURI) {
		return getDelegate().createQuery(query, baseURI);
	}

	@Override
	public IQuery<?> createQuery(String query, boolean includeInferred) {
		return getDelegate().createQuery(query, includeInferred);
	}

	@Override
	public IQuery<?> createQuery(String query, String baseURI,
			boolean includeInferred) {
		return getDelegate().createQuery(query, baseURI, includeInferred);
	}

	@Override
	public IReference createReference() {
		return getDelegate().createReference();
	}

	@Override
	public IReference createReference(String id) {
		return getDelegate().createReference(id);
	}

	@Override
	public IUpdate createUpdate(String update, String baseURI,
			boolean includeInferred) {
		return getDelegate().createUpdate(update, baseURI, includeInferred);
	}

	@Override
	public <T> T assignTypes(Object entity, Class<T> concept,
			Class<?>... concepts) {
		return getDelegate().assignTypes(entity, concept, concepts);
	}

	@Override
	public boolean equals(Object obj) {
		return getDelegate().equals(obj);
	}

	@Override
	public IEntity find(IReference reference) {
		return getDelegate().find(reference);
	}

	@Override
	public <T> T find(IReference uri, Class<T> concept, Class<?>... concepts) {
		return getDelegate().find(uri, concept, concepts);
	}

	@Override
	public <T> IExtendedIterator<T> findAll(Class<T> javaClass) {
		return getDelegate().findAll(javaClass);
	}

	@Override
	public <T> T findRestricted(IReference reference, Class<T> concept,
			Class<?>... concepts) {
		return getDelegate().findRestricted(reference, concept, concepts);
	}

	abstract public IEntityManager getDelegate();

	@Override
	public IEntityManagerFactory getFactory() {
		return getDelegate().getFactory();
	}

	@Override
	public InferencingCapability getInferencing() {
		return getDelegate().getInferencing();
	}

	@Override
	public Locale getLocale() {
		return getDelegate().getLocale();
	}

	@Override
	public LockModeType getLockMode(Object entity) {
		return getDelegate().getLockMode(entity);
	}

	@Override
	public URI getNamespace(String prefix) {
		return getDelegate().getNamespace(prefix);
	}

	@Override
	public IExtendedIterator<INamespace> getNamespaces() {
		return getDelegate().getNamespaces();
	}

	@Override
	public String getPrefix(net.enilink.komma.core.URI namespace) {
		return getDelegate().getPrefix(namespace);
	}

	@Override
	public Map<String, Object> getProperties() {
		return getDelegate().getProperties();
	}

	@Override
	public ITransaction getTransaction() {
		return getDelegate().getTransaction();
	}

	@Override
	public boolean hasDecorator(IEntityDecorator decorator) {
		return getDelegate().hasDecorator(decorator);
	}

	@Override
	public int hashCode() {
		return getDelegate().hashCode();
	}

	@Override
	public boolean hasMatch(IReference subject, IReference predicate,
			Object object) {
		return getDelegate().hasMatch(subject, predicate, object);
	}

	@Override
	public boolean hasMatchAsserted(IReference subject, IReference predicate,
			Object object) {
		return getDelegate().hasMatchAsserted(subject, predicate, object);
	}

	@Override
	public boolean isOpen() {
		return getDelegate().isOpen();
	}

	@Override
	public void joinTransaction() {
		getDelegate().joinTransaction();
	}

	@Override
	public void lock(Object entity, LockModeType mode) {
		getDelegate().lock(entity, mode);
	}

	@Override
	public void lock(Object entity, LockModeType lockMode,
			Map<String, Object> properties) {
		getDelegate().lock(entity, lockMode, properties);

	}

	@Override
	public IExtendedIterator<IStatement> match(IReference subject,
			IReference predicate, Object object) {
		return getDelegate().match(subject, predicate, object);
	}

	@Override
	public IExtendedIterator<IStatement> matchAsserted(IReference subject,
			IReference predicate, IValue object) {
		return getDelegate().matchAsserted(subject, predicate, object);
	}

	@Override
	public <T> T merge(T bean) {
		return getDelegate().merge(bean);
	}

	@Override
	public void refresh(Object entity) {
		getDelegate().refresh(entity);
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode) {
		getDelegate().refresh(entity, lockMode);
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode,
			Map<String, Object> properties) {
		getDelegate().refresh(entity, lockMode, properties);

	}

	@Override
	public void refresh(Object entity, Map<String, Object> properties) {
		getDelegate().refresh(entity, properties);
	}

	@Override
	public void remove(Iterable<? extends IStatementPattern> statements) {
		getDelegate().remove(statements);
	}

	@Override
	public void remove(Object entity) {
		getDelegate().remove(entity);
	}

	@Override
	public void removeDecorator(IEntityDecorator decorator) {
		getDelegate().removeDecorator(decorator);
	}

	@Override
	public void removeTypes(Object entity, Class<?>... concepts) {
		getDelegate().removeTypes(entity, concepts);
	}

	@Override
	public void removeNamespace(String prefix) {
		getDelegate().removeNamespace(prefix);
	}

	@Override
	public void removeRecursive(Object entity, boolean anonymousOnly) {
		getDelegate().removeRecursive(entity, anonymousOnly);
	}

	@Override
	public <T> T rename(T bean, net.enilink.komma.core.URI uri) {
		return getDelegate().rename(bean, uri);
	}

	@Override
	public void setNamespace(String prefix, net.enilink.komma.core.URI uri) {
		getDelegate().setNamespace(prefix, uri);
	}

	@Override
	public void setProperty(String propertyName, Object value) {
		getDelegate().setProperty(propertyName, value);
	}

	@Override
	public boolean supportsRole(Class<?> role) {
		return getDelegate().supportsRole(role);
	}

	@Override
	public Object toInstance(IValue value) {
		return getDelegate().toInstance(value);
	}

	@Override
	public Object toInstance(Object value, Class<?> type, IGraph graph) {
		return getDelegate().toInstance(value, type, graph);
	}

	@Override
	public String toString() {
		return getDelegate().toString();
	}

	public IValue toValue(Object instance) {
		return getDelegate().toValue(instance);
	}
}
