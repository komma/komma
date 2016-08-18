package net.enilink.komma.internal.rdf4j.result;

import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;

import com.google.inject.Inject;

import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.ITupleResult;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.LinkedHashBindings;
import net.enilink.komma.rdf4j.RDF4JValueConverter;

public class RDF4JTupleResult extends
		RDF4JResult<BindingSet, IBindings<IValue>> implements
		ITupleResult<IBindings<IValue>> {
	protected RDF4JValueConverter valueConverter;

	public RDF4JTupleResult(TupleQueryResult result) {
		super(result);
	}

	@Override
	protected IBindings<IValue> convert(BindingSet element) throws Exception {
		LinkedHashBindings<IValue> result = new LinkedHashBindings<IValue>(
				element.size());
		for (String name : getBindingNames()) {
			IValue value = valueConverter.fromRdf4j(element.getValue(name));
			if (value != null) {
				result.put(name, value);
			}
		}
		return result;
	}

	@Override
	public List<String> getBindingNames() {
		try {
			return ((TupleQueryResult) delegate).getBindingNames();
		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);
		}
	}

	@Inject
	public void setValueConverter(RDF4JValueConverter valueConverter) {
		this.valueConverter = valueConverter;
	}
}
