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
package net.enilink.komma.tests.map;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.openrdf.model.vocabulary.RDF;

import net.enilink.komma.KommaCore;
import net.enilink.komma.concepts.CONCEPTS;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.base.ModelSetFactory;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.sesame.SesameReference;
import net.enilink.komma.tests.KommaTestCase;

public class LiteralKeyMapTestcase extends KommaTestCase {
	IModelSet modelSet;

	protected void setUp() throws Exception {
		super.setUp();

		KommaModule module = KommaCore.createModelSetModule(getClass()
				.getClassLoader());

		modelSet = new ModelSetFactory(module, URIImpl
				.createURI(MODELS.NAMESPACE + "MemoryModelSet"))
				.createModelSet();
	}

	protected void tearDown() throws Exception {
		modelSet.dispose();

		super.tearDown();
	}

	public void testPutGetRemove() throws Exception {

		Map map = setUpMap();

		IModel model = modelSet.getModel(CONCEPTS.NAMESPACE_URI.trimFragment(),
				false);

		IObject object = (IObject) model.getManager().create(
				new SesameReference(RDF.LIST));

		assertTrue(object instanceof List);

		List liste1rein = (List) object;
		liste1rein.add(new String("Frank"));
		liste1rein.add(new String("Ken"));
		map.put("liste1", liste1rein);

		object = (IObject) model.getManager().create(
				new SesameReference(RDF.LIST));

		assertTrue(object instanceof List);

		List listeTiereRein = (List) object;
		listeTiereRein.add(new String("Katze"));
		listeTiereRein.add(new String("Hamster"));
		listeTiereRein.add(new String("Hund"));
		map.put("listeTiere", listeTiereRein);

		assertTrue(map.containsKey("liste1"));
		assertTrue(map.containsKey("listeTiere"));

		List liste1raus = (List) map.get("liste1");

		assertNotNull(liste1raus);

		Iterator iter = liste1raus.iterator();
		assertEquals(2, liste1raus.size());

		while (iter.hasNext()) {
			assertTrue(liste1rein.contains(iter.next()));
		}

		List removedlist = (List) map.remove("liste1");
		assertEquals(liste1rein, removedlist);
		assertEquals(1, map.size());

	}

	public void testClear() throws Exception {

		Map map = setUpMap();

		IModel model = modelSet.getModel(CONCEPTS.NAMESPACE_URI.trimFragment(),
				false);

		IObject object = (IObject) model.getManager().create(
				new SesameReference(RDF.LIST));

		assertTrue(object instanceof List);

		List liste1rein = (List) object;
		liste1rein.add(new String("Frank"));
		liste1rein.add(new String("Ken"));
		map.put("liste1", liste1rein);

		object = (IObject) model.getManager().create(
				new SesameReference(RDF.LIST));

		assertTrue(object instanceof List);

		List listeTiereRein = (List) object;
		listeTiereRein.add(new String("Katze"));
		listeTiereRein.add(new String("Hamster"));
		listeTiereRein.add(new String("Hund"));
		map.put("listeTiere", listeTiereRein);

		assertEquals(2, map.size());
		map.clear();
		assertEquals(0, map.size());
		assertNull(map.get("liste1"));
		assertNull(map.get("listeTiere"));

	}

	public void testEntrySet() throws Exception {
		Map map = setUpMap();

		IModel model = modelSet.getModel(CONCEPTS.NAMESPACE_URI.trimFragment(),
				false);

		IObject object = (IObject) model.getManager().create(
				new SesameReference(RDF.LIST));

		assertTrue(object instanceof List);

		List liste1rein = (List) object;
		liste1rein.add(new String("Frank"));
		liste1rein.add(new String("Ken"));
		map.put("liste1", liste1rein);

		object = (IObject) model.getManager().create(
				new SesameReference(RDF.LIST));

		assertTrue(object instanceof List);

		List listeTiereRein = (List) object;
		listeTiereRein.add(new String("Katze"));
		listeTiereRein.add(new String("Hamster"));
		listeTiereRein.add(new String("Hund"));
		map.put("listeTiere", listeTiereRein);

		Set<java.util.Map.Entry<Object, Object>> entrySet = map.entrySet();
		Iterator<Map.Entry<Object, Object>> iter = entrySet.iterator();

		while (iter.hasNext()) {
			Map.Entry<Object, Object> entry = iter.next();

			assertTrue((entry.getValue() instanceof List));

			if (entry.getKey().equals("liste1")) {
				assertEquals(liste1rein, entry.getValue());
			} else if (entry.getKey().equals("listeTiere")) {
				assertEquals(listeTiereRein, entry.getValue());
			} else {
				fail("Iterator liefert falsche Eintr�ge");
			}

			assertTrue(entrySet.contains(entry));

		}
	}

	private Map setUpMap() throws Exception {
		IModel model = modelSet.createModel(CONCEPTS.NAMESPACE_URI
				.trimFragment());

		IObject object = (IObject) model.getManager().create(
				CONCEPTS.TYPE_LITERALKEYMAP);

		assertTrue(object instanceof Map);

		return (Map) object;
	}

	public static Test suite() throws Exception {
		return suite(LiteralKeyMapTestcase.class);
	}
}
