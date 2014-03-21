package net.enilink.vocab.rdfs;

import net.enilink.komma.core.KommaModule;

public class RdfsModule extends KommaModule {
	{
		addConcept(net.enilink.vocab.rdf.Alt.class);
		addConcept(net.enilink.vocab.rdf.Bag.class);
		addConcept(net.enilink.vocab.rdf.List.class);
		addConcept(net.enilink.vocab.rdf.Property.class);
		addConcept(net.enilink.vocab.rdfs.Class.class);
		addConcept(net.enilink.vocab.rdfs.Container.class);
		addConcept(net.enilink.vocab.rdfs.ContainerMembershipProperty.class);
		addConcept(net.enilink.vocab.rdfs.Datatype.class);
		addConcept(net.enilink.vocab.rdf.Seq.class);
		addConcept(net.enilink.vocab.rdfs.Resource.class);
		addConcept(net.enilink.vocab.rdf.Statement.class);
	}
}
