/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.rdf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.traits.Behaviour;
import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.KommaEM;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.rdf4j.RDF4JModule;

@Iri(MODELS.NAMESPACE + "MemoryModelSet")
public abstract class MemoryModelSetSupport implements IModelSet,
		IModelSet.Internal, IRepositoryModelSet, Behaviour<IRepositoryModelSet> {
	public Repository createRepository() throws RepositoryException {
		NotifyingSail store = new MemoryStore();
		if (! Boolean.FALSE.equals(getInference())) {
			store = new SchemaCachingRDFSInferencer(store);
		}
		SailRepository repository = new SailRepository(store);
		repository.init();
		addBasicKnowledge(repository);
		return repository;
	}

	protected void addBasicKnowledge(Repository repository)
			throws RepositoryException {
		if (AbstractKommaPlugin.IS_OSGI_RUNNING) {
			Set<String> bundleNames = new HashSet<>(Arrays.asList("net.enilink.vocab.owl", "net.enilink.vocab.rdfs"));
			List<Bundle> bundles = Stream
					.of(FrameworkUtil.getBundle(MemoryModelSetSupport.class).getBundleContext().getBundles())
					.filter(b -> bundleNames.contains(b.getSymbolicName())).collect(Collectors.toList());
			
			RepositoryConnection conn = null;
			try {
				conn = repository.getConnection();
				for (Bundle bundle : bundles) {
					URL url = bundle.getResource(
							"META-INF/org.openrdf.ontologies");
					if (url != null) {
						URL resolvedUrl = FileLocator.resolve(url);

						Properties properties = new Properties();
						InputStream in = resolvedUrl.openStream();
						properties.load(in);
						in.close();

						URI baseUri = URIs.createURI(url.toString())
								.trimSegments(1);
						for (Map.Entry<Object, Object> entry : properties
								.entrySet()) {
							String file = entry.getKey().toString();
							if (!importRdfAndRdfsVocabulary()
									&& file.contains("rdfs")) {
								// skip RDF and RDFS schema
								continue;
							}

							URI fileUri = URIs.createFileURI(file);
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
													.createIRI(
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
						KommaEM.INSTANCE.log(e);
					}
				}
			}
		}
	}

	protected boolean importRdfAndRdfsVocabulary() {
		return true;
	}

	@Iri(MODELS.NAMESPACE + "inference")
	public abstract Boolean getInference();

	@Override
	public void collectInjectionModules(Collection<Module> modules) {
		modules.add(new RDF4JModule());
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
			}

			@Singleton
			@Provides
			protected Repository provideRepository() {
				try {
					return getBehaviourDelegate().createRepository();
				} catch (RepositoryException e) {
					throw new KommaException("Unable to create repository.", e);
				}
			}
		});
	}

	@Override
	public boolean isPersistent() {
		return false;
	}
}
