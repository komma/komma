package net.enilink.komma.internal.sesame.result;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Statement;

import com.google.inject.Inject;

import net.enilink.komma.core.IGraphResult;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.sesame.SesameValueConverter;

public class SesameGraphResult extends SesameResult<Statement, IStatement>
		implements IGraphResult {
	protected SesameValueConverter valueConverter;

	public SesameGraphResult(
			CloseableIteration<Statement, ? extends Exception> result) {
		super(result);
	}

	@Override
	protected IStatement convert(Statement element) throws Exception {
		return new net.enilink.komma.core.Statement(
				(IReference) valueConverter.fromSesame(element.getSubject()), //
				(IReference) valueConverter.fromSesame(element.getPredicate()), //
				valueConverter.fromSesame(element.getObject()), //
				(IReference) valueConverter.fromSesame(element.getContext()));
	}

	@Inject
	public void setValueConverter(SesameValueConverter valueConverter) {
		this.valueConverter = valueConverter;
	}
}
