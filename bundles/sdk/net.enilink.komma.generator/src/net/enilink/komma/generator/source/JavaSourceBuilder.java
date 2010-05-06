/*
 * Copyright (c) 2008, 2010, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package net.enilink.komma.generator.source;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.URI;

public class JavaSourceBuilder {
	private static Collection<String> keywords = Arrays.asList("abstract",
			"continue", "for", "new", "switch", "assert", "default", "goto",
			"package", "synchronized", "boolean", "do", "if", "private",
			"this", "break", "double", "implements", "protected", "throw",
			"byte", "else", "import", "public", "throws", "case", "enum",
			"instanceof", "return", "transient", "catch", "extends", "int",
			"short", "try", "char", "final", "interface", "static", "void",
			"class", "finally", "long", "strictfp", "volatile", "const",
			"float", "native", "super", "while", "true", "false", "null");
	protected Map<String, String> imports;
	protected StringBuilder sb;
	protected String indent = "";

	protected void setImports(Map<String, String> imports) {
		this.imports = imports;
	}

	protected void setStringBuilder(StringBuilder sb) {
		this.sb = sb;
	}

	protected void setIndent(String indent) {
		this.indent = indent;
	}

	public String imports(Class<?> klass) {
		return imports(klass.getName());
	}

	public String imports(String klass) {
		String name = klass.trim();
		if (name.contains("<"))
			return importsGeneric(name);
		int idx = name.lastIndexOf('.');
		String sn = name.substring(idx + 1);
		if (!imports.containsKey(sn)) {
			imports.put(sn, name);
			return sn;
		}
		if (name.equals(imports.get(sn)))
			return sn;
		return name;
	}

	public JavaCommentBuilder comment(String comment) {
		begin();
		return new JavaCommentBuilder(sb, indent, comment);
	}

	public JavaSourceBuilder annotate(Class<?> ann) {
		begin();
		sb.append(indent).append("@").append(imports(ann.getName())).append(
				"\n");
		return this;
	}

	public JavaSourceBuilder annotateStrings(Class<?> ann, List<String> values) {
		begin();
		if (!values.isEmpty()) {
			sb.append(indent).append("@").append(imports(ann));
			sb.append("(");
			if (values.size() > 1) {
				sb.append("{");
			}
			for (int i = 0, n = values.size(); i < n; i++) {
				if (i > 0) {
					sb.append(", ");
				}
				appendString(sb, values.get(i));
			}
			if (values.size() > 1) {
				sb.append("}");
			}
			sb.append(")");
			sb.append("\n");
		}
		return this;
	}

	public JavaSourceBuilder annotateEntities(Class<?> ann, IEntity[] values) {
		List<URI> uris = new ArrayList<URI>();
		if (values == null)
			return this;
		for (IEntity e : values) {
			URI uri = e.getURI();
			if (uri != null) {
				uris.add(uri);
			}
		}
		if (uris.isEmpty())
			return this;
		return annotateURIs(ann, uris);
	}

	public JavaSourceBuilder annotateURI(Class<?> ann, URI value) {
		return annotateURIs(ann, singletonList(value));
	}

	public JavaSourceBuilder annotateURIs(Class<?> ann, List<URI> values) {
		begin();
		if (!values.isEmpty()) {
			sb.append(indent).append("@").append(imports(ann));
			sb.append("(");
			if (values.size() > 1) {
				sb.append("{");
			}
			for (int i = 0, n = values.size(); i < n; i++) {
				if (i > 0) {
					sb.append(", ");
				}
				appendString(sb, values.get(i));
			}
			if (values.size() > 1) {
				sb.append("}");
			}
			sb.append(")");
			sb.append("\n");
		}
		return this;
	}

	public JavaSourceBuilder annotateLiterals(Class<?> ann, Iterable<?> values,
			String datatype) {
		List<String> labels = new ArrayList<String>();
		if (values == null)
			return this;
		for (Object o : values) {
			if (o == null)
				continue;
			labels.add(o.toString());
		}
		if (labels.isEmpty())
			return this;
		return annotateLabels(ann, labels, datatype);
	}

	public JavaSourceBuilder annotateLabels(Class<?> ann, String[] labels) {
		return annotateLabels(ann, Arrays.asList(labels), null);
	}

	public JavaSourceBuilder annotateLabels(Class<?> ann, List<String> labels,
			String datatype) {
		begin();
		sb.append(indent).append("@").append(imports(ann));
		if (!labels.isEmpty()) {
			sb.append("(");
			sb.append("label=");
			if (labels.size() > 1) {
				sb.append("{");
			}
			for (int i = 0, n = labels.size(); i < n; i++) {
				if (i > 0) {
					sb.append(", ");
				}
				appendString(sb, labels.get(i));
			}
			if (labels.size() > 1) {
				sb.append("}");
			}
			if (datatype != null) {
				sb.append(", datatype=");
				appendString(sb, datatype);
			}
			sb.append(")");
		}
		sb.append("\n");
		return this;
	}

	public JavaSourceBuilder annotateClasses(Class<?> ann, List<String> values) {
		begin();
		sb.append(indent).append("@").append(imports(ann));
		if (!values.isEmpty()) {
			sb.append("(");
			if (values.size() > 1) {
				sb.append("{");
			}
			for (int i = 0, n = values.size(); i < n; i++) {
				if (i > 0) {
					sb.append(", ");
				}
				sb.append(imports(values.get(i))).append(".class");
			}
			if (values.size() > 1) {
				sb.append("}");
			}
			sb.append(")");
		}
		sb.append("\n");
		return this;
	}

	public JavaSourceBuilder annotateClass(Class<?> ann, String value) {
		begin();
		sb.append(indent).append("@").append(imports(ann));
		sb.append("(").append(imports(value)).append(".class)\n");
		return this;
	}

	protected String var(String name) {
		if (keywords.contains(name))
			return "_" + name;
		return name;
	}

	protected void begin() {
		// allow subclass to override
	}

	private void appendString(StringBuilder sb, URI value) {
		sb.append("\"");
		sb.append(value.toString());
		sb.append("\"");
	}

	private void appendString(StringBuilder sb, String value) {
		String str = value;
		str = value.replace("\\", "\\\\");
		str = str.replace("\"", "\\\"");
		str = str.replace("\n", "\\n");
		sb.append("\"");
		sb.append(str);
		sb.append("\"");
	}

	private String importsGeneric(String name) {
		int start = name.indexOf('<');
		int end = name.lastIndexOf('>');
		StringBuilder sb = new StringBuilder();
		sb.append(imports(name.substring(0, start)));
		sb.append('<');
		int idx = start + 1;
		int nested = 0;
		for (int i = start + 1; i < end; i++) {
			switch (name.charAt(i)) {
			case ',':
				if (nested == 0) {
					sb.append(imports(name.substring(idx, i))).append(", ");
					idx = i + 1;
				}
				break;
			case '<':
				nested++;
				break;
			case '>':
				nested--;
				break;
			}
		}
		sb.append(imports(name.substring(idx, end)));
		sb.append('>');
		return sb.toString();
	}
}
