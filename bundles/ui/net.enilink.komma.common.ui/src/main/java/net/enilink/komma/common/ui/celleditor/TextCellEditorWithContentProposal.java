package net.enilink.komma.common.ui.celleditor;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import net.enilink.commons.ui.CommonsUi;
import net.enilink.komma.common.ui.assist.ContentProposals;

public class TextCellEditorWithContentProposal extends TextCellEditor {
	private IContentProposalProvider contentProposalProvider;
	private char[] autoActivationCharacters;

	private ContentProposalAdapter contentProposalAdapter;
	private boolean popupOpen = false; // true, iff popup is currently open
	private boolean proposalAccepted = false;

	public TextCellEditorWithContentProposal(Composite parent) {
		this(parent, SWT.SINGLE, null, null);
	}

	public TextCellEditorWithContentProposal(Composite parent, int style,
			IContentProposalProvider contentProposalProvider,
			char[] autoActivationCharacters) {
		super();
		setStyle(style);
		this.contentProposalProvider = contentProposalProvider;
		this.autoActivationCharacters = autoActivationCharacters;
		create(parent);
	}

	@Override
	protected Control createControl(Composite parent) {
		Control text = super.createControl(parent);
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
						proposalAccepted = false;
						popupOpen = true;
					}
				});
		contentProposalAdapter
				.addContentProposalListener(new IContentProposalListener() {
					public void proposalAccepted(IContentProposal proposal) {
						proposalAccepted = true;
					}
				});
		if ((getStyle() & SWT.MULTI) != 0 || CommonsUi.IS_RAP_RUNNING) {
			text.addListener(SWT.Traverse, new Listener() {
				public void handleEvent(Event e) {
					switch (e.character) {
					case SWT.LF:
					case SWT.CR:
						if (e.stateMask == 0
								&& !(popupOpen || proposalAccepted)) {
							// apply editor value if enter is pressed
							e.doit = false;
							fireApplyEditorValue();
							deactivate();
						}
					}
					proposalAccepted = false;
				}
			});
		}
		return text;
	}

	@Override
	protected void keyReleaseOccured(KeyEvent keyEvent) {
		proposalAccepted = false;
		if (CommonsUi.IS_RAP_RUNNING && keyEvent.character == '\r') {
			// correctly handle CTRL+Enter in RAP
			if (text != null && !text.isDisposed()
					&& (getStyle() & SWT.MULTI) != 0
					&& (keyEvent.stateMask & SWT.CTRL) != 0) {
				super.keyReleaseOccured(keyEvent);
			}
			return;
		}
		super.keyReleaseOccured(keyEvent);
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
