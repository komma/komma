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

import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.URI;

/**
 * Factory interface for creating instances of {@link IModelSet}.
 */
public interface IModelSetFactory {
	/**
	 * Create a model set instance for the given RDF types.
	 * 
	 * @param modelSetTypes
	 *            RDF types of the new model set
	 * @return A model set instance
	 */
	IModelSet createModelSet(URI... modelSetTypes);

	/**
	 * Create a model set instance by using a configuration specified as RDF graph.
	 * 
	 * @param name
	 *            The name of the model set. Also used to lookup configuration data
	 *            within the given <code>config</code> graph.
	 * @param config
	 *            An RDF graph with configuration data
	 * @return A model set instance
	 */
	IModelSet createModelSet(URI name, IGraph config);
}
