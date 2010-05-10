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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaCompiler {

	final Logger log = LoggerFactory.getLogger(JavaCompiler.class);

	private String version = "5";

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void compile(Iterable<String> content, File dir, List<File> classpath)
			throws Exception {
		List<File> source = new ArrayList<File>();
		for (String name : content) {
			String filename = name.replace('.', File.separatorChar);
			source.add(new File(dir, filename + ".java"));
		}
		if (javac(buildJavacArgs(source, classpath)) != 0)
			throw new AssertionError("Could not compile");
	}

	private int javac(String[] args) throws Exception {
		try {
			return javaCompilerTool(args);
		} catch (Exception e) {
		}
		try {
			return javaSunTools(args);
		} catch (Exception e) {
		}
		return javacCommand(args);
	}

	/**
	 * Requires JDK6.
	 */
	private int javaCompilerTool(String[] args) throws Exception {
		Class<?> provider = Class.forName("javax.tools.ToolProvider");
		Method getJavaCompiler = provider.getMethod("getSystemJavaCompiler");
		Class<?> tool = Class.forName("javax.tools.Tool");
		Method run = tool.getMethod("run", InputStream.class,
				OutputStream.class, OutputStream.class, args.getClass());
		Object compiler = getJavaCompiler.invoke(null);
		log.debug("invoke javax.tools.JavaCompiler#run");
		try {
			Object[] param = new Object[] { null, null, null, args };
			Object result = run.invoke(compiler, param);
			return ((Number) result).intValue();
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof Exception)
				throw (Exception) e.getCause();
			throw e;
		}
	}

	/**
	 * Requires Sun tools.jar in class-path.
	 */
	private int javaSunTools(String[] args) throws Exception {
		Class<?> sun = Class.forName("com.sun.tools.javac.Main");
		Method method = sun.getMethod("compile", args.getClass());
		log.debug("invoke com.sun.tools.javac.Main#compile");
		try {
			Object result = method.invoke(null, new Object[] { args });
			return ((Number) result).intValue();
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof Exception)
				throw (Exception) e.getCause();
			throw e;
		}
	}

	/**
	 * Requires JDK installation.
	 */
	private int javacCommand(String[] args) throws Exception {
		return exec(findJavac(), args);
	}

	private String findJavac() {
		String javac = findJavac(System.getProperty("jdk.home"));
		if (javac == null)
			javac = findJavac(System.getProperty("java.home"));
		if (javac == null)
			javac = findJavac(System.getenv("JAVA_HOME"));
		if (javac == null) {
			String systemPath = System.getenv("PATH");
			for (String path : systemPath.split(File.pathSeparator)) {
				File file = new File(path, "javac");
				if (file.exists())
					return file.getPath();
			}
		}
		if (javac == null)
			throw new AssertionError("No compiler found");
		return javac;
	}

	private String findJavac(String home) {
		if (home == null)
			return null;
		File javac = new File(new File(home, "bin"), "javac");
		if (javac.exists())
			return javac.getPath();
		javac = new File(new File(home, "bin"), "javac.exe");
		if (javac.exists())
			return javac.getPath();
		File parent = new File(home).getParentFile();
		javac = new File(new File(parent, "bin"), "javac");
		if (javac.exists())
			return javac.getPath();
		javac = new File(new File(parent, "bin"), "javac.exe");
		if (javac.exists())
			return javac.getPath();
		return null;
	}

	private int exec(String cmd, String[] args) throws IOException,
			InterruptedException {
		log.debug("exec " + cmd);
		String[] cmdArray = new String[1 + args.length];
		cmdArray[0] = cmd;
		System.arraycopy(args, 0, cmdArray, 1, args.length);
		final Process exec = Runtime.getRuntime().exec(cmdArray);
		Thread gobbler = new Thread() {
			@Override
			public void run() {
				try {
					InputStream in = exec.getInputStream();
					InputStreamReader isr = new InputStreamReader(in);
					BufferedReader br = new BufferedReader(isr);
					String line = null;
					while ((line = br.readLine()) != null)
						System.out.println(line);
				} catch (IOException ioe) {
					log.error(ioe.getMessage(), ioe);
				}
			}
		};
		gobbler.start();
		InputStream stderr = exec.getErrorStream();
		InputStreamReader isr = new InputStreamReader(stderr);
		BufferedReader br = new BufferedReader(isr);
		String line = null;
		while ((line = br.readLine()) != null)
			System.err.println(line);
		return exec.waitFor();
	}

	private String[] buildJavacArgs(List<File> sources, List<File> classpath) {
		String[] args = new String[6 + sources.size()];
		args[0] = "-source";
		args[1] = version;
		args[2] = "-target";
		args[3] = version;
		args[4] = "-classpath";
		StringBuilder sb = new StringBuilder();
		for (File jar : classpath) {
			sb.append(jar.getAbsolutePath());
			sb.append(File.pathSeparatorChar);
		}
		args[5] = sb.toString();
		for (int i = 0, n = sources.size(); i < n; i++) {
			args[6 + i] = sources.get(i).getAbsolutePath();
		}
		return args;
	}
}
