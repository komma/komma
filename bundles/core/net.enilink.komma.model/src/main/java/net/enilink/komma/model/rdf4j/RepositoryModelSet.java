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

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.properties.annotations.Transient;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.rdf4j.RDF4JValueConverter;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

@Iri(MODELS.NAMESPACE + "RepositoryModelSet")
public abstract class RepositoryModelSet extends MemoryModelSetSupport {
	static final Logger log = LoggerFactory.getLogger(RepositoryModelSet.class);

	@Transient
    @Iri(MODELS.NAMESPACE + "repository")
    public abstract IReference getRepository();

    public Repository createRepository(IGraph config) throws RepositoryException {
        RDF4JValueConverter converter = new RDF4JValueConverter(SimpleValueFactory.getInstance());
        Model configModel = new LinkedHashModel();

		// copy repository config statements into RDF4J model
	    Set<IReference> seen = new HashSet<>();
	    Queue<IReference> queue = new LinkedList<>();
	    queue.add(getRepository());
	    while (!queue.isEmpty()) {
		    IReference s = queue.remove();
		    if (seen.add(s)) {
			    for (IStatement stmt : config.filter(s, null, null)) {
				    configModel.add(converter.toRdf4j(stmt));
				    if (stmt.getObject() instanceof IReference && !seen.contains((IReference) stmt.getObject())) {
					    queue.add((IReference) stmt.getObject());
				    }
			    }
		    }
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
        String dataDirPath = getDataDirPath();
		if (dataDirPath != null) {
			log.info("Using data directory: " + dataDirPath);
			repository.setDataDir(new File(dataDirPath));
		}
        repository.init();
        addBasicKnowledge(repository);
        return repository;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }
}