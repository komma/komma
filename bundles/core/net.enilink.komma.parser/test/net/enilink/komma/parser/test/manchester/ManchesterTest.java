package net.enilink.komma.parser.test.manchester;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.ComparisonFailure;

import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.buffers.DefaultInputBuffer;
import org.parboiled.buffers.InputBuffer;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

import net.enilink.komma.parser.manchester.ManchesterActions;
import net.enilink.komma.parser.manchester.ManchesterSyntaxParser;
import net.enilink.komma.parser.test.GUnitBaseTestCase;

public class ManchesterTest extends GUnitBaseTestCase {

	ManchesterSyntaxParser parser = Parboiled.createParser(
			ManchesterSyntaxParser.class, new ManchesterActions());

	@Test
	public void objectProperty() throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(getClass()
				.getResourceAsStream("Ontology_ObjectProperty.gunit")));

		parse(in, 1);

	}

	@Test
	public void objectProperty_datatype_class() throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(getClass()
				.getResourceAsStream("Ontology_Datatype_Class.gunit")));

		parse(in, 2);
	}

	@Test
	public void dataProperty() throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(getClass()
				.getResourceAsStream("Ontology_DataProperty.gunit")));

		parse(in, 3);
	}

	@Test
	public void individual() throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(getClass()
				.getResourceAsStream("Ontology_Individual.gunit")));

		parse(in, 4);
	}

	@Test
	public void annotationProperty() throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(getClass()
				.getResourceAsStream("Ontology_AnnotationProperty.gunit")));

		parse(in, 5);
	}

	@Test
	public void misc() throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(getClass()
				.getResourceAsStream("Ontology_Misc.gunit")));

		parse(in, 6);
	}

	private void parse(BufferedReader in, int testcaseNumber) throws Exception {

		System.out
				.println("\n\n*************************************************");
		System.out.println("Testcase: " + testcaseNumber);

		int failures = 0;

		for (TextInfo textInfo : getTextInfos(in)) {
			System.out.println(textInfo.text);

			ParsingResult<Object> result = new ReportingParseRunner<Object>(
					parser.OntologyDocument()).run(textInfo.text);

			InputBuffer inputBuffer = new DefaultInputBuffer(
					textInfo.text.toCharArray());

			boolean passed = result.hasErrors()
					&& textInfo.result == Result.FAIL || !result.hasErrors()
					&& textInfo.result == Result.OK;

			if (result.hasErrors() && textInfo.result == Result.OK) {
				System.out.println(ErrorUtils.printParseErrors(result));
			}

			if (textInfo.pathCheck.size() > 0) {
				Set<String> keySet = textInfo.pathCheck.keySet();
				for (Iterator<String> iterator = keySet.iterator(); iterator
						.hasNext();) {
					String pfad = iterator.next();
					String expected = textInfo.pathCheck.get(pfad);

					try {
						assertNode(result, pfad, expected, inputBuffer);
					} catch (ComparisonFailure e) {
						System.err.println(e);
						passed = false;
					}

				}
			}

			if (!passed) {
				failures++;
				System.err.println(textInfo.text + " --> " + textInfo.result);
			}

			System.out.println(ParseTreeUtils.printNodeTree(result));

			System.out.println("*************************\n\n");
		}
		Assert.assertEquals(0, failures);
	}

}
