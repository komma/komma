package net.enilink.komma.internal.rdf4j.result;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Statement;

import com.google.inject.Inject;

import net.enilink.komma.core.IGraphResult;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.rdf4j.RDF4JValueConverter;

public class RDF4JGraphResult extends RDF4JResult<Statement, IStatement>
		implements IGraphResult {
	protected RDF4JValueConverter valueConverter;

	public RDF4JGraphResult(
			CloseableIteration<Statement, ? extends Exception> result) {
		super(result);
	}

	@Override
	protected IStatement convert(Statement element) throws Exception {
		return new net.enilink.komma.core.Statement(
				(IReference) valueConverter.fromRdf4j(element.getSubject()), //
				(IReference) valueConverter.fromRdf4j(element.getPredicate()), //
				valueConverter.fromRdf4j(element.getObject()), //
				(IReference) valueConverter.fromRdf4j(element.getContext()));
	}

	@Inject
	public void setValueConverter(RDF4JValueConverter valueConverter) {
		this.valueConverter = valueConverter;
	}
}
