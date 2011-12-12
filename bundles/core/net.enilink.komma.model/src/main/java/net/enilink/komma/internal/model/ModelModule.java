package net.enilink.komma.internal.model;

import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.model.sesame.MemoryModelSetSupport;
import net.enilink.komma.model.sesame.RemoteModelSetSupport;
import net.enilink.komma.model.sesame.SerializableModelSupport;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.util.KommaUtil;
import net.enilink.komma.util.RoleClassLoader;

public class ModelModule extends KommaModule {
	{
		RoleClassLoader loader = new RoleClassLoader(this);
		loader.load(KommaUtil.getBundleMetaInfLocations(ModelCore.PLUGIN_ID));

		addConcept(IModelSet.class);
		addConcept(IModel.class);
		addBehaviour(MemoryModelSetSupport.class);
		addBehaviour(SerializableModelSupport.class);
		addBehaviour(RemoteModelSetSupport.class);
	}

}
