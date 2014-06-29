package net.enilink.komma.parser.test;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;

import org.parboiled.Node;
import org.parboiled.buffers.InputBuffer;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

public class GUnitBaseTestCase {

	protected enum Result {
		OK, FAIL
	};

	protected class TextInfo {
		public Result result;
		public String text;
		public Map<String, String> pathCheck;

		public TextInfo(Result result, String text) {
			this.result = result;
			this.text = text;
		}
	}

	private TextInfo parseText(BufferedReader in) throws Exception {
		Pattern unicodes = Pattern.compile("\\\\u([0-9a-fA-F]{4})");

		StringBuffer text = new StringBuffer();
		Map<String, String> pathCheck = new HashMap<String, String>();
		while (in.ready()) {
			String line = in.readLine();
			if (line.startsWith(">>")) {

				TextInfo textInfo = new TextInfo(line.toLowerCase().contains(
						"ok") ? Result.OK : Result.FAIL, text.toString());
				textInfo.pathCheck = pathCheck;
				pathCheck.clear();

				return textInfo;
			} else if (line.startsWith("@check")) {
				String path = line.replace("@check:", "");
				String[] s = path.split("=");
				if (s.length == 2) {
					pathCheck.put(s[0], s[1]);
				} else {
					pathCheck.put(s[0], "");
				}
			} else {
				Matcher matcher = unicodes.matcher(line);
				while (matcher.find()) {
					matcher.appendReplacement(
							text,
							Character.toString((char) Integer.parseInt(
									matcher.group(1), 16)));
				}
				matcher.appendTail(text);
				text.append('\n');
			}
		}
		throw new NoSuchElementException("Expected \">>\"");
	}

	public List<TextInfo> getTextInfos(BufferedReader in) throws Exception {
		List<TextInfo> textInfos = new ArrayList<TextInfo>();

		while (in.ready()) {
			if (in.read() == '<' && in.ready() && in.read() == '<') {
				in.readLine();

				textInfos.add(parseText(in));
			}
		}
		return textInfos;
	}

	public void assertNode(ParsingResult<Object> result, String path,
			String expected, InputBuffer inputBuffer) throws Exception {

		List<Node<Object>> nodes = ParseTreeUtils.collectNodesByPath(
				(Node<Object>) result.parseTreeRoot, path,
				new ArrayList<Node<Object>>());

		Assert.assertTrue("Path not found: " + path, nodes.size() > 0);

		String s = ParseTreeUtils.getNodeText(nodes.get(0), inputBuffer);

		if (!"".equals(expected)) {
			Assert.assertEquals("Node check failed: " + path, expected, s);
		}

	}

}