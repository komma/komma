package net.enilink.komma.edit.ui.celleditor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

import net.enilink.komma.common.ui.celleditor.TextCellEditorWithContentProposal;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.properties.IPropertyEditingSupport;
import net.enilink.komma.edit.properties.IResourceProposal;
import net.enilink.komma.edit.properties.PropertyEditingHelper;
import net.enilink.komma.edit.ui.assist.JFaceContentProposal;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.core.IStatement;

/**
 * An abstract base class for editing of cells which represent RDF statements.
 */
public abstract class PropertyCellEditingSupport extends EditingSupport {
	private static final int PROPOSAL_DELAY = 1000;

	protected boolean editPredicate;

	protected PropertyEditingHelper helper;

	private Object currentElement;

	private TextCellEditorWithContentProposal textCellEditor;

	private IResourceProposal acceptedResourceProposal;

	private ICellEditorListener cellEditorListener = new ICellEditorListener() {
		@Override
		public void editorValueChanged(boolean oldValidState,
				boolean newValidState) {
			// user modifications reset the last value proposal
			acceptedResourceProposal = null;
		}

		@Override
		public void cancelEditor() {
			applyEditorValue();
		}

		@Override
		public void applyEditorValue() {
			// ensure that initial state is restored
			editorClosed(currentElement);
		}
	};

	public PropertyCellEditingSupport(ColumnViewer viewer) {
		this(viewer, false, SWT.NONE);
	}

	public PropertyCellEditingSupport(final ColumnViewer viewer,
			boolean editPredicate, final int cellEditorStyle) {
		super(viewer);
		this.editPredicate = editPredicate;
		helper = new PropertyEditingHelper(editPredicate) {
			@Override
			protected IStatement getStatement(Object element) {
				return PropertyCellEditingSupport.this.getStatement(element);
			}

			@Override
			protected IEditingDomain getEditingDomain() {
				return PropertyCellEditingSupport.this.getEditingDomain();
			}

			@Override
			protected void setProperty(Object element, IProperty property) {
				PropertyCellEditingSupport.this.setProperty(element, property);
			}

			@Override
			protected IPropertyEditingSupport getPropertyEditingSupport(
					IStatement stmt) {
				IPropertyEditingSupport support = PropertyCellEditingSupport.this
						.getPropertyEditingSupport(stmt);
				return support != null ? support : super
						.getPropertyEditingSupport(stmt);
			}
		};

		textCellEditor = new TextCellEditorWithContentProposal(
				(Composite) viewer.getControl(), cellEditorStyle | SWT.BORDER,
				null, null) {
			@Override
			public void deactivate() {
				fireApplyEditorValue();
				super.deactivate();
			}

			protected void focusLost() {
			}

			@Override
			public LayoutData getLayoutData() {
				LayoutData layoutData = super.getLayoutData();
				if ((cellEditorStyle & SWT.MULTI) != 0) {
					layoutData.verticalAlignment = SWT.TOP;
					layoutData.minimumHeight = getItemHeight() * 6;
				}
				return layoutData;
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
							acceptedResourceProposal = (IResourceProposal) delegate;
						}
					}
				});
		textCellEditor.addListener(cellEditorListener);
	}

	protected int getItemHeight() {
		Control control = getViewer().getControl();
		if (control instanceof Tree) {
			return ((Tree) control).getItemHeight();
		}
		if (control instanceof Table) {
			return ((Table) control).getItemHeight();
		}
		return 0;
	}

	@Override
	protected boolean canEdit(Object element) {
		return helper.canEdit(element);
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		acceptedResourceProposal = null;
		currentElement = unwrap(element);
		CellEditorHelper.updateProposals(textCellEditor,
				helper.getProposalSupport(currentElement));
		return textCellEditor;
	}

	protected IPropertyEditingSupport getPropertyEditingSupport(IStatement stmt) {
		return null;
	}

	protected Object unwrap(Object itemOrData) {
		if (itemOrData instanceof Item) {
			return ((Item) itemOrData).getData();
		}
		return itemOrData;
	}

	/**
	 * Returns the editing domain for executing commands.
	 */
	abstract protected IEditingDomain getEditingDomain();

	/**
	 * Returns the statement that is represented by the current cell.
	 */
	abstract protected IStatement getStatement(Object element);

	/**
	 * Notifies that the editor for the given element was recently closed.
	 */
	protected void editorClosed(Object element) {
	}

	/**
	 * Assigns a property to an element if operating in property editing mode.
	 */
	protected void setProperty(Object element, IProperty property) {
	}

	/**
	 * Notifies about the resulting status of an editing operation and related
	 * commands.
	 */
	protected void setEditStatus(Object element, IStatus status, Object value) {
	}

	@Override
	protected Object getValue(Object element) {
		return helper.getValue(unwrap(element));
	}

	@Override
	protected void setValue(Object element, Object value) {
		if (value == null || value.equals(getValue(element))) {
			return;
		}
		if (acceptedResourceProposal != null) {
			value = acceptedResourceProposal.getResource();
		}
		IStatus status = helper.setValue(element, value).getStatus();
		setEditStatus(element, status, value);
	}
}
