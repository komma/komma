package net.enilink.komma.em.concepts;

import net.enilink.composition.annotations.Iri;

import static net.enilink.komma.em.concepts.Concepts.NS;

@Iri(NS + "ObjectWithTypeEnum")
public interface ObjectWithTypeEnum {
	@Iri(NS + "type")
	Type getType();

	void setType(Type type);
}