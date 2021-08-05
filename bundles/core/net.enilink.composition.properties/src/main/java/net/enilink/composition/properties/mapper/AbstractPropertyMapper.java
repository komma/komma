package net.enilink.composition.properties.mapper;

import net.enilink.composition.mapping.IPropertyMapper;
import net.enilink.composition.mapping.PropertyAttribute;
import net.enilink.composition.mapping.PropertyDescriptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Locale.ENGLISH;

/**
 * Base class for custom property mappers.
 */
abstract public class AbstractPropertyMapper implements IPropertyMapper {
	protected static final String GET_PREFIX = "get";
	protected static final String SET_PREFIX = "set";
	protected static final String IS_PREFIX = "is";

	public AbstractPropertyMapper() {
	}

	@Override
	public Collection<PropertyDescriptor> getProperties(Class<?> concept) {
		List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>();
		while (concept != null) {
			for (Method method : concept.getDeclaredMethods()) {
				if (isMappedGetter(method)) {
					properties.add(createPropertyDescriptor(method));
				}
			}
			concept = concept.getSuperclass();
		}
		return properties;
	}

	abstract protected String getPredicate(Method readMethod);

	abstract protected boolean isMappedGetter(Method method);

	abstract protected List<PropertyAttribute> getAttributes(Method readMethod);

	protected PropertyDescriptor createPropertyDescriptor(Method readMethod) {
		List<PropertyAttribute> attributes = getAttributes(readMethod);
		String name = getPropertyName(readMethod);
		Method setter = getSetterMethod(name, readMethod);
		return new PropertyDescriptor(name, readMethod, setter, getPredicate(readMethod),
				attributes.toArray(new PropertyAttribute[attributes.size()]));
	}

	protected Method getSetterMethod(String property, Method getter) {
		try {
			Class<?> dc = getter.getDeclaringClass();
			Class<?> rt = getter.getReturnType();
			Method setter;
			if (isBeanGet(getter) || isBeanIs(getter)) {
				String setterName = SET_PREFIX + capitalize(property);
				setter = dc.getDeclaredMethod(setterName, rt);
			} else {
				setter = dc.getDeclaredMethod(getter.getName(), rt);
			}
			if (setter != null) {
				Class<?> returnType = setter.getReturnType();
				if (!Void.TYPE.equals(returnType)
						&& !returnType.isAssignableFrom(dc)) {
					throw new AssertionError(
							String.format(
									"Setter signature for property %s should either "
											+ "return void or %s or a superclass of %s",
									property, dc.getCanonicalName(),
									dc.getCanonicalName()));
				}
			}
			return setter;
		} catch (NoSuchMethodException exc) {
			return null;
		}
	}

	protected String getPropertyName(Method method) {
		String name = method.getName();
		if (isBeanGet(method)) {
			// Simple getter
			return decapitalize(name.substring(3));
		} else if (isBeanIs(method)) {
			// Boolean getter
			return decapitalize(name.substring(2));
		}
		return name;
	}

	protected static String capitalize(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		return name.substring(0, 1).toUpperCase(ENGLISH) + name.substring(1);
	}

	protected static String decapitalize(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		if (name.length() > 1 && Character.isUpperCase(name.charAt(1))
				&& Character.isUpperCase(name.charAt(0))) {
			return name;
		}
		char chars[] = name.toCharArray();
		chars[0] = Character.toLowerCase(chars[0]);
		return new String(chars);
	}

	protected static boolean isBeanGet(Method method) {
		String name = method.getName();
		return name.startsWith(GET_PREFIX) && name.length() > 3
				&& Character.isUpperCase(name.charAt(3));
	}

	protected static boolean isBeanIs(Method method) {
		String name = method.getName();
		boolean bool = method.getReturnType() == boolean.class;
		return bool && name.startsWith(IS_PREFIX) && name.length() > 2
				&& Character.isUpperCase(name.charAt(2));
	}
}
