package net.enilink.vocab.foaf;

import net.enilink.vocab.owl.Thing;
import net.enilink.composition.annotations.Iri;
import java.util.Set;

/** 
 * A person.
 * @generated 
 */
@Iri("http://xmlns.com/foaf/0.1/Person")
public interface Person extends Agent {
	/** 
	 * A current project this person works on.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/currentProject")
	Set<Thing> getFoafCurrentProject();
	/** 
	 * A current project this person works on.
	 * @generated 
	 */
	void setFoafCurrentProject(Set<? extends Thing> foafCurrentProject);

	/** 
	 * The family name of some person.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/familyName")
	Set<String> getFoafFamilyName();
	/** 
	 * The family name of some person.
	 * @generated 
	 */
	void setFoafFamilyName(Set<? extends String> foafFamilyName);

	/** 
	 * The family name of some person.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/family_name")
	Set<String> getFoafFamily_name();
	/** 
	 * The family name of some person.
	 * @generated 
	 */
	void setFoafFamily_name(Set<? extends String> foafFamily_name);

	/** 
	 * The first name of a person.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/firstName")
	Set<String> getFoafFirstName();
	/** 
	 * The first name of a person.
	 * @generated 
	 */
	void setFoafFirstName(Set<? extends String> foafFirstName);

	/** 
	 * A textual geekcode for this person, see http://www.geekcode.com/geek.html
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/geekcode")
	Set<String> getFoafGeekcode();
	/** 
	 * A textual geekcode for this person, see http://www.geekcode.com/geek.html
	 * @generated 
	 */
	void setFoafGeekcode(Set<? extends String> foafGeekcode);

	/** 
	 * An image that can be used to represent some thing (ie. those depictions which are particularly representative of something, eg. one's photo on a homepage).
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/img")
	Set<Image> getFoafImg();
	/** 
	 * An image that can be used to represent some thing (ie. those depictions which are particularly representative of something, eg. one's photo on a homepage).
	 * @generated 
	 */
	void setFoafImg(Set<? extends Image> foafImg);

	/** 
	 * A person known by this person (indicating some level of reciprocated interaction between the parties).
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/knows")
	Set<net.enilink.vocab.foaf.Person> getFoafKnows();
	/** 
	 * A person known by this person (indicating some level of reciprocated interaction between the parties).
	 * @generated 
	 */
	void setFoafKnows(Set<? extends net.enilink.vocab.foaf.Person> foafKnows);

	/** 
	 * The last name of a person.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/lastName")
	Set<String> getFoafLastName();
	/** 
	 * The last name of a person.
	 * @generated 
	 */
	void setFoafLastName(Set<? extends String> foafLastName);

	/** 
	 * A Myers Briggs (MBTI) personality classification.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/myersBriggs")
	Set<String> getFoafMyersBriggs();
	/** 
	 * A Myers Briggs (MBTI) personality classification.
	 * @generated 
	 */
	void setFoafMyersBriggs(Set<? extends String> foafMyersBriggs);

	/** 
	 * A project this person has previously worked on.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/pastProject")
	Set<Thing> getFoafPastProject();
	/** 
	 * A project this person has previously worked on.
	 * @generated 
	 */
	void setFoafPastProject(Set<? extends Thing> foafPastProject);

	/** 
	 * A .plan comment, in the tradition of finger and '.plan' files.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/plan")
	Set<String> getFoafPlan();
	/** 
	 * A .plan comment, in the tradition of finger and '.plan' files.
	 * @generated 
	 */
	void setFoafPlan(Set<? extends String> foafPlan);

	/** 
	 * A link to the publications of this person.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/publications")
	Set<Document> getFoafPublications();
	/** 
	 * A link to the publications of this person.
	 * @generated 
	 */
	void setFoafPublications(Set<? extends Document> foafPublications);

	/** 
	 * A homepage of a school attended by the person.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/schoolHomepage")
	Set<Document> getFoafSchoolHomepage();
	/** 
	 * A homepage of a school attended by the person.
	 * @generated 
	 */
	void setFoafSchoolHomepage(Set<? extends Document> foafSchoolHomepage);

	/** 
	 * The surname of some person.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/surname")
	Set<String> getFoafSurname();
	/** 
	 * The surname of some person.
	 * @generated 
	 */
	void setFoafSurname(Set<? extends String> foafSurname);

	/** 
	 * A work info homepage of some person; a page about their work for some organization.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/workInfoHomepage")
	Set<Document> getFoafWorkInfoHomepage();
	/** 
	 * A work info homepage of some person; a page about their work for some organization.
	 * @generated 
	 */
	void setFoafWorkInfoHomepage(Set<? extends Document> foafWorkInfoHomepage);

	/** 
	 * A workplace homepage of some person; the homepage of an organization they work for.
	 * @generated 
	 */
	@Iri("http://xmlns.com/foaf/0.1/workplaceHomepage")
	Set<Document> getFoafWorkplaceHomepage();
	/** 
	 * A workplace homepage of some person; the homepage of an organization they work for.
	 * @generated 
	 */
	void setFoafWorkplaceHomepage(Set<? extends Document> foafWorkplaceHomepage);

}
