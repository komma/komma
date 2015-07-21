package net.enilink.vocab.owl;

import net.enilink.komma.core.KommaModule;

public class OwlModule extends KommaModule {
	{
		addConcept(net.enilink.vocab.owl.AllDifferent.class);
		addConcept(net.enilink.vocab.owl.AnnotationProperty.class);
		addConcept(net.enilink.vocab.owl.Class.class);
		addConcept(net.enilink.vocab.owl.DataRange.class);
		addConcept(net.enilink.vocab.owl.DatatypeProperty.class);
		addConcept(net.enilink.vocab.owl.DeprecatedClass.class);
		addConcept(net.enilink.vocab.owl.DeprecatedProperty.class);
		addConcept(net.enilink.vocab.owl.FunctionalProperty.class);
		addConcept(net.enilink.vocab.owl.InverseFunctionalProperty.class);
		addConcept(net.enilink.vocab.owl.Nothing.class);
		addConcept(net.enilink.vocab.owl.ObjectProperty.class);
		addConcept(net.enilink.vocab.owl.Ontology.class);
		addConcept(net.enilink.vocab.owl.OntologyProperty.class);
		addConcept(net.enilink.vocab.owl.OwlProperty.class);
		addConcept(net.enilink.vocab.owl.Restriction.class);
		addConcept(net.enilink.vocab.owl.SymmetricProperty.class);
		addConcept(net.enilink.vocab.owl.Thing.class);
		addConcept(net.enilink.vocab.owl.TransitiveProperty.class);
	}
}
