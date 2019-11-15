/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.common.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * A highly extensible list implementation.
 */
public class ExtensibleList<E> extends AbstractList<E> implements IList<E>,
		RandomAccess, Cloneable, Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The size of the list.
	 */
	protected int size;

	/**
	 * The underlying data storage of the list.
	 */
	protected transient Object[] data;

	/**
	 * Creates an empty instance with no initial capacity. The data storage will
	 * be null.
	 */
	public ExtensibleList() {
		super();
	}

	/**
	 * Creates an empty instance with the given capacity.
	 * 
	 * @param initialCapacity
	 *            the initial capacity of the list before it must grow.
	 * @exception IllegalArgumentException
	 *                if the <code>initialCapacity</code> is negative.
	 */
	public ExtensibleList(int initialCapacity) {
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("Illegal Capacity: "
					+ initialCapacity);
		}

		data = newData(initialCapacity);
	}

	/**
	 * Creates an instance that is a copy of the collection.
	 * 
	 * @param collection
	 *            the initial contents of the list.
	 */
	public ExtensibleList(Collection<? extends E> collection) {
		size = collection.size();

		// Conditionally create the data.
		//
		if (size > 0) {
			// Allow for a bit-shift of growth.
			//
			data = newData(size + size / 8 + 1);
			collection.toArray(data);
		}
	}

	/**
	 * Creates an initialized instance that directly uses the given arguments.
	 * 
	 * @param size
	 *            the size of the list.
	 * @param data
	 *            the underlying storage of the list.
	 */
	protected ExtensibleList(int size, Object[] data) {
		this.size = size;
		this.data = data;
	}

	/**
	 * Returns new allocated data storage. Clients may override this to create
	 * typed storage. The cost of type checking via a typed array is negligible.
	 * 
	 * @return new data storage.
	 */
	protected Object[] newData(int capacity) {
		return new Object[capacity];
	}

	/**
	 * Returns whether <code>equals</code> rather than <code>==</code> should be
	 * used to compare members. The default is to return <code>true</code> but
	 * clients can optimize performance by returning <code>false</code>. The
	 * performance difference is highly significant.
	 * 
	 * @return whether <code>equals</code> rather than <code>==</code> should be
	 *         used.
	 */
	protected boolean useEquals() {
		return true;
	}

	/**
	 * Returns whether two objects are equal using the {@link #useEquals
	 * appropriate} comparison mechanism.
	 * 
	 * @return whether two objects are equal.
	 */
	protected boolean equalObjects(Object firstObject, Object secondObject) {
		return useEquals() && firstObject != null ? firstObject
				.equals(secondObject) : firstObject == secondObject;
	}

	/**
	 * Returns whether <code>null</code> is a valid object for the list. The
	 * default is to return <code>true</code>, but clients can override this to
	 * exclude <code>null</code>.
	 * 
	 * @return whether <code>null</code> is a valid object for the list.
	 */
	protected boolean canContainNull() {
		return true;
	}

	/**
	 * Returns whether objects are constrained to appear at most once in the
	 * list. The default is to return <code>false</code>, but clients can
	 * override this to ensure uniqueness of contents. The performance impact is
	 * significant: operations such as <code>add</code> are O(n) as a result
	 * requiring uniqueness.
	 * 
	 * @return whether objects are constrained to appear at most once in the
	 *         list.
	 */
	protected boolean isUnique() {
		return false;
	}

	/**
	 * Validates a new content object and returns the validated object. This
	 * implementation checks for null, if {@link #canContainNull necessary} and
	 * returns the argument object. Clients may throw additional types of
	 * runtime exceptions in order to handle constraint violations.
	 * 
	 * @param index
	 *            the position of the new content.
	 * @param object
	 *            the new content.
	 * @return the validated content.
	 * @exception IllegalArgumentException
	 *                if a constraint prevents the object from being added.
	 */
	protected E validate(int index, E object) {
		if (!canContainNull() && object == null) {
			throw new IllegalArgumentException(
					"The 'no null' constraint is violated");
		}

		return object;
	}

	/**
	 * Assigns the object into the data storage at the given index and returns
	 * the object that's been stored. Clients can monitor access to the storage
	 * via this method.
	 * 
	 * @param index
	 *            the position of the new content.
	 * @param object
	 *            the new content.
	 * @return the object that's been stored.
	 * 
	 */
	protected E assign(int index, E object) {
		data[index] = object;
		return object;
	}

	/**
	 * Resolves the object at the index and returns the result. This
	 * implementation simply returns the <code>object</code>; clients can use
	 * this to transform objects as they are fetched.
	 * 
	 * @param index
	 *            the position of the content.
	 * @param object
	 *            the content.
	 * @return the resolved object.
	 */
	protected E resolve(int index, E object) {
		return object;
	}

	/**
	 * Called to indicate that the data storage has been set. This
	 * implementation does nothing; clients can use this to monitor settings to
	 * the data storage.
	 * 
	 * @param index
	 *            the position that was set.
	 * @param newObject
	 *            the new object at the position.
	 * @param oldObject
	 *            the old object at the position.
	 */
	protected void didSet(int index, E newObject, E oldObject) {
		// Do nothing.
	}

	/**
	 * Called to indicate that an object has been added to the data storage.
	 * This implementation does nothing; clients can use this to monitor
	 * additions to the data storage.
	 * 
	 * @param index
	 *            the position object the new object.
	 * @param newObject
	 *            the new object at the position.
	 */
	protected void didAdd(int index, E newObject) {
		// Do nothing.
	}

	/**
	 * Called to indicate that an object has been removed from the data storage.
	 * This implementation does nothing; clients can use this to monitor
	 * removals from the data storage.
	 * 
	 * @param index
	 *            the position of the old object.
	 * @param oldObject
	 *            the old object at the position.
	 */
	protected void didRemove(int index, E oldObject) {
		// Do nothing.
	}

	/**
	 * Called to indicate that the data storage has been cleared. This
	 * implementation calls {@link #didRemove didRemove} for each object;
	 * clients can use this to monitor clearing of the data storage.
	 * 
	 * @param size
	 *            the original size of the list.
	 * @param oldObjects
	 *            the old data storage being discarded.
	 * @see #didRemove
	 */
	protected void didClear(int size, Object[] oldObjects) {
		if (oldObjects != null) {
			for (int i = 0; i < size; ++i) {
				@SuppressWarnings("unchecked")
				E object = (E) oldObjects[i];
				didRemove(i, object);
			}
		}
	}

	/**
	 * Called to indicate that an object has been moved in the data storage.
	 * This implementation does nothing; clients can use this to monitor
	 * movement in the data storage.
	 * 
	 * @param index
	 *            the position of the moved object.
	 * @param movedObject
	 *            the moved object at the position.
	 * @param oldIndex
	 *            the position the object was at before the move.
	 */
	protected void didMove(int index, E movedObject, int oldIndex) {
		// Do nothing.
	}

	/**
	 * Called to indicate that the data storage has been changed. This
	 * implementation does nothing; clients can use this to monitor change in
	 * the data storage.
	 */
	protected void didChange() {
		// Do nothing.
	}

	/**
	 * Returns the number of objects in the list.
	 * 
	 * @return the number of objects in the list.
	 */
	@Override
	public int size() {
		return size;
	}

	/**
	 * Returns whether the list has zero size.
	 * 
	 * @return whether the list has zero size.
	 */
	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Returns whether the list contains the object. This implementation uses
	 * either <code>equals</code> or <code>"=="</code> depending on
	 * {@link #useEquals useEquals}.
	 * 
	 * @param object
	 *            the object in question.
	 * @return whether the list contains the object.
	 * @see #useEquals
	 */
	@Override
	public boolean contains(Object object) {
		if (useEquals() && object != null) {
			for (int i = 0; i < size; ++i) {
				if (object.equals(data[i])) {
					return true;
				}
			}
		} else {
			for (int i = 0; i < size; ++i) {
				if (data[i] == object) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Returns whether the list contains each object in the collection. This
	 * implementation delegates to {@link #contains contains}, which may use
	 * either <code>equals</code> or <code>"=="</code> depending on
	 * {@link #useEquals useEquals}.
	 * 
	 * @param collection
	 *            the collection of objects in question.
	 * @return whether the list contains each object in the collection.
	 * @see #contains
	 * @see #useEquals
	 */
	@Override
	public boolean containsAll(Collection<?> collection) {
		for (Object o : collection) {
			if (!contains(o)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns the position of the first occurrence of the object in the list.
	 * This implementation uses either <code>equals</code> or <code>"=="</code>
	 * depending on {@link #useEquals useEquals}.
	 * 
	 * @param object
	 *            the object in question.
	 * @return the position of the first occurrence of the object in the list.
	 */
	@Override
	public int indexOf(Object object) {
		if (useEquals() && object != null) {
			for (int i = 0; i < size; ++i) {
				if (object.equals(data[i])) {
					return i;
				}
			}
		} else {
			for (int i = 0; i < size; ++i) {
				if (data[i] == object) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Returns the position of the last occurrence of the object in the list.
	 * This implementation uses either <code>equals</code> or <code>"=="</code>
	 * depending on {@link #useEquals useEquals}.
	 * 
	 * @param object
	 *            the object in question.
	 * @return the position of the last occurrence of the object in the list.
	 */
	@Override
	public int lastIndexOf(Object object) {
		if (useEquals() && object != null) {
			for (int i = size - 1; i >= 0; --i) {
				if (object.equals(data[i])) {
					return i;
				}
			}
		} else {
			for (int i = size - 1; i >= 0; --i) {
				if (data[i] == object) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Returns an array containing all the objects in sequence. Clients may
	 * override {@link #newData newData} to create typed storage in this case.
	 * 
	 * @return an array containing all the objects in sequence.
	 * @see #newData
	 */
	@Override
	public Object[] toArray() {
		Object[] result = newData(size);

		// Guard for no data.
		//
		if (size > 0) {
			System.arraycopy(data, 0, result, 0, size);
		}
		return result;
	}

	/**
	 * Returns an array containing all the objects in sequence.
	 * 
	 * @param array
	 *            the array that will be filled and returned, if it's big
	 *            enough; otherwise, a suitably large array of the same type
	 *            will be allocated and used instead.
	 * @return an array containing all the objects in sequence.
	 * @see #newData
	 */
	@Override
	public <T> T[] toArray(T[] array) {
		// Guard for no data.
		//
		if (size > 0) {
			if (array.length < size) {
				@SuppressWarnings("unchecked")
				T[] newArray = (T[]) Array.newInstance(array.getClass()
						.getComponentType(), size);
				array = newArray;
			}

			System.arraycopy(data, 0, array, 0, size);
		}

		if (array.length > size) {
			array[size] = null;
		}

		return array;
	}

	/**
	 * Returns direct <b>unsafe</b> access to the underlying data storage.
	 * Clients may <b>not</b> modify this and may <b>not</b> assume that the
	 * array remains valid as the list is modified.
	 * 
	 * @return direct <b>unsafe</b> access to the underlying data storage.
	 */
	public Object[] data() {
		return data;
	}

	/**
	 * Updates directly and <b>unsafely</b> the underlying data storage. Clients
	 * <b>must</b> be aware that this subverts all callbacks and hence possibly
	 * the integrity of the list.
	 */
	public void setData(int size, Object[] data) {
		this.size = size;
		this.data = data;
		++modCount;
	}

	/**
	 * An IndexOutOfBoundsException that constructs a message from the argument
	 * data. Having this avoids having the byte code that computes the message
	 * repeated/inlined at the creation site.
	 */
	protected static class BasicIndexOutOfBoundsException extends
			IndexOutOfBoundsException {
		private static final long serialVersionUID = 1L;

		/**
		 * Constructs an instance with a message based on the arguments.
		 */
		public BasicIndexOutOfBoundsException(int index, int size) {
			super("index=" + index + ", size=" + size);
		}
	}

	/**
	 * Returns the object at the index. This implementation delegates to
	 * {@link #resolve resolve} so that clients may transform the fetched
	 * object.
	 * 
	 * @param index
	 *            the position in question.
	 * @return the object at the index.
	 * @exception IndexOutOfBoundsException
	 *                if the index isn't within the size range.
	 * @see #resolve
	 * @see #basicGet
	 */
	@SuppressWarnings("unchecked")
	@Override
	public E get(int index) {
		if (index >= size)
			throw new BasicIndexOutOfBoundsException(index, size);

		return resolve(index, (E) data[index]);
	}

	/**
	 * Returns the object at the index without {@link #resolve resolving} it.
	 * 
	 * @param index
	 *            the position in question.
	 * @return the object at the index.
	 * @exception IndexOutOfBoundsException
	 *                if the index isn't within the size range.
	 * @see #resolve
	 * @see #get
	 */
	@SuppressWarnings("unchecked")
	public E basicGet(int index) {
		if (index >= size)
			throw new BasicIndexOutOfBoundsException(index, size);

		return (E) data[index];
	}

	/**
	 * Sets the object at the index and returns the old object at the index.
	 * This implementation delegates to {@link #setUnique setUnique} after range
	 * checking and after {@link #isUnique uniqueness} checking.
	 * 
	 * @param index
	 *            the position in question.
	 * @param object
	 *            the object to set.
	 * @return the old object at the index.
	 * @exception IndexOutOfBoundsException
	 *                if the index isn't within the size range.
	 * @exception IllegalArgumentException
	 *                if there is a constraint violation, e.g., non-uniqueness.
	 * @see #setUnique
	 */
	@Override
	public E set(int index, E object) {
		if (index >= size)
			throw new BasicIndexOutOfBoundsException(index, size);

		if (isUnique()) {
			int currentIndex = indexOf(object);
			if (currentIndex >= 0 && currentIndex != index) {
				throw new IllegalArgumentException(
						"The 'no duplicates' constraint is violated");
			}
		}

		return setUnique(index, object);
	}

	/**
	 * Sets the object at the index and returns the old object at the index; it
	 * does no ranging checking or uniqueness checking. This implementation
	 * delegates to {@link #assign assign}, {@link #didSet didSet}, and
	 * {@link #didChange didChange}.
	 * 
	 * @param index
	 *            the position in question.
	 * @param object
	 *            the object to set.
	 * @return the old object at the index.
	 * @see #set
	 */
	public E setUnique(int index, E object) {
		@SuppressWarnings("unchecked")
		E oldObject = (E) data[index];
		assign(index, validate(index, object));
		didSet(index, object, oldObject);
		didChange();
		return oldObject;
	}

	/**
	 * Adds the object at the end of the list and returns whether the object was
	 * added; if {@link #isUnique uniqueness} is required, duplicates will be
	 * ignored and <code>false</code> will be returned. This implementation
	 * delegates to {@link #addUnique(Object) addUnique(E)} after uniqueness
	 * checking.
	 * 
	 * @param object
	 *            the object to be added.
	 * @return whether the object was added.
	 * @see #addUnique(Object)
	 */
	@Override
	public boolean add(E object) {
		if (isUnique() && contains(object)) {
			return false;
		} else {
			addUnique(object);
			return true;
		}
	}

	/**
	 * Adds the object at the end of the list; it does no uniqueness checking.
	 * This implementation delegates to {@link #assign assign}, {@link #didAdd
	 * didAdd}, and {@link #didChange didChange}. after uniqueness checking.
	 * 
	 * @param object
	 *            the object to be added.
	 * @see #add(Object)
	 */
	public void addUnique(E object) {
		// ++modCount
		//
		grow(size + 1);

		assign(size, validate(size, object));
		didAdd(size++, object);
		didChange();
	}

	/**
	 * Adds the object at the given index in the list. If {@link #isUnique
	 * uniqueness} is required, duplicates will be ignored. This implementation
	 * delegates to {@link #addUnique(int, Object) addUnique(int, E)} after
	 * uniqueness checking.
	 * 
	 * @param object
	 *            the object to be added.
	 * @exception IllegalArgumentException
	 *                if {@link #isUnique uniqueness} is required, and the
	 *                object is a duplicate.
	 * @see #addUnique(int, Object)
	 */
	@Override
	public void add(int index, E object) {
		if (index > size)
			throw new BasicIndexOutOfBoundsException(index, size);

		if (isUnique() && contains(object)) {
			throw new IllegalArgumentException(
					"The 'no duplicates' constraint is violated");
		}

		addUnique(index, object);
	}

	/**
	 * Adds the object at the given index in the list; it does no ranging
	 * checking or uniqueness checking. This implementation delegates to
	 * {@link #assign assign}, {@link #didAdd didAdd}, and {@link #didChange
	 * didChange}.
	 * 
	 * @param object
	 *            the object to be added.
	 * @see #add(int, Object)
	 */
	public void addUnique(int index, E object) {
		// ++modCount
		//
		grow(size + 1);

		E validatedObject = validate(index, object);
		if (index != size) {
			System.arraycopy(data, index, data, index + 1, size - index);
		}
		assign(index, validatedObject);
		++size;
		didAdd(index, object);
		didChange();
	}

	/**
	 * Adds each object of the collection to the end of the list. If
	 * {@link #isUnique uniqueness} is required, duplicates will be
	 * {@link #getNonDuplicates removed} from the collection, which could even
	 * result in an empty collection. This implementation delegates to
	 * {@link #addAllUnique(Collection) addAllUnique(Collection)} after
	 * uniqueness checking.
	 * 
	 * @param collection
	 *            the collection of objects to be added.
	 * @see #addAllUnique(Collection)
	 */
	@Override
	public boolean addAll(Collection<? extends E> collection) {
		if (isUnique()) {
			collection = getNonDuplicates(collection);
		}
		return addAllUnique(collection);
	}

	/**
	 * Adds each object of the collection to the end of the list; it does no
	 * uniqueness checking. This implementation delegates to {@link #assign
	 * assign}, {@link #didAdd didAdd}, and {@link #didChange didChange}.
	 * 
	 * @param collection
	 *            the collection of objects to be added.
	 * @see #addAll(Collection)
	 */
	public boolean addAllUnique(Collection<? extends E> collection) {
		int growth = collection.size();

		// ++modCount
		//
		grow(size + growth);

		Iterator<? extends E> objects = collection.iterator();
		int oldSize = size;
		size += growth;
		for (int i = oldSize; i < size; ++i) {
			E object = objects.next();
			assign(i, validate(i, object));
			didAdd(i, object);
			didChange();
		}

		return growth != 0;
	}

	/**
	 * Adds each object of the collection at each successive index in the list
	 * and returns whether any objects were added. If {@link #isUnique
	 * uniqueness} is required, duplicates will be {@link #getNonDuplicates
	 * removed} from the collection, which could even result in an empty
	 * collection. This implementation delegates to
	 * {@link #addAllUnique(int, Collection) addAllUnique(int, Collection)}
	 * after uniqueness checking.
	 * 
	 * @param index
	 *            the index at which to add.
	 * @param collection
	 *            the collection of objects to be added.
	 * @return whether any objects were added.
	 * @see #addAllUnique(int, Collection)
	 */
	@Override
	public boolean addAll(int index, Collection<? extends E> collection) {
		if (index > size)
			throw new BasicIndexOutOfBoundsException(index, size);

		if (isUnique()) {
			collection = getNonDuplicates(collection);
		}
		return addAllUnique(index, collection);
	}

	/**
	 * Adds each object of the collection at each successive index in the list
	 * and returns whether any objects were added; it does no ranging checking
	 * or uniqueness checking. This implementation delegates to {@link #assign
	 * assign}, {@link #didAdd didAdd}, and {@link #didChange didChange}.
	 * 
	 * @param index
	 *            the index at which to add.
	 * @param collection
	 *            the collection of objects to be added.
	 * @return whether any objects were added.
	 * @see #addAll(int, Collection)
	 */
	public boolean addAllUnique(int index, Collection<? extends E> collection) {
		int growth = collection.size();

		// ++modCount
		//
		grow(size + growth);

		int shifted = size - index;
		if (shifted > 0) {
			System.arraycopy(data, index, data, index + growth, shifted);
		}

		Iterator<? extends E> objects = collection.iterator();
		size += growth;
		for (int i = 0; i < growth; ++i) {
			E object = objects.next();
			assign(index, validate(index, object));
			didAdd(index, object);
			didChange();
			++index;
		}

		return growth != 0;
	}

	/**
	 * Adds each object from start to end of the array at the index of list and
	 * returns whether any objects were added; it does no ranging checking or
	 * uniqueness checking. This implementation delegates to {@link #assign
	 * assign}, {@link #didAdd didAdd}, and {@link #didChange didChange}.
	 * 
	 * @param objects
	 *            the objects to be added.
	 * @param start
	 *            the index of first object to be added.
	 * @param end
	 *            the index past the last object to be added.
	 * @return whether any objects were added.
	 * @see #addAllUnique(Object[], int, int)
	 */
	public boolean addAllUnique(Object[] objects, int start, int end) {
		int growth = end - start;

		// ++modCount
		//
		grow(size + growth);

		size += growth;
		int index = size;
		for (int i = start; i < end; ++i) {
			@SuppressWarnings("unchecked")
			E object = (E) objects[i];
			assign(index, validate(index, object));
			didAdd(index, object);
			didChange();
			++index;
		}

		return growth != 0;
	}

	/**
	 * Adds each object from start to end of the array at each successive index
	 * in the list and returns whether any objects were added; it does no
	 * ranging checking or uniqueness checking. This implementation delegates to
	 * {@link #assign assign}, {@link #didAdd didAdd}, and {@link #didChange
	 * didChange}.
	 * 
	 * @param index
	 *            the index at which to add.
	 * @param objects
	 *            the objects to be added.
	 * @param start
	 *            the index of first object to be added.
	 * @param end
	 *            the index past the last object to be added.
	 * @return whether any objects were added.
	 * @see #addAllUnique(Object[], int, int)
	 */
	public boolean addAllUnique(int index, Object[] objects, int start, int end) {
		int growth = end - start;

		// ++modCount
		//
		grow(size + growth);

		int shifted = size - index;
		if (shifted > 0) {
			System.arraycopy(data, index, data, index + growth, shifted);
		}

		size += growth;
		for (int i = start; i < end; ++i) {
			@SuppressWarnings("unchecked")
			E object = (E) objects[i];
			assign(index, validate(index, object));
			didAdd(index, object);
			didChange();
			++index;
		}

		return growth != 0;
	}

	/**
	 * Removes the object from the list and returns whether the object was
	 * actually contained by the list. This implementation uses {@link #indexOf
	 * indexOf} to find the object and delegates to {@link #remove(int)
	 * remove(int)} in the case that it finds the object.
	 * 
	 * @param object
	 *            the object to be removed.
	 * @return whether the object was actually contained by the list.
	 */
	@Override
	public boolean remove(Object object) {
		int index = indexOf(object);
		if (index >= 0) {
			remove(index);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Removes each object of the collection from the list and returns whether
	 * any object was actually contained by the list.
	 * 
	 * @param collection
	 *            the collection of objects to be removed.
	 * @return whether any object was actually contained by the list.
	 */
	@Override
	public boolean removeAll(Collection<?> collection) {
		boolean modified = false;
		for (int i = size; --i >= 0;) {
			if (collection.contains(data[i])) {
				remove(i);
				modified = true;
			}
		}

		return modified;
	}

	/**
	 * Removes the object at the index from the list and returns it. This
	 * implementation delegates to {@link #didRemove didRemove} and
	 * {@link #didChange didChange}.
	 * 
	 * @param index
	 *            the position of the object to remove.
	 * @return the removed object.
	 * @exception IndexOutOfBoundsException
	 *                if the index isn't within the size range.
	 */
	@Override
	public E remove(int index) {
		if (index >= size)
			throw new BasicIndexOutOfBoundsException(index, size);

		++modCount;
		@SuppressWarnings("unchecked")
		E oldObject = (E) data[index];

		int shifted = size - index - 1;
		if (shifted > 0) {
			System.arraycopy(data, index + 1, data, index, shifted);
		}

		// Don't hold onto a duplicate reference to the last object.
		//
		data[--size] = null;
		didRemove(index, oldObject);
		didChange();

		return oldObject;
	}

	/**
	 * Removes from the list each object not contained by the collection and
	 * returns whether any object was actually removed. This delegates to
	 * {@link #remove(int) remove(int)} in the case that it finds an object that
	 * isn't retained.
	 * 
	 * @param collection
	 *            the collection of objects to be retained.
	 * @return whether any object was actually removed.
	 */
	@Override
	public boolean retainAll(Collection<?> collection) {
		boolean modified = false;
		for (int i = size; --i >= 0;) {
			if (!collection.contains(data[i])) {
				remove(i);
				modified = true;
			}
		}
		return modified;
	}

	/**
	 * Clears the list of all objects. This implementation discards the data
	 * storage without modifying it and delegates to {@link #didClear didClear}
	 * and {@link #didChange didChange}.
	 */
	@Override
	public void clear() {
		++modCount;

		Object[] oldData = data;
		int oldSize = size;

		// Give it all back to the garbage collector.
		//
		data = null;
		size = 0;

		didClear(oldSize, oldData);
		didChange();
	}

	/**
	 * Moves the object to the index of the list. This implementation uses
	 * {@link #indexOf} of find the object and delegates to
	 * {@link #move(int, int) move(int, int)}.
	 * 
	 * @param index
	 *            the new position for the object in the list.
	 * @param object
	 *            the object to be moved.
	 * @exception IndexOutOfBoundsException
	 *                if the index isn't within the size range or the object
	 *                isn't contained by the list.
	 */
	public void move(int index, E object) {
		move(index, indexOf(object));
	}

	/**
	 * Moves the object at the source index of the list to the target index of
	 * the list and returns the moved object. This implementation delegates to
	 * {@link #assign assign}, {@link #didMove didMove}, and {@link #didChange
	 * didChange}.
	 * 
	 * @param targetIndex
	 *            the new position for the object in the list.
	 * @param sourceIndex
	 *            the old position of the object in the list.
	 * @return the moved object.
	 * @exception IndexOutOfBoundsException
	 *                if either index isn't within the size range.
	 */
	public E move(int targetIndex, int sourceIndex) {
		++modCount;
		if (targetIndex >= size)
			throw new IndexOutOfBoundsException("targetIndex=" + targetIndex
					+ ", size=" + size);

		if (sourceIndex >= size)
			throw new IndexOutOfBoundsException("sourceIndex=" + sourceIndex
					+ ", size=" + size);

		@SuppressWarnings("unchecked")
		E object = (E) data[sourceIndex];
		if (targetIndex != sourceIndex) {
			if (targetIndex < sourceIndex) {
				System.arraycopy(data, targetIndex, data, targetIndex + 1,
						sourceIndex - targetIndex);
			} else {
				System.arraycopy(data, sourceIndex + 1, data, sourceIndex,
						targetIndex - sourceIndex);
			}
			assign(targetIndex, object);
			didMove(targetIndex, object, sourceIndex);
			didChange();
		}
		return object;
	}

	/**
	 * Shrinks the capacity of the list to the minimal requirements.
	 * 
	 * @see #grow
	 */
	public void shrink() {
		++modCount;

		// Conditionally create the data.
		//
		if (size == 0) {
			// Give it all back to the garbage collector.
			//
			data = null;
		} else if (size < data.length) {
			Object[] oldData = data;
			data = newData(size);
			System.arraycopy(oldData, 0, data, 0, size);
		}
	}

	/**
	 * Grows the capacity of the list to ensure that no additional growth is
	 * needed until the size exceeds the specified minimum capacity.
	 * 
	 * @see #shrink
	 */
	public void grow(int minimumCapacity) {
		++modCount;
		int oldCapacity = data == null ? 0 : data.length;
		if (minimumCapacity > oldCapacity) {
			Object oldData[] = data;

			// This seems to be a pretty sweet formula that supports good
			// growth.
			// Adding an object to a list will create a list of capacity 4,
			// which is just about the average list size.
			//
			int newCapacity = oldCapacity + oldCapacity / 2 + 4;
			if (newCapacity < minimumCapacity) {
				newCapacity = minimumCapacity;
			}
			data = newData(newCapacity);
			if (oldData != null) {
				System.arraycopy(oldData, 0, data, 0, size);
			}
		}
	}

	private synchronized void writeObject(ObjectOutputStream objectOutputStream)
			throws IOException {
		objectOutputStream.defaultWriteObject();
		if (data == null) {
			objectOutputStream.writeInt(0);
		} else {
			objectOutputStream.writeInt(data.length);
			for (int i = 0; i < size; ++i) {
				objectOutputStream.writeObject(data[i]);
			}
		}
	}

	private synchronized void readObject(ObjectInputStream objectInputStream)
			throws IOException, ClassNotFoundException {
		objectInputStream.defaultReadObject();
		int arrayLength = objectInputStream.readInt();
		if (arrayLength > 0) {
			try {
				data = newData(arrayLength);
			} catch (Throwable exception) {
				data = new Object[arrayLength];
			}

			for (int i = 0; i < size; ++i) {
				@SuppressWarnings("unchecked")
				E object = (E) objectInputStream.readObject();
				didAdd(i, assign(i, object));
			}
		}
	}

	/**
	 * Returns a shallow copy of this list.
	 * 
	 * @return a shallow copy of this list.
	 */
	@Override
	public Object clone() {
		try {
			@SuppressWarnings("unchecked")
			ExtensibleList<E> clone = (ExtensibleList<E>) super.clone();
			if (size > 0) {
				clone.size = size;
				clone.data = newData(size);
				System.arraycopy(data, 0, clone.data, 0, size);
			}
			return clone;
		} catch (CloneNotSupportedException exception) {
			throw new InternalError();
		}
	}

	/**
	 * Returns whether the object is a list with corresponding equal objects.
	 * This implementation uses either <code>equals</code> or <code>"=="</code>
	 * depending on {@link #useEquals useEquals}.
	 * 
	 * @return whether the object is a list with corresponding equal objects.
	 * @see #useEquals
	 */
	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}

		if (!(object instanceof List)) {
			return false;
		}

		List<?> list = (List<?>) object;
		if (list.size() != size) {
			return false;
		}

		Iterator<?> objects = list.iterator();
		if (useEquals()) {
			for (int i = 0; i < size; ++i) {
				Object o1 = data[i];
				Object o2 = objects.next();
				if (o1 == null ? o2 != null : !o1.equals(o2)) {
					return false;
				}
			}
		} else {
			for (int i = 0; i < size; ++i) {
				Object o1 = data[i];
				Object o2 = objects.next();
				if (o1 != o2) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Returns a hash code computed from each object's hash code.
	 * 
	 * @return a hash code.
	 */
	@Override
	public int hashCode() {
		int hashCode = 1;
		for (int i = 0; i < size; ++i) {
			Object object = data[i];
			hashCode = 31 * hashCode + (object == null ? 0 : object.hashCode());
		}
		return hashCode;
	}

	/**
	 * Returns a string of the form <code>"[object1, object2]"</code>.
	 * 
	 * @return a string of the form <code>"[object1, object2]"</code>.
	 */
	@Override
	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("[");
		for (int i = 0; i < size;) {
			stringBuffer.append(String.valueOf(data[i]));
			if (++i < size) {
				stringBuffer.append(", ");
			}
		}
		stringBuffer.append("]");
		return stringBuffer.toString();
	}

	/**
	 * Returns an iterator. This implementation allocates a
	 * {@link ExtensibleList.EIterator}.
	 * 
	 * @return an iterator.
	 * @see ExtensibleList.EIterator
	 */
	@Override
	public Iterator<E> iterator() {
		return new EIterator<E>();
	}

	/**
	 * An extensible iterator implementation.
	 */
	protected class EIterator<E1> implements Iterator<E1> {
		/**
		 * The current position of the iterator.
		 */
		protected int cursor = 0;

		/**
		 * The previous position of the iterator.
		 */
		protected int lastCursor = -1;

		/**
		 * The modification count of the containing list.
		 */
		protected int expectedModCount = modCount;

		/**
		 * Returns whether there are more objects.
		 * 
		 * @return whether there are more objects.
		 */
		public boolean hasNext() {
			return cursor != size();
		}

		/**
		 * Returns the next object and advances the iterator. This
		 * implementation delegates to {@link #doNext doNext}.
		 * 
		 * @return the next object.
		 * @exception NoSuchElementException
		 *                if the iterator is done.
		 */
		@SuppressWarnings("unchecked")
		public E1 next() {
			return (E1) doNext();
		}

		/**
		 * Returns the next object and advances the iterator. This
		 * implementation delegates to {@link ExtensibleList#get get}.
		 * 
		 * @return the next object.
		 * @exception NoSuchElementException
		 *                if the iterator is done.
		 */
		protected E doNext() {
			try {
				E next = ExtensibleList.this.get(cursor);
				checkModCount();
				lastCursor = cursor++;
				return next;
			} catch (IndexOutOfBoundsException exception) {
				checkModCount();
				throw new NoSuchElementException();
			}
		}

		/**
		 * Removes the last object returned by {@link #next()} from the list,
		 * it's an optional operation. This implementation can also function in
		 * a list iterator to act upon on the object returned by calling
		 * <code>previous</code>.
		 * 
		 * @exception IllegalStateException
		 *                if <code>next</code> has not yet been called, or
		 *                <code>remove</code> has already been called after the
		 *                last call to <code>next</code>.
		 */
		public void remove() {
			if (lastCursor == -1) {
				throw new IllegalStateException();
			}
			checkModCount();

			try {
				ExtensibleList.this.remove(lastCursor);
				expectedModCount = modCount;
				if (lastCursor < cursor) {
					--cursor;
				}
				lastCursor = -1;
			} catch (IndexOutOfBoundsException exception) {
				throw new ConcurrentModificationException();
			}
		}

		/**
		 * Checks that the modification count is as expected.
		 * 
		 * @exception ConcurrentModificationException
		 *                if the modification count is not as expected.
		 */
		protected void checkModCount() {
			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
		}
	}

	/**
	 * Returns a read-only iterator that does not {@link #resolve resolve}
	 * objects. This implementation allocates a {@link NonResolvingEIterator}.
	 * 
	 * @return a read-only iterator that does not resolve objects.
	 */
	protected Iterator<E> basicIterator() {
		return new NonResolvingEIterator<E>();
	}

	/**
	 * An extended read-only iterator that does not
	 * {@link ExtensibleList#resolve resolve} objects.
	 */
	protected class NonResolvingEIterator<E1> extends EIterator<E1> {
		/**
		 * Returns the next object and advances the iterator. This
		 * implementation accesses the data storage directly.
		 * 
		 * @return the next object.
		 * @exception NoSuchElementException
		 *                if the iterator is done.
		 */
		@Override
		protected E doNext() {
			try {
				@SuppressWarnings("unchecked")
				E next = (E) ExtensibleList.this.data[cursor];
				checkModCount();
				lastCursor = cursor++;
				return next;
			} catch (IndexOutOfBoundsException exception) {
				checkModCount();
				throw new NoSuchElementException();
			}
		}

		/**
		 * Throws and exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Returns a list iterator. This implementation allocates a
	 * {@link ExtensibleList.EListIterator}.
	 * 
	 * @return a list iterator.
	 * @see ExtensibleList.EListIterator
	 */
	@Override
	public ListIterator<E> listIterator() {
		return new EListIterator<E>();
	}

	/**
	 * Returns a list iterator advanced to the given index. This implementation
	 * allocates a {@link ExtensibleList.EListIterator}.
	 * 
	 * @param index
	 *            the starting index.
	 * @return a list iterator advanced to the index.
	 * @see ExtensibleList.EListIterator
	 * @exception IndexOutOfBoundsException
	 *                if the index isn't within the size range.
	 */
	@Override
	public ListIterator<E> listIterator(int index) {
		if (index < 0 || index > size())
			throw new BasicIndexOutOfBoundsException(index, size);

		return new EListIterator<E>(index);
	}

	/**
	 * An extensible list iterator implementation.
	 */
	protected class EListIterator<E1> extends EIterator<E1> implements
			ListIterator<E1> {
		/**
		 * Creates an instance.
		 */
		public EListIterator() {
			super();
		}

		/**
		 * Creates an instance advanced to the index.
		 * 
		 * @param index
		 *            the starting index.
		 */
		public EListIterator(int index) {
			cursor = index;
		}

		/**
		 * Returns whether there are more objects for {@link #previous}. Returns
		 * whether there are more objects.
		 */
		public boolean hasPrevious() {
			return cursor != 0;
		}

		/**
		 * Returns the previous object and advances the iterator. This
		 * implementation delegates to {@link #doPrevious doPrevious}.
		 * 
		 * @return the previous object.
		 * @exception NoSuchElementException
		 *                if the iterator is done.
		 */
		@SuppressWarnings("unchecked")
		public E1 previous() {
			return (E1) doPrevious();
		}

		/**
		 * Returns the previous object and advances the iterator. This
		 * implementation delegates to {@link ExtensibleList#get get}.
		 * 
		 * @return the previous object.
		 * @exception NoSuchElementException
		 *                if the iterator is done.
		 */
		protected E doPrevious() {
			try {
				E previous = ExtensibleList.this.get(--cursor);
				checkModCount();
				lastCursor = cursor;
				return previous;
			} catch (IndexOutOfBoundsException exception) {
				checkModCount();
				throw new NoSuchElementException();
			}
		}

		/**
		 * Returns the index of the object that would be returned by calling
		 * {@link #next() next}.
		 * 
		 * @return the index of the object that would be returned by calling
		 *         <code>next</code>.
		 */
		public int nextIndex() {
			return cursor;
		}

		/**
		 * Returns the index of the object that would be returned by calling
		 * {@link #previous previous}.
		 * 
		 * @return the index of the object that would be returned by calling
		 *         <code>previous</code>.
		 */
		public int previousIndex() {
			return cursor - 1;
		}

		/**
		 * Sets the object at the index of the last call to {@link #next() next}
		 * or {@link #previous previous}. This implementation delegates to
		 * {@link ExtensibleList#set set}.
		 * 
		 * @param object
		 *            the object to set.
		 * @exception IllegalStateException
		 *                if <code>next</code> or <code>previous</code> have not
		 *                yet been called, or {@link #remove(Object) remove} or
		 *                {@link #add add} have already been called after the
		 *                last call to <code>next</code> or
		 *                <code>previous</code>.
		 */
		@SuppressWarnings("unchecked")
		public void set(E1 object) {
			doSet((E) object);
		}

		/**
		 * Sets the object at the index of the last call to {@link #next() next}
		 * or {@link #previous previous}. This implementation delegates to
		 * {@link ExtensibleList#set set}.
		 * 
		 * @param object
		 *            the object to set.
		 * @exception IllegalStateException
		 *                if <code>next</code> or <code>previous</code> have not
		 *                yet been called, or {@link #remove(Object) remove} or
		 *                {@link #add add} have already been called after the
		 *                last call to <code>next</code> or
		 *                <code>previous</code>.
		 */
		protected void doSet(E object) {
			if (lastCursor == -1) {
				throw new IllegalStateException();
			}
			checkModCount();

			try {
				ExtensibleList.this.set(lastCursor, object);
			} catch (IndexOutOfBoundsException exception) {
				throw new ConcurrentModificationException();
			}
		}

		/**
		 * Adds the object at the {@link #next() next} index and advances the
		 * iterator past it. This implementation delegates to
		 * {@link #doAdd(Object) doAdd(E)}.
		 * 
		 * @param object
		 *            the object to add.
		 */
		@SuppressWarnings("unchecked")
		public void add(E1 object) {
			doAdd((E) object);
		}

		/**
		 * Adds the object at the {@link #next() next} index and advances the
		 * iterator past it. This implementation delegates to
		 * {@link ExtensibleList#add(int, Object) add(int, E)}.
		 * 
		 * @param object
		 *            the object to add.
		 */
		protected void doAdd(E object) {
			checkModCount();

			try {
				ExtensibleList.this.add(cursor++, object);
				expectedModCount = modCount;
				lastCursor = -1;
			} catch (IndexOutOfBoundsException exception) {
				throw new ConcurrentModificationException();
			}
		}
	}

	/**
	 * Returns a read-only list iterator that does not {@link #resolve resolve}
	 * objects. This implementation allocates a
	 * {@link NonResolvingEListIterator}.
	 * 
	 * @return a read-only list iterator that does not resolve objects.
	 */
	protected ListIterator<E> basicListIterator() {
		return new NonResolvingEListIterator<E>();
	}

	/**
	 * Returns a read-only list iterator advanced to the given index that does
	 * not {@link #resolve resolve} objects. This implementation allocates a
	 * {@link NonResolvingEListIterator}.
	 * 
	 * @param index
	 *            the starting index.
	 * @return a read-only list iterator advanced to the index.
	 * @exception IndexOutOfBoundsException
	 *                if the index isn't within the size range.
	 */
	protected ListIterator<E> basicListIterator(int index) {
		if (index < 0 || index > size())
			throw new BasicIndexOutOfBoundsException(index, size);

		return new NonResolvingEListIterator<E>(index);
	}

	/**
	 * An extended read-only list iterator that does not
	 * {@link ExtensibleList#resolve resolve} objects.
	 */
	protected class NonResolvingEListIterator<E1> extends EListIterator<E1> {
		/**
		 * Creates an instance.
		 */
		public NonResolvingEListIterator() {
			super();
		}

		/**
		 * Creates an instance advanced to the index.
		 * 
		 * @param index
		 *            the starting index.
		 */
		public NonResolvingEListIterator(int index) {
			super(index);
		}

		/**
		 * Returns the next object and advances the iterator. This
		 * implementation accesses the data storage directly.
		 * 
		 * @return the next object.
		 * @exception NoSuchElementException
		 *                if the iterator is done.
		 */
		@Override
		protected E doNext() {
			try {
				@SuppressWarnings("unchecked")
				E next = (E) ExtensibleList.this.data[cursor];
				checkModCount();
				lastCursor = cursor++;
				return next;
			} catch (IndexOutOfBoundsException exception) {
				checkModCount();
				throw new NoSuchElementException();
			}
		}

		/**
		 * Returns the previous object and advances the iterator. This
		 * implementation accesses the data storage directly.
		 * 
		 * @return the previous object.
		 * @exception NoSuchElementException
		 *                if the iterator is done.
		 */
		@Override
		protected E doPrevious() {
			try {
				@SuppressWarnings("unchecked")
				E previous = (E) ExtensibleList.this.data[--cursor];
				checkModCount();
				lastCursor = cursor;
				return previous;
			} catch (IndexOutOfBoundsException exception) {
				checkModCount();
				throw new NoSuchElementException();
			}
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public void set(E1 object) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public void add(E1 object) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * An unmodifiable version of {@link ExtensibleList}.
	 */
	public static class UnmodifiableEList<E> extends ExtensibleList<E> {
		private static final long serialVersionUID = 1L;

		/**
		 * Creates an initialized instance.
		 * 
		 * @param size
		 *            the size of the list.
		 * @param data
		 *            the underlying storage of the list.
		 */
		public UnmodifiableEList(int size, Object[] data) {
			this.size = size;
			this.data = data;
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public E set(int index, E object) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public boolean add(E object) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public void add(int index, E object) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public boolean addAll(Collection<? extends E> collection) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public boolean addAll(int index, Collection<? extends E> collection) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public boolean remove(Object object) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public E remove(int index) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public boolean removeAll(Collection<?> collection) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public boolean retainAll(Collection<?> collection) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public void move(int index, E object) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public E move(int targetIndex, int sourceIndex) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public void shrink() {
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception.
		 * 
		 * @exception UnsupportedOperationException
		 *                always because it's not supported.
		 */
		@Override
		public void grow(int minimumCapacity) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Returns the {@link ExtensibleList#basicIterator basic iterator}.
		 * 
		 * @return the basic iterator.
		 */
		@Override
		public Iterator<E> iterator() {
			return basicIterator();
		}

		/**
		 * Returns the {@link #basicListIterator() basic list iterator}.
		 * 
		 * @return the basic list iterator.
		 */
		@Override
		public ListIterator<E> listIterator() {
			return basicListIterator();
		}

		/**
		 * Returns the {@link #basicListIterator(int) basic list iterator}
		 * advanced to the index.
		 * 
		 * @param index
		 *            the starting index.
		 * @return the basic list iterator.
		 */
		@Override
		public ListIterator<E> listIterator(int index) {
			return basicListIterator(index);
		}
	}

	/**
	 * Returns an <b>unsafe</b> list that provides a {@link #resolve
	 * non-resolving} view of the underlying data storage.
	 * 
	 * @return an <b>unsafe</b> list that provides a non-resolving view of the
	 *         underlying data storage.
	 */
	protected List<E> basicList() {
		if (size == 0) {
			return Collections.emptyList();
		} else {
			return new UnmodifiableEList<E>(size, data);
		}
	}

	/**
	 * A <code>BasicEList</code> that {@link #useEquals uses} <code>==</code>
	 * instead of <code>equals</code> to compare members.
	 */
	public static class FastCompare<E> extends ExtensibleList<E> {
		private static final long serialVersionUID = 1L;

		/**
		 * Creates an empty instance with no initial capacity.
		 */
		public FastCompare() {
			super();
		}

		/**
		 * Creates an empty instance with the given capacity.
		 * 
		 * @param initialCapacity
		 *            the initial capacity of the list before it must grow.
		 * @exception IllegalArgumentException
		 *                if the <code>initialCapacity</code> is negative.
		 */
		public FastCompare(int initialCapacity) {
			super(initialCapacity);
		}

		/**
		 * Creates an instance that is a copy of the collection.
		 * 
		 * @param collection
		 *            the initial contents of the list.
		 */
		public FastCompare(Collection<? extends E> collection) {
			super(collection.size());
			addAll(collection);
		}

		/**
		 * Returns <code>false</code> because this list uses <code>==</code>.
		 * 
		 * @return <code>false</code>.
		 */
		@Override
		protected boolean useEquals() {
			return false;
		}
	}

	/**
	 * Returns the collection of objects in the given collection that are also
	 * contained by this list.
	 * 
	 * @param collection
	 *            the other collection.
	 * @return the collection of objects in the given collection that are also
	 *         contained by this list.
	 */
	protected Collection<E> getDuplicates(Collection<?> collection) {
		if (collection.isEmpty()) {
			return Collections.emptyList();
		} else {
			Collection<E> filteredResult = useEquals() ? new ExtensibleList<E>(
					collection.size()) : new FastCompare<E>(collection.size());
			for (E object : this) {
				if (collection.contains(object)) {
					filteredResult.add(object);
				}
			}
			return filteredResult;
		}
	}

	/**
	 * Returns the collection of objects in the given collection that are not
	 * also contained by this list.
	 * 
	 * @param collection
	 *            the other collection.
	 * @return the collection of objects in the given collection that are not
	 *         also contained by this list.
	 */
	protected Collection<E> getNonDuplicates(Collection<? extends E> collection) {
		Collection<E> result = useEquals() ? new UniqueExtensibleList<E>(
				collection.size()) : new UniqueExtensibleList.FastCompare<E>(
				collection.size());
		for (E object : collection) {
			if (!contains(object)) {
				result.add(object);
			}
		}
		return result;
	}
}
