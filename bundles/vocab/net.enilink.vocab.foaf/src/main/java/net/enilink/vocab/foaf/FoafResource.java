package net.enilink.vocab.foaf;

import net.enilink.komma.core.IEntity;
import net.enilink.vocab.rdfs.Resource;
import net.enilink.composition.annotations.Iri;
import java.util.Set;

/** 
 * 
 * @generated 
 */
@Iri("http://www.w3.org/2000/01/rdf-schema#Resource")
public interface FoafResource extends Resource, IEntity {
	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://purl.org/dc/elements/1.1/date")
	Set<Object> getDates();
	/** 
	 * 
	 * @generated 
	 */
	void setDates(Set<?> dates);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://purl.org/dc/elements/1.1/description")
	Set<Object> getDescriptions();
	/** 
	 * 
	 * @generated 
	 */
	void setDescriptions(Set<?> descriptions);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://www.w3.org/2003/06/sw-vocab-status/ns#term_status")
	Set<Object> getVsTerm_status();
	/** 
	 * 
	 * @generated 
	 */
	void setVsTerm_status(Set<?> vsTerm_status);

	/** 
	 * The given name of some person.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/givenName")
	Set<Object> getGivenName();
	/** 
	 * The given name of some person.
	 * @generated 
	 */
	void setGivenName(Set<?> givenName);

	/** 
	 * The given name of some person.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/givenname")
	Set<Object> getGivennames();
	/** 
	 * The given name of some person.
	 * @generated 
	 */
	void setGivennames(Set<?> givennames);

	/** 
	 * Indicates the class of individuals that are a member of a Group
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/membershipClass")
	Set<Object> getMembershipClass();
	/** 
	 * Indicates the class of individuals that are a member of a Group
	 * @generated 
	 */
	void setMembershipClass(Set<?> membershipClass);

	/** 
	 * A short informal nickname characterising an agent (includes login identifiers, IRC and other chat nicknames).
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/nick")
	Set<Object> getNicks();
	/** 
	 * A short informal nickname characterising an agent (includes login identifiers, IRC and other chat nicknames).
	 * @generated 
	 */
	void setNicks(Set<?> nicks);

	/** 
	 * A phone,  specified using fully qualified tel: URI scheme (refs: http://www.w3.org/Addressing/schemes.html#tel).
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/phone")
	Set<Object> getPhones();
	/** 
	 * A phone,  specified using fully qualified tel: URI scheme (refs: http://www.w3.org/Addressing/schemes.html#tel).
	 * @generated 
	 */
	void setPhones(Set<?> phones);

	/** 
	 * Title (Mr, Mrs, Ms, Dr. etc)
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/title")
	Set<String> getTitles();
	/** 
	 * Title (Mr, Mrs, Ms, Dr. etc)
	 * @generated 
	 */
	void setTitles(Set<String> titles);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://xmlns.com/wot/0.1/assurance")
	Set<Object> getAssurances();
	/** 
	 * 
	 * @generated 
	 */
	void setAssurances(Set<?> assurances);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://xmlns.com/wot/0.1/src_assurance")
	Set<Object> getSrc_assurances();
	/** 
	 * 
	 * @generated 
	 */
	void setSrc_assurances(Set<?> src_assurances);

}
