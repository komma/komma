package net.enilink.komma.model;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.NotifyingSail;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.store.StoreException;
import org.osgi.framework.Bundle;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.komma.KommaCore;
import net.enilink.komma.em.EagerCachingEntityManagerModule;
import net.enilink.komma.em.EntityManagerFactoryModule;
import net.enilink.komma.model.base.AbstractModelSetSupport;
import net.enilink.komma.model.base.AbstractModelSupport;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.sesame.SesameModule;
import net.enilink.komma.util.KommaUtil;
import net.enilink.komma.util.UnitOfWork;

public class ModelSetModule extends AbstractModule {
	private KommaModule parentModule;

	public ModelSetModule(KommaModule module) {
		this.parentModule = module;
	}

	@Override
	protected void configure() {
		UnitOfWork uow = new UnitOfWork();
		uow.begin();

		bind(UnitOfWork.class).toInstance(uow);
		bind(IUnitOfWork.class).toInstance(uow);
	}

	@Provides
	@Singleton
	public IModelSetFactory provideModelSetFactory(Injector injector) {
		KommaModule module = new KommaModule(getClass().getClassLoader());
		if (parentModule != null) {
			module.includeModule(parentModule);
		}

		// for (URL libraryUrl : KommaUtil.getConceptLibraries(
		// Platform.getBundle(KommaConcepts.PLUGIN_ID)).andThen(
		// KommaUtil.getBundleMetaInfLocations(KommaConcepts.PLUGIN_ID))) {
		// module.addJarFileUrl(libraryUrl);
		// }

		module.addConcept(AbstractModelSupport.class,
				MODELS.CLASS_MODEL.toString());
		module.addConcept(AbstractModelSetSupport.class,
				MODELS.CLASS_MODELSET.toString());

		module.addConcept(IModel.IDiagnostic.class,
				MODELS.CLASS_DIAGNOSTIC.toString());

		// module.addDataset(
		// getResource(ModelCore.PLUGIN_ID,
		// "META-INF/ontologies/models.owl"),
		// "http://enilink.net/vocab/komma/models#");

		module.includeModule(KommaUtil.getCoreModule());

		return injector.createChildInjector(
				new AbstractModule() {
					@Override
					protected void configure() {
					}

					@Singleton
					@Provides
					@SuppressWarnings("unused")
					Repository provideRepository() {
						return createMetaDataRepository();
					}
				},
				new SesameModule(),
				new EntityManagerFactoryModule(module, null,
						new EagerCachingEntityManagerModule())).getInstance(
				ModelSetFactory.class);
	}

	/**
	 * Initializes meta data repository with internal ontologies
	 */
	protected Repository createMetaDataRepository() {
		try {
			NotifyingSail sailStack = new MemoryStore();
			sailStack = new ForwardChainingRDFSInferencer(sailStack);

			Repository repository = new SailRepository(sailStack);
			repository.initialize();

			Collection<URL> conceptLibraries = KommaUtil.getConceptLibraries(
					ModelCore.PLUGIN_ID).toList();
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
		RDFFormat format = RDFFormat.forFileName(filename);
		if (format != null)
			return format;
		if (filename.endsWith(".owl"))
			return RDFFormat.RDFXML;
		throw new IllegalArgumentException("Unknow RDF format for " + filename);
	}

	protected URL getResource(String bundleName, String path) {
		Bundle bundle = null;
		if (KommaCore.IS_ECLIPSE_RUNNING) {
			bundle = Platform.getBundle(bundleName);
		}

		if (bundle != null) {
			return bundle.getEntry(path);
		} else {
			return getClass().getClassLoader().getResource(path);
		}
	}

	private void loadOntology(Repository repository, URL url)
			throws StoreException, IOException, RDFParseException {
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
		String name = "META-INF/org.openrdf.elmo.ontologies";
		Enumeration<URL> resources = cl.getResources(name);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			ontologies.load(url.openStream());
		}
		Collection<?> list = ontologies.keySet();
		return (Collection<String>) list;
	}
}
