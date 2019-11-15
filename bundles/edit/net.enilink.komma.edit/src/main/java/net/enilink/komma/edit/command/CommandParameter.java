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
 * $Id: CommandParameter.java,v 1.4 2008/05/07 19:08:46 emerks Exp $
 */
package net.enilink.komma.edit.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.ObjectProperty;
import net.enilink.komma.core.IReference;
import net.enilink.komma.em.concepts.IResource;

/**
 * This is a convenient common base class for all the command parameters need by
 * the various types of commands. It provides particular support for the
 * encodings need by the basic KOMMA-based command implementations.
 */
public class CommandParameter {
	/**
	 * This value is used to indicate that the optional positional index
	 * indicator is unspecified.
	 */
	public static final int NO_INDEX = -1;

	/**
	 * This is the object that is the target or subject of the command.
	 */
	public Object owner;

	/**
	 * This is the aspect of the owner that will be affected.
	 */
	public Object property;

	/**
	 * This is the collection of values involved in the command.
	 */
	public Collection<?> collection;

	/**
	 * This is the single value involved in the command.
	 */
	public Object value;

	/**
	 * This the index (usually the position indicator) of the command.
	 */
	public int index;

	/**
	 * This creates an instance specifying only an owner.
	 */
	public CommandParameter(Object owner) {
		this.owner = owner;
	}

	/**
	 * This creates an instance specifying an owner, a property, and a value.
	 */
	public CommandParameter(Object owner, Object property, Object value) {
		this.owner = owner;
		this.property = property;
		this.value = value;
		this.index = NO_INDEX;
	}

	/**
	 * This creates an instance specifying an owner, a property, a value, and an
	 * index.
	 */
	public CommandParameter(Object owner, Object property, Object value,
			int index) {
		this.owner = owner;
		this.property = property;
		this.value = value;
		this.index = index;
	}

	/**
	 * This creates an instance specifying an owner, a property, and a
	 * collection of values.
	 */
	public CommandParameter(Object owner, Object property,
			Collection<?> collection) {
		this.owner = owner;
		this.property = property;
		this.collection = collection;
		this.index = NO_INDEX;
	}

	/**
	 * This creates an instance specifying an owner, a property, a collection of
	 * values, and an index.
	 */
	public CommandParameter(Object owner, Object property,
			Collection<?> collection, int index) {
		this.owner = owner;
		this.property = property;
		this.collection = collection;
		this.index = index;
	}

	/**
	 * This creates an instance specifying an owner, a property, and a value,
	 * and a collection.
	 */
	public CommandParameter(Object owner, Object property, Object value,
			Collection<?> collection) {
		this.owner = owner;
		this.property = property;
		this.value = value;
		this.collection = collection;
		this.index = NO_INDEX;
	}

	/**
	 * This creates an instance specifying an owner, a property, a value, a
	 * collection, and an index.
	 */
	public CommandParameter(Object owner, Object property, Object value,
			Collection<?> collection, int index) {
		this.owner = owner;
		this.property = property;
		this.value = value;
		this.collection = collection;
		this.index = index;
	}

	/**
	 * This returns the specified owner.
	 */
	public Object getOwner() {
		return owner;
	}

	/**
	 * This returns the specified owner as {@link IResource}
	 */
	public IResource getOwnerResource() {
		return owner instanceof IResource ? (IResource) owner : null;
	}

	/**
	 * This sets the owner to the specified value.
	 */
	public void setOwner(Object owner) {
		this.owner = owner;
	}

	/**
	 * This returns the specified property.
	 */
	public Object getProperty() {
		return property;
	}

	/**
	 * This returns the specified property as a {@link ObjectProperty}, if it is
	 * one.
	 */
	public ObjectProperty getObjectProperty() {
		if (property instanceof ObjectProperty) {
			return (ObjectProperty) property;
		}
		if (!(property instanceof IResource) && getOwnerResource() != null
				&& property instanceof IReference) {
			property = getOwnerResource().getEntityManager().find(
					(IReference) property);

		}
		return property instanceof ObjectProperty ? (ObjectProperty) property
				: null;
	}

	/**
	 * This returns the specified property as a {@link DatatypeProperty}, if it
	 * is one.
	 */
	public DatatypeProperty getDatatypeProperty() {
		if (property instanceof DatatypeProperty) {
			return (DatatypeProperty) property;
		}
		if (!(property instanceof IResource) && getOwnerResource() != null
				&& property instanceof IReference) {
			property = getOwnerResource().getEntityManager().find(
					(IReference) property);

		}
		return property instanceof DatatypeProperty ? (DatatypeProperty) property
				: null;
	}

	/**
	 * This returns the specified collection.
	 */
	public Collection<?> getCollection() {
		return collection;
	}

	/**
	 * This returns the specified collection as a list. If the collection isn't
	 * a list, a new copy is created.
	 */
	public List<?> getList() {
		return collection == null ? null
				: collection instanceof List ? (List<?>) collection
						: new ArrayList<Object>(collection);
	}

	/**
	 * This returns the specified value.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * This returns the specified index.
	 */
	public int getIndex() {
		return index;
	}

	public static String collectionToString(Collection<?> collection) {
		if (collection == null) {
			return "null";
		} else {
			StringBuffer result = new StringBuffer();

			result.append("{ ");

			for (Iterator<?> objects = collection.iterator(); objects.hasNext();) {
				result.append(objects.next());
				if (objects.hasNext()) {
					result.append(", ");
				}
			}

			result.append(" }");

			return result.toString();
		}
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();

		result.append("CommandParameter");

		result.append("\n  owner        = ");
		result.append(owner);

		result.append("\n  property      = ");
		result.append(property);

		if (collection != null) {
			result.append("\n  collection   = ");
			result.append(collectionToString(collection));
		}

		if (value != null) {
			result.append("\n  value        = ");
			result.append(value);
		}

		if (index != NO_INDEX) {
			result.append("\n  index        = ");
			result.append(index);
		}

		return result.toString();
	}
}
