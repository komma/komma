/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.enilink.komma.rdfs.edit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.edit.provider.ReflectiveItemProviderAdapterFactory;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.URI;

/**
 * This is the factory that is used to provide the interfaces needed to support
 * Viewers.
 */
public class RDFSItemProviderAdapterFactory extends
		ReflectiveItemProviderAdapterFactory {
	public RDFSItemProviderAdapterFactory() {
		super(RDFSEditPlugin.INSTANCE, RDFS.NAMESPACE_URI, RDF.NAMESPACE_URI);
	}

	protected RDFSItemProviderAdapterFactory(IResourceLocator resourceLocator,
			URI... namespaceURIs) {
		super(resourceLocator, namespaceURIs);
	}

	@Override
	protected Collection<IClass> getTypes(Object object) {
		// classes
		if (object instanceof IClass) {
			return Arrays.asList(((IEntity) object).getEntityManager().find(
					RDFS.TYPE_CLASS, IClass.class));
		}
		if (object instanceof IProperty) {
			return Arrays.asList(((IEntity) object).getEntityManager().find(
					RDF.TYPE_PROPERTY, IClass.class));
		}
		// others
		Set<IClass> classes = ((IResource) object).getDirectNamedClasses()
				.toSet();
		return classes;
	}

	@Override
	protected Object createItemProvider(Object object,
			Collection<IClass> types, Object providerType) {
		if (object instanceof IClass) {
			return new RDFSClassItemProvider(this, resourceLocator, types);
		}
		if (object instanceof IProperty) {
			return new RDFSPropertyItemProvider(this, resourceLocator, types);
		}
		return null;
	}
}
