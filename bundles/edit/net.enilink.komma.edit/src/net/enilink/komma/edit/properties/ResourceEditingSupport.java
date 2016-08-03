package net.enilink.komma.edit.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.enilink.commons.iterator.Filter;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.edit.assist.ContentProposal;
import net.enilink.komma.edit.assist.IContentProposal;
import net.enilink.komma.edit.assist.IContentProposalProvider;
import net.enilink.komma.edit.properties.ResourceFinder.Match;
import net.enilink.komma.edit.properties.ResourceFinder.Options;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.parser.BaseRdfParser;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.QName;
import net.enilink.vocab.rdf.RDF;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.IndexRange;
import org.parboiled.support.ParsingResult;

public class ResourceEditingSupport implements IEditingSupport {
	static class ConstructorParser extends BaseRdfParser {
		public Rule Constructor() {
			return sequence(
					firstOf(sequence(firstOf(IriRef(), PN_LOCAL()), ch('a')),
							ch('a')), WS_NO_COMMENT(),
					firstOf(IriRef(), PN_LOCAL(), sequence(EMPTY, push(""))),
					drop(), push(matchRange()));
		}
	}

	protected class ResourceProposal extends ContentProposal implements
			IResourceProposal {
		IEntity resource;
		boolean useAsValue;
		int score;

		public ResourceProposal(String content, int cursorPosition,
				IEntity resource) {
			super(content, ResourceEditingSupport.this.getLabel(resource),
					ResourceEditingSupport.this.getLabel(resource),
					cursorPosition, false);
			this.resource = resource;
		}

		@Override
		public IEntity getResource() {
			return resource;
		}

		public ResourceProposal setScore(int score) {
			this.score = score;
			return this;
		}

		public int getScore() {
			return score;
		}

		public ResourceProposal setUseAsValue(boolean useAsValue) {
			this.useAsValue = useAsValue;
			return this;
		}

		public boolean getUseAsValue() {
			return useAsValue;
		}
	}

	class ResourceProposalProvider implements IContentProposalProvider {
		IEntity subject;
		IReference predicate;

		ResourceProposalProvider(IEntity subject, IReference predicate) {
			this.subject = subject;
			this.predicate = predicate;
		}

		@Override
		public IContentProposal[] getProposals(String contents, int position) {
			ParsingResult<Object> ctor = new BasicParseRunner<Object>(
					createConstructorParser().Constructor()).run(contents);
			String prefix = "";
			if (ctor.matched) {
				IndexRange range = (IndexRange) ctor.resultValue;
				prefix = contents.substring(0, range.start);
				contents = contents.substring(range.start, range.end);
				position = contents.length();
			}

			int limit = 20;
			Set<IReference> predicates = new LinkedHashSet<>();
			if (!ctor.matched) {
				predicates.add(predicate);
			}
			predicates.add(null);
			Map<IEntity, Match> allMatches = new LinkedHashMap<>();
			Options options = Options.create(subject,
					contents.substring(0, position), limit);
			if (editPredicate) {
				options = options.ofType(RDF.TYPE_PROPERTY);
			}
			// ensures that resources which match the predicate's range are
			// added in front of the result list
			ResourceFinder finder = new ResourceFinder();
			for (IReference p : predicates) {
				if (allMatches.size() >= limit) {
					break;
				}
				for (Match match : finder.findRestrictedResources(options
						.forPredicate(p))) {
					// globally filter duplicate proposals
					Match existing = allMatches.get(match.resource);
					if (existing == null || existing.score() < match.score()) {
						allMatches.put(match.resource, match);
					}
				}
			}
			List<ResourceProposal> resourceProposals = toProposals(
					allMatches.values(), prefix, !ctor.matched);
			Comparator<ResourceProposal> comparator = new Comparator<ResourceProposal>() {
				@Override
				public int compare(ResourceProposal p1, ResourceProposal p2) {
					// higher scores are better and hence should be inserted in
					// front of the list
					int scoreDiff = p2.score - p1.score;
					if (scoreDiff != 0) {
						return scoreDiff;
					}
					return p1.getLabel().compareTo(p2.getLabel());
				}
			};
			Collections.sort(resourceProposals, comparator);
			Collection<? extends IContentProposal> results = resourceProposals;
			if (resourceProposals.size() < limit && !contents.contains(":")) {
				final String contentsFinal = contents;
				List<IContentProposal> mixedProposals = new ArrayList<IContentProposal>(
						resourceProposals);
				try (IExtendedIterator<INamespace> it = subject
						.getEntityManager().getNamespaces()
						.filterKeep(new Filter<INamespace>() {
							public boolean accept(INamespace ns) {
								return ns.getPrefix().startsWith(contentsFinal);
							}
						})) {
					while (it.hasNext() && mixedProposals.size() < limit) {
						INamespace ns = it.next();
						mixedProposals.add(new ContentProposal(ns.getPrefix()
								+ ":", ns.getPrefix() + ":", ns.getURI()
								.toString()));
					}
				}
				results = mixedProposals;
			}
			return results.toArray(new IContentProposal[results.size()]);
		}

		protected List<ResourceProposal> toProposals(Iterable<Match> matches,
				String prefix, boolean useAsValue) {
			List<ResourceProposal> proposals = new ArrayList<>();
			for (Match match : matches) {
				String content = getLabel(match.resource);
				if (content.length() > 0) {
					content = prefix + content;
					proposals.add(new ResourceProposal(content, content
							.length(), match.resource)
							.setUseAsValue(useAsValue).setScore(match.score()));
				}
			}
			return proposals;
		}
	}

	protected IAdapterFactory adapterFactory;
	private boolean editPredicate;

	public ResourceEditingSupport(IAdapterFactory adapterFactory) {
		this(adapterFactory, false);
	}

	public ResourceEditingSupport(IAdapterFactory adapterFactory,
			boolean editPredicate) {
		this.adapterFactory = adapterFactory;
		this.editPredicate = editPredicate;
	}

	protected String getLabel(Object value) {
		if (value == null) {
			return "";
		}
		IItemLabelProvider labelProvider = (IItemLabelProvider) adapterFactory
				.adapt(value, IItemLabelProvider.class);
		if (labelProvider != null) {
			return labelProvider.getText(value);
		}
		return value.toString();
	}

	protected ConstructorParser createConstructorParser() {
		return Parboiled.createParser(ConstructorParser.class);
	}

	/**
	 * Returns the statement that is represented by this element.
	 */
	protected IStatement getStatement(Object element) {
		if (element instanceof IStatement) {
			return (IStatement) element;
		} else {
			return null;
		}
	}

	@Override
	public IProposalSupport getProposalSupport(Object element) {
		final IStatement stmt = getStatement(element);
		if (stmt == null) {
			return null;
		}
		return new IProposalSupport() {
			@Override
			public IContentProposalProvider getProposalProvider() {
				return new ResourceProposalProvider(
						(IEntity) stmt.getSubject(), stmt.getPredicate());
			}

			@Override
			public IItemLabelProvider getLabelProvider() {
				return new IItemLabelProvider() {
					@Override
					public String getText(Object object) {
						if (object instanceof ResourceProposal) {
							IEntity resource = ((ResourceProposal) object).resource;
							IItemLabelProvider labelProvider = (IItemLabelProvider) adapterFactory
									.adapt(resource, IItemLabelProvider.class);
							if (labelProvider != null) {
								return labelProvider.getText(resource);
							}
							return ModelUtil.getLabel(resource);
						}
						return ((ContentProposal) object).getLabel();
					}

					@Override
					public Object getImage(Object object) {
						if (object instanceof ResourceProposal) {
							IEntity resource = ((ResourceProposal) object).resource;
							IItemLabelProvider labelProvider = (IItemLabelProvider) adapterFactory
									.adapt(resource, IItemLabelProvider.class);
							if (labelProvider != null) {
								return labelProvider.getImage(resource);
							}
						}
						return null;
					}
				};
			}

			@Override
			public char[] getAutoActivationCharacters() {
				return null;
			}
		};
	}

	@Override
	public boolean canEdit(Object element) {
		return true;
	}

	@Override
	public Object getEditorValue(Object element) {
		Object value = null;
		IStatement stmt = getStatement(element);
		if (stmt != null) {
			value = editPredicate ? stmt.getPredicate() : stmt.getObject();
		}
		return value != null ? getLabel(value) : "";
	}

	protected URI toURI(IEntityManager manager, Object value) {
		if (value instanceof IriRef) {
			URI uri = URIs.createURI(((IriRef) value).getIri());
			if (uri.isRelative()) {
				URI ns = manager.getNamespace("");
				if (ns != null) {
					if (ns.fragment() != null) {
						uri = ns.appendLocalPart(uri.toString());
					} else {
						uri = uri.resolve(ns);
					}
				} else {
					throw new IllegalArgumentException(
							"Relative IRIs are not supported.");
				}
			}
			return uri;
		} else if (value instanceof QName) {
			String prefix = ((QName) value).getPrefix();
			String localPart = ((QName) value).getLocalPart();
			URI ns;
			if (prefix == null || prefix.trim().length() == 0) {
				prefix = "";
			}
			ns = manager.getNamespace(prefix);
			if (ns != null) {
				return ns.appendLocalPart(localPart);
			}
			throw new IllegalArgumentException("Unknown prefix for QName "
					+ value);
		} else if (value != null) {
			// try to parse value as IRI ref or prefixed name
			ParsingResult<Object> iriRef = new BasicParseRunner<Object>(
					createConstructorParser().IriRef()).run(value.toString());
			if (iriRef.matched) {
				return toURI(manager, iriRef.resultValue);
			}
			URI ns = manager.getNamespace("");
			if (ns != null) {
				return ns.appendLocalPart(value.toString());
			} else {
				throw new IllegalArgumentException(
						"Relative IRIs are not supported.");
			}
		}
		return null;
	}

	protected IEntity getSubject(Object element) {
		if (element instanceof IEntity) {
			return (IEntity) element;
		}
		IStatement stmt = getStatement(element);
		return (IEntity) ((stmt != null && stmt.getSubject() instanceof IEntity) ? stmt
				.getSubject() : null);
	}

	@Override
	public ICommand convertEditorValue(Object editorValue,
			final IEntityManager entityManager, Object element) {
		if (editorValue instanceof IValue) {
			// short-circuit if supplied value is already an RDF resource
			return new IdentityCommand(editorValue);
		}

		String valueStr = editorValue.toString().trim();
		if (valueStr.isEmpty()) {
			return new IdentityCommand("Remove element.");
		}

		final URI[] name = { null };
		final boolean[] createNew = { false };
		ParsingResult<Object> ctor = new BasicParseRunner<Object>(
				createConstructorParser().Constructor()).run(valueStr);
		if (ctor.matched) {
			createNew[0] = true;
			IndexRange range = (IndexRange) ctor.resultValue;
			valueStr = valueStr.substring(range.start, range.end);
			// check if a name for the new resource is given
			if (ctor.valueStack.size() > 1) {
				name[0] = toURI(entityManager, ctor.valueStack.peek(1));
			}
		}

		boolean forced = false;
		// allow to force the use of a resource by appending "!" even if it
		// does not exist
		if (valueStr.endsWith("!") && valueStr.length() > 1) {
			forced = true;
			valueStr = valueStr.substring(0, valueStr.length() - 1);
		}

		// allow to specify resources as full IRIs in the form
		// <http://example.org#resource> or as prefixed names
		// example:resource
		final URI uri = toURI(entityManager, valueStr);
		if (uri != null && (forced || entityManager.hasMatch(uri, null, null))) {
			return new SimpleCommand() {
				@Override
				protected CommandResult doExecuteWithResult(
						IProgressMonitor progressMonitor, IAdaptable info)
						throws ExecutionException {
					if (createNew[0] && name[0] != null) {
						// create a new named resource
						return CommandResult.newOKCommandResult(entityManager
								.createNamed(name[0], uri));
					} else if (createNew[0]) {
						// create a new blank node resource
						return CommandResult.newOKCommandResult(entityManager
								.create(uri));
					} else {
						return CommandResult.newOKCommandResult(entityManager
								.find(uri));
					}
				}
			};
		}

		// try a full-text search to find the resource
		IStatement stmt = getStatement(element);
		Options options;
		Object oldValue = null;
		IReference property = null;
		if (stmt != null) {
			options = Options.create((IEntity) stmt.getSubject(),
					(String) editorValue, 1);
			property = stmt.getPredicate();
			oldValue = property == null ? null : stmt.getObject();
		} else {
			options = Options.create(entityManager, null, (String) editorValue,
					1);
		}
		Iterator<Match> matches = new ResourceFinder().findAnyResources(
				options.forPredicate(createNew[0] ? null : property)).iterator();
		if (matches.hasNext()) {
			final IEntity resource = matches.next().resource;
			if (createNew[0] && getLabel(resource).equals(valueStr)) {
				// create a new object
				return new SimpleCommand() {
					@Override
					protected CommandResult doExecuteWithResult(
							IProgressMonitor progressMonitor, IAdaptable info)
							throws ExecutionException {
						return CommandResult.newOKCommandResult(entityManager
								.createNamed(name[0], resource));
					}
				};
			} else if (!resource.equals(oldValue)
					&& getLabel(resource).equals(valueStr)) {
				// replace value with existing object
				return new IdentityCommand(resource);
			}
		}
		return null;
	}
}
