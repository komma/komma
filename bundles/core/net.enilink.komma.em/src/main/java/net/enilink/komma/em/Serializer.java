package net.enilink.komma.em;

import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.mapping.IPropertyMapper;
import net.enilink.composition.mapping.PropertyAttribute;
import net.enilink.composition.mapping.PropertyDescriptor;
import net.enilink.composition.properties.exceptions.ObjectConversionException;
import net.enilink.komma.core.*;
import net.enilink.komma.core.visitor.IDataVisitor;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.literals.LiteralConverter;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.xmlschema.XMLSCHEMA;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Converts Java objects into an RDF representation.
 */
public class Serializer {
	private Map<Object, IReference> seen = new MapMaker().weakKeys().makeMap();

	private Map<Class<?>, Collection<PropertyDescriptor>> properties = new HashMap<>();

	private RoleMapper<URI> roleMapper;

	private IPropertyMapper propertyMapper;

	private LiteralConverter literalConverter;

	private Locale locale;

	public <V extends IDataVisitor<?>> IValue serialize(Object instance, V visitor) {
		return toValue(instance, visitor);
	}

	protected boolean isLocalized(PropertyAttribute[] attributes) {
		for (PropertyAttribute attribute : attributes) {
			if (PropertyAttribute.LOCALIZED.equals(attribute.getName())) {
				return true;
			}
		}
		return false;
	}

	protected <V extends IDataVisitor<?>> IValue toValue(Object instance, V visitor, PropertyAttribute... attributes) {
		if (instance == null) {
			return null;
		}

		IReference name = seen.get(instance);
		if (name != null) {
			// this is an entity that was already serialized
			return name;
		}
		Class<?> type = instance.getClass();
		if (literalConverter.isLiteralType(type)) {
			if (isLocalized(attributes)) {
				return new Literal(instance.toString(), locale.getLanguage());
			}
			return literalConverter.createLiteral(instance, null);
		}
		if (IEntity.class.isAssignableFrom(type) || isEntity(type)) {
			if (instance instanceof IReferenceable) {
				name = ((IReferenceable) instance).getReference();
			} else if (instance instanceof IReference) {
				name = (IReference) instance;
			} else {
				// TODO use some kind of name provider
				name = new BlankNode();
			}
			seen.put(instance, name);

			Set<URI> rdfTypes = new HashSet<>();
			if (instance instanceof IResource) {
				// this is already a mapped RDF resource
				for (IReference rdfType : ((IResource) instance).getRdfTypes()) {
					if (rdfType.getURI() != null) {
						rdfTypes.add(rdfType.getURI());
					}
				}
			} else {
				// this is a detached object
				getTypes(instance.getClass(), rdfTypes);
			}
			for (URI rdfType : rdfTypes) {
				visitor.visitStatement(new Statement(name, RDF.PROPERTY_TYPE, rdfType));
			}

			for (PropertyDescriptor pd : getProperties(instance.getClass())) {
				try {
					Object value = pd.getReadMethod().invoke(instance);
					Class<?> valueClass = pd.getPropertyType();
					if (value == null) {
						// do nothing
					} else if (Set.class.isAssignableFrom(valueClass) ||
							Collection.class.isAssignableFrom(valueClass) ||
							pd.isEnforceList()) {
						URI predicate = URIs.createURI(pd.getPredicate());
						for (Object listElement : ((Collection<?>) value)) {
							IValue rdfValue = toValue(listElement, visitor);
							visitor.visitStatement(new Statement(name, predicate, rdfValue));
						}
					} else if (List.class.isAssignableFrom(valueClass)) {
						// the values are either an rdf:List or some kind of RDF collection
						String rdfValueType = Arrays.stream(attributes)
								.filter(a -> PropertyAttribute.TYPE.equals(a.getName()))
								.findFirst().map(a -> a.getValue()).get();
						if (rdfValueType != null) {
							if (RDF.TYPE_LIST.toString().equals(rdfValueType)) {
								createRdfList((List<?>) value, visitor);
							} else {
								createRdfContainer(URIs.createURI(rdfValueType), (List<?>) value, visitor);
							}
						} else {
							createRdfList((List<?>) value, visitor);
						}
					} else {
						// this is just a single value
						IValue rdfValue = toValue(value, visitor);
						visitor.visitStatement(new Statement(name, URIs.createURI(pd.getPredicate()), rdfValue));
					}
				} catch (IllegalAccessException e) {
					throw new KommaException(e);
				} catch (InvocationTargetException e) {
					throw new KommaException(e);
				}
			}
		}
		try {
			// tries to use serialization etc. for unknown instance types
			return literalConverter.createLiteral(instance, null);
		} catch (ObjectConversionException e) {
			// finding a possible mapper failed
		}
		// just convert instance to string
		return literalConverter.createLiteral(String.valueOf(instance), XMLSCHEMA.TYPE_STRING);
	}

	public <V extends IDataVisitor<?>> IReference createRdfList(List<?> values, V visitor) {
		if (values.isEmpty()) {
			return RDF.NIL;
		} else {
			IReference result = null;
			IReference prev = null;
			for (Object value : values) {
				IReference list = new BlankNode();
				if (result == null) {
					result = list;
				}
				if (prev != null) {
					visitor.visitStatement(new Statement(prev, RDF.PROPERTY_REST, list));
				}
				visitor.visitStatement(new Statement(list, RDF.PROPERTY_TYPE, RDF.TYPE_LIST));
				visitor.visitStatement(new Statement(list, RDF.PROPERTY_FIRST, toValue(value, visitor)));
				prev = list;
			}
			visitor.visitStatement(new Statement(prev, RDF.PROPERTY_REST, RDF.NIL));
			return result;
		}
	}

	public <V extends IDataVisitor<?>> IReference createRdfContainer(URI containerType,
																	 List<?> values, V visitor) {
		IReference c = new BlankNode();
		visitor.visitStatement(new Statement(c, RDF.PROPERTY_TYPE, containerType));
		int i = 1;
		for (Object value : values) {
			visitor.visitStatement(new Statement(c, RDF.NAMESPACE_URI.appendLocalPart("_" + i),
					toValue(value, visitor)));
			i++;
		}
		return c;
	}

	private boolean isEntity(Class<?> type) {
		if (type == null)
			return false;
		if (properties.containsKey(type))
			return true;
		for (Class<?> face : type.getInterfaces()) {
			if (roleMapper.findType(face) != null)
				return true;
		}
		if (roleMapper.findType(type) != null)
			return true;
		return isEntity(type.getSuperclass());
	}

	private <C extends Collection<URI>> void getTypes(Class<?> role, C set) {
		URI type = roleMapper.findType(role);
		if (type == null) {
			Class<?> superclass = role.getSuperclass();
			if (superclass != null) {
				getTypes(superclass, set);
			}
			Class<?>[] interfaces = role.getInterfaces();
			for (int i = 0, n = interfaces.length; i < n; i++) {
				getTypes(interfaces[i], set);
			}
		} else {
			set.add(type);
		}
	}

	public Collection<PropertyDescriptor> getProperties(Class<?> concept) {
		Collection<PropertyDescriptor> descriptors = properties.get(concept);
		if (descriptors == null) {
			descriptors = new HashSet<>();
			descriptors.addAll(propertyMapper.getProperties(concept));
			// walk interfaces in BFS order
			Set<Class<?>> seen = new HashSet<>();
			Queue<Class<?>> queue = new LinkedList<>();
			queue.addAll(Arrays.asList(concept.getInterfaces()));
			while (!queue.isEmpty()) {
				Class<?> face = queue.remove();
				if (seen.add(face)) {
					descriptors.addAll(getProperties(face));
					queue.addAll(Arrays.asList(face.getInterfaces()));
				}
			}
			properties.put(concept, descriptors);
		}
		return descriptors;
	}

	@Inject
	protected void setRoleMapper(RoleMapper<URI> mapper) {
		this.roleMapper = mapper;
	}

	@Inject
	public void setPropertyMapper(IPropertyMapper propertyMapper) {
		this.propertyMapper = propertyMapper;
	}

	@Inject
	public void setLiteralConverter(LiteralConverter literalConverter) {
		this.literalConverter = literalConverter;
	}

	@Inject
	public void setLocale(Locale locale) {
		this.locale = locale;
	}
}
