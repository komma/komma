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
package net.enilink.komma.edit.ui.assist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.parboiled.MatchHandler;
import org.parboiled.MatcherContext;
import org.parboiled.Rule;
import org.parboiled.matchers.AbstractMatcher;
import org.parboiled.matchers.ActionMatcher;
import org.parboiled.matchers.CharIgnoreCaseMatcher;
import org.parboiled.matchers.CharMatcher;
import org.parboiled.matchers.FirstOfMatcher;
import org.parboiled.matchers.Matcher;
import org.parboiled.matchers.OneOrMoreMatcher;
import org.parboiled.matchers.OptionalMatcher;
import org.parboiled.matchers.SequenceMatcher;
import org.parboiled.matchers.ZeroOrMoreMatcher;
import org.parboiled.matchervisitors.DefaultMatcherVisitor;
import org.parboiled.matchervisitors.FollowMatchersVisitor;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.Characters;
import org.parboiled.support.Chars;
import org.parboiled.support.ParsingResult;

public class ParboiledProposalProvider implements IContentProposalProvider {
	static class CollectProposalsVisitor extends DefaultMatcherVisitor<Boolean> {
		Set<Matcher> active = new HashSet<Matcher>();
		int optional = 0;

		ISemanticProposalProvider proposalProvider;

		Map<ISemanticProposal, String> semanticProposals;

		Characters separators = Characters.of(' ', '\n', '\r', '\t', '\f',
				Chars.EOI);
		StringBuilder word;
		LinkedHashSet<String> words = new LinkedHashSet<String>();

		public CollectProposalsVisitor(
				ISemanticProposalProvider proposalProvider,
				Map<ISemanticProposal, String> semanticProposals) {
			this.proposalProvider = proposalProvider;
			this.semanticProposals = semanticProposals;
		}

		protected boolean addSemanticProposal(Matcher matcher) {
			if (proposalProvider == null) {
				return false;
			}

			ISemanticProposal proposal = proposalProvider.getProposal(matcher
					.getLabel());
			if (proposal != null && !semanticProposals.containsKey(proposal)) {
				semanticProposals.put(proposal, "");
				return true;
			}
			return false;
		}

		@Override
		public Boolean defaultValue(AbstractMatcher matcher) {
			addSemanticProposal(matcher);

			return word.length() > 0;
		}

		public LinkedHashSet<String> getWords() {
			return words;
		}

		boolean isOptional() {
			return optional > 0;
		}

		public void process(Matcher matcher) {
			active.clear();

			word = new StringBuilder();
			if (matcher.accept(this) && word.length() > 0) {
				words.add(word.toString());
			}
		}

		@Override
		public Boolean visit(ActionMatcher matcher) {
			return false;
		}

		@Override
		public Boolean visit(CharIgnoreCaseMatcher matcher) {
			addSemanticProposal(matcher);

			word.append(matcher.charLow);
			return false;
		}

		@Override
		public Boolean visit(CharMatcher matcher) {
			addSemanticProposal(matcher);

			word.append(matcher.character);
			return false;
		}

		@Override
		public Boolean visit(FirstOfMatcher matcher) {
			if (!active.add(matcher)) {
				return false;
			}
			addSemanticProposal(matcher);

			boolean complete = false;
			for (Matcher child : matcher.getChildren()) {
				int length = word.length();

				boolean accept = child.accept(this);
				complete |= accept;
				if (accept && word.length() > length) {
					words.add(word.toString());
				}
				word.replace(length, word.length(), "");
			}
			active.remove(matcher);
			return complete;
		}

		@Override
		public Boolean visit(OneOrMoreMatcher matcher) {
			if (!active.add(matcher)) {
				return false;
			}
			addSemanticProposal(matcher);

			try {
				return matcher.subMatcher.accept(this);
			} finally {
				active.remove(matcher);
			}
		}

		@Override
		public Boolean visit(OptionalMatcher matcher) {
			addSemanticProposal(matcher);

			if (word.length() > 0) {
				return true;
			}

			if (!active.add(matcher)) {
				return false;
			}

			try {
				optional++;
				return matcher.subMatcher.accept(this);
			} finally {
				optional--;
				active.remove(matcher);
			}
		}

		@Override
		public Boolean visit(SequenceMatcher matcher) {
			if (!active.add(matcher)) {
				return false;
			}

			addSemanticProposal(matcher);

			for (Matcher child : matcher.getChildren()) {
				if (child.accept(this)) {
					return true;
				}
			}
			active.remove(matcher);
			return false;
		}

		@Override
		public Boolean visit(ZeroOrMoreMatcher matcher) {
			addSemanticProposal(matcher);

			if (word.length() > 0) {
				return true;
			}

			if (!active.add(matcher)) {
				return false;
			}

			try {
				optional++;
				return matcher.subMatcher.accept(this);
			} finally {
				optional--;
				active.remove(matcher);
			}
		}
	}

	static class ProposalParseRunner extends BasicParseRunner<Object> {
		public class Handler implements MatchHandler {
			public boolean match(MatcherContext<?> context) {
				boolean matched = context.getMatcher().match(context);
				if (proposalIndex == 0 && context.getCurrentIndex() == 0
						|| proposalIndex == context.getCurrentIndex() + 1) {
					List<Matcher> matchers = new FollowMatchersVisitor()
							.getFollowMatchers(context);
					for (Matcher matcher : matchers) {
						visitor.process(matcher);
					}

					if (proposalProvider != null) {
						do {
							ISemanticProposal proposal = proposalProvider
									.getProposal(context.getMatcher()
											.getLabel());
							if (proposal != null
									&& !semanticProposals.containsKey(proposal)) {
								semanticProposals.put(
										proposal,
										context.getInputBuffer().extract(
												context.getStartIndex(),
												proposalIndex));
								break;
							}
							context = context.getParent();
						} while (context != null);
					}
				}
				return matched;
			}

			public boolean matchRoot(MatcherContext<?> rootContext) {
				return rootContext.runMatcher();
			}
		}

		int proposalIndex;
		ISemanticProposalProvider proposalProvider;
		Map<ISemanticProposal, String> semanticProposals = new HashMap<ISemanticProposal, String>();
		CollectProposalsVisitor visitor;

		ProposalParseRunner(Rule rule, int proposalIndex,
				ISemanticProposalProvider proposalProvider) {
			super(rule);
			this.proposalIndex = proposalIndex;
			this.proposalProvider = proposalProvider;
		}

		public Map<ISemanticProposal, String> getSemanticProposals() {
			return semanticProposals;
		}

		public Set<String> getWords() {
			return new TreeSet<String>(visitor.getWords());
		}

		@Override
		protected boolean runRootContext() {
			visitor = new CollectProposalsVisitor(proposalProvider,
					semanticProposals);
			return runRootContext(new Handler(), true);
		}
	}

	ISemanticProposalProvider semanticProposalProvider;
	Rule rule;

	public ParboiledProposalProvider(Rule rule,
			ISemanticProposalProvider semanticProposalProvider) {
		this.rule = rule;
		this.semanticProposalProvider = semanticProposalProvider;
	}

	@Override
	public IContentProposal[] getProposals(String contents, int position) {
		List<IContentProposal> proposals = new ArrayList<IContentProposal>();
		ProposalParseRunner runner = new ProposalParseRunner(rule, position,
				semanticProposalProvider);
		ParsingResult<Object> result = runner.run(contents);
		// if (result.hasErrors() || result.resultValue == null) {
		// result = new RecoveringParseRunner<Object>(parser.Query())
		// .run(contents);
		// }

		for (Map.Entry<ISemanticProposal, String> entry : runner
				.getSemanticProposals().entrySet()) {
			IContentProposal[] computedProposals = entry.getKey().compute(
					result, position, entry.getValue());
			if (computedProposals != null) {
				for (IContentProposal proposal : computedProposals) {
					proposals.add(proposal);
				}
			}
		}

		for (String word : runner.getWords()) {
			proposals.add(new ContentProposal(word, word, word, word.length()));
		}

		return proposals.toArray(new IContentProposal[proposals.size()]);
	}
}