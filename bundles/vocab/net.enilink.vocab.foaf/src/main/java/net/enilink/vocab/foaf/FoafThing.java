package net.enilink.vocab.foaf;

import net.enilink.vocab.owl.Thing;
import net.enilink.composition.annotations.Iri;
import java.util.Set;

/** 
 * 
 * @generated 
 */
@Iri("http://www.w3.org/2002/07/owl#Thing")
public interface FoafThing extends Thing, FoafResource {
	/** 
	 * A depiction of some thing.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/depiction")
	Set<Image> getDepictions();
	/** 
	 * A depiction of some thing.
	 * @generated 
	 */
	void setDepictions(Set<? extends Image> depictions);

	/** 
	 * An organization funding a project or person.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/fundedBy")
	Set<Thing> getFundedBy();
	/** 
	 * An organization funding a project or person.
	 * @generated 
	 */
	void setFundedBy(Set<? extends Thing> fundedBy);

	/** 
	 * A homepage for some thing.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/homepage")
	Set<Document> getHomepages();
	/** 
	 * A homepage for some thing.
	 * @generated 
	 */
	void setHomepages(Set<? extends Document> homepages);

	/** 
	 * A document that this thing is the primary topic of.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/isPrimaryTopicOf")
	Set<Document> getIsPrimaryTopicOf();
	/** 
	 * A document that this thing is the primary topic of.
	 * @generated 
	 */
	void setIsPrimaryTopicOf(Set<? extends Document> isPrimaryTopicOf);

	/** 
	 * A logo representing some thing.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/logo")
	Set<Thing> getLogos();
	/** 
	 * A logo representing some thing.
	 * @generated 
	 */
	void setLogos(Set<? extends Thing> logos);

	/** 
	 * An agent that  made this thing.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/maker")
	Set<Agent> getMakers();
	/** 
	 * An agent that  made this thing.
	 * @generated 
	 */
	void setMakers(Set<? extends Agent> makers);

	/** 
	 * A name for some thing.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/name")
	Set<String> getNames();
	/** 
	 * A name for some thing.
	 * @generated 
	 */
	void setNames(Set<? extends String> names);

	/** 
	 * A page or document about this thing.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/page")
	Set<Document> getPages();
	/** 
	 * A page or document about this thing.
	 * @generated 
	 */
	void setPages(Set<? extends Document> pages);

	/** 
	 * A theme.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/theme")
	Set<Thing> getThemes();
	/** 
	 * A theme.
	 * @generated 
	 */
	void setThemes(Set<? extends Thing> themes);

}
