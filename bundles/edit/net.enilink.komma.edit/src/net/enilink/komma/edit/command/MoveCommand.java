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
 * $Id: MoveCommand.java,v 1.6 2008/05/07 19:08:46 emerks Exp $
 */
package net.enilink.komma.edit.command;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IReference;

/**
 * The move command logically acts upon an owner object that has a
 * collection-based feature containing an object that is to be moved to a new
 * position within the collection. The static create method delegates command
 * creation to {@link IEditingDomain#createCommand EditingDomain.createCommand},
 * which may or may not result in the actual creation of an instance of this
 * class. Like all the low level commands in this package, the move command is
 * undoable.
 * 
 * <p>
 * The implementation of this class is low-level and EMF specific; it allows an
 * object to be moved to a new position within a many-valued feature of an
 * owner, i.e., it is equivalent of the call
 * 
 * <pre>
 * ((EList) ((EObject) owner).eGet((EStructuralFeature) feature)).move(index,
 * 		object);
 * </pre>
 * 
 * <p>
 * It can also be used as an equivalent to the call
 * 
 * <pre>
 * ((EList) extent).move(index, object);
 * </pre>
 * 
 * which is how root objects are moved within the contents of a resource. Like
 * all the low-level commands in this package, the move command is undoable.
 * 
 * <p>
 * A move command is an {@link IOverrideableCommand}.
 */
public class MoveCommand extends AbstractOverrideableCommand {
	/**
	 * This creates a command to move particular value to a particular index in
	 * the specified feature of the owner. The feature will often be null
	 * because the domain will deduce it.
	 */
	public static ICommand create(IEditingDomain domain, Object owner,
			Object property, Object value, int index) {
		return domain.createCommand(MoveCommand.class, new CommandParameter(
				owner, property, value, index));
	}

	/**
	 * This caches the label.
	 */
	protected static final String LABEL = KommaEditPlugin.INSTANCE
			.getString("_UI_MoveCommand_label");

	/**
	 * This caches the description.
	 */
	protected static final String DESCRIPTION = KommaEditPlugin.INSTANCE
			.getString("_UI_MoveCommand_description");

	/**
	 * This caches the description for a list-based command.
	 */
	protected static final String DESCRIPTION_FOR_LIST = KommaEditPlugin.INSTANCE
			.getString("_UI_MoveCommand_description_for_list");

	/**
	 * This is the owner object upon which the command will act. It could be
	 * null in the case that we are dealing with an
	 * {@link net.enilink.komma.rmf.common.util.EList}.
	 */
	protected IObject owner;

	/**
	 * This is the feature of the owner object upon the command will act. It
	 * could be null, in the case that we are dealing with an
	 * {@link net.enilink.komma.rmf.common.util.EList}.
	 */
	protected IReference property;

	/**
	 * This is the list in which the command will move an object.
	 */
	protected Collection<Object> ownerList;

	/**
	 * This is the value being moved within the owner list.
	 */
	protected Object value;

	/**
	 * This is the position to which the object will be moved.
	 */
	protected int index;

	/**
	 * This is the original position to which the object will be moved upon
	 * undo.
	 */
	protected int oldIndex;

	/**
	 * This constructs a primitive command to move a particular value to a
	 * particular index of the specified many-valued feature of the owner.
	 */
	public MoveCommand(IEditingDomain domain, IObject owner,
			IReference property, Object value, int index) {
		super(domain, LABEL, DESCRIPTION);

		this.owner = owner;
		this.property = property;
		this.value = value;
		this.index = index;

		ownerList = getOwnerList(this.owner, property);
	}

	/**
	 * This constructs a primitive command to move a particular value to a
	 * particular index of the specified extent.
	 */
	public MoveCommand(IEditingDomain domain, Collection<?> list, Object value,
			int index) {
		super(domain, LABEL, DESCRIPTION_FOR_LIST);

		this.value = value;
		this.index = index;

		@SuppressWarnings("unchecked")
		List<Object> untypedList = (List<Object>) list;
		ownerList = untypedList;
	}

	/**
	 * This returns the owner object upon which the command will act. It could
	 * be null in the case that we are dealing with an
	 * {@link net.enilink.komma.rmf.common.util.EList}.
	 */
	public IObject getOwner() {
		return owner;
	}

	/**
	 * This returns the feature of the owner object upon the command will act.
	 * It could be null, in the case that we are dealing with an
	 * {@link net.enilink.komma.rmf.common.util.EList}.
	 */
	public IReference getProperty() {
		return property;
	}

	/**
	 * This returns the list in which the command will move an object.
	 */
	public Collection<Object> getOwnerList() {
		return ownerList;
	}

	/**
	 * This returns the value being moved.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * This returns the position to which the value will be moved.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * This returns the original position to which the object will be moved upon
	 * undo.
	 */
	public int getOldIndex() {
		return oldIndex;
	}

	@Override
	protected boolean prepare() {
		// Return whether there is a list, the value is in the list, and index
		// is in range...
		boolean result = ownerList != null && ownerList instanceof List<?>
				&& ownerList.contains(value) && index >= 0
				&& index < ownerList.size()
				&& (owner == null || !getDomain().isReadOnly(owner.getModel()));

		return result;
	}

	@Override
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		oldIndex = ((List<?>) ownerList).indexOf(value);
		((List<Object>) ownerList).add(index, ownerList.remove(oldIndex));

		return CommandResult.newOKCommandResult(Collections.singleton(value));
	}

	@Override
	public Collection<?> doGetAffectedObjects() {
		return Collections.singleton(value);
	}

	@Override
	public Collection<?> doGetAffectedResources(Object type) {
		if (IModel.class.equals(type) && (owner != null || ownerList != null)) {
			Collection<Object> affected = new HashSet<Object>(
					super.doGetAffectedResources(type));
			if (value instanceof IObject) {
				affected.add(((IObject) value).getModel());
			}

			if (owner != null) {
				affected.add(owner.getModel());
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
		result.append(" (property: " + property + ")");
		result.append(" (ownerList: " + ownerList + ")");
		result.append(" (value: " + value + ")");
		result.append(" (index: " + index + ")");
		result.append(" (oldIndex: " + oldIndex + ")");

		return result.toString();
	}
}
