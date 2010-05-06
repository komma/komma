package net.enilink.komma.parser.manchester;

import org.parboiled.Rule;
import org.parboiled.support.In;

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
//TODO Es sind noch Leerzeichen und Zeilenumbrüche an den IRIs und Co dran.
public class ManchesterSyntaxParser extends BaseRdfParser {
	
	public static final String OWL_HAS_KEY = "owl:hasKey";

	//TODO muss in CONCEPTS
	public static final URI OWL_DISJOINT_UNION_OF = OWL.NAMESPACE_URI.appendFragment("disjointUnionOf");

	//TODO muss in CONCEPTS	
	public static final URI OWL_TYPES = OWL.NAMESPACE_URI.appendFragment("types");

	//TODO muss in CONCEPTS
	public static final URI OWL_EQUIVALENTTO = OWL.NAMESPACE_URI.appendFragment("equivalentTo");

	//TODO muss in CONCEPTS
	public static final URI OWL_CHARACTERISTICS = OWL.NAMESPACE_URI.appendFragment("Characteristics");
	
	//TODO muss in CONCEPTS
	public static final URI OWL_INDIVIDUAL = OWL.NAMESPACE_URI.appendFragment("Individual");
	
	//TODO muss in CONCEPTS
	public static final URI OWL_FACT = OWL.NAMESPACE_URI.appendFragment("Fact");

	
	
	public IManchesterAction actions;;
	
	 
	
	
	
	 public void setManchesterActions(IManchesterAction action) {
		 this.actions = action;
	 }
	 
	 // 2.1 IRIs, Integers, Literals, and Entities

	public Rule Datatype() {
		return firstOf(iriRef(), "integer", "decimal", "float", "string");
	}

	public Rule individual() {
		return firstOf(iriRef(), blankNode());
	}

	public Rule individual(@In GraphNode subject, @In URI predicate) {
		return firstOf(iriRef(subject,predicate), blankNode());
	}
	
	
	public Rule literal() {
		return firstOf(rdfLiteral(), numericLiteral(), booleanLiteral());
	}

	public Rule entity() {
		return firstOf(sequence("Datatype", '(', Datatype(), ')'), //
				sequence("Class", '(', iriRef(), ')'), //
				sequence("ObjectProperty", '(', iriRef(), ')'), //
				sequence("DataProperty", '(', iriRef(), ')'), //
				sequence("AnnotationProperty", '(', iriRef(), //
						')'), //
				sequence("NamedIndividual", '(', iriRef(), ')'));
	}

	// 2.2 Ontologies and Annotations

	public Rule annotations(@In GraphNode subject) {
		return zeroOrMore(sequence("Annotations:", annotatedList(annotation(subject))));
	}

	//TODO VALUE für action(.., pre,obj)
	public Rule annotation(@In GraphNode subject) {
		return sequence(iriRef(), annotationTarget(),
				DO(actions.action(subject, VALUE("iriRef"), TEXT("annotationTarget")))); 
	}

	public Rule annotationTarget() {
		return firstOf(blankNode(), iriRef(), literal());
	}

	public Rule ontologyDocument() {
		return sequence(zeroOrMore(prefixDeclaration()), ontology(), eoi());
	}

	public Rule prefixDeclaration() {
		return sequence("Prefix:", PNAME_NS(), IRI_REF());
	}

	public Rule ontology() {
		IriRef ontologyIri;
		
		return sequence("Ontology:", optional(sequence(ontologyIRI(),
				DO(ontologyIri = (IriRef)VALUE("ontologyIRI")),
				DO(actions.action(ontologyIri,RDF.PROPERTY_TYPE,OWL.TYPE_ONTOLOGY)),
				optional(sequence(versionIRI(),
						DO(actions.action(ontologyIri,OWL.PROPERTY_VERSIONINFO,TEXT(NODE_BY_LABEL("versionIRI"))))
				)))), // 
				zeroOrMore(importOntology()), //
				annotations(ontologyIri), //
				zeroOrMore(frame()));
	}

	public Rule iriRef(@In GraphNode subject,@In URI predicate) {
		return sequence(firstOf(
				  IRI_REF(), prefixedName(), 
				  sequence(PN_LOCAL(),SET(new QName("", TEXT("PN_LOCAL"))))
			   ),
			   DO(actions.action(subject,predicate,LAST_TEXT())));
	}
	
	public Rule iriRef() {
		return firstOf(
				  IRI_REF(), prefixedName(), 
				  sequence(PN_LOCAL(),SET(new QName("", TEXT("PN_LOCAL"))))
			   );
	}
	
	public Rule IRI_REF_WS() {
		return sequence(IRI_REF(), WS());
	}

	public Rule ontologyIRI() {
		return IRI_REF_WS();
	}

	public Rule versionIRI() {
		return IRI_REF_WS();
	}

	public Rule importOntology() {
		return sequence("Import:", IRI_REF_WS());
	}

	public Rule frame() {
		return firstOf(
				datatypeFrame(), //done
				classFrame(), //done
				objectPropertyFrame(), //done
				dataPropertyFrame(), // done
				annotationPropertyFrame(), //done
				individualFrame(),// done
				misc() //
		);
	}

	// 2.3 Property and Datatype Expressions

	public Rule objectPropertyExpression(@In GraphNode subject,@In URI predicate) {
		return sequence(firstOf(iriRef(), inverseObjectProperty()),
				DO(actions.action(subject,predicate, LAST_TEXT())));
		
	}

	public Rule objectPropertyExpression() {
		return firstOf(iriRef(), inverseObjectProperty());
		
	}
	
	public Rule inverseObjectProperty() {
		return sequence("inverse", iriRef());
	};

	public Rule dataPropertyExpression() {
		return iriRef();
	}

	public Rule dataPropertyExpression(@In GraphNode subject,@In URI predicate) {
		return sequence(iriRef(),DO(actions.action(subject,predicate, TEXT("iriRef"))));
	}
	
	
	
	public Rule dataRange() {
		return sequence(dataConjunction(), zeroOrMore(sequence("or",
				dataConjunction())));
	}
	
	
	//TODO was ist mit dem or und der 2. dataConjunction?
	public Rule dataRange(@In GraphNode subject,@In URI predicate) {
		return sequence(dataConjunction(), zeroOrMore(sequence("or",
				dataConjunction())),
				DO(actions.action(subject,predicate, TEXT("dataConjunction"))));
	}

	
	
	public Rule dataConjunction() {
		return sequence(dataPrimary(),
				zeroOrMore(sequence("and", dataPrimary())));
	}

	public Rule dataPrimary() {
		return sequence(optional("not"), dataAtomic());
	}

	public Rule dataAtomic() {
		return firstOf(datatypeRestriction(), sequence('{', list(literal()),
				'}'), sequence('(', dataRange(), ')'));
	}

	public Rule datatypeRestriction() {
		return sequence(Datatype(), optional(sequence('[', facet(),
				restrictionValue(), zeroOrMore(sequence(',', facet(),
						restrictionValue())), ']')));
	}

	public Rule facet() {
		return firstOf("length", "minLength", "maxLength", "pattern",
				"langPattern", "<=", '<', ">=", '>');
	}

	public Rule restrictionValue() {
		return literal();
	}

	// 2.4 Descriptions

	public Rule description(@In GraphNode subject,@In URI predicate) {
		return sequence(conjunction(),
				zeroOrMore(sequence("or", conjunction())),DO(actions.action(subject, predicate, TEXT("conjunction"))));
	}

	public Rule conjunction() {
		return firstOf(sequence(iriRef(), "that", optional("not"),
				restriction(), zeroOrMore(sequence("and", optional("not"),
						restriction()))), sequence(primary(),
				zeroOrMore(sequence("and", primary()))));
	}

	public Rule primary() {
		return sequence(optional("not"), firstOf(restriction(), atomic()));
	}

	public Rule restriction() {
		return firstOf(
				sequence(objectPropertyExpression(), "some", primary()),
				sequence(objectPropertyExpression(), "only", primary()),
				sequence(objectPropertyExpression(), "value", individual()),
				sequence(objectPropertyExpression(), "Self"), //
				sequence(objectPropertyExpression(), "min", INTEGER_POSITIVE(),
						optional(primary())), //
				sequence(objectPropertyExpression(), "max", INTEGER_POSITIVE(),
						optional(primary())), //
				sequence(objectPropertyExpression(), "exactly",
						INTEGER_POSITIVE(), optional(primary())), //
				sequence(dataPropertyExpression(), "some", dataPrimary()),
				sequence(dataPropertyExpression(), "only", dataPrimary()),
				sequence(dataPropertyExpression(), "value", literal()),
				sequence(dataPropertyExpression(), "min", INTEGER_POSITIVE(),
						optional(dataPrimary())), //
				sequence(dataPropertyExpression(), "max", INTEGER_POSITIVE(),
						optional(dataPrimary())), //
				sequence(dataPropertyExpression(), "exactly",
						INTEGER_POSITIVE(), optional(dataPrimary())));
	}

	public Rule atomic() {
		return firstOf(iriRef(), sequence('{', list(individual()), '}'),
				sequence('(', description(null,null), ')'));
	}

	// 2.5 Frames and Miscellaneous

	public Rule datatypeFrame() {
		QName datatypeIri;
		
		return sequence(
				"Datatype:",
				Datatype(),
				DO(datatypeIri = (QName)VALUE("Datatype")),
				DO(actions.action(datatypeIri, RDF.PROPERTY_TYPE, RDFS.TYPE_DATATYPE)),
				zeroOrMore(sequence("Annotations:", annotatedList(annotation(datatypeIri)))),
				//TODO Annotations, dataRange
				optional(sequence("EquivalentTo:", annotations(null), dataRange())),
				zeroOrMore(sequence("Annotations:", annotatedList(annotation(datatypeIri)))));
	}

	//TODO wenn man 2 Class frames hat geht es nicht, darum ist HasKey auskommentiert!
	public Rule classFrame() {
		QName classIri;
		
		
		return sequence("Class:", iriRef(),
				DO(classIri = (QName)VALUE("iriRef")),
				DO(actions.action(classIri, RDF.PROPERTY_TYPE, OWL.TYPE_CLASS)),
				zeroOrMore(firstOf(sequence(
				"Annotations:", annotatedList(annotation(classIri))), sequence(
				"SubClassOf:", annotatedList(description(classIri, RDFS.PROPERTY_SUBCLASSOF))), sequence(
				"EquivalentTo:", annotatedList(description(classIri, OWL_EQUIVALENTTO))), sequence(
				"DisjointWith:", annotatedList(description(classIri,OWL.PROPERTY_DISJOINTWITH))), sequence(
				//TODO Die Annotations fehlen noch
				"DisjointUnionOf:", annotations(classIri), list2(description(classIri,OWL_DISJOINT_UNION_OF)))))//,
				//TODO  Die Annotations fehlen noch
				/*sequence("HasKey:", annotations(classIri), oneOrMore(firstOf(
						objectPropertyExpression(classIri,OWL_HAS_KEY), dataPropertyExpression())))*/);
	}

	public boolean echo(String value) {
		System.out.println(value);
		return true;
	}

	public Rule objectPropertyFrame() {
		QName objectPropertyIri;
		
		return sequence("ObjectProperty:", iriRef(), 
				DO(objectPropertyIri = (QName)VALUE("iriRef")),
				DO(actions.action(objectPropertyIri, RDF.PROPERTY_TYPE, OWL.TYPE_OBJECTPROPERTY)),
				zeroOrMore(firstOf(
				sequence("Annotations:", annotatedList(annotation(objectPropertyIri))),
				sequence("Domain:", annotatedList(description(objectPropertyIri,RDFS.PROPERTY_DOMAIN))), 
				sequence("Range:", annotatedList(description(objectPropertyIri,RDFS.PROPERTY_RANGE))), 
				sequence("Characteristics:",annotatedList(objectPropertyCharacteristic(objectPropertyIri))),
				sequence("SubPropertyOf:",annotatedList(objectPropertyExpression(objectPropertyIri,RDFS.PROPERTY_SUBPROPERTYOF))), 
				sequence("EquivalentTo:",annotatedList(objectPropertyExpression(objectPropertyIri,OWL_EQUIVALENTTO))), 
				sequence("DisjointWith:",annotatedList(objectPropertyExpression(objectPropertyIri,OWL.PROPERTY_DISJOINTWITH))),
				sequence("InverseOf:",annotatedList(objectPropertyExpression(objectPropertyIri,OWL.PROPERTY_INVERSEOF))), 
				//TODO
				sequence("SubPropertyChain:", annotations(null), oneOrMore(sequence(
								objectPropertyExpression(null,null), 'o',
								objectPropertyExpression(null,null)))))));
	}

	public Rule objectPropertyCharacteristic(@In GraphNode subject) {
		return sequence(firstOf("Functional", "InverseFunctional", "Reflexive",
				"Irreflexive", "Symmetric", "Asymmetric", "Transitive"),DO(actions.action(subject, OWL_CHARACTERISTICS, LAST_TEXT())));
	}

	public Rule dataPropertyFrame() {
		QName datapropertyIri;
		
		return sequence("DataProperty:", iriRef(), 
				DO(datapropertyIri = (QName)VALUE("iriRef")),
				DO(actions.action(datapropertyIri, RDF.PROPERTY_TYPE, OWL.TYPE_DATATYPEPROPERTY)),
				zeroOrMore(firstOf(sequence(
				"Annotations:", annotatedList(annotation(datapropertyIri))), sequence(
				"Domain:", annotatedList(description(datapropertyIri,RDFS.PROPERTY_DOMAIN))), 
				sequence("Range:",annotatedList(dataRange(datapropertyIri,RDFS.PROPERTY_RANGE))), 
				//TODO Annotations
				sequence("Characteristics:",annotations(null), "Functional",
						DO(actions.action(datapropertyIri, OWL_CHARACTERISTICS, LAST_TEXT()))), 
				sequence("SubPropertyOf:",	annotatedList(dataPropertyExpression(datapropertyIri,RDFS.PROPERTY_SUBPROPERTYOF))), 
				sequence("EquivalentTo:", annotatedList(dataPropertyExpression(datapropertyIri,OWL_EQUIVALENTTO))),
				sequence("DisjointWith:",
						annotatedList(dataPropertyExpression(datapropertyIri,OWL.PROPERTY_DISJOINTWITH))))));
	}

	public Rule annotationPropertyFrame() {
		QName annotationPropertyIri;
		
		return sequence("AnnotationProperty:", iriRef(), 
				DO(annotationPropertyIri = (QName)VALUE("iriRef")),
				DO(actions.action(annotationPropertyIri, RDF.PROPERTY_TYPE, OWL.TYPE_ANNOTATIONPROPERTY)),
				zeroOrMore(firstOf(
				sequence("Annotations:", annotatedList(annotation(annotationPropertyIri))),
				sequence("Domain:", annotatedList(iriRef(annotationPropertyIri,RDFS.PROPERTY_DOMAIN))), 
				sequence("Range:", annotatedList(iriRef(annotationPropertyIri,RDFS.PROPERTY_RANGE))), 
				sequence("SubPropertyOf:", annotatedList(iriRef(annotationPropertyIri,RDFS.PROPERTY_SUBPROPERTYOF))))));
	}

	public Rule individualFrame() {
		QName individualIri;
		
		
		return sequence("Individual:", individual(), 
				DO(individualIri = (QName)VALUE("individual")),
				//TODO Name noch falsch
				DO(actions.action(individualIri, RDF.PROPERTY_TYPE,OWL_INDIVIDUAL)),
				zeroOrMore(firstOf(
				sequence("Annotations:", annotatedList(annotation(individualIri))),
				//TODO Name noch unklar
				sequence("Types:", annotatedList(description(individualIri,OWL_TYPES))), 
				sequence("Facts:", annotatedList(fact(individualIri))), 
				sequence("SameAs:",	annotatedList(individual(individualIri,OWL.PROPERTY_SAMEAS))), 
				sequence("DifferentFrom:", annotatedList(individual(individualIri,OWL.PROPERTY_DIFFERENTFROM))))));
	}

	//TODO Name (predicate) noch unklar
	public Rule fact(@In GraphNode subject) {
		
		return sequence(optional("not"), firstOf(objectPropertyFact(),
				dataPropertyFact()),
				DO(actions.action(subject,OWL_FACT, LAST_TEXT())));
	}

	public Rule objectPropertyFact() {
		return sequence(iriRef(), individual());
	}

	public Rule dataPropertyFact() {
		return sequence(iriRef(), literal());
	}

	public Rule misc() {
		return firstOf(
				sequence("EquivalentClasses:", annotations(null),
						list2(description(null,null))), //
				sequence("DisjointClasses:", annotations(null),
						list2(description(null,null))), //
				sequence("EquivalentProperties:", annotations(null),
						list2(iriRef())), //
				sequence("DisjointProperties:", annotations(null), list2(iriRef())), //
				sequence("SameIndividual:", annotations(null), list2(individual())), //
				sequence("DifferentIndividuals:", annotations(null),
						list2(individual())));
	}

	public Rule annotatedList(Rule element) {
		return sequence(annotations(null), element, zeroOrMore(sequence(',',
				annotations(null), element)));
	}

	

	
	public Rule list(Rule element) {
		return sequence(element, zeroOrMore(sequence(',', element)));
	}

	public Rule list2(Rule element) {
		return sequence(element, oneOrMore(sequence(',', element)));
	}
}