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
package net.enilink.komma.model.sesame;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import net.enilink.composition.annotations.Iri;
import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.KommaEM;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.sesame.SesameModule;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.openrdf.model.Resource;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

@Iri(MODELS.NAMESPACE + "MemoryModelSet")
public abstract class MemoryModelSetSupport implements IModelSet,
		IModelSet.Internal {
	protected Repository createRepository() throws RepositoryException {
		MemoryStore store = new MemoryStore();
		SailRepository repository = new SailRepository(store);
		repository.initialize();
		addBasicKnowledge(repository);
		// add RDFS inferencer after base knowledge was imported into the
		// repository
		return new SailRepository(Boolean.FALSE.equals(getInference()) ? store
				: new ForwardChainingRDFSInferencer(store));
	}

	protected void addBasicKnowledge(Repository repository)
			throws RepositoryException {
		String[] bundles = { "net.enilink.vocab.owl", "net.enilink.vocab.rdfs" };

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
		modules.add(new SesameModule());
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
			}

			@Singleton
			@Provides
			protected Repository provideRepository() {
				try {
					return createRepository();
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
