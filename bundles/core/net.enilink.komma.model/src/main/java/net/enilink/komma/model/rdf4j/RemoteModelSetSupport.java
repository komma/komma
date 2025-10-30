package net.enilink.komma.model.rdf4j;

import java.util.Collection;

import net.enilink.komma.core.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.IMap;
import net.enilink.composition.annotations.Iri;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerFactory;
import net.enilink.komma.dm.IDataManagerQuery;
import net.enilink.komma.dm.change.IDataChangeSupport;
import net.enilink.komma.internal.rdf4j.RDF4JRepositoryDataManager;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.rdf4j.RDF4JDataManagerFactory;
import net.enilink.komma.rdf4j.RDF4JModule;

@Iri(MODELS.NAMESPACE + "RemoteModelSet")
public abstract class RemoteModelSetSupport implements IModelSet.Internal {
	public static final String USERNAME = null;
	public static final String PASSWORD = null;

	protected <T> T valueOrDefault(T value, T defaultValue) {
		return value != null ? value : defaultValue;
	}

	@Iri(MODELS.NAMESPACE + "server")
	public abstract URI getServer();

	@Iri(MODELS.NAMESPACE + "repository")
	public abstract String getRepository();

	@Iri(MODELS.NAMESPACE + "username")
	public abstract String getUsername();

	@Iri(MODELS.NAMESPACE + "password")
	public abstract String getPassword();

	static class RemoteRepositoryDataManagerFactory extends
			RDF4JDataManagerFactory {
		@Override
		public IDataManager get() {
			return injector.getInstance(RemoteRepositoryDataManager.class);
		}
	}

	static class RemoteRepositoryDataManager extends
			RDF4JRepositoryDataManager {
		@Inject
		public RemoteRepositoryDataManager(Repository repository,
				IDataChangeSupport changeSupport) {
			super(repository, changeSupport);
		}

		protected void setParameters(IDataManagerQuery<?> query,
				IReference subject, IReference predicate, IValue object) {
			if (subject != null) {
				query.setParameter("s", subject);
			}
			if (predicate != null) {
				query.setParameter("p", predicate);
			}
			if (object != null) {
				query.setParameter("o", object);
			}
		}

		@Override
		protected IReference[] addNullContext(boolean includeInferred,
				IReference[] contexts) {
			if (includeInferred) {
				// contexts = Arrays.copyOf(contexts, contexts.length + 1);
				// contexts[contexts.length - 1] = null;
				contexts = new IReference[0];
			}
			return contexts;
		}

		@Override
		public boolean hasMatch(IReference subject, IReference predicate,
				IValue object, boolean includeInferred, IReference... contexts) {
			String query;
			if (contexts.length > 0 && !includeInferred) {
				query = "ASK { GRAPH ?g { ?s ?p ?o } }";
			} else {
				query = "ASK { ?s ?p ?o }";
			}
			IDataManagerQuery<?> dmQuery = createQuery(query, null,
					includeInferred, contexts);
			setParameters(dmQuery, subject, predicate, object);
			Object result = dmQuery.evaluate().next();
			return Boolean.TRUE.equals(result);
		}

		@Override
		public IExtendedIterator<IStatement> match(IReference subject,
				IReference predicate, IValue object, boolean includeInferred,
				IReference... contexts) {
			String query;
			if (contexts.length > 0 && !includeInferred) {
				query = "SELECT DISTINCT ?s ?p ?o ?g WHERE { GRAPH ?g { ?s ?p ?o } }";
			} else {
				query = "SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o }";
			}
			IDataManagerQuery<?> dmQuery = createQuery(query, null,
					includeInferred, contexts);
			setParameters(dmQuery, subject, predicate, object);
			return dmQuery.evaluate().mapWith(new IMap<Object, IStatement>() {
				@Override
				public IStatement map(Object value) {
					IBindings<?> bindings = (IBindings<?>) value;
					return new Statement((IReference) bindings.get("s"),
							(IReference) bindings.get("p"), bindings.get("o"),
							(IReference) bindings.get("g"));
				}
			});
		}
	}

	@Override
	public void collectInjectionModules(Collection<Module> modules, IGraph config) {
		modules.add(Modules.override(new RDF4JModule()).with(
				new AbstractModule() {
					@Override
					protected void configure() {
						bind(RemoteRepositoryDataManagerFactory.class).in(
								Singleton.class);
						bind(IDataManagerFactory.class).to(
								RemoteRepositoryDataManagerFactory.class);
					}
				}));
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
				bind(InferencingCapability.class).toInstance(
						new InferencingCapability() {
							@Override
							public boolean doesRDFS() {
								return true;
							}

							@Override
							public boolean doesOWL() {
								return true;
							}

							@Override
							public boolean inDefaultGraph() {
								return true;
							}
						});
			}

			@Singleton
			@Provides
			Repository provideRepository() {
				try {
					HTTPRepository repo = new HTTPRepository(getServer()
							.toString(), valueOrDefault(getRepository(),
							"enilink"));

					String username = getUsername();
					if (username != null) {
						String password = getPassword();
						repo.setUsernameAndPassword(username, password);
					}

					repo.init();
					addBasicKnowledge(repo);
					return repo;
				} catch (Exception e) {
					throw new KommaException("Unable to initialize repository",
							e);
				}

			}
		});
	}

	protected void addBasicKnowledge(Repository repository)
			throws RepositoryException {
		RepositoryUtil.addBasicKnowledge(repository, getDefaultGraph(), importRdfAndRdfsVocabulary());
	}

	@Override
	public URI getDefaultGraph() {
		return URIs.createURI("komma:default");
	}

	protected boolean importRdfAndRdfsVocabulary() {
		return true;
	}

	@Override
	public boolean isPersistent() {
		return true;
	}
}
