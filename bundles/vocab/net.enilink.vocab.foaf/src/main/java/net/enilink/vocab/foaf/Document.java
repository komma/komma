package net.enilink.vocab.foaf;

import net.enilink.vocab.owl.Thing;
import net.enilink.komma.core.IEntity;
import net.enilink.composition.annotations.Iri;
import java.util.Set;

/** 
 * A document.
 * @generated 
 */
@Iri("http://xmlns.com/foaf/0.1/Document")
public interface Document extends IEntity {
	/** 
	 * The primary topic of some page or document.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/primaryTopic")
	Thing getFoafPrimaryTopic();
	/** 
	 * The primary topic of some page or document.
	 * @generated 
	 */
	void setFoafPrimaryTopic(Thing foafPrimaryTopic);

	/** 
	 * A sha1sum hash, in hex.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/sha1")
	Set<Object> getFoafSha1();
	/** 
	 * A sha1sum hash, in hex.
	 * @generated 
	 */
	void setFoafSha1(Set<?> foafSha1);

	/** 
	 * A topic of some page or document.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/topic")
	Set<Thing> getFoafTopic();
	/** 
	 * A topic of some page or document.
	 * @generated 
	 */
	void setFoafTopic(Set<? extends Thing> foafTopic);

}
