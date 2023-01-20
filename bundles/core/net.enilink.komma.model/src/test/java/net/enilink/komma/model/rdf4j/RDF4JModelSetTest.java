/*******************************************************************************
 * Copyright (c) 2022 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.rdf4j;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.enilink.komma.core.URIs;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;

public class RDF4JModelSetTest {
	Repository repository;
	IModelSet modelSet;

	@Before
	public void beforeTest() throws Exception {
		repository = new SailRepository(new MemoryStore());
		repository.init();
		modelSet = RDF4JModelSetFactory.createModelSet(repository);
	}

	@After
	public void afterTest() throws Exception {
		modelSet.dispose();
		// repository is not closed if model set just wraps RDF4J repository
		repository.shutDown();
	}

	@Test
	public void testMultiWayAccess() throws Exception {
		String modelName = "test:model";
		String queryStr = "SELECT DISTINCT ?property WHERE { ?property a owl:ObjectProperty }";

		IModel model = modelSet.createModel(URIs.createURI(modelName));
		Map<Object, Object> options = new HashMap<>();
		options.put(IModel.OPTION_MIME_TYPE, "text/turtle");
		model.load(getClass().getResourceAsStream("/simple-model.ttl"), options);

		// query the model via KOMMA's API
		List<?> propertiesViaKomma = model.getManager().createQuery(queryStr).getResultList();

		assertEquals("The object properties couldn't be resolved.", 2, propertiesViaKomma.size());

		// query the model via RDF4J's API
		try (RepositoryConnection conn = repository.getConnection()) {
			TupleQuery nativeQuery = conn.prepareTupleQuery(queryStr);
			// restrict query to model context
			SimpleDataset ds = new SimpleDataset();
			ds.addDefaultGraph(repository.getValueFactory().createIRI(modelName));
			nativeQuery.setDataset(ds);

			List<?> propertiesViaRdf4j = nativeQuery.evaluate().stream().collect(Collectors.toList());
			assertEquals("Results of KOMMA query and native query differ.", propertiesViaKomma.size(),
					propertiesViaRdf4j.size());
		}
	}
}