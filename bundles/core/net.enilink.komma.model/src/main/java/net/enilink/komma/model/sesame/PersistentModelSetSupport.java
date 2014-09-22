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

import net.enilink.composition.annotations.Iri;
import net.enilink.komma.model.MODELS;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.nativerdf.NativeStore;

@Iri(MODELS.NAMESPACE + "PersistentModelSet")
public abstract class PersistentModelSetSupport extends MemoryModelSetSupport {

	@Iri(MODELS.NAMESPACE + "repository")
	public abstract String getRepository();

	public Repository createRepository() throws RepositoryException {
		NativeStore store = new NativeStore(new File(getRepository()));
		SailRepository repository = new SailRepository(store);
		repository.initialize();
		addBasicKnowledge(repository);
		return new SailRepository(Boolean.FALSE.equals(getInference()) ? store
				: new ForwardChainingRDFSInferencer(store));
	}

}
