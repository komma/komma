package net.enilink.komma.common.ui.assist;

import org.eclipse.jface.fieldassist.ContentProposal;

public class ContentProposalExt extends ContentProposal implements
		IContentProposalExt {
	private Type type;

	public ContentProposalExt(String content, String label, String description,
			int cursorPosition) {
		this(content, Type.INSERT, label, description, cursorPosition);
	}

	public ContentProposalExt(String content, Type type, String label,
			String description, int cursorPosition) {
		super(content, label, description, cursorPosition);
		this.type = type;
	}

	@Override
	public String getContent() {
		return getType() == Type.INSERT ? super.getContent() : "";
	}

	public String getReplacement() {
		return getType() == Type.REPLACE ? super.getContent() : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.komma.common.ui.assist.IContentProposalExt#getType()
	 */
	public Type getType() {
		return type;
	}
}
