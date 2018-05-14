package net.enilink.komma.edit.properties;

import net.enilink.komma.edit.assist.IContentProposalProvider;
import net.enilink.komma.edit.provider.IItemLabelProvider;

/**
 * Support interface for textual content proposals.
 */
public interface IProposalSupport {
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