package net.enilink.vocab.komma;

import net.enilink.komma.core.KommaModule;

public class KommaConceptsModule extends KommaModule {
	{
		addConcept(net.enilink.vocab.komma.KeyValueMap.class);
		addConcept(net.enilink.vocab.komma.KommaResource.class);
		addConcept(net.enilink.vocab.komma.LiteralKeyMap.class);
		addConcept(net.enilink.vocab.komma.LiteralKeyValueMap.class);
		addConcept(net.enilink.vocab.komma.LiteralValueMap.class);
		addConcept(net.enilink.vocab.komma.Map.class);
		addConcept(net.enilink.vocab.komma.MapEntry.class);
		addConcept(net.enilink.vocab.komma.Connection.class);
	}
}
