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
package net.enilink.komma.sparql.ui.assist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.parboiled.support.ParsingResult;

import net.enilink.komma.edit.assist.ContentProposal;
import net.enilink.komma.edit.assist.IContentProposal;
import net.enilink.komma.edit.assist.ReflectiveSemanticProposals;
import net.enilink.komma.parser.sparql.tree.Variable;
import net.enilink.komma.parser.sparql.tree.visitor.TreeWalker;
import net.enilink.komma.parser.sparql.tree.visitor.Visitable;

public class SparqlProposals extends ReflectiveSemanticProposals {
	static class VarCollector extends TreeWalker<Object> {
		private Set<String> queryVarNames = new HashSet<String>();

		public Collection<String> process(Visitable node) {
			node.accept(this, null);
			return queryVarNames;
		}

		@Override
		public Boolean variable(Variable variable, Object value) {
			queryVarNames.add(variable.getName());

			return variable.getPropertyList().accept(this, value);
		}
	}

	public IContentProposal[] Var(ParsingResult<?> result, int index,
			String prefix) {
		if (result.resultValue instanceof Visitable) {
			List<IContentProposal> results = new ArrayList<IContentProposal>();
			for (String var : new VarCollector()
					.process((Visitable) result.resultValue)) {
				String content = '?' + var;
				// content = content.substring(prefix.length());
				results.add(new ContentProposal(content, content, content));
			}
			return results.toArray(new IContentProposal[results.size()]);
		}
		return null;
	}
}
