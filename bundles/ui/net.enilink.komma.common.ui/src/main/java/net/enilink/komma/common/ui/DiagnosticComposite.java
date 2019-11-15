/**
 * <copyright>
 *
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: DiagnosticComposite.java,v 1.4 2007/06/14 18:32:41 emerks Exp $
 */
package net.enilink.komma.common.ui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import net.enilink.komma.common.util.Diagnostic;

/**
 * @since 2.3
 */
public class DiagnosticComposite extends Composite {
	public static class TextProvider {
		/**
		 * Returns the text associated to be displayed in the detail text when a
		 * a diagnostic is selected in the tree.
		 * 
		 * @param diagnostic
		 * @return a not null String
		 */
		public String getDetail(Diagnostic diagnostic) {
			Throwable throwable = diagnostic.getException();
			if (throwable != null) {
				StringWriter in = new StringWriter();
				PrintWriter ps = new PrintWriter(in);
				throwable.printStackTrace(ps);
				return in.getBuffer().toString();
			}

			for (Object datum : diagnostic.getData()) {
				if (datum instanceof StringBuilder) {
					return datum.toString();
				}
			}

			return "";
		}
	}

	public static int ERROR_WARNING_MASK = Diagnostic.ERROR
			| Diagnostic.WARNING;

	public static boolean severityMatches(Diagnostic diagnostic, int mask) {
		return (diagnostic.getSeverity() & mask) != 0;
	}

	protected Diagnostic diagnostic;
	protected TextProvider textProvider;

	protected boolean showRootDiagnostic = false;
	protected TreeViewer diagnosticTreeViewer;
	protected Text detailText;

	protected int severityMask = 0;

	/**
	 * @param parent
	 * @param style
	 */
	public DiagnosticComposite(Composite parent, int style) {
		super(parent, style);
		GridLayout layout = new GridLayout();
		int spacing = 3;
		layout.marginTop = -5;
		layout.marginBottom = -5;
		layout.marginLeft = -5;
		layout.marginRight = -5;
		layout.horizontalSpacing = spacing;
		layout.verticalSpacing = spacing;
		setLayout(layout);
	}

	@Override
	public void dispose() {
		diagnostic = null;
		diagnosticTreeViewer = null;
		detailText = null;

		super.dispose();
	}

	public void initialize(Diagnostic diagnostic) {
		if (!isInitialized()) {
			setDiagnostic(diagnostic);
			createControls(this);
		}
	}

	public boolean isInitialized() {
		return diagnosticTreeViewer != null && detailText != null;
	}

	public void setDiagnostic(Diagnostic diagnostic) {
		this.diagnostic = diagnostic;
		if (isInitialized()) {
			detailText.setText("");
			if (getDiagnostic() != null) {
				diagnosticTreeViewer.setInput(getDiagnostic());
			} else {
				diagnosticTreeViewer.getTree().removeAll();
			}
		}
	}

	public Diagnostic getDiagnostic() {
		return diagnostic;
	}

	public void setTextProvider(TextProvider textProvider) {
		this.textProvider = textProvider;
		if (detailText != null) {
			String detail = getTextProvider().getDetail(getSelection());
			setDetailText(detail);
		}
	}

	public TextProvider getTextProvider() {
		if (textProvider == null) {
			textProvider = new TextProvider();
		}
		return textProvider;
	}

	public void setShowRootDiagnostic(boolean showRootDiagnostic) {
		this.showRootDiagnostic = showRootDiagnostic;
	}

	public boolean isShowRootDiagnostic() {
		return showRootDiagnostic;
	}

	public void setSeverityMask(int severityMask) {
		this.severityMask = severityMask;
	}

	public int getSeverityMask() {
		return severityMask;
	}

	protected void createControls(Composite parent) {
		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH
				| GridData.GRAB_VERTICAL));

		diagnosticTreeViewer = new TreeViewer(sashForm, SWT.BORDER);
		diagnosticTreeViewer.getTree().setLayoutData(
				new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL
						| GridData.VERTICAL_ALIGN_BEGINNING));

		detailText = new Text(sashForm, SWT.BORDER | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 20;
		gridData.grabExcessVerticalSpace = true;
		detailText.setLayoutData(gridData);
		detailText.setBackground(detailText.getDisplay().getSystemColor(
				SWT.COLOR_LIST_BACKGROUND));

		sashForm.setWeights(new int[] { 70, 30 });
		sashForm.setMaximizedControl(diagnosticTreeViewer.getTree());

		diagnosticTreeViewer.setContentProvider(createContentProvider());
		diagnosticTreeViewer.setLabelProvider(createLabelProvider());

		diagnosticTreeViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						if (!event.getSelection().isEmpty()) {
							Diagnostic diagnostic = (Diagnostic) ((IStructuredSelection) event
									.getSelection()).getFirstElement();
							diagnosticSelected(diagnostic);
						} else {
							detailText.setText("");
						}
					}
				});

		if (getDiagnostic() != null) {
			diagnosticTreeViewer.setInput(getDiagnostic());
		}
		diagnosticTreeViewer.expandToLevel(2);
	}

	public void setDetailText(String text) {
		if (text == null) {
			text = "";
		}

		if (detailText != null && !text.equals(detailText.getText())) {
			detailText.setText(text);
		}
	}

	public String getDetailText() {
		return detailText == null ? "" : detailText.getText();
	}

	public Diagnostic getSelection() {
		return diagnosticTreeViewer == null ? null
				: (Diagnostic) ((IStructuredSelection) diagnosticTreeViewer
						.getSelection()).getFirstElement();
	}

	protected void diagnosticSelected(Diagnostic selection) {
		String detail = getTextProvider().getDetail(selection).trim();

		SashForm sashForm = (SashForm) detailText.getParent();
		setDetailText(detail);
		if (detail.length() == 0) {
			sashForm.setMaximizedControl(diagnosticTreeViewer.getTree());
		} else {
			sashForm.setMaximizedControl(null);
			if (diagnosticTreeViewer != null) {
				diagnosticTreeViewer.getTree().showSelection();
			}
		}
	}

	protected ITreeContentProvider createContentProvider() {
		return new ITreeContentProvider() {
			private boolean isRootElement = isShowRootDiagnostic();
			private Map<Diagnostic, Diagnostic[]> parentToChildrenMap = new HashMap<Diagnostic, Diagnostic[]>();

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				parentToChildrenMap.clear();
			}

			public void dispose() {
				parentToChildrenMap.clear();
			}

			public Object getParent(Object element) {
				return null;
			}

			public Object[] getElements(Object inputElement) {
				if (isRootElement) {
					isRootElement = false;
					Diagnostic diagnostic = (Diagnostic) inputElement;
					if (severityMatches(diagnostic, severityMask)) {
						if (diagnostic.getMessage() != null
								|| diagnostic.getException() != null) {
							return new Object[] { diagnostic };
						}
					} else {
						return new Object[0];
					}
				}
				return getChildren(inputElement);
			}

			public boolean hasChildren(Object element) {
				return getChildren(element).length > 0;
			}

			public Object[] getChildren(Object parentElement) {
				Diagnostic[] children = parentToChildrenMap.get(parentElement);
				if (children == null) {
					Diagnostic diagnostic = (Diagnostic) parentElement;
					List<Diagnostic> childList = new ArrayList<Diagnostic>(
							diagnostic.getChildren().size());
					for (Diagnostic child : diagnostic.getChildren()) {
						if (severityMatches(child, severityMask)) {
							childList.add(child);
						}
					}
					children = childList.toArray(new Diagnostic[childList
							.size()]);
					parentToChildrenMap.put(diagnostic, children);
				}
				return children;
			}
		};
	}

	protected ILabelProvider createLabelProvider() {
		return new LabelProvider() {
			@Override
			public String getText(Object element) {
				Diagnostic diagnostic = (Diagnostic) element;
				String message = diagnostic.getMessage();
				if (message == null) {
					switch (diagnostic.getSeverity()) {
					case Diagnostic.ERROR:
						message = CommonUIPlugin.getPlugin().getString(
								"_UI_DiagnosticError_label");
						break;
					case Diagnostic.WARNING:
						message = CommonUIPlugin.getPlugin().getString(
								"_UI_DiagnosticWarning_label");
						break;
					default:
						message = CommonUIPlugin.getPlugin().getString(
								"_UI_Diagnostic_label");
						break;
					}
				}
				return message;
			}

			@Override
			public Image getImage(Object element) {
				Diagnostic diagnostic = (Diagnostic) element;
				ISharedImages sharedImages = PlatformUI.getWorkbench()
						.getSharedImages();
				switch (diagnostic.getSeverity()) {
				case Diagnostic.ERROR:
					return sharedImages
							.getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
				case Diagnostic.CANCEL:
				case Diagnostic.WARNING:
					return sharedImages
							.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
				case Diagnostic.OK:
				case Diagnostic.INFO:
					return sharedImages
							.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
				}
				return null;
			}
		};
	}
}
