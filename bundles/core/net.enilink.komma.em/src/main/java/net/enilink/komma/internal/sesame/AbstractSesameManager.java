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
package net.enilink.komma.internal.sesame;

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
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryMetaData;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.enilink.commons.iterator.ConvertingIterator;
import net.enilink.commons.iterator.Filter;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.vocab.xmlschema.XMLSCHEMA;
import net.enilink.komma.KommaCore;
import net.enilink.komma.common.util.URIUtil;
import net.enilink.komma.literals.LiteralConverter;
import net.enilink.komma.core.FlushModeType;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IKommaManager;
import net.enilink.komma.core.IKommaTransaction;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.InferencingCapability;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.LockModeType;
import net.enilink.komma.core.TransactionRequiredException;
import net.enilink.komma.sesame.ISesameEntity;
import net.enilink.komma.sesame.ISesameManager;
import net.enilink.komma.sesame.ISesameResourceAware;
import net.enilink.komma.sesame.SesameManagerFactory;
import net.enilink.komma.sesame.iterators.SesameIterator;
import net.enilink.komma.util.RESULTS;

/**
 * Handles operations of {@link IKommaManager}.
 * 
 * @author Ken Wenzel
 * 
 */
public abstract class AbstractSesameManager implements ISesameManager {
	private static final URI RESULT_NODE = new URIImpl(
			RESULTS.TYPE_RESULT.toString());

	private ClassResolver<URI> classResolver;

	private ContextAwareConnection conn;

	private SesameManagerFactory factory;

	private InferencingCapability inferencing;

	@Inject
	protected Injector injector;

	@Inject
	private LiteralConverter literalConverter;

	@Inject
	private Locale locale;

	final Logger logger = LoggerFactory.getLogger(AbstractSesameManager.class);

	private RoleMapper<URI> mapper;

	private Map<net.enilink.komma.core.URI, String> uriToPrefix = null;

	@SuppressWarnings("unchecked")
	private Map<Object, Resource> merged = new ReferenceIdentityMap(
			ReferenceIdentityMap.WEAK, ReferenceIdentityMap.HARD, true);

	private SesameResourceManager resourceManager;

	private SesameTransaction transaction;

	private SesameTypeManager typeManager;

	@Override
	public void add(Iterable<? extends IStatement> statements) {
		try {
			Iterator<? extends IStatement> it = statements.iterator();

			RepositoryConnection conn = getConnection();
			while (it.hasNext()) {
				IStatement stmt = it.next();

				conn.add(getResource(stmt.getSubject()),
						(URI) getResource(stmt.getPredicate()),
						getValue(stmt.getObject()));
			}
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	private <C extends Collection<URI>> C addConcept(Resource resource,
			Class<?> role, C set) throws StoreException {
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

	private Resource assignResource(Object bean) {
		synchronized (merged) {
			if (merged.containsKey(bean)) {
				return merged.get(bean);
			}
			Resource resource = getResource(bean);
			if (resource == null) {
				resource = resourceManager.createResource(null);
			}
			merged.put(bean, resource);
			return resource;
		}
	}

	@Override
	public void clear() {
		try {
			getConnection().clear();
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public void clearNamespaces() {
		try {
			uriToPrefix = null;
			getConnection().clearNamespaces();
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public void close() {
		try {
			if (conn != null && !transaction.isActive())
				conn.close();
			conn = null;
		} catch (StoreException e) {
			throw new KommaException(e);
		}
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
		if (entity instanceof ISesameEntity) {
			ISesameEntity se = (ISesameEntity) entity;
			return this.equals(se.getSesameManager());
		} else if (entity instanceof Behaviour<?>) {
			Behaviour<?> behaviour = (Behaviour<?>) entity;
			Object delegate = behaviour.getBehaviourDelegate();
			if (delegate instanceof ISesameEntity) {
				ISesameEntity se = (ISesameEntity) delegate;
				return this.equals(se.getSesameManager());
			}
		}
		return false;
	}

	@Override
	public Object convertValue(ILiteral literal) {
		return getInstance(getValue((Object) literal), null);
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

	@Override
	@SuppressWarnings("unchecked")
	public <T> T create(Resource resource, Class<T> concept,
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
		} catch (StoreException e) {
			if (!isActive && getTransaction().isActive()) {
				getTransaction().rollback();
			}
		}

		IEntity bean = createBean(resource, null, null);
		assert assertConceptsRecorded(bean, combine(concept, concepts));
		return (T) bean;
	}

	@SuppressWarnings("unchecked")
	public ISesameEntity createBean(Resource resource, Collection<URI> types,
			Model model) {
		try {
			if (types == null || types.isEmpty()) {
				if (model != null) {
					types = (Collection<URI>) (Collection<?>) model.filter(
							resource, RDF.TYPE, null).objects();
				}

				if (types == null || types.isEmpty()) {
					types = typeManager.getTypes(resource);
				}
			}

			ISesameEntity bean = createBeanForClass(resource,
					classResolver.resolveComposite(types));
			if (model != null) {
				initializeBean(bean, model);
			}
			return bean;
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	protected ISesameEntity createBeanForClass(Resource resource, Class<?> type) {
		Object obj;
		try {
			obj = type.newInstance();

			injector.injectMembers(obj);

			assert obj instanceof ISesameManagerAware : "core roles are not registered, check your deployed classpath";
			ISesameManagerAware bean = (ISesameManagerAware) obj;
			bean.initSesameReference(((SesameManagerFactory) getFactory())
					.getReference(resource));
			return bean;
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
		Resource resource = resourceManager.createResource(uri);
		return create(resource, concept, concepts);
	}

	@Override
	public IEntity createNamed(net.enilink.komma.core.URI uri,
			IReference... concepts) {
		Resource resource = resourceManager.createResource(uri);
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit) {
				conn.begin();
			}
			for (IReference concept : concepts) {
				conn.add(new StatementImpl(resource, RDF.TYPE,
						getResource(concept)));
			}
			if (autoCommit) {
				conn.commit();
			}
		} catch (StoreException e) {
			throw new KommaException(e);
		}
		return createBean(resource, null, null);
	}

	public SesameQuery<?> createQuery(String query) {
		return createQuery(query, null);
	}

	public SesameQuery<?> createQuery(String query, String baseURI) {
		try {
			org.openrdf.query.Query qry = getConnection().prepareQuery(
					QueryLanguage.SPARQL, query, baseURI);

			System.out.println("query: " + query);

			return createSesameQuery(qry);
		} catch (MalformedQueryException e) {
			throw new KommaException(e);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	public Iterator<Resource> createRoleQuery(Class<?> concept) {
		StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT DISTINCT ?subj WHERE {");
		appendFilter(concept, querySb);
		querySb.append("}");
		String queryStr = querySb.toString();
		try {
			TupleQuery query = conn.prepareTupleQuery(queryStr);

			TupleResult result = query.evaluate();

			final String binding = result.getBindingNames().get(0);
			return new SesameIterator<BindingSet, Resource>(result) {
				@Override
				protected Resource convert(BindingSet sol) {
					Value value = sol.getValue(binding);
					assert value instanceof Resource : value;
					return (Resource) value;
				}
			};
		} catch (MalformedQueryException e) {
			throw new KommaException(e);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	protected SesameQuery<?> createSesameQuery(org.openrdf.query.Query query) {
		SesameQuery<?> sesameQuery = new SesameQuery<Object>(query);
		injector.injectMembers(sesameQuery);

		return sesameQuery;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T designateEntity(Object entity, Class<T> concept,
			Class<?>... concepts) {
		Resource resource = getResource(entity);
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
		} catch (StoreException e) {
			if (!isActive && getTransaction().isActive()) {
				getTransaction().rollback();
			}
		}

		IEntity bean = createBean(resource, types, null);
		assert assertConceptsRecorded(bean, concepts);
		return (T) bean;
	}

	protected IExtendedIterator<Value> filterResultNode(Set<Value> values) {
		return WrappedIterator.create(values.iterator()).filterDrop(
				new Filter<Value>() {
					@Override
					public boolean accept(Value o) {
						return RESULT_NODE.equals(o);
					}
				});
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T find(net.enilink.komma.core.URI uri,
			Class<T> concept, Class<?>... concepts) {
		Class<?>[] allConcepts = Arrays.copyOf(concepts, concepts.length + 1);
		allConcepts[allConcepts.length - 1] = concept;
		return (T) find(resourceManager.createResource(uri), allConcepts);
	}

	@Override
	public ISesameEntity find(IReference reference) {
		return find(getResource(reference));
	}

	@Override
	public ISesameEntity find(Resource resource) {
		return createBean(resource, null, null);
	}

	@Override
	public ISesameEntity find(Resource resource, Class<?>... concepts) {
		Set<URI> types = new LinkedHashSet<URI>(concepts.length);
		for (int i = 0; i < concepts.length; i++) {
			URI type = mapper.findType(concepts[i]);
			if (type != null) {
				types.add(type);
			} else {
				logger.warn("Unknown rdf type for concept class: "
						+ concepts[i]);
			}
		}
		try {
			types.addAll(typeManager.getTypes(resource));
		} catch (StoreException e) {
			throw new KommaException(e);
		}
		return createBean(resource, types, null);
	}

	public <T> IExtendedIterator<T> findAll(final Class<T> concept) {
		Iterator<Resource> iter = createRoleQuery(concept);
		return new ConvertingIterator<Resource, T>(iter) {
			@Override
			@SuppressWarnings("unchecked")
			public T convert(Resource resource) {
				return (T) find(resource);
			}
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T findRestricted(net.enilink.komma.core.URI uri,
			Class<T> concept, Class<?>... concepts) {
		Class<?>[] allConcepts = Arrays.copyOf(concepts, concepts.length + 1);
		allConcepts[allConcepts.length - 1] = concept;
		return (T) findRestricted(resourceManager.createResource(uri),
				allConcepts);
	}

	@Override
	public IReference findRestricted(Resource resource, Class<?>... concepts) {
		// if type is restricted to IReference then simply return the
		// corresponding reference value
		if (concepts.length == 1
				&& (IValue.class.equals(concepts[0]) || IReference.class
						.equals(concepts[0]))) {
			return ((SesameManagerFactory) getFactory()).getReference(resource);
		}

		List<URI> types = new ArrayList<URI>(concepts.length);
		for (int i = 0; i < concepts.length; i++) {
			types.add(mapper.findType(concepts[i]));
		}
		return createBean(resource, types, null);
	}

	@Override
	public void flush() {
	}

	@Override
	public ContextAwareConnection getConnection() {
		if (!isOpen())
			throw new IllegalStateException("Connection has been closed");
		return conn;
	}

	@Override
	public SesameManagerFactory getFactory() {
		return factory;
	}

	@Override
	public FlushModeType getFlushMode() {
		return FlushModeType.AUTO;
	}

	@Override
	public InferencingCapability getInferencing() {
		if (inferencing == null) {
			try {
				RepositoryMetaData metaData = getConnection().getRepository()
						.getMetaData();
				final boolean doesOWL = metaData.isOWLInferencing();
				final boolean doesRDFS = metaData.isRDFSInferencing()
						|| metaData.isInferencing();

				inferencing = new InferencingCapability() {
					@Override
					public boolean doesOWL() {
						return doesOWL;
					}

					@Override
					public boolean doesRDFS() {
						return doesRDFS;
					}
				};
			} catch (StoreException e) {
				KommaCore.log(e);
			}
		}

		return inferencing;
	}

	@Override
	public Object getInstance(Value value, Class<?> type) {
		return getInstance(value, type, null);
	}

	protected Object getInstance(Value value, Class<?> type, Model model) {
		if (value instanceof Resource) {
			Collection<URI> types = Collections.emptyList();
			if (type != null) {
				URI typeUri = mapper.findType(type);
				if (typeUri != null) {
					// ensure that specified type is added as role to resulting
					// object
					types = new ArrayList<URI>();
					types.add(typeUri);

					try {
						types.addAll(typeManager.getTypes((Resource) value));
					} catch (StoreException e) {
						throw new KommaException(e);
					}
				}
			}

			ISesameEntity bean = createBean((Resource) value, types, model);
			if (logger.isDebugEnabled()) {
				try {
					if (!getConnection().hasMatch((Resource) value, null, null))
						logger.debug("Warning: Unknown entity: " + value);
				} catch (StoreException e) {
					throw new KommaException(e);
				}
			}
			return bean;
		}

		Literal literal = (Literal) value;
		net.enilink.komma.core.URI datatype = literal.getDatatype() == null ? null
				: net.enilink.komma.core.URIImpl.createURI(literal
						.getDatatype().toString());
		Object instance = literalConverter.createObject(literal.getLabel(),
				datatype);
		if (type != null) {
			if (IValue.class.equals(type) || ILiteral.class.equals(type)) {
				instance = createLiteral(value, datatype, literal.getLanguage());
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

	protected List<Object> getInstances(Iterator<Value> values, Class<?> type,
			Model model) {
		List<Object> instances = new ArrayList<Object>();
		while (values.hasNext()) {
			instances.add(getInstance(values.next(), type, model));
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
	public SesameManagerFactory getManagerFactory() {
		return factory;
	}

	@Override
	public net.enilink.komma.core.URI getNamespace(String prefix) {
		try {
			String namespaceURI = getConnection().getNamespace(prefix);
			if (namespaceURI != null) {
				return net.enilink.komma.core.URIImpl
						.createURI(namespaceURI);
			}
			return null;
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public IExtendedIterator<INamespace> getNamespaces() {
		try {
			return new SesameIterator<Namespace, INamespace>(getConnection()
					.getNamespaces()) {
				@Override
				protected INamespace convert(Namespace element)
						throws Exception {
					try {
						return new net.enilink.komma.core.Namespace(
								element.getPrefix(), element.getName());
					} catch (IllegalArgumentException e) {
						return null;
					}
				}
			}.filterDrop(new Filter<INamespace>() {
				@Override
				public boolean accept(INamespace ns) {
					return ns == null;
				}
			});
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public String getPrefix(net.enilink.komma.core.URI namespaceUri) {
		if (uriToPrefix == null) {
			uriToPrefix = new HashMap<net.enilink.komma.core.URI, String>();
			for (INamespace namespace : getNamespaces()) {
				uriToPrefix.put(namespace.getUri(), namespace.getPrefix());
			}
		}
		return uriToPrefix.get(namespaceUri);
	}

	@Override
	public Map<String, Object> getProperties() {
		return Collections.emptyMap();
	}

	protected Resource getResource(Object bean) {
		if (bean instanceof Resource) {
			return (Resource) bean;
		} else if (bean instanceof net.enilink.komma.core.URI) {
			return new URIImpl(bean.toString());
		} else if (bean instanceof ISesameResourceAware) {
			return ((ISesameResourceAware) bean).getSesameResource();
		} else if (bean instanceof Behaviour<?>) {
			Behaviour<?> behaviour = (Behaviour<?>) bean;
			Object entity = behaviour.getBehaviourDelegate();
			if (entity instanceof ISesameResourceAware) {
				return ((ISesameEntity) entity).getSesameResource();
			}
		}
		return null;
	}

	private Resource getResourceOrFail(Object entity) {
		Resource resource = getResource(entity);
		if (resource == null) {
			throw new KommaException("Unknown Entity: " + entity);
		}
		return resource;
	}

	@Override
	public RoleMapper<URI> getRoleMapper() {
		return mapper;
	}

	@Override
	public Set<String> getSupportedProperties() {
		return Collections.emptySet();
	}

	@Override
	public IKommaTransaction getTransaction() {
		return transaction;
	}

	private <C extends Collection<URI>> C getTypes(Class<?> role, C set)
			throws StoreException {
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
	public Value getValue(Object instance) {
		if (instance == null) {
			return null;
		}

		Resource resource = getResource(instance);
		if (resource != null) {
			return resource;
		}

		if (instance instanceof ILiteral) {
			ILiteral literal = (ILiteral) instance;
			instance = literal.getValue();

			if (literal.getDatatype() != null) {
				getConnection().getValueFactory().createLiteral(
						literal.getLabel(),
						URIUtil.toSesameUri(literal.getDatatype()));
			}
			return getConnection().getValueFactory().createLiteral(
					literal.getLabel(), literal.getLanguage());
		}
		Class<?> type = instance.getClass();
		if (literalConverter.isDatatype(type)) {
			ILiteral literal = literalConverter.createLiteral(instance, null);

			net.enilink.komma.core.URI datatype = literal
					.getDatatype();
			return getConnection().getValueFactory().createLiteral(
					literal.getLabel(),
					datatype == null ? null : URIUtil.toSesameUri(datatype));
		}
		synchronized (merged) {
			if (merged.containsKey(instance)) {
				return merged.get(instance);
			}
		}
		if (IEntity.class.isAssignableFrom(type) || isEntity(type)) {
			return getValue(merge(instance));
		}

		return getConnection().getValueFactory().createLiteral(
				String.valueOf(instance),
				URIUtil.toSesameUri(XMLSCHEMA.TYPE_STRING));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void initializeBean(ISesameEntity bean, Model model) {
		if (!model.contains(bean.getSesameResource(), null, null)) {
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

					if (model.contains(bean.getSesameResource(), new URIImpl(
							Iri.value()), null)) {
						Set<Value> objects = new LinkedHashSet<Value>(model
								.filter(bean.getSesameResource(),
										new URIImpl(Iri.value()), null)
								.objects());
						model.remove(bean.getSesameResource(),
								new URIImpl(Iri.value()), null);

						Object result = method.invoke(bean);
						if (result instanceof PropertySet<?>) {
							((PropertySet) result).init(getInstances(
									filterResultNode(objects),
									((PropertySet) result).getElementType(),
									model));
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
						URI keyUri = new URIImpl(cacheable.key());
						if (model.contains(bean.getSesameResource(), keyUri,
								null)) {
							Set<Value> objects = new LinkedHashSet<Value>(model
									.filter(bean.getSesameResource(), keyUri,
											null).objects());
							model.remove(bean.getSesameResource(), keyUri, null);

							IExtendedIterator<Value> valuesIt = filterResultNode(objects);

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
										valueType, model));

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

	protected void initializeCache(ISesameEntity entity, Object property,
			Object value) {
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
		try {
			return conn != null && conn.isOpen();
		} catch (StoreException e) {
			throw new KommaException(e);
		}
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
			Resource resource = assignResource(bean);

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
				Object result = createBean(resource, types, null);
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

	public void remove(Object entity) {
		Resource resource = getResourceOrFail(entity);
		resourceManager.removeResource(resource);
	}

	@Override
	public void remove(Iterable<? extends IStatement> statements) {
		try {
			Iterator<? extends IStatement> it = statements.iterator();

			RepositoryConnection conn = getConnection();
			while (it.hasNext()) {
				IStatement stmt = it.next();

				conn.removeMatch(getResource(stmt.getSubject()),
						(URI) getResource(stmt.getPredicate()),
						getValue(stmt.getObject()));
			}
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public void removeDesignation(Object entity, Class<?>... concepts) {
		Resource resource = getResourceOrFail(entity);

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
		} catch (StoreException e) {
			if (!isActive && getTransaction().isActive()) {
				getTransaction().rollback();
			}
		}
	}

	@Override
	public void removeNamespace(String prefix) {
		try {
			uriToPrefix = null;
			getConnection().removeNamespace(prefix);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public <T> T rename(T bean, net.enilink.komma.core.URI uri) {
		Resource after = resourceManager.createResource(uri);
		return rename(bean, after);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T rename(T bean, Resource dest) {
		Resource before = getResourceOrFail(bean);
		resourceManager.renameResource(before, dest);
		return (T) createBean(dest, null, null);
	}

	@Inject
	protected void setClassResolver(ClassResolver<URI> classResolver) {
		this.classResolver = classResolver;
	}

	@Override
	@Inject
	public void setConnection(ContextAwareConnection connection) {
		this.conn = connection;
		this.transaction = new SesameTransaction(conn);
	}

	@Override
	public void setFlushMode(FlushModeType mode) {
	}

	@Inject
	protected void setManagerFactory(SesameManagerFactory factory) {
		this.factory = factory;
	}

	@Override
	public void setNamespace(String prefix,
			net.enilink.komma.core.URI uri) {
		try {
			uriToPrefix = null;
			getConnection().setNamespace(prefix, uri.toString());
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public void setProperty(String propertyName, Object value) {

	}

	@Inject
	protected void setResourceManager(SesameResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}

	@Inject
	protected void setRoleMapper(RoleMapper<URI> mapper) {
		this.mapper = mapper;
	}

	@Inject
	protected void setTypeManager(SesameTypeManager typeManager) {
		this.typeManager = typeManager;
	}
}
