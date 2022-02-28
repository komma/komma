package net.enilink.komma.em.concepts;

import static net.enilink.komma.em.concepts.Concepts.NS;

import java.util.Set;

import net.enilink.composition.annotations.Iri;

@Iri(NS + "Person")
public interface Person {
	@Iri(NS + "name")
	String getName();

	void setName(String name);

	@Iri(NS + "age")
	int getAge();

	void setAge(int age);

	@Iri(NS + "friend")
	Set<Person> getFriends();

	void setFriends(Set<Person> friends);
}