/*******************************************************************************
 * Copyright (c) 2024 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.rdf4j;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.composition.annotations.Iri;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.rdf4j.RDF4JValueConverter;
import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryRegistry;

@Iri(MODELS.NAMESPACE + "RepositoryModelSet")
public abstract class RepositoryModelSet extends MemoryModelSetSupport {

    @Iri(MODELS.NAMESPACE + "repository")
    public abstract IReference getRepository();

    public Repository createRepository() throws RepositoryException {
        IEntityManager manager = ((IEntity) getBehaviourDelegate()).getEntityManager();
        RDF4JValueConverter converter = new RDF4JValueConverter(SimpleValueFactory.getInstance());
        Model configModel = new LinkedHashModel();
        try (IExtendedIterator<IStatement> stmts = manager
            .createQuery("construct { ?s ?p ?o } where { ?repository (!<:>)* ?s . ?s ?p ?o }")
            .setParameter("repository", getRepository())
            .evaluateRestricted(IStatement.class)) {
            stmts.forEach(stmt -> {
                configModel.add(converter.toRdf4j(stmt));
            });
        }
        Set<String> repositoryIDs = RepositoryConfigUtil.getRepositoryIDs(configModel);
        if (repositoryIDs.isEmpty()) {
            throw new KommaException("No repository ID in configuration: " + getRepository());
        }
        if (repositoryIDs.size() != 1) {
            throw new KommaException("Multiple repository IDs in configuration: " + getRepository());
        }
        RepositoryConfig repoConfig = RepositoryConfigUtil.getRepositoryConfig(configModel,
            repositoryIDs.iterator().next());
        RepositoryImplConfig implConfig = repoConfig.getRepositoryImplConfig();
        if (implConfig == null) {
            throw new KommaException("No implementation config in configuration: " + getRepository());
        }
        RepositoryFactory factory = RepositoryRegistry.getInstance()
            .get(implConfig.getType())
            .orElseThrow(() -> new RepositoryConfigException("Unsupported repository type: " + implConfig.getType()));
        Repository repository = factory.getRepository(implConfig);
        repository.init();
        addBasicKnowledge(repository);
        return repository;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }
}