package net.enilink.komma.edit.ui.assist;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

public class JFaceProposalProvider implements IContentProposalProvider {
	net.enilink.komma.edit.assist.IContentProposalProvider proposalProvider;

	public JFaceProposalProvider(
			net.enilink.komma.edit.assist.IContentProposalProvider proposalProvider) {
		this.proposalProvider = proposalProvider;
	}

	public static IContentProposalProvider wrap(
			net.enilink.komma.edit.assist.IContentProposalProvider proposalProvider) {
		return proposalProvider == null ? null : new JFaceProposalProvider(
				proposalProvider);
	}

	@Override
	public IContentProposal[] getProposals(String contents, int position) {
		net.enilink.komma.edit.assist.IContentProposal[] proposals = proposalProvider
				.getProposals(contents, position);
		if (proposals == null) {
			return null;
		}
		IContentProposal[] results = new IContentProposal[proposals.length];
		for (int i = 0; i < proposals.length; i++) {
			results[i] = new JFaceContentProposal(proposals[i]);
		}
		return results;
	}
}