package net.enilink.vocab.foaf;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

public interface FOAF {
	public static final String NAMESPACE = "http://xmlns.com/foaf/0.1/";
	public static final URI NAMESPACE_URI = URIs.createURI(NAMESPACE);

	public static final URI TYPE_LABELPROPERTY = NAMESPACE_URI.appendLocalPart("LabelProperty");

	public static final URI TYPE_PERSON = NAMESPACE_URI.appendLocalPart("Person");

	public static final URI TYPE_AGENT = NAMESPACE_URI.appendLocalPart("Agent");

	public static final URI TYPE_DOCUMENT = NAMESPACE_URI.appendLocalPart("Document");

	public static final URI TYPE_ORGANIZATION = NAMESPACE_URI.appendLocalPart("Organization");

	public static final URI TYPE_GROUP = NAMESPACE_URI.appendLocalPart("Group");

	public static final URI TYPE_PROJECT = NAMESPACE_URI.appendLocalPart("Project");

	public static final URI TYPE_IMAGE = NAMESPACE_URI.appendLocalPart("Image");

	public static final URI TYPE_PERSONALPROFILEDOCUMENT = NAMESPACE_URI.appendLocalPart("PersonalProfileDocument");

	public static final URI TYPE_ONLINEACCOUNT = NAMESPACE_URI.appendLocalPart("OnlineAccount");

	public static final URI TYPE_ONLINEGAMINGACCOUNT = NAMESPACE_URI.appendLocalPart("OnlineGamingAccount");

	public static final URI TYPE_ONLINEECOMMERCEACCOUNT = NAMESPACE_URI.appendLocalPart("OnlineEcommerceAccount");

	public static final URI TYPE_ONLINECHATACCOUNT = NAMESPACE_URI.appendLocalPart("OnlineChatAccount");

	public static final URI PROPERTY_GENDER = NAMESPACE_URI.appendLocalPart("gender");

	public static final URI PROPERTY_PRIMARYTOPIC = NAMESPACE_URI.appendLocalPart("primaryTopic");

	public static final URI PROPERTY_BIRTHDAY = NAMESPACE_URI.appendLocalPart("birthday");

	public static final URI PROPERTY_AGE = NAMESPACE_URI.appendLocalPart("age");

	public static final URI PROPERTY_MBOX = NAMESPACE_URI.appendLocalPart("mbox");

	public static final URI PROPERTY_BASED_NEAR = NAMESPACE_URI.appendLocalPart("based_near");

	public static final URI PROPERTY_PHONE = NAMESPACE_URI.appendLocalPart("phone");

	public static final URI PROPERTY_HOMEPAGE = NAMESPACE_URI.appendLocalPart("homepage");

	public static final URI PROPERTY_WEBLOG = NAMESPACE_URI.appendLocalPart("weblog");

	public static final URI PROPERTY_OPENID = NAMESPACE_URI.appendLocalPart("openid");

	public static final URI PROPERTY_TIPJAR = NAMESPACE_URI.appendLocalPart("tipjar");

	public static final URI PROPERTY_MADE = NAMESPACE_URI.appendLocalPart("made");

	public static final URI PROPERTY_MAKER = NAMESPACE_URI.appendLocalPart("maker");

	public static final URI PROPERTY_IMG = NAMESPACE_URI.appendLocalPart("img");

	public static final URI PROPERTY_DEPICTION = NAMESPACE_URI.appendLocalPart("depiction");

	public static final URI PROPERTY_DEPICTS = NAMESPACE_URI.appendLocalPart("depicts");

	public static final URI PROPERTY_THUMBNAIL = NAMESPACE_URI.appendLocalPart("thumbnail");

	public static final URI PROPERTY_WORKPLACEHOMEPAGE = NAMESPACE_URI.appendLocalPart("workplaceHomepage");

	public static final URI PROPERTY_WORKINFOHOMEPAGE = NAMESPACE_URI.appendLocalPart("workInfoHomepage");

	public static final URI PROPERTY_SCHOOLHOMEPAGE = NAMESPACE_URI.appendLocalPart("schoolHomepage");

	public static final URI PROPERTY_KNOWS = NAMESPACE_URI.appendLocalPart("knows");

	public static final URI PROPERTY_INTEREST = NAMESPACE_URI.appendLocalPart("interest");

	public static final URI PROPERTY_TOPIC_INTEREST = NAMESPACE_URI.appendLocalPart("topic_interest");

	public static final URI PROPERTY_PUBLICATIONS = NAMESPACE_URI.appendLocalPart("publications");

	public static final URI PROPERTY_CURRENTPROJECT = NAMESPACE_URI.appendLocalPart("currentProject");

	public static final URI PROPERTY_PASTPROJECT = NAMESPACE_URI.appendLocalPart("pastProject");

	public static final URI PROPERTY_FUNDEDBY = NAMESPACE_URI.appendLocalPart("fundedBy");

	public static final URI PROPERTY_LOGO = NAMESPACE_URI.appendLocalPart("logo");

	public static final URI PROPERTY_TOPIC = NAMESPACE_URI.appendLocalPart("topic");

	public static final URI PROPERTY_FOCUS = NAMESPACE_URI.appendLocalPart("focus");

	public static final URI PROPERTY_PAGE = NAMESPACE_URI.appendLocalPart("page");

	public static final URI PROPERTY_THEME = NAMESPACE_URI.appendLocalPart("theme");

	public static final URI PROPERTY_ACCOUNT = NAMESPACE_URI.appendLocalPart("account");

	public static final URI PROPERTY_HOLDSACCOUNT = NAMESPACE_URI.appendLocalPart("holdsAccount");

	public static final URI PROPERTY_ACCOUNTSERVICEHOMEPAGE = NAMESPACE_URI.appendLocalPart("accountServiceHomepage");

	public static final URI PROPERTY_MEMBER = NAMESPACE_URI.appendLocalPart("member");

	public static final URI PROPERTY_ISPRIMARYTOPICOF = NAMESPACE_URI.appendLocalPart("isPrimaryTopicOf");

	public static final URI PROPERTY_STATUS = NAMESPACE_URI.appendLocalPart("status");

	public static final URI PROPERTY_SHA1 = NAMESPACE_URI.appendLocalPart("sha1");

	public static final URI PROPERTY_NICK = NAMESPACE_URI.appendLocalPart("nick");

	public static final URI PROPERTY_MBOX_SHA1SUM = NAMESPACE_URI.appendLocalPart("mbox_sha1sum");

	public static final URI PROPERTY_GEEKCODE = NAMESPACE_URI.appendLocalPart("geekcode");

	public static final URI PROPERTY_DNACHECKSUM = NAMESPACE_URI.appendLocalPart("dnaChecksum");

	public static final URI PROPERTY_TITLE = NAMESPACE_URI.appendLocalPart("title");

	public static final URI PROPERTY_JABBERID = NAMESPACE_URI.appendLocalPart("jabberID");

	public static final URI PROPERTY_AIMCHATID = NAMESPACE_URI.appendLocalPart("aimChatID");

	public static final URI PROPERTY_SKYPEID = NAMESPACE_URI.appendLocalPart("skypeID");

	public static final URI PROPERTY_ICQCHATID = NAMESPACE_URI.appendLocalPart("icqChatID");

	public static final URI PROPERTY_YAHOOCHATID = NAMESPACE_URI.appendLocalPart("yahooChatID");

	public static final URI PROPERTY_MSNCHATID = NAMESPACE_URI.appendLocalPart("msnChatID");

	public static final URI PROPERTY_NAME = NAMESPACE_URI.appendLocalPart("name");

	public static final URI PROPERTY_FIRSTNAME = NAMESPACE_URI.appendLocalPart("firstName");

	public static final URI PROPERTY_LASTNAME = NAMESPACE_URI.appendLocalPart("lastName");

	public static final URI PROPERTY_GIVENNAME = NAMESPACE_URI.appendLocalPart("givenname");

	public static final URI PROPERTY_SURNAME = NAMESPACE_URI.appendLocalPart("surname");

	public static final URI PROPERTY_FAMILY_NAME = NAMESPACE_URI.appendLocalPart("family_name");

	public static final URI PROPERTY_FAMILYNAME = NAMESPACE_URI.appendLocalPart("familyName");

	public static final URI PROPERTY_PLAN = NAMESPACE_URI.appendLocalPart("plan");

	public static final URI PROPERTY_MYERSBRIGGS = NAMESPACE_URI.appendLocalPart("myersBriggs");

	public static final URI PROPERTY_ACCOUNTNAME = NAMESPACE_URI.appendLocalPart("accountName");

	public static final URI PROPERTY_MEMBERSHIPCLASS = NAMESPACE_URI.appendLocalPart("membershipClass");

}
