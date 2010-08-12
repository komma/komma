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
package net.enilink.komma.sesame;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.enilink.composition.ClassDefiner;
import net.enilink.composition.ClassResolver;
import net.enilink.composition.CompositionModule;
import net.enilink.composition.DefaultObjectFactory;
import net.enilink.composition.ObjectFactory;
import net.enilink.composition.mappers.DefaultRoleMapper;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.mappers.TypeFactory;
import net.enilink.composition.properties.PropertyMapper;
import net.enilink.composition.properties.PropertySetDescriptorFactory;
import net.enilink.composition.properties.behaviours.PropertyMapperProcessor;
import net.enilink.composition.properties.komma.KommaPropertySetDescriptorFactory;
import net.enilink.composition.properties.sparql.SparqlBehaviourMethodProcessor;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.URIFactory;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.store.StoreException;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import net.enilink.komma.common.util.Literal;
import net.enilink.komma.common.util.URIUtil;
import net.enilink.komma.internal.sesame.AbstractSesameManager;
import net.enilink.komma.internal.sesame.ByteArrayConverter;
import net.enilink.komma.internal.sesame.DecoratingSesameManager;
import net.enilink.komma.internal.sesame.SesameEntitySupport;
import net.enilink.komma.internal.sesame.repository.LoaderRepository;
import net.enilink.komma.internal.sesame.roles.RoleClassLoader;
import net.enilink.komma.literals.IConverter;
import net.enilink.komma.literals.LiteralConverter;
import net.enilink.komma.core.IKommaManager;
import net.enilink.komma.core.IKommaManagerFactory;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.ILiteralFactory;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIImpl;

/**
 * Creates SesameManagers.
 * 
 */
public abstract class SesameManagerFactory implements IKommaManagerFactory {
	protected class ManagerCompositionModule extends AbstractModule {
		private SesameManagerFactory factory;
		private KommaModule module;
		private Locale locale;

		protected ManagerCompositionModule(SesameManagerFactory factory,
				KommaModule module, Locale locale) {
			this.factory = factory;
			this.module = module;
			this.locale = locale == null ? Locale.getDefault() : locale;
		}

		@Override
		protected void configure() {
			install(new CompositionModule<URI>() {
				@Override
				protected void initBindings() {
					super.initBindings();

					getBehaviourClassProcessorBinder().addBinding()
							.to(PropertyMapperProcessor.class)
							.in(Singleton.class);

					getBehaviourMethodProcessorBinder().addBinding()
							.to(SparqlBehaviourMethodProcessor.class)
							.in(Singleton.class);
				}

				protected RoleMapper<URI> provideRoleMapper(
						TypeFactory<URI> typeFactory) {
					RoleMapper<URI> roleMapper = new DefaultRoleMapper<URI>(
							typeFactory);

					RoleClassLoader<URI> loader = new RoleClassLoader<URI>();
					loader.setClassLoader(module.getClassLoader());
					loader.setRoleMapper(roleMapper);
					loader.setTypeFactory(typeFactory);

					List<URL> libraries = module.getLibraries();
					loader.load(libraries);

					for (KommaModule.Association e : module.getConcepts()) {
						if (e.getRdfType() == null) {
							roleMapper.addConcept(e.getJavaClass());
						} else {
							roleMapper.addConcept(e.getJavaClass(),
									typeFactory.createType(e.getRdfType()));
						}
					}
					for (KommaModule.Association e : module.getBehaviours()) {
						if (e.getRdfType() == null) {
							roleMapper.addBehaviour(e.getJavaClass());
						} else {
							roleMapper.addBehaviour(e.getJavaClass(),
									typeFactory.createType(e.getRdfType()));
						}
					}

					roleMapper.addBehaviour(SesameEntitySupport.class,
							RDFS.RESOURCE);

					return roleMapper;
				}

				protected ClassDefiner provideClassDefiner() {
					return getSharedDefiner(module.getClassLoader());
				}
			});

			bind(Locale.class).toInstance(locale);

			bind(new Key<ObjectFactory<URI>>() {
			}).to(new TypeLiteral<DefaultObjectFactory<URI>>() {
			});

			bind(new TypeLiteral<ClassResolver<URI>>() {
			}).in(Singleton.class);

			bind(
					new TypeLiteral<Class<? extends PropertySetDescriptorFactory>>() {
					})
					.toInstance(
							(Class<? extends PropertySetDescriptorFactory>) KommaPropertySetDescriptorFactory.class);
		}

		@Provides
		@Singleton
		protected Repository provideRepository() {
			return repository;
		}

		@Provides
		@Singleton
		protected TypeFactory<URI> provideTypeFactory(
				final RepositoryConnection connection) {
			return new TypeFactory<URI>() {
				URIFactory uriFactory = connection.getValueFactory();

				@Override
				public URI createType(String type) {
					return uriFactory.createURI(type);
				}

				@Override
				public String toString(URI type) {
					return type.stringValue();
				}
			};
		}

		@Provides
		@Singleton
		protected PropertyMapper providePropertyMapper() {
			return new PropertyMapper(getClass().getClassLoader(), true);
		}

		@Provides
		@Singleton
		protected IKommaManager provideKommaManager(ISesameManager manager) {
			return manager;
		}

		@Provides
		@Singleton
		protected ISesameManager provideSesameManager(Injector injector) {
			AbstractSesameManager manager = createSesameManager();
			injector.injectMembers(manager);
			return manager;
		}

		@Provides
		@Singleton
		protected KommaModule provideKommaModule() {
			return module;
		}

		@Provides
		@Singleton
		protected ClassLoader provideClassLoader() {
			return module.getClassLoader();
		}

		@Provides
		@Singleton
		protected LiteralConverter provideLiteralManager(Injector injector,
				TypeFactory<URI> typeFactory, RepositoryConnection connection) {
			LiteralConverter literalManager = new LiteralConverter();
			injector.injectMembers(literalManager);

			for (KommaModule.Association e : module.getDatatypes()) {
				literalManager.addDatatype(e.getJavaClass(),
						URIImpl.createURI(e.getRdfType()));
			}

			// record additional converter for Base64 encoded byte arrays
			IConverter<?> converter = new ByteArrayConverter();
			literalManager.registerConverter(converter.getJavaClassName(),
					converter);
			literalManager.recordType(byte[].class, converter.getDatatype());

			return literalManager;
		}

		@Provides
		@Singleton
		protected ILiteralFactory provideLiteralFactory() {
			return new ILiteralFactory() {
				@Override
				public ILiteral createLiteral(Object value, String label,
						net.enilink.komma.core.URI datatype,
						String language) {
					if (datatype != null) {
						// let datatype take precedence if set, cannot set both
						return new Literal(value, datatype);
					} else {
						return new Literal(value, language);
					}
				}

				@Override
				public ILiteral createLiteral(String label,
						net.enilink.komma.core.URI datatype,
						String language) {
					return createLiteral(label, label, datatype, language);
				}
			};
		}

		@Provides
		@Singleton
		protected ContextAwareConnection provideContextAwareConnection(
				Repository repository) throws StoreException {
			ContextAwareConnection conn = createConnection(repository);
			conn.setQueryLanguage(getQueryLanguage());
			// TODO check if transaction isolation levels are fully supported by
			// Sesame
			// conn.setTransactionIsolation(Isolation.READ_UNCOMMITTED);

			net.enilink.komma.core.URI[] writeContext = null, readContext = null;

			if (!module.getWritableGraphs().isEmpty()) {
				writeContext = new net.enilink.komma.core.URI[module
						.getWritableGraphs().size()];
				Iterator<net.enilink.komma.core.URI> iter = module
						.getWritableGraphs().iterator();
				for (int i = 0; i < writeContext.length; i++) {
					writeContext[i] = iter.next();
				}
			}
			if (!module.getReadableGraphs().isEmpty()) {
				readContext = new net.enilink.komma.core.URI[module
						.getReadableGraphs().size()];
				Iterator<net.enilink.komma.core.URI> iter = module
						.getReadableGraphs().iterator();
				for (int i = 0; i < readContext.length; i++) {
					readContext[i] = iter.next();
				}
			}

			if (writeContext != null && writeContext.length > 0) {
				URI[] writeResources = createURI(writeContext);
				conn.setAddContexts(writeResources);
				conn.setRemoveContexts(writeResources);
			}
			if (readContext != null && readContext.length > 0) {
				URI[] readResources = createURI(readContext);
				conn.setReadContexts(readResources);
			}

			return conn;
		}

		@Provides
		@Singleton
		protected RepositoryConnection provideConnection(
				ContextAwareConnection connection) {
			return connection;
		}

		@Provides
		@Singleton
		SesameManagerFactory provideManagerFactory() {
			return factory;
		}

		private URI[] createURI(net.enilink.komma.core.URI... context) {
			URI[] result = new URI[context.length];
			for (int i = 0; i < result.length; i++) {
				if (context[i] == null) {
					result[i] = null;
				} else {
					result[i] = (URI) URIUtil.toSesameUri(context[i]);
				}
			}
			return result;
		}
	}

	private static Map<ClassLoader, WeakReference<ClassDefiner>> definers = new WeakHashMap<ClassLoader, WeakReference<ClassDefiner>>();

	private boolean opened = true;

	private QueryLanguage ql = QueryLanguage.SPARQL;

	private LoaderRepository repository;

	private boolean shutDownRepositoryOnClose = false;

	private KommaModule module;

	public SesameManagerFactory(KommaModule module) {
		try {
			Repository repository = new SailRepository(new MemoryStore());
			repository.initialize();
			shutDownRepositoryOnClose = true;
			init(module, new LoaderRepository(repository));
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	public SesameManagerFactory(KommaModule module, Repository repository) {
		init(module, new LoaderRepository(repository));
	}

	public void close() {
		try {
			if (shutDownRepositoryOnClose) {
				repository.shutDown();
			} else {
				repository.clearLoadedContexts();
			}
		} catch (StoreException e) {
			throw new KommaException(e);
		}
		opened = false;
	}

	@Override
	public Collection<AbstractModule> createGuiceModules(Locale locale) {
		if (!opened) {
			throw new IllegalStateException("SesameManagerFactory is closed");
		}

		List<AbstractModule> compositionModules = new ArrayList<AbstractModule>();
		createCoreGuiceModules(compositionModules, module, locale);
		createAdditionalGuiceModules(compositionModules, module, locale);

		return compositionModules;
	}

	@Override
	public ISesameManager createKommaManager() {
		return createKommaManager(null);
	}

	public SesameReference getReference(Resource resource) {
		return new SesameReference(resource);
	}

	@Override
	public ISesameManager createKommaManager(Locale locale) {
		Injector injector = Guice.createInjector(createGuiceModules(locale));
		return injector.getInstance(ISesameManager.class);
	}

	protected void createCoreGuiceModules(Collection<AbstractModule> modules,
			KommaModule module, Locale locale) {
		modules.add(new ManagerCompositionModule(this, module, locale));
	}

	protected void createAdditionalGuiceModules(
			Collection<AbstractModule> modules, KommaModule module,
			Locale locale) {
	}

	protected AbstractSesameManager createSesameManager() {
		return new DecoratingSesameManager();
	}

	@Override
	public Map<String, Object> getProperties() {
		return null;
	}

	public QueryLanguage getQueryLanguage() {
		return ql;
	}

	public Repository getRepository() {
		return repository;
	}

	private ClassDefiner getSharedDefiner(ClassLoader cl) {
		ClassDefiner definer = null;
		synchronized (definers) {
			WeakReference<ClassDefiner> ref = definers.get(cl);
			if (ref != null) {
				definer = ref.get();
			}
			if (definer == null) {
				definer = new ClassDefiner(cl);
				definers.put(cl, new WeakReference<ClassDefiner>(definer));
			}
		}
		return definer;
	}

	@Override
	public Set<String> getSupportedProperties() {
		return null;
	}

	protected void init(KommaModule module, LoaderRepository repository) {
		this.module = module;
		this.repository = repository;

		for (Map.Entry<URL, String> e : module.getDatasets().entrySet()) {
			loadContext(e.getKey(), e.getValue());
		}
	}

	public boolean isOpen() {
		return opened;
	}

	private void loadContext(URL dataset, String context) throws KommaException {
		try {
			ValueFactory vf = repository.getValueFactory();
			repository.loadContext(dataset, vf.createURI(context));
		} catch (StoreException e) {
			throw new KommaException(e);
		} catch (RDFParseException e) {
			throw new KommaException(e);
		} catch (IOException e) {
			throw new KommaException(e);
		}
	}

	public void setQueryLanguage(QueryLanguage ql) {
		this.ql = ql;
	}

	protected ContextAwareConnection createConnection(Repository repository)
			throws StoreException {
		return new ContextAwareConnection(repository);
	}
}
