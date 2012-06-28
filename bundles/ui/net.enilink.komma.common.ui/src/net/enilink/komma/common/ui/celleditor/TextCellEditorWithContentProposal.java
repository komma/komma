package net.enilink.komma.common.ui.celleditor;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.enilink.komma.common.ui.assist.ContentProposals;

public class TextCellEditorWithContentProposal extends TextCellEditor {
	private ContentProposalAdapter contentProposalAdapter;
	private boolean popupOpen = false; // true, iff popup is currently open

	public TextCellEditorWithContentProposal(Composite parent) {
		this(parent, SWT.SINGLE, null, null);
	}

	public TextCellEditorWithContentProposal(Composite parent, int style,
			IContentProposalProvider contentProposalProvider,
			char[] autoActivationCharacters) {
		super(parent, style);

		if (contentProposalProvider == null) {
			contentProposalProvider = ContentProposals.NULL_PROPOSAL_PROVIDER;
		}

		contentProposalAdapter = ContentProposals.enableContentProposal(text,
				contentProposalProvider, autoActivationCharacters);

		// Listen for popup open/close events to be able to handle focus events
		// correctly
		contentProposalAdapter
				.addContentProposalListener(new IContentProposalListener2() {
					public void proposalPopupClosed(
							ContentProposalAdapter adapter) {
						popupOpen = false;
					}

					public void proposalPopupOpened(
							ContentProposalAdapter adapter) {
						popupOpen = true;
					}
				});
	}

	@Override
	protected Control createControl(Composite parent) {
		Control text = super.createControl(parent);
		if ((getStyle() & SWT.MULTI) != 0) {
			text.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					if (e.character == '\r' && e.stateMask == 0) {
						e.doit = false;
						fireApplyEditorValue();
						deactivate();
					}
				}
			});
		}
		return text;
	}

	/**
	 * Return the {@link ContentProposalAdapter} of this cell editor.
	 * 
	 * @return the {@link ContentProposalAdapter}
	 */
	public ContentProposalAdapter getContentProposalAdapter() {
		return contentProposalAdapter;
	}

	@Override
	protected void focusLost() {
		if (!popupOpen) {
			// Focus lost deactivates the cell editor.
			// This must not happen if focus lost was caused by activating
			// the completion proposal popup.
			super.focusLost();
		}
	}

	@Override
	protected boolean dependsOnExternalFocusListener() {
		// Always return false;
		// Otherwise, the ColumnViewerEditor will install an additional focus
		// listener
		// that cancels cell editing on focus lost, even if focus gets lost due
		// to
		// activation of the completion proposal popup. See also bug 58777.
		return false;
	}
}
