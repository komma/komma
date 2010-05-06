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
package net.enilink.komma.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import net.enilink.komma.core.KommaModule;
import net.enilink.komma.sesame.DecoratingSesameManagerFactory;
import net.enilink.komma.sesame.ISesameManager;

public abstract class KommaManagerTestCase extends KommaTestCase {
	protected Repository repository;

	protected KommaModule module;

	protected ISesameManager manager;

	@Override
	protected void setUp() throws Exception {
		repository = new SailRepository(new MemoryStore());
		repository.initialize();
		RepositoryConnection conn = repository.getConnection();
		conn.clear();
		conn.clearNamespaces();
		conn.close();

		module = createModule();

		DecoratingSesameManagerFactory managerFactory = new DecoratingSesameManagerFactory(
				module, repository);
		manager = managerFactory.createKommaManager();
	}

	protected KommaModule createModule() throws Exception {
		return new KommaModule(getClass().getClassLoader());
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			if (manager.isOpen()) {
				manager.close();
			}
			repository.shutDown();
		} catch (Exception e) {
		}
	}

	public static Test suite() throws Exception {
		return new TestSuite();
	}
}