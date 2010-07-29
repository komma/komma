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
package net.enilink.komma.sesame;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.properties.sesame.ObjectQuery;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.contextaware.ContextAwareConnection;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.internal.sesame.ISesameManagerAware;
import net.enilink.komma.core.FlushModeType;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IKommaManagerFactory;
import net.enilink.komma.core.IKommaTransaction;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.InferencingCapability;
import net.enilink.komma.core.LockModeType;

public class DelegatingSesameManager implements ISesameManager {
	ISesameManager delegate;

	IEntityDecorator managerInjector = new IEntityDecorator() {
		@Override
		public void decorate(IEntity entity) {
			((ISesameManagerAware) entity)
					.initSesameManager(DelegatingSesameManager.this);
		}
	};

	@Override
	public void addDecorator(IEntityDecorator decorator) {
		delegate.addDecorator(decorator);
	}

	@Override
	public void add(Iterator<? extends IStatement> statements) {
		delegate.add(statements);
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public void clearNamespaces() {
		delegate.clearNamespaces();
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public void close(Iterator<?> iter) {
		delegate.close(iter);
	}

	@Override
	public boolean contains(Object entity) {
		return delegate.contains(entity);
	}

	@Override
	public Object convertValue(ILiteral literal) {
		return delegate.convertValue(literal);
	}

	@Override
	public <T> T create(Class<T> concept, Class<?>... concepts) {
		return delegate.create(concept, concepts);
	}

	@Override
	public IEntity create(IReference... concepts) {
		return delegate.create(concepts);
	}

	@Override
	public <T> T create(Resource resource, Class<T> concept,
			Class<?>... concepts) {
		return delegate.create(resource, concept, concepts);
	}

	@Override
	public ISesameEntity createBean(Resource resource, Collection<URI> types,
			Model model) {
		return delegate.createBean(resource, types, model);
	}

	@Override
	public ILiteral createLiteral(Object value,
			net.enilink.komma.core.URI datatype, String language) {
		return delegate.createLiteral(value, datatype, language);
	}

	@Override
	public <T> T createNamed(net.enilink.komma.core.URI uri,
			Class<T> concept, Class<?>... concepts) {
		return delegate.createNamed(uri, concept, concepts);
	}

	@Override
	public IEntity createNamed(net.enilink.komma.core.URI uri,
			IReference... concepts) {
		return delegate.createNamed(uri, concepts);
	}

	@Override
	public IQuery<?> createQuery(String query) {
		return delegate.createQuery(query);
	}

	@Override
	public IQuery<?> createQuery(String query, String baseURI) {
		return delegate.createQuery(query, baseURI);
	}

	@Override
	public <T> T designateEntity(Object entity, Class<T> concept,
			Class<?>... concepts) {
		return delegate.designateEntity(entity, concept, concepts);
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	@Override
	public <T> T find(net.enilink.komma.core.URI uri,
			Class<T> concept, Class<?>... concepts) {
		return delegate.find(uri, concept, concepts);
	}

	@Override
	public ISesameEntity find(IReference reference) {
		return (ISesameEntity) delegate.find(reference);
	}

	@Override
	public ISesameEntity find(Resource resource) {
		return delegate.find(resource);
	}

	@Override
	public ISesameEntity find(Resource resource, Class<?>... concepts) {
		return delegate.find(resource, concepts);
	}

	@Override
	public <T> IExtendedIterator<T> findAll(Class<T> javaClass) {
		return delegate.findAll(javaClass);
	}

	@Override
	public <T> T findRestricted(net.enilink.komma.core.URI uri,
			Class<T> concept, Class<?>... concepts) {
		return delegate.find(uri, concept, concepts);
	}

	@Override
	public ISesameEntity findRestricted(Resource resource, Class<?>... concepts) {
		return delegate.findRestricted(resource, concepts);
	}

	@Override
	public void flush() {
		delegate.flush();
	}

	@Override
	public ContextAwareConnection getConnection() {
		return delegate.getConnection();
	}

	@Override
	public Object getDelegate() {
		return delegate.getDelegate();
	}

	@Override
	public IKommaManagerFactory getFactory() {
		return delegate.getFactory();
	}

	@Override
	public FlushModeType getFlushMode() {
		return delegate.getFlushMode();
	}

	@Override
	public InferencingCapability getInferencing() {
		return delegate.getInferencing();
	}

	@Override
	public Object getInstance(Value value, Class<?> type) {
		return delegate.getInstance(value, type);
	}

	@Override
	public String getLanguage() {
		return delegate.getLanguage();
	}

	@Override
	public Locale getLocale() {
		return delegate.getLocale();
	}

	@Override
	public LockModeType getLockMode(Object entity) {
		return delegate.getLockMode(entity);
	}

	@Override
	public IKommaManagerFactory getManagerFactory() {
		return delegate.getManagerFactory();
	}

	@Override
	public net.enilink.komma.core.URI getNamespace(String prefix) {
		return delegate.getNamespace(prefix);
	}

	@Override
	public IExtendedIterator<INamespace> getNamespaces() {
		return delegate.getNamespaces();
	}

	@Override
	public String getPrefix(net.enilink.komma.core.URI namespace) {
		return delegate.getPrefix(namespace);
	}

	@Override
	public Map<String, Object> getProperties() {
		return delegate.getProperties();
	}

	@Override
	public RoleMapper<URI> getRoleMapper() {
		return delegate.getRoleMapper();
	}

	@Override
	public Set<String> getSupportedProperties() {
		return delegate.getSupportedProperties();
	}

	@Override
	public IKommaTransaction getTransaction() {
		return delegate.getTransaction();
	}

	@Override
	public Value getValue(Object instance) {
		return delegate.getValue(instance);
	}

	@Override
	public boolean hasDecorator(IEntityDecorator decorator) {
		return delegate.hasDecorator(decorator);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public void joinTransaction() {
		delegate.joinTransaction();
	}

	@Override
	public void lock(Object entity, LockModeType mode) {
		delegate.lock(entity, mode);
	}

	@Override
	public void lock(Object entity, LockModeType lockMode,
			Map<String, Object> properties) {
		delegate.lock(entity, lockMode, properties);

	}

	@Override
	public <T> T merge(T bean) {
		return delegate.merge(bean);
	}

	@Override
	public void persist(Object bean) {
		delegate.persist(bean);
	}

	@Override
	public ObjectQuery prepareObjectQuery(String query, String baseURI) {
		return delegate.prepareObjectQuery(query, baseURI);
	}

	@Override
	public void refresh(Object entity) {
		delegate.refresh(entity);
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode) {
		delegate.refresh(entity, lockMode);
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode,
			Map<String, Object> properties) {
		delegate.refresh(entity, lockMode, properties);

	}

	@Override
	public void refresh(Object entity, Map<String, Object> properties) {
		delegate.refresh(entity, properties);
	}

	@Override
	public void remove(Object entity) {
		delegate.remove(entity);
	}

	@Override
	public void remove(Iterator<? extends IStatement> statements) {
		delegate.remove(statements);
	}

	@Override
	public void removeDecorator(IEntityDecorator decorator) {
		delegate.removeDecorator(decorator);
	}

	@Override
	public void removeDesignation(Object entity, Class<?>... concepts) {
		delegate.removeDesignation(entity, concepts);
	}

	@Override
	public void removeNamespace(String prefix) {
		delegate.removeNamespace(prefix);
	}

	@Override
	public <T> T rename(T bean, net.enilink.komma.core.URI uri) {
		return delegate.rename(bean, uri);
	}

	@Override
	public <T> T rename(T bean, Resource dest) {
		return delegate.rename(bean, dest);
	}

	@Override
	public void setConnection(ContextAwareConnection connection) {
		delegate.setConnection(connection);
	}

	@Override
	public void setFlushMode(FlushModeType flushMode) {
		delegate.setFlushMode(flushMode);
	}

	public void setKommaManager(ISesameManager delegate) {
		this.delegate = delegate;
		this.delegate.addDecorator(managerInjector);
	}

	@Override
	public void setNamespace(String prefix,
			net.enilink.komma.core.URI uri) {
		delegate.setNamespace(prefix, uri);
	}

	@Override
	public void setProperty(String propertyName, Object value) {
		delegate.setProperty(propertyName, value);
	}

	@Override
	public String toString() {
		return delegate.toString();
	}
}
