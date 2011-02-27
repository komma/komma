package net.enilink.komma.internal.sesame;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryMetaData;
import org.openrdf.store.StoreException;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.enilink.commons.iterator.Filter;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerQuery;
import net.enilink.komma.dm.change.IDataChangeSupport;
import net.enilink.komma.internal.sesame.result.SesameGraphResult;
import net.enilink.komma.internal.sesame.result.SesameResult;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IReferenceable;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.InferencingCapability;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.sesame.SesameValueConverter;

public class SesameRepositoryDataManager implements IDataManager {
	private static final URI[] ALL_CONTEXTS = new URI[0];

	protected URI[] addCtx = ALL_CONTEXTS;
	
	protected URI[] readCtx = ALL_CONTEXTS;

	protected net.enilink.komma.core.URI[] changeCtx;
	
	@Inject
	protected IDataChangeSupport changeSupport;

	protected RepositoryConnection connection;

	private boolean includeInferred = true;

	protected InferencingCapability inferencing;

	@Inject
	protected Injector injector;

	@Inject
	protected Repository repository;

	protected SesameTransaction transaction;

	@Inject
	protected SesameValueConverter valueConverter;

	@Inject
	public SesameRepositoryDataManager(Repository repository,
			IDataChangeSupport changeSupport) {
		try {
			connection = repository.getConnection();
		} catch (StoreException e) {
			throw new KommaException(e);
		}
		this.changeSupport = changeSupport;
		this.transaction = new SesameTransaction(this, changeSupport);
	}

	@Override
	public IDataManager add(Iterable<? extends IStatement> statements) {
		try {
			Iterator<? extends IStatement> it = statements.iterator();

			RepositoryConnection conn = getConnection();
			while (it.hasNext()) {
				IStatement stmt = it.next();

				Resource subject = getResource(stmt.getSubject());
				URI predicate = (URI) getResource(stmt.getPredicate());
				Value object = valueConverter.toSesame((IValue) stmt
						.getObject());

				if (changeSupport.isEnabled(this)) {
					if (!conn.hasMatch(subject, predicate, object, false,
							readCtx)) {
						changeSupport.add(this, stmt.getSubject(),
								stmt.getPredicate(), (IValue) stmt.getObject(),
								changeCtx);
					}
				}
				conn.add(subject, predicate, object, addCtx);
			}
			if (changeSupport.isEnabled(this) && !getTransaction().isActive()) {
				changeSupport.commit(this);
			}
		} catch (StoreException e) {
			throw new KommaException(e);
		}
		return this;
	}

	@Override
	public IDataManager clearNamespaces() {
		try {
			if (changeSupport.isEnabled(this)) {
				for (INamespace namespace : getNamespaces()) {
					changeSupport.removeNamespace(this, namespace.getPrefix(),
							namespace.getURI());
				}
				changeSupport.commit(this);
			}

			getConnection().clearNamespaces();
		} catch (StoreException e) {
			throw new KommaException(e);
		}
		return this;
	}

	@Override
	public void close() {
		if (connection == null) {
			return;
		}
		try {
			connection.close();
		} catch (StoreException e) {
			throw new KommaException(e);
		} finally {
			connection = null;
		}
	}

	@Override
	public <R> IDataManagerQuery<R> createQuery(String query, String baseURI) {
		try {
			Query sesameQuery = getConnection().prepareQuery(
					QueryLanguage.SPARQL, query, baseURI);

			if (readCtx.length > 0) {
				DatasetImpl ds = new DatasetImpl();
				for (URI graph : readCtx) {
					ds.addDefaultGraph(graph);
					ds.addNamedGraph(graph);
				}
				sesameQuery.setDataset(ds);
			}
			sesameQuery.setIncludeInferred(includeInferred);

			SesameQuery<R> result = new SesameQuery<R>(sesameQuery);
			injector.injectMembers(result);
			return result;
		} catch (StoreException e) {
			throw new KommaException(e);
		} catch (MalformedQueryException e) {
			throw new KommaException("Invalid query format", e);
		}
	}

	RepositoryConnection getConnection() {
		return connection;
	}

	@Override
	public boolean getIncludeInferred() {
		return includeInferred;
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
				throw new KommaException(
						"Error while determining inferencing capabilities", e);
			}
		}
		return inferencing;
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
			return new SesameResult<Namespace, INamespace>(getConnection()
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

	protected Resource getResource(IReference reference) {
		if (reference instanceof IReferenceable) {
			reference = ((IReferenceable) reference).getReference();
		}
		if (reference instanceof SesameReference) {
			return ((SesameReference) reference).getSesameResource();
		} else if (reference instanceof net.enilink.komma.core.URI) {
			return repository.getURIFactory().createURI(reference.toString());
		}
		return null;
	}

	@Override
	public ITransaction getTransaction() {
		return transaction;
	}

	@Override
	public boolean hasMatch(IReference subject, IReference predicate,
			IValue object) {
		try {
			return getConnection().hasMatch(
					(Resource) valueConverter.toSesame(subject),
					(URI) valueConverter.toSesame(predicate),
					valueConverter.toSesame(object), includeInferred, readCtx);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public boolean isOpen() {
		return connection != null;
	}

	@Override
	public IExtendedIterator<IStatement> match(IReference subject,
			IReference predicate, IValue object) {
		try {
			SesameGraphResult result = new SesameGraphResult(getConnection()
					.match((Resource) valueConverter.toSesame(subject),
							(URI) valueConverter.toSesame(predicate),
							valueConverter.toSesame(object), includeInferred,
							readCtx));
			injector.injectMembers(result);
			return result;
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public IExtendedIterator<IStatement> matchAsserted(IReference subject,
			IReference predicate, IValue object) {
		try {
			SesameGraphResult result = new SesameGraphResult(getConnection()
					.match((Resource) valueConverter.toSesame(subject),
							(URI) valueConverter.toSesame(predicate),
							valueConverter.toSesame(object), false, readCtx));
			injector.injectMembers(result);
			return result;
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public IReference newBlankNode() {
		return new SesameReference(getConnection().getValueFactory()
				.createBNode());
	}

	@Override
	public IDataManager remove(Iterable<? extends IStatement> statements) {
		try {
			Iterator<? extends IStatement> it = statements.iterator();

			RepositoryConnection conn = getConnection();
			while (it.hasNext()) {
				IStatement stmt = it.next();

				Resource subject = getResource(stmt.getSubject());
				URI predicate = (URI) getResource(stmt.getPredicate());
				Value object = valueConverter.toSesame((IValue) stmt
						.getObject());

				if (changeSupport.isEnabled(this)) {
					for (IStatement existing : matchAsserted(stmt.getSubject(),
							stmt.getPredicate(), (IValue) stmt.getObject())) {
						changeSupport.remove(this, existing.getSubject(),
								existing.getPredicate(),
								(IValue) existing.getObject(),
								existing.getContext());
					}
				}
				conn.removeMatch(subject, predicate, object, addCtx);
			}
			if (changeSupport.isEnabled(this) && !getTransaction().isActive()) {
				changeSupport.commit(this);
			}
		} catch (StoreException e) {
			throw new KommaException(e);
		}
		return this;
	}

	@Override
	public IDataManager removeNamespace(String prefix) {
		try {
			if (changeSupport.isEnabled(this)) {
				String namespace = getConnection().getNamespace(prefix);
				if (namespace != null) {
					changeSupport.removeNamespace(this, prefix,
							net.enilink.komma.core.URIImpl
									.createURI(namespace));
				}
			}

			getConnection().removeNamespace(prefix);

			if (changeSupport.isEnabled(this)) {
				changeSupport.commit(this);
			}
		} catch (StoreException e) {
			throw new KommaException(e);
		}
		return this;
	}

	@Override
	public IDataManager setAddContexts(
			Set<net.enilink.komma.core.URI> addContexts) {
		this.changeCtx = addContexts
				.toArray(new net.enilink.komma.core.URI[addContexts
						.size()]);
		this.addCtx = toURI(addContexts);
		return this;
	}

	@Override
	public IDataManager setIncludeInferred(boolean includeInferred) {
		this.includeInferred = includeInferred;
		return this;
	}

	@Override
	public IDataManager setNamespace(String prefix,
			net.enilink.komma.core.URI uri) {
		try {
			if (changeSupport.isEnabled(this)) {
				net.enilink.komma.core.URI existing = getNamespace(prefix);
				changeSupport.setNamespace(this, prefix, existing, uri);
			}

			getConnection().setNamespace(prefix, uri.toString());

			if (changeSupport.isEnabled(this)) {
				changeSupport.commit(this);
			}
		} catch (StoreException e) {
			throw new KommaException(e);
		}
		return this;
	}

	@Override
	public IDataManager setReadContexts(
			Set<net.enilink.komma.core.URI> readContexts) {
		this.readCtx = toURI(readContexts);
		return this;
	}

	protected URI[] toURI(Collection<net.enilink.komma.core.URI> uris) {
		URI[] converted = new URI[uris.size()];

		int i = 0;
		for (net.enilink.komma.core.URI uri : uris) {
			converted[i++] = valueConverter.toSesame(uri);
		}
		return converted;
	}
}
