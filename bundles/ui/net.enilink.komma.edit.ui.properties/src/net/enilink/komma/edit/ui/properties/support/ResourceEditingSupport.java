package net.enilink.komma.edit.ui.properties.support;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.common.ui.assist.ContentProposalExt;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.util.ISparqlConstants;

public class ResourceEditingSupport implements IPropertyEditingSupport {
	class ResourceProposal extends ContentProposalExt {
		IEntity resource;

		public ResourceProposal(String content, int cursorPosition,
				IEntity resource) {
			super(content, Type.REPLACE, getText(resource), getText(resource),
					cursorPosition);
			this.resource = resource;
		}

		public IEntity getResource() {
			return resource;
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
			List<IContentProposal> proposals = new ArrayList<IContentProposal>();
			for (IEntity resource : getResourceProposals(subject, predicate,
					contents.substring(0, position), 20)) {
				String content = getText(resource);
				if (content.length() > 0) {
					proposals.add(new ResourceProposal(content, content
							.length(), resource));
				}
			}
			return proposals.toArray(new IContentProposal[proposals.size()]);
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

	protected String getText(Object value) {
		if (value == null) {
			return null;
		}
		IItemLabelProvider labelProvider = (IItemLabelProvider) adapterFactory
				.adapt(value, IItemLabelProvider.class);
		if (labelProvider != null) {
			return labelProvider.getText(value);
		}
		return value.toString();
	}

	protected IExtendedIterator<IResource> getResourceProposals(
			IEntity subject, IReference predicate, String template, int limit) {
		template = template.trim();

		String uriPattern = template;
		if (!template.matches("[#/]")) {
			uriPattern = "#" + template;

			int colonIndex = template.lastIndexOf(':');
			if (colonIndex == 0) {
				uriPattern = "#" + template.substring(1);
			} else if (colonIndex > 0) {
				String prefix = template.substring(0, colonIndex);
				URI namespaceUri = subject.getEntityManager().getNamespace(
						prefix);
				if (namespaceUri != null) {
					uriPattern = namespaceUri.appendFragment(
							template.substring(colonIndex + 1)).toString();
				}
			}
		}

		StringBuilder sparql = new StringBuilder(ISparqlConstants.PREFIX
				+ "SELECT DISTINCT ?s WHERE {");

		if (editPredicate) {
			sparql.append("?s a rdf:Property . ");
		} else {
			// TODO compute correct intersection of ranges
			if (predicate != null) {
				sparql.append("{?property rdfs:range ?sType FILTER (?sType != owl:Thing)}");
				sparql.append(" UNION {?subject a [rdfs:subClassOf ?r] . ?r owl:onProperty ?property {?r owl:allValuesFrom ?sType} UNION {?r owl:someValuesFrom ?sType}}");
				sparql.append(" ?s a ?sType . ");
			}
		}

		sparql.append("{?s ?p ?o . FILTER regex(str(?s), ?uriPattern)}");
		sparql.append(" UNION ");
		sparql.append("{?s rdfs:label ?l . FILTER regex(str(?l), ?template)}");
		sparql.append("} LIMIT " + limit);

		// TODO incorporate correct ranges
		IQuery<?> query = subject.getEntityManager()
				.createQuery(sparql.toString()) //
				.setParameter("uriPattern", uriPattern) //
				.setParameter("template", "^" + template);
		if (!editPredicate) {
			query.setParameter("subject", subject);
			query.setParameter("property", predicate);
		}

		return query.evaluate(IResource.class);
	}

	@Override
	public ProposalSupport getProposalSupport(final IEntity subject,
			final IReference property, Object value) {
		return new ProposalSupport() {
			@Override
			public IContentProposalProvider getProposalProvider() {
				return new ResourceProposalProvider(subject, property);
			}

			@Override
			public IItemLabelProvider getLabelProvider() {
				return new IItemLabelProvider() {
					@Override
					public String getText(Object object) {
						IItemLabelProvider labelProvider = (IItemLabelProvider) adapterFactory
								.adapt(((ResourceProposal) object).resource,
										IItemLabelProvider.class);
						if (labelProvider != null) {
							return labelProvider
									.getText(((ResourceProposal) object).resource);
						}
						return ModelUtil
								.getLabel(((ResourceProposal) object).resource);
					}

					@Override
					public Object getImage(Object object) {
						IItemLabelProvider labelProvider = (IItemLabelProvider) adapterFactory
								.adapt(((ResourceProposal) object).resource,
										IItemLabelProvider.class);
						if (labelProvider != null) {
							return labelProvider
									.getImage(((ResourceProposal) object).resource);
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
	public boolean canEdit(IEntity subject, IReference property, Object value) {
		return true;
	}

	@Override
	public Object getValueForEditor(IEntity subject, IReference property,
			Object value) {
		if (editPredicate) {
			value = property;
		}
		String text = value != null ? getText(value) : null;
		return text != null ? text : "";
	}

	@Override
	public ICommand convertValueFromEditor(Object editorValue, IEntity subject,
			IReference property, Object oldValue) {
		IExtendedIterator<IResource> resources = getResourceProposals(subject,
				property, (String) editorValue, 1);
		if (resources.hasNext()) {
			IEntity resource = resources.next();
			if (!resource.equals(oldValue)
					&& getText(resource).equals(((String) editorValue).trim())) {
				return new IdentityCommand(resource);
			}
		}
		return null;
	}
}
