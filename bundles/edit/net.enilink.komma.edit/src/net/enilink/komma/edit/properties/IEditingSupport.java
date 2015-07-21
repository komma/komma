package net.enilink.komma.edit.properties;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.core.IEntityManager;

/**
 * Interface that encapsulates the editing actions for RDF values.
 */
public interface IEditingSupport {
	/**
	 * Returns <code>true</code> if the given element (resource or statement) is
	 * editable, else <code>false</code>.
	 * 
	 * @param element
	 *            A resource or a statement.
	 * 
	 * @return <code>true</code> if editable, else <code>false</code>.
	 */
	boolean canEdit(Object element);

	/**
	 * Returns an instance of {@link IProposalSupport} if this editing support
	 * is able to provide proposals for a content assistant.
	 * 
	 * @param stmt
	 *            The edited element or <code>null</code>.
	 * 
	 * @return Instance of {@link IProposalSupport} or <code>null</code>.
	 */
	IProposalSupport getProposalSupport(Object element);

	/**
	 * Returns a value that can be displayed and modified by a corresponding
	 * graphical control (e.g. a text field).
	 * 
	 * @param value
	 *            The value that should be edited. (may be null)
	 * 
	 * @return Initial value for the graphical control.
	 */
	Object getEditorValue(Object value);

	/**
	 * Returns an {@link ICommand} that transforms the <code>editorValue</code>
	 * into an RDF representation.
	 * 
	 * @param editorValue
	 *            The new value.
	 * @param entityManager
	 *            The target entity manager.
	 * @param element
	 *            The current value or <code>null</code>.
	 * 
	 * @return An {@link ICommand} that creates a corresponding RDF
	 *         representation for the given <code>editorValue</code>.
	 */
	ICommand convertEditorValue(Object editorValue,
			IEntityManager entityManager, Object element);
}
