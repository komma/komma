package net.enilink.komma.edit.ui.properties.support;

import org.eclipse.jface.fieldassist.IContentProposal;

import net.enilink.komma.core.IEntity;

/**
 * Represents a special content proposal that is associated to a resource.
 * 
 */
public interface IResourceProposal extends IContentProposal {
	/**
	 * The resource that is represented by this content proposal.
	 * 
	 * @return The resource belonging to this proposal.
	 */
	IEntity getResource();

	/**
	 * Marks if this resource should be used as the result value of an editing
	 * operation.
	 * 
	 * @return <code>true</code> if the resource should be used as result value,
	 *         else <code>false</code>.
	 */
	boolean getUseAsValue();
}