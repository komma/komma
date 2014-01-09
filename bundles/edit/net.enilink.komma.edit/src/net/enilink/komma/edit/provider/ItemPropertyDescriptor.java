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
 * $Id: ItemPropertyDescriptor.java,v 1.32 2008/08/13 15:11:42 emerks Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;

import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.ExtendedCompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.common.util.Log;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.command.SetCommand;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.provider.IItemPropertyDescriptor.OverrideableCommandOwner;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.em.util.KommaUtil;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;

/**
 * This implementation of an item property descriptor supports delegating of the
 * {@link IItemPropertySource} interface to the {@link IItemPropertyDescriptor}
 * interface.
 */
public class ItemPropertyDescriptor implements IItemPropertyDescriptor,
		OverrideableCommandOwner {
	/**
	 * Returns the feature's default {@link #getId(Object) identifier}.
	 * 
	 * @param eStructuralFeature
	 *            the feature to lookup.
	 * @return the feature's default identifier.
	 */
	public static String getDefaultId(IReference property) {
		return property.getURI().toString();
	}

	public static final Object BOOLEAN_VALUE_IMAGE = KommaEditPlugin.INSTANCE
			.getImage("full/obj16/BooleanValue");
	public static final Object GENERIC_VALUE_IMAGE = KommaEditPlugin.INSTANCE
			.getImage("full/obj16/GenericValue");
	public static final Object INTEGRAL_VALUE_IMAGE = KommaEditPlugin.INSTANCE
			.getImage("full/obj16/IntegralValue");
	public static final Object REAL_VALUE_IMAGE = KommaEditPlugin.INSTANCE
			.getImage("full/obj16/RealValue");
	public static final Object TEXT_VALUE_IMAGE = KommaEditPlugin.INSTANCE
			.getImage("full/obj16/TextValue");

	/**
	 * For now we need to keep track of the adapter factory, because we need it
	 * to provide a correct label provider.
	 */
	protected IAdapterFactory adapterFactory;

	/**
	 * This is used to locate resources for translated values like enumeration
	 * literals.
	 */
	protected IResourceLocator resourceLocator;

	/**
	 * This is a convenient wrapper of the {@link #adapterFactory}.
	 */
	protected AdapterFactoryItemDelegator itemDelegator;

	/**
	 * This is returned by {@link #canSetProperty}.
	 */
	protected boolean isSettable;

	/**
	 * This is the name that is displayed in the property sheet.
	 */
	protected String displayName;

	/**
	 * This is the description shown in the property sheet when this property is
	 * selected.
	 */
	protected String description;

	/**
	 * This is the structural feature that provides the values for this
	 * property. This is mutually exclusive with {@link #parentReferences}.
	 */
	protected IReference property;

	/**
	 * This is the set of single-valued references that act as a parent, only
	 * one can have a non null value at a time. This is mutually exclusive with
	 * {@link #property}.
	 */
	protected IReference[] parentReferences;

	/**
	 * Whether the value of this property consists of multi-line text.
	 */
	protected boolean multiLine;

	/**
	 * Whether the choices for this property should be sorted for display.
	 */
	protected boolean sortChoices;

	/**
	 * This represents the group of properties into which this one should be
	 * placed.
	 */
	protected String category;

	/**
	 * These are the flags used as filters in the property sheet.
	 */
	protected String[] filterFlags;

	/**
	 * This is the label provider used to render property values.
	 */
	// protected Object labelProvider;
	/**
	 * This is the image that will be used with the value no matter what type of
	 * object it is.
	 */
	protected Object staticImage;

	/**
	 * If non-null, this object will be the owner of commands created to set the
	 * property's value.
	 */
	protected Object commandOwner;

	/**
	 * This class uses a static image
	 */
	protected class ItemDelegator extends AdapterFactoryItemDelegator {
		protected IResourceLocator resourceLocator;

		public ItemDelegator(IAdapterFactory adapterFactory) {
			super(adapterFactory);
		}

		public ItemDelegator(IAdapterFactory adapterFactory,
				IResourceLocator resourceLocator) {
			super(adapterFactory);
			this.resourceLocator = resourceLocator;
		}

		@Override
		public String getText(Object object) {
			if (property instanceof DatatypeProperty) {
				if (isMany(object)) {
					if (object instanceof Collection<?>) {
						StringBuffer result = new StringBuffer();
						for (Iterator<?> i = ((Collection<?>) object)
								.iterator(); i.hasNext();) {
							Object value = i.next();
							result.append(ModelUtil.getLabel(value));
							if (i.hasNext()) {
								result.append(", ");
							}
						}
						return result.toString();
					}
				} else {
					return ModelUtil.getLabel(object);
				}
			}

			return super.getText(object);
		}

		// This is copied from ItemProviderAdapterFactory.
		//
		protected String crop(String text) {
			if (text != null) {
				char[] chars = text.toCharArray();
				for (int i = 0; i < chars.length; i++) {
					if (Character.isISOControl(chars[i])) {
						return text.substring(0, i) + "...";
					}
				}
			}
			return text;
		}

		@Override
		public Object getImage(Object object) {
			return staticImage == null ? super.getImage(object) : staticImage;
		}
	}

	/**
	 * This creates an instance that uses a resource locator and determines the
	 * cell editor from the type of the structural feature.
	 */
	public ItemPropertyDescriptor(IAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, String displayName,
			String description, IReference property, boolean isSettable) {
		this(adapterFactory, resourceLocator, displayName, description,
				property, isSettable, false, false, null, null, null);
	}

	/**
	 * This creates an instance that uses a resource locator, specifies a static
	 * image, and determines the cell editor from the type of the structural
	 * feature.
	 */
	public ItemPropertyDescriptor(IAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, String displayName,
			String description, IReference property, boolean isSettable,
			Object staticImage) {
		this(adapterFactory, resourceLocator, displayName, description,
				property, isSettable, false, false, staticImage, null, null);
	}

	/**
	 * This creates an instance that uses a resource locator, specifies a
	 * category and filter flags, and determines the cell editor from the type
	 * of the structural feature.
	 */
	public ItemPropertyDescriptor(IAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, String displayName,
			String description, IReference property, boolean isSettable,
			String category, String[] filterFlags) {
		this(adapterFactory, resourceLocator, displayName, description,
				property, isSettable, false, false, null, category, filterFlags);
	}

	/**
	 * This creates an instance that uses a resource locator; specifies a static
	 * image, a category, and filter flags; and determines the cell editor from
	 * the type of the structural feature.
	 */
	public ItemPropertyDescriptor(IAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, String displayName,
			String description, IReference property, boolean isSettable,
			Object staticImage, String category, String[] filterFlags) {
		this(adapterFactory, resourceLocator, displayName, description,
				property, isSettable, false, false, staticImage, category,
				filterFlags);

	}

	/**
	 * This creates an instance that uses a resource locator; indicates whether
	 * to be multi-line and to sort choices; specifies a static image, a
	 * category, and filter flags; and determines the cell editor from the type
	 * of the structural feature.
	 */
	public ItemPropertyDescriptor(IAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, String displayName,
			String description, IReference property, boolean isSettable,
			boolean multiLine, boolean sortChoices, Object staticImage,
			String category, String[] filterFlags) {
		this.adapterFactory = adapterFactory;
		this.resourceLocator = resourceLocator;
		this.itemDelegator = new ItemDelegator(adapterFactory, resourceLocator);
		this.displayName = displayName;
		this.description = description;
		this.property = property;
		this.isSettable = isSettable;
		this.multiLine = multiLine;
		this.sortChoices = sortChoices;
		this.staticImage = staticImage;
		this.category = category;
		this.filterFlags = filterFlags;
	}

	/**
	 * This creates an instance that uses a resource locator and determines the
	 * cell editor from the parent references.
	 */
	public ItemPropertyDescriptor(IAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, String displayName,
			String description, IReference[] parentReferences,
			boolean isSettable) {
		this(adapterFactory, resourceLocator, displayName, description,
				parentReferences, isSettable, null, null);
	}

	/**
	 * This creates an instance that uses a resource locator, specifies a
	 * category and filter flags, and determines the cell editor from the parent
	 * references.
	 */
	public ItemPropertyDescriptor(IAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, String displayName,
			String description, IReference[] parentReferences,
			boolean isSettable, String category, String[] filterFlags) {
		this.adapterFactory = adapterFactory;
		this.resourceLocator = resourceLocator;
		this.itemDelegator = new ItemDelegator(adapterFactory, resourceLocator);
		this.displayName = displayName;
		this.description = description;
		this.parentReferences = parentReferences;
		this.isSettable = isSettable;
		this.category = category;
		this.filterFlags = filterFlags;
	}

	/**
	 * This returns the group of properties into which this one should be
	 * placed.
	 */
	public String getCategory(Object object) {
		return category;
	}

	/**
	 * This returns the description to be displayed in the property sheet when
	 * this property is selected.
	 */
	public String getDescription(Object object) {
		return description;
	}

	/**
	 * This returns the name of the property to be displayed in the property
	 * sheet.
	 */
	public String getDisplayName(Object object) {
		return displayName;
	}

	/**
	 * This returns the flags used as filters in the property sheet.
	 */
	public String[] getFilterFlags(Object object) {
		return filterFlags;
	}

	/**
	 * This returns the {@link #getDefaultId(EStructuralFeature) default
	 * identifier} of the {@link #property feature} if it's present, or
	 * dash-separated concatenation of the default identifier of each
	 * {@link #parentReferences parent reference}. This key that must uniquely
	 * identify this descriptor among the other descriptors from the same
	 * {@link IItemPropertySource#getPropertyDescriptor(Object, Object) property
	 * source}.
	 */
	public String getId(Object object) {
		if (property != null) {
			return getDefaultId(property);
		} else if (parentReferences != null && parentReferences.length != 0) {
			StringBuffer result = new StringBuffer(
					getDefaultId(parentReferences[0]));
			for (int i = 1; i < parentReferences.length; ++i) {
				result.append('-');
				result.append(getDefaultId(parentReferences[i]));
			}
			return result.toString();
		} else {
			return displayName;
		}
	}

	public Object getHelpContextIds(Object object) {
		return null;
	}

	/**
	 * This will be called to populate a list of choices. The label provider
	 * will be used to determine the labels for the objects this returns. This
	 * default implementation uses {@link #getReachableObjectsOfType
	 * getReachableObjectsOfType}.
	 */
	protected Collection<?> getComboBoxObjects(Object object) {
		if (object instanceof IObject) {
			IModel model = ((IObject) object).getModel();

			if (parentReferences != null) {
				Collection<Object> result = new LinkedHashSet<Object>();
				for (int i = 0; i < parentReferences.length; ++i) {
					parentReferences[i] = (IProperty) model
							.resolve(parentReferences[i]);

					result.addAll(KommaUtil.getInstances(
							model.getManager(),
							((IProperty) parentReferences[i]).getNamedRanges(
									(IObject) object, true).toSet()));
				}
				return result;
			} else if (property != null) {
				Collection<IResource> instances = KommaUtil.getInstances(
						model.getManager(),
						((IProperty) property).getNamedRanges(
								(IResource) object, true).toSet());
				if (((IObject) object).getApplicableCardinality(property)
						.getSecond() == 1 && !instances.contains(null)) {
					instances.add(null);
				}
				return instances;
			}
		}

		return null;
	}

	/**
	 * This returns the label provider that will be used to render the value of
	 * this property. The implementation here just creates an
	 * {@link AdapterFactoryItemDelegator}.
	 */
	public IItemLabelProvider getLabelProvider(Object object) {
		return itemDelegator;
	}

	// /**
	// * This indicates whether these two property descriptors are equal. It's
	// not
	// * really clear to me how this is meant to be used, but it's a little bit
	// * like an equals test.
	// */
	// public boolean isCompatibleWith(Object object, Object anotherObject,
	// IItemPropertyDescriptor anotherItemPropertyDescriptor) {
	// /*
	// * if (propertyDescriptor == this) { return true; } else if
	// * (propertyDescriptor instanceof ItemPropertyDescriptor) {
	// * ItemPropertyDescriptor itemPropertyDescriptor =
	// * (ItemPropertyDescriptor)propertyDescriptor; if (adapterFactory ==
	// * itemPropertyDescriptor.adapterFactory &&
	// * displayName.equals(itemPropertyDescriptor.displayName) && (category
	// * == null && itemPropertyDescriptor.category == null ||
	// * category.equals(itemPropertyDescriptor.category))) { return true; } }
	// */
	//
	// return false;
	// }

	static public class PropertyValueWrapper implements IItemLabelProvider,
			IItemPropertySource {
		protected Object object;
		protected Object propertyValue;
		protected Object nestedPropertySource;
		protected AdapterFactoryItemDelegator itemDelegator;

		public PropertyValueWrapper(IAdapterFactory adapterFactory,
				Object object, Object propertyValue, Object nestedPropertySource) {
			this.object = object;
			this.propertyValue = propertyValue;
			this.nestedPropertySource = nestedPropertySource;
			this.itemDelegator = new AdapterFactoryItemDelegator(adapterFactory);
		}

		public String getText(Object thisObject) {
			return itemDelegator.getText(propertyValue);
		}

		public Object getImage(Object thisObject) {
			return itemDelegator.getImage(propertyValue);
		}

		public List<IItemPropertyDescriptor> getPropertyDescriptors(
				Object thisObject) {
			List<IItemPropertyDescriptor> list = itemDelegator
					.getPropertyDescriptors(nestedPropertySource);
			if (list != null) {
				List<IItemPropertyDescriptor> result = new ArrayList<IItemPropertyDescriptor>(
						list.size());
				for (IItemPropertyDescriptor itemPropertyDescriptor : list) {
					result.add(createPropertyDescriptorDecorator(
							nestedPropertySource, itemPropertyDescriptor));
				}
				return result;
			}

			return Collections.emptyList();
		}

		public IItemPropertyDescriptor getPropertyDescriptor(Object thisObject,
				Object propertyId) {
			return createPropertyDescriptorDecorator(nestedPropertySource,
					itemDelegator.getPropertyDescriptor(nestedPropertySource,
							propertyId));
		}

		public Object getEditableValue(Object thisObject) {
			return propertyValue;
		}

		protected IItemPropertyDescriptor createPropertyDescriptorDecorator(
				Object object, IItemPropertyDescriptor itemPropertyDescriptor) {
			return new ItemPropertyDescriptorDecorator(object,
					itemPropertyDescriptor);
		}
	}

	protected Object createPropertyValueWrapper(Object object,
			Object propertyValue) {
		return new PropertyValueWrapper(adapterFactory, object, propertyValue,
				null);
	}

	/**
	 * This is called by {@link #getPropertyValue getPropertyValue} to
	 * reflectively obtain the value of a feature from an object. It can be
	 * overridden by a subclass to provide additional processing of the value.
	 */
	protected Object getValue(IResource object, IReference property) {
		try {
			return object.get(property);
		} catch (Throwable exception) {
			return null;
		}
	}

	/**
	 * This does the delegated job of getting the property value from the given
	 * object; and it sets object, which is necessary if
	 * {@link #getComboBoxObjects getComboBoxObjects} is called. It is
	 * implemented in a generic way using the structural feature or parent
	 * references.
	 */
	public Object getPropertyValue(Object object) {
		if (property instanceof DatatypeProperty) {
			Object result = getValue((IResource) object,
					(DatatypeProperty) property);

			if (result == null) {
				return null;
			} else {
				return createPropertyValueWrapper(object, result);
			}
		} else if (parentReferences != null) {
			for (int i = 0; i < parentReferences.length; ++i) {
				Object result = getValue((IResource) object,
						parentReferences[i]);
				if (result != null) {
					return createPropertyValueWrapper(object, result);
				}
			}
			return "";
		} else {
			return createPropertyValueWrapper(object,
					getValue((IResource) object, property));
		}
	}

	/**
	 * This does the delegated job of determine whether the property value from
	 * the given object is set. It is implemented in a generic way using the
	 * structural feature.
	 */
	public boolean isPropertySet(Object object) {
		// System.out.println("isPropertySet " + object);
		if (parentReferences != null) {
			for (int i = 0; i < parentReferences.length; ++i) {
				Object value = ((IResource) object).get(parentReferences[i]);
				if (value != null
						&& !(value instanceof Collection<?> && ((Collection<?>) value)
								.isEmpty())) {
					return true;
				}
			}
			return false;
		} else {
			Object value = ((IResource) object).get(property);
			return value != null
					&& !(value instanceof Collection<?> && ((Collection<?>) value)
							.isEmpty());
		}
	}

	/**
	 * This determines whether this descriptor's property for the object
	 * supports set (and reset).
	 */
	public boolean canSetProperty(Object object) {
		if (isSettable) {
			IEditingDomain editingDomain = getEditingDomain(object);
			if (editingDomain != null) {
				return !editingDomain.isReadOnly((IEntity) object);
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	/**
	 * Sets the object to use as the owner of commands created to set the
	 * property's value.
	 */
	public void setCommandOwner(Object commandOwner) {
		this.commandOwner = commandOwner;
	}

	/**
	 * Returns the override command owner set via {@link #setCommandOwner
	 * setCommandOwner}.
	 */
	public Object getCommandOwner() {
		return commandOwner;
	}

	/**
	 * Returns either the override command owner set via
	 * {@link #setCommandOwner setCommandOwner} or, if that is null, the
	 * fall-back object provided.
	 */
	protected Object getCommandOwner(Object fallback) {
		return commandOwner != null ? commandOwner : fallback;
	}

	/**
	 * This does the delegated job of resetting property value back to it's
	 * default value.
	 */
	public void resetPropertyValue(Object object) {
		IObject iObject = (IObject) object;
		IEditingDomain editingDomain = getEditingDomain(object);
		try {
			if (parentReferences != null) {
				for (int i = 0; i < parentReferences.length; ++i) {
					final IReference parentReference = parentReferences[i];
					if (iObject.isPropertySet(parentReference, true)) {
						if (editingDomain == null) {
							iObject.set(parentReferences[i], null);
						} else {
							editingDomain.getCommandStack()
									.execute(
											SetCommand.create(editingDomain,
													getCommandOwner(iObject),
													parentReference,
													SetCommand.UNSET_VALUE),
											null, null);
						}
						break;
					}
				}
			} else {
				if (editingDomain == null) {
					iObject.set(property, null);
				} else {
					editingDomain.getCommandStack().execute(
							SetCommand.create(editingDomain,
									getCommandOwner(iObject), property,
									SetCommand.UNSET_VALUE), null, null);
				}
			}
		} catch (ExecutionException e) {
			Log.error(KommaEditPlugin.getPlugin(), 0,
					"Error while resetting property value", e);
		}
	}

	public IEditingDomain getEditingDomain(Object object) {
		IEditingDomain result = AdapterFactoryEditingDomain
				.getEditingDomainFor(object);
		if (result == null) {
			if (adapterFactory instanceof IEditingDomainProvider) {
				result = ((IEditingDomainProvider) adapterFactory)
						.getEditingDomain();
			}

			if (result == null
					&& adapterFactory instanceof IComposeableAdapterFactory) {
				IAdapterFactory rootAdapterFactory = ((IComposeableAdapterFactory) adapterFactory)
						.getRootAdapterFactory();
				if (rootAdapterFactory instanceof IEditingDomainProvider) {
					result = ((IEditingDomainProvider) rootAdapterFactory)
							.getEditingDomain();
				}
			}
		}
		return result;
	}

	/**
	 * This does the delegated job of setting the property to the given value.
	 * It is implemented in a generic way using the structural feature.
	 */
	public void setPropertyValue(Object object, Object value) {
		IResource resource = (IResource) object;
		IEditingDomain editingDomain = getEditingDomain(object);
		try {
			if (parentReferences != null) {
				ICommand removeCommand = null;
				for (int i = 0; i < parentReferences.length; ++i) {
					Object oldValue = resource.get(parentReferences[i]);
					if (oldValue != null) {
						final IProperty parentReference = (IProperty) resource
								.getEntityManager().find(parentReferences[i]);

						if (oldValue == value) {
							return;
						} else if (parentReference.isRangeCompatible(value)) {
							if (editingDomain == null) {
								resource.set(parentReference, value);
							} else {
								editingDomain.getCommandStack().execute(
										SetCommand.create(editingDomain,
												getCommandOwner(resource),
												parentReference, value), null,
										null);
							}
							return;
						} else {
							if (editingDomain == null) {
								resource.set(parentReference, null);
							} else {
								removeCommand = SetCommand.create(
										editingDomain,
										getCommandOwner(resource),
										parentReference, null);
							}
							break;
						}
					}
				}

				for (int i = 0; i < parentReferences.length; ++i) {
					final IProperty parentReference = (IProperty) resource
							.getEntityManager().find(parentReferences[i]);

					if (parentReference.isRangeCompatible(value)) {
						if (editingDomain == null) {
							resource.set(parentReferences[i], value);
						} else {
							if (removeCommand != null) {
								final ExtendedCompositeCommand compoundCommand = new ExtendedCompositeCommand(
										ExtendedCompositeCommand.LAST_COMMAND_ALL);
								compoundCommand.add(removeCommand);
								compoundCommand.add(SetCommand.create(
										editingDomain,
										getCommandOwner(resource),
										parentReference, value));
								editingDomain.getCommandStack().execute(
										compoundCommand, null, null);
							} else {
								editingDomain.getCommandStack().execute(
										SetCommand.create(editingDomain,
												getCommandOwner(resource),
												parentReference, value), null,
										null);
							}
						}
						break;
					}
				}
			} else {
				if (editingDomain == null) {
					resource.set(property, value);
				} else {
					editingDomain.getCommandStack()
							.execute(
									SetCommand.create(editingDomain,
											getCommandOwner(resource),
											property, value), null, null);
				}
			}
		} catch (ExecutionException e) {
			Log.error(KommaEditPlugin.getPlugin(), 0,
					"Error while setting property value", e);
		}
	}

	public Object getProperty(Object object) {
		if (property != null) {
			return property;
		} else if (parentReferences != null) {
			return parentReferences;
		} else {
			return null;
		}
	}

	/**
	 * Returns whether this property represents multiple values. This is true
	 * only if we're using a {@link #property structural feature} to provide the
	 * values for this property, and if that feature is multi-valued.
	 */
	public boolean isMany(Object object) {
		return parentReferences == null
				&& property != null
				&& object instanceof IResource
				&& ((IResource) object).getApplicableCardinality(property)
						.getSecond() > 1;
	}

	public Collection<?> getChoiceOfValues(Object object) {
		return getComboBoxObjects(object);
	}

	public boolean isMultiLine(Object object) {
		return multiLine;
	}

	public boolean isSortChoices(Object object) {
		return sortChoices;
	}
}
