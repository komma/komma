/*******************************************************************************
 * Copyright (c) 2022 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em.internal;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.NiceIterator;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IStatementSource;
import net.enilink.komma.core.IValue;

import java.util.Arrays;
import java.util.List;

/**
 * A statement source that combines multiple other sources into a single view.
 */
public class CompositeStatementSource implements IStatementSource {
	protected final List<IStatementSource> sources;

	public CompositeStatementSource(IStatementSource... sources) {
		this(Arrays.asList(sources));
	}

	public CompositeStatementSource(List<IStatementSource> sources) {
		this.sources = sources;
	}

	@Override
	public boolean hasMatch(IReference subject, IReference predicate, IValue object, boolean includeInferred, IReference... contexts) {
		return sources.stream().anyMatch(source -> source.hasMatch(subject, predicate, object, includeInferred, contexts));
	}

	@Override
	public IExtendedIterator<IStatement> match(IReference subject, IReference predicate, IValue object, boolean includeInferred, IReference... contexts) {
		IExtendedIterator<IStatement> it = NiceIterator.emptyIterator();
		for (IStatementSource source : sources) {
			it = it.andThen(source.match(subject, predicate, object, includeInferred, contexts));
		}
		return it;
	}
}
