package net.enilink.komma.em.results;

import java.util.regex.Matcher;

import org.junit.Test;

import net.enilink.vocab.komma.KOMMA;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;

public class ResultDescriptorTest {
	static final String PREFIX = "PREFIX rdf: <" + RDF.NAMESPACE + "> PREFIX rdfs: <" + RDFS.NAMESPACE
			+ "> PREFIX owl: <" + OWL.NAMESPACE + "> PREFIX komma: <" + KOMMA.NAMESPACE + "> ";

	static final String TEST_QUERY_1 = PREFIX + "SELECT DISTINCT ?superClass { "
			+ "?subClass :subClassOf ?superClass . " //
			+ "FILTER NOT EXISTS { ?superClass a owl:Restriction } " //
			+ "FILTER NOT EXISTS {"  //
			+ "?subClass rdfs:subClassOf ?otherSuperClass . " //
			+ "?otherSuperClass rdfs:subClassOf ?superClass . "
			+ "FILTER (?subClass != ?otherSuperClass && ?superClass != ?otherSuperClass" //
			+ "} FILTER (?subClass != ?superClass" + ") } ORDER BY ?superClass";

	static final String TEST_QUERY_2 = PREFIX + "SELECT DISTINCT ?superClass { "
			+ "?subClass rdfs:label '''This is\\''' a test\\'''', \"rdfs:te\\\"st1\", \"\"\"rdfs:test2\"\"\", 'rdfs:test3', '''rdfs:test4'''" //
			+ "}";

	@Test
	public void testUniquePropertySets() {
		ResultDescriptor2<Object> rd2 = new ResultDescriptor2<>(TEST_QUERY_1);
		// System.out.println(rd2.prefixDeclarations);
	}

	@Test
	public void testFindPrefixes() {
		Matcher m = ResultDescriptor2.PNAME_LN.matcher(TEST_QUERY_1);
		while (m.find()) {
			System.out.println(m.group(0));
		}
	}

	@Test
	public void testLiterals() {
		Matcher m = ResultDescriptor2.STRING_OR_IRI.matcher(TEST_QUERY_2);
		while (m.find()) {
			System.out.println(m.group(0));
		}
	}
}