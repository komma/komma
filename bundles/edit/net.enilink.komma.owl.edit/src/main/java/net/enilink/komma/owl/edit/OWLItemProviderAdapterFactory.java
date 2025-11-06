/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.enilink.komma.owl.edit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import net.enilink.composition.annotations.Iri;
import net.enilink.vocab.owl.AnnotationProperty;
import net.enilink.vocab.owl.Class;
import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.owl.ObjectProperty;
import net.enilink.vocab.owl.Ontology;
import net.enilink.komma.rdfs.edit.RDFSItemProviderAdapterFactory;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;

/**
 * This is the factory that is used to provide the interfaces needed to support
 * Viewers.
 */
public class OWLItemProviderAdapterFactory extends
		RDFSItemProviderAdapterFactory {
	public OWLItemProviderAdapterFactory() {
		super(OWLEditPlugin.INSTANCE, URIs.createURI(Class.class
				.getPackage().getAnnotation(Iri.class).value()));
	}

	@Override
	protected Collection<IClass> getTypes(Object object) {
		// classes
		if (object instanceof AnnotationProperty) {
			return Collections.singletonList(((IResource) object).getEntityManager().find(
					OWL.TYPE_ANNOTATIONPROPERTY, IClass.class));
		}
		if (object instanceof DatatypeProperty) {
			return Collections.singletonList(((IResource) object).getEntityManager().find(
					OWL.TYPE_DATATYPEPROPERTY, IClass.class));
		}
		if (object instanceof ObjectProperty) {
			return Collections.singletonList(((IResource) object).getEntityManager().find(
					OWL.TYPE_OBJECTPROPERTY, IClass.class));
		}
		if (object instanceof IClass) {
			return Collections.singletonList(((IResource) object).getEntityManager().find(
					OWL.TYPE_CLASS, IClass.class));
		}
		// others
		return ((IResource) object).getDirectNamedClasses().toSet();
	}

	@Override
	protected Object createItemProvider(Object object,
			Collection<IClass> types, Object providerType) {
		if (object instanceof IProperty) {
			return new OWLPropertyItemProvider(this, resourceLocator, types);
		}
		if (object instanceof IClass) {
			return new OWLClassItemProvider(this, resourceLocator, types);
		}
		if (object instanceof Ontology) {
			return new OWLResourceItemProvider(this, resourceLocator, types);
		}
		return super.createItemProvider(object, types, providerType);
	}
}
