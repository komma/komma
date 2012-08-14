package net.enilink.komma.internal.sesame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.openrdf.repository.RepositoryException;

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
	private static final URI[] EMPTY_URIS = new URI[0];

	@Inject
	protected IDataChangeSupport changeSupport;

	protected RepositoryConnection connection;

	@Inject(optional = true)
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
		} catch (Exception e) {
			throw new KommaException(e);
		}
		this.changeSupport = changeSupport;
		this.transaction = new SesameTransaction(this, changeSupport);
	}

	@Override
	public IDataManager add(Iterable<? extends IStatement> statements,
			IReference[] readContexts, IReference... addContexts) {
		URI[] readCtx = toURI(readContexts);
		URI[] addCtx = toURI(addContexts);
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
					if (!conn.hasStatement(subject, predicate, object, false,
							readCtx)) {
						changeSupport.add(this, stmt.getSubject(),
								stmt.getPredicate(), (IValue) stmt.getObject(),
								addContexts);
					}
				}
				conn.add(subject, predicate, object, addCtx);
			}
			if (changeSupport.isEnabled(this) && !getTransaction().isActive()) {
				changeSupport.commit(this);
			}
		} catch (Exception e) {
			throw new KommaException(e);
		}
		return this;
	}

	@Override
	public IDataManager add(Iterable<? extends IStatement> statements,
			IReference... addContexts) {
		return add(statements, addContexts, addContexts);
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
		} catch (Exception e) {
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
		} catch (Exception e) {
			throw new KommaException(e);
		} finally {
			connection = null;
		}
	}

	protected Query prepareSesameQuery(String query, String baseURI,
			boolean includeInferred) throws MalformedQueryException,
			RepositoryException {
		return getConnection().prepareQuery(QueryLanguage.SPARQL, query,
				baseURI);
	}

	@Override
	public <R> IDataManagerQuery<R> createQuery(String query, String baseURI,
			boolean includeInferred, IReference... contexts) {
		try {
			Query sesameQuery = prepareSesameQuery(query, baseURI,
					includeInferred);
			if (contexts.length > 0) {
				DatasetImpl ds = new DatasetImpl();
				for (URI graph : toURI(contexts)) {
					ds.addDefaultGraph(graph);
					if (graph != null) {
						ds.addNamedGraph(graph);
					}
				}
				sesameQuery.setDataset(ds);
			}
			sesameQuery.setIncludeInferred(includeInferred);

			SesameQuery<R> result = new SesameQuery<R>(sesameQuery);
			injector.injectMembers(result);
			return result;
		} catch (RepositoryException e) {
			throw new KommaException(e);
		} catch (MalformedQueryException e) {
			throw new KommaException("Invalid query format", e);
		}
	}

	protected RepositoryConnection getConnection() {
		return connection;
	}

	@Override
	public InferencingCapability getInferencing() {
		if (inferencing == null) {
			try {
				inferencing = new InferencingCapability() {
					@Override
					public boolean doesOWL() {
						return false;
					}

					@Override
					public boolean doesRDFS() {
						// assume that RDFS is supported
						return true;
					}
				};
			} catch (Exception e) {
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
		} catch (Exception e) {
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
		} catch (Exception e) {
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
			return repository.getValueFactory().createURI(reference.toString());
		}
		return null;
	}

	protected URI[] toURI(IReference... references) {
		if (references.length == 0) {
			return EMPTY_URIS;
		}
		List<URI> uris = new ArrayList<URI>(references.length);
		for (IReference ref : references) {
			if (ref == null) {
				uris.add(null);
			} else {
				Resource resource = getResource(ref);
				if (resource instanceof URI) {
					uris.add((URI) resource);
				}
			}
		}
		return uris.toArray(new URI[uris.size()]);
	}

	@Override
	public ITransaction getTransaction() {
		return transaction;
	}

	@Override
	public boolean hasMatch(IReference subject, IReference predicate,
			IValue object, boolean includeInferred, IReference... contexts) {
		try {
			return getConnection().hasStatement(
					(Resource) valueConverter.toSesame(subject),
					(URI) valueConverter.toSesame(predicate),
					valueConverter.toSesame(object), includeInferred,
					toURI(contexts));
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	@Override
	public boolean isOpen() {
		return connection != null;
	}

	@Override
	public IExtendedIterator<IStatement> match(IReference subject,
			IReference predicate, IValue object, boolean includeInferred,
			IReference... contexts) {
		try {
			SesameGraphResult result = new SesameGraphResult(getConnection()
					.getStatements((Resource) valueConverter.toSesame(subject),
							(URI) valueConverter.toSesame(predicate),
							valueConverter.toSesame(object), includeInferred,
							toURI(contexts)));
			injector.injectMembers(result);
			return result;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	@Override
	public IReference newBlankNode() {
		return new SesameReference(getConnection().getValueFactory()
				.createBNode());
	}

	@Override
	public IDataManager remove(Iterable<? extends IStatement> statements,
			IReference... contexts) {
		URI[] removeContexts = toURI(contexts);
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
					for (IStatement existing : match(stmt.getSubject(),
							stmt.getPredicate(), (IValue) stmt.getObject(),
							false, contexts)) {
						// pretend that statement was in changeCtx if no context
						// is set
						IReference[] changeContexts = existing.getContext() != null ? new IReference[] { existing
								.getContext() } : contexts;
						changeSupport.remove(this, existing.getSubject(),
								existing.getPredicate(),
								(IValue) existing.getObject(), changeContexts);
					}
				}
				conn.remove(subject, predicate, object, removeContexts);
			}
			if (changeSupport.isEnabled(this) && !getTransaction().isActive()) {
				changeSupport.commit(this);
			}
		} catch (Exception e) {
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
		} catch (Exception e) {
			throw new KommaException(e);
		}
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
		} catch (Exception e) {
			throw new KommaException(e);
		}
		return this;
	}
}
