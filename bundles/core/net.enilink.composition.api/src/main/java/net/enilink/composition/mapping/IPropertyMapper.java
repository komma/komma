package net.enilink.composition.mapping;

import java.util.Collection;

public interface IPropertyMapper {
	Collection<PropertyDescriptor> getProperties(Class<?> concept);
}
