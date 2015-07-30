package net.enilink.komma.internal.model;

import net.enilink.komma.model.IModel;

public interface IModelAware {
	/**
	 * Associated some entity with an {@link IModel} which contains the data of
	 * this object.
	 */
	void initModel(IModel model);
}
