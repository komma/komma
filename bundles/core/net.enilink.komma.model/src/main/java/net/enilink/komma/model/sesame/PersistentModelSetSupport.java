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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import net.enilink.composition.annotations.Iri;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.model.MODELS;

@Iri(MODELS.NAMESPACE + "PersistentModelSet")
public abstract class PersistentModelSetSupport extends MemoryModelSetSupport {

	@Iri(MODELS.NAMESPACE + "repository")
	public abstract URI getRepository();

	public Repository createRepository() throws RepositoryException {
		URI repo = getRepository();
		if (repo.scheme() == "workspace") {
			URL loc = Platform.getInstanceLocation().getURL();
			try {
				URI workspace = URIs.createURI(FileLocator.resolve(loc)
						.toString());
				if (workspace.lastSegment() == "") {
					workspace = workspace.trimSegments(1);
				}
				repo = workspace.appendSegments(repo.segments());
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		}

		NativeStore store = new NativeStore(new File(repo.toFileString()));
		SailRepository repository = new SailRepository(store);
		repository.initialize();
		addBasicKnowledge(repository);
		return new SailRepository(Boolean.FALSE.equals(getInference()) ? store
				: new ForwardChainingRDFSInferencer(store));
	}
}
