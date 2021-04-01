package net.enilink.komma.em.results;

import net.enilink.komma.core.IResultDescriptor;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.vocab.komma.KOMMA;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

public class ResultDescriptorTest {
	static final String PREFIX = "PREFIX : <" + RDFS.NAMESPACE + "> PREFIX rdf: <" + RDF.NAMESPACE + "> PREFIX rdfs: <" + RDFS.NAMESPACE
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
	public void testPrefixedNames() {
		List<String> expected = Arrays.asList(":subClassOf", "owl:Restriction", "rdfs:subClassOf");
		Matcher m = ResultDescriptor.PNAME_LN.matcher(TEST_QUERY_1);
		while (m.find()) {
			String name = m.group(0);
			Assert.assertTrue("Prefixed name " + name + " should be found.", expected.contains(name));
		}
	}

	@Test
	public void testLiterals() {
		List<String> expected = Arrays.asList(
				"<http://www.w3.org/2002/07/owl#>",
				"<http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
				"<http://www.w3.org/2000/01/rdf-schema#>",
				"<http://www.w3.org/2002/07/owl#>",
				"<http://enilink.net/vocab/komma#>",
				"'''This is\\''' a test\\''''", "\"rdfs:te\\\"st1\"", "\"\"\"rdfs:test2\"\"\"", "'rdfs:test3'",
				"'''rdfs:test4'''");
		Matcher m = ResultDescriptor.STRING_OR_IRI.matcher(TEST_QUERY_2);
		while (m.find()) {
			String literal = m.group(0);
			Assert.assertTrue("Literal " + literal + " should be found.", expected.contains(literal));
		}
	}

	@Test
	public void testVars() {
		List<String> expected = Arrays.asList("?superClass", "?subClass");
		Matcher m = ResultDescriptor.VAR.matcher(TEST_QUERY_2);
		while (m.find()) {
			String v = m.group(0);
			Assert.assertTrue("Variable " + v + " should be found.", expected.contains(v));
		}
	}

	@Test
	public void testFindWhereClause() {
		ResultDescriptor.QueryFragment pq = new ResultDescriptor.QueryFragment(TEST_QUERY_1);
		Assert.assertTrue(pq.whereClause.startsWith("?subClass"));
		Assert.assertTrue(pq.whereClause.endsWith("FILTER (?subClass != ?superClass)"));
	}

	@Test
	public void testPrefetchQuery() {
		String selectSubClasses = PREFIX + "SELECT DISTINCT ?subClass { ?subClass rdfs:subClassOf+ ?superClass . " //
				+ "FILTER (?subClass != ?superClass && isIRI(?subClass)) } ORDER BY ?subClass";
		String hasNamedSubClasses = PREFIX + "ASK { ?subClass rdfs:subClassOf ?superClass . "
				+ "FILTER (isIRI(?subClass) && ?subClass != ?superClass && ?subClass != owl:Nothing)}";
		IResultDescriptor<IClass> rd = new ResultDescriptor<IClass>(selectSubClasses)
				.prefetch(new ResultDescriptor<IClass>(hasNamedSubClasses,
						"komma:hasNamedSubClasses", "subClass", "superClass"));

		String query = rd.toQueryString().trim();
		Assert.assertTrue(query.toLowerCase().startsWith("construct"));
		Assert.assertTrue(query.contains("?subClass a <komma:Result>"));
		Assert.assertTrue(query.matches(".*\\?subClass\\s+<komma:hasNamedSubClasses>\\s+\\?.*"));
	}
}