package net.enilink.vocab.foaf;

import net.enilink.vocab.owl.Thing;
import net.enilink.composition.annotations.Iri;
import java.util.Set;

/** 
 * An image.
 * @generated 
 */
@Iri("http://xmlns.com/foaf/0.1/Image")
public interface Image extends Document {
	/** 
	 * A thing depicted in this representation.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/depicts")
	Set<Thing> getFoafDepicts();
	/** 
	 * A thing depicted in this representation.
	 * @generated 
	 */
	void setFoafDepicts(Set<? extends Thing> foafDepicts);

	/** 
	 * A derived thumbnail image.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/thumbnail")
	Set<net.enilink.vocab.foaf.Image> getFoafThumbnail();
	/** 
	 * A derived thumbnail image.
	 * @generated 
	 */
	void setFoafThumbnail(Set<? extends net.enilink.vocab.foaf.Image> foafThumbnail);

}
