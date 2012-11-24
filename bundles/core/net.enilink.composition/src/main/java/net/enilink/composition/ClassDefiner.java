/*
 * Copyright (c) 2009, 2010, James Leigh All rights reserved.
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
package net.enilink.composition;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.enilink.composition.asm.DefaultBehaviourFactory;

/**
 * Factory class for creating Classes.
 * 
 * @author James Leigh
 * @author Ken Wenzel
 */
public class ClassDefiner extends ClassLoader {
	private static final URL exists;

	static {
		try {
			exists = new URL("http://java/"
					+ ClassDefiner.class.getName().replace('.', '/')
					+ "#exists");
		} catch (MalformedURLException e) {
			throw new AssertionError(e);
		}
	}
	private ConcurrentMap<String, byte[]> bytecodes = new ConcurrentHashMap<String, byte[]>();

	private File output;

	/**
	 * Creates a new Class Factory using the current context class loader.
	 */
	public ClassDefiner() {
		this(Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Create a given Class Factory with the given class loader.
	 * 
	 * @param parent
	 */
	public ClassDefiner(ClassLoader parent) {
		super(parent);
	}

	/**
	 * Creates a new Class Factory using the current context class loader.
	 */
	public ClassDefiner(File dir) {
		this(dir, Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Create a given Class Factory with the given class loader.
	 * 
	 * @param parent
	 */
	public ClassDefiner(File dir, ClassLoader parent) {
		super(parent);
		this.output = dir;
		dir.mkdirs();
	}

	@Override
	public URL getResource(String name) {
		if (output != null) {
			try {
				File file = new File(output, name);
				if (file.exists()) {
					return file.toURI().toURL();
				}
			} catch (MalformedURLException e) {
				throw new AssertionError(e);
			}
		}
		if (bytecodes != null && bytecodes.containsKey(name)) {
			return exists;
		}
		return super.getResource(name);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		if (output != null) {
			File file = new File(output, name);
			if (file.exists()) {
				try {
					return new FileInputStream(file);
				} catch (FileNotFoundException e) {
				}
			}
		}
		if (bytecodes != null && bytecodes.containsKey(name)) {
			byte[] b = bytecodes.get(name);
			return new ByteArrayInputStream(b);
		}
		return getParent().getResourceAsStream(name);
	}

	public Class<?> defineClass(String name, byte[] bytecode) {
		String resource = name.replace('.', '/') + ".class";
		if (output != null) {
			saveResource(resource, bytecode);
		}
		if (bytecodes != null) {
			bytecodes.putIfAbsent(resource, bytecode);
		}
		return defineClass(name, bytecode, 0, bytecode.length);
	}

	private void saveResource(String fileName, byte[] bytecode) {
		try {
			File file = new File(output, fileName);
			file.getParentFile().mkdirs();
			FileOutputStream out = new FileOutputStream(file);
			try {
				out.write(bytecode);
			} finally {
				out.close();
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		if (name.startsWith(ClassResolver.PKG_PREFIX)
				|| name.startsWith(DefaultBehaviourFactory.PKG_PREFIX)) {
			// prevent delegation to parent class loader,
			// since this class should be defined in this class loader
			synchronized (getClassLoadingLock(name)) {
				Class<?> c = findLoadedClass(name);
				if (c == null) {
					throw new ClassNotFoundException(name);
				}
				if (resolve) {
					resolveClass(c);
				}
				return c;
			}
		}
		return super.loadClass(name, resolve);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		// required to load classes from this bundle or from other (dynamically)
		// imported bundles
		return getClass().getClassLoader().loadClass(name);
	}
}
