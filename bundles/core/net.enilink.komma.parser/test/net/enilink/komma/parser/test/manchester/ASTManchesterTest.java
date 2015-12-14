package net.enilink.komma.parser.test.manchester;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ComparisonFailure;
import org.junit.Test;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import com.google.common.base.Joiner;

import net.enilink.komma.parser.manchester.ManchesterActions;
import net.enilink.komma.parser.manchester.ManchesterSyntaxParser;
import net.enilink.komma.parser.test.GUnitBaseTestCase;

public class ASTManchesterTest extends GUnitBaseTestCase {

	private static ManchesterSyntaxParser parser;

	@BeforeClass
	public static void before() {
		parser = Grappa.createParser(ManchesterSyntaxParser.class, new ManchesterActions());
	}

	@Test
	public void ast() throws Exception {
		BufferedReader in = new BufferedReader(
				new InputStreamReader(getClass().getResourceAsStream("Ontology_AST.gunit")));

		int failures = 0;
		for (TextInfo textInfo : getTextInfos(in)) {
			ParsingResult<Object> result = new ListeningParseRunner<Object>(parser.OntologyDocument())
					.run(textInfo.text);

			InputBuffer inputBuffer = new DefaultInputBuffer(textInfo.text.toCharArray());

			boolean passed = !result.isSuccess() && textInfo.result == Result.FAIL
					|| result.isSuccess() && textInfo.result == Result.OK;

			if (result.isSuccess() && textInfo.result == Result.OK) {
				System.out.println(Joiner.on("---\n").join(result.parseErrors));
			}

			if (textInfo.pathCheck.size() > 0) {
				Set<String> keySet = textInfo.pathCheck.keySet();
				for (Iterator<String> iterator = keySet.iterator(); iterator.hasNext();) {
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
