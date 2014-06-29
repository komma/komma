/*
 * Copyright (c) 2009-2010 Ken Wenzel
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

import java.util.Stack;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

/**
 * Extension of {@link GeneratorAdapter} to allow the generation of
 * {@link InsnList}s.
 * 
 * @author Ken Wenzel
 * 
 */
public class InsnListGenerator extends
		org.objectweb.asm.commons.GeneratorAdapter {
	public InsnList instructions;

	protected MethodNode mn;
	protected Stack<InsnList> stack;

	public InsnListGenerator(MethodNode mn) {
		super(Opcodes.ASM5, mn, mn.access, mn.name, mn.desc);
		this.mn = mn;
		this.instructions = mn.instructions;
	}

	public InsnListGenerator(int access, String name, String desc,
			String signature, String[] exceptions) {
		this(new MethodNode(access, name, desc, signature, exceptions));
	}

	public InsnListGenerator(int access, String name, String desc) {
		this(access, name, desc, null, null);
	}

	public InsnList pushInsns() {
		if (stack == null) {
			stack = new Stack<InsnList>();
		}
		stack.push(instructions);
		instructions = new InsnList();

		return instructions;
	}

	public InsnList popInsns() {
		if (stack == null || stack.isEmpty()) {
			throw new IllegalStateException(
					"Cannot restore instructions from empty stack");
		}
		instructions = stack.pop();

		return instructions;
	}

	public InsnList peekInsns() {
		if (stack == null || stack.isEmpty()) {
			throw new IllegalStateException(
					"Cannot restore instructions from empty stack");
		}

		return stack.peek();
	}

	/**
	 * Generates the instruction to invoke a specific method.
	 * 
	 * @param type
	 *            the class in which the method is defined.
	 * @param method
	 *            the method to be invoked.
	 */
	public void invokeSpecial(final Type type, final Method method) {
		super.invokeConstructor(type, method);
	}

	public void loadArray(Type[] types) {
		Type classType = Type.getType(Class.class);
		push(types.length);
		newArray(classType);
		for (int i = 0; i < types.length; i++) {
			dup();
			push(i);
			push(types[i]);
			arrayStore(classType);
		}
	}
}