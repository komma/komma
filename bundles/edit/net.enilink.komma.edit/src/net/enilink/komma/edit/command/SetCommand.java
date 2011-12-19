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
 * $Id: SetCommand.java,v 1.15 2008/04/22 19:46:16 emerks Exp $
 */
package net.enilink.komma.edit.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.vocab.owl.InverseFunctionalProperty;
import net.enilink.vocab.owl.ObjectProperty;
import net.enilink.vocab.rdf.Property;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ExtendedCompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IReference;

/**
 * The set command logically acts upon an owner object to set a particular
 * feature to a specified value or to unset a feature. The static create methods
 * delegate command creation to {@link IEditingDomain#createCommand
 * EditingDomain.createCommand}, which may or may not result in the actual
 * creation of an instance of this class.
 * 
 * <p>
 * The implementation of this class is low-level and EMF specific; it allows a
 * value to be set to a single-valued feature of an owner, i.e., it is
 * equivalent of the call
 * 
 * <pre>
 * ((EObject) object).eSet((EStructuralFeature) feature, value);
 * </pre>
 * 
 * or to
 * 
 * <pre>
 * ((EObject) object).eUnset((EStructuralFeature) feature);
 * </pre>
 * 
 * if the value is {@link #UNSET_VALUE}.
 * <p>
 * Setting a feature that is a bidirectional reference with a multiplicity-many
 * reverse or with a multiplicity-1 reverse that is already set (on value), is
 * not undoable. In this case, the SetCommand static create function will not
 * return an instance of this class, but instead will return a compound command
 * (e.g., a {@link RemoveCommand} followed by an {@link AddCommand} for the
 * other end of the relation) which could not be undone.
 * <p>
 * The exception to the above is when an empty list is being set to empty or
 * unset. Such commands are undoable and represent the only way to toggle
 * whether the feature is set.
 * <p>
 * When setting a containment (or container) feature, we always assume that the
 * object that will be contained is not already in a container, but take no
 * action in this class to ensure this is the case.
 * <p>
 * A set command is an {@link IOverrideableCommand}.
 */
public class SetCommand extends AbstractOverrideableCommand {
	/**
	 * Specify this as the value in order to unset a feature. Note that this
	 * value can be specified for a multiplicity-1 feature or for a
	 * multiplicity-many feature with no index given. Unsetting a single value
	 * within a list is not possible.
	 */
	public static final Object UNSET_VALUE = new Object();

	/**
	 * This creates a command to set the owner's feature to the specified value.
	 */
	public static ICommand create(IEditingDomain domain, final Object owner,
			Object feature, Object value) {
		return create(domain, owner, feature, value, CommandParameter.NO_INDEX);
	}

	/**
	 * This creates a command to set the owner's feature to the specified value
	 * at the specified index.
	 */
	public static ICommand create(IEditingDomain domain, final Object owner,
			Object property, Object value, int index) {
		if (owner instanceof IResource
				&& property != null
				&& ((IResource) owner)
						.hasApplicableProperty((IReference) property)) {
			if (((IResource) owner).getApplicableCardinality(
					(IReference) property).getSecond() != 1
					&& index == CommandParameter.NO_INDEX) {
				// We never directly set a multiplicity-many feature to a list
				// directly. Instead, we remove the old values
				// values, move the values that remain, and insert the new
				// values. If all old values are removed, we'll still
				// set it to an empty list, or unset it, as appropriate.
				//
				List<?> values = value == UNSET_VALUE ? Collections.EMPTY_LIST
						: (List<?>) value;
				Collection<?> oldValues = (Collection<?>) ((IResource) owner)
						.get((IReference) property);

				ExtendedCompositeCommand compound = null;
				compound = new ExtendedCompositeCommand(
						ExtendedCompositeCommand.LAST_COMMAND_ALL, LABEL,
						DESCRIPTION) {
					@Override
					public Collection<?> getAffectedObjects() {
						return Collections.singleton(owner);
					}
				};

				if (!oldValues.isEmpty()) {
					if (!values.isEmpty()) {
						Set<Object> removedValues = new HashSet<Object>(
								oldValues);
						removedValues.removeAll(values);

						// If we aren't simply removing all the old values...
						//
						if (!removedValues.equals(oldValues)) {
							// If there are values to remove, append a command
							// for them.
							//
							if (!removedValues.isEmpty()) {
								compound.add(RemoveCommand.create(domain,
										owner, property, new HashSet<Object>(
												removedValues)));
							}

							// Determine the values that will remain and move
							// them into the right order, if necessary.
							//
							List<Object> remainingValues = new ArrayList<Object>(
									oldValues);
							remainingValues.removeAll(removedValues);
							int count = -1;
							for (Object object : values) {
								int position = remainingValues.indexOf(object);
								if (position != -1 && position != ++count) {
									compound.add(MoveCommand.create(domain,
											owner, property, object, count));
								}
							}

							// Determine the values to be added and add them at
							// the right position.
							//
							Set<Object> addedValues = new HashSet<Object>(
									values);
							addedValues.removeAll(remainingValues);
							for (ListIterator<?> i = values.listIterator(); i
									.hasNext();) {
								Object object = i.next();
								if (addedValues.contains(object)) {
									int addIndex = i.previousIndex();
									if (addIndex > oldValues.size()) {
										addIndex = -1;
									}
									compound.add(AddCommand.create(domain,
											owner, property, object, addIndex));
								}
							}
							return compound;
						}
					}

					compound.add(RemoveCommand.create(domain, owner, property,
							new HashSet<Object>(oldValues)));
				}

				if (!values.isEmpty()) {
					compound.add(AddCommand.create(domain, owner, property,
							values));
				} else if (value == UNSET_VALUE) {// &&
					// eReference.isUnsettable())
					// {
					compound.add(domain.createCommand(SetCommand.class,
							new CommandParameter(owner, property, value)));
				} else if (compound.getCommandList().isEmpty()) {
					return IdentityCommand.INSTANCE;
				}
				return compound;
			} // end setting whole list
				// else if (resolvedProperty instanceof ObjectProperty) {
				// for (ObjectProperty otherEnd : ((ObjectProperty)
				// resolvedProperty)
				// .getOwlInverseOf()) {
				// if (((IProperty)otherEnd).isMany()) {
				// if (eReference.isMany()) {
				// // For a many-to-many association, the command can
				// // only
				// // be undoable if the value or owner is last in its
				// // respective list, since the undo will include an
				// // inverse add. So, if the value is last, but the
				// // owner
				// // is
				// // not, we create an undoable compound command that
				// // removes from the opposite end and then inserts
				// // the
				// // new
				// // value.
				// //
				// EList<?> list = (EList<?>) ((EObject) owner)
				// .eGet(eReference);
				// if (index == list.size() - 1) {
				// EObject oldValue = (EObject) list.get(index);
				// EList<?> oppositeList = (EList<?>) oldValue
				// .eGet(eOtherEnd);
				// if (oppositeList.get(oppositeList.size() - 1) != owner) {
				// CompoundCommand compound = new CompoundCommand(
				// CompoundCommand.LAST_COMMAND_ALL,
				// LABEL, DESCRIPTION) {
				// @Override
				// public Collection<?> getAffectedObjects() {
				// return Collections.singleton(owner);
				// }
				// };
				// compound
				// .append(RemoveCommand.create(
				// domain, oldValue,
				// eOtherEnd, owner));
				// compound.append(AddCommand.create(domain,
				// owner, property, value));
				// return compound;
				// }
				// }
				// } else {
				// // For a 1-to-many association, doing the set as a
				// // remove and add from the other end will make it
				// // undoable.
				// // In particular, if there is an existing non-null
				// // value, we first need to remove it from the other
				// // end,
				// // so
				// // that it will be reinserted at the correct index
				// // on
				// // undo.
				// //
				// Object oldValue = ((EObject) owner)
				// .eGet(eReference);
				//
				// if (value == null || value == UNSET_VALUE) {
				// if (oldValue == null) { // (value == null) &&
				// // (oldValue == null)
				// // A simple set/unset will suffice.
				// //
				// return domain.createCommand(
				// SetCommand.class,
				// new CommandParameter(owner,
				// eReference, value));
				// } else { // (value == null) && (oldValue !=
				// // null)
				// // Remove owner from the old value and unset
				// // if
				// // necessary.
				// //
				// Command removeCommand = RemoveCommand
				// .create(domain, oldValue,
				// eOtherEnd, Collections
				// .singleton(owner));
				//
				// if (value != UNSET_VALUE
				// || !eReference.isUnsettable()) {
				// return removeCommand;
				// } else {
				// CompoundCommand compound = new CompoundCommand(
				// LABEL, DESCRIPTION);
				// compound.append(removeCommand);
				// compound.append(domain.createCommand(
				// SetCommand.class,
				// new CommandParameter(owner,
				// eReference, value)));
				// return compound;
				// }
				// }
				// } else { // ((value != null)
				// Command addCommand = new CommandWrapper(
				// AddCommand.create(domain, value,
				// eOtherEnd, Collections
				// .singleton(owner))) {
				// @Override
				// public Collection<?> getAffectedObjects() {
				// return Collections.singleton(owner);
				// }
				// };
				//
				// if (oldValue == null) { // (value != null) &&
				// // (oldValue == null)
				// // Add owner to new value.
				// //
				// return addCommand;
				// } else { // ((value != null) && (oldValue !=
				// // null))
				// // Need a compound command to remove owner
				// // from
				// // old value and add it to new value.
				// //
				// CompoundCommand compound = new CompoundCommand(
				// CompoundCommand.LAST_COMMAND_ALL,
				// LABEL, DESCRIPTION);
				// compound.append(RemoveCommand.create(
				// domain, oldValue, eOtherEnd,
				// Collections.singleton(owner)));
				// compound.append(addCommand);
				// return compound;
				// }
				// }
				// }
				// } else if (eOtherEnd.isContainment()) {
				// if (value != null && value != UNSET_VALUE) {
				// // For consistency, we always set 1-1 container
				// // relations from the container end.
				// //
				// return new CommandWrapper(SetCommand.create(domain,
				// value, eOtherEnd, owner)) {
				// @Override
				// public Collection<?> getResult() {
				// return Collections.singleton(owner);
				// }
				//
				// @Override
				// public Collection<?> getAffectedObjects() {
				// return Collections.singleton(owner);
				// }
				// };
				// }
				// } else {
				// // For a many-to-1 or 1-to-1 association, if the
				// // opposite
				// // reference on the new value is already set to
				// // something, we need a compound command that first
				// // explicitly removes that reference, so that it will be
				// // restored in the undo.
				// //
				// if (value instanceof EObject) {
				// EObject otherEObject = (EObject) ((EObject) value)
				// .eGet(eOtherEnd);
				// if (otherEObject != null) {
				// CompoundCommand compound = new CompoundCommand(
				// CompoundCommand.LAST_COMMAND_ALL) {
				// @Override
				// public boolean canUndo() {
				// return true;
				// }
				// };
				// if (eReference.isMany()) {
				// // For a many-to-1, we use
				// // SetCommand.create()
				// // to create the command to remove the
				// // opposite
				// // reference;
				// // a RemoveCommand on its opposite will
				// // actually
				// // result.
				// //
				// compound.append(SetCommand.create(domain,
				// value, eOtherEnd, null));
				// } else {
				// // For a 1-to-1, we can directly create a
				// // SetCommand.
				// //
				// compound
				// .append(domain
				// .createCommand(
				// SetCommand.class,
				// eOtherEnd
				// .isChangeable() ? new CommandParameter(
				// value,
				// eOtherEnd,
				// null)
				// : new CommandParameter(
				// otherEObject,
				// eReference,
				// null)));
				// }
				// compound.append(domain.createCommand(
				// SetCommand.class,
				// new CommandParameter(owner, eReference,
				// value, index)));
				// return compound;
				// }
				// }
				// }
				// }
				// }
		}
		return domain.createCommand(SetCommand.class, new CommandParameter(
				owner, property, value, index));
	}

	/**
	 * This caches the label.
	 */
	protected static final String LABEL = KommaEditPlugin.INSTANCE
			.getString("_UI_SetCommand_label");

	/**
	 * This caches the description.
	 */
	protected static final String DESCRIPTION = KommaEditPlugin.INSTANCE
			.getString("_UI_SetCommand_description");

	/**
	 * This is the owner object upon which the command will act.
	 */
	protected IResource owner;

	/**
	 * This is the feature of the owner object upon the command will act.
	 */
	protected IReference property;

	/**
	 * If non-null, this is the list in which the command will set a value. If
	 * null, feature is single-valued or no index was specified.
	 */
	protected Collection<Object> ownerList;

	/**
	 * This is the value to be set.
	 */
	protected Object value;

	/**
	 * This is the old value of the feature which must be restored during undo.
	 */
	protected Object oldValue;

	/**
	 * This is the position at which the object will be set.
	 */
	protected int index;

	/**
	 * This specified whether or not this command can be undone.
	 */
	protected boolean canUndo = true;

	/**
	 * This is any remove commands needed to clear this many valued list or to
	 * update the opposite properly.
	 */
	protected ICommand removeCommand;

	/**
	 * This constructs a primitive command to set the owner's feature to the
	 * specified value.
	 */
	public SetCommand(IEditingDomain domain, IResource owner,
			IReference feature, Object value) {
		super(domain, LABEL, DESCRIPTION);

		// Initialize all the fields from the command parameter.
		//
		this.owner = owner;
		this.property = feature;
		this.value = value;
		this.index = CommandParameter.NO_INDEX;
	}

	/**
	 * This constructs a primitive command to set the owner's feature to the
	 * specified value at the given index.
	 */
	public SetCommand(IEditingDomain domain, IResource owner,
			IReference feature, Object value, int index) {
		super(domain, LABEL, DESCRIPTION);

		// Initialize all the fields from the command parameter.
		//
		this.owner = owner;
		this.property = feature;
		this.value = value;
		this.index = index;

		if (index != CommandParameter.NO_INDEX) {
			ownerList = getOwnerList(owner, feature);
		}
	}

	/**
	 * This returns the owner object upon which the command will act.
	 */
	public IResource getOwner() {
		return owner;
	}

	/**
	 * This returns the feature of the owner object upon the command will act.
	 */
	public IReference getProperty() {
		return property;
	}

	/**
	 * If the command will set a single value in a list, this returns the list
	 * in which it will set; null otherwise.
	 */
	public Collection<Object> getOwnerList() {
		return ownerList;
	}

	/**
	 * This returns the position at which the objects will be added.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * This returns the value to be set.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * This returns the old value of the feature which must be restored during
	 * undo.
	 */
	public Object getOldValue() {
		return oldValue;
	}

	@Override
	protected boolean prepare() {
		boolean result = false;

		// If there is an owner.
		//
		if (owner != null) {
			if (getDomain().isReadOnly(owner) || property == null) {
				return false;
			}

			IProperty resolvedProperty = (IProperty) owner.getEntityManager()
					.find(property);

			// Is the feature an attribute of the owner...
			// if (resolvedProperty.isDomainCompatible(owner)) {
			// If must be of this type then.
			if (ownerList != null) {
				// Setting at an index. Make sure the index is valid,
				// the
				// type is valid, and the value isn't already in a
				// unique feature. Record the old value.
				//
				if (index >= 0
						&& ownerList instanceof List
						&& index < ownerList.size()
						&& resolvedProperty.isRangeCompatible(value)
						&& (!(resolvedProperty instanceof InverseFunctionalProperty) || !ownerList
								.contains(value))) {
					oldValue = ((List<?>) ownerList).get(index);
					result = true;
				}
			} else if (resolvedProperty.isMany(owner)) {
				// If the attribute is set, record it's old value.
				if (owner.isPropertySet(property, true)) {
					oldValue = owner.get(property);
				} else {
					oldValue = UNSET_VALUE;
				}

				if (value == UNSET_VALUE) {
					result = true;
				} else if (value instanceof Collection<?>) {
					Collection<?> collection = (Collection<?>) value;
					result = true;
					if (!resolvedProperty.hasListRange()) {
						for (Object object : collection) {
							if (!resolvedProperty.isRangeCompatible(object)) {
								result = false;
								break;
							}
						}
					}
				}
			} else {
				// If the attribute is set, record it's old value.
				if (owner.isPropertySet(property, true)) {
					oldValue = owner.get(property);
				} else {
					oldValue = UNSET_VALUE;
				}

				result = value == null || value == UNSET_VALUE
						|| resolvedProperty.isRangeCompatible(value);
			}

			// Make sure the container is not being put into a contained
			// object.
			if (result && resolvedProperty instanceof ObjectProperty
					&& resolvedProperty.isContainment()) {
				// use seen to prevent infinite loops due to invalid usage
				// of komma:contains
				Set<IResource> seen = new HashSet<IResource>();
				for (IResource container = owner; container != null; container = container
						.getContainer()) {
					if (!seen.add(container) || value.equals(container)) {
						result = false;
						break;
					}
				}
			}
			// }
		}

		return result;
	}

	@Override
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		property = owner.getEntityManager().find(property);

		// Check whether there is an opposite that needs attention.
		if (property instanceof ObjectProperty
				&& !((ObjectProperty) property).getOwlInverseOf().isEmpty()) {
			// Because of the old factoring approach in the create method,
			// it might be the case that the state of the old value has
			// changed by the time we get here,
			// and in that case, we don't want to duplicate the removals in
			// this code.
			//
			if (oldValue instanceof Collection<?>) {
				oldValue = new ArrayList<Object>(
						(Collection<?>) owner.get(property));
			} else if (oldValue != UNSET_VALUE
					&& index == CommandParameter.NO_INDEX) {
				oldValue = owner.get(property);
			}
			for (Property otherProperty : ((ObjectProperty) property)
					.getOwlInverseOf()) {
				if (oldValue instanceof IResource
						&& ((IProperty) otherProperty)
								.isMany((IResource) oldValue)) {
					// If the other end is a many, then we should remove the
					// owner from the old value's opposite feature so that
					// undo
					// will put it back.
					if (oldValue instanceof Collection<?>) {
						@SuppressWarnings("unchecked")
						Collection<IResource> oldValues = (Collection<IResource>) oldValue;
						if (oldValues != null && !oldValues.isEmpty()) {
							ExtendedCompositeCommand compoundCommand = new ExtendedCompositeCommand();
							for (IResource oldValueObject : oldValues) {
								compoundCommand
										.appendIfCanExecute(new RemoveCommand(
												getDomain(), oldValueObject,
												otherProperty, owner));
							}
							removeCommand = compoundCommand;
						}
					}
				} else {
					// If the other end is single, then we should unset the
					// owner from the old value's opposite feature so that
					// undo will put it back.
					if (value instanceof Collection<?>) {
						@SuppressWarnings("unchecked")
						Collection<IResource> newValues = (Collection<IResource>) value;
						if (!newValues.isEmpty()) {
							ExtendedCompositeCommand compoundCommand = new ExtendedCompositeCommand();
							for (IResource newValueObject : newValues) {
								compoundCommand
										.appendIfCanExecute(new SetCommand(
												getDomain(), newValueObject,
												otherProperty, UNSET_VALUE));
								IResource otherObject = (IResource) newValueObject
										.get(otherProperty);
								if (otherObject != null) {
									compoundCommand
											.appendIfCanExecute(new SetCommand(
													getDomain(),
													newValueObject,
													otherProperty, UNSET_VALUE));
								}
							}
							removeCommand = compoundCommand;
						}
					} else if (value instanceof IResource) {
						IResource eObject = (IResource) value;
						IResource otherEObject = (IResource) eObject
								.get(otherProperty);
						if (otherEObject != null) {
							removeCommand = new SetCommand(getDomain(),
									eObject, otherProperty, UNSET_VALUE);
						}
					}
				}
				if (removeCommand != null) {
					if (removeCommand.canExecute()) {
						removeCommand.execute(progressMonitor, info);
					} else {
						removeCommand = null;
					}
				}
			}
		}

		// Either set or unset the feature.
		if (ownerList instanceof List<?>) {
			if (removeCommand == null || ownerList.size() > index
					&& ((List<?>) ownerList).get(index) == oldValue) {
				((List<Object>) ownerList).set(index, value);
			} else {
				((List<Object>) ownerList).add(index, value);
			}
		} else if (value == UNSET_VALUE) {
			owner.removeProperty(property);
		} else {
			owner.set(property, value);
		}

		return CommandResult.newOKCommandResult(Collections.singleton(owner));
	}

	@Override
	public boolean doCanUndo() {
		return canUndo;
	}

	@Override
	public Collection<?> doGetAffectedObjects() {
		return Collections.singleton(owner);
	}

	@Override
	public Collection<?> doGetAffectedResources(Object type) {
		if (IModel.class.equals(type) && (owner != null || ownerList != null)) {
			Collection<Object> affected = new HashSet<Object>(
					super.doGetAffectedResources(type));
			if (owner instanceof IObject) {
				affected.add(((IObject) owner).getModel());
			}

			if (ownerList != null) {
				for (Object element : ownerList) {
					Object object = AdapterFactoryEditingDomain.unwrap(element);
					if (object instanceof IObject) {
						affected.add(((IObject) object).getModel());
					}
				}
			}

			return affected;
		}
		return super.doGetAffectedResources(type);
	}

	/**
	 * This gives an abbreviated name using this object's own class' name,
	 * without package qualification, followed by a space separated list of
	 * <tt>field:value</tt> pairs.
	 */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (owner: " + owner + ")");
		result.append(" (feature: " + property + ")");
		if (ownerList != null) {
			result.append(" (ownerList: " + ownerList + ")");
			result.append(" (index: " + index + ")");
		}
		result.append(" (value: " + value + ")");
		result.append(" (oldValue: " + oldValue + ")");

		return result.toString();
	}
}
