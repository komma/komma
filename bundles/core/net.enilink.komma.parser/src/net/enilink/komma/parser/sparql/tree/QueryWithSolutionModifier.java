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
package net.enilink.komma.parser.sparql.tree;

public abstract class QueryWithSolutionModifier extends Query {
	protected LimitModifier limitModifier;
	protected OffsetModifier offsetModifier;
	protected OrderModifier orderModifier;

	public QueryWithSolutionModifier(Dataset dataset, Graph graph,
			SolutionModifier... modifiers) {
		super(dataset, graph);

		for (SolutionModifier modifier : modifiers) {
			if (modifier instanceof LimitModifier) {
				assertNull(LimitModifier.class, limitModifier);
				limitModifier = (LimitModifier) modifier;
			} else if (modifier instanceof OffsetModifier) {
				assertNull(OffsetModifier.class, offsetModifier);
				offsetModifier = (OffsetModifier) modifier;
			} else if (modifier instanceof OrderModifier) {
				assertNull(OrderModifier.class, orderModifier);
				orderModifier = (OrderModifier) modifier;
			}
		}
	}

	public LimitModifier getLimitModifier() {
		return limitModifier;
	}

	public OffsetModifier getOffsetModifier() {
		return offsetModifier;
	}

	public OrderModifier getOrderModifier() {
		return orderModifier;
	}

	protected <T> void assertNull(Class<T> type, T value) {
		if (value != null) {
			throw new IllegalArgumentException("Modifier of type " + type
					+ " may only be assigned once.");
		}
	}
}
