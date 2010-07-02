package net.enilink.komma.edit.ui.properties.internal.parts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Item;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.ui.celleditor.TextCellEditorWithContentProposal;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.ui.properties.internal.wizards.PropertyUtil;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.URI;
import net.enilink.komma.util.ISparqlConstants;

class ValueEditingSupport extends EditingSupport {
	class ContentProposal implements IContentProposal {
		String content;
		int cursorPosition;
		String description;
		String label;

		IEntity resource;

		public ContentProposal(String content, String description,
				String label, int cursorPosition) {
			this.content = content;
			this.description = description;
			this.label = label;
			this.cursorPosition = cursorPosition;
		}

		public ContentProposal(String content, int cursorPosition,
				IEntity resource) {
			this(content, labelProvider.getText(resource), labelProvider
					.getText(resource), cursorPosition);
			this.resource = resource;
		}

		@Override
		public String getContent() {
			return content;
		}

		@Override
		public int getCursorPosition() {
			return cursorPosition;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public String getLabel() {
			return label;
		}

		public IEntity getResource() {
			return resource;
		}
	}

	class ProposalProvider implements IContentProposalProvider {
		@Override
		public IContentProposal[] getProposals(String contents, int position) {
			List<IContentProposal> proposals = new ArrayList<IContentProposal>();
			for (IEntity resource : getResourceProposals(contents.substring(0,
					position), 20)) {
				String label = getText(resource);
				String content = "";
				if (position < label.length()) {
					content = label.substring(position);
				}
				if (content.length() > 0) {
					proposals.add(new ContentProposal(content,
							content.length(), resource));
				}
			}
			return proposals.toArray(new IContentProposal[proposals.size()]);
		}
	}

	private Object currentElement;
	private IEditingDomain editingDomain;
	private ILabelProvider labelProvider;

	private TextCellEditor literalEditor;

	private TextCellEditorWithContentProposal resourceEditor;

	public ValueEditingSupport(TreeViewer viewer) {
		super(viewer);

		literalEditor = new TextCellEditor(viewer.getTree());
		resourceEditor = new TextCellEditorWithContentProposal(
				viewer.getTree(), new ProposalProvider(), null);
		resourceEditor.getContentProposalAdapter().setLabelProvider(
				new LabelProvider() {
					@Override
					public Image getImage(Object object) {
						return labelProvider == null ? null : labelProvider
								.getImage(((ContentProposal) object)
										.getResource());
					}

					@Override
					public String getText(Object object) {
						return ValueEditingSupport.this
								.getText(((ContentProposal) object)
										.getResource());
					}
				});
	}

	@Override
	protected boolean canEdit(Object element) {
		if (element instanceof PropertyNode
				&& ((TreeViewer) getViewer()).getExpandedState(element)) {
			return false;
		}
		IStatement stmt = getStatement(element);
		return !stmt.isInferred();
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		currentElement = element;
		if (getStatement(element).getObject() instanceof IReference) {
			return resourceEditor;
		} else {
			return literalEditor;
		}
	}

	IExtendedIterator<IResource> getResourceProposals(String template, int limit) {
		template = template.trim();

		IStatement stmt = getStatement(currentElement);

		String uriPattern = template;
		if (!template.matches("[#/]")) {
			uriPattern = "#" + template;

			int colonIndex = template.lastIndexOf(':');
			if (colonIndex == 0) {
				uriPattern = "#" + template.substring(1);
			} else if (colonIndex > 0) {
				String prefix = template.substring(0, colonIndex);
				URI namespaceUri = ((IEntity) stmt.getSubject())
						.getKommaManager().getNamespace(prefix);
				if (namespaceUri != null) {
					uriPattern = namespaceUri.appendFragment(
							template.substring(colonIndex + 1)).toString();
				}
			}
		}

		// TODO incorporate correct ranges
		return ((IEntity) stmt.getSubject())
				.getKommaManager()
				.createQuery(
						ISparqlConstants.PREFIX
								+ "SELECT DISTINCT ?s WHERE {{?s ?p ?o . FILTER regex(str(?s), ?uriPattern)}"
								+ " UNION "
								+ "{?s rdfs:label ?l . FILTER regex(str(?l), ?template)}"
								+ "} LIMIT " + limit) //
				.setParameter("uriPattern", uriPattern) //
				.setParameter("template", "^" + template) // 
				.evaluate(IResource.class);
	}

	IStatement getStatement(Object element) {
		if (element instanceof Item) {
			element = ((Item) element).getData();
		}
		if (element instanceof PropertyNode) {
			element = ((PropertyNode) element).getFirstStatement();
		}
		return (IStatement) element;
	}

	@Override
	protected Object getValue(Object element) {
		IStatement stmt = getStatement(element);

		Object object = stmt.getObject();
		if (object instanceof ILiteral) {
			ILiteral literal = (ILiteral) object;
			return literal.getLabel();
		}

		return getText(object);
	}

	protected String getText(Object element) {
		return labelProvider == null ? ModelUtil.getLabel(element)
				: labelProvider.getText(element);
	}

	@Override
	protected void setValue(Object element, Object value) {
		if (value == null) {
			return;
		}

		IStatement stmt = getStatement(element);

		IResource subject = (IResource) stmt.getSubject();
		Object object = stmt.getObject();
		Object newObject = null;
		;
		if (value instanceof String) {
			if (object instanceof IReference) {
				IExtendedIterator<IResource> resources = getResourceProposals(
						(String) value, 1);
				if (resources.hasNext()) {
					IEntity resource = resources.next();
					if (!object.equals(resource)
							&& getText(resource)
									.equals(((String) value).trim())) {
						newObject = resource;
					}
				}
			} else {
				URI literalType = null;
				String literalLanguage = null;
				if (object instanceof ILiteral) {
					literalType = ((ILiteral) object).getDatatype();
					literalLanguage = ((ILiteral) object).getLanguage();
				}
				newObject = subject.getKommaManager().createLiteral(
						(String) value, literalType, literalLanguage);
				if (newObject.equals(object)) {
					newObject = null;
				}
			}

			if (newObject != null) {
				PropertyUtil.removeProperty(editingDomain, (IResource) stmt
						.getSubject(), (IProperty) stmt.getPredicate(), stmt
						.getObject());
				PropertyUtil.addProperty(editingDomain, subject,
						(IProperty) stmt.getPredicate(), newObject);
			}
		}

		// -- is handled by listener --
		// getViewer().update(element, null);
	}

	public void setEditingDomain(IEditingDomain editingDomain) {
		this.editingDomain = editingDomain;
	}

	public void setLabelProvider(ILabelProvider labelProvider) {
		this.labelProvider = labelProvider;
	}
}
