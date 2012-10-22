package net.enilink.komma.edit.properties;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.edit.assist.IContentProposalProvider;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;

/**
 * Interface that encapsulates the editing actions for property values.
 * 
 */
public interface IPropertyEditingSupport {
	/**
	 * Support interface for textual content proposals.
	 */
	interface ProposalSupport {
		/**
		 * Returns a label provider that is used to create labels and images for
		 * content proposals.
		 * 
		 * @return Label provider or <code>null</code>.
		 */
		IItemLabelProvider getLabelProvider();

		/**
		 * Returns a proposal provider that is able to create textual proposals.
		 * 
		 * @return Proposal provider or <code>null</code>.
		 */
		IContentProposalProvider getProposalProvider();

		/**
		 * Returns the auto activation characters that trigger the content
		 * assistant.
		 * 
		 * @return Array with auto activation characters or <code>null</code>.
		 */
		char[] getAutoActivationCharacters();
	}

	/**
	 * Returns an instance of {@link ProposalSupport} if this editing support is
	 * able to provide proposals for a content assistant.
	 * 
	 * @param subject
	 *            The statement's subject.
	 * @param property
	 *            The statement's predicate.
	 * @param value
	 *            The statement's object. (may be null)
	 * 
	 * @return Instance of {@link ProposalSupport} or <code>null</code>.
	 */
	ProposalSupport getProposalSupport(IEntity subject, IReference property,
			Object value);

	/**
	 * Returns <code>true</code> if the given statement is editable, else
	 * <code>false</code>.
	 * 
	 * @param subject
	 *            The statement's subject.
	 * @param property
	 *            The statement's predicate.
	 * @param value
	 *            The statement's object. (may be null)
	 * 
	 * @return <code>true</code> if editable, else <code>false</code>.
	 */
	boolean canEdit(IEntity subject, IReference property, Object value);

	/**
	 * Returns a value that can be displayed and modified by a corresponding
	 * graphical control (e.g. a text field).
	 * 
	 * @param subject
	 *            The statement's subject.
	 * @param property
	 *            The statement's predicate.
	 * @param value
	 *            The statement's object. (may be null)
	 * 
	 * @return Initial value for the graphical control.
	 */
	Object getValueForEditor(IEntity subject, IReference property, Object value);

	/**
	 * Returns an {@link ICommand} that transforms the <code>editorValue</code>
	 * into an RDF representation.
	 * 
	 * @param editorValue
	 *            The new value.
	 * @param subject
	 *            The statement's subject.
	 * @param property
	 *            The statement's predicate.
	 * @param oldValue
	 *            The statement's current object. (may be null)
	 * 
	 * @return An {@link ICommand} that creates a corresponding RDF
	 *         representation for the given <code>editorValue</code>.
	 */
	ICommand convertValueFromEditor(Object editorValue, IEntity subject,
			IReference property, Object oldValue);
}