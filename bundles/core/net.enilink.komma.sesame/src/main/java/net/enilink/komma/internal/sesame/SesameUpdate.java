package net.enilink.komma.internal.sesame;

import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.dm.IDataManagerUpdate;
import net.enilink.komma.sesame.SesameValueConverter;

import org.openrdf.model.Value;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;

import com.google.inject.Inject;

/**
 * Implements {@link IDataManagerUpdate} for {@link SesameRepositoryDataManager}
 * .
 */
public class SesameUpdate implements IDataManagerUpdate {
	protected Update update;

	@Inject
	SesameValueConverter valueConverter;

	public SesameUpdate(Update update) {
		this.update = update;
	}

	@Override
	public void execute() {
		try {
			update.execute();
		} catch (UpdateExecutionException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public IDataManagerUpdate setParameter(String name, IValue value) {
		Value boundValue = valueConverter.toSesame(value);
		if (boundValue == null) {
			update.removeBinding(name);
		} else {
			update.setBinding(name, boundValue);
		}
		return this;
	}

}
