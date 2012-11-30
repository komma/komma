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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import net.enilink.composition.asm.util.MethodNodeGenerator;
import net.enilink.composition.properties.PropertyMapper;
import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.PropertySetDescriptor;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.properties.traits.Mergeable;
import net.enilink.composition.properties.traits.Refreshable;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Properties that have the {@link Iri} annotation are replaced with getters and
 * setters that access an underlying repository directly.
 */
public class PropertyMapperProcessor implements BehaviourClassProcessor,
		Opcodes, Types {
	private static final String DESCRIPTOR_SUFFIX = "Descriptor";

	private static final String FACTORY_FIELD = "_$descriptorFactory";

	private static final String PROPERTY_SUFFIX = "Property";

	@Inject
	protected ClassDefiner definer;

	protected PropertyMapper propertyMapper;

	@Inject
	protected Class<? extends PropertySetFactory> propertySetDescriptorFactoryClass;

	private void addDescriptorFactoryField(BehaviourClassNode node) {
		String fieldName = getFactoryField();

		FieldNode factoryField = new FieldNode(Opcodes.ACC_PRIVATE
				| Opcodes.ACC_STATIC, fieldName,
				Type.getDescriptor(PropertySetFactory.class), null,
				null);
		node.addField(factoryField);

		Type factoryType = Type.getType(propertySetDescriptorFactoryClass);

		MethodNodeGenerator gen = node.getClassInitGen();
		gen.newInstance(factoryType);
		gen.dup();
		gen.invokeConstructor(factoryType,
				org.objectweb.asm.commons.Method.getMethod("void <init>()"));
		gen.putStatic(node.getType(), fieldName,
				Type.getType(PropertySetFactory.class));
	}

	private FieldNode createDescriptorField(PropertyDescriptor pd,
			BehaviourClassNode node) throws Exception {
		Method method = pd.getReadMethod();
		Method setter = pd.getWriteMethod();
		String property = pd.getName();
		String setterName = setter == null ? null : setter.getName();
		Class<?> dc = method.getDeclaringClass();
		String getterName = method.getName();
		Type propertyDescType = Type.getType(PropertyDescriptor.class);
		Type propertySetDescType = Type.getType(PropertySetDescriptor.class);
		String fieldName = getDescriptorField(property);

		FieldNode descriptorField = new FieldNode(Opcodes.ACC_PRIVATE
				| Opcodes.ACC_STATIC, fieldName,
				propertySetDescType.getDescriptor(), null, null);
		node.addField(descriptorField);

		MethodNodeGenerator gen = node.getClassInitGen();
		loadFactory(node, gen);

		// instantiate PropertyDescriptor
		gen.newInstance(propertyDescType);
		gen.dup();
		gen.push(property);
		gen.push(Type.getType(dc));
		gen.push(getterName);
		gen.push(setterName);
		gen.invokeConstructor(propertyDescType,
				new org.objectweb.asm.commons.Method("<init>", Type.VOID_TYPE,
						new Type[] { STRING_TYPE, CLASS_TYPE, STRING_TYPE,
								STRING_TYPE }));

		gen.push(propertyMapper.findPredicate(pd));
		gen.push(false);

		// call PropertySetDescriptor.createDescriptor(...)
		gen.invoke(Methods.PROPERTYSETFACTORY_CREATEDESCRIPTOR);
		gen.putStatic(node.getType(), descriptorField.name, propertySetDescType);

		return descriptorField;
	}

	private FieldNode createPropertyField(String property,
			BehaviourClassNode node) throws Exception {
		String fieldName = getPropertyFieldName(property);
		FieldNode propertyField = new FieldNode(Opcodes.ACC_PRIVATE, fieldName,
				Type.getDescriptor(PropertySet.class), null, null);
		node.addField(propertyField);
		return propertyField;
	}

	private String getDescriptorField(String property) {
		return "_$" + property + DESCRIPTOR_SUFFIX;
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

	private void implementGetter(Method getter, FieldNode field,
			FieldNode descriptorField, BehaviourClassNode node)
			throws Exception {
		Class<?> classType = getter.getReturnType();
		Type type = Type.getType(classType);

		ExtendedMethod mn = node.addExtendedMethod(getter, definer);
		BehaviourMethodGenerator gen = new BehaviourMethodGenerator(mn);

		lazyInitializePropertySet(field, descriptorField, gen);

		if (isCollection(classType)) {
			gen.loadThis();
			gen.getField(field.name, Type.getType(field.desc));
			gen.invoke(Methods.PROPERTYSET_GET_ALL);
			gen.returnValue();
		} else if (classType.isPrimitive()) {
			gen.loadThis();
			gen.getField(field.name, Type.getType(field.desc));
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
			gen.loadThis();
			gen.getField(field.name, Type.getType(field.desc));
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
			gen.loadThis();
			gen.getField(field.name, Type.getType(field.desc));
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
		String property = pd.getName();
		FieldNode field = createPropertyField(property, node);
		FieldNode descriptor = createDescriptorField(pd, node);

		implementGetter(pd.getReadMethod(), field, descriptor, node);

		Method setter = pd.getWriteMethod();
		if (setter != null) {
			implementSetter(setter, field, descriptor, node);
		}
	}

	@Override
	public boolean implementsClass(Class<?> concept) {
		return !propertyMapper.findProperties(concept).isEmpty();
	}

	private void implementSetter(Method setter, FieldNode field,
			FieldNode descriptorField, BehaviourClassNode node)
			throws Exception {
		Class<?> classType = setter.getParameterTypes()[0];
		Type type = Type.getType(classType);

		ExtendedMethod mn = node.addExtendedMethod(setter, definer);
		BehaviourMethodGenerator gen = new BehaviourMethodGenerator(mn);

		lazyInitializePropertySet(field, descriptorField, gen);

		gen.loadThis();
		gen.getField(field.name, Type.getType(field.desc));

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

	private void lazyInitializePropertySet(FieldNode field,
			FieldNode descriptorField, BehaviourMethodGenerator gen)
			throws Exception {
		gen.loadThis();
		gen.getField(field.name, Type.getType(field.desc));

		Label exists = gen.newLabel();
		gen.ifNonNull(exists);
		gen.loadThis(); // required for put field
		gen.getStatic(descriptorField.name, Type.getType(descriptorField.desc));

		gen.loadBean();
		gen.invokeInterface(
				Type.getType(PropertySetDescriptor.class),
				new org.objectweb.asm.commons.Method(
						PropertySetDescriptor.CREATE_PROPERTY_SET, Type
								.getType(PropertySet.class),
						new Type[] { OBJECT_TYPE }));
		gen.injectMembers();
		gen.putField(field.name, Type.getType(field.desc));

		gen.mark(exists);
	}

	private void loadFactory(BehaviourClassNode node, MethodNodeGenerator gen) {
		gen.getStatic(node.getType(), getFactoryField(),
				Type.getType(PropertySetFactory.class));
	}

	private void mergeProperty(String property, Class<?> type,
			BehaviourMethodGenerator gen) throws Exception {
		FieldNode field = gen.getMethod().getOwner()
				.getField(getPropertyFieldName(property));
		FieldNode descriptorField = gen.getMethod().getOwner()
				.getField(getDescriptorField(property));
		if (type.isPrimitive()) {
			lazyInitializePropertySet(field, descriptorField, gen);
			persistValue(field, type, gen);
		} else {
			gen.dup();

			Label isNull = gen.newLabel();
			gen.ifNull(isNull);

			lazyInitializePropertySet(field, descriptorField, gen);
			persistValue(field, type, gen);

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

			mergeProperty(pd.getName(), pd.getPropertyType(), gen);
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

	private void persistValue(FieldNode field, Class<?> type,
			BehaviourMethodGenerator gen) throws Exception {
		gen.loadThis();
		gen.getField(field.name, Type.getType(field.desc));
		gen.swap();
		if (isCollection(type)) {
			gen.invoke(Methods.PROPERTYSET_ADD_ALL);
		} else {
			gen.box(Type.getType(type));
			gen.invoke(Methods.PROPERTYSET_ADD_SINGLE);
		}
		gen.pop();
	}

	private static final String INJECTOR_FIELD = "_$injector";

	@SuppressWarnings("unchecked")
	protected void addInjectorField(BehaviourClassNode classNode) {
		FieldNode injectorField = new FieldNode(ACC_PROTECTED, INJECTOR_FIELD,
				Type.getDescriptor(Injector.class), null, null);
		injectorField.visitAnnotation(Type.getDescriptor(Inject.class), true);
		classNode.fields.add(injectorField);
	}

	@Override
	public void process(BehaviourClassNode classNode) throws Exception {
		classNode.addInterface(Type.getInternalName(Mergeable.class));
		classNode.addInterface(Type.getInternalName(Refreshable.class));

		addDescriptorFactoryField(classNode);
		classNode.addInjectorField();

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
