package net.enilink.komma.parser;

import org.parboiled.BaseParser;
import org.parboiled.Rule;

import net.enilink.komma.parser.sparql.tree.BNode;
import net.enilink.komma.parser.sparql.tree.BooleanLiteral;
import net.enilink.komma.parser.sparql.tree.DoubleLiteral;
import net.enilink.komma.parser.sparql.tree.GenericLiteral;
import net.enilink.komma.parser.sparql.tree.IntegerLiteral;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.QName;

public abstract class BaseRdfParser extends BaseParser<Object> {

	public GenericLiteral createLiteral(String label, String language) {
		return new GenericLiteral(label.trim(), null,
				language != null ? language.trim() : null);
	}

	public GenericLiteral createTypedLiteral(String label, String datatype) {
		return new GenericLiteral(label.trim(), datatype != null ? datatype
				.trim() : null, null);
	}

	public Rule rdfLiteral() {
		return sequence(string(), optional(firstOf(LANGTAG(), sequence("^^",
				iriRef()))), //
				SET(NODE("o/f/s/iriRef") != null ? //
				createTypedLiteral((String) VALUE("string"),
						TEXT("o/f/s/iriRef"))
						: createLiteral((String) VALUE("string"),
								TEXT("o/f/LANGTAG")) //
				));
	}

	public Rule numericLiteral() {
		return firstOf(numericLiteralUnsigned(), numericLiteralPositive(),
				numericLiteralNegative());
	}

	public Rule numericLiteralUnsigned() {
		return firstOf(DOUBLE(), DECIMAL(), INTEGER());
	}

	public Rule numericLiteralPositive() {
		return firstOf(DOUBLE_POSITIVE(), DECIMAL_POSITIVE(),
				INTEGER_POSITIVE());
	}

	public Rule numericLiteralNegative() {
		return firstOf(DOUBLE_NEGATIVE(), DECIMAL_NEGATIVE(),
				INTEGER_NEGATIVE());
	}

	public Rule booleanLiteral() {
		return sequence(firstOf("TRUE", "FALSE"), //
				SET(new BooleanLiteral("true".equals(TEXT("").toLowerCase()))));
	}

	public Rule string() {
		return firstOf(STRING_LITERAL_LONG1(), STRING_LITERAL1(),
				STRING_LITERAL_LONG2(), STRING_LITERAL2());
	}

	public Rule iriRef() {
		return firstOf(IRI_REF(), prefixedName());
	}

	public String stripColon(String prefix) {
		return prefix.trim().replaceAll(":", "");
	}

	public String trim(String string) {
		return string != null ? string.trim() : null;
	}

	public Rule prefixedName() {
		return firstOf(PNAME_LN(), //
				sequence(PNAME_NS(), //
						SET(new QName(stripColon(TEXT("PNAME_NS")), ""))));
	}

	public Rule blankNode() {
		return sequence(firstOf(BLANK_NODE_LABEL(), sequence('[', ']')),
				SET(new BNode(trim(TEXT("f/B")))));
	}

	public Rule WS() {
		return zeroOrMore(firstOf(COMMENT(), WS_NO_COMMENT()));
	}

	public Rule WS_NO_COMMENT() {
		return firstOf(ch(' '), ch('\t'), ch('\f'), EOL());
	}

	public Rule PNAME_NS() {
		return sequence(optional(PN_PREFIX()), ':');
	}

	public Rule PNAME_LN() {
		return sequence(PNAME_NS(), PN_LOCAL(), SET(new QName(
				stripColon(TEXT("PNAME_NS")), TEXT("PN_LOCAL").trim())));
	}

	public Rule IRI_REF() {
		return sequence(LESS_NO_COMMENT(), zeroOrMore(sequence(testNot(firstOf(
				LESS_NO_COMMENT(), ch('>'), ch('"'), ch('{'), ch('}'), ch('|'),
				ch('^'), ch('\\'), ch('`'), charRange('\u0000', '\u0020'))),
				any())), '>', SET(new IriRef(TEXT("z").trim())));
	}

	public Rule BLANK_NODE_LABEL() {
		return sequence(string("_:"), PN_LOCAL(), WS());
	}

	public Rule LANGTAG() {
		return sequence(ch('@'), oneOrMore(PN_CHARS_BASE()),
				zeroOrMore(sequence('-', oneOrMore(sequence(PN_CHARS_BASE(),
						DIGIT())))), WS());
	}

	public Rule INTEGER() {
		return sequence(oneOrMore(DIGIT()), WS(), //
				SET(new IntegerLiteral(Integer.parseInt((TEXT(""))))) //
		);
	}

	public Rule DECIMAL() {
		return sequence(firstOf(sequence(oneOrMore(DIGIT()), '.',
				zeroOrMore(DIGIT())), sequence('.', oneOrMore(DIGIT()))), WS(), //
				SET(new DoubleLiteral(Double.parseDouble((TEXT(""))))));
	}

	public Rule DOUBLE() {
		return sequence(firstOf(sequence(oneOrMore(DIGIT()), '.',
				zeroOrMore(DIGIT()), EXPONENT()), sequence('.',
				oneOrMore(DIGIT()), EXPONENT()), sequence(oneOrMore(DIGIT()),
				EXPONENT())), WS(), //
				SET(new DoubleLiteral(Double.parseDouble(TEXT("")))));
	}

	public Rule INTEGER_POSITIVE() {
		return sequence('+', INTEGER());
	}

	public Rule DECIMAL_POSITIVE() {
		return sequence('+', DECIMAL());
	}

	public Rule DOUBLE_POSITIVE() {
		return sequence('+', DOUBLE());
	}

	public Rule INTEGER_NEGATIVE() {
		return sequence('-', INTEGER(), //
				SET(new IntegerLiteral(-((IntegerLiteral) VALUE()).getValue())) //
		);
	}

	public Rule DECIMAL_NEGATIVE() {
		return sequence('-', DECIMAL(), //
				SET(new DoubleLiteral(-((DoubleLiteral) VALUE()).getValue())) //
		);
	}

	public Rule DOUBLE_NEGATIVE() {
		return sequence('-', DOUBLE(), //
				SET(new DoubleLiteral(-((DoubleLiteral) VALUE()).getValue())) //
		);
	}

	public Rule EXPONENT() {
		return sequence(charIgnoreCase('e'), optional(firstOf('+', '-')),
				oneOrMore(DIGIT()));
	}

	public Rule STRING_LITERAL1() {
		return sequence(ch('\''), zeroOrMore(firstOf(sequence(testNot(firstOf(
				ch('\''), ch('\\'), ch('\n'), ch('\r'))), any()), ECHAR())),
				SET(TEXT("z").replaceAll("\\'", "'")), ch('\''), WS());
	}

	public Rule STRING_LITERAL2() {
		return sequence(ch('"'), zeroOrMore(firstOf(sequence(testNot(firstOf(
				ch('"'), ch('\\'), ch('\n'), ch('\r'))), any()), ECHAR())),
				SET(TEXT("z").replaceAll("\\\"", "\"")), ch('"'), WS());
	}

	public Rule STRING_LITERAL_LONG1() {
		return sequence(string("'''"), zeroOrMore(sequence(optional(firstOf(
				string("''"), ch('\''))), firstOf(sequence(testNot(firstOf(
				ch('\''), ch('\\'))), any()), ECHAR()))), SET(TEXT("z")
				.replaceAll("\\'", "'")), string("'''"), WS());
	}

	public Rule STRING_LITERAL_LONG2() {
		return sequence(string("\"\"\""), zeroOrMore(sequence(optional(firstOf(
				string("\"\""), ch('\"'))), firstOf(sequence(testNot(firstOf(
				ch('\"'), ch('\\'))), any()), ECHAR()))), SET(TEXT("z")
				.replaceAll("\\\"", "\"")), string("\"\"\""), WS());
	}

	public Rule ECHAR() {
		return sequence(ch('\\'), firstOf(ch('t'), ch('b'), ch('n'), ch('r'),
				ch('f'), ch('\\'), ch('"'), ch('\'')));
	}

	public Rule PN_CHARS_U() {
		return firstOf(PN_CHARS_BASE(), ch('_'));
	}

	public Rule PN_CHARS() {
		return firstOf('-', DIGIT(), PN_CHARS_U(), ch('\u00B7'), charRange(
				'\u0300', '\u036F'), charRange('\u203F', '\u2040'));
	}

	public Rule PN_PREFIX() {
		return sequence(PN_CHARS_BASE(), optional(zeroOrMore(firstOf(
				PN_CHARS(), sequence('.', PN_CHARS())))));
	}

	public Rule PN_LOCAL() {
		return sequence(firstOf(PN_CHARS_U(), DIGIT()),
				optional(zeroOrMore(firstOf(PN_CHARS(), sequence('.',
						PN_CHARS())))), WS());
	}

	public Rule PN_CHARS_BASE() {
		return firstOf( //
				charRange('A', 'Z'),//
				charRange('a', 'z'), //
				charRange('\u00C0', '\u00D6'), //
				charRange('\u00D8', '\u00F6'), //
				charRange('\u00F8', '\u02FF'), //
				charRange('\u0370', '\u037D'), //
				charRange('\u037F', '\u1FFF'), //
				charRange('\u200C', '\u200D'), //
				charRange('\u2070', '\u218F'), //
				charRange('\u2C00', '\u2FEF'), //
				charRange('\u3001', '\uD7FF'), //
				charRange('\uF900', '\uFDCF'), //
				charRange('\uFDF0', '\uFFFD') //
		);
	}

	public Rule DIGIT() {
		return charRange('0', '9');
	}

	public Rule COMMENT() {
		return sequence(ch('#'), zeroOrMore(sequence(testNot(EOL()), any())),
				EOL());
	}

	public Rule EOL() {
		return firstOf(ch('\n'), ch('\r'));
	}

	public Rule LESS_NO_COMMENT() {
		return sequence(ch('<'), zeroOrMore(WS_NO_COMMENT()));
	}

	@Override
	protected Rule fromCharLiteral(char c) {
		return sequence(ch(c), WS());
	}

	@Override
	protected Rule fromStringLiteral(String string) {
		return sequence(string(string), WS());
	}
}
