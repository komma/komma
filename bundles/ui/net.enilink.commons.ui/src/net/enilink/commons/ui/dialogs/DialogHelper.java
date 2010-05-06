package net.enilink.commons.ui.dialogs;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.jface.dialogs.IDialogConstants;

import net.enilink.commons.ui.CommonsUi;

public class DialogHelper {
	public static int NONE = 0;
	public static int SHOW = 1 << 0;
	public static int HIDE = 1 << 1;

	private IDialogConstants dialogConstants;
	{
		if (CommonsUi.IS_RAP_RUNNING) {
			try {
				Method get = IDialogConstants.class.getMethod("get");
				dialogConstants = (IDialogConstants) get.invoke(null);
			} catch (Exception e) {
				// ignore
			}
		}
	}

	private DialogHelper() {
	}

	public String getButtonLabel(int buttonId) {
		return getButtonLabel(buttonId, NONE);
	}

	public String getButtonLabel(int buttonId, int flags) {
		String fieldName = null;
		switch (buttonId) {
		/**
		 * The label for OK buttons.
		 */
		case IDialogConstants.OK_ID:
			fieldName = "OK_LABEL";
			break;

		/**
		 * The label for cancel buttons.
		 */
		case IDialogConstants.CANCEL_ID:
			fieldName = "CANCEL_LABEL";
			break;

		/**
		 * The label for yes buttons.
		 */
		case IDialogConstants.YES_ID:
			fieldName = "YES_LABEL";
			break;

		/**
		 * The label for no buttons.
		 */
		case IDialogConstants.NO_ID:
			fieldName = "NO_LABEL";
			break;

		/**
		 * The label for not to all buttons.
		 */
		case IDialogConstants.NO_TO_ALL_ID:
			fieldName = "NO_TO_ALL_LABEL";
			break;

		/**
		 * The label for yes to all buttons.
		 */
		case IDialogConstants.YES_TO_ALL_ID:
			fieldName = "YES_TO_ALL_LABEL";
			break;

		/**
		 * The label for skip buttons.
		 */
		case IDialogConstants.SKIP_ID:
			fieldName = "SKIP_LABEL";
			break;

		/**
		 * The label for stop buttons.
		 */
		case IDialogConstants.STOP_ID:
			fieldName = "STOP_LABEL";
			break;

		/**
		 * The label for abort buttons.
		 */
		case IDialogConstants.ABORT_ID:
			fieldName = "ABORT_LABEL";
			break;

		/**
		 * The label for retry buttons.
		 */
		case IDialogConstants.RETRY_ID:
			fieldName = "RETRY_LABEL";
			break;

		/**
		 * The label for ignore buttons.
		 */
		case IDialogConstants.IGNORE_ID:
			fieldName = "IGNORE_LABEL";
			break;

		/**
		 * The label for proceed buttons.
		 */
		case IDialogConstants.PROCEED_ID:
			fieldName = "PROCEED_LABEL";
			break;

		/**
		 * The label for open buttons.
		 */
		case IDialogConstants.OPEN_ID:
			fieldName = "OPEN_LABEL";
			break;

		/**
		 * The label for close buttons.
		 */
		case IDialogConstants.CLOSE_ID:
			fieldName = "CLOSE_LABEL";
			break;

		/**
		 * The label for back buttons.
		 */
		case IDialogConstants.BACK_ID:
			fieldName = "BACK_LABEL";
			break;

		/**
		 * The label for next buttons.
		 */
		case IDialogConstants.NEXT_ID:
			fieldName = "NEXT_LABEL";
			break;

		/**
		 * The label for finish buttons.
		 */
		case IDialogConstants.FINISH_ID:
			fieldName = "FINISH_LABEL";
			break;

		/**
		 * The label for help buttons.
		 */
		case IDialogConstants.HELP_ID:
			fieldName = "HELP_LABEL";
			break;

		/**
		 * The labels for show/hide details buttons.
		 */
		case IDialogConstants.DETAILS_ID:
			if ((flags & SHOW) != 0) {
				fieldName = "SHOW_DETAILS_LABEL";
			} else if ((flags & HIDE) != 0) {
				fieldName = "HIDE_DETAILS_LABEL";
			} else {
				throw new IllegalArgumentException("Either SHOW or HIDE has to be specified.");
			}
			break;
		}

		if (fieldName == null) {
			throw new IllegalArgumentException("Unsupported button id: "
					+ buttonId);
		}
		try {
			Field field = IDialogConstants.class.getField(fieldName);
			return (String) field.get(dialogConstants);
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot access field: "
					+ fieldName, e);
		}
	}

	public static DialogHelper get() {
		return new DialogHelper();
	}
}
