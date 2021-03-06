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
		return Sequence(
				StringLiteral(),
				push(null),
				Optional(FirstOf(LANGTAG(), Sequence(String("^^"), IriRef())),
						drop(1) //
				), //
				push(peek() instanceof GraphNode ? //
				createTypedLiteral((String) pop(1), (GraphNode) pop())
						: createLiteral((String) pop(1), (String) pop()) //
				), WS());
	}

	public Rule NumericLiteral() {
		return FirstOf(NumericLiteralUnsigned(), NumericLiteralPositive(),
				NumericLiteralNegative());
	}

	public Rule NumericLiteralUnsigned() {
		return FirstOf(DOUBLE(), DECIMAL(), INTEGER());
	}

	public Rule NumericLiteralPositive() {
		return FirstOf(DOUBLE_POSITIVE(), DECIMAL_POSITIVE(),
				INTEGER_POSITIVE());
	}

	public Rule NumericLiteralNegative() {
		return FirstOf(DOUBLE_NEGATIVE(), DECIMAL_NEGATIVE(),
				INTEGER_NEGATIVE());
	}

	public Rule BooleanLiteral() {
		return Sequence(FirstOf("TRUE", "FALSE"), //
				push(new BooleanLiteral("true".equals(match().toLowerCase()))));
	}

	public Rule StringLiteral() {
		return FirstOf(STRING_LITERAL_LONG1(), STRING_LITERAL1(),
				STRING_LITERAL_LONG2(), STRING_LITERAL2());
	}

	public Rule IriRef() {
		return FirstOf(IRI_REF(), PrefixedName());
	}

	public String stripColon(String prefix) {
		return prefix.trim().replaceAll(":", "");
	}

	public String trim(String string) {
		return string != null ? string.trim() : null;
	}

	public Rule PrefixedName() {
		return FirstOf(PNAME_LN(), //
				Sequence(PNAME_NS(), WS(), //
						push(new QName((String) pop(), ""))));
	}

	public Rule BlankNode() {
		return Sequence(
				FirstOf(BLANK_NODE_LABEL(), Sequence('[', ']', push(null))),
				push(new BNode((String) pop())));
	}

	@SuppressNode
	public Rule WS() {
		return ZeroOrMore(FirstOf(COMMENT(), WS_NO_COMMENT()));
	}

	public Rule WS_NO_COMMENT() {
		return FirstOf(Ch(' '), Ch('\t'), Ch('\f'), EOL());
	}

	public Rule PNAME_NS() {
		return Sequence(Optional(PN_PREFIX()), push(match()), Ch(':'));
	}

	public Rule PNAME_LN() {
		return Sequence(PNAME_NS(), PN_LOCAL(), push(new QName((String) pop(1),
				((String) pop()).trim())));
	}

	public Rule IRI_REF() {
		return Sequence(
				LESS_NO_COMMENT(),
				ZeroOrMore(TestNot(FirstOf(IRI_REF_CHARS_WO_SPACE(), Ch(' '))),
						ANY), push(new IriRef(match().trim())), '>');
	}

	/**
	 * Rule that allows spaces in IRIs which are normally disallowed.
	 */
	public Rule IRI_REF_WSPACE() {
		return Sequence(LESS_NO_COMMENT(),
				ZeroOrMore(TestNot(IRI_REF_CHARS_WO_SPACE()), ANY),
				push(new IriRef(match().trim())), '>');
	}

	public Rule IRI_REF_CHARS_WO_SPACE() {
		return FirstOf(LESS_NO_COMMENT(), Ch('>'), Ch('"'), Ch('{'), Ch('}'),
				Ch('|'), Ch('^'), Ch('\\'), Ch('`'),
				CharRange('\u0000', '\u0019'));
	}

	public Rule BLANK_NODE_LABEL() {
		return Sequence(String("_:"), PN_LOCAL(), WS());
	}

	public Rule LANGTAG() {
		return Sequence(
				Ch('@'),
				Sequence(OneOrMore(PN_CHARS_BASE()),
						ZeroOrMore('-', OneOrMore(PN_CHARS_BASE(), DIGIT()))),
				push(match()));
	}

	public Rule INTEGER() {
		return Sequence(OneOrMore(DIGIT()),
				push(new IntegerLiteral(Integer.parseInt(match().trim()))),
				WS());
	}

	public Rule DECIMAL() {
		return Sequence(
				FirstOf(Sequence(OneOrMore(DIGIT()), '.', ZeroOrMore(DIGIT())),
						Sequence('.', OneOrMore(DIGIT()))),
				push(new DoubleLiteral(Double.parseDouble(match().trim()))),
				WS());
	}

	public Rule DOUBLE() {
		return Sequence(
				FirstOf(Sequence(OneOrMore(DIGIT()), '.', ZeroOrMore(DIGIT()),
						EXPONENT()),
						Sequence('.', OneOrMore(DIGIT()), EXPONENT()),
						Sequence(OneOrMore(DIGIT()), EXPONENT())),
				push(new DoubleLiteral(Double.parseDouble(match().trim()))),
				WS());
	}

	public Rule INTEGER_POSITIVE() {
		return Sequence('+', INTEGER());
	}

	public Rule DECIMAL_POSITIVE() {
		return Sequence('+', DECIMAL());
	}

	public Rule DOUBLE_POSITIVE() {
		return Sequence('+', DOUBLE());
	}

	public Rule INTEGER_NEGATIVE() {
		return Sequence('-', INTEGER(), //
				push(new IntegerLiteral(-((IntegerLiteral) pop()).getValue())) //
		);
	}

	public Rule DECIMAL_NEGATIVE() {
		return Sequence('-', DECIMAL(), //
				push(new DoubleLiteral(-((DoubleLiteral) pop()).getValue())) //
		);
	}

	public Rule DOUBLE_NEGATIVE() {
		return Sequence('-', DOUBLE(), //
				push(new DoubleLiteral(-((DoubleLiteral) pop()).getValue())) //
		);
	}

	public Rule EXPONENT() {
		return Sequence(IgnoreCase('e'), Optional(FirstOf('+', '-')),
				OneOrMore(DIGIT()));
	}

	public Rule STRING_LITERAL1() {
		return Sequence(
				Ch('\''),
				push(new StringBuilder()),
				ZeroOrMore(FirstOf(
						Sequence(TestNot(FirstOf('\'', '\\', '\n', '\r')), ANY,
								appendToSb(matchedChar())), ECHAR(), UCHAR())),
				push(pop().toString()), '\'', WS());
	}

	public Rule STRING_LITERAL2() {
		return Sequence(
				Ch('"'),
				push(new StringBuilder()),
				ZeroOrMore(FirstOf(
						Sequence(TestNot(FirstOf('"', '\\', '\n', '\r')), ANY,
								appendToSb(matchedChar())), ECHAR(), UCHAR())),
				push(pop().toString()), '"', WS());
	}

	public Rule STRING_LITERAL_LONG1() {
		return Sequence(
				String("'''"),
				push(new StringBuilder()),
				ZeroOrMore(
						TestNot("'''"),
						Optional(FirstOf("''", '\''), appendToSb(match())),
						FirstOf(Sequence(TestNot(FirstOf('\'', '\\')), ANY,
								appendToSb(matchedChar())), ECHAR(), UCHAR())),
				push(pop().toString()), "'''", WS());
	}

	public Rule STRING_LITERAL_LONG2() {
		return Sequence(
				"\"\"\"",
				push(new StringBuilder()),
				ZeroOrMore(
						TestNot("\"\"\""),
						Optional(FirstOf("\"\"", '\"'), appendToSb(match())),
						FirstOf(Sequence(TestNot(FirstOf('\"', '\\')), ANY,
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
		return Sequence(Ch(c), appendToSb(unescape(c)));
	}

	public Rule ECHAR() {
		return Sequence(
				'\\',
				FirstOf(Ech('t'), Ech('b'), Ech('n'), Ech('r'), Ech('f'),
						Ech('"'), Ech('\''), Ech('\\')));
	}

	public Rule UCHAR() {
		return FirstOf(
				Sequence(
						"\\u",
						Sequence(HEX(), HEX(), HEX(), HEX()),
						appendToSb(new String(Character.toChars(Integer
								.parseInt(match(), 16))))),
				Sequence(
						"\\U",
						Sequence(HEX(), HEX(), HEX(), HEX(), HEX(), HEX(),
								HEX(), HEX()),
						appendToSb(new String(Character.toChars(Integer
								.parseInt(match(), 16))))));
	}

	public Rule PN_CHARS_U() {
		return FirstOf(PN_CHARS_BASE(), Ch('_'));
	}

	public Rule PN_CHARS() {
		return FirstOf(PN_CHARS_U(), '-', DIGIT(), Ch('\u00B7'),
				CharRange('\u0300', '\u036F'), CharRange('\u203F', '\u2040'));
	}

	public Rule PN_PREFIX() {
		return Sequence(PN_CHARS_BASE(),
				ZeroOrMore(FirstOf(PN_CHARS(), Sequence('.', PN_CHARS()))));
	}

	public Rule PN_CHARS_SUFFIX() {
		return FirstOf(PN_CHARS(), Ch(':'), PLX());
	}

	public Rule PN_LOCAL() {
		return Sequence(
				Sequence(
						FirstOf(PN_CHARS_U(), Ch(':'), DIGIT(), PLX()),
						ZeroOrMore(FirstOf(PN_CHARS_SUFFIX(),
								Sequence(Ch('.'), PN_CHARS_SUFFIX())))),
				push(match()), WS());
	}

	public Rule PLX() {
		return FirstOf(PERCENT(), PN_LOCAL_ESC());
	}

	public Rule PERCENT() {
		return Sequence(Ch('%'), HEX(), HEX());
	}

	public Rule HEX() {
		return FirstOf(DIGIT(), CharRange('A', 'F'), CharRange('a', 'f'));
	}

	public Rule PN_LOCAL_ESC() {
		return Sequence(
				'\\',
				FirstOf('_', '~', '.', '-', '!', '$', '&', "'", '(', ')', '*',
						'+', ',', ';', '=', '/', '?', '#', '@', '%'));
	}

	public Rule PN_CHARS_BASE() {
		return FirstOf( //
				CharRange('A', 'Z'),//
				CharRange('a', 'z'), //
				CharRange('\u00C0', '\u00D6'), //
				CharRange('\u00D8', '\u00F6'), //
				CharRange('\u00F8', '\u02FF'), //
				CharRange('\u0370', '\u037D'), //
				CharRange('\u037F', '\u1FFF'), //
				CharRange('\u200C', '\u200D'), //
				CharRange('\u2070', '\u218F'), //
				CharRange('\u2C00', '\u2FEF'), //
				CharRange('\u3001', '\uD7FF'), //
				CharRange('\uF900', '\uFDCF'), //
				CharRange('\uFDF0', '\uFFFD') //
		);
	}

	public Rule DIGIT() {
		return CharRange('0', '9');
	}

	public Rule COMMENT() {
		return Sequence(Ch('#'), ZeroOrMore(TestNot(EOL()), ANY), EOL());
	}

	public Rule EOL() {
		return FirstOf(Ch('\n'), Ch('\r'));
	}

	public Rule LESS_NO_COMMENT() {
		return Sequence(Ch('<'), ZeroOrMore(WS_NO_COMMENT()));
	}

	@Override
	protected Rule fromCharLiteral(char c) {
		return Sequence(Ch(c), WS());
	}

	@Override
	protected Rule fromStringLiteral(String string) {
		return Sequence(String(string), WS());
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
