/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
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
 * $Id: ReplaceCommand.java,v 1.8 2008/05/07 19:08:46 emerks Exp $
 */
package net.enilink.komma.edit.command;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.vocab.rdf.List;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IReference;

/**
 * The replace command logically acts upon an owner object that has a
 * collection-type feature in which an object can be replaced by a collection of
 * other objects. The static create methods delegate command creation to
 * {@link IEditingDomain#createCommand EditingDomain.createCommand}, which may
 * or may not result in the actual creation of an instance of this class.
 * 
 * <p>
 * The implementation of this class is low-level and EMF specific; it allows an
 * object from a many-valued feature of an owner to be replaced by a collection
 * of other objects. i.e., it is equivalent of the call
 * 
 * <pre>
 * int index = ((EList) ((EObject) owner).eGet((EStructuralFeature) feature))
 * 		.indexOf(value);
 * ((EList) ((EObject) owner).eGet((EStructuralFeature) feature)).remove(value);
 * ((EList) ((EObject) owner).eGet((EStructuralFeature) feature)).addAll(index,
 * 		(Collection) collection);
 * </pre>
 * 
 * <p>
 * It can also be used as an equivalent to the call
 * 
 * <pre>
 * int index = ((EList) extent).indexOf(value);
 * ((EList) extent).remove(value);
 * ((EList) extent).addAll(index, (Collection) collection);
 * </pre>
 * 
 * which is how root objects are replaced in the contents of a resource. Like
 * all the low level commands in this package, the replace command is undoable.
 * 
 * <p>
 * A replace command is an {@link IOverrideableCommand}.
 */
public class ReplaceCommand extends AbstractOverrideableCommand {
	/**
	 * This creates a command to replace an object with a collection of
	 * replacements.
	 */
	public static ICommand create(IEditingDomain domain, Object value,
			Collection<?> collection) {
		return create(domain, null, null, value, collection);
	}

	/**
	 * This creates a command to replace a particular value in the specified
	 * feature of the owner with a collection replacements objects.
	 */
	public static ICommand create(IEditingDomain domain, Object owner,
			Object feature, Object value, Collection<?> collection) {
		return domain.createCommand(ReplaceCommand.class, new CommandParameter(
				owner, feature, value, collection));
	}

	/**
	 * This caches the label.
	 */
	protected static final String LABEL = KommaEditPlugin.INSTANCE
			.getString("_UI_ReplaceCommand_label");

	/**
	 * This caches the description.
	 */
	protected static final String DESCRIPTION = KommaEditPlugin.INSTANCE
			.getString("_UI_ReplaceCommand_description");

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
	 * This is the list from which the command will replace.
	 */
	protected Collection<Object> ownerList;

	/**
	 * This is value that is being replaced.
	 */
	protected Object value;

	/**
	 * This is the collection of replacements.
	 */
	protected Collection<?> collection;

	/**
	 * The is the value returned by {@link ICommand#getAffectedObjects}. The
	 * affected objects are different after an execute than after an undo, so we
	 * record it.
	 */
	protected Collection<?> affectedObjects;

	/**
	 * This constructs a primitive command to replace a particular value in the
	 * specified feature of the owner with the specified replacement.
	 */
	public ReplaceCommand(IEditingDomain domain, IResource owner,
			IReference feature, Object value, Object replacement) {
		this(domain, owner, feature, value, Collections.singleton(replacement));
	}

	/**
	 * This constructs a primitive command to replace a particular value in the
	 * specified feature of the owner with the specified collection of
	 * replacements.
	 */
	public ReplaceCommand(IEditingDomain domain, IResource owner,
			IReference feature, Object value, Collection<?> collection) {
		super(domain, LABEL, DESCRIPTION);

		// Initialize all the fields from the command parameter.
		//
		this.owner = owner;
		this.property = feature;
		this.value = value;
		this.collection = collection;

		ownerList = getOwnerList(this.owner, feature);
	}

	/**
	 * This constructs a primitive command to replace a particular value in the
	 * specified extent with the given replacement.
	 */
	public ReplaceCommand(IEditingDomain domain, Collection<?> list,
			Object value, Object replacement) {
		this(domain, list, value, Collections.singleton(replacement));
	}

	/**
	 * This constructs a primitive command to replace a particular value in the
	 * specified extent with the given collection of replacements.
	 */
	public ReplaceCommand(IEditingDomain domain, Collection<?> list,
			Object value, Collection<?> collection) {
		super(domain, LABEL, DESCRIPTION);

		// Initialize all the fields from the command parameter.
		//
		this.value = value;
		this.collection = collection;

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
	 * This returns the list in which the command will replace.
	 */
	public Collection<Object> getOwnerList() {
		return ownerList;
	}

	/**
	 * This returns the value being replaced.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * This returns the collection of replacement objects.
	 */
	public Collection<?> getCollection() {
		return collection;
	}

	@Override
	protected boolean prepare() {
		// This can't execute if there is no owner list
		// or the owner list doesn't contain the value being replaced or
		// there are not replacements.
		//
		if (ownerList == null || !ownerList.contains(value)
				|| collection == null || collection.isEmpty()) {
			return false;
		} else if (owner != null && getDomain().isReadOnly(owner)) {
			return false;
		} else if (property == null) {
			// An extent allows anything to be added.
			//
			return true;
		} else {

			if (owner != null && property != null) {
				IProperty property = (IProperty) owner.getEntityManager().find(
						this.property);
				// Make sure each object conforms to the range of the property.
				for (Object replacement : collection) {
					if (!property.isRangeCompatible(replacement)) {
						return false;
					}
				}
			}

			return true;
		}
	}

	@Override
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		// Record the position of the value in the owner list.
		int index = -1;
		if (ownerList instanceof List<?>) {
			index = ((List<?>) ownerList).indexOf(value);
		}

		// Simply remove the object from the owner list.
		//
		ownerList.remove(value);

		// Insert the collection at the right place.
		if (index != -1 && ownerList instanceof List<?>) {
			((List<Object>) ownerList).addAll(index, collection);
		} else {
			ownerList.addAll(collection);
		}

		// We'd like the collection of replacements selected after this replace
		// completes.
		//
		affectedObjects = collection;

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
			Collection<Object> affected = new HashSet<Object>(
					super.doGetAffectedResources(type));
			if (value instanceof IObject) {
				affected.add(((IObject) value).getModel());
			}

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
		String result = super.toString() + " (owner: " + owner + ")" +
				" (property: " + property + ")" +
				" (ownerList: " + ownerList + ")" +
				" (value: " + value + ")" +
				" (collection: " + collection + ")" +
				" (affectedObjects:" + affectedObjects + ")";

		return result;
	}
}
