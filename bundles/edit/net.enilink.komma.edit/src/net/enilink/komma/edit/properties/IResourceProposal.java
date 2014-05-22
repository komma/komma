package net.enilink.komma.edit.properties;

import net.enilink.komma.edit.assist.IContentProposal;
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

	/**
	 * Returns the score of the corresponding resource for the related proposal
	 * query (e.g. the resource matches some property range).
	 * 
	 * A score &gt; 1000 can be considered as a perfect match.
	 * 
	 * @return The match score for the corresponding resource.
	 */
	int getScore();
}