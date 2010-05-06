/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
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
 * $Id: PropertyDescriptor.java,v 1.18 2008/12/22 14:26:18 emerks Exp $
 */
package net.enilink.komma.edit.ui.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.openrdf.model.vocabulary.XMLSchema;

import net.enilink.vocab.owl.AnnotationProperty;
import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.OntologyProperty;
import net.enilink.komma.KommaCore;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.ui.celleditor.ExtendedComboBoxCellEditor;
import net.enilink.komma.common.ui.celleditor.ExtendedDialogCellEditor;
import net.enilink.komma.common.util.Diagnostic;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.edit.provider.IItemPropertyDescriptor;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.celleditor.PropertyEditorDialog;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.sesame.SesameReference;
import net.enilink.komma.util.KommaUtil;

/**
 * This is used to encapsulate an {@link IItemPropertyDescriptor} along with the
 * object for which it is an item property source and make it behave like an
 * {@link org.eclipse.ui.views.properties.IPropertyDescriptor}.
 */
public class PropertyDescriptor implements IPropertyDescriptor {
	/**
	 * This is the object for which this class is a property source.
	 */
	protected Object object;

	protected IAdapterFactory adapterFactory;

	/**
	 * This is the descriptor to which we will delegate all the
	 * {@link org.eclipse.ui.views.properties.IPropertyDescriptor} methods.
	 */
	protected IItemPropertyDescriptor itemPropertyDescriptor;

	/**
	 * An instance is constructed from an object and its item property source.
	 */
	public PropertyDescriptor(Object object, IAdapterFactory adapterFactory,
			IItemPropertyDescriptor itemPropertyDescriptor) {
		this.object = object;
		this.adapterFactory = adapterFactory;
		this.itemPropertyDescriptor = itemPropertyDescriptor;
	}

	public String getCategory() {
		return itemPropertyDescriptor.getCategory(object);
	}

	public String getDescription() {
		return itemPropertyDescriptor.getDescription(object);
	}

	public String getDisplayName() {
		return itemPropertyDescriptor.getDisplayName(object);
	}

	public String[] getFilterFlags() {
		return itemPropertyDescriptor.getFilterFlags(object);
	}

	public Object getHelpContextIds() {
		return itemPropertyDescriptor.getHelpContextIds(object);
	}

	public Object getId() {
		return itemPropertyDescriptor.getId(object);
	}

	public ILabelProvider getLabelProvider() {
		final IItemLabelProvider itemLabelProvider = itemPropertyDescriptor
				.getLabelProvider(object);
		return new LabelProvider() {
			@Override
			public String getText(Object object) {
				return itemLabelProvider.getText(object);
			}

			@Override
			public Image getImage(Object object) {
				return ExtendedImageRegistry.getInstance().getImage(
						itemLabelProvider.getImage(object));
			}
		};
	}

	protected ILabelProvider getEditLabelProvider() {
		return getLabelProvider();
	}

	public boolean isCompatibleWith(IPropertyDescriptor anotherProperty) {
		return false;
	}

	/**
	 * A delegate for handling validation and conversion for data type values.
	 */
	protected class DatatypeValueHandler implements ICellEditorValidator,
			IInputValidator {
		protected Collection<? extends IReference> datatypes;

		public DatatypeValueHandler(Collection<? extends IReference> datatypes) {
			this.datatypes = datatypes;
			if (this.datatypes == null || this.datatypes.isEmpty()) {
				this.datatypes = Arrays.asList(new SesameReference(
						XMLSchema.STRING));
			}
		}

		public String isValid(Object value) {
			StringBuilder messages = null;
			for (Iterator<? extends IReference> it = datatypes.iterator(); it
					.hasNext();) {
				IReference datatype = it.next();

				Object convertedValue;
				try {
					convertedValue = toValue((String) value);
				} catch (Exception exception) {
					String message = exception.getClass().getName();
					int index = message.lastIndexOf('.');
					if (index >= 0) {
						message = message.substring(index + 1);
					}
					if (exception.getLocalizedMessage() != null) {
						message = message + ": "
								+ exception.getLocalizedMessage();
					}
					if (!it.hasNext()) {
						return message;
					} else {
						if (messages == null) {
							messages = new StringBuilder(message);
						} else {
							messages.append("\n").append(message);
						}
					}
					convertedValue = null;
				}

				if (convertedValue != null) {
					Diagnostic diagnostic = KommaCore.getDefault()
							.getDefaultValidator().validate(datatype,
									convertedValue);
					if (diagnostic.getSeverity() == Diagnostic.OK) {
						return null;
					} else {
						return (diagnostic.getChildren().get(0)).getMessage()
								.replaceAll("'", "''").replaceAll("\\{", "'{'"); // }}
					}
				}
			}
			return messages != null ? messages.toString() : null;
		}

		public String isValid(String text) {
			return isValid((Object) text);
		}

		public Object toValue(String string) {
			return KommaUtil.convertToRange(((IEntity) object).getKommaManager(),
					datatypes, string);
		}

		public String toString(Object value) {
			String result = KommaUtil.getLabel(value);
			return result == null ? "" : result;
		}

	}

	public class DatatypeCellEditor extends TextCellEditor {
		protected DatatypeValueHandler valueHandler;

		public DatatypeCellEditor(Collection<? extends IReference> datatypes,
				Composite parent) {
			super(parent);
			valueHandler = new DatatypeValueHandler(datatypes);
			setValidator(valueHandler);
		}

		@Override
		public Object doGetValue() {
			return valueHandler.toValue((String) super.doGetValue());
		}

		@Override
		public void doSetValue(Object value) {
			value = valueHandler.toString(value);
			super.doSetValue(value);
		}

		// CellEditor.setValue() calls isCorrect() to validate the value that is
		// about to be set. We are doing conversion
		// between the value and a corresponding string, and we would usually
		// like to validate the string. Because
		// setValue() is final, we cannot do that conversion there, so we need
		// to record what we're trying to validate and
		// work around the problem in isCorrect().
		// 
		protected boolean validateAsValue = true;

		@Override
		protected void editOccured(ModifyEvent e) {
			validateAsValue = false;
			super.editOccured(e);
			validateAsValue = true;
		}

		@Override
		protected boolean isCorrect(Object value) {
			if (validateAsValue) {
				value = valueHandler.toString(value);
			}
			return super.isCorrect(value);
		}
	}

	private static class MultiLineInputDialog extends InputDialog {
		public MultiLineInputDialog(Shell parentShell, String title,
				String message, String initialValue, IInputValidator validator) {
			super(parentShell, title, message, initialValue, validator);
			setShellStyle(getShellStyle() | SWT.RESIZE);
		}

		@Override
		protected Text createText(Composite composite) {
			Text text = new Text(composite, SWT.MULTI | SWT.H_SCROLL
					| SWT.V_SCROLL | SWT.BORDER);
			GridData data = new GridData(GridData.FILL_HORIZONTAL
					| GridData.FILL_VERTICAL);
			data.heightHint = 5 * text.getLineHeight();
			data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
			text.setLayoutData(data);
			return text;
		}
	}

	protected CellEditor createDatatypeCellEditor(
			final Collection<? extends IReference> datatypes,
			Composite composite) {
		if (itemPropertyDescriptor.isMultiLine(object)) {
			return new ExtendedDialogCellEditor(composite,
					getEditLabelProvider()) {
				protected DatatypeValueHandler valueHandler = new DatatypeValueHandler(
						datatypes);

				@Override
				protected Object openDialogBox(Control cellEditorWindow) {
					InputDialog dialog = new MultiLineInputDialog(
							cellEditorWindow.getShell(),
							KommaEditUIPlugin.INSTANCE.getString(
									"_UI_FeatureEditorDialog_title",
									new Object[] {
											getDisplayName(),
											getEditLabelProvider().getText(
													object) }),
							KommaEditUIPlugin.INSTANCE
									.getString("_UI_MultiLineInputDialog_message"),
							valueHandler.toString(getValue()), valueHandler);
					return dialog.open() == Window.OK ? valueHandler
							.toValue(dialog.getValue()) : null;
				}
			};
		}
		return new DatatypeCellEditor(datatypes, composite);
	}

	/**
	 * This returns the cell editor that will be used to edit the value of this
	 * property. This default implementation determines the type of cell editor
	 * from the nature of the structural feature.
	 */
	public CellEditor createPropertyEditor(Composite composite) {
		if (!itemPropertyDescriptor.canSetProperty(object)) {
			return null;
		}

		CellEditor result = null;

		Object genericProperty = itemPropertyDescriptor.getProperty(object);
		if (genericProperty instanceof IReference[]) {
			result = new ExtendedComboBoxCellEditor(composite,
					new ArrayList<Object>(itemPropertyDescriptor
							.getChoiceOfValues(object)),
					getEditLabelProvider(), itemPropertyDescriptor
							.isSortChoices(object));
		} else if (!(genericProperty instanceof AnnotationProperty
				|| genericProperty instanceof DatatypeProperty || genericProperty instanceof OntologyProperty)) {
			final IProperty property = (IProperty) genericProperty;
			final Collection<?> choiceOfValues = itemPropertyDescriptor
					.getChoiceOfValues(object);
			if (choiceOfValues != null) {
				if (itemPropertyDescriptor.isMany(object)) {
					// boolean valid = true;
					// for (Object choice : choiceOfValues) {
					// if (!property.isRangeCompatible((IResource) object,
					// choice)) {
					// valid = false;
					// break;
					// }
					// }
					//
					// if (valid) {
					final ILabelProvider editLabelProvider = getEditLabelProvider();
					result = new ExtendedDialogCellEditor(composite,
							editLabelProvider) {
						@Override
						protected Object openDialogBox(Control cellEditorWindow) {
							PropertyEditorDialog dialog = new PropertyEditorDialog(
									cellEditorWindow.getShell(),
									adapterFactory, editLabelProvider,
									(IObject) object, property.getNamedRanges(
											(IObject) object, true)
											.toSet(),
									(Collection<?>) doGetValue(),
									getDisplayName(), new ArrayList<Object>(
											choiceOfValues), false,
									itemPropertyDescriptor
											.isSortChoices(object));
							dialog.open();
							return dialog.getResult();
						}
					};
					// }
				}

				if (result == null) {
					result = new ExtendedComboBoxCellEditor(composite,
							new ArrayList<Object>(choiceOfValues),
							getEditLabelProvider(), itemPropertyDescriptor
									.isSortChoices(object));
				}
			}
		} else if (genericProperty instanceof IProperty) {
			final IProperty property = (IProperty) genericProperty;
			if (itemPropertyDescriptor.isMany(object)) {
				final ILabelProvider editLabelProvider = getEditLabelProvider();
				result = new ExtendedDialogCellEditor(composite,
						editLabelProvider) {
					@Override
					protected Object openDialogBox(Control cellEditorWindow) {
						PropertyEditorDialog dialog = new PropertyEditorDialog(
								cellEditorWindow.getShell(), adapterFactory,
								editLabelProvider, (IObject) object, property
										.getRanges(true).toSet(),
								(Collection<?>) doGetValue(), getDisplayName(),
								null, itemPropertyDescriptor
										.isMultiLine(object), false);
						dialog.open();
						return dialog.getResult();
					}
				};
			} else if (property.getRdfsRanges().contains(
					new SesameReference(XMLSchema.BOOLEAN))) {
				result = new ExtendedComboBoxCellEditor(composite, Arrays
						.asList(new Object[] { Boolean.FALSE, Boolean.TRUE }),
						getEditLabelProvider(), itemPropertyDescriptor
								.isSortChoices(object));
			} else {
				result = createDatatypeCellEditor(property.getNamedRanges(
						(IObject) object, true).toSet(), composite);
			}
		}

		return result;
	}
}
