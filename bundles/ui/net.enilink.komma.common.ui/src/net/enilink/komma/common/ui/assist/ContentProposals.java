package net.enilink.komma.common.ui.assist;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;

import net.enilink.commons.ui.CommonsUi;

public class ContentProposals {
	public static ContentProposalAdapter enableContentProposal(Control control,
			IContentProposalProvider contentProposalProvider,
			char[] autoActivationCharacters) {
		KeyStroke triggerKeyStroke;
		if (CommonsUi.IS_RAP_RUNNING) {
			// TODO find good trigger key stroke for RAP
			triggerKeyStroke = null;
		} else {
			triggerKeyStroke = KeyStroke.getInstance(SWT.CTRL, ' ');
		}
		return new ContentProposalAdapter(control, new TextContentAdapter(),
				contentProposalProvider, triggerKeyStroke,
				autoActivationCharacters);
	}
}
