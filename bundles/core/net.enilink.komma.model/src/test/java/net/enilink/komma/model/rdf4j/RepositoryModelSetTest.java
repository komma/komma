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

import com.google.inject.Guice;
import net.enilink.komma.core.*;
import net.enilink.komma.core.visitor.IDataVisitor;
import net.enilink.komma.model.*;
import net.enilink.vocab.owl.Restriction;
import org.junit.Assert;
import org.junit.Test;

public class RepositoryModelSetTest {
	@Test
	public void testBasicConfig() {
		// create configuration and a model set factory
		KommaModule module = ModelPlugin.createModelSetModule(getClass().getClassLoader());
		IModelSetFactory factory = Guice.createInjector(new ModelSetModule(module)).getInstance(IModelSetFactory.class);

		IGraph config = new LinkedHashGraph();
		ModelUtil.readData(getClass().getResourceAsStream("/repository-modelset-config.ttl"), null,
				"text/turtle", new IDataVisitor<Object>() {
			@Override
			public Object visitBegin() {
				return null;
			}

			@Override
			public Object visitEnd() {
				return null;
			}

			@Override
			public Object visitStatement(IStatement stmt) {
				return config.add(stmt);
			}
		});

		IModelSet modelSet = factory.createModelSet(URIs.createURI("urn:enilink:data"), config);
		Assert.assertTrue(modelSet.createModel(URIs.createURI("test:model"))
				.getManager().create(Restriction.class) instanceof Restriction);
		modelSet.dispose();
	}
}