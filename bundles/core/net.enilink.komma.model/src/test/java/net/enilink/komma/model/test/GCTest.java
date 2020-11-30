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
package net.enilink.komma.model.test;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.cache.CacheBuilder;
import com.google.inject.Guice;
import com.google.inject.Module;

import net.enilink.komma.core.BlankNode;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.CacheModule;
import net.enilink.komma.em.DecoratingEntityManagerModule;
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
public class GCTest {
	IModelSet modelSet;

	@Before
	public void beforeTest() throws Exception {
		KommaModule module = ModelPlugin.createModelSetModule(getClass().getClassLoader());
		// overwrite the default cache configuration to expire elements after a
		// very short lifespan
		CacheModule.DEFAULT_BUILDER = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MILLISECONDS);
		IModelSetFactory factory = Guice.createInjector(new ModelSetModule(module) {
			@Override
			protected Module getEntityManagerModule() {
				return new DecoratingEntityManagerModule();
			}
		}).getInstance(IModelSetFactory.class);
		modelSet = factory.createModelSet(MODELS.NAMESPACE_URI.appendLocalPart("MemoryModelSet"));
	}

	@After
	public void afterTest() throws Exception {
		modelSet.dispose();
	}

	/**
	 * Test the garbage collection of models. KOMMA should support the creation
	 * and disposal of an unlimited number of models.
	 */
	@Test
	public void testGC() throws Exception {
		int count = 30;
		final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(15);
		final Set<Reference<IModel>> refs = Collections.synchronizedSet(new HashSet<Reference<IModel>>());
		final ReferenceQueue<IModel> refQueue = new ReferenceQueue<>();
		class TestRunnable implements Runnable {
			@Override
			public void run() {
				IUnitOfWork uow = modelSet.getUnitOfWork();
				uow.begin();

				IModel model = modelSet.createModel(URIs.createURI("test:model:" + UUID.randomUUID().toString()));
				refs.add(new WeakReference<>(model, refQueue));

				modelSet.getDataChangeSupport().setEnabled(null, false);
				ITransaction transaction = model.getManager().getTransaction();
				transaction.begin();
				try {
					// add some classes and restrictions
					URI name = URIs.createURI("class:" + BlankNode.generateId().substring(2));
					Class c = model.getManager().createNamed(name, Class.class);
					c.setRdfsLabel(name.toString());
					Restriction r = model.getManager().create(Restriction.class);
					r.setOwlOnProperty(model.getManager().find(RDFS.PROPERTY_LABEL, OwlProperty.class));
					r.setOwlMaxCardinality(BigInteger.valueOf(1));
					c.getRdfsSubClassOf().add(r);
					transaction.commit();

					// read some data
					c.getRdfsLabel();
					for (net.enilink.vocab.rdfs.Class superClass : c.getRdfsSubClassOf()) {
						if (superClass instanceof Restriction) {
							((Restriction) superClass).getOwlMaxCardinality();
						}
					}
				} finally {
					if (transaction.isActive()) {
						transaction.rollback();
					}
					uow.end();
				}
			}
		}

		System.out.println("Start");
		for (int i = 0; i < count; i++) {
			executorService.scheduleWithFixedDelay(new TestRunnable(), 0, (int) (1 + Math.random() * 20),
					TimeUnit.MILLISECONDS);
		}

		// run some iterations
		while (refs.size() < 300) {
			Thread.sleep(500);
		}

		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);

		// ask for removal of weak references
		for (int i = 0; i < 3; i++) {
			System.gc();
			Thread.sleep(100);
		}

		System.out.println("Created references: " + refs.size());
		// try to remove the model references
		Reference<?> ref;
		while ((ref = refQueue.remove(1000)) != null) {
			refs.remove(ref);
		}
		System.out.println("Remaining reference: " + refs.size());
	}
}