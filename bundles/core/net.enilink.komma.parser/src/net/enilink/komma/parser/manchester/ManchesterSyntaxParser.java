package net.enilink.komma.parser.manchester;

import org.parboiled.Rule;
import org.parboiled.annotations.Var;

import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.parser.BaseRdfParser;
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
	public static final String OWL_HAS_KEY = "owl:hasKey";

	// TODO muss in CONCEPTS
	public static final URI OWL_DISJOINT_UNION_OF = OWL.NAMESPACE_URI
			.appendFragment("disjointUnionOf");

	// TODO muss in CONCEPTS
	public static final URI OWL_INDIVIDUAL = OWL.NAMESPACE_URI
			.appendFragment("Individual");

	// TODO muss in CONCEPTS
	public static final URI OWL_FACT = OWL.NAMESPACE_URI.appendFragment("Fact");

	public IManchesterAction actions;

	public ManchesterSyntaxParser(IManchesterAction actions) {
		this.actions = actions;
	}

	// 2.1 IRIs, Integers, Literals, and Entities

	public Rule Datatype() {
		return FirstOf(IriRef(), "integer", "decimal", "float", "string");
	}

	public Rule Individual() {
		return FirstOf(IriRef(), BlankNode());
	}

	public Rule Individual(@Var GraphNode subject, @Var URI predicate) {
		return FirstOf(IriRef(subject, predicate), BlankNode());
	}

	public Rule Literal() {
		return FirstOf(RdfLiteral(), NumericLiteral(), BooleanLiteral());
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

	// 2.2 Ontologies and Annotations

	public Rule Annotations(@Var GraphNode subject) {
		return ZeroOrMore(Sequence("Annotations:",
				AnnotatedList(Annotation(subject))));
	}

	// TODO VALUE für action(.., pre,obj)
	public Rule Annotation(@Var GraphNode subject) {
		return Sequence(IriRef(), AnnotationTarget(), //
				DO(actions.createStmt(subject, value("IriRef"),
						text("AnnotationTarget"))));
	}

	public Rule AnnotationTarget() {
		return FirstOf(BlankNode(), IriRef(), Literal());
	}

	public Rule OntologyDocument() {
		return Sequence(ZeroOrMore(PrefixDeclaration()), Ontology(), Eoi());
	}

	public Rule PrefixDeclaration() {
		return Sequence("Prefix:", PNAME_NS(), IRI_REF());
	}

	public Rule Ontology() {
		IriRef ontologyIri;

		return Sequence(
				"Ontology:",
				Optional(Sequence(
						OntologyIRI(),
						DO(ontologyIri = (IriRef) value("OntologyIRI")),
						DO(actions.createStmt(ontologyIri, RDF.PROPERTY_TYPE,
								OWL.TYPE_ONTOLOGY)),
						Optional(Sequence(VersionIRI(), DO(actions.createStmt(
								ontologyIri, OWL.PROPERTY_VERSIONINFO,
								text(nodeByLabel("versionIRI")))))))), //
				ZeroOrMore(ImportOntology()), //
				Annotations(ontologyIri), //
				ZeroOrMore(Frame()));
	}

	public Rule IriRef(@Var GraphNode subject, @Var URI predicate) {
		return Sequence(
				FirstOf(IRI_REF(),
						PrefixedName(),
						Sequence(PN_LOCAL(),
								set(new QName("", text("PN_LOCAL"))))),
				DO(actions.createStmt(subject, predicate, lastText())));
	}

	public Rule IriRef() {
		return FirstOf(IRI_REF(), PrefixedName(),
				Sequence(PN_LOCAL(), set(new QName("", text("PN_LOCAL")))));
	}

	public Rule IRI_REF_WS() {
		return Sequence(IRI_REF(), WS());
	}

	public Rule OntologyIRI() {
		return IRI_REF_WS();
	}

	public Rule VersionIRI() {
		return IRI_REF_WS();
	}

	public Rule ImportOntology() {
		return Sequence("Import:", IRI_REF_WS());
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

	// 2.3 Property and Datatype Expressions

	public Rule ObjectPropertyExpression(@Var GraphNode subject,
			@Var URI predicate) {
		return Sequence(FirstOf(IriRef(), InverseObjectProperty()),
				DO(actions.createStmt(subject, predicate, lastText())));

	}

	public Rule ObjectPropertyExpression() {
		return FirstOf(IriRef(), InverseObjectProperty());

	}

	public Rule InverseObjectProperty() {
		return Sequence("inverse", IriRef());
	};

	public Rule DataPropertyExpression() {
		return IriRef();
	}

	public Rule DataPropertyExpression(@Var GraphNode subject,
			@Var URI predicate) {
		return Sequence(IriRef(),
				DO(actions.createStmt(subject, predicate, text("IriRef"))));
	}

	public Rule DataRange() {
		return Sequence(DataConjunction(),
				ZeroOrMore(Sequence("or", DataConjunction())));
	}

	// TODO was ist mit dem or und der 2. dataConjunction?
	public Rule DataRange(@Var GraphNode subject, @Var URI predicate) {
		return Sequence(DataConjunction(),
				ZeroOrMore(Sequence("or", DataConjunction())),
				DO(actions.createStmt(subject, predicate,
						text("DataConjunction"))));
	}

	public Rule DataConjunction() {
		return Sequence(DataPrimary(),
				ZeroOrMore(Sequence("and", DataPrimary())));
	}

	public Rule DataPrimary() {
		return Sequence(Optional("not"), DataAtomic());
	}

	public Rule DataAtomic() {
		return FirstOf(DatatypeRestriction(),
				Sequence('{', List(Literal()), '}'),
				Sequence('(', DataRange(), ')'));
	}

	public Rule DatatypeRestriction() {
		return Sequence(
				Datatype(),
				Optional(Sequence('[', Facet(), RestrictionValue(),
						ZeroOrMore(Sequence(',', Facet(), RestrictionValue())),
						']')));
	}

	public Rule Facet() {
		return FirstOf("length", "minLength", "maxLength", "pattern",
				"langPattern", "<=", '<', ">=", '>');
	}

	public Rule RestrictionValue() {
		return Literal();
	}

	// 2.4 Descriptions

	public Rule Description(@Var GraphNode subject, @Var URI predicate) {
		return Sequence(Conjunction(),
				ZeroOrMore(Sequence("or", Conjunction())),
				DO(actions.createStmt(subject, predicate, text("Conjunction"))));
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

	public Rule Atomic() {
		return FirstOf(IriRef(), Sequence('{', List(Individual()), '}'),
				Sequence('(', Description(null, null), ')'));
	}

	// 2.5 Frames and Miscellaneous

	public Rule DatatypeFrame() {
		QName datatypeIri;

		return Sequence(
				"Datatype:",
				Datatype(),
				DO(datatypeIri = (QName) value("Datatype")),
				DO(actions.createStmt(datatypeIri, RDF.PROPERTY_TYPE,
						RDFS.TYPE_DATATYPE)),
				ZeroOrMore(Sequence("Annotations:",
						AnnotatedList(Annotation(datatypeIri)))),
				// TODO Annotations, dataRange
				Optional(Sequence("EquivalentTo:", Annotations(null),
						DataRange())),
				ZeroOrMore(Sequence("Annotations:",
						AnnotatedList(Annotation(datatypeIri)))));
	}

	// TODO wenn man 2 Class frames hat geht es nicht, darum ist HasKey
	// auskommentiert!
	public Rule ClassFrame() {
		QName classIri;

		return Sequence(
				"Class:",
				IriRef(),
				DO(classIri = (QName) value("IriRef")),
				DO(actions.createStmt(classIri, RDF.PROPERTY_TYPE,
						OWL.TYPE_CLASS)),
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
										OWL_DISJOINT_UNION_OF)))))// ,
		// TODO Die Annotations fehlen noch
		/*
		 * Sequence("HasKey:", Annotations(classIri), OneOrMore(FirstOf(
		 * ObjectPropertyExpression(classIri,OWL_HAS_KEY),
		 * DataPropertyExpression())))
		 */);
	}

	public boolean echo(String value) {
		System.out.println(value);
		return true;
	}

	public Rule ObjectPropertyFrame() {
		QName objectPropertyIri;

		return Sequence(
				"ObjectProperty:",
				IriRef(),
				DO(objectPropertyIri = (QName) value("IriRef")),
				DO(actions.createStmt(objectPropertyIri, RDF.PROPERTY_TYPE,
						OWL.TYPE_OBJECTPROPERTY)),
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

	public Rule ObjectPropertyCharacteristic(@Var GraphNode subject) {
		return Sequence(
				FirstOf("Functional", "InverseFunctional", "Reflexive",
						"Irreflexive", "Symmetric", "Asymmetric", "Transitive"),
				DO(actions.createStmt(
						subject,
						RDF.PROPERTY_TYPE,
						OWL.NAMESPACE_URI.appendFragment(lastText().trim()
								+ "Property"))));
	}

	public Rule DataPropertyFrame() {
		QName datapropertyIri;

		return Sequence(
				"DataProperty:",
				IriRef(),
				DO(datapropertyIri = (QName) value("IriRef")),
				DO(actions.createStmt(datapropertyIri, RDF.PROPERTY_TYPE,
						OWL.TYPE_DATATYPEPROPERTY)),
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
								"Functional", DO(actions.createStmt(
										datapropertyIri, RDF.PROPERTY_TYPE,
										OWL.TYPE_FUNCTIONALPROPERTY))),
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

	public Rule AnnotationPropertyFrame() {
		QName annotationPropertyIri;

		return Sequence(
				"AnnotationProperty:",
				IriRef(),
				DO(annotationPropertyIri = (QName) value("IriRef")),
				DO(actions.createStmt(annotationPropertyIri, RDF.PROPERTY_TYPE,
						OWL.TYPE_ANNOTATIONPROPERTY)),
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

	public Rule IndividualFrame() {
		QName individualIri;

		return Sequence(
				"Individual:",
				Individual(),
				DO(individualIri = (QName) value("Individual")),
				// TODO Name noch falsch
				DO(actions.createStmt(individualIri, RDF.PROPERTY_TYPE,
						OWL_INDIVIDUAL)),
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

	// TODO Name (predicate) noch unklar
	public Rule Fact(@Var GraphNode subject) {
		return Sequence(Optional("not"),
				FirstOf(ObjectPropertyFact(), DataPropertyFact()),
				DO(actions.createStmt(subject, OWL_FACT, lastText())));
	}

	public Rule ObjectPropertyFact() {
		return Sequence(IriRef(), Individual());
	}

	public Rule DataPropertyFact() {
		return Sequence(IriRef(), Literal());
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

	public Rule AnnotatedList(Rule element) {
		return Sequence(Annotations(null), element,
				ZeroOrMore(Sequence(',', Annotations(null), element)));
	}

	public Rule List(Rule element) {
		return Sequence(element, ZeroOrMore(Sequence(',', element)));
	}

	public Rule List2(Rule element) {
		return Sequence(element, OneOrMore(Sequence(',', element)));
	}
}