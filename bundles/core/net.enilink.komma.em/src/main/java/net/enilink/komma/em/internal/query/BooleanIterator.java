package net.enilink.komma.em.internal.query;

import java.util.NoSuchElementException;

import net.enilink.commons.iterator.NiceIterator;
import net.enilink.komma.core.IBooleanResult;

public class BooleanIterator extends NiceIterator<Boolean> implements
		IBooleanResult {
	protected Boolean value;

	public BooleanIterator(Boolean value) {
		this.value = value;
	}

	@Override
	public boolean asBoolean() {
		return next();
	}

	@Override
	public boolean hasNext() {
		return value != null;
	}

	@Override
	public Boolean next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		Boolean result = value;
		value = null;
		return result;
	}
}
