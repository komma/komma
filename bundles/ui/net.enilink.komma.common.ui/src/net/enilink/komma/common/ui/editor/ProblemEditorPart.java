/**
 * <copyright>
 *
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: ProblemEditorPart.java,v 1.6 2007/01/26 06:08:16 marcelop Exp $
 */
package net.enilink.komma.common.ui.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import net.enilink.commons.ui.dialogs.DialogHelper;
import net.enilink.komma.common.ui.CommonUIPlugin;
import net.enilink.komma.common.ui.DiagnosticComposite;
import net.enilink.komma.common.ui.MarkerHelper;
import net.enilink.komma.common.ui.dialogs.DiagnosticDialog;
import net.enilink.komma.common.util.BasicDiagnostic;
import net.enilink.komma.common.util.Diagnostic;

/**
 * @since 2.2.0
 */
public class ProblemEditorPart extends EditorPart {
	/**
	 * Since 2.3.0
	 */
	public static class TextProvider extends DiagnosticComposite.TextProvider {
		/**
		 * Returns the message to be displayed next to the icon, at the top of
		 * the editor.
		 * 
		 * @return a not null String
		 */
		public String getMessage(Diagnostic rootDiagnostic) {
			return rootDiagnostic.getSeverity() == Diagnostic.OK ? CommonUIPlugin
					.getPlugin().getString("_UI_NoProblems_message")
					: rootDiagnostic.getMessage() != null ? rootDiagnostic
							.getMessage() : CommonUIPlugin.getPlugin()
							.getString("_UI_DefaultProblem_message");
		}
	}

	protected Diagnostic diagnostic;

	protected String editorToOpen;

	protected Label imageLabel;
	protected Text messageText;
	protected Button detailsButton;
	protected Composite detailsComposite;
	protected DiagnosticComposite diagnosticComposite;

	protected TextProvider textProvider = new TextProvider();
	protected MarkerHelper markerUtil;

	@Override
	public void dispose() {
		diagnostic = null;
		imageLabel = null;
		messageText = null;
		detailsButton = null;
		detailsComposite = null;
		diagnosticComposite = null;
		textProvider = null;
		markerUtil = null;

		super.dispose();
	}

	public void setMarkerHelper(MarkerHelper markerHelper) {
		this.markerUtil = markerHelper;
	}

	public MarkerHelper getMarkerHelper() {
		return markerUtil;
	}

	public void setTextProvider(TextProvider textProvider) {
		this.textProvider = textProvider;
		if (diagnosticComposite != null) {
			diagnosticComposite.setTextProvider(textProvider);
		}
	}

	public TextProvider getTextProvider() {
		return textProvider;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(CommonUIPlugin.getPlugin().getString("_UI_Problems_label"));
	}

	@Override
	public void createPartControl(Composite parent) {
		{
			GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			int spacing = 8;
			int margins = 8;
			layout.marginBottom = margins;
			layout.marginTop = margins;
			layout.marginLeft = margins;
			layout.marginRight = margins;
			layout.horizontalSpacing = spacing;
			layout.verticalSpacing = spacing;
			parent.setLayout(layout);
		}

		imageLabel = new Label(parent, SWT.NONE);

		messageText = new Text(parent, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP
				| SWT.NO_FOCUS);
		messageText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_BEGINNING));
		messageText.setBackground(messageText.getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_BACKGROUND));

		Composite buttonComposite = new Composite(parent, SWT.NONE);
		buttonComposite.setLayoutData(new GridData(
				GridData.VERTICAL_ALIGN_BEGINNING
						| GridData.HORIZONTAL_ALIGN_END));
		buttonComposite.setLayout(new GridLayout());
		{
			GridLayout layout = new GridLayout();
			int spacing = 3;
			layout.marginTop = -5;
			layout.marginRight = -5;
			layout.horizontalSpacing = spacing;
			layout.verticalSpacing = spacing;
			buttonComposite.setLayout(layout);
		}

		editorToOpen = computeEditorToOpen();
		if (editorToOpen != null) {
			Button openButton = new Button(buttonComposite, SWT.PUSH);
			openButton.setLayoutData(new GridData(
					GridData.HORIZONTAL_ALIGN_BEGINNING
							| GridData.FILL_HORIZONTAL));
			openButton.setText(CommonUIPlugin.getPlugin().getString(
					"_UI_ErrorEditor_OpenEditor_label"));
			openButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					openEditor();
				}
			});
		}

		if (markerUtil != null) {
			Button createMarkersButton = new Button(buttonComposite, SWT.PUSH);
			createMarkersButton.setLayoutData(new GridData(
					GridData.HORIZONTAL_ALIGN_BEGINNING
							| GridData.FILL_HORIZONTAL));
			createMarkersButton.setText(CommonUIPlugin.getPlugin().getString(
					"_UI_ErrorEditor_CreateMarkers_label"));
			createMarkersButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					createMarkers();
				}
			});
		}

		detailsButton = new Button(buttonComposite, SWT.PUSH);
		detailsButton
				.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING
						| GridData.FILL_HORIZONTAL));
		detailsButton.setData(Boolean.FALSE);
		detailsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				toggleDetails();
			}
		});
		updateDetails();

		detailsComposite = new Composite(parent, SWT.NONE);
		GridData data = new GridData(GridData.FILL_BOTH
				| GridData.GRAB_VERTICAL);
		data.horizontalSpan = 3;
		data.verticalSpan = 2;
		detailsComposite.setLayoutData(data);
		{
			GridLayout layout = new GridLayout();
			int margin = -5;
			int spacing = 3;
			layout.marginTop = margin;
			layout.marginLeft = margin;
			layout.marginRight = margin;
			layout.marginBottom = margin;
			layout.horizontalSpacing = spacing;
			layout.verticalSpacing = spacing;
			detailsComposite.setLayout(layout);
		}

		refresh();
		parent.layout(true);
	}

	public Diagnostic getDiagnostic() {
		return diagnostic;
	}

	public void setDiagnostic(Diagnostic diagnostic) {
		this.diagnostic = diagnostic;
		refresh();
	}

	protected void refresh() {
		if (diagnostic != null && messageText != null) {
			Image image = getImage();
			if (image != null) {
				// TODO find equivalent for RAP
				// image.setBackground(imageLabel.getBackground());
				imageLabel.setImage(image);
				imageLabel.setLayoutData(new GridData(
						GridData.HORIZONTAL_ALIGN_CENTER
								| GridData.VERTICAL_ALIGN_BEGINNING));
			}

			messageText.setText(getMessage());

			if (diagnosticComposite != null
					&& diagnosticComposite.getDiagnostic() != diagnostic) {
				diagnosticComposite.setDiagnostic(diagnostic);
			}
		}
	}

	protected Image getImage() {
		Display display = Display.getCurrent();
		switch (diagnostic.getSeverity()) {
		case Diagnostic.ERROR:
			return display.getSystemImage(SWT.ICON_ERROR);
		case Diagnostic.WARNING:
			return display.getSystemImage(SWT.ICON_WARNING);
		default:
			return display.getSystemImage(SWT.ICON_INFORMATION);
		}
	}

	protected String getMessage() {
		return getTextProvider().getMessage(getDiagnostic());
	}

	protected void updateDetails() {
		if (detailsButton.getData() == Boolean.TRUE) {
			if (diagnosticComposite == null) {
				diagnosticComposite = new DiagnosticComposite(detailsComposite,
						SWT.NONE);
				diagnosticComposite
						.setSeverityMask(DiagnosticComposite.ERROR_WARNING_MASK);
				diagnosticComposite.setLayoutData(new GridData(
						GridData.FILL_BOTH | GridData.GRAB_VERTICAL));
				if (getTextProvider() != null) {
					diagnosticComposite.setTextProvider(getTextProvider());
				}
				diagnosticComposite.initialize(getDiagnostic());
				detailsComposite.layout(true);
			} else {
				diagnosticComposite.setVisible(true);
			}
			detailsButton.setText(DialogHelper.get().getButtonLabel(
					IDialogConstants.DETAILS_ID, DialogHelper.HIDE));
		} else {
			if (diagnosticComposite != null) {
				diagnosticComposite.setVisible(false);
			}
			detailsButton.setText(DialogHelper.get().getButtonLabel(
					IDialogConstants.DETAILS_ID, DialogHelper.HIDE));
		}
	}

	protected void toggleDetails() {
		detailsButton
				.setData(detailsButton.getData() == Boolean.TRUE ? Boolean.FALSE
						: Boolean.TRUE);
		updateDetails();
	}

	protected String computeEditorToOpen() {
		IEditorDescriptor editorDescriptor = PlatformUI.getWorkbench()
				.getEditorRegistry().getDefaultEditor("foo.txt");
		return editorDescriptor != null ? editorDescriptor.getId() : null;
	}

	protected void openEditor() {
		if (editorToOpen != null) {
			try {
				IWorkbenchPage workbenchPage = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage();
				IEditorInput editorInput = getEditorInput();
				if (markerUtil != null && diagnosticComposite != null
						&& diagnosticComposite.isVisible()) {
					Diagnostic diagnostic = diagnosticComposite.getSelection();
					IEditorInput diagnosticEditorInput = markerUtil
							.getEditorInput(diagnostic);
					if (diagnosticEditorInput != null) {
						editorInput = diagnosticEditorInput;
					}
				}
				workbenchPage.openEditor(editorInput, editorToOpen, true,
						IWorkbenchPage.MATCH_INPUT | IWorkbenchPage.MATCH_ID);
			} catch (Exception exception) {
				openErrorDialog(CommonUIPlugin.getPlugin().getString(
						"_UI_OpenEditorError_message"), exception);
			}
		}
	}

	protected void createMarkers() {
		if (markerUtil != null) {
			markerUtil.deleteMarkers(diagnostic);
			if (diagnostic.getSeverity() != Diagnostic.OK) {
				try {
					markerUtil.createMarkers(diagnostic);
				} catch (CoreException exception) {
					openErrorDialog(CommonUIPlugin.getPlugin().getString(
							"_UI_CreateMarkerError_message"), exception);
				}
			}
		}
	}

	protected void openErrorDialog(String message, Exception exception) {
		DiagnosticDialog.open(Display.getCurrent().getActiveShell(),
				CommonUIPlugin.getPlugin().getString("_UI_Error_label"),
				message, BasicDiagnostic.toDiagnostic(exception));
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// Empty block
	}

	@Override
	public void doSaveAs() {
		// Empty block
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void setFocus() {
		// Empty block
	}
}
