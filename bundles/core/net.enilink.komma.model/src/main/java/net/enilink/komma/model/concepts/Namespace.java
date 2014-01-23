package net.enilink.komma.model.concepts;

import net.enilink.composition.annotations.Iri;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.URI;

@Iri("http://enilink.net/vocab/komma/models#Namespace")
public interface Namespace extends INamespace {
	@Iri("http://enilink.net/vocab/komma/models#prefix")
	String getPrefix();

	void setPrefix(String prefix);

	@Iri("http://enilink.net/vocab/komma/models#uri")
	URI getURI();

	void setURI(URI uri);
}
