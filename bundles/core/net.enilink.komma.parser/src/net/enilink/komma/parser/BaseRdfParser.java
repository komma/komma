package net.enilink.komma.parser;

import org.parboiled.BaseParser;
import org.parboiled.Rule;

import net.enilink.komma.parser.sparql.tree.BNode;
import net.enilink.komma.parser.sparql.tree.BooleanLiteral;
import net.enilink.komma.parser.sparql.tree.DoubleLiteral;
import net.enilink.komma.parser.sparql.tree.GenericLiteral;
import net.enilink.komma.parser.sparql.tree.GraphNode;
import net.enilink.komma.parser.sparql.tree.IntegerLiteral;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.QName;

public abstract class BaseRdfParser extends BaseParser<Object> {

	public GenericLiteral createLiteral(String label, String language) {
		return new GenericLiteral(label.trim(), null,
				language != null ? language.trim() : null);
	}

	public GenericLiteral createTypedLiteral(String label, GraphNode datatype) {
		return new GenericLiteral(label.trim(), datatype, null);
	}

	public Rule RdfLiteral() {
		return Sequence(
				String(),
				Optional(FirstOf(LANGTAG(), Sequence("^^", IriRef()))), //
				set(node("O/F/S/IriRef") != null ? //
				createTypedLiteral((String) value("String"),
						(GraphNode) value("O/F/S/IriRef")) : createLiteral(
						(String) value("String"), text("O/F/LANGTAG")) //
				));
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
				set(new BooleanLiteral("true".equals(text("").toLowerCase()))));
	}

	public Rule String() {
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
				Sequence(PNAME_NS(), //
						set(new QName(stripColon(text("PNAME_NS")), ""))));
	}

	public Rule BlankNode() {
		return Sequence(FirstOf(BLANK_NODE_LABEL(), Sequence('[', ']')),
				set(new BNode(trim(text("f/B")))));
	}

	public Rule WS() {
		return ZeroOrMore(FirstOf(COMMENT(), WS_NO_COMMENT()));
	}

	public Rule WS_NO_COMMENT() {
		return FirstOf(Ch(' '), Ch('\t'), Ch('\f'), EOL());
	}

	public Rule PNAME_NS() {
		return Sequence(Optional(PN_PREFIX()), ':');
	}

	public Rule PNAME_LN() {
		return Sequence(PNAME_NS(), PN_LOCAL(), set(new QName(
				stripColon(text("PNAME_NS")), text("PN_LOCAL").trim())));
	}

	public Rule IRI_REF() {
		return Sequence(
				LESS_NO_COMMENT(),
				ZeroOrMore(Sequence(
						TestNot(FirstOf(LESS_NO_COMMENT(), Ch('>'), Ch('"'),
								Ch('{'), Ch('}'), Ch('|'), Ch('^'), Ch('\\'),
								Ch('`'), CharRange('\u0000', '\u0020'))), Any())),
				'>', set(new IriRef(text("Z").trim())));
	}

	public Rule BLANK_NODE_LABEL() {
		return Sequence(String("_:"), PN_LOCAL(), WS());
	}

	public Rule LANGTAG() {
		return Sequence(
				Ch('@'),
				OneOrMore(PN_CHARS_BASE()),
				ZeroOrMore(Sequence('-',
						OneOrMore(Sequence(PN_CHARS_BASE(), DIGIT())))), WS());
	}

	public Rule INTEGER() {
		return Sequence(OneOrMore(DIGIT()), WS(), //
				set(new IntegerLiteral(Integer.parseInt((text(""))))) //
		);
	}

	public Rule DECIMAL() {
		return Sequence(
				FirstOf(Sequence(OneOrMore(DIGIT()), '.', ZeroOrMore(DIGIT())),
						Sequence('.', OneOrMore(DIGIT()))), WS(), //
				set(new DoubleLiteral(Double.parseDouble((text(""))))));
	}

	public Rule DOUBLE() {
		return Sequence(
				FirstOf(Sequence(OneOrMore(DIGIT()), '.', ZeroOrMore(DIGIT()),
						EXPONENT()),
						Sequence('.', OneOrMore(DIGIT()), EXPONENT()),
						Sequence(OneOrMore(DIGIT()), EXPONENT())), WS(), //
				set(new DoubleLiteral(Double.parseDouble(text("")))));
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
				set(new IntegerLiteral(-((IntegerLiteral) value()).getValue())) //
		);
	}

	public Rule DECIMAL_NEGATIVE() {
		return Sequence('-', DECIMAL(), //
				set(new DoubleLiteral(-((DoubleLiteral) value()).getValue())) //
		);
	}

	public Rule DOUBLE_NEGATIVE() {
		return Sequence('-', DOUBLE(), //
				set(new DoubleLiteral(-((DoubleLiteral) value()).getValue())) //
		);
	}

	public Rule EXPONENT() {
		return Sequence(CharIgnoreCase('e'), Optional(FirstOf('+', '-')),
				OneOrMore(DIGIT()));
	}

	public Rule STRING_LITERAL1() {
		return Sequence(
				Ch('\''),
				ZeroOrMore(FirstOf(
						Sequence(
								TestNot(FirstOf(Ch('\''), Ch('\\'), Ch('\n'),
										Ch('\r'))), Any()), ECHAR())),
				set(text("Z").replaceAll("\\'", "'")), Ch('\''), WS());
	}

	public Rule STRING_LITERAL2() {
		return Sequence(
				Ch('"'),
				ZeroOrMore(FirstOf(
						Sequence(
								TestNot(FirstOf(Ch('"'), Ch('\\'), Ch('\n'),
										Ch('\r'))), Any()), ECHAR())),
				set(text("Z").replaceAll("\\\"", "\"")), Ch('"'), WS());
	}

	public Rule STRING_LITERAL_LONG1() {
		return Sequence(
				String("'''"),
				ZeroOrMore(Sequence(
						Optional(FirstOf(String("''"), Ch('\''))),
						FirstOf(Sequence(TestNot(FirstOf(Ch('\''), Ch('\\'))),
								Any()), ECHAR()))),
				set(text("Z").replaceAll("\\'", "'")), String("'''"), WS());
	}

	public Rule STRING_LITERAL_LONG2() {
		return Sequence(
				String("\"\"\""),
				ZeroOrMore(Sequence(
						Optional(FirstOf(String("\"\""), Ch('\"'))),
						FirstOf(Sequence(TestNot(FirstOf(Ch('\"'), Ch('\\'))),
								Any()), ECHAR()))),
				set(text("Z").replaceAll("\\\"", "\"")), String("\"\"\""), WS());
	}

	public Rule ECHAR() {
		return Sequence(
				Ch('\\'),
				FirstOf(Ch('t'), Ch('b'), Ch('n'), Ch('r'), Ch('f'), Ch('\\'),
						Ch('"'), Ch('\'')));
	}

	public Rule PN_CHARS_U() {
		return FirstOf(PN_CHARS_BASE(), Ch('_'));
	}

	public Rule PN_CHARS() {
		return FirstOf('-', DIGIT(), PN_CHARS_U(), Ch('\u00B7'),
				CharRange('\u0300', '\u036F'), CharRange('\u203F', '\u2040'));
	}

	public Rule PN_PREFIX() {
		return Sequence(
				PN_CHARS_BASE(),
				Optional(ZeroOrMore(FirstOf(PN_CHARS(),
						Sequence('.', PN_CHARS())))));
	}

	public Rule PN_LOCAL() {
		return Sequence(
				FirstOf(PN_CHARS_U(), DIGIT()),
				Optional(ZeroOrMore(FirstOf(PN_CHARS(),
						Sequence('.', PN_CHARS())))), WS());
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
		return Sequence(Ch('#'), ZeroOrMore(Sequence(TestNot(EOL()), Any())),
				EOL());
	}

	public Rule EOL() {
		return FirstOf(Ch('\n'), Ch('\r'));
	}

	public Rule LESS_NO_COMMENT() {
		return Sequence(Ch('<'), ZeroOrMore(WS_NO_COMMENT()));
	}

	@Override
	protected Rule FromCharLiteral(char c) {
		return Sequence(Ch(c), WS());
	}

	@Override
	protected Rule FromStringLiteral(String string) {
		return Sequence(String(string), WS());
	}
}
