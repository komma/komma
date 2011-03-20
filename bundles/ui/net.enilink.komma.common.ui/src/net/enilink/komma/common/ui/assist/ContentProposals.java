package net.enilink.komma.common.ui.assist;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
		List<Object> args = new ArrayList<Object>(Arrays.asList(control,
				new TextContentAdapter(), contentProposalProvider));
		Constructor<ContentProposalAdapter> constructor;
		try {
			if (CommonsUi.IS_RAP_RUNNING) {
				constructor = ContentProposalAdapter.class.getConstructor(
						Control.class, IControlContentAdapter.class,
						IContentProposalProvider.class, char[].class);
			} else {
				Class<?> keyStrokeClass = ContentProposals.class
						.getClassLoader().loadClass(
								"org.eclipse.jface.bindings.keys.KeyStroke");
				constructor = ContentProposalAdapter.class.getConstructor(
						Control.class, IControlContentAdapter.class,
						IContentProposalProvider.class, keyStrokeClass,
						char[].class);

				Method getInstance = keyStrokeClass.getMethod("getInstance",
						int.class, int.class);

				args.add(getInstance.invoke(null, SWT.CTRL, ' '));
			}

			args.add(autoActivationCharacters);
			return constructor.newInstance(args.toArray());
		} catch (Exception e) {
			throw new RuntimeException(
					"Unable to initialize content proposals", e);
		}
<<<<<<< HEAD
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
=======
>>>>>>> parent of 1269e51... Adapt to new RAP API with KeyStroke class.
	}
}
