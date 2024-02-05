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
import net.enilink.komma.model.*;
import net.enilink.vocab.owl.Restriction;
import net.enilink.vocab.rdf.RDF;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RepositoryManagerModelSetTest {
	Path tempDir;

	@Before
	public void setup() throws IOException {
		tempDir = Files.createTempDirectory("repomanager-modelset");
	}

	@After
	public void cleanup() throws IOException {
		FileUtils.deleteDirectory(tempDir.toFile());
	}

	@Test
	public void testMemoryConfig() throws IOException {
		Path repoDir = tempDir.resolve("repositories").resolve("memory");
		Files.createDirectories(repoDir);
		Files.copy(getClass().getResourceAsStream("/memory-config.ttl"), repoDir.resolve("config.ttl"));

		// create configuration and a model set factory
		KommaModule module = ModelPlugin.createModelSetModule(getClass().getClassLoader());
		IModelSetFactory factory = Guice.createInjector(new ModelSetModule(module)).getInstance(IModelSetFactory.class);

		IGraph config = new LinkedHashGraph();
		URI msUri = URIs.createURI("urn:enilink:data");
		config.add(msUri, RDF.PROPERTY_TYPE, MODELS.NAMESPACE_URI.appendFragment("RepositoryManagerModelSet"));
		config.add(msUri, MODELS.NAMESPACE_URI.appendFragment("baseDir"), URIs.createURI(tempDir.toUri().toString()));
		config.add(msUri, MODELS.NAMESPACE_URI.appendFragment("repositoryID"), "memory");

		IModelSet modelSet = factory.createModelSet(msUri, config);
		Assert.assertTrue(modelSet.createModel(URIs.createURI("test:model"))
				.getManager().create(Restriction.class) instanceof Restriction);
		modelSet.dispose();
	}
}