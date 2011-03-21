package net.enilink.komma.common.ui.assist;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;

import net.enilink.commons.ui.CommonsUi;

public class ContentProposals {
	public static ContentProposalAdapter enableContentProposal(
			final Control control,
			IContentProposalProvider contentProposalProvider,
			char[] autoActivationCharacters) {
		KeyStroke triggerKeyStroke;
		if (CommonsUi.IS_RAP_RUNNING) {
			// TODO find good trigger key stroke for RAP
			triggerKeyStroke = null;
		} else {
			triggerKeyStroke = KeyStroke.getInstance(SWT.CTRL, ' ');
		}

		final IControlContentAdapter controlContentAdapter = new TextContentAdapter();
		final ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(
				control, controlContentAdapter, contentProposalProvider,
				triggerKeyStroke, autoActivationCharacters);
		proposalAdapter
				.addContentProposalListener(new IContentProposalListener() {
					public void proposalAccepted(IContentProposal proposal) {
						if (proposalAdapter.getProposalAcceptanceStyle() == ContentProposalAdapter.PROPOSAL_IGNORE) {
							if (proposal instanceof IContentProposalExt) {
								switch (((IContentProposalExt) proposal)
										.getType()) {
								case REPLACE:
									controlContentAdapter.setControlContents(
											control, proposal.getContent(),
											proposal.getCursorPosition());
									return;
								}
							}
							// default is insert
							controlContentAdapter.insertControlContents(
									control, proposal.getContent(),
									proposal.getCursorPosition());
						}
					}
				});
		return proposalAdapter;
	}
}
