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
package net.enilink.komma.sparql.ui.assist;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.parboiled.support.ParsingResult;

import net.enilink.komma.KommaCore;
import net.enilink.komma.edit.ui.assist.ISemanticProposal;
import net.enilink.komma.edit.ui.assist.ISemanticProposalProvider;
import net.enilink.komma.parser.sparql.tree.Variable;
import net.enilink.komma.parser.sparql.tree.visitor.TreeWalker;
import net.enilink.komma.parser.sparql.tree.visitor.Visitable;

public class SparqlProposals implements ISemanticProposalProvider {
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

	class Proposal implements ISemanticProposal {
		Method m;

		public Proposal(Method m) {
			this.m = m;
		}

		@Override
		public IContentProposal[] compute(ParsingResult<?> result, int index,
				String prefix) {
			try {
				return (IContentProposal[]) m.invoke(SparqlProposals.this,
						result, index, prefix);
			} catch (Exception e) {
				KommaCore.log(e);
			}
			return null;
		}
	}

	Map<String, Proposal> proposals = new HashMap<String, Proposal>();

	public SparqlProposals() {
		for (Method m : getClass().getMethods()) {
			if (IContentProposal[].class.equals(m.getReturnType())) {
				Class<?>[] paramTypes = m.getParameterTypes();
				if (paramTypes.length == 3
						&& ParsingResult.class.equals(paramTypes[0])
						&& int.class.equals(paramTypes[1])
						&& String.class.equals(paramTypes[2])) {
					proposals.put(m.getName(), new Proposal(m));
				}
			}
		}
	}

	@Override
	public ISemanticProposal getProposal(String rule) {
		return proposals.get(rule);
	}

	public IContentProposal[] Var(ParsingResult<?> result, int index,
			String prefix) {
		if (result.resultValue instanceof Visitable) {
			List<IContentProposal> results = new ArrayList<IContentProposal>();
			for (String var : new VarCollector()
					.process((Visitable) result.resultValue)) {
				String content = '?' + var;
				// content = content.substring(prefix.length());
				results.add(new ContentProposal(content, content, content,
						content.length()));
			}
			return results.toArray(new IContentProposal[results.size()]);
		}
		return null;
	}
}
