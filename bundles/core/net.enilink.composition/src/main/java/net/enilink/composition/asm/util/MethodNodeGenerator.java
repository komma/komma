/*
 * Copyright (c) 2009, 2010 Ken Wenzel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.enilink.composition.asm.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import net.enilink.composition.asm.Types;

/**
 * Generator for method nodes.
 * 
 * @author Ken Wenzel
 * 
 */
public class MethodNodeGenerator extends InsnListGenerator {
	public MethodNode mn;

	public MethodNodeGenerator(int access, String name, String desc) {
		this(access, name, desc, null, null);
	}

	public MethodNodeGenerator(int access, String name, String desc,
			String signature, String[] exceptions) {
		this(new MethodNode(access, name, desc, signature, exceptions));
	}

	public MethodNodeGenerator(MethodNode mn) {
		super(mn);
		this.mn = mn;
	}

	/**
	 * Makes the given class visitor visit the generated method.
	 * 
	 * @param cv
	 *            a class visitor.
	 */
	public void accept(final ClassVisitor cv) {
		mn.accept(cv);
	}

	/**
	 * Makes the given method visitor visit the generated method.
	 * 
	 * @param mv
	 *            a method visitor.
	 */
	public void accept(final MethodVisitor mv) {
		mn.accept(mv);
	}

	public void appendToStringBuilder() {
		Type type = Type.getType(StringBuilder.class);
		invokeVirtual(type, new org.objectweb.asm.commons.Method("append",
				type, new Type[] { Types.STRING_TYPE }));
	}

	/**
	 * Calls {@link Object#toString()}.
	 * 
	 */
	public void invokeToString() {
		invokeVirtual(Types.OBJECT_TYPE, new org.objectweb.asm.commons.Method(
				"toString", Types.STRING_TYPE, new Type[0]));
	}

	@SuppressWarnings("unchecked")
	public InsnListGenerator createSubGenerator() {
		return new InsnListGenerator(new MethodNode(mn.access, mn.name,
				mn.desc, mn.signature, (String[]) mn.exceptions
						.toArray(new String[mn.exceptions.size()])));
	}

	public void invoke(Constructor<?> constructor) {
		Class<?> declaringClass = constructor.getDeclaringClass();
		Type declaringType = Type.getType(declaringClass);
		org.objectweb.asm.commons.Method methodDesc = org.objectweb.asm.commons.Method
				.getMethod(constructor);

		invokeConstructor(declaringType, methodDesc);
	}

	public void invoke(Method method) {
		Class<?> declaringClass = method.getDeclaringClass();
		Type declaringType = Type.getType(declaringClass);
		org.objectweb.asm.commons.Method methodDesc = org.objectweb.asm.commons.Method
				.getMethod(method);

		if (declaringClass.isInterface()) {
			invokeInterface(declaringType, methodDesc);
		} else if (Modifier.isStatic(method.getModifiers())) {
			invokeStatic(declaringType, methodDesc);
		} else {
			invokeVirtual(declaringType, methodDesc);
		}
	}

	public void newStringBuilder() {
		Type type = Type.getType(StringBuilder.class);
		newInstance(type);
		dup();
		invokeConstructor(type, new org.objectweb.asm.commons.Method("<init>",
				Type.VOID_TYPE, new Type[0]));
	}
}
