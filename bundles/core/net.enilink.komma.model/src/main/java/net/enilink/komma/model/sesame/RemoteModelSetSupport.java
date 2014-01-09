package net.enilink.komma.model.sesame;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.IMap;
import net.enilink.composition.annotations.Iri;
import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.InferencingCapability;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerFactory;
import net.enilink.komma.dm.IDataManagerQuery;
import net.enilink.komma.dm.change.IDataChangeSupport;
import net.enilink.komma.internal.sesame.SesameRepositoryDataManager;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.sesame.SesameDataManagerFactory;
import net.enilink.komma.sesame.SesameModule;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.openrdf.model.Resource;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

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
			SesameDataManagerFactory {
		@Override
		public IDataManager get() {
			return injector.getInstance(RemoteRepositoryDataManager.class);
		}
	}

	static class RemoteRepositoryDataManager extends
			SesameRepositoryDataManager {
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

		protected IReference[] addNull(boolean includeInferred,
				IReference[] contexts) {
			if (includeInferred) {
//				contexts = Arrays.copyOf(contexts, contexts.length + 1);
//				contexts[contexts.length - 1] = null;
				contexts = new IReference[0];
			}
			return contexts;
		}

		@Override
		public <R> IDataManagerQuery<R> createQuery(String query,
				String baseURI, boolean includeInferred, IReference... contexts) {
			return super.createQuery(query, baseURI, includeInferred,
					addNull(includeInferred, contexts));
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
	public void collectInjectionModules(Collection<Module> modules) {
		modules.add(Modules.override(new SesameModule()).with(
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
						});
			}

			@Singleton
			@Provides
			protected Repository provideRepository() {
				try {
					HTTPRepository repo = new HTTPRepository(getServer()
							.toString(), valueOrDefault(getRepository(),
							"enilink"));

					String username = getUsername();
					if (username != null) {
						String password = getPassword();
						repo.setUsernameAndPassword(username, password);
					}

					repo.initialize();
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
		String[] bundles = { "net.enilink.vocab.owl",
				"net.enilink.vocab.rdfs" };

		if (AbstractKommaPlugin.IS_ECLIPSE_RUNNING) {
			RepositoryConnection conn = null;
			try {
				conn = repository.getConnection();
				for (String name : bundles) {
					URL url = Platform.getBundle(name).getResource(
							"META-INF/org.openrdf.ontologies");
					if (url != null) {
						URL resolvedUrl = FileLocator.resolve(url);

						Properties properties = new Properties();
						InputStream in = resolvedUrl.openStream();
						properties.load(in);
						in.close();

						URI baseUri = URIImpl.createURI(url.toString())
								.trimSegments(1);
						for (Map.Entry<Object, Object> entry : properties
								.entrySet()) {
							String file = entry.getKey().toString();
							if (file.contains("rdfs") && skipRdfsOnImport()) {
								// skip RDF and RDFS schema
								continue;
							}

							URIImpl fileUri = URIImpl.createFileURI(file);
							fileUri = fileUri.resolve(baseUri);

							resolvedUrl = FileLocator.resolve(new URL(fileUri
									.toString()));
							if (resolvedUrl != null) {
								in = resolvedUrl.openStream();
								if (in != null && in.available() > 0) {
									URI defaultGraph = getDefaultGraph();
									Resource[] contexts = defaultGraph == null ? new Resource[0]
											: new Resource[] { repository
													.getValueFactory()
													.createURI(
															defaultGraph
																	.toString()) };
									conn.add(in, "", RDFFormat.RDFXML, contexts);
								}
								if (in != null) {
									in.close();
								}
							}
						}
					}
				}
			} catch (IOException e) {
				throw new KommaException("Cannot access RDF data", e);
			} catch (RepositoryException e) {
				throw new KommaException("Loading RDF failed", e);
			} catch (RDFParseException e) {
				throw new KommaException("Invalid RDF data", e);
			} finally {
				if (conn != null) {
					try {
						conn.close();
					} catch (RepositoryException e) {
						ModelPlugin.log(e);
					}
				}
			}
		}
	}

	@Override
	public URI getDefaultGraph() {
		return URIImpl.createURI("komma:default");
	}

	protected boolean skipRdfsOnImport() {
		return false;
	}

	@Override
	public boolean isPersistent() {
		return true;
	}
}
