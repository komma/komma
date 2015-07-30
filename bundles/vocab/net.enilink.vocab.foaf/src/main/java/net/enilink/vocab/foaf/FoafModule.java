package net.enilink.vocab.foaf;

import net.enilink.komma.core.KommaModule;

public class FoafModule extends KommaModule {
	{
		addConcept(net.enilink.vocab.foaf.Agent.class);
		addConcept(net.enilink.vocab.foaf.Document.class);
		addConcept(net.enilink.vocab.foaf.Group.class);
		addConcept(net.enilink.vocab.foaf.Image.class);
		addConcept(net.enilink.vocab.foaf.LabelProperty.class);
		addConcept(net.enilink.vocab.foaf.OnlineAccount.class);
		addConcept(net.enilink.vocab.foaf.OnlineChatAccount.class);
		addConcept(net.enilink.vocab.foaf.OnlineEcommerceAccount.class);
		addConcept(net.enilink.vocab.foaf.OnlineGamingAccount.class);
		addConcept(net.enilink.vocab.foaf.Organization.class);
		addConcept(net.enilink.vocab.foaf.Person.class);
		addConcept(net.enilink.vocab.foaf.PersonalProfileDocument.class);
		addConcept(net.enilink.vocab.foaf.Project.class);
	}
}