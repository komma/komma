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
 * $Id: ResourceDialog.java,v 1.5 2007/05/24 21:22:27 marcelop Exp $
 */
package net.enilink.komma.common.ui.rcp.dialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.common.ui.CommonUIPlugin;
import net.enilink.komma.core.URIImpl;

/**
 * Instances of this class allow a user to specify one or more URIs identifying
 * resources. The dialog includes buttons that allow the file system or
 * workspace to be browsed, so that the URI can be automatically filled based on
 * the selected file.
 * <p>
 * Subclasses are encouraged to override {@link #processResources}, where they
 * can process the specified URIs.
 * 
 * @since 2.2.0
 */
public class ResourceDialog extends Dialog {
	protected static final int CONTROL_OFFSET = 10;
	protected String title;
	protected int style;
	protected Text uriField;
	protected String uriText;

	/**
	 * Creates a new instance of this class, given a parent shell, an optional
	 * title, and a style value describing its behaviour.
	 * 
	 * @param parent
	 *            a shell which will be the parent of the new instance
	 * @param title
	 *            an optional title for the dialog, to be used in place of the
	 *            default
	 * @param style
	 *            {@link SWT SWT style bits}, indicating whether
	 *            {@link SWT#OPEN existing (<code>SWT.OPEN</code>)} or
	 *            {@link SWT#SAVE new <code>(SWT.SAVE)</code>} resources are to
	 *            be specified and, in the former case, whether
	 *            {@link SWT#SINGLE single (<code>SWT.SINGLE</code>)} or
	 *            {@link SWT#MULTI multiple (<code>SWT.MULTI</code>)}. Open
	 *            existing and single resource are the defaults.
	 */
	public ResourceDialog(Shell parent, String title, int style) {
		super(parent);
		this.title = title != null ? title : CommonUIPlugin.INSTANCE
				.getString("_UI_ResourceDialog_title");
		this.style = style;

		normalizeStyle();
		setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
	}

	protected void normalizeStyle() {
		// Ensure there are no extraneous style bits.
		//
		if ((style & ~(SWT.MULTI | SWT.SINGLE | SWT.OPEN | SWT.SAVE)) != 0) {
			throw new IllegalArgumentException(
					"extraneous style bits specified (only SWT.MULTI, SWT.SINGLE, SWT.OPEN, SWT.SAVE allowed");
		}

		// Assign default style bits, if necessary, and ensure none conflict.
		//
		if ((style & SWT.MULTI) == 0 && (style & SWT.SINGLE) == 0) {
			style |= SWT.SINGLE;
		} else if ((style & SWT.MULTI) != 0 && (style & SWT.SINGLE) != 0) {
			throw new IllegalArgumentException(
					"conflicting style bits specified (sWT.MUTLI and SWT.SINGLE)");
		}

		if ((style & SWT.OPEN) == 0 && (style & SWT.SAVE) == 0) {
			style |= SWT.OPEN;
		} else if ((style & SWT.OPEN) != 0 && (style & SWT.SAVE) != 0) {
			throw new IllegalArgumentException(
					"conflicting style bits specified (sWT.OPEN and SWT.SAVE)");
		}

		if (isMulti() && isSave()) {
			throw new IllegalArgumentException(
					"conflicting style bits specified (sWT.MULTI and SWT.SAVE)");
		}
	}

	protected boolean isSave() {
		return (style & SWT.SAVE) != 0;
	}

	protected boolean isMulti() {
		return (style & SWT.MULTI) != 0;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(title);
	}

	/**
	 * Creates and returns the contents of the upper part of this dialog. This
	 * implementation creates a labeled text field for the URI(s) and buttons
	 * for browsing the file system and workspace. These buttons are configured
	 * (selection listeners are added) by calling
	 * {@link #prepareBrowseFileSystemButton} and
	 * {@link #prepareBrowseWorkspaceButton}, respectively.
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		{
			FormLayout layout = new FormLayout();
			composite.setLayout(layout);

			GridData data = new GridData();
			data.verticalAlignment = GridData.FILL;
			data.grabExcessVerticalSpace = true;
			data.horizontalAlignment = GridData.FILL;
			data.grabExcessHorizontalSpace = true;
			if (!AbstractKommaPlugin.IS_RESOURCES_BUNDLE_AVAILABLE) {
				data.widthHint = 330;
			}
			composite.setLayoutData(data);
		}

		// buttonComposite has to be the first child of composite because
		// subclasses are expecting this.
		Composite buttonComposite = new Composite(composite, SWT.NONE);

		Label resourceURILabel = new Label(composite, SWT.LEFT);
		{
			resourceURILabel.setText(CommonUIPlugin.INSTANCE
					.getString(isMulti() ? "_UI_ResourceURIs_label"
							: "_UI_ResourceURI_label"));
			FormData data = new FormData();
			data.left = new FormAttachment(0, CONTROL_OFFSET);
			data.top = new FormAttachment(0, CONTROL_OFFSET);
			resourceURILabel.setLayoutData(data);
		}

		{
			FormData data = new FormData();
			data.top = new FormAttachment(resourceURILabel, CONTROL_OFFSET,
					SWT.CENTER);
			data.left = new FormAttachment(resourceURILabel, CONTROL_OFFSET);
			data.right = new FormAttachment(100, -CONTROL_OFFSET);
			buttonComposite.setLayoutData(data);

			buttonComposite.setLayout(new FormLayout());
		}

		uriField = new Text(composite, SWT.BORDER);
		{
			FormData data = new FormData();
			data.top = new FormAttachment(buttonComposite, CONTROL_OFFSET);
			data.left = new FormAttachment(0, CONTROL_OFFSET);
			data.right = new FormAttachment(100, -CONTROL_OFFSET);
			uriField.setLayoutData(data);
		}

		Button browseFileSystemButton = new Button(buttonComposite, SWT.PUSH);
		browseFileSystemButton.setText(CommonUIPlugin.INSTANCE
				.getString("_UI_BrowseFileSystem_label"));
		prepareBrowseFileSystemButton(browseFileSystemButton);

		if (AbstractKommaPlugin.IS_RESOURCES_BUNDLE_AVAILABLE) {
			Button browseWorkspaceButton = new Button(buttonComposite, SWT.PUSH);
			{
				FormData data = new FormData();
				data.right = new FormAttachment(100);
				browseWorkspaceButton.setLayoutData(data);
			}
			{
				FormData data = new FormData();
				data.right = new FormAttachment(browseWorkspaceButton,
						-CONTROL_OFFSET);
				browseFileSystemButton.setLayoutData(data);
			}
			browseWorkspaceButton.setText(CommonUIPlugin.INSTANCE
					.getString("_UI_BrowseWorkspace_label"));
			prepareBrowseWorkspaceButton(browseWorkspaceButton);
		} else {
			FormData data = new FormData();
			data.right = new FormAttachment(100);
			browseFileSystemButton.setLayoutData(data);
		}

		Label separatorLabel = new Label(composite, SWT.SEPARATOR
				| SWT.HORIZONTAL);
		{
			FormData data = new FormData();
			data.top = new FormAttachment(uriField,
					(int) (1.5 * CONTROL_OFFSET));
			data.left = new FormAttachment(0, -CONTROL_OFFSET);
			data.right = new FormAttachment(100, CONTROL_OFFSET);
			separatorLabel.setLayoutData(data);
		}

		composite.setTabList(new Control[] { uriField, buttonComposite });
		return composite;
	}

	/**
	 * Called to prepare the Browse File System button, this implementation adds
	 * a selection listener that creates an appropriate {@link FileDialog}.
	 */
	protected void prepareBrowseFileSystemButton(Button browseFileSystemButton) {
		browseFileSystemButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				FileDialog fileDialog = new FileDialog(getShell(), style);
				fileDialog.open();

				String filterPath = fileDialog.getFilterPath();
				if (isMulti()) {
					String[] fileNames = fileDialog.getFileNames();
					StringBuffer uris = new StringBuffer();

					for (int i = 0, len = fileNames.length; i < len; i++) {
						uris.append(URIImpl.createFileURI(
								filterPath + File.separator + fileNames[i])
								.toString());
						uris.append("  ");
					}
					uriField.setText((uriField.getText() + "  " + uris
							.toString()).trim());
				} else {
					String fileName = fileDialog.getFileName();
					if (fileName != null) {
						uriField.setText(URIImpl.createFileURI(
								filterPath + File.separator + fileName)
								.toString());
					}
				}
			}
		});
	}

	/**
	 * Called to prepare the Browse Workspace button, this implementation adds a
	 * selection listener that creates an appropriate
	 * {@link WorkspaceResourceDialog}.
	 */
	protected void prepareBrowseWorkspaceButton(Button browseWorkspaceButton) {
		browseWorkspaceButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (isMulti()) {
					StringBuffer uris = new StringBuffer();

					IFile[] files = WorkspaceResourceDialog.openFileSelection(
							getShell(), null, null, true, null, null);
					for (int i = 0, len = files.length; i < len; i++) {
						uris.append(URIImpl.createPlatformResourceURI(files[i]
								.getFullPath().toString(), true));
						uris.append("  ");
					}
					uriField.setText((uriField.getText() + "  " + uris
							.toString()).trim());
				} else {
					IFile file = null;

					if (isSave()) {
						file = WorkspaceResourceDialog.openNewFile(getShell(),
								null, null, null, null);
					} else {
						IFile[] files = WorkspaceResourceDialog
								.openFileSelection(getShell(), null, null,
										false, null, null);
						if (files.length != 0) {
							file = files[0];
						}
					}

					if (file != null) {
						uriField
								.setText(URIImpl.createPlatformResourceURI(
										file.getFullPath().toString(), true)
										.toString());
					}
				}
			}
		});
	}

	/**
	 * Called when the OK button has been pressed, this method calls
	 * {@link #getURIText} to cache and return the URI text field's value (so
	 * that it may be retrieved even after the field has been disposed). It then
	 * calls {@link #processResources} to handle the specified URIs and
	 * optionally closes the dialog, based on its return value.
	 */
	@Override
	protected void okPressed() {
		uriText = getURIText();
		if (processResources()) {
			super.okPressed();
		} else {
			uriField.selectAll();
			uriField.setFocus();
		}
	}

	/**
	 * Returns the value of the URI text field.
	 */
	public String getURIText() {
		return uriField != null && !uriField.isDisposed() ? uriField.getText()
				: uriText;
	}

	/**
	 * Returns the list of space-separated URIs from the URI text field.
	 */
	public List<URIImpl> getURIs() {
		List<URIImpl> uris = new ArrayList<URIImpl>();
		for (StringTokenizer stringTokenizer = new StringTokenizer(getURIText()); stringTokenizer
				.hasMoreTokens();) {
			String uri = stringTokenizer.nextToken();
			uris.add(URIImpl.createURI(uri));
		}
		return uris;
	}

	/**
	 * Called by {@link #okPressed} to handle the specified URIs, this
	 * implementation simply returns true, allowing the dialog to close.
	 * Subclasses can override this method to load, save, or otherwise process
	 * resources, and based on this processing, to optionally prevent the dialog
	 * from being closed if the URIs are invalid.
	 * 
	 * @return <code>true</code> if the dialog can be closed, <code>false</code>
	 *         if URI(s) must be re-entered
	 */
	protected boolean processResources() {
		return true;
	}
}