package net.enilink.komma.parser.manchester;

import java.util.Collection;

import org.parboiled.Rule;
import org.parboiled.annotations.Var;

import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.parser.BaseRdfParser;
import net.enilink.komma.parser.sparql.tree.BNode;
import net.enilink.komma.parser.sparql.tree.GraphNode;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.QName;
import net.enilink.komma.core.URI;

/**
 * Parser for Manchester OWL Syntax
 * 
 * @see <a href="http://www.w3.org/TR/owl2-manchester-syntax/">Manchester OWL
 *      Syntax</a>
 * 
 * @author Ken Wenzel
 */
// TODO Es sind noch Leerzeichen und Zeilenumbrüche an den IRIs und Co dran.
public class ManchesterSyntaxParser extends BaseRdfParser {
	// TODO muss in CONCEPTS
	public static final URI OWL_DISJOINT_UNION_OF = OWL.NAMESPACE_URI
			.appendFragment("disjointUnionOf");

	// TODO muss in CONCEPTS
	public static final URI OWL_FACT = OWL.NAMESPACE_URI.appendFragment("Fact");

	public static final URI OWL_HAS_KEY = OWL.NAMESPACE_URI
			.appendFragment("hasKey");

	// TODO muss in CONCEPTS
	public static final URI OWL_INDIVIDUAL = OWL.NAMESPACE_URI
			.appendFragment("Individual");

	public IManchesterAction actions;

	public ManchesterSyntaxParser(IManchesterAction actions) {
		this.actions = actions;
	}

	// 2.1 IRIs, Integers, Literals, and Entities

	public Rule AnnotatedList(Rule element) {
		return Sequence(Annotations(null), element,
				ZeroOrMore(Sequence(',', Annotations(null), element)));
	}

	// TODO VALUE für action(.., pre,obj)
	public Rule Annotation(@Var GraphNode subject) {
		return Sequence(IriRef(), AnnotationTarget(), //
				actions.createStmt(subject, pop(1), pop()));
	}

	public Rule AnnotationPropertyFrame() {
		QName annotationPropertyIri;

		return Sequence(
				"AnnotationProperty:",
				IriRef(),
				annotationPropertyIri = (QName) pop(),
				actions.createStmt(annotationPropertyIri, RDF.PROPERTY_TYPE,
						OWL.TYPE_ANNOTATIONPROPERTY),
				ZeroOrMore(FirstOf(
						Sequence(
								"Annotations:",
								AnnotatedList(Annotation(annotationPropertyIri))),
						Sequence(
								"Domain:",
								AnnotatedList(IriRef(annotationPropertyIri,
										RDFS.PROPERTY_DOMAIN))),
						Sequence(
								"Range:",
								AnnotatedList(IriRef(annotationPropertyIri,
										RDFS.PROPERTY_RANGE))),
						Sequence(
								"SubPropertyOf:",
								AnnotatedList(IriRef(annotationPropertyIri,
										RDFS.PROPERTY_SUBPROPERTYOF))))));
	}

	public Rule Annotations(@Var GraphNode subject) {
		return ZeroOrMore(Sequence("Annotations:",
				AnnotatedList(Annotation(subject))));
	}

	public Rule AnnotationTarget() {
		return FirstOf(BlankNode(), IriRef(), Literal());
	}

	// 2.2 Ontologies and Annotations

	public Rule Atomic() {
		return FirstOf(IriRef(), Sequence('{', List(Individual()), '}'),
				Sequence('(', Description(null, null), ')'));
	}

	// TODO wenn man 2 Class frames hat geht es nicht, darum ist HasKey
	// auskommentiert!
	public Rule ClassFrame() {
		QName classIri;

		return Sequence(
				"Class:",
				IriRef(),
				classIri = (QName) pop(),
				actions.createStmt(classIri, RDF.PROPERTY_TYPE, OWL.TYPE_CLASS),
				ZeroOrMore(FirstOf(
						Sequence("Annotations:",
								AnnotatedList(Annotation(classIri))),
						Sequence(
								"SubClassOf:",
								AnnotatedList(Description(classIri,
										RDFS.PROPERTY_SUBCLASSOF))),
						Sequence(
								"EquivalentTo:",
								AnnotatedList(Description(classIri,
										OWL.PROPERTY_EQUIVALENTCLASS))),
						Sequence(
								"DisjointWith:",
								AnnotatedList(Description(classIri,
										OWL.PROPERTY_DISJOINTWITH))),
						Sequence(
								// TODO Die Annotations fehlen noch
								"DisjointUnionOf:",
								Annotations(classIri),
								List2(Description(classIri,
										OWL_DISJOINT_UNION_OF))))),

				Sequence(
						"HasKey:",
						Annotations(classIri),
						OneOrMore(FirstOf(
								ObjectPropertyExpression(classIri, OWL_HAS_KEY),
								DataPropertyExpression()))));
	}

	public Rule Conjunction() {
		return FirstOf(
				Sequence(
						IriRef(),
						"that",
						Optional("not"),
						Restriction(),
						ZeroOrMore(Sequence("and", Optional("not"),
								Restriction()))),
				Sequence(Primary(), ZeroOrMore(Sequence("and", Primary()))));
	}

	public GraphNode createRdfList(Collection<? extends GraphNode> elements) {
		GraphNode head = null, last = null;

		for (GraphNode element : elements) {
			GraphNode current = new BNode();
			// actions.createStmt(current, RDF.PROPERTY_TYPE, RDF.TYPE_LIST);
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
		return FirstOf(DatatypeRestriction(),
				Sequence('{', List(Literal()), '}'),
				Sequence('(', DataRange(), ')'));
	}

	public Rule DataConjunction() {
		return Sequence(
				DataPrimary(),
				Optional(push(LIST_BEGIN), OneOrMore("and", DataPrimary()),
						actions.createStmt(null, OWL.PROPERTY_INTERSECTIONOF,
								createRdfList(popList(GraphNode.class, 1)))));
	}

	public Rule DataPrimary() {
		return Sequence(Optional("not"), DataAtomic());
	}

	public Rule DataPropertyExpression() {
		return IriRef();
	}

	public Rule DataPropertyExpression(@Var GraphNode subject,
			@Var URI predicate) {
		return Sequence(IriRef(), actions.createStmt(subject, predicate, pop()));
	}

	public Rule DataPropertyFact() {
		return Sequence(IriRef(), Literal());
	}

	public Rule DataPropertyFrame() {
		QName datapropertyIri;

		return Sequence(
				"DataProperty:",
				IriRef(),
				datapropertyIri = (QName) pop(),
				actions.createStmt(datapropertyIri, RDF.PROPERTY_TYPE,
						OWL.TYPE_DATATYPEPROPERTY),
				ZeroOrMore(FirstOf(
						Sequence("Annotations:",
								AnnotatedList(Annotation(datapropertyIri))),
						Sequence(
								"Domain:",
								AnnotatedList(Description(datapropertyIri,
										RDFS.PROPERTY_DOMAIN))),
						Sequence(
								"Range:",
								AnnotatedList(DataRange(datapropertyIri,
										RDFS.PROPERTY_RANGE))),
						// TODO Annotations
						Sequence("Characteristics:", Annotations(null),
								"Functional", actions.createStmt(
										datapropertyIri, RDF.PROPERTY_TYPE,
										OWL.TYPE_FUNCTIONALPROPERTY)),
						Sequence(
								"SubPropertyOf:",
								AnnotatedList(DataPropertyExpression(
										datapropertyIri,
										RDFS.PROPERTY_SUBPROPERTYOF))),
						Sequence(
								"EquivalentTo:",
								AnnotatedList(DataPropertyExpression(
										datapropertyIri,
										OWL.PROPERTY_EQUIVALENTPROPERTY))),
						Sequence(
								"DisjointWith:",
								AnnotatedList(DataPropertyExpression(
										datapropertyIri,
										OWL.PROPERTY_DISJOINTWITH))))));
	}

	public Rule DataRange() {
		return Sequence(
				DataConjunction(),
				Optional(push(LIST_BEGIN), OneOrMore("or", DataConjunction()),
						actions.createStmt(null, OWL.PROPERTY_UNIONOF,
								createRdfList(popList(GraphNode.class, 1)))));
	}

	// TODO was ist mit dem or und der 2. dataConjunction?
	public Rule DataRange(@Var GraphNode subject, @Var URI predicate) {
		return Sequence(DataConjunction(),
				ZeroOrMore(Sequence("or", DataConjunction())));
	}

	// 2.3 Property and Datatype Expressions

	public Rule Datatype() {
		return FirstOf(IriRef(), "integer", "decimal", "float", "string");
	}

	public Rule DatatypeFrame() {
		QName datatypeIri;

		return Sequence(
				"Datatype:",
				Datatype(),
				datatypeIri = (QName) pop(),
				actions.createStmt(datatypeIri, RDF.PROPERTY_TYPE,
						RDFS.TYPE_DATATYPE),
				ZeroOrMore(Sequence("Annotations:",
						AnnotatedList(Annotation(datatypeIri)))),
				// TODO Annotations, dataRange
				Optional(Sequence("EquivalentTo:", Annotations(null),
						DataRange())),
				ZeroOrMore(Sequence("Annotations:",
						AnnotatedList(Annotation(datatypeIri)))));
	}

	public Rule DatatypeRestriction() {
		return Sequence(
				Datatype(),
				Optional(Sequence('[', Facet(), RestrictionValue(),
						ZeroOrMore(Sequence(',', Facet(), RestrictionValue())),
						']')));
	};

	public Rule Description(@Var GraphNode subject, @Var URI predicate) {
		return Sequence(Conjunction(),
				ZeroOrMore(Sequence("or", Conjunction())));
	}

	public boolean echo(String value) {
		System.out.println(value);
		return true;
	}

	public Rule Entity() {
		return FirstOf(Sequence("Datatype", '(', Datatype(), ')'), //
				Sequence("Class", '(', IriRef(), ')'), //
				Sequence("ObjectProperty", '(', IriRef(), ')'), //
				Sequence("DataProperty", '(', IriRef(), ')'), //
				Sequence("AnnotationProperty", '(', IriRef(), //
						')'), //
				Sequence("NamedIndividual", '(', IriRef(), ')'));
	}

	public Rule Facet() {
		return FirstOf("length", "minLength", "maxLength", "pattern",
				"langPattern", "<=", '<', ">=", '>');
	}

	// TODO Name (predicate) noch unklar
	public Rule Fact(@Var GraphNode subject) {
		return Sequence(Optional("not"),
				FirstOf(ObjectPropertyFact(), DataPropertyFact()),
				actions.createStmt(subject, OWL_FACT, match()));
	}

	public Rule Frame() {
		return FirstOf(DatatypeFrame(), //
				ClassFrame(), //
				ObjectPropertyFrame(), //
				DataPropertyFrame(), //
				AnnotationPropertyFrame(), //
				IndividualFrame(), //
				Misc() //
		);
	}

	public Rule ImportOntology() {
		return Sequence("Import:", IRI_REF_WS());
	}

	public Rule Individual() {
		return FirstOf(IriRef(), BlankNode());
	}

	public Rule Individual(@Var GraphNode subject, @Var URI predicate) {
		return FirstOf(IriRef(subject, predicate), BlankNode());
	}

	public Rule IndividualFrame() {
		QName individualIri;

		return Sequence(
				"Individual:",
				Individual(),
				individualIri = (QName) pop(),
				// TODO Name noch falsch
				actions.createStmt(individualIri, RDF.PROPERTY_TYPE,
						OWL_INDIVIDUAL),
				ZeroOrMore(FirstOf(
						Sequence("Annotations:",
								AnnotatedList(Annotation(individualIri))),
						// TODO Name noch unklar
						Sequence(
								"Types:",
								AnnotatedList(Description(individualIri,
										RDF.PROPERTY_TYPE))),
						Sequence("Facts:", AnnotatedList(Fact(individualIri))),
						Sequence(
								"SameAs:",
								AnnotatedList(Individual(individualIri,
										OWL.PROPERTY_SAMEAS))),
						Sequence(
								"DifferentFrom:",
								AnnotatedList(Individual(individualIri,
										OWL.PROPERTY_DIFFERENTFROM))))));
	}

	public Rule InverseObjectProperty() {
		return Sequence("inverse", IriRef());
	}

	// 2.4 Descriptions

	public Rule IRI_REF_WS() {
		return Sequence(IRI_REF(), WS());
	}

	public Rule IriRef() {
		return FirstOf(IRI_REF(), PrefixedName(),
				Sequence(PN_LOCAL(), push(new QName("", (String) pop()))));
	}

	public Rule IriRef(@Var GraphNode subject, @Var URI predicate) {
		return Sequence(
				FirstOf(IRI_REF(),
						PrefixedName(),
						Sequence(PN_LOCAL(),
								push(new QName("", (String) pop())))),
				actions.createStmt(subject, predicate, match()));
	}

	public Rule List(Rule element) {
		return Sequence(element, ZeroOrMore(Sequence(',', element)));
	}

	public Rule List2(Rule element) {
		return Sequence(element, OneOrMore(Sequence(',', element)));
	}

	// 2.5 Frames and Miscellaneous

	public Rule Literal() {
		return FirstOf(RdfLiteral(), NumericLiteral(), BooleanLiteral());
	}

	public Rule Misc() {
		return FirstOf(
				Sequence("EquivalentClasses:", Annotations(null),
						List2(Description(null, null))), //
				Sequence("DisjointClasses:", Annotations(null),
						List2(Description(null, null))), //
				Sequence("EquivalentProperties:", Annotations(null),
						List2(IriRef())), //
				Sequence("DisjointProperties:", Annotations(null),
						List2(IriRef())), //
				Sequence("SameIndividual:", Annotations(null),
						List2(Individual())), //
				Sequence("DifferentIndividuals:", Annotations(null),
						List2(Individual())));
	}

	public Rule ObjectPropertyCharacteristic(@Var GraphNode subject) {
		return Sequence(
				FirstOf("Functional", "InverseFunctional", "Reflexive",
						"Irreflexive", "Symmetric", "Asymmetric", "Transitive"),
				actions.createStmt(
						subject,
						RDF.PROPERTY_TYPE,
						OWL.NAMESPACE_URI.appendFragment(match().trim()
								+ "Property")));
	}

	public Rule ObjectPropertyExpression() {
		return FirstOf(IriRef(), InverseObjectProperty());

	}

	public Rule ObjectPropertyExpression(@Var GraphNode subject,
			@Var URI predicate) {
		return Sequence(FirstOf(IriRef(), InverseObjectProperty()),
				actions.createStmt(subject, predicate, pop()));

	}

	public Rule ObjectPropertyFact() {
		return Sequence(IriRef(), Individual());
	}

	public Rule ObjectPropertyFrame() {
		QName objectPropertyIri;

		return Sequence(
				"ObjectProperty:",
				IriRef(),
				objectPropertyIri = (QName) pop(),
				actions.createStmt(objectPropertyIri, RDF.PROPERTY_TYPE,
						OWL.TYPE_OBJECTPROPERTY),
				ZeroOrMore(FirstOf(
						Sequence("Annotations:",
								AnnotatedList(Annotation(objectPropertyIri))),
						Sequence(
								"Domain:",
								AnnotatedList(Description(objectPropertyIri,
										RDFS.PROPERTY_DOMAIN))),
						Sequence(
								"Range:",
								AnnotatedList(Description(objectPropertyIri,
										RDFS.PROPERTY_RANGE))),
						Sequence(
								"Characteristics:",
								AnnotatedList(ObjectPropertyCharacteristic(objectPropertyIri))),
						Sequence(
								"SubPropertyOf:",
								AnnotatedList(ObjectPropertyExpression(
										objectPropertyIri,
										RDFS.PROPERTY_SUBPROPERTYOF))),
						Sequence(
								"EquivalentTo:",
								AnnotatedList(ObjectPropertyExpression(
										objectPropertyIri,
										OWL.PROPERTY_EQUIVALENTPROPERTY))),
						Sequence(
								"DisjointWith:",
								AnnotatedList(ObjectPropertyExpression(
										objectPropertyIri,
										OWL.PROPERTY_DISJOINTWITH))),
						Sequence(
								"InverseOf:",
								AnnotatedList(ObjectPropertyExpression(
										objectPropertyIri,
										OWL.PROPERTY_INVERSEOF))),
						// TODO
						Sequence(
								"SubPropertyChain:",
								Annotations(null),
								OneOrMore(Sequence(
										ObjectPropertyExpression(null, null),
										'o',
										ObjectPropertyExpression(null, null)))))));
	}

	public Rule Ontology() {
		IriRef ontologyIri;

		return Sequence(
				"Ontology:",
				Optional(Sequence(
						OntologyIRI(),
						ontologyIri = (IriRef) pop(),
						actions.createStmt(ontologyIri, RDF.PROPERTY_TYPE,
								OWL.TYPE_ONTOLOGY),
						Optional(Sequence(VersionIRI(), actions.createStmt(
								ontologyIri, OWL.PROPERTY_VERSIONINFO, match()))))), //
				ZeroOrMore(ImportOntology()), //
				Annotations(ontologyIri), //
				ZeroOrMore(Frame()));
	}

	public Rule OntologyDocument() {
		return Sequence(ZeroOrMore(PrefixDeclaration()), Ontology(), EOI);
	}

	public Rule OntologyIRI() {
		return IRI_REF_WS();
	}

	public Rule PrefixDeclaration() {
		return Sequence("Prefix:", PNAME_NS(), IRI_REF());
	}

	public Rule Primary() {
		return Sequence(Optional("not"), FirstOf(Restriction(), Atomic()));
	}

	public Rule Restriction() {
		return FirstOf(
				Sequence(ObjectPropertyExpression(), "some", Primary()),
				Sequence(ObjectPropertyExpression(), "only", Primary()),
				Sequence(ObjectPropertyExpression(), "value", Individual()),
				Sequence(ObjectPropertyExpression(), "Self"), //
				Sequence(ObjectPropertyExpression(), "min", INTEGER_POSITIVE(),
						Optional(Primary())), //
				Sequence(ObjectPropertyExpression(), "max", INTEGER_POSITIVE(),
						Optional(Primary())), //
				Sequence(ObjectPropertyExpression(), "exactly",
						INTEGER_POSITIVE(), Optional(Primary())), //
				Sequence(DataPropertyExpression(), "some", DataPrimary()),
				Sequence(DataPropertyExpression(), "only", DataPrimary()),
				Sequence(DataPropertyExpression(), "value", Literal()),
				Sequence(DataPropertyExpression(), "min", INTEGER_POSITIVE(),
						Optional(DataPrimary())), //
				Sequence(DataPropertyExpression(), "max", INTEGER_POSITIVE(),
						Optional(DataPrimary())), //
				Sequence(DataPropertyExpression(), "exactly",
						INTEGER_POSITIVE(), Optional(DataPrimary())));
	}

	public Rule RestrictionValue() {
		return Literal();
	}

	public Rule VersionIRI() {
		return IRI_REF_WS();
	}
}