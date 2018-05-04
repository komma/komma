package net.enilink.komma.parser.test;

import net.enilink.komma.core.URI;
import net.enilink.komma.parser.manchester.ManchesterSyntaxParser;
import net.enilink.komma.parser.sparql.tree.BNode;
import net.enilink.komma.parser.sparql.tree.DoubleLiteral;
import net.enilink.komma.parser.sparql.tree.GenericLiteral;
import net.enilink.komma.parser.sparql.tree.IntegerLiteral;
import net.enilink.komma.parser.sparql.tree.QName;
import net.enilink.vocab.owl.OWL;

import org.junit.Assert;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

public class ManchesterSyntaxTest extends Assert {

	public interface IValidator {
		public void validate(URI type, QName property, Object value);
	}

	@Test
	public void test() {
		testRestriction("property value \"5\"^^type", new IValidator() {
			@Override
			public void validate(URI type, QName property, Object value) {
				String label = ((GenericLiteral) value).getLabel();
				String datatype = ((GenericLiteral) value).getDatatype()
						.toString();

				assertEquals(OWL.PROPERTY_HASVALUE, type);
				assertEquals("{}property", ((QName) property).toString());
				assertEquals("5", label);
				assertEquals("{}type", datatype);
			}
		});
		testRestriction("property value 5", new IValidator() {
			@Override
			public void validate(URI type, QName property, Object value) {
				int valueInt = ((IntegerLiteral) value).getValue();
				assertEquals(OWL.PROPERTY_HASVALUE, type);
				assertEquals("{}property", ((QName) property).toString());
				assertEquals(5, valueInt);
			}
		});
		testRestriction("property value 5.0", new IValidator() {
			@Override
			public void validate(URI type, QName property, Object value) {
				double valueInt = ((DoubleLiteral) value).getValue();
				assertEquals(OWL.PROPERTY_HASVALUE, type);
				assertEquals("{}property", ((QName) property).toString());
				assertEquals(5.0, valueInt, 0);
			}
		});
		testRestriction("property value xyz", new IValidator() {
			@Override
			public void validate(URI type, QName property, Object value) {
				assertEquals(OWL.PROPERTY_HASVALUE, type);
				assertEquals("{}property", ((QName) property).toString());
				assertEquals("{}xyz", ((QName) value).toString());
			}
		});
	}

	private void testRestriction(String input, final IValidator validator) {
		final ManchesterSyntaxParser parser = Parboiled
				.createParser(ManchesterSyntaxParser.class);
		ParsingResult<Object> result = new ReportingParseRunner<Object>(
				parser.Description()).run(input);
		assertTrue(BNode.class.equals(result.resultValue.getClass()));
	}
}
