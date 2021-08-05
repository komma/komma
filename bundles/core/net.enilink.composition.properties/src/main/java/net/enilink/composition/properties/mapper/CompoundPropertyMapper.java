package net.enilink.composition.properties.mapper;

import net.enilink.composition.mapping.IPropertyMapper;
import net.enilink.composition.mapping.PropertyDescriptor;

import java.util.*;

/**
 * A property mapper that uses a map of classes to mappers or a default mapper
 * to determine the mapped properties of a specific class.
 */
public class CompoundPropertyMapper implements IPropertyMapper {
	final Map<Class<?>, IPropertyMapper> propertyMappers;
	final IPropertyMapper defaultMapper;

	public CompoundPropertyMapper(Map<Class<?>, IPropertyMapper> propertyMappers, IPropertyMapper defaultMapper) {
		this.propertyMappers = propertyMappers;
		this.defaultMapper = defaultMapper;
	}

	@Override
	public Collection<PropertyDescriptor> getProperties(Class<?> concept) {
		IPropertyMapper mapper = null;
		if (!propertyMappers.isEmpty()) {
			Class<?> c = concept;
			while (mapper == null) {
				// walk superclasses to determine mapper
				// if c is an interface or Object then superclass is null
				if (c == null) break;
				mapper = propertyMappers.get(c);
				c = c.getSuperclass();
			}
			if (mapper == null) {
				// walk interfaces in BFS order
				Set<Class<?>> seen = new HashSet<>();
				Queue<Class<?>> queue = new LinkedList<>();
				queue.addAll(Arrays.asList(concept.getInterfaces()));
				while (!queue.isEmpty()) {
					Class<?> face = queue.remove();
					if (seen.add(face)) {
						mapper = propertyMappers.get(face);
						if (mapper != null) break;
						queue.addAll(Arrays.asList(face.getInterfaces()));
					}
				}
			}
		}
		if (mapper == null) {
			// user default mapper if no specialized mapper was found
			mapper = defaultMapper;
		}
		return mapper.getProperties(concept);
	}
}
