package net.enilink.komma.edit.ui.assist;

import org.eclipse.jface.fieldassist.IContentProposal;

import net.enilink.komma.common.ui.assist.IContentProposalExt;

public class JFaceContentProposal implements IContentProposal,
		IContentProposalExt {
	private net.enilink.komma.edit.assist.IContentProposal delegate;

	public JFaceContentProposal(
			net.enilink.komma.edit.assist.IContentProposal delegate) {
		this.delegate = delegate;
	}

	public String getContent() {
		return delegate.getContent();
	}

	public int getCursorPosition() {
		return delegate.getCursorPosition();
	}

	public net.enilink.komma.edit.assist.IContentProposal getDelegate() {
		return delegate;
	}

	public String getDescription() {
		return delegate.getDescription();
	}

	public String getLabel() {
		return delegate.getLabel();
	}

	@Override
	public Type getType() {
		return delegate.isInsert() ? Type.INSERT : Type.REPLACE;
	}
}
