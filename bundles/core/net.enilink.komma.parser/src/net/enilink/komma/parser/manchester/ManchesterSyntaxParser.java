package net.enilink.komma.parser.manchester;

import java.util.Collection;
import java.util.List;

import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.support.Var;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.parser.BaseRdfParser;
import net.enilink.komma.parser.manchester.tree.Annotation;
import net.enilink.komma.parser.sparql.tree.BNode;
import net.enilink.komma.parser.sparql.tree.BooleanLiteral;
import net.enilink.komma.parser.sparql.tree.GraphNode;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.Literal;
import net.enilink.komma.parser.sparql.tree.QName;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.vocab.xmlschema.XMLSCHEMA;

/**
 * Parser for Manchester OWL Syntax
 * 
 * @see <a href="http://www.w3.org/TR/owl2-manchester-syntax/">Manchester OWL
 *      Syntax</a>
 * 
 * @author Ken Wenzel
 */
public class ManchesterSyntaxParser extends BaseRdfParser {
	public IManchesterActions actions;

	public ManchesterSyntaxParser() {
		this(new IManchesterActions() {
			@Override
			public boolean createStmt(Object subject, Object predicate,
					Object object) {
				return true;
			}
		});
	}

	public ManchesterSyntaxParser(IManchesterActions actions) {
		this.actions = actions;
	}

	// 2.1 IRIs, Integers, Literals, and Entities

	public Rule AnnotatedList(Var<? extends GraphNode> source, URI property,
			Rule target) {
		return sequence(WithAnnotations(source, property, target),
				zeroOrMore(',', WithAnnotations(source, property, target)));
	}

	public Rule Annotation() {
		return sequence(IriRef(), AnnotationTarget(), //
				push(new Annotation((GraphNode) pop(1), (GraphNode) pop())));
	}

	public Rule AnnotationPropertyFrame() {
		Var<GraphNode> annotationPropertyIri = new Var<>();
		return sequence(
				"AnnotationProperty:",
				IriRef(),
				annotationPropertyIri.set((GraphNode) pop()),
				actions.createStmt(annotationPropertyIri, RDF.PROPERTY_TYPE,
						OWL.TYPE_ANNOTATIONPROPERTY),
				zeroOrMore(firstOf(
						sequence(
								"Annotations:",
								AnnotatedList(annotationPropertyIri, null,
										Annotation())),
						sequence(
								"Domain:",
								AnnotatedList(annotationPropertyIri,
										RDFS.PROPERTY_DOMAIN, IriRef())),
						sequence(
								"Range:",
								AnnotatedList(annotationPropertyIri,
										RDFS.PROPERTY_RANGE, IriRef())),
						sequence(
								"SubPropertyOf:",
								AnnotatedList(annotationPropertyIri,
										RDFS.PROPERTY_SUBPROPERTYOF, IriRef())))));
	}

	@SuppressWarnings("unchecked")
	public Rule WithAnnotations(Var<? extends GraphNode> source, URI property,
			Rule target) {
		return sequence(
				Annotations(),
				target,
				createAnnotatedStmt((List<Annotation>) pop(1), source.get(),
						property, pop()));
	}

	public boolean createAnnotatedStmt(List<Annotation> annotations,
			Object source, URI property, Object target) {
		if (source != null && property != null) {
			// this is the annotated statement
			actions.createStmt(source, property, target);
		} else if (target instanceof Annotation) {
			// this is an annotated annotation
			push(target);

			source = target;
			property = URIs.createURI(((Annotation) target).getPredicate()
					.toString());
			target = ((Annotation) target).getObject();
		}

		for (Annotation annotation : annotations) {
			if (property == null) {
				actions.createStmt(source, annotation.getPredicate(),
						annotation.getObject());
			} else {
				BNode annotationNode = new BNode();
				actions.createStmt(annotationNode, RDF.PROPERTY_TYPE,
						OWL.TYPE_ANNOTATIONPROPERTY);
				actions.createStmt(annotationNode,
						OWL.PROPERTY_ANNOTATEDSOURCE, source);
				actions.createStmt(annotationNode,
						OWL.PROPERTY_ANNOTATEDPROPERTY, property);
				actions.createStmt(annotationNode,
						OWL.PROPERTY_ANNOTATEDTARGET, target);

				actions.createStmt(annotationNode, annotation.getPredicate(),
						annotation.getObject());
			}
		}

		return true;
	}

	public Rule Annotations() {
		return sequence(
				push(LIST_BEGIN),
				zeroOrMore(
						"Annotations:",
						AnnotatedList(new Var<>((GraphNode) null), null,
								Annotation())), push(popList(Annotation.class)));
	}

	public Rule AnnotationTarget() {
		return firstOf(BlankNode(), IriRef(), Literal());
	}

	// 2.2 Ontologies and Annotations

	public Rule Atomic() {
		return firstOf(IriRef(), sequence('{', List(Individual()), '}'),
				sequence('(', Description(), ')'));
	}

	public Rule ClassFrame() {
		Var<GraphNode> classIri = new Var<>();
		return sequence(
				"Class:",
				IriRef(),
				classIri.set((GraphNode) pop()),
				actions.createStmt(classIri, RDF.PROPERTY_TYPE, OWL.TYPE_CLASS),
				zeroOrMore(firstOf(
						sequence("Annotations:",
								AnnotatedList(classIri, null, Annotation())),
						sequence(
								"SubClassOf:",
								AnnotatedList(classIri,
										RDFS.PROPERTY_SUBCLASSOF, Description())),
						sequence(
								"EquivalentTo:",
								AnnotatedList(classIri,
										OWL.PROPERTY_EQUIVALENTCLASS,
										Description())),
						sequence(
								"DisjointWith:",
								AnnotatedList(classIri,
										OWL.PROPERTY_DISJOINTWITH,
										Description())),
						sequence(
								"DisjointUnionOf:",
								WithAnnotations(
										classIri,
										OWL.PROPERTY_DISJOINTUNIONOF,
										sequence(
												List2(Description()),
												push(createRdfList((List<?>) pop()))))),
						sequence(
								"HasKey:",
								WithAnnotations(classIri, OWL.PROPERTY_HASKEY,
										oneOrMore(PropertyExpression()))))));
	}

	public Rule PropertyExpression() {
		return firstOf(ObjectPropertyExpression(), DataPropertyExpression());
	}

	public Rule Conjunction() {
		return firstOf(
				sequence(IriRef(), "that", optional("not"), Restriction(),
						zeroOrMore("and", optional("not"), Restriction())),

				sequence(
						Primary(),
						optional(push(LIST_BEGIN), oneOrMore("and", Primary()),
								push(new BNode()), //
								actions.createStmt(peek(), RDF.PROPERTY_TYPE,
										OWL.TYPE_CLASS), //
								actions.createStmt(
										peek(),
										OWL.PROPERTY_INTERSECTIONOF,
										createRdfList(popList(1,
												GraphNode.class, 1))))));
	}

	public GraphNode createRdfList(Collection<? extends Object> elements) {
		GraphNode head = null, last = null;

		for (Object element : elements) {
			GraphNode current = new BNode();
			actions.createStmt(current, RDF.PROPERTY_TYPE, RDF.TYPE_LIST);
			actions.createStmt(current, RDF.PROPERTY_FIRST, element);
			if (last != null) {
				actions.createStmt(last, RDF.PROPERTY_REST, current);
			}
			if (head == null) {
				head = current;
			}
			last = current;
		}
		actions.createStmt(last, RDF.PROPERTY_REST, RDF.NIL);

		return head;
	}

	public Rule DataAtomic() {
		return firstOf(DatatypeRestriction(),
				sequence('{', List(Literal()), '}'),
				sequence('(', DataRange(), ')'));
	}

	public Rule DataConjunction() {
		return sequence(
				DataPrimary(),
				optional(push(LIST_BEGIN), oneOrMore("and", DataPrimary()),
						push(new BNode()), actions.createStmt(peek(),
								OWL.PROPERTY_INTERSECTIONOF,
								createRdfList(popList(1, GraphNode.class, 1)))));
	}

	public Rule DataPrimary() {
		return sequence(optional("not"), DataAtomic());
	}

	public Rule DataPropertyExpression() {
		return IriRef();
	}

	public Rule DataPropertyFact() {
		return sequence(IriRef(), Literal());
	}

	public Rule DataPropertyFrame() {
		Var<GraphNode> datapropertyIri = new Var<>();
		return sequence(
				"DataProperty:",
				IriRef(),
				datapropertyIri.set((GraphNode) pop()),
				actions.createStmt(datapropertyIri, RDF.PROPERTY_TYPE,
						OWL.TYPE_DATATYPEPROPERTY),
				zeroOrMore(firstOf(
						sequence(
								"Annotations:",
								AnnotatedList(datapropertyIri, null,
										Annotation())),
						sequence(
								"Domain:",
								AnnotatedList(datapropertyIri,
										RDFS.PROPERTY_DOMAIN, Description())),
						sequence(
								"Range:",
								AnnotatedList(datapropertyIri,
										RDFS.PROPERTY_RANGE, DataRange())),
						// TODO Annotations
						sequence(
								"Characteristics:",
								WithAnnotations(
										datapropertyIri,
										RDF.PROPERTY_TYPE,
										sequence(
												"Functional",
												push(new IriRef(
														OWL.TYPE_FUNCTIONALPROPERTY
																.toString()))))),
						sequence(
								"SubPropertyOf:",
								AnnotatedList(datapropertyIri,
										RDFS.PROPERTY_SUBPROPERTYOF,
										DataPropertyExpression())),
						sequence(
								"EquivalentTo:",
								AnnotatedList(datapropertyIri,
										OWL.PROPERTY_EQUIVALENTPROPERTY,
										DataPropertyExpression())),
						sequence(
								"DisjointWith:",
								AnnotatedList(datapropertyIri,
										OWL.PROPERTY_DISJOINTWITH,
										DataPropertyExpression())))));
	}

	public Rule DataRange() {
		return sequence(
				DataConjunction(),
				optional(push(LIST_BEGIN), oneOrMore("or", DataConjunction()),
						push(new BNode()), actions.createStmt(peek(),
								OWL.PROPERTY_UNIONOF,
								createRdfList(popList(1, GraphNode.class, 1)))));
	}

	// 2.3 Property and Datatype Expressions

	public Rule Datatype() {
		return firstOf(
				IriRef(),
				sequence(firstOf("integer", "decimal", "float", "string"),
						push(new IriRef(XMLSCHEMA.NAMESPACE + match()))));
	}

	public Rule DatatypeFrame() {
		Var<GraphNode> datatypeIri = new Var<>();
		return sequence(
				"Datatype:",
				Datatype(),
				datatypeIri.set((GraphNode) pop()),
				actions.createStmt(datatypeIri, RDF.PROPERTY_TYPE,
						RDFS.TYPE_DATATYPE),
				zeroOrMore("Annotations:",
						AnnotatedList(datatypeIri, null, Annotation())),
				// TODO Annotations, dataRange
				optional(
						"EquivalentTo:",
						WithAnnotations(datatypeIri,
								OWL.PROPERTY_EQUIVALENTCLASS, DataRange())),
				zeroOrMore("Annotations:",
						AnnotatedList(datatypeIri, null, Annotation())));
	}

	public Rule DatatypeRestriction() {
		Var<BNode> restriction = new Var<>();
		return sequence(
				Datatype(),
				optional(
						'[',
						restriction.set(new BNode()),
						actions.createStmt(restriction.get(),
								RDF.PROPERTY_TYPE, RDFS.TYPE_DATATYPE),
						actions.createStmt(restriction.get(),
								OWL.PROPERTY_ONDATATYPE, pop()),

						push(LIST_BEGIN), //
						push(new BNode()), //
						Facet(), RestrictionValue(), actions.createStmt(
								peek(2), pop(1), pop()),
						zeroOrMore(',', push(new BNode()), //
								Facet(), RestrictionValue(), //
								actions.createStmt(peek(2), pop(1), pop())),
						']', actions.createStmt(restriction.get(),
								OWL.PROPERTY_WITHRESTRICTIONS,
								createRdfList(popList(GraphNode.class))),
						push(restriction.get())));
	};

	public Rule Description() {
		return sequence(
				Conjunction(),
				optional(push(LIST_BEGIN), oneOrMore("or", Conjunction()),
						push(new BNode()), //
						actions.createStmt(peek(), RDF.PROPERTY_TYPE,
								OWL.TYPE_CLASS), //
						actions.createStmt(peek(), OWL.PROPERTY_UNIONOF,
								createRdfList(popList(1, GraphNode.class, 1)))));
	}

	public Rule Entity() {
		return firstOf(sequence("Datatype", '(', Datatype(), ')'), //
				sequence("Class", '(', IriRef(), ')'), //
				sequence("ObjectProperty", '(', IriRef(), ')'), //
				sequence("DataProperty", '(', IriRef(), ')'), //
				sequence("AnnotationProperty", '(', IriRef(), //
						')'), //
				sequence("NamedIndividual", '(', IriRef(), ')'));
	}

	public Rule Facet() {
		return sequence(
				firstOf("length", "minLength", "maxLength", "pattern",
						"langPattern", "<=", '<', ">=", '>'),
				push(createFacet(match().trim())));
	}

	public IriRef createFacet(String facet) {
		String name = facet;
		if ("<=".equals(facet)) {
			name = "minInclusive";
		} else if ("<".equals(facet)) {
			name = "minExclusive";
		} else if (">=".equals(facet)) {
			name = "maxInclusive";
		} else if (">".equals(facet)) {
			name = "maxExclusive";
		}
		return new IriRef(XMLSCHEMA.NAMESPACE + name);
	}

	public Rule Fact(Var<GraphNode> individualIri) {
		Var<Boolean> not = new Var<>(false);
		return sequence(optional("not", not.set(true)),
				firstOf(ObjectPropertyFact(), DataPropertyFact()),
				createPropertyAssertion(individualIri.get(), not.get()));
	}

	public boolean createPropertyAssertion(GraphNode individual,
			boolean negative) {
		if (negative) {
			BNode assertion = new BNode();
			GraphNode property = (GraphNode) pop(1);
			Object value = pop();
			actions.createStmt(assertion, RDF.PROPERTY_TYPE,
					OWL.TYPE_NEGATIVEPROPERTYASSERTION);
			actions.createStmt(assertion, OWL.PROPERTY_SOURCEINDIVIDUAL,
					individual);
			actions.createStmt(assertion, OWL.PROPERTY_ASSERTIONPROPERTY,
					property);
			actions.createStmt(assertion,
					value instanceof Literal ? OWL.PROPERTY_TARGETVALUE
							: OWL.PROPERTY_TARGETINDIVIDUAL, value);
			push(assertion);
		} else {
			actions.createStmt(pop(1), pop(), individual);
			push(individual);
		}
		return true;
	}

	public Rule Frame() {
		return firstOf(DatatypeFrame(), //
				ClassFrame(), //
				ObjectPropertyFrame(), //
				DataPropertyFrame(), //
				AnnotationPropertyFrame(), //
				IndividualFrame(), //
				Misc() //
		);
	}

	public Rule ImportOntology() {
		return sequence("Import:", IRI_REF_WS());
	}

	public Rule Individual() {
		return firstOf(IriRef(), BlankNode());
	}

	public Rule IndividualFrame() {
		Var<GraphNode> individualIri = new Var<>();
		return sequence(
				"Individual:",
				Individual(),
				individualIri.set((GraphNode) pop()),
				actions.createStmt(individualIri.get(), RDF.PROPERTY_TYPE,
						OWL.TYPE_INDIVIDUAL),
				zeroOrMore(firstOf(
						sequence(
								"Annotations:",
								AnnotatedList(individualIri, null, Annotation())),
						sequence(
								"Types:",
								AnnotatedList(individualIri, RDF.PROPERTY_TYPE,
										Description())),
						sequence(
								"Facts:",
								AnnotatedList(individualIri, null,
										Fact(individualIri))),
						sequence(
								"SameAs:",
								AnnotatedList(individualIri,
										OWL.PROPERTY_SAMEAS, Individual())),
						sequence(
								"DifferentFrom:",
								AnnotatedList(individualIri,
										OWL.PROPERTY_DIFFERENTFROM,
										Individual())))));
	}

	public Rule InverseObjectProperty() {
		return sequence("inverse", IriRef());
	}

	// 2.4 Descriptions

	public Rule IRI_REF_WS() {
		return sequence(IRI_REF(), WS());
	}

	public Rule IriRef() {
		return firstOf(IRI_REF(), PrefixedName(),
				sequence(PN_LOCAL(), push(new QName("", (String) pop()))));
	}

	public Rule List(Rule element) {
		return sequence(push(LIST_BEGIN), element, zeroOrMore(',', element),
				push(createRdfList(popList(GraphNode.class))));
	}

	public Rule List2(Rule element) {
		return sequence(push(LIST_BEGIN), element,
				oneOrMore(sequence(',', element)),
				push(popList(GraphNode.class)));
	}

	// 2.5 Frames and Miscellaneous

	public Rule Literal() {
		return firstOf(RdfLiteral(), NumericLiteral(), BooleanLiteral());
	}

	public Rule Misc() {
		return firstOf(
				sequence("EquivalentClasses:", Annotations(),
						List2(Description())), //
				sequence("DisjointClasses:", Annotations(),
						List2(Description())), //
				sequence("EquivalentProperties:", Annotations(),
						List2(IriRef())), //
				sequence("DisjointProperties:", Annotations(), List2(IriRef())), //
				sequence("SameIndividual:", Annotations(), List2(Individual())), //
				sequence("DifferentIndividuals:", Annotations(),
						List2(Individual())));
	}

	public Rule ObjectPropertyCharacteristic() {
		return sequence(
				firstOf("Functional", "InverseFunctional", "Reflexive",
						"Irreflexive", "Symmetric", "Asymmetric", "Transitive"),
				push(new IriRef(OWL.NAMESPACE_URI.appendFragment(
						match().trim() + "Property").toString())));
	}

	public Rule ObjectPropertyExpression() {
		return firstOf(IriRef(), InverseObjectProperty());
	}

	public Rule ObjectPropertyFact() {
		return sequence(IriRef(), Individual());
	}

	public Rule ObjectPropertyFrame() {
		Var<GraphNode> objectPropertyIri = new Var<>();
		return sequence(
				"ObjectProperty:",
				IriRef(),
				objectPropertyIri.set((GraphNode) pop()),
				actions.createStmt(objectPropertyIri, RDF.PROPERTY_TYPE,
						OWL.TYPE_OBJECTPROPERTY),
				zeroOrMore(firstOf(
						sequence(
								"Annotations:",
								AnnotatedList(objectPropertyIri, null,
										Annotation())),
						sequence(
								"Domain:",
								AnnotatedList(objectPropertyIri,
										RDFS.PROPERTY_DOMAIN, Description())),
						sequence(
								"Range:",
								AnnotatedList(objectPropertyIri,
										RDFS.PROPERTY_RANGE, Description())),
						sequence(
								"Characteristics:",
								AnnotatedList(objectPropertyIri,
										RDF.PROPERTY_TYPE,
										ObjectPropertyCharacteristic())),
						sequence(
								"SubPropertyOf:",
								AnnotatedList(objectPropertyIri,
										RDFS.PROPERTY_SUBPROPERTYOF,
										ObjectPropertyExpression())),
						sequence(
								"EquivalentTo:",
								AnnotatedList(objectPropertyIri,
										OWL.PROPERTY_EQUIVALENTPROPERTY,
										ObjectPropertyExpression())),
						sequence(
								"DisjointWith:",
								AnnotatedList(objectPropertyIri,
										OWL.PROPERTY_DISJOINTWITH,
										ObjectPropertyExpression())),
						sequence(
								"InverseOf:",
								AnnotatedList(objectPropertyIri,
										OWL.PROPERTY_INVERSEOF,
										ObjectPropertyExpression())),
						// TODO
						sequence(
								"SubPropertyChain:",
								WithAnnotations(
										objectPropertyIri,
										OWL.PROPERTY_PROPERTYCHAINAXIOM,
										sequence(
												push(LIST_BEGIN),
												oneOrMore(sequence(
														ObjectPropertyExpression(),
														'o',
														ObjectPropertyExpression())),
												push(createRdfList(popList(GraphNode.class)))))) //
				)));
	}

	public Rule Ontology() {
		Var<IriRef> ontologyIri = new Var<>();
		return sequence(
				"Ontology:",
				optional(sequence(
						OntologyIRI(),
						ontologyIri.set((IriRef) pop()),
						actions.createStmt(ontologyIri.get(),
								RDF.PROPERTY_TYPE, OWL.TYPE_ONTOLOGY),
						optional(sequence(VersionIRI(), actions.createStmt(
								ontologyIri.get(), OWL.PROPERTY_VERSIONINFO,
								match()))))), //
				zeroOrMore(ImportOntology()), //

				// FIXME What should be done if ontologyIri is null?

				Annotations(), //
				zeroOrMore(Frame()));
	}

	public Rule OntologyDocument() {
		return sequence(zeroOrMore(PrefixDeclaration()), Ontology(), EOI);
	}

	public Rule OntologyIRI() {
		return IRI_REF_WS();
	}

	public Rule PrefixDeclaration() {
		return sequence("Prefix:", PNAME_NS(), IRI_REF());
	}

	public Rule Primary() {
		Var<Boolean> isComplement = new Var<>(false);
		return sequence(
				optional("not", isComplement.set(true)),
				firstOf(Restriction(), Atomic()),
				// create complement class
				firstOf(isComplement.get() //
						&& push(new BNode()) //
						&& actions.createStmt(peek(), RDF.PROPERTY_TYPE,
								OWL.TYPE_CLASS) //
						&& actions.createStmt(peek(),
								OWL.PROPERTY_COMPLEMENTOF, pop(1)), true));
	}

	public Rule Restriction() {
		Var<URI> type = new Var<>();
		Var<URI> on = new Var<>();
		return sequence(
				firstOf(sequence(
						DataPropertyExpression(),
						firstOf(sequence("some",
								type.set(OWL.PROPERTY_SOMEVALUESFROM),
								DataPrimary()), //
								sequence("only",
										type.set(OWL.PROPERTY_ALLVALUESFROM),
										DataPrimary()),
								sequence("value",
										type.set(OWL.PROPERTY_HASVALUE),
										Literal()), //
								sequence(
										"min",
										type.set(OWL.PROPERTY_MINCARDINALITY),
										INTEGER(),
										optional(DataPrimary(), on
												.set(OWL.PROPERTY_ONDATARANGE))), //
								sequence(
										"max",
										type.set(OWL.PROPERTY_MAXCARDINALITY),
										INTEGER(),
										optional(DataPrimary(), on
												.set(OWL.PROPERTY_ONDATARANGE))), //
								sequence(
										"exactly",
										type.set(OWL.PROPERTY_CARDINALITY),
										INTEGER(),
										optional(DataPrimary(), on
												.set(OWL.PROPERTY_ONDATARANGE))))),
						sequence(
								ObjectPropertyExpression(),
								firstOf(sequence("some",
										type.set(OWL.PROPERTY_SOMEVALUESFROM),
										Primary()),
										sequence(
												"only",
												type.set(OWL.PROPERTY_ALLVALUESFROM),
												Primary()),
										sequence(
												"value",
												type.set(OWL.PROPERTY_HASVALUE),
												Individual()),
										sequence("Self",
												type.set(OWL.PROPERTY_HASSELF),
												push(new BooleanLiteral(true))), //
										sequence(
												"min",
												type.set(OWL.PROPERTY_MINCARDINALITY),
												INTEGER(),
												optional(
														Primary(),
														on.set(OWL.PROPERTY_ONCLASS))), //
										sequence(
												"max",
												type.set(OWL.PROPERTY_MAXCARDINALITY),
												INTEGER(),
												optional(
														Primary(),
														on.set(OWL.PROPERTY_ONCLASS))), //
										sequence(
												"exactly",
												type.set(OWL.PROPERTY_CARDINALITY),
												INTEGER(),
												optional(
														Primary(),
														on.set(OWL.PROPERTY_ONCLASS))))) //
				), createRestriction(type.get(), on.get()));
	}

	public boolean createRestriction(URI type, URI on) {
		Object onTarget = null;
		if (on != null) {
			onTarget = pop();

			if (type.equals(OWL.PROPERTY_CARDINALITY)) {
				type = OWL.PROPERTY_QUALIFIEDCARDINALITY;
			} else if (type.equals(OWL.PROPERTY_MAXCARDINALITY)) {
				type = OWL.PROPERTY_MAXQUALIFIEDCARDINALITY;
			} else if (type.equals(OWL.PROPERTY_MINCARDINALITY)) {
				type = OWL.PROPERTY_MINQUALIFIEDCARDINALITY;
			}
		}
		GraphNode property = (GraphNode) pop(1);
		BNode restriction = new BNode();
		actions.createStmt(restriction, RDF.PROPERTY_TYPE, OWL.TYPE_RESTRICTION);
		actions.createStmt(restriction, OWL.PROPERTY_ONPROPERTY, property);
		actions.createStmt(restriction, type, pop());
		if (on != null) {
			actions.createStmt(restriction, on, onTarget);
		}
		push(restriction);
		return true;
	}

	public Rule RestrictionValue() {
		return Literal();
	}

	public Rule VersionIRI() {
		return IRI_REF_WS();
	}
}