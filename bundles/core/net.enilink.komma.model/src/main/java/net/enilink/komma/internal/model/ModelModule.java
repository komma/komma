package net.enilink.komma.internal.model;

import net.enilink.komma.core.KommaModule;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.rdf4j.MemoryModelSetSupport;
import net.enilink.komma.model.rdf4j.PersistentModelSetSupport;
import net.enilink.komma.model.rdf4j.RemoteModelSetSupport;
import net.enilink.komma.model.rdf4j.SPARQLModelSetSupport;
import net.enilink.komma.model.rdf4j.SerializableModelSupport;

public class ModelModule extends KommaModule {
	{
		addConcept(net.enilink.komma.model.concepts.Diagnostic.class);
		addConcept(net.enilink.komma.model.concepts.Model.class);
		addConcept(net.enilink.komma.model.concepts.ModelSet.class);
		addConcept(net.enilink.komma.model.concepts.ModelSetContainer.class);
		addConcept(net.enilink.komma.model.concepts.Namespace.class);

		addConcept(IModelSet.class);
		addConcept(IModel.class);
		addBehaviour(MemoryModelSetSupport.class);
		addBehaviour(SerializableModelSupport.class);
		addBehaviour(RemoteModelSetSupport.class);
		addBehaviour(SPARQLModelSetSupport.class);
		addBehaviour(PersistentModelSetSupport.class);
	}
}