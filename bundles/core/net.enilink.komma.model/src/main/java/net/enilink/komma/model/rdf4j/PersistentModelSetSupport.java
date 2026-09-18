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

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.properties.annotations.Transient;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IValue;
import net.enilink.komma.model.MODELS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@Iri(MODELS.NAMESPACE + "PersistentModelSet")
public abstract class PersistentModelSetSupport extends MemoryModelSetSupport {
	private static final Logger log = LoggerFactory.getLogger(PersistentModelSetSupport.class);

	@Transient
	@Iri(MODELS.NAMESPACE + "repository")
	public abstract IValue getRepository();

	public Repository createRepository(IGraph config) throws RepositoryException {
		IValue repo = getRepository();
		if (repo == null) {
			repo = getDataDir();
		}
		if (repo == null) {
			throw new RepositoryException("No repository location specified");
		}
		String dataDir;
		if (repo instanceof IReference && ((IReference) repo).getURI() != null) {
			dataDir = resolveWorkspaceURI(((IReference) repo).getURI()).toFileString();
		} else {
			dataDir = repo instanceof ILiteral ? ((ILiteral) repo).getLabel() : repo.toString();
		}
		log.info("Using data directory: " + dataDir);

		NotifyingSail store = new NativeStore(new File(dataDir));
		if (Boolean.TRUE.equals(getInference())) {
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
