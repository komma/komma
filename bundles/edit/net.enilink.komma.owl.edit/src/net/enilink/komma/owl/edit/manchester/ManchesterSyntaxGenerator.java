package net.enilink.komma.owl.edit.manchester;

import java.util.Iterator;
import java.util.List;

import net.enilink.vocab.owl.DataRange;
import net.enilink.vocab.owl.Restriction;
import net.enilink.vocab.rdfs.Class;
import net.enilink.vocab.xmlschema.XMLSCHEMA;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.core.ILiteral;

/**
 * Generator for the <a
 * href="http://www.w3.org/2007/OWL/wiki/ManchesterSyntax">Manchester OWL
 * Syntax</a>.
 * 
 */
public class ManchesterSyntaxGenerator {
	public static String generateText(Object object) {
		if (object instanceof Class) {
			return new ManchesterSyntaxGenerator().clazz((Class) object)
					.toString();
		}
		return ModelUtil.getLabel(object);
	}

	private StringBuilder sb = new StringBuilder();

	private ManchesterSyntaxGenerator clazz(Class clazz) {
		if (clazz.getURI() == null) {
			if (clazz instanceof Restriction) {
				return restriction((Restriction) clazz);
			} else if (clazz instanceof net.enilink.vocab.owl.Class) {
				net.enilink.vocab.owl.Class owlClass = (net.enilink.vocab.owl.Class) clazz;
				if (owlClass.getOwlUnionOf() != null) {
					return setOfClasses(owlClass.getOwlUnionOf(), "or");
				} else if (owlClass.getOwlIntersectionOf() != null) {
					return setOfClasses(owlClass.getOwlIntersectionOf(), "and");
				} else if (owlClass.getOwlComplementOf() != null) {
					sb.append("not ");
					return clazz(owlClass.getOwlComplementOf());
				} else if (owlClass.getOwlOneOf() != null) {
					return list(owlClass.getOwlOneOf());
				}
			}
		}

		return value(clazz);
	}

	private ManchesterSyntaxGenerator dataRange(DataRange dataRange) {
		return clazz(dataRange);
	}

	private ManchesterSyntaxGenerator list(List<? extends Object> list) {
		sb.append("{");
		Iterator<? extends Object> it = list.iterator();
		while (it.hasNext()) {
			value(it.next());
			if (it.hasNext()) {
				sb.append(" ").append(", ").append(" ");
			}
			sb.append("}");
		}
		return this;
	}

	private ManchesterSyntaxGenerator onClassOrDataRange(Restriction restriction) {
		if (restriction.getOwlOnClass() != null) {
			return clazz(restriction.getOwlOnClass());
		} else if (restriction.getOwlOnDataRange() != null) {
			return dataRange(restriction.getOwlOnDataRange());
		}

		return this;
	}

	public ManchesterSyntaxGenerator restriction(Restriction restriction) {
		if (restriction.getURI() == null) {
			if (restriction.getOwlOnProperty() != null) {
				value(restriction.getOwlOnProperty());
			} else if (restriction.getOwlOnProperties() != null) {
				// TODO How is this correctly represented as manchester syntax?
				list(restriction.getOwlOnProperties());
			} else {
				// this is an invalid restriction, since target properties are
				// missing, so just return the name of this restriction
				return value(restriction);
			}

			sb.append(" ");

			if (restriction.getOwlAllValuesFrom() != null) {
				sb.append("only").append(" ");
				clazz(restriction.getOwlAllValuesFrom());
			} else if (restriction.getOwlSomeValuesFrom() != null) {
				sb.append("some").append(" ");
				clazz(restriction.getOwlSomeValuesFrom());
			} else if (restriction.getOwlMaxCardinality() != null) {
				sb.append("max").append(" ");
				sb.append(restriction.getOwlMaxCardinality());
			} else if (restriction.getOwlMinCardinality() != null) {
				sb.append("min").append(" ");
				sb.append(restriction.getOwlMinCardinality());
			} else if (restriction.getOwlCardinality() != null) {
				sb.append("exactly").append(" ");
				sb.append(restriction.getOwlCardinality());
			} else if (restriction.getOwlMaxQualifiedCardinality() != null) {
				sb.append("max").append(" ");
				sb.append(restriction.getOwlMaxQualifiedCardinality()).append(
						" ");
				onClassOrDataRange(restriction);
			} else if (restriction.getOwlMinQualifiedCardinality() != null) {
				sb.append("min").append(" ");
				sb.append(restriction.getOwlMinQualifiedCardinality()).append(
						" ");
				onClassOrDataRange(restriction);
			} else if (restriction.getOwlQualifiedCardinality() != null) {
				sb.append("exactly").append(" ");
				sb.append(restriction.getOwlQualifiedCardinality()).append(" ");
				onClassOrDataRange(restriction);
			} else if (restriction.getOwlHasValue() != null) {
				sb.append("value").append(" ");
				value(restriction.getOwlHasValue());
			}

			return this;
		}
		return value(restriction);
	}

	private ManchesterSyntaxGenerator setOfClasses(List<? extends Class> set,
			String operator) {
		Iterator<? extends Class> it = set.iterator();
		if (set.size() > 1) {
			sb.append("(");
		}
		while (it.hasNext()) {
			clazz(it.next());
			if (it.hasNext()) {
				sb.append(" ").append(operator).append(" ");
			}
		}
		if (set.size() > 1) {
			sb.append(")");
		}
		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	private ManchesterSyntaxGenerator value(Object value) {
		if (value instanceof ILiteral) {
			ILiteral literal = (ILiteral) value;
			boolean quoted = XMLSCHEMA.TYPE_STRING
					.equals(literal.getDatatype())
					|| literal.getDatatype() == null;
			if (quoted) {
				sb.append("\"");
			}
			sb.append(ModelUtil.getLabel(value));
			if (quoted) {
				sb.append("\"");
			}
			if (literal.getDatatype() != null) {
				sb.append("^^").append("<")
						.append(ModelUtil.getLabel(literal.getDatatype()))
						.append(">");
			}
			return this;
		}
		sb.append(ModelUtil.getLabel(value));
		return this;
	}
}
