package net.enilink.komma.edit.ui.celleditor;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import net.enilink.komma.common.ui.assist.ContentProposals;
import net.enilink.komma.common.ui.celleditor.TextCellEditorWithContentProposal;
import net.enilink.komma.edit.properties.IPropertyEditingSupport;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.edit.ui.assist.JFaceContentProposal;
import net.enilink.komma.edit.ui.assist.JFaceProposalProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;

/**
 * Helper class for cell editors when using {@link IPropertyEditingSupport}.
 */
public class CellEditorHelper {
	/**
	 * Update the content proposals of a text cell editor.
	 */
	public static void updateProposals(
			TextCellEditorWithContentProposal textCellEditor,
			IPropertyEditingSupport.ProposalSupport proposals) {
		ContentProposalAdapter proposalAdapter = textCellEditor
				.getContentProposalAdapter();
		if (proposals != null) {
			final IItemLabelProvider labelProvider = proposals
					.getLabelProvider();
			if (labelProvider != null) {
				proposalAdapter.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof JFaceContentProposal) {
							element = ((JFaceContentProposal) element)
									.getDelegate();
						}
						return labelProvider.getText(element);
					}

					@Override
					public Image getImage(Object element) {
						if (element instanceof JFaceContentProposal) {
							element = ((JFaceContentProposal) element)
									.getDelegate();
						}
						return ExtendedImageRegistry.getInstance().getImage(
								labelProvider.getImage(element));
					}
				});
			} else {
				proposalAdapter.setLabelProvider(null);
			}
			proposalAdapter.setContentProposalProvider(JFaceProposalProvider
					.wrap(proposals.getProposalProvider()));
			proposalAdapter.setAutoActivationCharacters(proposals
					.getAutoActivationCharacters());
			proposalAdapter.setEnabled(true);
		} else {
			proposalAdapter.setLabelProvider(null);
			proposalAdapter
					.setContentProposalProvider(ContentProposals.NULL_PROPOSAL_PROVIDER);
			proposalAdapter.setAutoActivationCharacters(null);
			proposalAdapter.setEnabled(false);
		}
	}
}
