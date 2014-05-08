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
package net.enilink.komma.model.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;

import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.NotificationFilter;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelSetModule;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.concepts.IClass;

public class ModelTest {
	IModelSet modelSet;

	@Before
	public void beforeTest() throws Exception {
		KommaModule module = ModelPlugin.createModelSetModule(getClass()
				.getClassLoader());
		IModelSetFactory factory = Guice.createInjector(
				new ModelSetModule(module)).getInstance(IModelSetFactory.class);
		modelSet = factory.createModelSet(MODELS.NAMESPACE_URI
				.appendLocalPart("MemoryModelSet"));
	}

	@After
	public void afterTest() throws Exception {
		modelSet.dispose();
	}

	@Test
	public void testNotifications() throws Exception {
		IModel model = modelSet.createModel(URIs
				.createURI("http://iwu.fraunhofer.de/test/model1"));
		final boolean[] notified = new boolean[] { false };
		final Object[] subject = new Object[1];

		modelSet.addListener(new INotificationListener<INotification>() {
			@Override
			public NotificationFilter<INotification> getFilter() {
				return NotificationFilter
						.instanceOf(IStatementNotification.class);
			}

			@Override
			public void notifyChanged(
					Collection<? extends INotification> notifications) {
				for (INotification notification : notifications) {
					subject[0] = ((IStatementNotification) notification)
							.getSubject();

					System.out.println("changed: " + notification);
				}
				notified[0] = true;
			}
		});
		IClass resource = (IClass) model.getManager().create(
				net.enilink.vocab.owl.Class.class);

		assertEquals("Reference is unequal to resource", subject[0], resource);
		assertEquals("Resource is unequal to reference", resource, subject[0]);

		assertTrue(notified[0]);
	}
}