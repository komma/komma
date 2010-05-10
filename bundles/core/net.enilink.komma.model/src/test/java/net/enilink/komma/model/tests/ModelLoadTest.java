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
package net.enilink.komma.model.tests;

import java.util.Collections;
import java.util.List;

import junit.framework.Test;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.model.base.ModelSetFactory;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.tests.KommaTestCase;

public class ModelLoadTest extends KommaTestCase {
	IModelSet modelSet;

	protected void setUp() throws Exception {
		super.setUp();

		KommaModule module = ModelCore.createModelSetModule(getClass()
				.getClassLoader());

		modelSet = new ModelSetFactory(module, URIImpl
				.createURI(MODELS.NAMESPACE + "MemoryModelSet"))
				.createModelSet();

		Repository repository = new SailRepository(new MemoryStore());
		repository.initialize();
		RepositoryConnection conn = repository.getConnection();
		conn.clear();
		conn.clearNamespaces();
		conn.close();
	}

	protected void tearDown() throws Exception {
		modelSet.dispose();

		super.tearDown();
	}

	public void testModel() throws Exception {
		IModel model = modelSet
				.createModel(URIImpl
						.createURI("http://www.tu-chemnitz.de/ontologies/tessi/tessi-ooa"));
		model.load(Collections.emptyMap());

		List<?> test = model
				.getManager()
				.createQuery(
						"SELECT DISTINCT ?o WHERE { <http://www.tu-chemnitz.de/ontologies/tessi/tessi-core#hasColor> a ?o }")
				.getResultList();

		assertEquals("The Ontologies couldn't resolved.", 4, test.size());
	}

	public static Test suite() throws Exception {
		return suite(ModelLoadTest.class);
	}
}
