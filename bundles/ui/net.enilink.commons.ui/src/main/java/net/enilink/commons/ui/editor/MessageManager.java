/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.ui.editor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.IMessagePrefixProvider;
import org.eclipse.ui.forms.widgets.Hyperlink;

/**
 * @see IMessageManager
 */

public class MessageManager implements IMessageManager {
	private static final DefaultPrefixProvider DEFAULT_PREFIX_PROVIDER = new DefaultPrefixProvider();
	private ArrayList<Message> messages = new ArrayList<Message>();
	private Map<Control, ControlDecorator> decorators = new HashMap<Control, ControlDecorator>();
	private boolean autoUpdate = true;
	private IMessageContainer messageContainer;
	private IMessagePrefixProvider prefixProvider = DEFAULT_PREFIX_PROVIDER;
	private int decorationPosition = SWT.LEFT | SWT.BOTTOM;
	private static FieldDecoration standardError = FieldDecorationRegistry
			.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
	private static FieldDecoration standardWarning = FieldDecorationRegistry
			.getDefault().getFieldDecoration(
					FieldDecorationRegistry.DEC_WARNING);

	private static final String[] SINGLE_MESSAGE_SUMMARY_KEYS = {
			Messages.MessageManager_sMessageSummary,
			Messages.MessageManager_sMessageSummary,
			Messages.MessageManager_sWarningSummary,
			Messages.MessageManager_sErrorSummary };

	private static final String[] MULTIPLE_MESSAGE_SUMMARY_KEYS = {
			Messages.MessageManager_pMessageSummary,
			Messages.MessageManager_pMessageSummary,
			Messages.MessageManager_pWarningSummary,
			Messages.MessageManager_pErrorSummary };

	static class Message implements IMessage {
		Control control;
		Object data;
		Object key;
		String message;
		int type;
		String prefix;

		Message(Object key, String message, int type, Object data) {
			this.key = key;
			this.message = message;
			this.type = type;
			this.data = data;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.dialogs.IMessage#getKey()
		 */
		public Object getKey() {
			return key;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.dialogs.IMessageProvider#getMessage()
		 */
		public String getMessage() {
			return message;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.dialogs.IMessageProvider#getMessageType()
		 */
		public int getMessageType() {
			return type;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.forms.messages.IMessage#getControl()
		 */
		public Control getControl() {
			return control;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.forms.messages.IMessage#getData()
		 */
		public Object getData() {
			return data;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.forms.messages.IMessage#getPrefix()
		 */
		public String getPrefix() {
			return prefix;
		}
	}

	static class DefaultPrefixProvider implements IMessagePrefixProvider {

		public String getPrefix(Control c) {
			Composite parent = c.getParent();
			Control[] siblings = parent.getChildren();
			for (int i = 0; i < siblings.length; i++) {
				if (siblings[i] == c) {
					// this is us - go backward until you hit
					// a label-like widget
					for (int j = i - 1; j >= 0; j--) {
						Control label = siblings[j];
						String ltext = null;
						if (label instanceof Label) {
							ltext = ((Label) label).getText();
						} else if (label instanceof Hyperlink) {
							ltext = ((Hyperlink) label).getText();
						} else if (label instanceof CLabel) {
							ltext = ((CLabel) label).getText();
						}
						if (ltext != null) {
							if (!ltext.endsWith(":")) //$NON-NLS-1$
								return ltext + ": "; //$NON-NLS-1$
							return ltext + " "; //$NON-NLS-1$
						}
					}
					break;
				}
			}
			return null;
		}
	}

	class ControlDecorator {
		private ControlDecoration decoration;
		private ArrayList<Message> controlMessages = new ArrayList<Message>();
		private String prefix;

		ControlDecorator(Control control) {
			this.decoration = new ControlDecoration(control,
					decorationPosition, messageContainer.getComposite());
		}

		public boolean isDisposed() {
			return decoration.getControl() == null;
		}

		void updatePrefix() {
			prefix = null;
		}

		void updatePosition() {
			Control control = decoration.getControl();
			decoration.dispose();
			this.decoration = new ControlDecoration(control,
					decorationPosition, messageContainer.getComposite());
			update();
		}

		String getPrefix() {
			if (prefix == null)
				createPrefix();
			return prefix;
		}

		private void createPrefix() {
			if (prefixProvider == null) {
				prefix = ""; //$NON-NLS-1$
				return;
			}
			prefix = prefixProvider.getPrefix(decoration.getControl());
			if (prefix == null)
				// make a prefix anyway
				prefix = ""; //$NON-NLS-1$
		}

		void addAll(ArrayList<Message> target) {
			target.addAll(controlMessages);
		}

		void addMessage(Object key, String text, Object data, int type) {
			Message message = MessageManager.this.addMessage(getPrefix(), key,
					text, data, type, controlMessages);
			message.control = decoration.getControl();
			if (isAutoUpdate())
				update();
		}

		boolean removeMessage(Object key) {
			Message message = findMessage(key, controlMessages);
			if (message != null) {
				controlMessages.remove(message);
				if (isAutoUpdate())
					update();
			}
			return message != null;
		}

		boolean removeMessages() {
			if (controlMessages.isEmpty())
				return false;
			controlMessages.clear();
			if (isAutoUpdate())
				update();
			return true;
		}

		public void update() {
			if (controlMessages.isEmpty()) {
				decoration.setDescriptionText(null);
				decoration.hide();
			} else {
				ArrayList<Message> peers = createPeers(controlMessages);
				int type = peers.get(0).getMessageType();
				String description = createDetails(createPeers(peers), true);
				if (type == IMessageProvider.ERROR)
					decoration.setImage(standardError.getImage());
				else if (type == IMessageProvider.WARNING)
					decoration.setImage(standardWarning.getImage());
				decoration.setDescriptionText(description);
				decoration.show();
			}
		}
	}

	/**
	 * Creates a new instance of the message manager that will work with the
	 * provided form.
	 * 
	 * @param scrolledForm
	 *            the form to control
	 */
	public MessageManager(IMessageContainer messageContainer) {
		this.messageContainer = messageContainer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IMessageManager#addMessage(java.lang.Object,
	 * java.lang.String, int)
	 */
	public void addMessage(Object key, String messageText, Object data, int type) {
		addMessage(null, key, messageText, data, type, messages);
		if (isAutoUpdate())
			updateForm();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IMessageManager#addMessage(java.lang.Object,
	 * java.lang.String, int, org.eclipse.swt.widgets.Control)
	 */
	public void addMessage(Object key, String messageText, Object data,
			int type, Control control) {
		ControlDecorator dec = decorators.get(control);

		if (dec == null) {
			dec = new ControlDecorator(control);
			decorators.put(control, dec);
		}
		dec.addMessage(key, messageText, data, type);
		if (isAutoUpdate()) {
			updateForm();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IMessageManager#removeMessage(java.lang.Object)
	 */
	public void removeMessage(Object key) {
		Message message = findMessage(key, messages);
		if (message != null) {
			messages.remove(message);
			if (isAutoUpdate())
				updateForm();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IMessageManager#removeMessages()
	 */
	public void removeMessages() {
		if (!messages.isEmpty()) {
			messages.clear();
			if (isAutoUpdate())
				updateForm();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IMessageManager#removeMessage(java.lang.Object,
	 * org.eclipse.swt.widgets.Control)
	 */
	public void removeMessage(Object key, Control control) {
		ControlDecorator dec = decorators.get(control);
		if (dec == null) {
			return;
		}
		if (dec.removeMessage(key))
			if (isAutoUpdate())
				updateForm();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.forms.IMessageManager#removeMessages(org.eclipse.swt.widgets
	 * .Control)
	 */
	public void removeMessages(Control control) {
		ControlDecorator dec = decorators.get(control);
		if (dec != null) {
			if (dec.removeMessages()) {
				if (isAutoUpdate())
					updateForm();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IMessageManager#removeAllMessages()
	 */
	public void removeAllMessages() {
		boolean needsUpdate = false;
		for (ControlDecorator control : decorators.values()) {
			if (control.removeMessages()) {
				needsUpdate = true;
			}
		}
		if (!messages.isEmpty()) {
			messages.clear();
			needsUpdate = true;
		}
		if (needsUpdate && isAutoUpdate())
			updateForm();
	}

	/*
	 * Adds the message if it does not already exist in the provided list.
	 */

	private Message addMessage(String prefix, Object key, String messageText,
			Object data, int type, ArrayList<Message> list) {
		Message message = findMessage(key, list);
		if (message == null) {
			message = new Message(key, messageText, type, data);
			message.prefix = prefix;
			list.add(message);
		} else {
			message.message = messageText;
			message.type = type;
			message.data = data;
		}
		return message;
	}

	/*
	 * Finds the message with the provided key in the provided list.
	 */

	private Message findMessage(Object key, ArrayList<Message> list) {
		for (int i = 0; i < list.size(); i++) {
			Message message = list.get(i);
			if (message.getKey().equals(key))
				return message;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IMessageManager#update()
	 */
	public void update() {
		// Update decorations
		for (Iterator<ControlDecorator> iter = decorators.values().iterator(); iter
				.hasNext();) {
			ControlDecorator dec = iter.next();
			dec.update();
		}
		// Update the form
		updateForm();
	}

	/*
	 * Updates the container by rolling the messages up from the controls.
	 */

	private void updateForm() {
		ArrayList<Message> mergedList = new ArrayList<Message>();
		mergedList.addAll(messages);
		for (ControlDecorator control : decorators.values()) {
			control.addAll(mergedList);
		}
		update(mergedList);
	}

	private void update(ArrayList<Message> mergedList) {
		pruneControlDecorators();
		if (mergedList.isEmpty() || mergedList == null) {
			messageContainer.setMessage(null, IMessageProvider.NONE);
			return;
		}
		ArrayList<Message> peers = createPeers(mergedList);
		int maxType = peers.get(0).getMessageType();
		String messageText;
		IMessage[] array = peers.toArray(new IMessage[peers.size()]);
		if (peers.size() == 1 && peers.get(0).prefix == null) {
			// a single message
			IMessage message = peers.get(0);
			messageText = message.getMessage();
			messageContainer.setMessage(messageText, maxType, array);
		} else {
			// show a summary message for the message
			// and list of errors for the details
			if (peers.size() > 1)
				messageText = Messages.bind(
						MULTIPLE_MESSAGE_SUMMARY_KEYS[maxType],
						new String[] { peers.size() + "" }); //$NON-NLS-1$
			else
				messageText = SINGLE_MESSAGE_SUMMARY_KEYS[maxType];

			messageContainer.setMessage(messageText, maxType, array);
		}
	}

	private static String getFullMessage(IMessage message) {
		if (message.getPrefix() == null)
			return message.getMessage();
		return message.getPrefix() + message.getMessage();
	}

	private ArrayList<Message> createPeers(ArrayList<Message> messages) {
		ArrayList<Message> peers = new ArrayList<Message>();
		int maxType = 0;
		for (int i = 0; i < messages.size(); i++) {
			Message message = messages.get(i);
			if (message.type > maxType) {
				peers.clear();
				maxType = message.type;
			}
			if (message.type == maxType)
				peers.add(message);
		}
		return peers;
	}

	private String createDetails(ArrayList<Message> messages,
			boolean excludePrefix) {
		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);

		for (int i = 0; i < messages.size(); i++) {
			if (i > 0)
				out.println();
			IMessage m = messages.get(i);
			out.print(excludePrefix ? m.getMessage() : getFullMessage(m));
		}
		out.flush();
		return sw.toString();
	}

	public static String createDetails(IMessage[] messages) {
		if (messages == null || messages.length == 0)
			return null;
		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);

		for (int i = 0; i < messages.length; i++) {
			if (i > 0)
				out.println();
			out.print(getFullMessage(messages[i]));
		}
		out.flush();
		return sw.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.forms.IMessageManager#createSummary(org.eclipse.ui.forms
	 * .IMessage[])
	 */
	public String createSummary(IMessage[] messages) {
		return createDetails(messages);
	}

	private void pruneControlDecorators() {
		for (Iterator<ControlDecorator> iter = decorators.values().iterator(); iter
				.hasNext();) {
			ControlDecorator dec = iter.next();
			if (dec.isDisposed())
				iter.remove();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IMessageManager#getMessagePrefixProvider()
	 */
	public IMessagePrefixProvider getMessagePrefixProvider() {
		return prefixProvider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.forms.IMessageManager#setMessagePrefixProvider(org.eclipse
	 * .ui.forms.IMessagePrefixProvider)
	 */
	public void setMessagePrefixProvider(IMessagePrefixProvider provider) {
		this.prefixProvider = provider;
		for (Iterator<ControlDecorator> iter = decorators.values().iterator(); iter
				.hasNext();) {
			ControlDecorator dec = iter.next();
			dec.updatePrefix();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IMessageManager#getDecorationPosition()
	 */
	public int getDecorationPosition() {
		return decorationPosition;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IMessageManager#setDecorationPosition(int)
	 */
	public void setDecorationPosition(int position) {
		this.decorationPosition = position;
		for (Iterator<ControlDecorator> iter = decorators.values().iterator(); iter
				.hasNext();) {
			ControlDecorator dec = iter.next();
			dec.updatePosition();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IMessageManager#isAutoUpdate()
	 */
	public boolean isAutoUpdate() {
		return autoUpdate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IMessageManager#setAutoUpdate(boolean)
	 */
	public void setAutoUpdate(boolean autoUpdate) {
		boolean needsUpdate = !this.autoUpdate && autoUpdate;
		this.autoUpdate = autoUpdate;
		if (needsUpdate)
			update();
	}
}