package net.enilink.vocab.foaf;

import net.enilink.composition.annotations.Iri;
import java.util.Set;

/** 
 * A class of Agents.
 * @generated 
 */
@Iri("http://xmlns.com/foaf/0.1/Group")
public interface Group extends Agent {
	/** 
	 * Indicates a member of a Group
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/member")
	Set<Agent> getFoafMember();
	/** 
	 * Indicates a member of a Group
	 * @generated 
	 */
	void setFoafMember(Set<? extends Agent> foafMember);

}
