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

import org.eclipse.rdf4j.repository.Repository;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.name.Named;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.traits.Behaviour;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.rdf4j.RDF4JModule;

/**
 * A model set implementation that uses a {@link Repository data repository}
 * provided via Guice.
 * 
 */
@Iri(MODELS.NAMESPACE + "RDF4JRepositoryModelSet")
public abstract class RDF4JRepositoryModelSetSupport
		implements IModelSet, IModelSet.Internal, IRepositoryModelSet, Behaviour<IRepositoryModelSet> {
	@Inject
	@Named("data-repository")
	Repository dataRepository;

	@Override
	public void collectInjectionModules(Collection<Module> modules) {
		modules.add(new RDF4JModule());
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Repository.class).toInstance(dataRepository);
			}
		});
	}

	@Override
	public boolean isPersistent() {
		return false;
	}
}
