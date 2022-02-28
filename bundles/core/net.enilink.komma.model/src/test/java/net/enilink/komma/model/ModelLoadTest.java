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
package net.enilink.komma.model;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.enilink.komma.core.KommaModule;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelSetModule;
import net.enilink.vocab.komma.KOMMA;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;

public class ModelLoadTest {
	IModelSet modelSet;

	@Before
	public void beforeTest() throws Exception {
		KommaModule module = ModelPlugin.createModelSetModule(getClass()
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
		IModel model = modelSet.createModel(KOMMA.NAMESPACE_URI
				.trimFragment());
		Map<Object, Object> options = new HashMap<>();
		options.put(IModel.OPTION_MIME_TYPE, "text/turtle");
		model.load(getClass().getResourceAsStream("/simple-model.ttl"), options);

		List<?> test = model
				.getManager()
				.createQuery(
						"SELECT DISTINCT ?property WHERE { ?property a owl:ObjectProperty }")
				.getResultList();

		assertEquals("The object properties couldn't be resolved.", 2,
				test.size());
	}
}
