package net.enilink.komma.em.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.enilink.komma.core.KommaModule;
import net.enilink.komma.em.internal.behaviours.RDFSContainer;

public class KommaCoreModule extends KommaModule {
	{
		for (String type : Arrays.asList(
				"http://www.w3.org/2000/01/rdf-schema#Container",
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq",
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#Alt",
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#Bag")) {
			addConcept(List.class, type);
			addBehaviour(RDFSContainer.class, type);
		}
		addConcept(Map.class, "http://enilink.net/vocab/komma#Map");

		addBehaviour(net.enilink.komma.em.internal.behaviours.RDFList.class);
		addBehaviour(net.enilink.komma.em.internal.behaviours.RDFSContainerDisabler.class);
		addBehaviour(net.enilink.komma.em.internal.behaviours.KeyValueMap.class);
		addBehaviour(net.enilink.komma.em.internal.behaviours.LiteralKeyValueMap.class);
		addBehaviour(net.enilink.komma.em.internal.behaviours.LiteralKeyMap.class);
		addBehaviour(net.enilink.komma.em.internal.behaviours.LiteralValueMap.class);
		addBehaviour(net.enilink.komma.em.concepts.OntologySupport.class);

		addAnnotation(net.enilink.composition.annotations.Matching.class);
	}
}
