package net.enilink.komma.internal.sesame;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import net.enilink.commons.iterator.Filter;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IStatementPattern;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.InferencingCapability;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URIs;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerQuery;
import net.enilink.komma.dm.IDataManagerUpdate;
import net.enilink.komma.dm.change.IDataChangeSupport;
import net.enilink.komma.internal.sesame.result.SesameGraphResult;
import net.enilink.komma.internal.sesame.result.SesameResult;
import net.enilink.komma.sesame.SesameValueConverter;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Operation;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class SesameRepositoryDataManager implements IDataManager {
	protected static final IReference[] NULL_CTX = { null };

	protected IDataChangeSupport changeSupport;

	protected RepositoryConnection connection;

	@Inject(optional = true)
	protected volatile InferencingCapability inferencing;

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

	protected IReference[] addNullContext(boolean includeInferred,
			IReference[] contexts) {
		if (includeInferred && getInferencing().inDefaultGraph()) {
			for (IReference ctx : contexts) {
				if (ctx == null) {
					return contexts;
				}
			}
			contexts = Arrays.copyOf(contexts, contexts.length + 1);
			contexts[contexts.length - 1] = null;
		}
		return contexts;
	}

	@Override
	public IDataManager add(Iterable<? extends IStatement> statements,
			IReference[] readContexts, IReference... addContexts) {
		if (addContexts.length == 0) {
			addContexts = NULL_CTX;
		}
		URI[] readCtx = valueConverter.toSesameURI(readContexts);
		URI[] addCtx = valueConverter.toSesameURI(addContexts);
		try {
			Iterator<? extends IStatement> it = statements.iterator();

			RepositoryConnection conn = getConnection();
			boolean trackChanges = changeSupport.isEnabled(this);
			while (it.hasNext()) {
				IStatement stmt = it.next();

				Resource subject = valueConverter.toSesame(stmt.getSubject());
				URI predicate = (URI) valueConverter.toSesame(stmt
						.getPredicate());
				Value object = valueConverter.toSesame((IValue) stmt
						.getObject());

				if (trackChanges) {
					if (!conn.hasStatement(subject, predicate, object, false,
							readCtx)) {
						for (IReference ctx : addContexts) {
							changeSupport.add(
									this,
									new Statement(stmt.getSubject(), stmt
											.getPredicate(), stmt.getObject(),
											ctx, stmt.isInferred()));
						}
					}
				}
				if (!stmt.isInferred()) {
					conn.add(subject, predicate, object, addCtx);
				}
			}
			if (!getTransaction().isActive()) {
				clearNodeMappings();
				if (changeSupport.isEnabled(this)) {
					changeSupport.commit(this);
				}
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

	protected String ensureBindingsInGraph(String query, IReference[] contexts) {
		if (contexts.length == 0) {
			return query;
		}
		boolean allNull = true;
		for (int i = 0; allNull && i < contexts.length; i++) {
			allNull &= contexts[i] == null;
		}
		if (allNull) {
			return query;
		}
		Set<String> variables = new LinkedHashSet<String>();
		String delim = " ,)(;.{}";
		int i = 0;
		int qlen = query.length();
		int open = 0, closed = 0;
		while (i < qlen) {
			int ch = query.charAt(i++);
			if (ch == '{') {
				if (variables.size() > 0) {
					// only projection part of select queries is investigated
					break;
				}
				open++;
			} else if (ch == '}') {
				closed++;
			} else if (ch == '?' || ch == '$') { // variable
				int j = i;
				while (j < qlen && delim.indexOf(query.charAt(j)) < 0) {
					j++;
				}
				if (j != i) {
					String varName = query.substring(i - 1, j).trim();
					variables.add(varName);
				}
			}
			if (open > 0 && open == closed) {
				// only first group pattern is investigated
				// ASK { this one }, CONSTRUCT { this one } WHERE { ... }
				break;
			}
		}
		// ensure that at least one statement from the data set mentions a
		// selected variable
		int lastBrace = query.lastIndexOf('}');
		StringBuilder sb = new StringBuilder(query.substring(0, lastBrace));
		int n = 0, g = 0;
		if (!variables.isEmpty()) {
			sb.append("\nfilter (");
			for (Iterator<String> varIt = variables.iterator(); varIt.hasNext();) {
				String var = varIt.next();

				sb.append("\n(!bound(").append(var).append(") || isLiteral(")
						.append(var).append(") || exists { graph ?__g")
						.append(g++).append(" {\n");
				sb.append("\t{ ").append(var).append(" ?__p").append(n++)
						.append(" ?__o").append(n++).append(" } UNION ");
				sb.append("{ ?__s").append(n++).append(" ?__p").append(n++)
						.append(" ").append(var).append(" } UNION ");
				sb.append("{ ?__s").append(n++).append(" ").append(var)
						.append(" ?__o").append(n++).append(" }\n");
				sb.append("} })\n");

				if (varIt.hasNext()) {
					sb.append(" && ");
				}
			}
			sb.append(")");
		}
		sb.append(query.substring(lastBrace));
		return sb.toString();
	}

	protected void setDataset(Operation operation, IReference[] readContexts,
			IReference... modifyContexts) {
		if (readContexts.length > 0 || modifyContexts.length > 0) {
			operation.setDataset(valueConverter.createDataset(readContexts,
					modifyContexts));
		}
	}

	@Override
	public <R> IDataManagerQuery<R> createQuery(String query, String baseURI,
			boolean includeInferred, IReference... contexts) {
		contexts = addNullContext(includeInferred, contexts);
		try {
			// query = ensureBindingsInGraph(query, contexts);
			Query sesameQuery = prepareSesameQuery(query, baseURI,
					includeInferred);
			setDataset(sesameQuery, contexts);
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

	@Override
	public IDataManagerUpdate createUpdate(final String update, String baseURI,
			final boolean includeInferred, final IReference... contexts) {
		return createUpdate(update, baseURI, includeInferred, contexts,
				contexts);
	}

	@Override
	public IDataManagerUpdate createUpdate(String update, String baseURI,
			boolean includeInferred, IReference[] readContexts,
			IReference... modifyContexts) {
		readContexts = addNullContext(includeInferred, readContexts);
		if (changeSupport.isEnabled(this)) {
			return new SesameUpdate(this, update, baseURI, includeInferred,
					readContexts, modifyContexts);
		} else {
			try {
				Update updateOp = getConnection().prepareUpdate(
						QueryLanguage.SPARQL, update);
				setDataset(updateOp, readContexts, modifyContexts);
				updateOp.setIncludeInferred(includeInferred);
				return new SesameUpdateRemote(updateOp);
			} catch (Exception e) {
				throw new KommaException(e);
			}
		}

	}

	protected RepositoryConnection getConnection() {
		return connection;
	}

	@Override
	public InferencingCapability getInferencing() {
		if (inferencing == null) {
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

				@Override
				public boolean inDefaultGraph() {
					return true;
				}
			};
		}
		return inferencing;
	}

	@Override
	public net.enilink.komma.core.URI getNamespace(String prefix) {
		try {
			String namespaceURI = getConnection().getNamespace(prefix);
			if (namespaceURI != null) {
				return net.enilink.komma.core.URIs.createURI(namespaceURI);
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
								element.getPrefix(), URIs.createURI(element
										.getName()));
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

	@Override
	public ITransaction getTransaction() {
		return transaction;
	}

	@Override
	public boolean hasMatch(IReference subject, IReference predicate,
			IValue object, boolean includeInferred, IReference... contexts) {
		contexts = addNullContext(includeInferred, contexts);
		try {
			return getConnection().hasStatement(
					(Resource) valueConverter.toSesame(subject),
					(URI) valueConverter.toSesame(predicate),
					valueConverter.toSesame(object), includeInferred,
					valueConverter.toSesameURI(contexts));
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
		contexts = addNullContext(includeInferred, contexts);
		try {
			SesameGraphResult result = new SesameGraphResult(getConnection()
					.getStatements(valueConverter.toSesame(subject),
							(URI) valueConverter.toSesame(predicate),
							valueConverter.toSesame(object), includeInferred,
							valueConverter.toSesameURI(contexts)));
			injector.injectMembers(result);
			return result;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	@Override
	public IReference blankNode() {
		return new SesameReference(getConnection().getValueFactory()
				.createBNode());
	}

	@Override
	public IReference blankNode(String id) {
		if (id == null) {
			return blankNode();
		}
		if (id.startsWith("_:")) {
			id = id.substring(2);
		}
		return new SesameReference(getConnection().getValueFactory()
				.createBNode(id));
	}

	@Override
	public IDataManager remove(
			Iterable<? extends IStatementPattern> statements,
			IReference... contexts) {
		if (contexts.length == 0) {
			contexts = NULL_CTX;
		}
		URI[] removeContexts = valueConverter.toSesameURI(contexts);
		try {
			RepositoryConnection conn = getConnection();
			boolean trackChanges = changeSupport.isEnabled(this);
			for (IStatementPattern stmt : statements) {
				if (stmt instanceof IStatement
						&& ((IStatement) stmt).isInferred()) {
					// special handling for inferred statements
					if (trackChanges) {
						for (IReference ctx : contexts) {
							changeSupport.remove(
									this,
									new Statement(stmt.getSubject(), stmt
											.getPredicate(), stmt.getObject(),
											ctx, true));
						}
					}
				} else {
					Resource subject = valueConverter.toSesame(stmt
							.getSubject());
					URI predicate = (URI) valueConverter.toSesame(stmt
							.getPredicate());
					Value object = valueConverter.toSesame((IValue) stmt
							.getObject());
					if (trackChanges) {
						for (IStatement existing : match(stmt.getSubject(),
								stmt.getPredicate(), (IValue) stmt.getObject(),
								false, contexts)) {
							// pretend that statement was in changeCtx if no
							// context
							// is set
							IReference[] changeContexts = existing.getContext() != null ? new IReference[] { existing
									.getContext() } : contexts;
							for (IReference ctx : changeContexts) {
								changeSupport.remove(this,
										new Statement(existing.getSubject(),
												existing.getPredicate(),
												existing.getObject(), ctx,
												existing.isInferred()));
							}
						}
					}
					conn.remove(subject, predicate, object, removeContexts);
				}
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
							URIs.createURI(namespace));
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

	/**
	 * Clear cache of generated blank nodes.
	 */
	void clearNodeMappings() {
		valueConverter.reset();
	}
}
