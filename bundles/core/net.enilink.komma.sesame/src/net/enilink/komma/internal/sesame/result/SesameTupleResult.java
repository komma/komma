package net.enilink.komma.internal.sesame.result;

import java.util.List;

import org.openrdf.query.BindingSet;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;

import com.google.inject.Inject;

import net.enilink.komma.core.ITupleResult;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.sesame.SesameValueConverter;

public class SesameTupleResult extends SesameResult<BindingSet, IValue[]>
		implements ITupleResult<IValue[]> {
	private TupleResult result;

	protected SesameValueConverter valueConverter;

	public SesameTupleResult(TupleResult result) {
		super(result);
		this.result = result;
	}

	@Override
	protected IValue[] convert(BindingSet element) throws Exception {
		IValue[] result = new IValue[getBindingNames().size()];
		int i = 0;
		for (String name : getBindingNames()) {
			result[i++] = valueConverter.fromSesame(element.getValue(name));
		}
		return result;
	}

	@Override
	public List<String> getBindingNames() {
		try {
			return result.getBindingNames();
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Inject
	public void setValueConverter(SesameValueConverter valueConverter) {
		this.valueConverter = valueConverter;
	}
}