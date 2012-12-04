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
package net.enilink.composition.properties.behaviours;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import net.enilink.composition.ClassDefiner;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.asm.BehaviourClassNode;
import net.enilink.composition.asm.BehaviourClassProcessor;
import net.enilink.composition.asm.ExtendedMethod;
import net.enilink.composition.asm.Types;
import net.enilink.composition.asm.util.BehaviourMethodGenerator;
import net.enilink.composition.properties.PropertyMapper;
import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.properties.traits.Mergeable;
import net.enilink.composition.properties.traits.Refreshable;

import com.google.inject.Inject;

/**
 * Properties that have the {@link Iri} annotation are replaced with getters and
 * setters that access an underlying repository directly.
 */
public class PropertyMapperProcessor implements BehaviourClassProcessor,
		Opcodes, Types {
	private static final String FACTORY_FIELD = "_$propertySetFactory";

	private static final String PROPERTY_SUFFIX = "Property";

	@Inject
	protected ClassDefiner definer;

	protected PropertyMapper propertyMapper;

	private void addPropertySetFactoryField(BehaviourClassNode node) {
		FieldNode factoryField = new FieldNode(Opcodes.ACC_PRIVATE,
				getFactoryField(),
				Type.getDescriptor(PropertySetFactory.class), null, null);
		factoryField.visitAnnotation(Type.getDescriptor(Inject.class), true);
		node.addField(factoryField);
	}

	private String getFactoryField() {
		return FACTORY_FIELD;
	}

	private String getPropertyFieldName(String property) {
		return "_$" + property + PROPERTY_SUFFIX;
	}

	private Collection<FieldNode> getPropertySetFields(BehaviourClassNode node) {
		Collection<FieldNode> fields = new ArrayList<FieldNode>();
		for (Object field : node.fields) {
			if (((FieldNode) field).name.endsWith(PROPERTY_SUFFIX)) {
				fields.add((FieldNode) field);
			}
		}
		return fields;
	}

	private void loadPropertySet(PropertyDescriptor pd,
			BehaviourMethodGenerator gen) {
		gen.loadThis();
		gen.invokeVirtual(
				gen.getMethod().getOwner().getType(),
				new org.objectweb.asm.commons.Method("_$get" + pd.getName(),
						Type.getMethodDescriptor(Type
								.getType(PropertySet.class))));
	}

	private void createPropertySetAccessor(PropertyDescriptor pd,
			BehaviourClassNode node) throws Exception {
		String fieldName = getPropertyFieldName(pd.getName());
		FieldNode propertyField = new FieldNode(Opcodes.ACC_PRIVATE, fieldName,
				Type.getDescriptor(PropertySet.class), null, null);
		node.addField(propertyField);

		ExtendedMethod propertyAccessor = new ExtendedMethod(node,
				Opcodes.ACC_PRIVATE, "_$get" + pd.getName(),
				Type.getMethodDescriptor(Type.getType(PropertySet.class)),
				null, null);
		node.methods.add(propertyAccessor);
		BehaviourMethodGenerator gen = new BehaviourMethodGenerator(
				propertyAccessor);
		lazyInitializePropertySet(pd, node, propertyField, gen);
		gen.returnValue();
		gen.endMethod();
	}

	private void lazyInitializePropertySet(PropertyDescriptor pd,
			BehaviourClassNode node, FieldNode field,
			BehaviourMethodGenerator gen) throws Exception {
		gen.loadThis();
		gen.getField(field.name, Type.getType(field.desc));
		gen.dup();

		Label exists = gen.newLabel();
		gen.ifNonNull(exists);
		gen.pop();

		Method getter = pd.getReadMethod();
		Method setter = pd.getWriteMethod();

		loadFactory(node, gen);
		gen.loadBean();
		gen.push(propertyMapper.findPredicate(pd));

		// load element type
		Class<?> propertyType = pd.getPropertyType();
		if (Set.class.equals(propertyType)) {
			java.lang.reflect.Type t = pd.getReadMethod()
					.getGenericReturnType();
			if (t instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) t;
				java.lang.reflect.Type[] args = pt.getActualTypeArguments();
				if (args.length == 1 && args[0] instanceof Class<?>) {
					propertyType = (Class<?>) args[0];
				}
			}
		}
		gen.push(Type.getType(propertyType));
		gen.push(setter == null);

		// instantiate PropertyDescriptor
		Type propertyDescType = Type.getType(PropertyDescriptor.class);
		gen.newInstance(propertyDescType);
		gen.dup();

		// call constructor
		gen.push(pd.getName());
		gen.push(Type.getType(getter.getDeclaringClass()));
		gen.push(getter.getName());
		gen.push(setter == null ? null : setter.getName());
		gen.invokeConstructor(propertyDescType,
				new org.objectweb.asm.commons.Method("<init>", Type.VOID_TYPE,
						new Type[] { STRING_TYPE, CLASS_TYPE, STRING_TYPE,
								STRING_TYPE }));

		// retrieve read method annotations
		gen.invokeVirtual(propertyDescType,
				new org.objectweb.asm.commons.Method("getReadMethod",
						METHOD_TYPE, new Type[0]));
		gen.invokeVirtual(
				METHOD_TYPE,
				new org.objectweb.asm.commons.Method("getAnnotations", Type
						.getType(Annotation[].class), new Type[0]));

		gen.invoke(Methods.PROPERTYSETFACTORY_CREATEPROPERTYSET);
		gen.dup();
		// gen.injectMembers();

		// store property set in field
		gen.loadThis();
		gen.swap();
		gen.putField(field.name, Type.getType(field.desc));

		gen.mark(exists);
	}

	private void implementGetter(PropertyDescriptor pd, BehaviourClassNode node)
			throws Exception {
		Class<?> classType = pd.getReadMethod().getReturnType();
		Type type = Type.getType(classType);

		ExtendedMethod mn = node.addExtendedMethod(pd.getReadMethod(), definer);
		BehaviourMethodGenerator gen = new BehaviourMethodGenerator(mn);

		loadPropertySet(pd, gen);

		if (isCollection(classType)) {
			gen.invoke(Methods.PROPERTYSET_GET_ALL);
			gen.returnValue();
		} else if (classType.isPrimitive()) {
			gen.invoke(Methods.PROPERTYSET_GET_SINGLE);
			gen.dup();
			Label isNull = gen.newLabel();
			gen.ifNull(isNull);
			gen.unbox(type);
			gen.returnValue();

			gen.mark(isNull);
			gen.pop();
			gen.push(0);
			gen.cast(Type.INT_TYPE, type);
			gen.returnValue();
		} else {
			Label tryLabel = gen.mark();
			// try
			gen.invoke(Methods.PROPERTYSET_GET_SINGLE);
			gen.checkCast(type);
			gen.returnValue();

			// catch (ClassCastException)
			Label catchLabel = gen.mark();
			Type exceptionType = Type.getType(ClassCastException.class);
			gen.catchException(tryLabel, catchLabel, exceptionType);
			gen.newInstance(exceptionType);
			gen.dup();

			gen.newStringBuilder();

			// reload property value
			loadPropertySet(pd, gen);
			gen.invoke(Methods.PROPERTYSET_GET_SINGLE);
			gen.invokeToString();
			gen.appendToStringBuilder();

			gen.push("cannot be cast to " + classType.getName());
			gen.appendToStringBuilder();
			gen.invokeToString();

			gen.invokeConstructor(exceptionType,
					org.objectweb.asm.commons.Method
							.getMethod("void <init>(String)"));
			gen.throwException();
		}

		gen.endMethod();
	}

	private void implementProperty(PropertyDescriptor pd,
			BehaviourClassNode node) throws Exception {
		createPropertySetAccessor(pd, node);

		implementGetter(pd, node);

		Method setter = pd.getWriteMethod();
		if (setter != null) {
			implementSetter(pd, node);
		}
	}

	@Override
	public boolean implementsClass(Class<?> concept) {
		return !propertyMapper.findProperties(concept).isEmpty();
	}

	private void implementSetter(PropertyDescriptor pd, BehaviourClassNode node)
			throws Exception {
		Class<?> classType = pd.getWriteMethod().getParameterTypes()[0];
		Type type = Type.getType(classType);

		ExtendedMethod mn = node
				.addExtendedMethod(pd.getWriteMethod(), definer);
		BehaviourMethodGenerator gen = new BehaviourMethodGenerator(mn);

		loadPropertySet(pd, gen);

		gen.loadArgs();
		if (isCollection(classType)) {
			gen.invoke(Methods.PROPERTYSET_SET_ALL);
		} else {
			gen.box(type);
			gen.invoke(Methods.PROPERTYSET_SET_SINGLE);
		}
		gen.returnValue();
		gen.endMethod();
	}

	private boolean isCollection(Class<?> type) throws Exception {
		return Set.class.equals(type);
	}

	private void loadFactory(BehaviourClassNode node,
			BehaviourMethodGenerator gen) {
		gen.loadThis();
		gen.getField(getFactoryField(), Type.getType(PropertySetFactory.class));
	}

	private void mergeProperty(PropertyDescriptor pd,
			BehaviourMethodGenerator gen) throws Exception {
		Class<?> type = pd.getPropertyType();
		if (type.isPrimitive()) {
			loadPropertySet(pd, gen);
			persistValue(type, gen);
		} else {
			gen.dup();

			Label isNull = gen.newLabel();
			gen.ifNull(isNull);

			loadPropertySet(pd, gen);
			persistValue(type, gen);

			Label end = gen.newLabel();
			gen.goTo(end);

			gen.mark(isNull);
			gen.pop();
			gen.mark(end);
		}
	}

	private void overrideMergeMethod(BehaviourClassNode node) throws Exception {
		Method merge = Mergeable.class.getMethod("merge", Object.class);
		BehaviourMethodGenerator gen = new BehaviourMethodGenerator(
				node.addExtendedMethod(merge, definer));

		try {
			// check if there is an already implemented merge method
			Method implementedMerge = node.getParentClass().getMethod("merge",
					Object.class);
			if ((implementedMerge.getModifiers() & Modifier.ABSTRACT) == 0) {
				// invoke super.merge()
				gen.loadThis();
				gen.loadArgs();
				gen.invokeSpecial(node.getParentType(),
						org.objectweb.asm.commons.Method
								.getMethod(implementedMerge));
			}
		} catch (NoSuchMethodException e) {
			// continue
		}

		gen.loadArg(0);
		gen.instanceOf(node.getParentType());
		Label notInstanceOf = gen.newLabel();
		gen.ifZCmp(IFEQ, notInstanceOf);

		for (PropertyDescriptor pd : propertyMapper.findProperties(node
				.getParentClass())) {
			gen.loadArg(0);
			gen.checkCast(Type.getType(pd.getReadMethod().getDeclaringClass()));
			gen.invoke(pd.getReadMethod());

			mergeProperty(pd, gen);
		}

		gen.mark(notInstanceOf);
		gen.returnValue();
		gen.endMethod();
	}

	private void overrideRefreshMethod(BehaviourClassNode node)
			throws Exception {
		Method refresh = Refreshable.class.getMethod("refresh");
		BehaviourMethodGenerator gen = new BehaviourMethodGenerator(
				node.addExtendedMethod(refresh, definer));

		try {
			// check if there is an already implemented refresh method
			Method implementedRefresh = node.getParentClass().getMethod(
					"refresh");
			if ((implementedRefresh.getModifiers() & Modifier.ABSTRACT) == 0) {
				// invoke super.refresh()
				gen.loadThis();
				gen.invokeSpecial(node.getParentType(),
						org.objectweb.asm.commons.Method
								.getMethod(implementedRefresh));
			}
		} catch (NoSuchMethodException e) {
			// continue
		}

		for (FieldNode field : getPropertySetFields(node)) {
			gen.loadThis();
			gen.getField(field.name, Type.getType(field.desc));
			gen.dup();

			Label isNull = gen.newLabel();
			gen.ifNull(isNull);
			gen.invoke(refresh);
			Label end = gen.newLabel();
			gen.goTo(end);
			gen.mark(isNull);
			gen.pop();
			gen.mark(end);
		}

		gen.returnValue();
		gen.endMethod();
	}

	private void persistValue(Class<?> type, BehaviourMethodGenerator gen)
			throws Exception {
		gen.swap();
		if (isCollection(type)) {
			gen.invoke(Methods.PROPERTYSET_ADD_ALL);
		} else {
			gen.box(Type.getType(type));
			gen.invoke(Methods.PROPERTYSET_ADD_SINGLE);
		}
		gen.pop();
	}

	@Override
	public void process(BehaviourClassNode classNode) throws Exception {
		classNode.addInterface(Type.getInternalName(Mergeable.class));
		classNode.addInterface(Type.getInternalName(Refreshable.class));

		classNode.addInjectorField();
		addPropertySetFactoryField(classNode);

		for (PropertyDescriptor pd : propertyMapper.findProperties(classNode
				.getParentClass())) {
			implementProperty(pd, classNode);
		}

		overrideMergeMethod(classNode);
		overrideRefreshMethod(classNode);
	}

	@Inject
	public void setPropertyMapper(PropertyMapper mapper) {
		this.propertyMapper = mapper;
	}
}
