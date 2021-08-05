package net.enilink.composition.properties.util;

import java.util.*;

/**
 * Wraps a given collection to provide the {@link List} interface.
 *
 * @param <E> The set's element type
 */
public class CollectionAsListWrapper<E> extends AbstractSequentialList<E> {
	protected final Collection<E> base;

	public CollectionAsListWrapper(Collection<E> base) {
		this.base = base;
	}
	/**
	 * A list iterator that also modifies the underlying base set.
	 */
	class CollectionAsListIterator implements ListIterator<E> {
		ListIterator<E> baseIterator;
		E current;

		public CollectionAsListIterator(int index) {
			// convert underlying set to array list (may have random sort order)
			List<E> baseElements = new ArrayList<>(base);
			// iterate over this array list
			this.baseIterator = baseElements.listIterator(index);
		}

		@Override
		public boolean hasNext() {
			return baseIterator.hasNext();
		}

		@Override
		public E next() {
			return current = baseIterator.next();
		}

		@Override
		public boolean hasPrevious() {
			return baseIterator.hasPrevious();
		}

		@Override
		public E previous() {
			return current = baseIterator.previous();
		}

		@Override
		public int nextIndex() {
			return baseIterator.nextIndex();
		}

		@Override
		public int previousIndex() {
			return baseIterator.previousIndex();
		}

		@Override
		public void remove() {
			baseIterator.remove();
			base.remove(current);
		}

		@Override
		public void set(E e) {
			baseIterator.set(e);
			base.remove(current);
			base.add(e);
		}

		@Override
		public void add(E e) {
			baseIterator.add(e);
			base.add(e);
		}
	}

	@Override
	public ListIterator<E> listIterator(int i) {
		return new CollectionAsListIterator(i);
	}

	@Override
	public int size() {
		return base.size();
	}
}