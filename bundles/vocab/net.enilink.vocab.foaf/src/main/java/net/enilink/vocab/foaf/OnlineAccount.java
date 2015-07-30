package net.enilink.vocab.foaf;

import net.enilink.komma.core.IEntity;
import net.enilink.vocab.owl.Thing;
import net.enilink.composition.annotations.Iri;
import java.util.Set;

/** 
 * An online account.
 * @generated 
 */
@Iri("http://xmlns.com/foaf/0.1/OnlineAccount")
public interface OnlineAccount extends Thing, IEntity {
	/** 
	 * Indicates the name (identifier) associated with this online account.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/accountName")
	Set<String> getFoafAccountName();
	/** 
	 * Indicates the name (identifier) associated with this online account.
	 * @generated 
	 */
	void setFoafAccountName(Set<? extends String> foafAccountName);

	/** 
	 * Indicates a homepage of the service provide for this online account.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/accountServiceHomepage")
	Set<Document> getFoafAccountServiceHomepage();
	/** 
	 * Indicates a homepage of the service provide for this online account.
	 * @generated 
	 */
	void setFoafAccountServiceHomepage(Set<? extends Document> foafAccountServiceHomepage);

}
