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

import java.io.File;
import java.net.URL;

import net.enilink.composition.properties.annotations.Transient;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IReference;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import net.enilink.composition.annotations.Iri;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.model.MODELS;

@Iri(MODELS.NAMESPACE + "PersistentModelSet")
public abstract class PersistentModelSetSupport extends MemoryModelSetSupport {

	@Transient
	@Iri(MODELS.NAMESPACE + "repository")
	public abstract IReference getRepository();

	public Repository createRepository(IGraph config) throws RepositoryException {
		final IReference repo = getRepository();
		if (repo == null || repo.getURI() == null) {
			throw new RepositoryException("No repository location specified");
		}
		URI repoUri = repo.getURI();
		if ("workspace".equals(repoUri.scheme())) {
			try {
				String instanceFilter = "(type=osgi.instance.area)";
				BundleContext context = FrameworkUtil.getBundle(PersistentModelSetSupport.class).getBundleContext();
				ServiceReference<?>[] refs = context
						.getServiceReferences("org.eclipse.osgi.service.datalocation.Location", instanceFilter);
				if (refs.length > 0) {
					Object location = context.getService(refs[0]);
					URL loc = (URL) location.getClass().getMethod("getURL").invoke(location);
					URI workspace = URIs.createURI(FileLocator.resolve(loc).toString());
					if ("".equals(workspace.lastSegment())) {
						workspace = workspace.trimSegments(1);
					}
					repoUri = workspace.appendSegments(repoUri.segments());
				}
			} catch (Exception e) {
				throw new RepositoryException(e);
			}
		} else {
			throw new RepositoryException("Location service for workspace scheme not found");
		}

		NotifyingSail store = new NativeStore(new File(repoUri.toFileString()));
		if (! Boolean.FALSE.equals(getInference())) {
			store = new SchemaCachingRDFSInferencer(store);
		}
		SailRepository repository = new SailRepository(store);
		repository.init();
		addBasicKnowledge(repository);
		return repository;
	}

	@Override
	public boolean isPersistent() {
		return true;
	}
}
