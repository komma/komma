package net.enilink.komma.internal.rdf4j;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;

import com.google.inject.Inject;

import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.dm.IDataManagerUpdate;
import net.enilink.komma.rdf4j.RDF4JValueConverter;

/**
 * Implements {@link IDataManagerUpdate} for {@link RDF4JRepositoryDataManager}
 * .
 */
public class RDF4JUpdateRemote implements IDataManagerUpdate {
	protected Update update;

	@Inject
	RDF4JValueConverter valueConverter;

	public RDF4JUpdateRemote(Update update) {
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
		Value boundValue = valueConverter.toRdf4j(value);
		if (boundValue == null) {
			update.removeBinding(name);
		} else {
			update.setBinding(name, boundValue);
		}
		return this;
	}
}
