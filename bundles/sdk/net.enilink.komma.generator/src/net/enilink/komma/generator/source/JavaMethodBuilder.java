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

import java.util.Map;

public class JavaMethodBuilder extends JavaSourceBuilder {
	private String methodName;
	private boolean isInterface;
	private boolean isStatic;
	private boolean hasParameters;
	private boolean isAbstract = true;
	private StringBuilder body = new StringBuilder();

	public JavaMethodBuilder(String name, boolean isInterface, boolean isStatic, Map<String, String> imports, StringBuilder sb) {
		this.methodName = name;
		this.isInterface = isInterface;
		this.isStatic = isStatic;
		setImports(imports);
		setStringBuilder(sb);
		setIndent("\t");
	}

	public JavaMethodBuilder returnType(String type) {
		body.append(imports(type)).append(" ");
		return this;
	}

	public JavaMethodBuilder returnSetOf(String type) {
		body.append(imports("java.util.Set"));
		body.append("<").append(imports(type)).append("> ");
		return this;
	}

	public JavaMethodBuilder paramSetOf(String type, String name) {
		if (hasParameters) {
			body.append(", ");
		} else {
			hasParameters = true;
			body.append(methodName);
			body.append("(");
		}
		body.append(imports("java.util.Set"));
		body.append("<").append(imports(type)).append("> ");
		body.append(name);
		return this;
	}

	public JavaMethodBuilder param(String type, String name) {
		if (hasParameters) {
			body.append(", ");
		} else {
			hasParameters = true;
			body.append(methodName);
			body.append("(");
		}
		body.append(imports(type)).append(" ").append(var(name));
		return this;
	}

	public JavaMethodBuilder code(String code) {
		if (code == null)
			return this;
		if (isAbstract) {
			if (!hasParameters) {
				body.append(methodName);
				body.append("(");
			}
			isAbstract = false;
			body.append(") {\n\t\t");
		}
		body.append(code);
		return this;
	}

	public void end() {
		sb.append("\t");
		if (!isInterface) {
			sb.append("public ");
			if (isStatic) {
				sb.append("static ");
			}
			if (isAbstract) {
				sb.append("abstract ");
			}
		}
		sb.append(body);
		if (isAbstract) {
			if (!hasParameters) {
				sb.append(methodName);
				sb.append("(");
			}
			sb.append(");\n\n");
		} else {
			sb.append("\n\t}\n\n");
		}
	}

}
