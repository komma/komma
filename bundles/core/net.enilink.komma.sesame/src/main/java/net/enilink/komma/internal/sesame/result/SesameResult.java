package net.enilink.komma.internal.sesame.result;

import info.aduna.iteration.CloseableIteration;
import net.enilink.commons.iterator.NiceIterator;
import net.enilink.komma.core.KommaException;

public abstract class SesameResult<S, T> extends NiceIterator<T> {
	protected CloseableIteration<S, ? extends Exception> delegate;

	private S current;

	public SesameResult(CloseableIteration<S, ? extends Exception> delegate) {
		this.delegate = delegate;
		if (!hasNext()) {
			close();
		}
	}

	public void close() {
		if (delegate != null) {
			try {
				delegate.close();
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new KommaException(e);
			} finally {
				delegate = null;
			}
		}
	}

	protected abstract T convert(S element) throws Exception;

	public boolean hasNext() {
		try {
			if (delegate != null && delegate.hasNext()) {
				return true;
			} else {
				close();
				return false;
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	public T next() {
		try {
			current = delegate.next();
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