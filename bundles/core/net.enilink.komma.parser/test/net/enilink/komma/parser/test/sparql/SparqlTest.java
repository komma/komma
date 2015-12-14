/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.parser.test.sparql;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import com.github.parboiled1.grappa.stack.DefaultValueStack;

import net.enilink.komma.parser.sparql.SparqlParser;
import net.enilink.komma.parser.test.GUnitBaseTestCase;

/**
 * Simple JUnit Test for the SPARQL Parser
 * 
 * @author Ken Wenzel
 */
public class SparqlTest extends GUnitBaseTestCase {
	SparqlParser parser = Grappa.createParser(SparqlParser.class);

	@Test
	public void test() throws Exception {
		int failures = 0;
		BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("Sparql.gunit")));

		for (TextInfo textInfo : getTextInfos(in)) {
			ParsingResult<Object> result = new ListeningParseRunner<Object>(parser.Query())
					.run((CharSequence) textInfo.text);

			boolean passed = !result.isSuccess() && textInfo.result == Result.FAIL
					|| result.isSuccess() && textInfo.result == Result.OK;

			if (!result.isSuccess() && textInfo.result == Result.OK) {
				// System.out.println("Parse Tree:\n"
				// + GraphUtils.printTree(result.parseTreeRoot,
				// new ToStringFormatter<org.parboiled.Node<Node>>(
				// null)) + '\n');
				System.out.println(ErrorUtils.printParseErrors(result));
			}

			// if (!result.hasErrors()) {
			// Query query = (Query) result.parseTreeRoot.getValue();
			// System.out.println("ORIGINAL:\n" + textInfo.text);
			//
			// System.out.println("PARSED:\n"
			// + query.accept(new ToStringVisitor(),
			// new StringBuilder()).toString());
			// System.out.println();
			// }

			if (!passed) {
				failures++;
				System.err.println(textInfo.text + " --> " + textInfo.result);
			}
		}
		// currently 12 tests require semantic validation and therefore fail
		Assert.assertEquals(12, failures);
	}

}
