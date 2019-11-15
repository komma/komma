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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.enilink.komma.parser.sparql.tree.expr.Expression;
import net.enilink.komma.parser.sparql.tree.visitor.Visitable;
import net.enilink.komma.parser.sparql.tree.visitor.Visitor;

public class Dataset implements Visitable {
	protected Set<Expression> defaultGraphs = new LinkedHashSet<Expression>();

	protected Set<Expression> namedGraphs = new LinkedHashSet<Expression>();

	public Dataset() {
	}

	public Dataset(Set<Expression> defaultGraphs, Set<Expression> namedGraphs) {
		this.defaultGraphs.addAll(defaultGraphs);
		this.namedGraphs.addAll(namedGraphs);
	}

	public Set<Expression> getDefaultGraphs() {
		return Collections.unmodifiableSet(defaultGraphs);
	}

	/**
	 * Adds a graph URI to the set of default graph URIs.
	 */
	public boolean addDefaultGraph(Expression graph) {
		return defaultGraphs.add(graph);
	}

	/**
	 * Removes a graph URI from the set of default graph URIs.
	 * 
	 * @return <tt>true</tt> if the URI was removed from the set, <tt>false</tt>
	 *         if the set did not contain the URI.
	 */
	public boolean removeDefaultGraph(Expression graph) {
		return defaultGraphs.remove(graph);
	}

	/**
	 * Gets the (unmodifiable) set of named graph URIs.
	 */
	public Set<Expression> getNamedGraphs() {
		return Collections.unmodifiableSet(namedGraphs);
	}

	/**
	 * Adds a graph URI to the set of named graph URIs.
	 */
	public boolean addNamedGraph(Expression graph) {
		return namedGraphs.add(graph);
	}

	/**
	 * Removes a graph URI from the set of named graph URIs.
	 * 
	 * @return <tt>true</tt> if the URI was removed from the set, <tt>false</tt>
	 *         if the set did not contain the URI.
	 */
	public boolean removeNamedGraph(Expression graph) {
		return namedGraphs.remove(graph);
	}

	/**
	 * Removes all graph URIs (both default and named) from this dataset.
	 */
	public void clear() {
		defaultGraphs.clear();
		namedGraphs.clear();
	}

	public boolean isEmpty() {
		return defaultGraphs.isEmpty() && namedGraphs.isEmpty();
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.dataset(this, data);
	}

	public void add(Dataset dataset) {
		defaultGraphs.addAll(dataset.getDefaultGraphs());
		namedGraphs.addAll(dataset.getNamedGraphs());
	}
}
