/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.core;

public interface InferencingCapability {
	final static InferencingCapability NONE = new InferencingCapability() {
		@Override
		public boolean doesRDFS() {
			return false;
		}

		@Override
		public boolean doesOWL() {
			return false;
		}

		public boolean inDefaultGraph() {
			return false;
		}
	};

	boolean doesOWL();

	boolean doesRDFS();

	boolean inDefaultGraph();
}
