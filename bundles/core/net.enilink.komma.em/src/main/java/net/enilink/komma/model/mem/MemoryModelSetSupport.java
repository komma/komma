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
package net.enilink.komma.model.mem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import net.enilink.composition.annotations.Iri;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryMetaData;
import org.openrdf.repository.base.RepositoryMetaDataWrapper;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.NotifyingSail;
import org.openrdf.sail.federation.Federation;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.store.StoreException;

import net.enilink.komma.KommaCore;
import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.base.AbstractModelSetSupport;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

@Iri(MODELS.NAMESPACE + "MemoryModelSet")
public abstract class MemoryModelSetSupport extends AbstractModelSetSupport {
	protected Repository createRepository() throws StoreException {
		NotifyingSail sailStack = new MemoryStore();
		sailStack = new ForwardChainingRDFSInferencer(sailStack);

		Repository repository = new SailRepository(sailStack);
		repository.initialize();

		// return repository;
		Repository federationRepository = createFederation(repository);
		return federationRepository;
	}

	protected Repository createFederation(Repository repository)
			throws StoreException {
		MemoryStore owlStore = new MemoryStore();
		SailRepository owlRepository = new SailRepository(owlStore) {
			RepositoryMetaData metaData;

			@Override
			public RepositoryMetaData getMetaData() throws StoreException {
				if (metaData == null) {
					metaData = new RepositoryMetaDataWrapper(super
							.getMetaData()) {
						@Override
						public boolean isReadOnly() {
							return true;
						}
					};
				}
				return metaData;
			}

		};
		owlRepository.initialize();

		String[] bundles = { "net.enilink.vocab.owl",
				"net.enilink.vocab.rdfs" };

		if (AbstractKommaPlugin.IS_ECLIPSE_RUNNING) {
			RepositoryConnection conn = null;

			try {
				conn = owlRepository.getConnection();
				for (String name : bundles) {
					URL url = FileLocator.find(Platform.getBundle(name),
							new Path("META-INF/org.openrdf.elmo.ontologies"),
							Collections.emptyMap());
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
							// String namespaces = entry.getValue().toString();
							String file = entry.getKey().toString();

							URIImpl fileUri = URIImpl.createFileURI(file);
							fileUri = fileUri.resolve(baseUri);

							resolvedUrl = FileLocator.resolve(new URL(fileUri
									.toString()));
							if (resolvedUrl != null) {
								in = resolvedUrl.openStream();
								if (in != null && in.available() > 0) {
									conn.add(in, "", RDFFormat.RDFXML);
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
			} catch (StoreException e) {
				throw new KommaException("Loading RDF failed", e);
			} catch (RDFParseException e) {
				throw new KommaException("Invalid RDF data", e);
			} finally {
				if (conn != null) {
					try {
						conn.close();
					} catch (StoreException e) {
						KommaCore.log(e);
					}
				}
			}

		}

		Federation federation = new Federation() {
			@Override
			public void initialize() throws StoreException {
			}
		};
		federation.setDistinct(true);

		federation.addMember(repository);
		federation.addMember(owlRepository);

		Repository federationRepository = new SailRepository(federation);
		federationRepository.initialize();

		return federationRepository;
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	protected void init() {
		super.init();

		Repository repository;
		try {
			repository = createRepository();
		} catch (StoreException e) {
			throw new KommaException("Creating repository failed", e);
		}
		NotifyingRepositoryWrapper notifyingRepository = new NotifyingRepositoryWrapper(
				repository);
		notifyingRepository.setDefaultReportDeltas(true);
		initRepository(notifyingRepository);
	}

	@Override
	public boolean isPersistent() {
		return false;
	}
}
