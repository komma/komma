/*
 * Copyright (c) 2007, 2010, James Leigh All rights reserved.
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
package net.enilink.komma.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.enilink.komma.generator.java.merge.SourceMerger;

/**
 * Saves the generated source code from CodeGenerator to disk.
 * 
 */
public class FileSourceCodeHandler implements SourceCodeHandler {
	private static final Pattern PACKAGE = Pattern.compile("package ([^;]*);");

	private static final Pattern INTERFACE = Pattern
			.compile("public (interface|class|abstract class) (\\S*) ");

	private static final Pattern CONCRETE = Pattern.compile(
			".*public class .*", Pattern.DOTALL);

	private static final Pattern ABSTRACT = Pattern.compile(
			".*public abstract class .*", Pattern.DOTALL);

	private static final Pattern ANNOTATED = Pattern.compile(
			".*" + PACKAGE.pattern() + ".*@.*" + INTERFACE.pattern() + ".*",
			Pattern.DOTALL);

	private final Logger log = LoggerFactory.getLogger(OntologyConverter.class);

	private File target;

	private List<String> content = new ArrayList<String>();

	private List<String> annotatedClasses = new ArrayList<String>();

	private List<String> concreteClasses = new ArrayList<String>();

	private List<String> abstractClasses = new ArrayList<String>();

	private SourceMerger merger;

	public FileSourceCodeHandler() throws IOException {
		String prefix = FileSourceCodeHandler.class.getSimpleName();
		target = File.createTempFile(prefix, "");
		target.delete();
		target.mkdir();
	}

	public FileSourceCodeHandler(File target) {
		this.target = target;
	}

	public File getTarget() {
		return target;
	}

	public List<String> getConcreteClasses() {
		return concreteClasses;
	}

	public List<String> getAbstractClasses() {
		return abstractClasses;
	}

	public List<String> getAnnotatedClasses() {
		return annotatedClasses;
	}

	public List<String> getClasses() {
		return content;
	}

	public synchronized void handleSource(String code) throws IOException {
		String pkg = getPackageName(code);
		String name = getSimpleClassName(code);
		if (name == null)
			name = "package-info";
		String className = pkg + '.' + name;
		log.debug("Saving " + className);
		saveClass(pkg, name, code);
		content.add(className);
		if (ANNOTATED.matcher(code).matches()) {
			annotatedClasses.add(className);
		}
		if (ABSTRACT.matcher(code).matches()) {
			abstractClasses.add(className);
		}
		if (CONCRETE.matcher(code).matches()) {
			concreteClasses.add(className);
		}
	}

	private String getPackageName(String code) {
		Matcher m = PACKAGE.matcher(code);
		m.find();
		String pkg = m.group(1);
		return pkg;
	}

	private String getSimpleClassName(String code) {
		Matcher m;
		m = INTERFACE.matcher(code);
		if (m.find())
			return m.group(2);
		return null;
	}

	private File saveClass(String pkg, String name, String code)
			throws IOException {
		File folder = new File(target, pkg.replace('.', '/'));
		folder.mkdirs();
		File file = new File(folder, name + ".java");
		if (file.exists() && Platform.isRunning()) {
			// merge existing code with newly generated code
			if (merger == null) {
				merger = new SourceMerger();
			}
			code = merger.merge(file, code);
		}
		if (code != null) {
			Writer writer = new FileWriter(file);
			try {
				writer.write(code);
			} finally {
				writer.close();
			}
		}
		return file;
	}
}
