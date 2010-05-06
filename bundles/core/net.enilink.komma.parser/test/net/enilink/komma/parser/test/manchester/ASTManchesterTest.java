package net.enilink.komma.parser.test.manchester;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.ComparisonFailure;

import org.junit.BeforeClass;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.ReportingParseRunner;
import org.parboiled.common.StringUtils;
import org.parboiled.support.InputBuffer;
import org.parboiled.support.ParsingResult;

import net.enilink.komma.parser.manchester.ManchesterActions;
import net.enilink.komma.parser.manchester.ManchesterSyntaxParser;
import net.enilink.komma.parser.test.GUnitBaseTestCase;

public class ASTManchesterTest extends GUnitBaseTestCase {

	
	private static ManchesterSyntaxParser parser;
	
	@BeforeClass
	public static void before() {
		parser = Parboiled
		.createParser(ManchesterSyntaxParser.class);

		parser.setManchesterActions(new ManchesterActions());	
	}
	

	

	@Test
	public void ast() throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(getClass()
				.getResourceAsStream("Ontology_AST.gunit")));

		int failures = 0;

		
		for (TextInfo textInfo : getTextInfos(in)) {

			ParsingResult<Object> result = new ReportingParseRunner<Object>(
					parser.ontologyDocument(), textInfo.text).run();

			InputBuffer inputBuffer = new InputBuffer(textInfo.text);

			boolean passed = result.hasErrors()
					&& textInfo.result == Result.FAIL || !result.hasErrors()
					&& textInfo.result == Result.OK;

			if (result.hasErrors() && textInfo.result == Result.OK) {
				System.out.println(StringUtils
						.join(result.parseErrors, "---\n"));
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
		}
		Assert.assertEquals(0, failures);
		
		
		
	}

}
