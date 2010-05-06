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
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.openrdf.model.Statement;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.result.ModelResult;
import org.openrdf.store.StoreException;

import net.enilink.komma.KommaCore;
import net.enilink.komma.common.util.URIUtil;
import net.enilink.komma.concepts.CONCEPTS;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.base.ModelSetFactory;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.sesame.ISesameManager;
import net.enilink.komma.tests.KommaTestCase;

public class LiteralKeyValueMapTestcase extends KommaTestCase {
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

		Map<Object, String> map = setUpMap();

		map.put("Vorname", "Peter");
		map.put("Nachname", "Wolf");

		assertEquals("Wolf", map.get("Nachname"));
		assertEquals("Peter", map.get("Vorname"));
		assertEquals(2, map.size());

		Object removed = map.remove("Vorname");
		assertEquals("Peter", removed.toString());
		assertEquals(1, map.size());

		assertNull(map.get("Vorname"));

		assertFalse(map.containsKey("Vorname"));
		assertTrue(map.containsKey("Nachname"));

		Object o = map.put("Nachname", "Bl�d");
		assertEquals("Wolf", o.toString());
		assertTrue(map.containsKey("Nachname"));
		assertEquals(1, map.size());
		assertEquals("Bl�d", map.get("Nachname"));

	}

	public void testClear() throws Exception {

		Map<Object, String> map = setUpMap();
		map.put("Vorname", "Peter");
		map.put("Nachname", "Wolf");

		map.clear();

		assertEquals(0, map.size());

		assertNull(map.get("Vorname"));
		assertNull(map.get("Nachname"));

		assertTrue(isRepositoryClean(modelSet.getModels().iterator().next()));

	}

	public void testEntrySet() throws Exception {
		Map<Object, String> map = setUpMap();
		map.put("Vorname", "Peter");
		map.put("Nachname", "Wolf");

		Iterator<Map.Entry<Object, String>> iter = map.entrySet().iterator();

		while (iter.hasNext()) {
			Map.Entry<Object, String> entry = iter.next();

			if (entry.getKey().equals("Vorname")) {
				assertEquals("Peter", entry.getValue());
			} else if (entry.getKey().equals("Nachname")) {
				assertEquals("Wolf", entry.getValue());
			} else {
				fail("Iterator liefert falsche Einträge");
			}

		}
	}

	public void testKeySet() throws Exception {
		Map<Object, String> map = setUpMap();
		map.put("Vorname", "Peter");
		map.put("Nachname", "Wolf");

		Set<Object> keys = map.keySet();
		assertNotNull(keys);

		assertTrue(keys.contains("Vorname"));
		assertTrue(keys.contains("Nachname"));
	}

	@SuppressWarnings("unchecked")
	private Map<Object, String> setUpMap() throws Exception {
		IModel model = modelSet.createModel(CONCEPTS.NAMESPACE_URI
				.trimFragment());

		IObject object = (IObject) model.getManager().create(
				CONCEPTS.TYPE_LITERALKEYVALUEMAP);

		assertTrue(object instanceof Map);

		return (Map<Object, String>) object;
	}

	private boolean isRepositoryClean(IModel model) throws Exception {

		String string = null;
		String tmp = null;
		tmp = isRepositoryCleanSelect(model, URIUtil
				.toSesameUri(CONCEPTS.PROPERTY_ENTRY));
		if (tmp != null)
			string = tmp;

		tmp = isRepositoryCleanSelect(model, URIUtil
				.toSesameUri(CONCEPTS.PROPERTY_KEY));
		if (tmp != null)
			string = string + tmp;

		tmp = isRepositoryCleanSelect(model, URIUtil
				.toSesameUri(CONCEPTS.PROPERTY_KEYDATA));
		if (tmp != null)
			string = string + tmp;

		tmp = isRepositoryCleanSelect(model, URIUtil
				.toSesameUri(CONCEPTS.PROPERTY_VALUE));
		if (tmp != null)
			string = string + tmp;

		tmp = isRepositoryCleanSelect(model, URIUtil
				.toSesameUri(CONCEPTS.PROPERTY_VALUEDATA));
		if (tmp != null)
			string = string + tmp;

		if (string == null) {
			return true;
		} else {
			return false;
		}

	}

	private String isRepositoryCleanSelect(IModel model,
			org.openrdf.model.URI predicate) throws StoreException {
		String string = null;
		ModelResult statements = ((ISesameManager) model.getManager())
				.getConnection().match(null,
						URIUtil.toSesameUri(CONCEPTS.PROPERTY_ENTRY), null);

		while (statements.hasNext()) {
			Statement s = statements.next();
			string = string + s.getSubject() + s.getPredicate() + s.getObject();
		}
		return string;
	}

	public static Test suite() throws Exception {
		return suite(LiteralKeyValueMapTestcase.class);
	}
}
