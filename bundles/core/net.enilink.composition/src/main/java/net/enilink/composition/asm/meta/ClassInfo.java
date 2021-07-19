/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.composition.asm.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Reflective meta data for classes that also contains the visible and invisible
 * annotations of their methods.
 */
public class ClassInfo extends ClassVisitor {

	/**
	 * The class version.
	 */
	public int version;

	/**
	 * The class's access flags (see {@link org.objectweb.asm.Opcodes}). This
	 * field also indicates if the class is deprecated.
	 */
	public int access;

	/**
	 * The internal name of the class (see
	 * {@link org.objectweb.asm.Type#getInternalName() getInternalName}).
	 */
	public String name;

	/**
	 * The signature of the class. Mayt be <tt>null</tt>.
	 */
	public String signature;

	/**
	 * The internal of name of the super class (see
	 * {@link org.objectweb.asm.Type#getInternalName() getInternalName}). For
	 * interfaces, the super class is {@link Object}. May be <tt>null</tt>, but
	 * only for the {@link Object} class.
	 */
	public String superName;

	/**
	 * The internal names of the class's interfaces (see
	 * {@link org.objectweb.asm.Type#getInternalName() getInternalName}). This
	 * list is a list of {@link String} objects.
	 */
	public List<String> interfaces;

	/**
	 * The methods of this class.
	 */
	public Map<Method, MethodNode> methods;

	/**
	 * The runtime visible annotations of this class. This list is a list of
	 * {@link AnnotationNode} objects. May be <tt>null</tt>.
	 * 
	 * @associates org.objectweb.asm.tree.AnnotationNode
	 * @label visible
	 */
	public List<AnnotationNode> visibleAnnotations;

	/**
	 * The runtime invisible annotations of this class. This list is a list of
	 * {@link AnnotationNode} objects. May be <tt>null</tt>.
	 * 
	 * @associates org.objectweb.asm.tree.AnnotationNode
	 * @label invisible
	 */
	public List<AnnotationNode> invisibleAnnotations;

	/**
	 * Constructs a new {@link ClassNode}.
	 */
	public ClassInfo() {
		super(Opcodes.ASM7);

		this.interfaces = new ArrayList<String>();
		this.methods = new HashMap<Method, MethodNode>();
	}

	// ------------------------------------------------------------------------
	// Implementation of the ClassVisitor interface
	// ------------------------------------------------------------------------

	public void visit(final int version, final int access, final String name,
			final String signature, final String superName,
			final String[] interfaces) {
		this.version = version;
		this.access = access;
		this.name = name;
		this.signature = signature;
		this.superName = superName;
		if (interfaces != null) {
			this.interfaces.addAll(Arrays.asList(interfaces));
		}
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc,
			final boolean visible) {
		AnnotationNode an = new AnnotationNode(desc);
		if (visible) {
			if (visibleAnnotations == null) {
				visibleAnnotations = new ArrayList<AnnotationNode>(1);
			}
			visibleAnnotations.add(an);
		} else {
			if (invisibleAnnotations == null) {
				invisibleAnnotations = new ArrayList<AnnotationNode>(1);
			}
			invisibleAnnotations.add(an);
		}
		return an;
	}

	public MethodVisitor visitMethod(final int access, final String name,
			final String desc, final String signature, final String[] exceptions) {
		MethodNode mn = new MethodNode(Opcodes.ASM7, access, name, desc,
				signature, exceptions) {
			public void visitEnd() {
				instructions.clear();
			}
		};
		methods.put(new Method(name, desc), mn);
		return mn;
	}

	public MethodNode getMethod(Method method) {
		return methods.get(method);
	}

	@Override
	public void visitSource(String source, String debug) {
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
	}

	@Override
	public void visitInnerClass(String name, String outerName,
			String innerName, int access) {
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc,
			String signature, Object value) {
		return null;
	}

	/**
	 * Copies annotations of this class.
	 * 
	 * @param cv
	 *            a class visitor.
	 */
	public void copyAnnotations(final ClassVisitor cv) {
		// visits annotations
		int i, n;
		n = visibleAnnotations == null ? 0 : visibleAnnotations.size();
		for (i = 0; i < n; ++i) {
			AnnotationNode an = (AnnotationNode) visibleAnnotations.get(i);
			an.accept(cv.visitAnnotation(an.desc, true));
		}
		n = invisibleAnnotations == null ? 0 : invisibleAnnotations.size();
		for (i = 0; i < n; ++i) {
			AnnotationNode an = (AnnotationNode) invisibleAnnotations.get(i);
			an.accept(cv.visitAnnotation(an.desc, false));
		}
	}
}