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
import static org.junit.Assert.assertFalse;
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
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.vocab.owl.OWL;

public class ModelTest {
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
	public void testNotifications() throws Exception {
		IModel model = modelSet.createModel(URIs.createURI("http://iwu.fraunhofer.de/test/model1"));
		final boolean[] notified = new boolean[]{false};
		final Object[] subject = new Object[1];

		modelSet.addListener(new INotificationListener<>() {
			@Override
			public NotificationFilter<INotification> getFilter() {
				return NotificationFilter.instanceOf(IStatementNotification.class);
			}

			@Override
			public void notifyChanged(Collection<? extends INotification> notifications) {
				for (INotification notification : notifications) {
					if (notification instanceof IStatementNotification statementNotification) {
						if (statementNotification.getStatement().isInferred()) {
							continue;
						}
						System.out.println("changed: " + statementNotification.getSubject());
						subject[0] = statementNotification.getSubject();
					}
				}
				notified[0] = true;
			}
		});
		IClass resource = (IClass) model.getManager().create(net.enilink.vocab.owl.Class.class);

		assertEquals("Reference is unequal to resource", subject[0], resource);
		assertEquals("Resource is unequal to reference", resource, subject[0]);

		assertTrue(notified[0]);
	}

	@Test
	public void testOwlImportsUpdateReflectsAccessViaSparqlAndApi() throws Exception {
		assertOwlImportsUpdateReflectsAccess(true);
		assertOwlImportsUpdateReflectsAccess(false);
	}

	private void assertOwlImportsUpdateReflectsAccess(boolean useSparqlUpdate) {
		String suffix = useSparqlUpdate ? "sparql" : "api";
		URI model1Uri = URIs.createURI("urn:test:model1:" + suffix);
		URI model2Uri = URIs.createURI("urn:test:model2:" + suffix);
		URI model3Uri = URIs.createURI("urn:test:model3:" + suffix);
		URI predicate = URIs.createURI("urn:test:predicate");
		URI model1Subject = URIs.createURI("urn:test:model1-subject:" + suffix);
		URI model2Subject = URIs.createURI("urn:test:model2-subject:" + suffix);
		URI object = URIs.createURI("urn:test:object");

		IModel model1 = modelSet.createModel(model1Uri);
		IModel model2 = modelSet.createModel(model2Uri);
		IModel model3 = modelSet.createModel(model3Uri);

		model1.getManager().add(new Statement(model1Subject, predicate, object));
		model2.getManager().add(new Statement(model2Subject, predicate, object));
		model3.addImport(model1Uri, null);
		model3.addImport(model2Uri, null);

		assertTrue("Model3 should access statements from model1 via owl:imports.",
				model3.getManager().hasMatch(model1Subject, predicate, object));
		assertTrue("Model3 should access statements from model2 via owl:imports.",
				model3.getManager().hasMatch(model2Subject, predicate, object));

		if (useSparqlUpdate) {
			model3.getManager().createUpdate(
					"DELETE DATA { <" + model3Uri + "> <" + OWL.PROPERTY_IMPORTS + "> <" + model2Uri + "> }", null, false)
					.execute();
		} else {
			model3.removeImport(model2Uri);
		}

		assertTrue("Model3 should still access statements from model1.",
				model3.getManager().hasMatch(model1Subject, predicate, object));
		assertFalse("Model3 should lose access to statements from model2 after removing owl:imports.",
				model3.getManager().hasMatch(model2Subject, predicate, object));

		if (useSparqlUpdate) {
			model3.getManager().createUpdate(
					"INSERT DATA { <" + model3Uri + "> <" + OWL.PROPERTY_IMPORTS + "> <" + model2Uri + "> }", null, false)
					.execute();
		} else {
			model3.addImport(model2Uri, null);
		}

		assertTrue("Model3 should regain access to statements from model2 after re-adding owl:imports.",
				model3.getManager().hasMatch(model2Subject, predicate, object));
	}
}