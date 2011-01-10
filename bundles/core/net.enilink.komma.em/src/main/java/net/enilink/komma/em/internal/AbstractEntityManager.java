/*
 * Copyright (c) 2007, 2010, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package net.enilink.komma.em.internal;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.ReferenceIdentityMap;
import net.enilink.composition.ClassResolver;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.cache.annotations.Cacheable;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.komma.ConversionUtil;
import net.enilink.composition.properties.traits.Mergeable;
import net.enilink.composition.properties.traits.Refreshable;
import net.enilink.composition.traits.Behaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

import net.enilink.commons.iterator.ConvertingIterator;
import net.enilink.commons.iterator.Filter;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.xmlschema.XMLSCHEMA;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.em.internal.behaviours.IEntityManagerAware;
import net.enilink.komma.em.internal.query.EntityManagerQuery;
import net.enilink.komma.literals.LiteralConverter;
import net.enilink.komma.core.FlushModeType;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IReferenceable;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.InferencingCapability;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.LockModeType;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.TransactionRequiredException;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.util.RESULTS;

/**
 * Handles operations of {@link IEntityManager}.
 * 
 */
public abstract class AbstractEntityManager implements IEntityManager,
		IEntityManagerInternal {
	private static final URI RESULT_NODE = RESULTS.TYPE_RESULT;

	private ClassResolver<URI> classResolver;

	IDataManager dm;

	private IEntityManagerFactory factory;

	@Inject
	protected Injector injector;

	@Inject
	private LiteralConverter literalConverter;

	@Inject
	private Locale locale;

	final Logger logger = LoggerFactory.getLogger(AbstractEntityManager.class);

	private RoleMapper<URI> mapper;

	@SuppressWarnings("unchecked")
	private Map<Object, IReference> merged = new ReferenceIdentityMap(
			ReferenceIdentityMap.WEAK, ReferenceIdentityMap.HARD, true);

	ResourceManager resourceManager;

	TypeManager typeManager;

	private Map<net.enilink.komma.core.URI, String> uriToPrefix = null;

	@Inject(optional = true)
	@Named("readContexts")
	private Set<URI> readContexts;

	@Inject(optional = true)
	@Named("addContexts")
	private Set<URI> addContexts;

	@Override
	public void add(Iterable<? extends IStatement> statements) {
		dm.add(new ConvertingIterator<IStatement, IStatement>(statements
				.iterator()) {
			@Override
			protected IStatement convert(IStatement stmt) {
				if (stmt.getObject() instanceof IValue) {
					return stmt;
				}
				return new Statement(stmt.getSubject(), stmt.getPredicate(),
						toValue(stmt.getObject()), stmt.getContext());
			}
		});
	}

	private <C extends Collection<URI>> C addConcept(IReference resource,
			Class<?> role, C set) {
		URI type = mapper.findType(role);
		if (type == null) {
			throw new KommaException(
					"Concept is anonymous or is not registered: "
							+ role.getSimpleName());
		}
		typeManager.addType(resource, type);
		set.add(type);
		return set;
	}

	private void appendFilter(Class<?> concept, StringBuilder query) {
		Collection<URI> types = new HashSet<URI>();
		mapper.findSubTypes(concept, types);
		Iterator<URI> iter = types.iterator();
		if (iter.hasNext()) {
			while (iter.hasNext()) {
				query.append("{ ?subj a <");
				query.append(iter.next()).append(">}\n");
				if (iter.hasNext()) {
					query.append(" UNION ");
				}
			}
		} else {
			throw new KommaException("Concept not registered: "
					+ concept.getSimpleName());
		}
	}

	private boolean assertConceptsRecorded(IEntity bean, Class<?>... concepts) {
		for (Class<?> concept : concepts) {
			assert !concept.isInterface()
					|| concept.isAssignableFrom(bean.getClass()) : "Concept has not been recorded: "
					+ concept.getSimpleName();
		}
		return true;
	}

	private IReference assignReference(Object bean) {
		synchronized (merged) {
			if (merged.containsKey(bean)) {
				return merged.get(bean);
			}
			IReference reference = getReference(bean);
			if (reference == null) {
				reference = resourceManager.createResource(null);
			}
			merged.put(bean, reference);
			return reference;
		}
	}

	@Override
	public void clear() {
		dm.remove(new Statement(null, null, null));
	}

	@Override
	public void clearNamespaces() {
		uriToPrefix = null;
		dm.clearNamespaces();
	}

	@Override
	public void close() {
		if (dm != null) {
			dm.close();
		}
		dm = null;
	}

	@Override
	public void close(Iterator<?> iter) {
		if (iter instanceof Closeable) {
			try {
				((Closeable) iter).close();
			} catch (IOException e) {
				throw new KommaException(e);
			}
		}
	}

	private <T> Class<?>[] combine(Class<T> concept, Class<?>... concepts) {
		Class<?>[] roles;
		if (concepts == null || concepts.length == 0) {
			roles = new Class<?>[] { concept };
		} else {
			roles = new Class<?>[concepts.length + 1];
			roles[0] = concept;
			System.arraycopy(concepts, 0, roles, 1, concepts.length);
		}
		return roles;
	}

	@Override
	public boolean contains(Object entity) {
		if (entity instanceof IEntity) {
			return this.equals(((IEntity) entity).getEntityManager());
		} else if (entity instanceof Behaviour<?>) {
			Behaviour<?> behaviour = (Behaviour<?>) entity;
			Object delegate = behaviour.getBehaviourDelegate();
			if (delegate instanceof IEntity) {
				return this.equals(((IEntity) delegate).getEntityManager());
			}
		}
		return false;
	}

	@Override
	public <T> T create(Class<T> concept, Class<?>... concepts) {
		return createNamed((net.enilink.komma.core.URI) null, concept,
				concepts);
	}

	@Override
	public IEntity create(IReference... concepts) {
		return createNamed(null, concepts);
	}

	@SuppressWarnings("unchecked")
	public <T> T create(IReference resource, Class<T> concept,
			Class<?>... concepts) {
		boolean isActive = false;
		try {
			Set<URI> types = new HashSet<URI>();

			isActive = getTransaction().isActive();

			if (!isActive) {
				getTransaction().begin();
			}

			addConcept(resource, concept, types);
			for (Class<?> c : concepts) {
				addConcept(resource, c, types);
			}

			if (!isActive) {
				getTransaction().commit();
			}
		} catch (Exception e) {
			if (!isActive && getTransaction().isActive()) {
				getTransaction().rollback();
			}
			if (e instanceof KommaException) {
				throw (KommaException) e;
			}
			throw new KommaException(e);
		}

		IEntity bean = createBean(resource, null, false, null);
		assert assertConceptsRecorded(bean, combine(concept, concepts));
		return (T) bean;
	}

	@SuppressWarnings("unchecked")
	public IEntity createBean(IReference resource, Collection<URI> types,
			boolean restrictTypes, IGraph graph) {
		Collection<URI> entityTypes = new ArrayList<URI>();
		if (!restrictTypes) {
			if (graph != null) {
				entityTypes.addAll((Collection<URI>) (Collection<?>) graph
						.filter(resource, RDF.PROPERTY_TYPE, null).objects());
			}

			if (entityTypes.isEmpty()) {
				entityTypes.addAll(typeManager.getTypes(resource));
			}
		}
		if (types != null) {
			entityTypes.addAll(types);
		}

		IEntity bean = createBeanForClass(resource,
				classResolver.resolveComposite(entityTypes));
		if (graph != null) {
			initializeBean(bean, graph);
		}
		return bean;
	}

	protected IEntity createBeanForClass(IReference resource, Class<?> type) {
		Object obj;
		try {
			obj = type.newInstance();

			injector.injectMembers(obj);

			assert obj instanceof IEntityManagerAware : "core roles are not registered, check your deployed classpath";
			IEntityManagerAware bean = (IEntityManagerAware) obj;
			bean.initReference(resource);
			return (IEntity) bean;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	public ILiteral createLiteral(Object value,
			net.enilink.komma.core.URI datatype, String language) {
		if (datatype == null && language != null) {
			return new net.enilink.komma.common.util.Literal(value,
					language);
		}
		return literalConverter.createLiteral(value, datatype);
	}

	public <T> T createNamed(net.enilink.komma.core.URI uri,
			Class<T> concept, Class<?>... concepts) {
		IReference resource = resourceManager.createResource(uri);
		return create(resource, concept, concepts);
	}

	@Override
	public IEntity createNamed(net.enilink.komma.core.URI uri,
			IReference... concepts) {
		IReference resource = resourceManager.createResource(uri);
		try {
			boolean active = dm.getTransaction().isActive();
			if (!active) {
				dm.getTransaction().begin();
			}
			for (IReference concept : concepts) {
				dm.add(new Statement(resource, RDF.PROPERTY_TYPE, concept));
			}
			if (!active) {
				dm.getTransaction().commit();
			}
		} catch (KommaException e) {
			if (dm.getTransaction().isActive()) {
				dm.getTransaction().rollback();
			}
			throw new KommaException(e);
		}
		return createBean(resource, null, false, null);
	}

	public IQuery<?> createQuery(String query) {
		return createQuery(query, null);
	}

	public IQuery<?> createQuery(String query, String baseURI) {
		System.out.println("query: " + query);

		IQuery<?> result = new EntityManagerQuery<Object>(this, dm.createQuery(
				query, baseURI));
		injector.injectMembers(result);
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T designateEntity(Object entity, Class<T> concept,
			Class<?>... concepts) {
		IReference resource = getReference(entity);
		Collection<URI> types = new ArrayList<URI>();

		boolean isActive = false;
		try {
			getTypes(entity.getClass(), types);

			isActive = getTransaction().isActive();

			if (!isActive) {
				getTransaction().begin();
			}

			addConcept(resource, concept, types);
			for (Class<?> c : concepts) {
				addConcept(resource, c, types);
			}

			if (!isActive) {
				getTransaction().commit();
			}
		} catch (Exception e) {
			if (!isActive && getTransaction().isActive()) {
				getTransaction().rollback();
			}
		}

		IEntity bean = createBean(resource, types, false, null);
		assert assertConceptsRecorded(bean, concepts);
		return (T) bean;
	}

	protected <T> IExtendedIterator<T> filterResultNode(Set<T> values) {
		return WrappedIterator.create(values.iterator()).filterDrop(
				new Filter<T>() {
					@Override
					public boolean accept(T o) {
						return RESULT_NODE.equals(o);
					}
				});
	}

	@Override
	public IEntity find(IReference reference) {
		return createBean(reference, null, false, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T find(IReference reference, Class<T> concept,
			Class<?>... concepts) {
		Class<?>[] allConcepts = Arrays.copyOf(concepts, concepts.length + 1);
		allConcepts[allConcepts.length - 1] = concept;
		return (T) find(reference, Arrays.asList(allConcepts));
	}

	public IEntity find(IReference reference, Collection<Class<?>> concepts) {
		Set<URI> types = new LinkedHashSet<URI>(concepts.size());
		for (Class<?> concept : concepts) {
			if (IValue.class.equals(concept)
					|| IReference.class.equals(concept)) {
				continue;
			}

			URI type = mapper.findType(concept);
			if (type != null) {
				types.add(type);
			} else {
				logger.warn("Unknown rdf type for concept class: " + concept);
			}
		}
		return createBean(reference, types, false, null);
	}

	public <T> IExtendedIterator<T> findAll(final Class<T> concept) {
		StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT DISTINCT ?subj WHERE {");
		appendFilter(concept, querySb);
		querySb.append("}");
		return createQuery(querySb.toString()).bindResultType(concept)
				.evaluate();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T findRestricted(IReference reference, Class<T> concept,
			Class<?>... concepts) {
		Class<?>[] allConcepts = Arrays.copyOf(concepts, concepts.length + 1);
		allConcepts[allConcepts.length - 1] = concept;
		return (T) findRestricted(reference, Arrays.asList(allConcepts));
	}

	@Override
	public IReference findRestricted(IReference reference,
			Collection<Class<?>> concepts) {
		// if type is restricted to IReference then simply return the
		// corresponding reference value
		if (concepts.size() == 1) {
			Class<?> concept = concepts.iterator().next();
			if ((IValue.class.equals(concept) || IReference.class
					.equals(concept))) {
				return reference;
			}
		}

		List<URI> types = new ArrayList<URI>(concepts.size());
		for (Class<?> concept : concepts) {
			types.add(mapper.findType(concept));
		}
		return createBean(reference, types, true, null);
	}

	@Override
	public void flush() {
	}

	@Override
	public IEntityManagerFactory getFactory() {
		return factory;
	}

	@Override
	public FlushModeType getFlushMode() {
		return FlushModeType.AUTO;
	}

	@Override
	public InferencingCapability getInferencing() {
		return dm.getInferencing();
	}

	protected List<Object> getInstances(Iterator<?> values, Class<?> type,
			IGraph graph) {
		List<Object> instances = new ArrayList<Object>();
		while (values.hasNext()) {
			instances.add(toInstance(values.next(), type, graph));
		}
		return instances;
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public LockModeType getLockMode(Object entity) {
		return null;
	}

	@Override
	public net.enilink.komma.core.URI getNamespace(String prefix) {
		return dm.getNamespace(prefix);
	}

	@Override
	public IExtendedIterator<INamespace> getNamespaces() {
		return dm.getNamespaces();
	}

	@Override
	public String getPrefix(net.enilink.komma.core.URI namespaceUri) {
		if (uriToPrefix == null) {
			uriToPrefix = new HashMap<net.enilink.komma.core.URI, String>();
			for (INamespace namespace : getNamespaces()) {
				uriToPrefix.put(namespace.getURI(), namespace.getPrefix());
			}
		}
		return uriToPrefix.get(namespaceUri);
	}

	@Override
	public Map<String, Object> getProperties() {
		return Collections.emptyMap();
	}

	protected IReference getReference(Object bean) {
		if (bean instanceof IReferenceable) {
			return ((IReferenceable) bean).getReference();
		}
		if (bean instanceof IReference) {
			return (IReference) bean;
		}
		return null;
	}

	private IReference getReferenceOrFail(Object entity) {
		IReference reference = getReference(entity);
		if (reference == null) {
			throw new KommaException("Unknown Entity: " + entity);
		}
		return reference;
	}

	@Override
	public Set<String> getSupportedProperties() {
		return Collections.emptySet();
	}

	@Override
	public ITransaction getTransaction() {
		return dm.getTransaction();
	}

	private <C extends Collection<URI>> C getTypes(Class<?> role, C set) {
		URI type = mapper.findType(role);
		if (type == null) {
			Class<?> superclass = role.getSuperclass();
			if (superclass != null) {
				getTypes(superclass, set);
			}
			Class<?>[] interfaces = role.getInterfaces();
			for (int i = 0, n = interfaces.length; i < n; i++) {
				getTypes(interfaces[i], set);
			}
		} else {
			set.add(type);
		}
		return set;
	}

	@Override
	public boolean hasMatch(IReference subject, IReference predicate,
			Object object) {
		return dm.hasMatch(subject, predicate, toValue(object));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void initializeBean(IEntity bean, IGraph graph) {
		if (!graph.contains(bean, null, null)) {
			return;
		}
		try {
			for (Method method : bean.getClass().getMethods()) {
				if (method.getParameterTypes().length > 0) {
					continue;
				}

				// initialize a property set
				if (method.isAnnotationPresent(Iri.class)) {
					Iri Iri = method.getAnnotation(Iri.class);

					if (graph.contains(bean, URIImpl.createURI(Iri.value()),
							null)) {
						Set<Object> objects = new LinkedHashSet<Object>(graph
								.filter(bean, URIImpl.createURI(Iri.value()),
										null).objects());
						graph.remove(bean, URIImpl.createURI(Iri.value()), null);

						Object result = method.invoke(bean);
						if (result instanceof PropertySet<?>) {
							((PropertySet) result).init(getInstances(
									filterResultNode(objects),
									((PropertySet) result).getElementType(),
									graph));
						}
					}
				} else if (method.isAnnotationPresent(Cacheable.class)) {
					Cacheable cacheable = method.getAnnotation(Cacheable.class);
					if (cacheable.key().isEmpty()) {
						continue;
					}

					// initialize cache entries
					Class<?> returnType = method.getReturnType();

					boolean isIterator = false;
					boolean isBoolean = false;
					Collection<Object> collection = null;
					if (Iterator.class.isAssignableFrom(returnType)) {
						isIterator = true;
						collection = new ArrayList<Object>();
					} else if (Set.class.isAssignableFrom(returnType)) {
						collection = new HashSet<Object>();
					} else if (List.class.isAssignableFrom(returnType)) {
						collection = new ArrayList<Object>();
					} else if (Collection.class.isAssignableFrom(returnType)) {
						collection = new ArrayList<Object>();
					} else if (Boolean.class.equals(returnType)
							|| Boolean.TYPE.equals(returnType)) {
						isBoolean = true;
					}

					if (collection != null || isBoolean) {
						URI keyUri = URIImpl.createURI(cacheable.key());
						if (graph.contains(bean, keyUri, null)) {
							Set<Object> objects = new LinkedHashSet<Object>(
									graph.filter(bean, keyUri, null).objects());
							graph.remove(bean, keyUri, null);

							IExtendedIterator<Object> valuesIt = filterResultNode(objects);

							Object value = null;
							if (collection != null) {
								// get element type
								Type t = method.getGenericReturnType();
								Class<?> valueType = null;
								if (t instanceof ParameterizedType) {
									ParameterizedType pt = (ParameterizedType) t;
									Type[] args = pt.getActualTypeArguments();
									if (args.length == 1
											&& args[0] instanceof Class<?>) {
										valueType = (Class<?>) args[0];
									}
								}

								collection.addAll(getInstances(valuesIt,
										valueType, graph));

								if (isIterator) {
									value = collection.iterator();
								} else {
									value = collection;
								}
							} else if (isBoolean) {
								value = valuesIt.hasNext();
							}
							initializeCache(bean, cacheable.key(), value);
						}
					}
				}
			}

		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	protected void initializeCache(IEntity entity, Object property, Object value) {
		// does nothing - overridden by subclasses
	}

	private boolean isEntity(Class<?> type) {
		if (type == null)
			return false;
		for (Class<?> face : type.getInterfaces()) {
			if (mapper.findType(face) != null)
				return true;
		}
		if (mapper.findType(type) != null)
			return true;
		return isEntity(type.getSuperclass());
	}

	@Override
	public boolean isOpen() {
		return dm != null && dm.isOpen();
	}

	@Override
	public void joinTransaction() {
		if (!isOpen())
			throw new TransactionRequiredException();
	}

	public void lock(Object entity, LockModeType mode) {
		throw new UnsupportedOperationException("locking is not supported");
	}

	@Override
	public void lock(Object entity, LockModeType lockMode,
			Map<String, Object> properties) {
		lock(entity, lockMode);
	}

	@Override
	public IExtendedIterator<IStatement> match(IReference subject,
			IReference predicate, Object object) {
		return dm.match(subject, predicate, toValue(object));
	}

	@SuppressWarnings("unchecked")
	public <T> T merge(T bean) {
		if (bean == null) {
			return null;
		} else if (bean instanceof Set<?>) {
			// so we can merge both a List and a Set
			Set<?> old = (Set<?>) bean;
			Set<Object> set = new HashSet<Object>(old.size());
			for (Object o : old) {
				set.add(merge(o));
			}
			return (T) set;
		} else {
			IReference resource = assignReference(bean);

			boolean isActive = getTransaction().isActive();
			if (!isActive) {
				getTransaction().begin();
			}
			try {
				Class<?> proxy = bean.getClass();
				List<URI> types = getTypes(proxy, new ArrayList<URI>());
				for (URI type : types) {
					typeManager.addType(resource, type);
				}
				Object result = createBean(resource, types, false, null);
				if (result instanceof Mergeable) {
					((Mergeable) result).merge(bean);
				}
				if (!isActive) {
					getTransaction().commit();
				}

				return (T) result;
			} catch (Exception e) {
				throw new KommaException(e);
			} finally {
				if (!isActive && getTransaction().isActive()) {
					getTransaction().rollback();
				}
			}
		}
	}

	public void persist(Object bean) {
		merge(bean);
	}

	public void refresh(Object entity) {
		if (entity instanceof Refreshable) {
			((Refreshable) entity).refresh();
		}
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode) {
		refresh(entity);
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode,
			Map<String, Object> properties) {
		refresh(entity);
	}

	@Override
	public void refresh(Object entity, Map<String, Object> properties) {
		refresh(entity);
	}

	@Override
	public void remove(Iterable<? extends IStatement> statements) {
		dm.remove(new ConvertingIterator<IStatement, IStatement>(statements
				.iterator()) {
			@Override
			protected IStatement convert(IStatement stmt) {
				if (stmt.getObject() instanceof IValue) {
					return stmt;
				}
				return new Statement(stmt.getSubject(), stmt.getPredicate(),
						toValue(stmt.getObject()), stmt.getContext());
			}
		});
	}

	public void remove(Object entity) {
		IReference resource = getReferenceOrFail(entity);
		resourceManager.removeResource(resource);
	}

	@Override
	public void removeDesignation(Object entity, Class<?>... concepts) {
		IReference resource = getReferenceOrFail(entity);

		boolean isActive = false;
		try {
			isActive = getTransaction().isActive();

			if (!isActive) {
				getTransaction().begin();
			}

			for (Class<?> c : concepts) {
				URI type = mapper.findType(c);
				if (type == null) {
					continue;
				}

				typeManager.removeType(resource, type);
			}

			if (!isActive) {
				getTransaction().commit();
			}
		} catch (KommaException e) {
			if (!isActive && getTransaction().isActive()) {
				getTransaction().rollback();
			}
		}
	}

	@Override
	public void removeNamespace(String prefix) {
		uriToPrefix = null;
		dm.removeNamespace(prefix);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T rename(T bean, net.enilink.komma.core.URI uri) {
		IReference before = getReferenceOrFail(bean);
		IReference after = resourceManager.createResource(uri);
		resourceManager.renameResource(before, after);

		T newBean = (T) createBean(after, null, false, null);
		((IEntityManagerAware) bean).initReference(((IReferenceable) newBean)
				.getReference());

		return newBean;
	}

	@Inject
	protected void setClassResolver(ClassResolver<URI> classResolver) {
		this.classResolver = classResolver;
	}

	@Inject
	protected void setDataManager(IDataManager dm) {
		this.dm = dm;
		if (addContexts != null) {
			this.dm.setAddContexts(addContexts);
		}
		if (readContexts != null) {
			this.dm.setReadContexts(readContexts);
		}

		resourceManager = new ResourceManager(dm);
		typeManager = new TypeManager(dm);
	}

	@Override
	public void setFlushMode(FlushModeType mode) {
	}

	@Inject
	protected void setManagerFactory(IEntityManagerFactory factory) {
		this.factory = factory;
	}

	@Override
	public void setNamespace(String prefix, URI uri) {
		uriToPrefix = null;
		dm.setNamespace(prefix, uri);
	}

	@Override
	public void setProperty(String propertyName, Object value) {

	}

	@Inject
	protected void setRoleMapper(RoleMapper<URI> mapper) {
		this.mapper = mapper;
	}

	@Override
	public Object toInstance(IValue value) {
		return toInstance(value, null, null);
	}

	@Override
	public Object toInstance(Object value, Class<?> type, IGraph graph) {
		if (value instanceof IReference) {
			Collection<URI> types = null;
			if (type != null) {
				URI typeUri = mapper.findType(type);
				if (typeUri != null) {
					// ensure that specified type is added as role to resulting
					// object
					types = Collections.singleton(typeUri);
				}
			}

			IEntity bean = createBean((IReference) value, types, false, graph);
			if (logger.isDebugEnabled()) {
				if (!createQuery("ASK {?s ?p ?o}").setParameter("s",
						(IReference) value).getBooleanResult()) {
					logger.debug("Warning: Unknown entity: " + value);
				}
			}
			return bean;
		}

		ILiteral literal = (ILiteral) value;
		URI datatype = literal.getDatatype() == null ? null : URIImpl
				.createURI(literal.getDatatype().toString());
		Object instance = literalConverter.createObject(literal.getLabel(),
				datatype);
		if (type != null) {
			if (IValue.class.isAssignableFrom(type)) {
				instance = createLiteral(instance, datatype,
						literal.getLanguage());
			} else if (instance == null) {
				if (type.isPrimitive()) {
					instance = ConversionUtil.convertValue(type, 0, null);
				}
			} else if (!type.isAssignableFrom(ConversionUtil
					.wrapperType(instance.getClass()))) {
				// convert instance if actual type is not compatible
				// with valueType
				instance = ConversionUtil
						.convertValue(type, instance, instance);
			}
		}
		return instance;
	}

	@Override
	public IValue toValue(Object instance) {
		if (instance == null) {
			return null;
		}

		IReference reference = getReference(instance);
		if (reference != null) {
			return reference;
		}

		if (instance instanceof IValue) {
			return (IValue) reference;
		}

		Class<?> type = instance.getClass();
		if (literalConverter.isDatatype(type)) {
			return literalConverter.createLiteral(instance, null);
		}
		synchronized (merged) {
			if (merged.containsKey(instance)) {
				return merged.get(instance);
			}
		}
		if (IEntity.class.isAssignableFrom(type) || isEntity(type)) {
			return toValue(merge(instance));
		}

		return literalConverter.createLiteral(String.valueOf(instance),
				XMLSCHEMA.TYPE_STRING);
	}
}
