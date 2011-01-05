/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.base;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.event.NotifyingRepository;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.NotifyingSail;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.store.StoreException;
import org.osgi.framework.Bundle;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.KommaCore;
import net.enilink.komma.common.util.URIUtil;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.manager.EagerCachingKommaManagerModule;
import net.enilink.komma.manager.ISesameManager;
import net.enilink.komma.manager.SesameReference;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IKommaManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.LinkedHashGraph;
import net.enilink.komma.core.URI;
import net.enilink.komma.util.KommaUtil;

public class ModelSetFactory implements IModelSetFactory {
	private URI[] modelSetTypes;
	private KommaModule parentModule;

	public ModelSetFactory(KommaModule module, URI... modelSetTypes) {
		this.modelSetTypes = modelSetTypes;
		this.parentModule = module;
	}

	/**
	 * Initializes manager for meta data
	 */
	protected IKommaManager createMetaDataManager() {
		KommaModule module = new KommaModule(getClass().getClassLoader());
		if (parentModule != null) {
			module.includeModule(parentModule);
		}

		// for (URL libraryUrl : KommaUtil.getConceptLibraries(
		// Platform.getBundle(KommaConcepts.PLUGIN_ID)).andThen(
		// KommaUtil.getBundleMetaInfLocations(KommaConcepts.PLUGIN_ID))) {
		// module.addJarFileUrl(libraryUrl);
		// }

		module.addConcept(IModel.IDiagnostic.class,
				MODELS.CLASS_DIAGNOSTIC.toString());

		module.addDataset(
				getResource(ModelCore.PLUGIN_ID,
						"META-INF/ontologies/models.owl"),
				"http://enilink.net/vocab/komma/models#");

		module.includeModule(KommaUtil.getCoreModule());

		return new EagerCachingKommaManagerModule(module,
				createMetaDataRepository()).createKommaManager();
	}

	/**
	 * Initializes meta data repository with internal ontologies
	 */
	protected NotifyingRepository createMetaDataRepository() {
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

			NotifyingRepositoryWrapper notifyingRepository = new NotifyingRepositoryWrapper(
					repository);
			notifyingRepository.setDefaultReportDeltas(true);

			return notifyingRepository;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	@Override
	public IModelSet createModelSet() {
		return createModelSet(new LinkedHashGraph());
	}

	@Override
	public IModelSet createModelSet(IGraph configuration) {
		IKommaManager metaDataManager = createMetaDataManager();
		metaDataManager.add(configuration);

		List<IReference> types = new ArrayList<IReference>();
		types.addAll(Arrays.asList(modelSetTypes));
		types.add(MODELS.CLASS_MODELSET);
		types.add(RDFS.TYPE_RESOURCE);

		IModelSet modelSet = null;

		// check if a model set is setup by configuration
		IExtendedIterator<IModelSet> it = null;
		try {
			it = metaDataManager.findAll(IModelSet.class);
			if (it.hasNext()) {
				modelSet = it.next();
			}
		} finally {
			it.close();
		}

		if (modelSet != null) {
			// if a model set already exists, then simply add the additional
			// types
			try {
				metaDataManager.getTransaction().begin();

				@SuppressWarnings("unchecked")
				Set<Object> rdfTypes = (Set<Object>) ((IResource) modelSet)
						.get(RDF.PROPERTY_TYPE);
				rdfTypes.addAll(types);

				metaDataManager.getTransaction().commit();
			} catch (Exception e) {
				metaDataManager.getTransaction().rollback();
				throw new KommaException(e);
			}
		}

		if (modelSet == null) {
			// create a model set if not found
			modelSet = (IModelSet) metaDataManager.create(types
					.toArray(new IReference[types.size()]));
		}
		// modelSetResource.setPersistent(isPersistent());
		return modelSet;
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
