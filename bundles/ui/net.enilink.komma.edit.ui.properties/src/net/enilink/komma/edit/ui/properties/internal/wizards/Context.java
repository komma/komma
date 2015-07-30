package net.enilink.komma.edit.ui.properties.internal.wizards;

import net.enilink.vocab.owl.AnnotationProperty;
import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.URI;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;

class Context {
	IAdapterFactory adapterFactory;
	IUnitOfWork unitOfWork;

	IResource subject;
	IProperty predicate;
	Object object;

	String objectLabel;
	URI objectType;
	String objectLanguage;

	void clearObject() {
		object = null;
		objectLabel = null;
		objectLanguage = null;
		objectType = null;
	}

	boolean isDatatypeProperty() {
		return predicate instanceof DatatypeProperty
				|| predicate instanceof AnnotationProperty;
	}
}
