package net.enilink.vocab.foaf;

import net.enilink.vocab.owl.Thing;
import net.enilink.composition.annotations.Iri;
import java.util.Set;

/** 
 * 
 * @generated 
 */
@Iri("http://www.w3.org/2004/02/skos/core#Concept")
public interface FoafConcept extends FoafResource {
	/** 
	 * The underlying or 'focal' entity associated with some SKOS-described concept.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/focus")
	Set<Thing> getFocus();
	/** 
	 * The underlying or 'focal' entity associated with some SKOS-described concept.
	 * @generated 
	 */
	void setFocus(Set<? extends Thing> focus);

}
