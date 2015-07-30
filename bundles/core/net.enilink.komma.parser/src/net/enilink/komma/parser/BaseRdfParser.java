package net.enilink.komma.parser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.enilink.komma.parser.sparql.tree.BNode;
import net.enilink.komma.parser.sparql.tree.BooleanLiteral;
import net.enilink.komma.parser.sparql.tree.DoubleLiteral;
import net.enilink.komma.parser.sparql.tree.GenericLiteral;
import net.enilink.komma.parser.sparql.tree.GraphNode;
import net.enilink.komma.parser.sparql.tree.IntegerLiteral;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.QName;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.SuppressNode;

public abstract class BaseRdfParser extends BaseParser<Object> {
	public static Object LIST_BEGIN = new Object();

	public GenericLiteral createLiteral(String label, String language) {
		return new GenericLiteral(label.trim(), null,
				language != null ? language.trim() : null);
	}

	public GenericLiteral createTypedLiteral(String label, GraphNode datatype) {
		return new GenericLiteral(label.trim(), datatype, null);
	}

	public Rule RdfLiteral() {
		return sequence(
				StringLiteral(),
				push(null),
				optional(firstOf(LANGTAG(), sequence(string("^^"), IriRef())),
						drop(1) //
				), //
				push(peek() instanceof GraphNode ? //
				createTypedLiteral((String) pop(1), (GraphNode) pop())
						: createLiteral((String) pop(1), (String) pop()) //
				), WS());
	}

	public Rule NumericLiteral() {
		return firstOf(NumericLiteralUnsigned(), NumericLiteralPositive(),
				NumericLiteralNegative());
	}

	public Rule NumericLiteralUnsigned() {
		return firstOf(DOUBLE(), DECIMAL(), INTEGER());
	}

	public Rule NumericLiteralPositive() {
		return firstOf(DOUBLE_POSITIVE(), DECIMAL_POSITIVE(),
				INTEGER_POSITIVE());
	}

	public Rule NumericLiteralNegative() {
		return firstOf(DOUBLE_NEGATIVE(), DECIMAL_NEGATIVE(),
				INTEGER_NEGATIVE());
	}

	public Rule BooleanLiteral() {
		return sequence(firstOf("TRUE", "FALSE"), //
				push(new BooleanLiteral("true".equals(match().toLowerCase()))));
	}

	public Rule StringLiteral() {
		return firstOf(STRING_LITERAL_LONG1(), STRING_LITERAL1(),
				STRING_LITERAL_LONG2(), STRING_LITERAL2());
	}

	public Rule IriRef() {
		return firstOf(IRI_REF(), PrefixedName());
	}

	public String stripColon(String prefix) {
		return prefix.trim().replaceAll(":", "");
	}

	public String trim(String string) {
		return string != null ? string.trim() : null;
	}

	public Rule PrefixedName() {
		return firstOf(PNAME_LN(), //
				sequence(PNAME_NS(), WS(), //
						push(new QName((String) pop(), ""))));
	}

	public Rule BlankNode() {
		return sequence(
				firstOf(BLANK_NODE_LABEL(), sequence('[', ']', push(null))),
				push(new BNode((String) pop())));
	}

	@SuppressNode
	public Rule WS() {
		return zeroOrMore(firstOf(COMMENT(), WS_NO_COMMENT()));
	}

	public Rule WS_NO_COMMENT() {
		return firstOf(ch(' '), ch('\t'), ch('\f'), EOL());
	}

	public Rule PNAME_NS() {
		return sequence(optional(PN_PREFIX()), push(match()), ch(':'));
	}

	public Rule PNAME_LN() {
		return sequence(PNAME_NS(), PN_LOCAL(), push(new QName((String) pop(1),
				((String) pop()).trim())));
	}

	public Rule IRI_REF() {
		return sequence(
				LESS_NO_COMMENT(),
				zeroOrMore(testNot(firstOf(IRI_REF_CHARS_WO_SPACE(), ch(' '))),
						ANY), push(new IriRef(match().trim())), '>');
	}

	/**
	 * Rule that allows spaces in IRIs which are normally disallowed.
	 */
	public Rule IRI_REF_WSPACE() {
		return sequence(LESS_NO_COMMENT(),
				zeroOrMore(testNot(IRI_REF_CHARS_WO_SPACE()), ANY),
				push(new IriRef(match().trim())), '>');
	}

	public Rule IRI_REF_CHARS_WO_SPACE() {
		return firstOf(LESS_NO_COMMENT(), ch('>'), ch('"'), ch('{'), ch('}'),
				ch('|'), ch('^'), ch('\\'), ch('`'),
				charRange('\u0000', '\u0019'));
	}

	public Rule BLANK_NODE_LABEL() {
		return sequence(string("_:"), PN_LOCAL(), WS());
	}

	public Rule LANGTAG() {
		return sequence(
				ch('@'),
				sequence(oneOrMore(PN_CHARS_BASE()),
						zeroOrMore('-', oneOrMore(PN_CHARS_BASE(), DIGIT()))),
				push(match()));
	}

	public Rule INTEGER() {
		return sequence(oneOrMore(DIGIT()),
				push(new IntegerLiteral(Integer.parseInt(match().trim()))),
				WS());
	}

	public Rule DECIMAL() {
		return sequence(
				firstOf(sequence(oneOrMore(DIGIT()), '.', zeroOrMore(DIGIT())),
						sequence('.', oneOrMore(DIGIT()))),
				push(new DoubleLiteral(Double.parseDouble(match().trim()))),
				WS());
	}

	public Rule DOUBLE() {
		return sequence(
				firstOf(sequence(oneOrMore(DIGIT()), '.', zeroOrMore(DIGIT()),
						EXPONENT()),
						sequence('.', oneOrMore(DIGIT()), EXPONENT()),
						sequence(oneOrMore(DIGIT()), EXPONENT())),
				push(new DoubleLiteral(Double.parseDouble(match().trim()))),
				WS());
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
				push(new IntegerLiteral(-((IntegerLiteral) pop()).getValue())) //
		);
	}

	public Rule DECIMAL_NEGATIVE() {
		return sequence('-', DECIMAL(), //
				push(new DoubleLiteral(-((DoubleLiteral) pop()).getValue())) //
		);
	}

	public Rule DOUBLE_NEGATIVE() {
		return sequence('-', DOUBLE(), //
				push(new DoubleLiteral(-((DoubleLiteral) pop()).getValue())) //
		);
	}

	public Rule EXPONENT() {
		return sequence(ignoreCase('e'), optional(firstOf('+', '-')),
				oneOrMore(DIGIT()));
	}

	public Rule STRING_LITERAL1() {
		return sequence(
				ch('\''),
				push(new StringBuilder()),
				zeroOrMore(firstOf(
						sequence(testNot(firstOf('\'', '\\', '\n', '\r')), ANY,
								appendToSb(matchedChar())), ECHAR(), UCHAR())),
				push(pop().toString()), '\'', WS());
	}

	public Rule STRING_LITERAL2() {
		return sequence(
				ch('"'),
				push(new StringBuilder()),
				zeroOrMore(firstOf(
						sequence(testNot(firstOf('"', '\\', '\n', '\r')), ANY,
								appendToSb(matchedChar())), ECHAR(), UCHAR())),
				push(pop().toString()), '"', WS());
	}

	public Rule STRING_LITERAL_LONG1() {
		return sequence(
				string("'''"),
				push(new StringBuilder()),
				zeroOrMore(
						testNot("'''"),
						optional(firstOf("''", '\''), appendToSb(match())),
						firstOf(sequence(testNot(firstOf('\'', '\\')), ANY,
								appendToSb(matchedChar())), ECHAR(), UCHAR())),
				push(pop().toString()), "'''", WS());
	}

	public Rule STRING_LITERAL_LONG2() {
		return sequence(
				"\"\"\"",
				push(new StringBuilder()),
				zeroOrMore(
						testNot("\"\"\""),
						optional(firstOf("\"\"", '\"'), appendToSb(match())),
						firstOf(sequence(testNot(firstOf('\"', '\\')), ANY,
								appendToSb(matchedChar())), ECHAR(), UCHAR())),
				push(pop().toString()), "\"\"\"", WS());
	}

	public boolean appendToSb(String s) {
		((StringBuilder) peek()).append(s);
		return true;
	}

	public boolean appendToSb(char c) {
		((StringBuilder) peek()).append(c);
		return true;
	}

	public char unescape(char c) {
		switch (c) {
		case 't':
			return '\t';
		case 'b':
			return '\b';
		case 'n':
			return '\n';
		case 'r':
			return '\r';
		case 'f':
			return '\f';
		default:
			return c;
		}
	}

	/**
	 * Unescapes the character <code>c</code> and appends it to a string builder
	 * on the value stack.
	 */
	public Rule Ech(char c) {
		return sequence(ch(c), appendToSb(unescape(c)));
	}

	public Rule ECHAR() {
		return sequence(
				'\\',
				firstOf(Ech('t'), Ech('b'), Ech('n'), Ech('r'), Ech('f'),
						Ech('"'), Ech('\''), Ech('\\')));
	}

	public Rule UCHAR() {
		return firstOf(
				sequence(
						"\\u",
						sequence(HEX(), HEX(), HEX(), HEX()),
						appendToSb(new String(Character.toChars(Integer
								.parseInt(match(), 16))))),
				sequence(
						"\\U",
						sequence(HEX(), HEX(), HEX(), HEX(), HEX(), HEX(),
								HEX(), HEX()),
						appendToSb(new String(Character.toChars(Integer
								.parseInt(match(), 16))))));
	}

	public Rule PN_CHARS_U() {
		return firstOf(PN_CHARS_BASE(), ch('_'));
	}

	public Rule PN_CHARS() {
		return firstOf(PN_CHARS_U(), '-', DIGIT(), ch('\u00B7'),
				charRange('\u0300', '\u036F'), charRange('\u203F', '\u2040'));
	}

	public Rule PN_PREFIX() {
		return sequence(PN_CHARS_BASE(),
				zeroOrMore(firstOf(PN_CHARS(), sequence('.', PN_CHARS()))));
	}

	public Rule PN_CHARS_SUFFIX() {
		return firstOf(PN_CHARS(), ch(':'), PLX());
	}

	public Rule PN_LOCAL() {
		return sequence(
				sequence(
						firstOf(PN_CHARS_U(), ch(':'), DIGIT(), PLX()),
						zeroOrMore(firstOf(PN_CHARS_SUFFIX(),
								sequence(ch('.'), PN_CHARS_SUFFIX())))),
				push(match()), WS());
	}

	public Rule PLX() {
		return firstOf(PERCENT(), PN_LOCAL_ESC());
	}

	public Rule PERCENT() {
		return sequence(ch('%'), HEX(), HEX());
	}

	public Rule HEX() {
		return firstOf(DIGIT(), charRange('A', 'F'), charRange('a', 'f'));
	}

	public Rule PN_LOCAL_ESC() {
		return sequence(
				'\\',
				firstOf('_', '~', '.', '-', '!', '$', '&', "'", '(', ')', '*',
						'+', ',', ';', '=', '/', '?', '#', '@', '%'));
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
		return sequence(ch('#'), zeroOrMore(testNot(EOL()), ANY), EOL());
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

	@SuppressWarnings("unchecked")
	public <T> List<T> popList(int start, Class<T> elementType, int additional) {
		LinkedList<T> list = new LinkedList<T>();
		Object element;
		while ((element = pop(start)) != LIST_BEGIN) {
			list.addFirst((T) element);
		}
		while (additional-- > 0) {
			list.addFirst((T) pop(start));
		}
		return new ArrayList<T>(list);
	}

	public <T> List<T> popList(int start, Class<T> elementType) {
		return popList(start, elementType);
	}

	public <T> List<T> popList(Class<T> elementType, int additional) {
		return popList(0, elementType, additional);
	}

	public <T> List<T> popList(Class<T> elementType) {
		return popList(elementType, 0);
	}
}
