/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.core;

/**
 * Describes the inferencing capabilities of an RDF store.
 * <p>
 * The inferencing capabilities can be evaluated to decide if inferencing of
 * types and values has to be implemented within the program or if it is already
 * provided by an underlying RDF store.
 */
public interface InferencingCapability {
	/**
	 * An inferencing capability describing that the RDF store does no inferencing
	 * at all.
	 */
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

	/**
	 * Whether the RDF store does some (unspecified) kind of OWL reasoning.
	 * <p>
	 * It should be expected that at least reasoning of sub properties and
	 * transitive properties is working.
	 * 
	 * @return <code>true</code> if the RDF store does OWL reasoning, else
	 *         <code>false</code>
	 */
	boolean doesOWL();

	/**
	 * Whether the RDF store does RDFS reasoning.
	 * <p>
	 * It should be expected that at least sub class reasoning is working.
	 * 
	 * @return <code>true</code> if the RDF store does RDFS reasoning, else
	 *         <code>false</code>
	 */
	boolean doesRDFS();

	/**
	 * Whether the inferred statements reside in the default graph (normal behaviour
	 * of RDF4J) or are made available as part of the queried named graphs.
	 * 
	 * @return <code>true</code> if the inferred statements are within the default
	 *         graph, else <code>false</code>
	 */
	boolean inDefaultGraph();
}
