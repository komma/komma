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
package net.enilink.komma.parser.sparql.tree;

import net.enilink.komma.parser.sparql.tree.visitor.Visitable;

public abstract class Query implements Visitable {
	protected Prologue prologue;
	protected Dataset dataset;
	protected Graph graph;

	protected Query(Dataset dataset, Graph graph) {
		this.dataset = dataset;
		this.graph = graph;
	}

	public void setPrologue(Prologue prologue) {
		this.prologue = prologue;
	}

	public Prologue getPrologue() {
		return prologue;
	}

	public Dataset getDataset() {
		return dataset;
	}

	public Graph getGraph() {
		return graph;
	}

	public void setGraph(Graph graph) {
		this.graph = graph;
	}
}
