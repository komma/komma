package net.enilink.komma.internal.sesame.result;

import java.util.Iterator;
import java.util.List;

import org.openrdf.query.Binding;
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
		IValue[] result = new IValue[element.size()];
		int i = 0;
		for (Iterator<Binding> it = element.iterator(); it.hasNext(); i++) {
			Binding binding = it.next();

			result[i] = valueConverter.fromSesame(binding.getValue());
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