package net.enilink.komma.internal.sesame.result;

import org.openrdf.cursor.Cursor;

import net.enilink.commons.iterator.NiceIterator;
import net.enilink.komma.core.KommaException;

public abstract class SesameResult<S, T> extends NiceIterator<T> {
	private Cursor<S> delegate;

	private S next, current;

	public SesameResult(Cursor<S> delegate) {
		this.delegate = delegate;
		if (!hasNext()) {
			close();
		}
	}

	public void close() {
		try {
			delegate.close();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	protected abstract T convert(S element) throws Exception;

	public boolean hasNext() {
		try {
			return next != null || (next = delegate.next()) != null;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	public T next() {
		try {
			current = next;
			next = null;
			if (!hasNext()) {
				close();
			}
			if (current == null) {
				return null;
			}
			return convert(current);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	public void remove() {
		try {
			remove(current);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	protected void remove(S element) throws Exception {
		throw new UnsupportedOperationException();
	}
}