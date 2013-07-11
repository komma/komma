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
 * $Id: RemoveCommand.java,v 1.10 2008/05/25 17:18:39 emerks Exp $
 */
package net.enilink.komma.edit.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IReference;

/**
 * The remove command logically acts upon an owner object that has a
 * collection-type feature from which objects can be removed. The static create
 * methods delegate command creation to {@link IEditingDomain#createCommand
 * EditingDomain.createCommand}, which may or may not result in the actual
 * creation of an instance of this class.
 * 
 * <p>
 * The implementation of this class is low-level and KOMA specific; it allows
 * one or more objects to be removed from a many-valued feature of an owner.
 * i.e., it is almost equivalent of the call
 * 
 * <pre>
 * ((EList) ((EObject) owner).eGet((EStructuralFeature) feature))
 * 		.removeAll((Collection) collection);
 * </pre>
 * 
 * <p>
 * It can also be used as a near-equivalent to the call
 * 
 * <pre>
 * ((EList) extent).removeAll((Collection) collection);
 * </pre>
 * 
 * which is how root objects are removed from the contents of a resource.
 * 
 * <p>
 * The one difference is that, while <code>EList.removeAll(Collection)</code>
 * removes all values equal to a value in the collection, this command will
 * remove no more than one value per value in the collection. When duplicates
 * are allowed and present in the list, this command will first look for
 * identical (<code>==</code>) values, in order, and failing that, equal values
 * (<code>.equals()</code>).
 * 
 * <p>
 * Like all the low-level commands in this package, the remove command is
 * undoable.
 * 
 * <p>
 * A remove command is an {@link IOverrideableCommand}.
 */
public class RemoveCommand extends AbstractOverrideableCommand {
	/**
	 * This creates a command to remove an object.
	 */
	public static ICommand create(IEditingDomain domain, Object value) {
		return create(domain, Collections.singleton(value));
	}

	/**
	 * This creates a command to remove a particular value from the specified
	 * feature of the owner.
	 */
	public static ICommand create(IEditingDomain domain, Object owner,
			Object feature, Object value) {
		return create(domain, owner, feature, Collections.singleton(value));
	}

	/**
	 * This creates a command to remove multiple objects.
	 */
	public static ICommand create(final IEditingDomain domain,
			final Collection<?> collection) {
		return create(domain, null, null, collection);
	}

	/**
	 * This creates a command to remove a collection of values from the
	 * specified feature of the owner.
	 */
	public static ICommand create(final IEditingDomain domain,
			final Object owner, final Object feature,
			final Collection<?> collection) {
		return domain.createCommand(RemoveCommand.class, new CommandParameter(
				owner, feature, collection));
	}

	/**
	 * This caches the label.
	 */
	protected static final String LABEL = KommaEditPlugin.INSTANCE
			.getString("_UI_RemoveCommand_label");

	/**
	 * This caches the description.
	 */
	protected static final String DESCRIPTION = KommaEditPlugin.INSTANCE
			.getString("_UI_RemoveCommand_description");

	/**
	 * This caches the description for a list-based command.
	 */
	protected static final String DESCRIPTION_FOR_LIST = KommaEditPlugin.INSTANCE
			.getString("_UI_RemoveCommand_description_for_list");

	/**
	 * This is the owner object upon which the command will act. It could be
	 * null, in the case that we are dealing with an
	 * {@link net.enilink.komma.rmf.common.util.EList}.
	 */
	protected IResource owner;

	/**
	 * This is the feature of the owner object upon the command will act. It
	 * could be null, in the case that we are dealing with an
	 * {@link net.enilink.komma.rmf.common.util.EList}.
	 */
	protected IReference property;

	/**
	 * This is the list from which the command will remove.
	 */
	protected Collection<Object> ownerList;

	/**
	 * This is the collection of objects being removed.
	 */
	protected Collection<Object> collection;

	/**
	 * These are the indices at which to reinsert the removed objects during an
	 * undo so as to achieve the original list order.
	 */
	protected int[] indices;

	/**
	 * The is the value returned by {@link ICommand#getAffectedObjects}. The
	 * affected objects are different after an execute than after an undo, so we
	 * record it.
	 */
	protected Collection<?> affectedObjects;

	/**
	 * This constructs a primitive command to remove a particular value from the
	 * specified feature of the owner.
	 */
	public RemoveCommand(IEditingDomain domain, IResource owner,
			IReference property, Object value) {
		this(domain, owner, property, Collections.singleton(value));
	}

	/**
	 * This constructs a primitive command to remove a collection of values from
	 * the specified feature of the owner.
	 */
	public RemoveCommand(IEditingDomain domain, IResource owner,
			IReference property, Collection<?> collection) {
		super(domain, LABEL, DESCRIPTION);

		// Initialize all the fields from the command parameter.
		//
		this.owner = owner;
		this.property = property;
		this.collection = collection == null ? null : new ArrayList<Object>(
				collection);

		ownerList = getOwnerList(this.owner, property);
	}

	/**
	 * This constructs a primitive command to remove a particular value from the
	 * specified extent.
	 */
	public RemoveCommand(IEditingDomain domain, Collection<?> list, Object value) {
		this(domain, list, Collections.singleton(value));
	}

	/**
	 * This constructs a primitive command to remove a collection of values from
	 * the specified extent.
	 */
	public RemoveCommand(IEditingDomain domain, Collection<?> list,
			Collection<?> collection) {
		super(domain, LABEL, DESCRIPTION_FOR_LIST);

		// Initialize all the fields from the command parameter.
		//
		this.collection = collection == null ? null : new ArrayList<Object>(
				collection);

		@SuppressWarnings("unchecked")
		Collection<Object> untypedList = (Collection<Object>) list;
		ownerList = untypedList;
	}

	/**
	 * This returns the owner object upon which the command will act. It could
	 * be null, in the case that we are dealing with an
	 * {@link net.enilink.komma.rmf.common.util.EList}.
	 */
	public IResource getOwner() {
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
	 * This returns the list from which the command will remove.
	 */
	public Collection<Object> getOwnerList() {
		return ownerList;
	}

	/**
	 * This returns the collection of objects being removed.
	 */
	public Collection<?> getCollection() {
		return collection;
	}

	/**
	 * These returns the indices at which to reinsert the removed objects during
	 * an undo so as to achieve the original list order.
	 */
	public int[] getIndices() {
		return indices;
	}

	@Override
	protected boolean prepare() {
		// This can execute if there is an owner list and a collection and the
		// owner list contains all the objects of the collection.
		//
		boolean result = ((ownerList != null && collection != null && ownerList
				.containsAll(collection)) ||
		// allow removal of properties with cardinality 1
				(ownerList == null && owner != null && collection.size() == 1 && owner
						.hasProperty(property, collection.iterator().next(),
								false)))
				&& (owner == null || !getDomain().isReadOnly(owner));

		return result;
	}

	@Override
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		boolean transactionWasActive = true;
		if (owner != null) {
			transactionWasActive = owner.getEntityManager().getTransaction()
					.isActive();
			if (!transactionWasActive) {
				owner.getEntityManager().getTransaction().begin();
			}
		}
		try {
			if (ownerList != null) {
				if (ownerList instanceof List<?>) {
					// if ownerList is an ordered list then
					// determine positions of the elements to remove
					indices = new int[collection.size()];
					int i = 0;
					for (Object obj : collection) {
						indices[i++] = ((List<?>) ownerList).indexOf(obj);
					}
				}
				ownerList.removeAll(collection);
			} else {
				owner.removeProperty(property, collection.iterator().next());
			}

			// We'd like the owner selected after this remove completes.
			affectedObjects = owner == null ? Collections.EMPTY_SET
					: Collections.singleton(owner);

			if (!transactionWasActive) {
				owner.getEntityManager().getTransaction().commit();
			}
		} catch (Throwable e) {
			if (!transactionWasActive) {
				owner.getEntityManager().getTransaction().rollback();
			}
			throw new ExecutionException("Error while removing element", e);
		}

		return CommandResult.newOKCommandResult(collection);
	}

	@Override
	protected CommandResult doUndoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		CommandResult result = super.doUndoWithResult(progressMonitor, info);

		// We'd like the collection of things added to be selected after this
		// command completes.
		affectedObjects = collection;

		return result;
	}

	@Override
	protected CommandResult doRedoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		CommandResult result = super.doRedoWithResult(progressMonitor, info);

		// We'd like the owner selected after this remove completes.
		//
		affectedObjects = owner == null ? Collections.EMPTY_SET : Collections
				.singleton(owner);

		return result;
	}

	@Override
	public Collection<?> doGetAffectedResources(Object type) {
		if (IModel.class.equals(type)
				&& (owner != null || ownerList != null || collection != null)) {
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
	public Collection<?> doGetAffectedObjects() {
		return affectedObjects;
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
		result.append(" (indices: " + Arrays.toString(indices) + ")");
		result.append(" (affectedObjects: " + affectedObjects + ")");

		return result.toString();
	}
}
