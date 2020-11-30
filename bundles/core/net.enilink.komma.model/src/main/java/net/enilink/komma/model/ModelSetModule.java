package net.enilink.komma.model;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.komma.core.IProvider;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.em.EagerCachingEntityManagerModule;
import net.enilink.komma.em.EntityManagerFactoryModule;
import net.enilink.komma.em.util.KommaUtil;
import net.enilink.komma.em.util.UnitOfWork;
import net.enilink.komma.rdf4j.RDF4JModule;

public class ModelSetModule extends AbstractModule {
	private KommaModule parentModule;

	public ModelSetModule(KommaModule module) {
		this.parentModule = module;
	}

	@Override
	protected void configure() {
		bind(UnitOfWork.class).in(Singleton.class);
		bind(IUnitOfWork.class).to(UnitOfWork.class);
	}

	@Provides
	@Singleton
	public IModelSetFactory provideModelSetFactory(Injector injector) {
		KommaModule module = new KommaModule(getClass().getClassLoader());
		if (parentModule != null) {
			module.includeModule(parentModule);
		}

		module.includeModule(KommaUtil.getCoreModule());

		ModelSetFactory factory = injector.createChildInjector(
				createFactoryModules(module))
				.getInstance(ModelSetFactory.class);
		return factory;
	}

	protected List<? extends Module> createFactoryModules(
			KommaModule kommaModule) {
		return Arrays.<Module> asList(new AbstractModule() {
			@Singleton
			@Provides
			Repository provideRepository() {
				return createMetaDataRepository();
			}
		}, new RDF4JModule(), new EntityManagerFactoryModule(kommaModule,
				getLocaleProvider(), getEntityManagerModule()));
	}

	protected IProvider<Locale> getLocaleProvider() {
		return null;
	}

	protected Module getEntityManagerModule() {
		return new EagerCachingEntityManagerModule();
	}

	/**
	 * Initializes meta data repository with internal ontologies
	 */
	protected Repository createMetaDataRepository() {
		try {
			Repository repository = new SailRepository(new MemoryStore());
			repository.init();

			Collection<URL> conceptLibraries = KommaUtil.getConceptLibraries(
					ModelPlugin.PLUGIN_ID).toList();
			URLClassLoader cl = new URLClassLoader(
					conceptLibraries.toArray(new URL[conceptLibraries.size()]));
			for (String owl : loadOntologyList(cl)) {
				URL url = cl.getResource(owl);
				loadOntology(repository, url);
			}

			return repository;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	private RDFFormat formatForFileName(String filename) {
		Optional<RDFFormat> format = Rio.getWriterFormatForFileName(filename);
		if (format.isPresent())
			return format.get();
		if (filename.endsWith(".owl"))
			return RDFFormat.RDFXML;
		throw new IllegalArgumentException("Unknow RDF format for " + filename);
	}

	private void loadOntology(Repository repository, URL url)
			throws RepositoryException, IOException, RDFParseException {
		String filename = url.toString();
		RDFFormat format = formatForFileName(filename);
		RepositoryConnection conn = repository.getConnection();
		try {
			conn.add(url, "", format);
		} finally {
			conn.close();
		}
	}

	@SuppressWarnings("unchecked")
	private Collection<String> loadOntologyList(ClassLoader cl)
			throws IOException {
		Properties ontologies = new Properties();
		String name = "META-INF/org.openrdf.ontologies";
		Enumeration<URL> resources = cl.getResources(name);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			ontologies.load(url.openStream());
		}
		Collection<?> list = ontologies.keySet();
		return (Collection<String>) list;
	}
}
