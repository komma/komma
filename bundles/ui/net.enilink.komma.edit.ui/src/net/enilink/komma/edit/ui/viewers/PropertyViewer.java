package net.enilink.komma.edit.ui.viewers;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.enilink.commons.ui.editor.EditorWidgetFactory;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.ui.celleditor.TextCellEditorWithContentProposal;
import net.enilink.komma.edit.IEditImages;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.properties.IPropertyEditingSupport;
import net.enilink.komma.edit.properties.IResourceProposal;
import net.enilink.komma.edit.properties.PropertyEditingHelper;
import net.enilink.komma.edit.ui.assist.JFaceContentProposal;
import net.enilink.komma.edit.ui.celleditor.CellEditorHelper;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.provider.reflective.StatementPatternContentProvider;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IStatementPattern;
import net.enilink.komma.core.Statement;

public class PropertyViewer extends ContentViewer {
	private static final int PROPOSAL_DELAY = 1000;

	private Composite composite;
	private Label iconLabel;
	private Text valueText;
	private TextCellEditorWithContentProposal valueEditor;

	@Inject(optional = true)
	protected Provider<IEditingDomain> editingDomainProvider;

	@Inject(optional = true)
	protected Provider<IAdapterFactory> adapterFactoryProvider;

	IPropertyEditingSupport propertyEditingSupport;

	protected Object currentElement;

	private final PropertyEditingHelper helper = new PropertyEditingHelper(
			false) {
		public IPropertyEditingSupport getPropertyEditingSupport(Object element) {
			return propertyEditingSupport != null ? propertyEditingSupport
					: super.getPropertyEditingSupport(element);
		}

		@Override
		protected IStatement getStatement(Object element) {
			if (element instanceof IStatement) {
				return (IStatement) element;
			}
			if (element instanceof IStatementPattern) {
				return fillPattern((IStatementPattern) element, null);
			}
			return null;
		}

		protected IAdapterFactory getAdapterFactory() {
			return adapterFactoryProvider.get();
		}

		@Override
		protected IEditingDomain getEditingDomain() {
			return editingDomainProvider.get();
		}
	};

	public PropertyViewer(Composite parent, int textStyle) {
		this(parent, textStyle, null);
	}

	public PropertyViewer(Composite parent, int textStyle,
			EditorWidgetFactory widgetFactory) {
		composite = widgetFactory != null ? widgetFactory
				.createComposite(parent) : new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);

		iconLabel = widgetFactory != null ? widgetFactory.createLabel(
				composite, "") : new Label(composite, SWT.NONE);
		defaultIcon(iconLabel);
		if (editingDomainProvider != null) {
			valueText = widgetFactory != null ? widgetFactory.createText(
					composite, "") : new Text(composite, textStyle);
			valueText.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
					false));
		} else {
			int style = textStyle;
			if (widgetFactory != null) {
				style |= widgetFactory.getBorderStyle();
			}
			valueEditor = createCellEditor(composite, style);
			valueEditor.getControl().setLayoutData(
					new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			if (widgetFactory != null) {
				widgetFactory.adapt(valueEditor.getControl(), false, false);
			}
		}
		hookControl(composite);
		setContentProvider(new StatementPatternContentProvider());
	}

	protected TextCellEditorWithContentProposal createCellEditor(
			Composite parent, final int textStyle) {
		final IResourceProposal[] acceptedResourceProposal = { null };
		final TextCellEditorWithContentProposal textCellEditor = new TextCellEditorWithContentProposal(
				parent, textStyle, null, null) {
			@Override
			public void deactivate() {
			}

			protected void focusLost() {
			}
		};
		textCellEditor.getContentProposalAdapter().setAutoActivationDelay(
				PROPOSAL_DELAY);
		textCellEditor.getContentProposalAdapter().addContentProposalListener(
				new IContentProposalListener() {
					@Override
					public void proposalAccepted(IContentProposal proposal) {
						Object delegate = proposal instanceof JFaceContentProposal ? ((JFaceContentProposal) proposal)
								.getDelegate() : proposal;
						if (delegate instanceof IResourceProposal
								&& ((IResourceProposal) delegate)
										.getUseAsValue()) {
							acceptedResourceProposal[0] = (IResourceProposal) delegate;
						}
					}
				});
		textCellEditor.addListener(new ICellEditorListener() {
			@Override
			public void editorValueChanged(boolean oldValidState,
					boolean newValidState) {
				// user modifications reset the last value proposal
				acceptedResourceProposal[0] = null;
			}

			@Override
			public void cancelEditor() {
			}

			@Override
			public void applyEditorValue() {
				helper.setValue(
						currentElement,
						acceptedResourceProposal[0] != null ? acceptedResourceProposal[0]
								.getResource() : textCellEditor.getValue());
			}
		});
		return textCellEditor;
	}

	@Override
	public Control getControl() {
		return composite;
	}

	@Override
	public ISelection getSelection() {
		// not supported
		return null;
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		// not supported
	}

	public void setPropertyEditingSupport(
			IPropertyEditingSupport propertyEditingSupport) {
		this.propertyEditingSupport = propertyEditingSupport;
	}

	@Override
	protected void inputChanged(Object input, Object oldInput) {
		if (oldInput == null) {
			if (input == null) {
				return;
			}
			refresh();
			return;
		}
		refresh();
	}

	@Override
	protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
		if (event != null) {
			refresh(event.getElements());
		}
	}

	protected Object unwrap(Object element) {
		if (element instanceof IStatement) {
			return ((IStatement) element).getObject();
		}
		return element;
	}

	protected IStatement fillPattern(IStatementPattern pattern, Object value) {
		IReference s = pattern.getSubject() != null ? pattern.getSubject()
				: (value instanceof IReference ? (IReference) value : null);
		IReference p = pattern.getPredicate() != null ? pattern.getPredicate()
				: (value instanceof IReference ? (IReference) value : null);
		Object o = pattern.getObject() != null ? pattern.getObject() : value;
		return new Statement(s, p, o);
	}

	@Override
	public void refresh() {
		Object input = this.getInput();
		if (input instanceof IStatementPattern
				&& getContentProvider() instanceof IStructuredContentProvider) {
			// use content provider to get first element
			Object[] elements = ((IStructuredContentProvider) getContentProvider())
					.getElements(input);
			if (elements.length > 0) {
				input = fillPattern((IStatementPattern) input, elements[0]);
			}
		}
		currentElement = input;
		if (currentElement instanceof IStatement) {
			ILabelProvider labelProvider = (ILabelProvider) getLabelProvider();
			if (valueEditor != null) {
				valueEditor.setValue(helper.getValue(currentElement));
			} else {
				valueText
						.setText(labelProvider.getText(unwrap(currentElement)));
			}
			iconLabel.setImage(labelProvider.getImage(unwrap(currentElement)));
		} else {
			defaultIcon(iconLabel);
			if (valueEditor != null) {
				valueEditor.setValue("");
			} else {
				valueText.setText(null);
			}
		}
		if (valueEditor != null) {
			CellEditorHelper.updateProposals(valueEditor,
					helper.getProposalSupport(currentElement));
		}
	}

	protected void defaultIcon(Label label) {
		label.setImage(ExtendedImageRegistry.getInstance().getImage(
				KommaEditPlugin.INSTANCE.getImage(IEditImages.ITEM)));
	}

	/**
	 * Refreshes the presentation if currently chosen element is on the list.
	 * 
	 * @param objs
	 *            list of changed object
	 */
	private void refresh(Object[] objs) {
		if (objs == null || getInput() == null) {
			return;
		}
		Object input = getInput();
		for (int i = 0; i < objs.length; i++) {
			if (objs[i].equals(input)) {
				refresh();
				break;
			}
		}
	}
}
