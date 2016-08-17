package net.enilink.komma.internal.sesame.result;

import org.eclipse.rdf4j.common.iteration.SingletonIteration;

import net.enilink.komma.core.IBooleanResult;

public class SesameBooleanResult extends SesameResult<Boolean, Boolean>
		implements IBooleanResult {
	Boolean value;

	public SesameBooleanResult(Boolean value) {
		super(new SingletonIteration<Boolean, Exception>(value));
	}

	@Override
	public boolean asBoolean() {
		if (value == null) {
			next();
			close();
		}
		return value;
	}

	@Override
	protected Boolean convert(Boolean element) throws Exception {
		value = element;
		return value;
	}
}
