/*******************************************************************************
 * Copyright (c) 2009, 2020 Fraunhofer IWU and others.
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

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import net.enilink.composition.annotations.Iri;
import net.enilink.komma.dm.IDataManagerFactory;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.rdf4j.RDF4JDataManagerFactory;

/**
 * A model set implementation that uses a {@link Repository data repository}
 * provided via Guice.
 * 
 */
@Iri(MODELS.NAMESPACE + "InjectedRepositoryModelSet")
public abstract class InjectedRepositoryModelSetSupport implements IModelSet, IModelSet.Internal {
	/**
	 * Data manager factory that does not close the underlying repository if factory is closed.
	 * This prevents {@link IModelSet#dispose()} to close the wrapped repository. 
	 */
	static class RDF4JDataManagerFactoryWithoutClose extends RDF4JDataManagerFactory {
		@Override
		public void close() {
			// discard closing of repository, just set it to null
			repository = null;
		}
	}

	@Inject
	@Named("data-repository")
	Repository dataRepository;

	@Override
	public void collectInjectionModules(Collection<Module> modules) {
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
				bind(RDF4JDataManagerFactoryWithoutClose.class).in(Singleton.class);
				bind(IDataManagerFactory.class).to(RDF4JDataManagerFactoryWithoutClose.class);

				bind(Repository.class).toInstance(dataRepository);
			}

			@Provides
			ValueFactory provideValueFactory(Repository repository) {
				return repository.getValueFactory();
			}
		});
	}

	@Override
	public boolean isPersistent() {
		return false;
	}
}
