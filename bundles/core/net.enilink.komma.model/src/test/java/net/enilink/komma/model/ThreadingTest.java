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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;

import net.enilink.komma.core.BlankNode;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelSetModule;
import net.enilink.vocab.owl.Class;
import net.enilink.vocab.owl.OwlProperty;
import net.enilink.vocab.owl.Restriction;
import net.enilink.vocab.rdfs.RDFS;

/**
 * Stress tests for using the model API in a multi-threaded environment.
 */
public class ThreadingTest {
	IModelSet modelSet;

	@Before
	public void beforeTest() throws Exception {
		KommaModule module = ModelPlugin.createModelSetModule(getClass().getClassLoader());
		IModelSetFactory factory = Guice.createInjector(new ModelSetModule(module)).getInstance(IModelSetFactory.class);
		modelSet = factory.createModelSet(MODELS.NAMESPACE_URI.appendLocalPart("MemoryModelSet"));
	}

	@After
	public void afterTest() throws Exception {
		modelSet.dispose();
	}

	@Test
	public void testThreading() throws Exception {
		final IModel model = modelSet.createModel(URIs.createURI("test:model1"));
		int count = 15;
		final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);
		final AtomicInteger iterations = new AtomicInteger();
		class TestRunnable implements Runnable {
			@Override
			public void run() {
				IUnitOfWork uow = modelSet.getUnitOfWork();
				uow.begin();
				modelSet.getDataChangeSupport().setEnabled(null, false);
				ITransaction transaction = model.getManager().getTransaction();
				transaction.begin();
				try {
					iterations.incrementAndGet();
					// add some classes and restrictions
					URI name = URIs.createURI("class:" + BlankNode.generateId().substring(2));
					Class c = model.getManager().createNamed(name, Class.class);
					c.setRdfsLabel(name.toString());
					Restriction r = model.getManager().create(Restriction.class);
					r.setOwlOnProperty(model.getManager().find(RDFS.PROPERTY_LABEL, OwlProperty.class));
					r.setOwlMaxCardinality(BigInteger.valueOf(1));
					c.getRdfsSubClassOf().add(r);

					transaction.commit();
				} finally {
					if (transaction.isActive()) {
						transaction.rollback();
					}
					uow.end();
				}
			}
		}

		System.out.println("Start");
		long start = System.currentTimeMillis();
		List<ScheduledFuture<?>> futures = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			futures.add(executorService.scheduleWithFixedDelay(new TestRunnable(), 0, (int) (1 + Math.random() * 2), //
					TimeUnit.MILLISECONDS));
		}

		// wait until some iterations are done
		Thread.sleep(5000);

		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);
		long end = System.currentTimeMillis();

		// ensure that weak reference are removed
		for (int i = 0; i < 3; i++) {
			System.gc();
		}
		System.out.println("Number of iterations: " + iterations.get() + " in " + ((end - start) / 1000) + " seconds.");
	}
}