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

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;

import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.model.ModelSetModule;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIImpl;

public class ModelLoadTest {
	IModelSet modelSet;

	@Before
	public void beforeTest() throws Exception {
		KommaModule module = ModelCore.createModelSetModule(getClass()
				.getClassLoader());

		IModelSetFactory factory = Guice.createInjector(
				new ModelSetModule(module)).getInstance(IModelSetFactory.class);

		modelSet = factory.createModelSet(MODELS.NAMESPACE_URI
				.appendFragment("MemoryModelSet"));
	}

	@After
	public void afterTest() throws Exception {
		modelSet.dispose();
	}

	@Test
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
}
