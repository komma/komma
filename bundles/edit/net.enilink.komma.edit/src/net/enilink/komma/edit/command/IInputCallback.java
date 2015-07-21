package net.enilink.komma.edit.command;

import net.enilink.komma.core.URI;
import net.enilink.komma.model.IModel;

/**
 * This interface allows to request user interactions from business logic.
 * 
 */
public interface IInputCallback {
	/**
	 * Ask for the previously defined inputs.
	 * 
	 * @param model
	 *            The model that should be used as reference for new names and
	 *            others.
	 * 
	 * @return <code>true</code> if user has given the required inputs, else
	 *         <code>false</code>.
	 */
	boolean ask(IModel model);

	/**
	 * Return the value for the given input type.
	 * 
	 * @param inputType
	 *            The input type for which the value should be returned.
	 * @return The value for the input type
	 */
	Object get(URI inputType);

	/**
	 * Request a specific type of input.
	 * 
	 * @param inputType
	 *            The input type that should be requested
	 * @param parameters
	 *            The parameters for the input type
	 * 
	 * @return This input callback
	 */
	IInputCallback require(URI inputType, Object... parameters);
}
