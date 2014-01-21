package net.enilink.komma.dm;

import net.enilink.komma.core.IValue;

public interface IDataManagerUpdate {
	/**
	 * Executes this update.
	 */
	void execute();

	/**
	 * Assigns an entity or literal to the given name.
	 * 
	 * @param name
	 *            Name of the variable to bind to.
	 * @param value
	 *            managed entity or literal.
	 */
	IDataManagerUpdate setParameter(String name, IValue value);
}
