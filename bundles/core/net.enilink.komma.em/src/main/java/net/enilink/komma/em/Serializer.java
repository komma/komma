package net.enilink.komma.em;

import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Injector;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.util.Pair;
import net.enilink.composition.ClassResolver;
import net.enilink.composition.ObjectFactory;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.mapping.IPropertyMapper;
import net.enilink.composition.mapping.PropertyAttribute;
import net.enilink.composition.mapping.PropertyDescriptor;
import net.enilink.composition.properties.exceptions.ObjectConversionException;
import net.enilink.komma.core.*;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.literals.LiteralConverter;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.xmlschema.XMLSCHEMA;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Converts Java objects to RDF and vice-versa.
 */
public class Serializer {
	private Map<Object, IReference> seen = new MapMaker().weakKeys().makeMap();

	private Map<Class<?>, Collection<PropertyDescriptor>> properties = new HashMap<>();

	private RoleMapper<URI> roleMapper;

	@Inject(optional = true)
	private ClassResolver<URI> classResolver;

	@Inject
	private Injector injector;

	private IPropertyMapper propertyMapper;

	private LiteralConverter literalConverter;

	private Locale locale;

	public IValue serialize(Object instance, Consumer<IStatement> sink) {
		return toValue(instance, sink);
	}

	public Object read(IStatementSource source, IReference id, ObjectFactory<URI> objectFactory) {
		List<URI> types = source.match(id, RDF.PROPERTY_TYPE, null, true)
				.filterKeep(stmt -> stmt.getObject() instanceof URI)
				.mapWith(stmt -> (URI) stmt.getObject()).toList();
		Object instance;
		if (objectFactory != null) {
			instance = objectFactory.createObject(types);
		} else {
			instance = injector.getInstance(classResolver.resolveComposite(types));
		}
		for (PropertyDescriptor pd : getProperties(instance.getClass())) {
			if (pd.getWriteMethod() == null) {
				continue;
			}

			URI predicate = URIs.createURI(pd.getPredicate());
			try {
				Class<?> valueClass = pd.getPropertyType();
				if (Set.class.isAssignableFrom(valueClass) ||
						Collection.class.isAssignableFrom(valueClass) ||
						pd.isEnforceList()) {
					Collection<Object> values;
					if (List.class.isAssignableFrom(valueClass)) {
						values = new ArrayList<>();
					} else {
						values = new HashSet<>();
					}
					IExtendedIterator<Object> rdfValues = source.match(id, predicate, null, true)
							.mapWith(stmt -> stmt.getObject());
					for (Object rdfValue : rdfValues) {
						values.add(toJava(source, rdfValue, objectFactory));
					}
					// set values on instance
					pd.getWriteMethod().invoke(instance, values);
				} else {
					try (IExtendedIterator<Object> rdfValues = source.match(id, predicate, null, true)
							.mapWith(stmt -> stmt.getObject())) {
						Object rdfValue = rdfValues.hasNext() ? rdfValues.next() : null;

						Object value;
						if (rdfValue == null) {
							value = null;
						} else if (List.class.isAssignableFrom(valueClass)) {
							// the values are either an rdf:List or some kind of RDF collection
							String rdfValueType = Arrays.stream(pd.getAttributes())
									.filter(a -> PropertyAttribute.TYPE.equals(a.getName()))
									.findFirst().map(a -> a.getValue()).get();
							if (rdfValueType != null) {
								if (RDF.TYPE_LIST.toString().equals(rdfValueType)) {
									value = createJavaList(source, (IReference) rdfValue, objectFactory);
								} else {
									value = createJavaContainer(source, (IReference) rdfValue, objectFactory);
								}
							} else {
								// TODO decide type based on existing statements rdf:first, rdf:rest etc.
								value = createJavaList(source, (IReference) rdfValue, objectFactory);
							}
						} else {
							value = toJava(source, rdfValue, objectFactory);
						}
						// set value on instance
						pd.getWriteMethod().invoke(instance, value);
					}
				}
			} catch (IllegalAccessException e) {
				throw new KommaException(e);
			} catch (InvocationTargetException e) {
				throw new KommaException(e);
			}
		}
		return instance;
	}

	protected Object toJava(IStatementSource source, Object value, ObjectFactory<URI> objectFactory) {
		if (value instanceof ILiteral) {
			return literalConverter.createObject((ILiteral) value);
		} else if (value instanceof IReference) {
			return read(source, (IReference) value, objectFactory);
		} else {
			return value;
		}
	}

	protected boolean isLocalized(PropertyAttribute[] attributes) {
		for (PropertyAttribute attribute : attributes) {
			if (PropertyAttribute.LOCALIZED.equals(attribute.getName())) {
				return true;
			}
		}
		return false;
	}

	protected IValue toValue(Object instance, Consumer<IStatement> sink, PropertyAttribute... attributes) {
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
				sink.accept(new Statement(name, RDF.PROPERTY_TYPE, rdfType));
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
							IValue rdfValue = toValue(listElement, sink);
							sink.accept(new Statement(name, predicate, rdfValue));
						}
					} else if (List.class.isAssignableFrom(valueClass)) {
						// the values are either an rdf:List or some kind of RDF collection
						String rdfValueType = Arrays.stream(attributes)
								.filter(a -> PropertyAttribute.TYPE.equals(a.getName()))
								.findFirst().map(a -> a.getValue()).get();
						if (rdfValueType != null) {
							if (RDF.TYPE_LIST.toString().equals(rdfValueType)) {
								createRdfList((List<?>) value, sink);
							} else {
								createRdfContainer(URIs.createURI(rdfValueType), (List<?>) value, sink);
							}
						} else {
							createRdfList((List<?>) value, sink);
						}
					} else {
						// this is just a single value
						IValue rdfValue = toValue(value, sink);
						sink.accept(new Statement(name, URIs.createURI(pd.getPredicate()), rdfValue));
					}
				} catch (IllegalAccessException e) {
					throw new KommaException(e);
				} catch (InvocationTargetException e) {
					throw new KommaException(e);
				}
			}
			return name;
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

	protected List<Object> createJavaList(IStatementSource source, IReference list, ObjectFactory<URI> objectFactory) {
		List<Object> items = new ArrayList<>();
		Set<IReference> seen = new HashSet<>();
		while (list != null && seen.add(list) && !RDF.NIL.equals(list)) {
			IReference rest = null;
			IValue first = null;
			for (IStatement stmt : source.match(list, null, null, true)) {
				if (RDF.PROPERTY_FIRST.equals(stmt.getPredicate())) {
					first = (IValue) stmt.getObject();
				} else if (RDF.PROPERTY_REST.equals(stmt.getPredicate())) {
					if (stmt.getObject() instanceof IReference) {
						rest = (IReference) stmt.getObject();
					} else {
						// invalid list data
						break;
					}
				}
			}
			if (first != null) {
				// convert RDF value to Java
				items.add(toJava(source, first, objectFactory));
				list = rest;
			}
		}
		return items;
	}

	public IReference createRdfList(List<?> values, Consumer<IStatement> sink) {
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
					sink.accept(new Statement(prev, RDF.PROPERTY_REST, list));
				}
				sink.accept(new Statement(list, RDF.PROPERTY_TYPE, RDF.TYPE_LIST));
				sink.accept(new Statement(list, RDF.PROPERTY_FIRST, toValue(value, sink)));
				prev = list;
			}
			sink.accept(new Statement(prev, RDF.PROPERTY_REST, RDF.NIL));
			return result;
		}
	}

	protected List<Object> createJavaContainer(IStatementSource source, IReference list, ObjectFactory<URI> objectFactory) {
		List<Pair<IStatement, Integer>> values = new ArrayList<>(source.match(list, null, null, true)
				.filterKeep(stmt -> {
					// keep only the container statements
					URI predicate = stmt.getPredicate().getURI();
					return predicate.namespace().equals(RDF.NAMESPACE_URI) && predicate.localPart().matches("_[0-9]+");
				})
				.mapWith(stmt -> new Pair<>(stmt, Integer.valueOf(stmt.getPredicate().getURI().localPart().substring(1))))
				.toList());
		values.sort(Comparator.comparingInt(v -> v.getSecond()));
		if (values.isEmpty()) {
			return new ArrayList<>();
		} else {
			// size is the index of the last element since index are 1-based
			int size = values.get(values.size() - 1).getSecond();
			List<Object> items = new ArrayList<>(size);
			for (Pair<IStatement, Integer> v : values) {
				// this allows indexes without values
				items.set(v.getSecond(), toJava(source, v.getFirst().getObject(), objectFactory));
			}
			return items;
		}
	}

	public IReference createRdfContainer(URI containerType, List<?> values, Consumer<IStatement> sink) {
		IReference c = new BlankNode();
		sink.accept(new Statement(c, RDF.PROPERTY_TYPE, containerType));
		int i = 1;
		for (Object value : values) {
			if (value != null) {
				// only write non-null values to RDF
				sink.accept(new Statement(c, RDF.NAMESPACE_URI.appendLocalPart("_" + i),
						toValue(value, sink)));
			}
			i++;
		}
		return c;
	}

	private boolean isEntity(Class<?> type) {
		if (type == null)
			return false;
		if (properties.containsKey(type))
			return true;
		if (roleMapper.findType(type) != null)
			return true;
		for (Class<?> face : type.getInterfaces()) {
			if (isEntity(face))
				return true;
		}
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

	protected Collection<PropertyDescriptor> getProperties(Class<?> concept) {
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
