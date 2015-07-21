package net.enilink.vocab.foaf;

import java.util.Date;
import java.util.Set;

import net.enilink.composition.annotations.Iri;

import net.enilink.vocab.owl.Thing;

/**
 * An agent (eg. person, group, software or physical artifact).
 * 
 * @generated
 */
@Iri("http://xmlns.com/foaf/0.1/Agent")
public interface Agent {
	/**
	 * Indicates an account held by this agent.
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/account")
	Set<OnlineAccount> getFoafAccount();

	/**
	 * Indicates an account held by this agent.
	 * 
	 * @generated
	 */
	void setFoafAccount(Set<? extends OnlineAccount> foafAccount);

	/**
	 * The age in years of some agent.
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/age")
	String getFoafAge();

	/**
	 * The age in years of some agent.
	 * 
	 * @generated
	 */
	void setFoafAge(String foafAge);

	/**
	 * An AIM chat ID
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/aimChatID")
	Set<String> getFoafAimChatID();

	/**
	 * An AIM chat ID
	 * 
	 * @generated
	 */
	void setFoafAimChatID(Set<? extends String> foafAimChatID);

	/**
	 * The birthday of this Agent, represented in mm-dd string form, eg.
	 * '12-31'.
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/birthday")
	Date getFoafBirthday();

	/**
	 * The birthday of this Agent, represented in mm-dd string form, eg.
	 * '12-31'.
	 * 
	 * @generated
	 */
	void setFoafBirthday(Date foafBirthday);

	/**
	 * The gender of this Agent (typically but not necessarily 'male' or
	 * 'female').
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/gender")
	String getFoafGender();

	/**
	 * The gender of this Agent (typically but not necessarily 'male' or
	 * 'female').
	 * 
	 * @generated
	 */
	void setFoafGender(String foafGender);

	/**
	 * Indicates an account held by this agent.
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/holdsAccount")
	Set<OnlineAccount> getFoafHoldsAccount();

	/**
	 * Indicates an account held by this agent.
	 * 
	 * @generated
	 */
	void setFoafHoldsAccount(Set<? extends OnlineAccount> foafHoldsAccount);

	/**
	 * An ICQ chat ID
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/icqChatID")
	Set<String> getFoafIcqChatID();

	/**
	 * An ICQ chat ID
	 * 
	 * @generated
	 */
	void setFoafIcqChatID(Set<? extends String> foafIcqChatID);

	/**
	 * A page about a topic of interest to this person.
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/interest")
	Set<Document> getFoafInterest();

	/**
	 * A page about a topic of interest to this person.
	 * 
	 * @generated
	 */
	void setFoafInterest(Set<? extends Document> foafInterest);

	/**
	 * A jabber ID for something.
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/jabberID")
	Set<String> getFoafJabberID();

	/**
	 * A jabber ID for something.
	 * 
	 * @generated
	 */
	void setFoafJabberID(Set<? extends String> foafJabberID);

	/**
	 * Something that was made by this agent.
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/made")
	Set<Thing> getFoafMade();

	/**
	 * Something that was made by this agent.
	 * 
	 * @generated
	 */
	void setFoafMade(Set<? extends Thing> foafMade);

	/**
	 * A personal mailbox, ie. an Internet mailbox associated with exactly one
	 * owner, the first owner of this mailbox. This is a 'static inverse
	 * functional property', in that there is (across time and change) at most
	 * one individual that ever has any particular value for foaf:mbox.
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/mbox")
	Set<Thing> getFoafMbox();

	/**
	 * A personal mailbox, ie. an Internet mailbox associated with exactly one
	 * owner, the first owner of this mailbox. This is a 'static inverse
	 * functional property', in that there is (across time and change) at most
	 * one individual that ever has any particular value for foaf:mbox.
	 * 
	 * @generated
	 */
	void setFoafMbox(Set<? extends Thing> foafMbox);

	/**
	 * The sha1sum of the URI of an Internet mailbox associated with exactly one
	 * owner, the first owner of the mailbox.
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/mbox_sha1sum")
	Set<String> getFoafMbox_sha1sum();

	/**
	 * The sha1sum of the URI of an Internet mailbox associated with exactly one
	 * owner, the first owner of the mailbox.
	 * 
	 * @generated
	 */
	void setFoafMbox_sha1sum(Set<? extends String> foafMbox_sha1sum);

	/**
	 * An MSN chat ID
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/msnChatID")
	Set<String> getFoafMsnChatID();

	/**
	 * An MSN chat ID
	 * 
	 * @generated
	 */
	void setFoafMsnChatID(Set<? extends String> foafMsnChatID);

	/**
	 * An OpenID for an Agent.
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/openid")
	Set<Document> getFoafOpenid();

	/**
	 * An OpenID for an Agent.
	 * 
	 * @generated
	 */
	void setFoafOpenid(Set<? extends Document> foafOpenid);

	/**
	 * A Skype ID
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/skypeID")
	Set<String> getFoafSkypeID();

	/**
	 * A Skype ID
	 * 
	 * @generated
	 */
	void setFoafSkypeID(Set<? extends String> foafSkypeID);

	/**
	 * A string expressing what the user is happy for the general public
	 * (normally) to know about their current activity.
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/status")
	Set<String> getFoafStatus();

	/**
	 * A string expressing what the user is happy for the general public
	 * (normally) to know about their current activity.
	 * 
	 * @generated
	 */
	void setFoafStatus(Set<? extends String> foafStatus);

	/**
	 * A tipjar document for this agent, describing means for payment and
	 * reward.
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/tipjar")
	Set<Document> getFoafTipjar();

	/**
	 * A tipjar document for this agent, describing means for payment and
	 * reward.
	 * 
	 * @generated
	 */
	void setFoafTipjar(Set<? extends Document> foafTipjar);

	/**
	 * A thing of interest to this person.
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/topic_interest")
	Set<Thing> getFoafTopic_interest();

	/**
	 * A thing of interest to this person.
	 * 
	 * @generated
	 */
	void setFoafTopic_interest(Set<? extends Thing> foafTopic_interest);

	/**
	 * A weblog of some thing (whether person, group, company etc.).
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/weblog")
	Set<Document> getFoafWeblog();

	/**
	 * A weblog of some thing (whether person, group, company etc.).
	 * 
	 * @generated
	 */
	void setFoafWeblog(Set<? extends Document> foafWeblog);

	/**
	 * A Yahoo chat ID
	 * 
	 * @generated
	 */
	@Iri("http://xmlns.com/foaf/0.1/yahooChatID")
	Set<String> getFoafYahooChatID();

	/**
	 * A Yahoo chat ID
	 * 
	 * @generated
	 */
	void setFoafYahooChatID(Set<? extends String> foafYahooChatID);

}
