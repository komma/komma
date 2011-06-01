package net.enilink.komma.internal.sesame.result;

import org.openrdf.cursor.Cursor;

import net.enilink.komma.core.IBooleanResult;

public class SesameBooleanResult extends SesameResult<Boolean, Boolean>
		implements IBooleanResult {
	Boolean value;

	public SesameBooleanResult(Cursor<Boolean> delegate) {
		super(delegate);
	}

	@Override
	public boolean asBoolean() {
		if (value == null) {
			next();
		}
		return value;
	}

	@Override
	protected Boolean convert(Boolean element) throws Exception {
		value = element;
		return value;
	}
}