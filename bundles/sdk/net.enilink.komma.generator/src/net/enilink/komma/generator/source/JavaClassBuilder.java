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

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

public class JavaClassBuilder extends JavaSourceBuilder {
	private PrintWriter out;
	private String pkg;
	private String name;
	private boolean isInterface;
	private boolean extendsPrinted;
	private boolean implementsPrinted;
	private boolean headerPrinted;
	private boolean headerStarted;
	private boolean ended;
	private boolean closeHeader;
	private Set<String> extended = new HashSet<String>();

	public JavaClassBuilder(PrintWriter out) {
		HashMap<String, String> imports = new HashMap<String, String>();
		imports.put("char", null);
		imports.put("byte", null);
		imports.put("short", null);
		imports.put("int", null);
		imports.put("long", null);
		imports.put("float", null);
		imports.put("double", null);
		imports.put("boolean", null);
		imports.put("void", null);
		setImports(imports);
		setStringBuilder(new StringBuilder());
		this.out = out;
	}

	public void close() {
		end();
		out.close();
	}

	public JavaClassBuilder pkg(String pkg) {
		if (sb.length() > 0) {
			out.append(sb);
			sb.setLength(0);
		}
		this.pkg = pkg;
		if (pkg != null) {
			out.print("package ");
			out.print(pkg);
			out.println(";");
			out.println();
		}
		return this;
	}

	public JavaClassBuilder className(String name) {
		this.name = name;
		headerStarted = true;
		imports.put(name, null);
		sb.append("public class ");
		sb.append(name);
		return this;
	}

	public JavaClassBuilder abstractName(String name) {
		this.name = name;
		headerStarted = true;
		imports.put(name, null);
		sb.append("public abstract class ");
		sb.append(name);
		return this;
	}

	public JavaClassBuilder interfaceName(String name) {
		this.name = name;
		headerStarted = true;
		isInterface = true;
		imports.put(name, null);
		sb.append("public interface ");
		sb.append(name);
		return this;
	}

	public JavaClassBuilder extend(String name) {
		if (extended.contains(name))
			return this;
		extended.add(name);
		closeHeader = true;
		if (extendsPrinted) {
			sb.append(", ");
		} else {
			sb.append(" extends ");
			extendsPrinted = true;
		}
		sb.append(imports(name));
		return this;
	}

	public JavaClassBuilder implement(String name) {
		if (isInterface)
			return extend(name);
		if (extended.contains(name))
			return this;
		extended.add(name);
		closeHeader = true;
		if (implementsPrinted) {
			sb.append(", ");
		} else {
			sb.append(" implements ");
			implementsPrinted = true;
		}
		sb.append(imports(name));
		return this;
	}

	@Override
	protected void begin() {
		if (closeHeader) {
			closeHeader();
		}
		super.begin();
	}

	public JavaMethodBuilder staticMethod(String name) {
		closeHeader();
		JavaMethodBuilder method;
		method = new JavaMethodBuilder(name, isInterface, true, imports, sb);
		return method;
	}

	public JavaClassBuilder staticURIField(String name, URI value) {
		closeHeader();
		sb.append("\tpublic static final ").append(imports(URI.class));
		sb.append(" ").append(name).append(" = ")
				.append(imports(URIs.class)).append(".createURI(");
		sb.append("\"").append(value.toString()).append("\");\n");
		return this;
	}

	public JavaClassBuilder staticStringField(String name, String value) {
		closeHeader();
		sb.append("\tpublic static final ").append(imports(String.class));
		sb.append(" ").append(name).append(" = \"").append(value).append(
				"\";\n");
		return this;
	}

	public JavaClassBuilder staticURIArrayField(String name, List<String> names) {
		closeHeader();
		sb.append("\tpublic static final ").append(imports(URI.class));
		sb.append("[] ").append(name).append(" = new ").append(
				imports(URI.class));
		sb.append("[]{");
		Iterator<String> iter = names.iterator();
		while (iter.hasNext()) {
			sb.append(iter.next());
			if (iter.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append("};\n");
		return this;
	}

	public JavaClassBuilder field(String type, String name) {
		closeHeader();
		sb.append("\tprivate ").append(imports(type));
		sb.append(" ").append(var(name)).append(";\n");
		return this;
	}

	public JavaMethodBuilder constructor() {
		closeHeader();
		sb.append("\n");
		JavaMethodBuilder method;
		method = new JavaMethodBuilder(name, isInterface, false, imports, sb);
		return method;
	}

	public JavaPropertyBuilder property(String name) {
		closeHeader();
		JavaPropertyBuilder property;
		property = new JavaPropertyBuilder(name, isInterface, imports, sb);
		return property;
	}

	public JavaMethodBuilder method(String name) {
		closeHeader();
		JavaMethodBuilder method;
		method = new JavaMethodBuilder(name, isInterface, false, imports, sb);
		return method;
	}

	public JavaClassBuilder code(String code) {
		closeHeader();
		sb.append(code);
		return this;
	}

	public void end() {
		if (ended)
			return;
		ended = true;
		boolean importsPrinted = false;
		for (String cn : imports.values()) {
			if (cn == null)
				continue; // primitive
			int packageEnd = cn.lastIndexOf('.');
			if (packageEnd <= 0) {
				continue;
			}
			String pack = cn.substring(0, packageEnd);
			if (pack.equals("java.lang"))
				continue;
			if (pack.equals(pkg))
				continue;
			out.print("import ");
			out.print(cn);
			out.println(";");
			importsPrinted = true;
		}
		if (importsPrinted) {
			out.println();
		}
		if (headerStarted) {
			closeHeader();
			out.append(sb);
			out.println("}");
		}
		out.flush();
	}

	private void closeHeader() {
		if (!headerPrinted) {
			headerPrinted = true;
			sb.append(" {\n");
		}
	}
}
