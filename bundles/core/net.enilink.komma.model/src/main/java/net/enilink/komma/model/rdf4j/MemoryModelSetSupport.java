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

import java.util.Collection;

import net.enilink.composition.properties.annotations.Transient;
import net.enilink.komma.core.IGraph;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.traits.Behaviour;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.rdf4j.RDF4JModule;

@Iri(MODELS.NAMESPACE + "MemoryModelSet")
public abstract class MemoryModelSetSupport implements IModelSet,
		IModelSet.Internal, IRepositoryModelSet, Behaviour<IRepositoryModelSet> {
	public Repository createRepository(IGraph config) throws RepositoryException {
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
		RepositoryUtil.addBasicKnowledge(repository, getDefaultGraph(), importRdfAndRdfsVocabulary());
	}

	protected boolean importRdfAndRdfsVocabulary() {
		return true;
	}

	@Transient
	@Iri(MODELS.NAMESPACE + "inference")
	public abstract Boolean getInference();

	@Override
	public void collectInjectionModules(Collection<Module> modules, IGraph config) {
		modules.add(new RDF4JModule());
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
			}

			@Singleton
			@Provides
			Repository provideRepository() {
				try {
					return getBehaviourDelegate().createRepository(config);
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
