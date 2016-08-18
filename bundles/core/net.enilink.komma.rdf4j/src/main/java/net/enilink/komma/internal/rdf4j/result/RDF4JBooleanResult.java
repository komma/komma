package net.enilink.komma.internal.rdf4j.result;

import org.eclipse.rdf4j.common.iteration.SingletonIteration;

import net.enilink.komma.core.IBooleanResult;

public class RDF4JBooleanResult extends RDF4JResult<Boolean, Boolean>
		implements IBooleanResult {
	Boolean value;

	public RDF4JBooleanResult(Boolean value) {
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
