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
 * $Id: AddCommand.java,v 1.12 2008/04/22 19:46:16 emerks Exp $
 */
package net.enilink.komma.edit.command;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.vocab.rdf.List;
import net.enilink.vocab.rdfs.Class;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IReference;

/**
 * The add command logically acts upon an owner object that has a
 * collection-based feature to which other objects can be added. The static
 * create methods delegate command creation to
 * {@link IEditingDomain#createCommand EditingDomain.createCommand}, which may
 * or may not result in the actual creation of an instance of this class.
 * 
 * <p>
 * The implementation of this class is low-level and EMF specific; it allows one
 * or more objects to be added to a many-valued feature of an owner, i.e., it is
 * equivalent of the call
 * 
 * <pre>
 * ((EList) ((EObject) owner).eGet((EStructuralFeature) feature)).addAll(index,
 * 		(Collection) collection);
 * </pre>
 * 
 * <p>
 * It can also be used as an equivalent to the call
 * 
 * <pre>
 * ((EList) extent).addAll(index, (Collection) collection);
 * </pre>
 * 
 * which is how root objects are added into the contents of a resource. Like all
 * the low-level commands in this package, the add command is undoable.
 * 
 * <p>
 * An add command is an {@link IOverrideableCommand}.
 */
public class AddCommand extends AbstractOverrideableCommand {
	/**
	 * This caches the description.
	 */
	protected static final String DESCRIPTION = KommaEditPlugin.INSTANCE
			.getString("_UI_AddCommand_description");

	/**
	 * This caches the description for a list-based addition.
	 */
	protected static final String DESCRIPTION_FOR_LIST = KommaEditPlugin.INSTANCE
			.getString("_UI_AddCommand_description_for_list");

	/**
	 * This caches the label.
	 */
	protected static final String LABEL = KommaEditPlugin.INSTANCE
			.getString("_UI_AddCommand_label");

	/**
	 * This creates a command to add a collection of values to the specified
	 * feature of the owner. The feature will often be null because the domain
	 * will deduce it.
	 */
	public static ICommand create(IEditingDomain domain, Object owner,
			Object property, Collection<?> collection) {
		return domain.createCommand(AddCommand.class, new CommandParameter(
				owner, property, collection, CommandParameter.NO_INDEX));
	}

	/**
	 * This creates a command to insert a collection of values at a particular
	 * index in the specified feature of the owner. The feature will often be
	 * null because the domain will deduce it.
	 */
	public static ICommand create(IEditingDomain domain, Object owner,
			Object property, Collection<?> collection, int index) {
		return domain.createCommand(AddCommand.class, new CommandParameter(
				owner, property, collection, index));
	}

	/**
	 * This creates a command to add a particular value to the specified feature
	 * of the owner. The feature will often be null because the domain will
	 * deduce it.
	 */
	public static ICommand create(IEditingDomain domain, Object owner,
			Object property, Object value) {
		return create(domain, owner, property, Collections.singleton(value),
				CommandParameter.NO_INDEX);
	}

	/**
	 * This creates a command to insert particular value at a particular index
	 * in the specified feature of the owner. The feature will often be null
	 * because the domain will deduce it.
	 */
	public static ICommand create(IEditingDomain domain, Object owner,
			Object property, Object value, int index) {
		return create(domain, owner, property, Collections.singleton(value),
				index);
	}

	/**
	 * This is the value returned by {@link ICommand#getAffectedObjects}. The
	 * affected objects are different after an execute than after an undo, so we
	 * record it.
	 */
	protected Collection<?> affectedObjects;

	/**
	 * This is the collection of objects being added to the owner list.
	 */
	protected Collection<?> collection;

	/**
	 * This is the position at which the objects will be inserted.
	 */
	protected int index;

	/**
	 * This is the owner object upon which the command will act. It could be
	 * null in the case that we are dealing with an
	 * {@link net.enilink.komma.rmf.common.util.EList}.
	 */
	protected IObject owner;

	/**
	 * This is the list to which the command will add the collection.
	 */
	protected Collection<Object> ownerList;

	/**
	 * This is the feature of the owner object upon the command will act. It
	 * could be null, in the case that we are dealing with an
	 * {@link net.enilink.komma.rmf.common.util.EList}.
	 */
	protected IReference property;

	/**
	 * This constructs a primitive command to insert a collection of values into
	 * the specified extent.
	 */
	public AddCommand(IEditingDomain domain, Collection<?> list,
			Collection<?> collection) {
		this(domain, list, collection, CommandParameter.NO_INDEX);
	}

	/**
	 * This constructs a primitive command to insert a collection of values into
	 * the specified extent.
	 */
	public AddCommand(IEditingDomain domain, Collection<?> list,
			Collection<?> collection, int index) {
		super(domain, LABEL, DESCRIPTION_FOR_LIST);

		this.collection = collection;
		this.index = index;

		@SuppressWarnings("unchecked")
		Collection<Object> untypedList = (Collection<Object>) list;
		ownerList = untypedList;
	}

	/**
	 * This constructs a primitive command to add a particular value into the
	 * specified extent.
	 */
	public AddCommand(IEditingDomain domain, Collection<?> list, Object value) {
		this(domain, list, Collections.singleton(value),
				CommandParameter.NO_INDEX);
	}

	/**
	 * This constructs a primitive command to insert particular value into the
	 * specified extent.
	 */
	public AddCommand(IEditingDomain domain, Collection<?> list, Object value,
			int index) {
		this(domain, list, Collections.singleton(value), index);
	}

	/**
	 * This constructs a primitive command to add a collection of values to the
	 * specified many-valued feature of the owner.
	 */
	public AddCommand(IEditingDomain domain, IObject owner,
			IReference property, Collection<?> collection) {
		this(domain, owner, property, collection, CommandParameter.NO_INDEX);
	}

	/**
	 * This constructs a primitive command to insert a collection of values into
	 * the specified many-valued feature of the owner.
	 */
	public AddCommand(IEditingDomain domain, IObject owner,
			IReference property, Collection<?> collection, int index) {
		super(domain, LABEL, DESCRIPTION);

		this.owner = owner;
		this.property = property;
		this.collection = collection;
		this.index = index;

		ownerList = getOwnerList(owner, property);
	}

	/**
	 * This constructs a primitive command to add a particular value to the
	 * specified many-valued feature of the owner.
	 */
	public AddCommand(IEditingDomain domain, IObject owner,
			IReference property, Object value) {
		this(domain, owner, property, Collections.singleton(value),
				CommandParameter.NO_INDEX);
	}

	/**
	 * This constructs a primitive command to insert particular value into the
	 * specified many-valued feature of the owner.
	 */
	public AddCommand(IEditingDomain domain, IObject owner,
			IReference property, Object value, int index) {
		this(domain, owner, property, Collections.singleton(value), index);
	}

	@Override
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		boolean transactionWasActive = owner.getKommaManager().getTransaction()
				.isActive();
		if (!transactionWasActive) {
			owner.getKommaManager().getTransaction().begin();
		}
		try {
			Class listClass = getListType();
			if (listClass != null) {
				@SuppressWarnings("unchecked")
				List<Object> newList = (List<Object>) ((IClass) listClass)
						.newInstance();
				ownerList = newList;
			}

			// Simply add the collection to the list.
			if (ownerList instanceof List<?>
					&& index != CommandParameter.NO_INDEX) {
				((List<Object>) ownerList).addAll(index, collection);
			} else {
				ownerList.addAll(collection);
			}

			// We'd like the collection of things added to be selected after
			// this
			// command completes.
			affectedObjects = collection;

			if (!transactionWasActive) {
				owner.getKommaManager().getTransaction().commit();
			}
		} catch (Throwable e) {
			if (!transactionWasActive) {
				owner.getKommaManager().getTransaction().rollback();
			}
			throw new ExecutionException("Error while adding element", e);
		}

		return CommandResult.newOKCommandResult(collection);
	}

	@Override
	public Collection<?> doGetAffectedObjects() {
		return affectedObjects;
	}

	@Override
	public Collection<?> doGetAffectedResources(Object type) {
		if (IModel.class.equals(type)
				&& (owner != null || ownerList != null || collection != null)) {
			Collection<Object> affected = new HashSet<Object>(super
					.doGetAffectedResources(type));
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

			if (collection != null) {
				for (Object element : collection) {
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

	@Override
	protected CommandResult doRedoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		CommandResult result = super.doRedoWithResult(progressMonitor, info);

		// We'd like the collection of things added to be selected after this
		// command completes.
		affectedObjects = collection;

		return result;
	}

	@Override
	protected CommandResult doUndoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		CommandResult result = super.doUndoWithResult(progressMonitor, info);

		// We'd like the owner selected after this undo completes.
		affectedObjects = owner == null ? Collections.EMPTY_SET : Collections
				.singleton(owner);

		return result;
	}

	/**
	 * This returns the collection of objects being added.
	 */
	public Collection<?> getCollection() {
		return collection;
	}

	/**
	 * This returns the position at which the objects will be added.
	 */
	public int getIndex() {
		return index;
	}

	protected Class getListType() {
		if (ownerList == null) {
			IProperty property = (IProperty) owner.getModel().resolve(
					this.property);

			for (Class clazz : property.getNamedRanges(owner, true)) {
				if (clazz.getURI() != null) {
					return clazz;
				}
			}
		}

		return null;
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
	 * This returns the list to which the command will add.
	 */
	public Collection<?> getOwnerList() {
		return ownerList;
	}

	/**
	 * This returns the feature of the owner object upon the command will act.
	 * It could be null, in the case that we are dealing with an
	 * {@link net.enilink.komma.rmf.common.util.EList}.
	 */
	public IReference getProperty() {
		return property;
	}

	@Override
	protected boolean prepare() {
		// If there is no list to add to, no collection or an empty collection
		// from which to add, or the index is out of range...
		if (ownerList == null && getListType() == null || collection == null
				|| collection.size() == 0 || index != CommandParameter.NO_INDEX
				&& (// ownerList != null && !(ownerList instanceof List<?>) ||
				index < 0 || ownerList != null && index > ownerList.size())) {
			return false;
		}

		if (property != null) {
			IProperty property = (IProperty) owner.getModel().resolve(
					this.property);
			// Check each object...
			for (Object object : collection) {
				boolean containment = false;

				// Check type of object.
				//
				// if (!property..isInstance(object)) {
				// return false;
				// }

				// Check that the object isn't already in a unique list.
				//
				// if (property.isUnique() && ownerList.contains(object)) {
				// return false;
				// }

				// Check to see if a container is being put into a contained
				// object.
				containment |= property.isContainment();
				if (containment) {
					for (IObject container = owner; container != null; container = container
							.getContainer()) {
						if (object.equals(container)) {
							return false;
						}
					}
				}
			}
		}

		if (owner != null && getDomain().isReadOnly(owner.getModel())) {
			return false;
		}

		return true;
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
		result.append(" (ownerList: " + ownerList + ")");
		result.append(" (collection: " + collection + ")");
		result.append(" (index: " + index + ")");
		result.append(" (affectedObjects:" + affectedObjects + ")");

		return result.toString();
	}
}
