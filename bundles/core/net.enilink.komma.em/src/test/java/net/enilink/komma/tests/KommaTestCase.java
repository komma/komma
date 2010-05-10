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

import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public abstract class KommaTestCase extends TestCase {
	public KommaTestCase() {
		super("");
	}

	public static Test suite(Class<? extends TestCase> subclass)
			throws Exception {
		String sname = subclass.getName();
		TestSuite suite = new TestSuite(sname);
		for (Method method : subclass.getMethods()) {
			String name = method.getName();
			if (name.startsWith("test")) {
				TestCase test = subclass.newInstance();
				test.setName(name);
				suite.addTest(test);
			}
		}
		return suite;
	}
}
