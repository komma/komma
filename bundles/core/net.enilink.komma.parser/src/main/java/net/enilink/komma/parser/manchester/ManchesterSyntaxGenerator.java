package net.enilink.komma.parser.manchester;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.vocab.owl.DataRange;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.owl.Ontology;
import net.enilink.vocab.owl.Restriction;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.Class;
import net.enilink.vocab.rdfs.Datatype;
import net.enilink.vocab.xmlschema.XMLSCHEMA;

/**
 * Generator for the
 * <a href="http://www.w3.org/2007/OWL/wiki/ManchesterSyntax">Manchester OWL
 * Syntax</a>.
 */
public class ManchesterSyntaxGenerator {
	protected static final Comparator<? super IReference> REF_COMPARATOR = new Comparator<IReference>() {
		@Override
		public int compare(IReference o1, IReference o2) {
			if (o1 == null) {
				if (o2 == null) {
					return 0;
				}
				return -1;
			} else if (o2 == null) {
				return 1;
			}
			return o1.toString().compareTo(o2.toString());
		}
	};

	protected int indent = 0;
	protected static final String SPARQL_PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> PREFIX owl: <http://www.w3.org/2002/07/owl#> ";
	static final String FACET_QUERY = createFacetQuery();

	protected void indent() {
		indent += 2;
	}

	protected void dedent() {
		indent -= 2;
	}

	protected void withIndent(Runnable runnable) {
		indent();
		runnable.run();
		dedent();
	}

	protected static String createFacetQuery() {
		StringBuilder sb = new StringBuilder("PREFIX xsd: <" + XMLSCHEMA.NAMESPACE + ">\n");
		sb.append("select ?facet ?value where {\n");
		Iterator<String> facets = Arrays.asList("length", "minLength", "maxLength", "pattern", "langPattern",
				"minInclusive", "minExclusive", "maxInclusive", "maxExclusive").iterator();
		while (facets.hasNext()) {
			String facet = "xsd:" + facets.next();
			sb.append("\t{ ?s ").append(facet).append(" ?value\n");
			sb.append("\tbind ( ").append(facet).append(" as ?facet ) }\n");
			if (facets.hasNext()) {
				sb.append("\tunion\n");
			}
		}
		sb.append("} limit 1");
		return sb.toString();
	}

	// maps XSD facets to shorthand notations
	static final Map<String, String> FACET_SHORTHANDS = new HashMap<String, String>();

	static {
		FACET_SHORTHANDS.put("minInclusive", "<=");
		FACET_SHORTHANDS.put("minExclusive", "<");
		FACET_SHORTHANDS.put("maxInclusive", ">=");
		FACET_SHORTHANDS.put("maxExclusive", ">");
	}

	public String generateText(Object object) {
		if (object instanceof Class) {
			return clazz((Class) object, 0).toString();
		}
		return value(object).toString();
	}

	protected ManchesterSyntaxGenerator append(Object token) {
		if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
			for (int i = 0; i < indent; i++) {
				sb.append(' ');
			}
		}
		sb.append(token);
		return this;
	}

	protected StringBuilder sb = new StringBuilder();

	public String ontologyDocument(Ontology ontology) {
		IEntityManager em = ontology.getEntityManager();
		try (IExtendedIterator<INamespace> it = em.getNamespaces()) {
			if (it.hasNext()) {
				it.forEach(ns -> prefix(ns));
			}
			append("\n");
		}

		ontology(ontology);

		IQuery<?> query = em.createQuery(SPARQL_PREFIX + //
				"SELECT DISTINCT ?class WHERE { " + //
				"{ ?class a rdfs:Class } UNION { ?class a owl:Class } " + //
				"graph ?g { ?class ?someP ?someO }" + //
				"FILTER isIRI(?class) . " + //
				"} ORDER BY ?class");
		query.setParameter("g", ontology);
		try (IExtendedIterator<Class> it = query.evaluate(Class.class)) {
			it.forEach(this::classDefinition);
		}
		return sb.toString();
	}

	protected ManchesterSyntaxGenerator prefix(INamespace ns) {
		append("Prefix: " + ns.getPrefix() + ":");
		value(ns.getURI());
		return append("\n");
	}

	protected ManchesterSyntaxGenerator ontology(Ontology ontology) {
		append("Ontology: ");
		value(ontology);
		append("\n");
		withIndent(() -> {
			ontology.getOwlImports().stream().sorted(REF_COMPARATOR).forEachOrdered(this::ontologyImport);
		});
		return append("\n");
	}

	protected ManchesterSyntaxGenerator ontologyImport(Ontology ontology) {
		append("Import: ");
		value(ontology);
		return append("\n");
	}

	protected ManchesterSyntaxGenerator classDefinition(Class clazz) {
		append("Class: ");
		value(clazz);
		append("\n");

		withIndent(() -> {
			classŔelation("SubClassOf", clazz.getRdfsSubClassOf());
			classŔelation("EquivalentTo", ((net.enilink.vocab.owl.Class) clazz).getOwlEquivalentClasses());
			classŔelation("DisjointWith", ((net.enilink.vocab.owl.Class) clazz).getOwlDisjointWith());
			// classŔelation("DisjointUnionOf",
			// ((net.enilink.vocab.owl.Class)clazz).getOwlDisjointUnionOf());
		});
		classDefinitionExt(clazz);
		return this;
	}

	protected void classŔelation(String key, Collection<? extends Class> elements) {
		List<Class> named = new ArrayList<>();
		List<Class> anonymous = new ArrayList<>();
		Map<IReference, List<Restriction>> restrictions = new HashMap<>();
		elements.forEach(c -> {
			if (OWL.TYPE_THING.equals(c)) {
				// ignore, this is implicit knowledge
			} else if (c.getURI() != null) {
				named.add(c);
			} else {
				if (c instanceof Restriction) {
					Restriction r = (Restriction) c;
					// group by property
					restrictions.compute(r.getOwlOnProperty(), (k, v) -> {
						if (v == null) {
							v = new ArrayList<>();
						}
						v.add(r);
						return v;
					});
				} else {
					anonymous.add(c);
				}
			}
		});

		appendKey(key, named, this::clazz);
		appendKey(key, anonymous, this::clazz);
		restrictions.entrySet().stream().sorted((a, b) -> REF_COMPARATOR.compare(a.getKey(), b.getKey()))
				.forEachOrdered(entry -> {
					appendKey(key, entry.getValue(), this::clazz);
				});
	}

	private <T> void appendKey(String key, List<T> elements, Consumer<T> func) {
		if (!elements.isEmpty()) {
			append(key + ": ");
			Iterator<T> it = elements.iterator();
			while (it.hasNext()) {
				func.accept(it.next());
				if (it.hasNext()) {
					append(", ");
				}
			}
			append("\n");
		}
	}

	protected void classDefinitionExt(Class clazz) {
	}

	protected ManchesterSyntaxGenerator clazz(Class clazz) {
		return clazz(clazz, 0);
	}

	protected ManchesterSyntaxGenerator clazz(Class clazz, int prio) {
		if (clazz.getURI() == null) {
			if (clazz instanceof Restriction) {
				return restriction((Restriction) clazz, prio);
			} else if (clazz instanceof Datatype && ((Datatype) clazz).getOwlOnDatatype() != null) {
				append(toString(((Datatype) clazz).getOwlOnDatatype()));
				return datatypeRestrictions(((Datatype) clazz).getOwlWithRestrictions());
			} else if (clazz instanceof net.enilink.vocab.owl.Class) {
				net.enilink.vocab.owl.Class owlClass = (net.enilink.vocab.owl.Class) clazz;
				if (owlClass.getOwlUnionOf() != null) {
					return setOfClasses(owlClass.getOwlUnionOf(), "or", 1, prio);
				} else if (owlClass.getOwlIntersectionOf() != null) {
					return setOfClasses(owlClass.getOwlIntersectionOf(), "and", 2, prio);
				} else if (owlClass.getOwlComplementOf() != null) {
					append("not ");
					return clazz(owlClass.getOwlComplementOf(), 3);
				} else if (owlClass.getOwlOneOf() != null) {
					return list(owlClass.getOwlOneOf());
				}
			}
		}

		return value(clazz);
	}

	/**
	 * Converts a list of datatype restrictions to a Manchester expression in the
	 * form [facet1 value1, facet2 value2, ...]
	 * 
	 * Example: xsd:int[>=18]
	 * 
	 * @param list
	 *            The list of datatype restrictions which are expressed using XML
	 *            Schema facets.
	 * 
	 * @return The generator instance.
	 */
	protected ManchesterSyntaxGenerator datatypeRestrictions(List<?> list) {
		if (list != null) {
			append("[");
			Iterator<? extends Object> it = list.iterator();
			while (it.hasNext()) {
				IEntity dtRestriction = (IEntity) it.next();
				for (IBindings<?> bindings : dtRestriction.getEntityManager().createQuery(FACET_QUERY)
						.setParameter("s", dtRestriction).evaluate(IBindings.class)) {
					IReference facet = (IReference) bindings.get("facet");
					String facetShortHand = FACET_SHORTHANDS.get(facet.getURI().localPart());
					if (facetShortHand == null) {
						facetShortHand = facet.getURI().localPart();
					}
					append(facetShortHand).append(toString(bindings.get("value")));
					if (it.hasNext()) {
						append(", ");
					}
				}
			}
			append("]");
		}
		return this;
	}

	protected ManchesterSyntaxGenerator dataRange(DataRange dataRange) {
		return clazz(dataRange, 0);
	}

	private ManchesterSyntaxGenerator list(Collection<? extends Object> list) {
		append("{");
		Iterator<? extends Object> it = list.iterator();
		while (it.hasNext()) {
			value(it.next());
			if (it.hasNext()) {
				append(", ").append(" ");
			}
		}
		append("}");
		return this;
	}

	protected ManchesterSyntaxGenerator onClassOrDataRange(Restriction restriction) {
		if (restriction.getOwlOnClass() != null) {
			return clazz(restriction.getOwlOnClass(), 0);
		} else if (restriction.getOwlOnDataRange() != null) {
			return dataRange(restriction.getOwlOnDataRange());
		}
		return this;
	}

	public ManchesterSyntaxGenerator restriction(Restriction restriction, int prio) {
		if (restriction.getURI() == null) {
			int operatorPrio = 4;
			if (restriction.getOwlOnProperty() != null) {
				if (prio >= operatorPrio) {
					append("(");
				}
				value(restriction.getOwlOnProperty());
			} else if (restriction.getOwlOnProperties() != null) {
				if (prio >= operatorPrio) {
					append("(");
				}
				// TODO How is this correctly represented as manchester syntax?
				list(restriction.getOwlOnProperties());
			} else {
				// this is an invalid restriction, since target properties are
				// missing, so just return the name of this restriction
				return value(restriction);
			}

			append(" ");

			if (restriction.getOwlAllValuesFrom() != null) {
				append("only").append(" ");
				clazz(restriction.getOwlAllValuesFrom(), operatorPrio);
			} else if (restriction.getOwlSomeValuesFrom() != null) {
				append("some").append(" ");
				clazz(restriction.getOwlSomeValuesFrom(), operatorPrio);
			} else if (restriction.getOwlMaxCardinality() != null) {
				append("max").append(" ");
				append(restriction.getOwlMaxCardinality());
			} else if (restriction.getOwlMinCardinality() != null) {
				append("min").append(" ");
				append(restriction.getOwlMinCardinality());
			} else if (restriction.getOwlCardinality() != null) {
				append("exactly").append(" ");
				append(restriction.getOwlCardinality());
			} else if (restriction.getOwlMaxQualifiedCardinality() != null) {
				append("max").append(" ");
				append(restriction.getOwlMaxQualifiedCardinality()).append(" ");
				onClassOrDataRange(restriction);
			} else if (restriction.getOwlMinQualifiedCardinality() != null) {
				append("min").append(" ");
				append(restriction.getOwlMinQualifiedCardinality()).append(" ");
				onClassOrDataRange(restriction);
			} else if (restriction.getOwlQualifiedCardinality() != null) {
				append("exactly").append(" ");
				append(restriction.getOwlQualifiedCardinality()).append(" ");
				onClassOrDataRange(restriction);
			} else if (restriction.getOwlHasValue() != null) {
				append("value").append(" ");
				value(restriction.getOwlHasValue());
			}
			if (prio >= operatorPrio) {
				append(")");
			}
			return this;
		}
		return value(restriction);
	}

	protected ManchesterSyntaxGenerator setOfClasses(List<? extends Class> set, String operator, int operatorPrio,
			int prio) {
		Iterator<? extends Class> it = set.iterator();
		if (operatorPrio < prio && set.size() > 1) {
			append("(");
		}
		while (it.hasNext()) {
			clazz(it.next(), operatorPrio);
			if (it.hasNext()) {
				append(" ").append(operator).append(" ");
			}
		}
		if (operatorPrio < prio && set.size() > 1) {
			append(")");
		}
		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	protected String escapeLiteral(String label) {
		return label.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r").replace("\"",
				"\\\"");
	}

	protected ManchesterSyntaxGenerator value(Object value) {
		if (value instanceof ILiteral) {
			ILiteral literal = (ILiteral) value;
			boolean quoted = XMLSCHEMA.TYPE_STRING.equals(literal.getDatatype())
					|| RDF.TYPE_LANGSTRING.equals(literal.getDatatype()) || literal.getDatatype() == null;
			if (quoted) {
				append("\"");
			}
			append(escapeLiteral(literal.getLabel()));
			if (quoted) {
				append("\"");
			}
			if (literal.getLanguage() != null) {
				append("@").append(toString(literal.getLanguage()));
			} else if (literal.getDatatype() != null) {
				append("^^").append(toString(literal.getDatatype()));
			}
		} else {
			append(toString(value));
		}
		return this;
	}

	protected String getPrefix(IReference reference) {
		if (reference instanceof IEntity) {
			return ((IEntity) reference).getEntityManager().getPrefix(reference.getURI().namespace());
		}
		return null;
	}

	protected String toString(Object value) {
		if (value instanceof IReference) {
			URI uri = ((IReference) value).getURI();
			if (uri != null) {
				String prefix = getPrefix((IReference) value);
				String localPart = uri.localPart();
				boolean hasLocalPart = localPart != null && localPart.length() > 0;
				StringBuilder text = new StringBuilder();
				if (prefix != null && prefix.length() > 0 && hasLocalPart) {
					text.append(prefix).append(":");
				}
				if (hasLocalPart && prefix != null) {
					text.append(localPart);
				} else {
					text.append("<").append(uri.toString()).append(">");
				}
				return text.toString();
			}
		}
		return String.valueOf(value);
	}
}
