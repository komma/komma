package net.enilink.komma.parser.sparql;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.parboiled.Node;
import org.parboiled.support.InputBuffer;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

public class GUnitBaseTestCase {

	protected enum Result {
		OK, FAIL
	};
	
	public GUnitBaseTestCase() {
		super();
	}

	
	protected class TextInfo {
		public Result result;
		public String text;
		public HashMap<String, String> pfadCheck;

		public TextInfo(Result result, String text) {
			super();
			this.result = result;
			this.text = text;
			pfadCheck = new HashMap<String, String>();
		}
	}
	
	
	
	private TextInfo parseText(BufferedReader in) throws Exception {
		Pattern unicodes = Pattern.compile("\\\\u([0-9a-fA-F]{4})");

		StringBuffer text = new StringBuffer();
		HashMap<String, String> tmpPfadcheck = new HashMap<String, String>();
		while (in.ready()) {
			String line = in.readLine();
			if (line.startsWith(">>")) {
				
				TextInfo textInfo =  
				new TextInfo(
						line.toLowerCase().contains("ok") ? Result.OK
								: Result.FAIL, text.toString());
				textInfo.pfadCheck = tmpPfadcheck;
				
				return textInfo;
				
			} else if(line.startsWith("@check")) { 
			
				String pfad= line.replace("@check:", "");
				String[] s = pfad.split("=");
				if(s.length == 2) {
					tmpPfadcheck.put(s[0],s[1]);
				} else {
					tmpPfadcheck.put(s[0],"");
				}
				
				
			
			} else {
				Matcher matcher = unicodes.matcher(line);
				while (matcher.find()) {
					matcher.appendReplacement(text, Character
							.toString((char) Integer.parseInt(matcher.group(1),
									16)));
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
	
	public void assertNode(ParsingResult<Object> result,String pfad,String expected,InputBuffer inputBuffer) throws Exception {
		
		ArrayList<Node<Object>> list = new ArrayList<Node<Object>>();
		list = ParseTreeUtils.collectNodesByPath((Node<Object>) result.parseTreeRoot, pfad,
				list);
		
		Assert.assertTrue("Pfad nicht gefunden: "+pfad,list.size()>0);
		
		String s = ParseTreeUtils.getNodeText(list.get(0), inputBuffer);
		
		if(!"".equals(expected)) {
			Assert.assertEquals("Nodecheck failed: "+pfad,expected,s);
		}	
		
	}

}