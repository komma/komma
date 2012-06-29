package net.enilink.komma.common.ui.assist;

public interface IContentProposalExt {
	enum Type {
		INSERT, REPLACE
	};

	Type getType();

	/**
	 * Returns the replacement content in case this proposal is of type REPLACE.
	 */
	String getReplacement();
}