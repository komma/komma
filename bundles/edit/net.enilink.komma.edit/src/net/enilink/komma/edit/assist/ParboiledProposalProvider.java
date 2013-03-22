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
package net.enilink.komma.edit.assist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.parboiled.MatcherContext;
import org.parboiled.Rule;
import org.parboiled.buffers.InputBuffer;
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
		boolean includeSemanticProposals;

		int optional = 0;
		String prefix;

		ISemanticProposalProvider proposalProvider;

		Map<ISemanticProposal, String> semanticProposals;
		Characters separators = Characters.of(' ', '\n', '\r', '\t', '\f',
				Chars.EOI);
		StringBuilder word;
		LinkedHashSet<Proposal> words = new LinkedHashSet<Proposal>();

		public CollectProposalsVisitor(
				ISemanticProposalProvider proposalProvider,
				Map<ISemanticProposal, String> semanticProposals) {
			this.proposalProvider = proposalProvider;
			this.semanticProposals = semanticProposals;
		}

		protected boolean addSemanticProposal(Matcher matcher) {
			if (proposalProvider == null || !includeSemanticProposals) {
				return false;
			}

			ISemanticProposal proposal = proposalProvider.getProposal(matcher
					.getLabel());
			if (proposal != null && !semanticProposals.containsKey(proposal)) {
				semanticProposals.put(proposal, prefix);
				return true;
			}
			return false;
		}

		protected void addWord(String word) {
			if (prefix == null || word.startsWith(prefix)) {
				if (word.equals(prefix)) {
					return;
				}
				String content = prefix == null ? word : word.substring(prefix
						.length());
				words.add(new Proposal(content, word));
			}
		}

		@Override
		public Boolean defaultValue(AbstractMatcher matcher) {
			addSemanticProposal(matcher);

			return word.length() > 0;
		}

		public LinkedHashSet<Proposal> getWords() {
			return words;
		}

		boolean isOptional() {
			return optional > 0;
		}

		public void process(String prefix, boolean includeSemanticProposals,
				Matcher matcher) {
			active.clear();
			this.prefix = prefix;
			this.includeSemanticProposals = includeSemanticProposals;

			word = new StringBuilder();
			if (matcher.accept(this) && word.length() > 0) {
				addWord(word.toString());
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
					addWord(word.toString());
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

	static class Proposal implements Comparable<Proposal> {
		String content;
		String label;

		public Proposal(String content, String label) {
			this.content = content;
			this.label = label;
		}

		@Override
		public int compareTo(Proposal o) {
			return label.compareTo(o.label);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Proposal other = (Proposal) obj;
			if (content == null) {
				if (other.content != null)
					return false;
			} else if (!content.equals(other.content))
				return false;
			if (label == null) {
				if (other.label != null)
					return false;
			} else if (!label.equals(other.label))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((content == null) ? 0 : content.hashCode());
			result = prime * result + ((label == null) ? 0 : label.hashCode());
			return result;
		}

		@Override
		public String toString() {
			return "{content : \"" + content + "\", label : \"" + label + "\"}";
		}
	}

	static class ProposalParseRunner extends BasicParseRunner<Object> {
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

		private String getLastWord(MatcherContext<?> context) {
			// determine the last non-whitespace word
			String prefix = getPrefix(context);
			if (prefix.matches(".*\\s$")) {
				return "";
			}
			String[] words = prefix.split("\\s");
			return words[words.length - 1];
		}

		private String getPrefix(MatcherContext<?> context) {
			return context.getInputBuffer().extract(context.getStartIndex(),
					proposalIndex);
		}

		public Map<ISemanticProposal, String> getSemanticProposals() {
			return semanticProposals;
		}

		public Set<Proposal> getWords() {
			return new TreeSet<Proposal>(visitor.getWords());
		}

		public boolean match(MatcherContext<?> context) {
			boolean matched = context.getMatcher().match(context);

			if (!matched) {
				// TODO improve performance in this case
				if (context.getStartIndex() <= proposalIndex) {
					String prefix = getPrefix(context);
					if (!prefix.matches(".*\\S.*\\s$")) {
						visitor.process(prefix, false, context.getMatcher());
					}
				}
			}
			if (proposalIndex == context.getCurrentIndex()) {
				String lastWord = getLastWord(context);
				List<Matcher> matchers = new FollowMatchersVisitor()
						.getFollowMatchers(context);
				for (Matcher matcher : matchers) {
					visitor.process(lastWord, true, matcher);
				}

				if (proposalProvider != null) {
					do {
						ISemanticProposal proposal = proposalProvider
								.getProposal(context.getMatcher().getLabel());
						if (proposal != null
								&& !semanticProposals.containsKey(proposal)) {
							semanticProposals.put(proposal, getPrefix(context));
							break;
						}
						context = context.getParent();
					} while (context != null);
				}
			}

			return matched;
		}

		@Override
		public ParsingResult<Object> run(InputBuffer inputBuffer) {
			visitor = new CollectProposalsVisitor(proposalProvider,
					semanticProposals);
			return super.run(inputBuffer);
		}
	}

	Rule rule;
	ISemanticProposalProvider semanticProposalProvider;

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

		for (Proposal proposal : runner.getWords()) {
			proposals.add(new ContentProposal(proposal.content, proposal.label,
					proposal.label));
		}

		return proposals.toArray(new IContentProposal[proposals.size()]);
	}
}