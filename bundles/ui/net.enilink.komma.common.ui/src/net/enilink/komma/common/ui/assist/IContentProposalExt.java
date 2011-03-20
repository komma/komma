package net.enilink.komma.common.ui.assist;


public interface IContentProposalExt {
	enum Type {
		INSERT, REPLACE
	};

	Type getType();
}