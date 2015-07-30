/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.common.notify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An extensible notification implementation.
 */
public class PropertyNotification implements IPropertyNotification,
		INotificationChain {
	/**
	 * An {@link #primitiveType indicator} that the feature is not a primitive
	 * type.
	 * 
	 * @see #primitiveType
	 */
	public static final int PRIMITIVE_TYPE_OBJECT = -1;

	/**
	 * An {@link #primitiveType indicator} that the feature is a
	 * <code>boolean</code>.
	 * 
	 * @see #primitiveType
	 */
	public static final int PRIMITIVE_TYPE_BOOLEAN = 0;

	/**
	 * An {@link #primitiveType indicator} that the feature is a
	 * <code>byte</code>.
	 * 
	 * @see #primitiveType
	 */
	public static final int PRIMITIVE_TYPE_BYTE = 1;

	/**
	 * An {@link #primitiveType indicator} that the feature is a
	 * <code>char</code>.
	 * 
	 * @see #primitiveType
	 */
	public static final int PRIMITIVE_TYPE_CHAR = 2;

	/**
	 * An {@link #primitiveType indicator} that the feature is a
	 * <code>double</code>.
	 * 
	 * @see #primitiveType
	 */
	public static final int PRIMITIVE_TYPE_DOUBLE = 3;

	/**
	 * An {@link #primitiveType indicator} that the feature is a
	 * <code>float</code>.
	 * 
	 * @see #primitiveType
	 */
	public static final int PRIMITIVE_TYPE_FLOAT = 4;

	/**
	 * An {@link #primitiveType indicator} that the feature is a
	 * <code>int</code>.
	 * 
	 * @see #primitiveType
	 */
	public static final int PRIMITIVE_TYPE_INT = 5;

	/**
	 * An {@link #primitiveType indicator} that the feature is a
	 * <code>long</code>.
	 * 
	 * @see #primitiveType
	 */
	public static final int PRIMITIVE_TYPE_LONG = 6;

	/**
	 * An {@link #primitiveType indicator} that the feature is a
	 * <code>short</code>.
	 * 
	 * @see #primitiveType
	 */
	public static final int PRIMITIVE_TYPE_SHORT = 7;

	/**
	 * A {@link #position position} value which indicates that {@link #isTouch}
	 * should return <code>false</code>.
	 * 
	 * @see #primitiveType
	 */
	protected static final int IS_SET_CHANGE_INDEX = NO_INDEX - 1;

	/**
	 * The type of the feature.
	 * 
	 * @see #PRIMITIVE_TYPE_OBJECT
	 * @see #PRIMITIVE_TYPE_BOOLEAN
	 * @see #PRIMITIVE_TYPE_BYTE
	 * @see #PRIMITIVE_TYPE_CHAR
	 * @see #PRIMITIVE_TYPE_DOUBLE
	 * @see #PRIMITIVE_TYPE_FLOAT
	 * @see #PRIMITIVE_TYPE_INT
	 * @see #PRIMITIVE_TYPE_LONG
	 * @see #PRIMITIVE_TYPE_SHORT
	 */
	protected int primitiveType;

	/**
	 * The type of the change.
	 * 
	 * @see #getEventType()
	 */
	protected int eventType;

	/**
	 * The old value for the case of {@link #PRIMITIVE_TYPE_OBJECT}.
	 * 
	 * @see #getOldValue()
	 */
	protected Object oldValue;

	/**
	 * The new value for the case of {@link #PRIMITIVE_TYPE_OBJECT}.
	 * 
	 * @see #getNewValue()
	 */
	protected Object newValue;

	/**
	 * The old value for the case of {@link #PRIMITIVE_TYPE_BOOLEAN},
	 * {@link #PRIMITIVE_TYPE_BYTE}, {@link #PRIMITIVE_TYPE_CHAR},
	 * {@link #PRIMITIVE_TYPE_INT}, {@link #PRIMITIVE_TYPE_LONG}, and
	 * {@link #PRIMITIVE_TYPE_SHORT}.
	 * 
	 * @see #getOldValue()
	 * @see #getOldBooleanValue()
	 * @see #getOldByteValue()
	 * @see #getOldCharValue()
	 * @see #getOldIntValue()
	 * @see #getOldLongValue()
	 * @see #getOldShortValue()
	 */
	protected long oldSimplePrimitiveValue;

	/**
	 * The new value for the case of {@link #PRIMITIVE_TYPE_BOOLEAN},
	 * {@link #PRIMITIVE_TYPE_BYTE}, {@link #PRIMITIVE_TYPE_CHAR},
	 * {@link #PRIMITIVE_TYPE_INT}, {@link #PRIMITIVE_TYPE_LONG}, and
	 * {@link #PRIMITIVE_TYPE_SHORT}.
	 * 
	 * @see #getNewValue()
	 * @see #getNewBooleanValue()
	 * @see #getNewByteValue()
	 * @see #getNewCharValue()
	 * @see #getNewIntValue()
	 * @see #getNewLongValue()
	 * @see #getNewShortValue()
	 */
	protected long newSimplePrimitiveValue;

	/**
	 * The old value for the case of {@link #PRIMITIVE_TYPE_DOUBLE}, and
	 * {@link #PRIMITIVE_TYPE_FLOAT}.
	 * 
	 * @see #getOldValue()
	 * @see #getOldDoubleValue()
	 * @see #getOldFloatValue()
	 */
	protected double oldIEEEPrimitiveValue;

	/**
	 * The old value for the case of {@link #PRIMITIVE_TYPE_DOUBLE}, and
	 * {@link #PRIMITIVE_TYPE_FLOAT}.
	 * 
	 * @see #getOldValue()
	 * @see #getOldDoubleValue()
	 * @see #getOldFloatValue()
	 */
	protected double newIEEEPrimitiveValue;

	/**
	 * The position.
	 * 
	 * @see #getPosition()
	 */
	protected int position;

	/**
	 * The internal chain.
	 */
	protected INotificationChain chain;

	protected Object property;

	protected Object source;

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldValue
	 *            the old value before the change.
	 * @param newValue
	 *            the new value after the change.
	 */
	public PropertyNotification(int eventType, Object oldValue, Object newValue) {
		this(eventType, oldValue, newValue, NO_INDEX);
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldValue
	 *            the old value before the change.
	 * @param newValue
	 *            the new value after the change.
	 * @param isSetChange
	 *            the indication of whether the state has changed.
	 */
	public PropertyNotification(int eventType, Object oldValue,
			Object newValue, boolean isSetChange) {
		this(eventType, oldValue, newValue, isSetChange ? IS_SET_CHANGE_INDEX
				: NO_INDEX);
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldValue
	 *            the old value before the change.
	 * @param newValue
	 *            the new value after the change.
	 */
	public PropertyNotification(int eventType, Object oldValue,
			Object newValue, int position) {
		this.eventType = eventType;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.position = position;
		this.primitiveType = PRIMITIVE_TYPE_OBJECT;
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldValue
	 *            the old value before the change.
	 * @param newValue
	 *            the new value after the change.
	 * @param wasSet
	 *            whether the feature was set before the change.
	 */
	public PropertyNotification(int eventType, Object oldValue,
			Object newValue, int position, boolean wasSet) {
		this.eventType = eventType;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.position = position;
		this.primitiveType = PRIMITIVE_TYPE_OBJECT;
		if (!wasSet) {
			this.position = IS_SET_CHANGE_INDEX - position - 1;
		}
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldBooleanValue
	 *            the old value before the change.
	 * @param newBooleanValue
	 *            the new value after the change.
	 * @param isSetChange
	 *            the indication of whether the state has changed.
	 */
	public PropertyNotification(int eventType, boolean oldBooleanValue,
			boolean newBooleanValue, boolean isSetChange) {
		this(eventType, oldBooleanValue, newBooleanValue);
		if (isSetChange) {
			this.position = IS_SET_CHANGE_INDEX;
		}
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldBooleanValue
	 *            the old value before the change.
	 * @param newBooleanValue
	 *            the new value after the change.
	 */
	public PropertyNotification(int eventType, boolean oldBooleanValue,
			boolean newBooleanValue) {
		this.eventType = eventType;
		this.oldSimplePrimitiveValue = oldBooleanValue ? 1 : 0;
		this.newSimplePrimitiveValue = newBooleanValue ? 1 : 0;
		this.position = NO_INDEX;
		this.primitiveType = PRIMITIVE_TYPE_BOOLEAN;
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldByteValue
	 *            the old value before the change.
	 * @param newByteValue
	 *            the new value after the change.
	 * @param isSetChange
	 *            the indication of whether the state has changed.
	 */
	public PropertyNotification(int eventType, byte oldByteValue,
			byte newByteValue, boolean isSetChange) {
		this(eventType, oldByteValue, newByteValue);
		if (isSetChange) {
			this.position = IS_SET_CHANGE_INDEX;
		}
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldByteValue
	 *            the old value before the change.
	 * @param newByteValue
	 *            the new value after the change.
	 */
	public PropertyNotification(int eventType, byte oldByteValue,
			byte newByteValue) {
		this.eventType = eventType;
		this.oldSimplePrimitiveValue = oldByteValue;
		this.newSimplePrimitiveValue = newByteValue;
		this.position = PropertyNotification.NO_INDEX;
		this.primitiveType = PRIMITIVE_TYPE_BYTE;
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldCharValue
	 *            the old value before the change.
	 * @param newCharValue
	 *            the new value after the change.
	 * @param isSetChange
	 *            the indication of whether the state has changed.
	 */
	public PropertyNotification(int eventType, char oldCharValue,
			char newCharValue, boolean isSetChange) {
		this(eventType, oldCharValue, newCharValue);
		if (isSetChange) {
			this.position = IS_SET_CHANGE_INDEX;
		}
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldCharValue
	 *            the old value before the change.
	 * @param newCharValue
	 *            the new value after the change.
	 */
	public PropertyNotification(int eventType, char oldCharValue,
			char newCharValue) {
		this.eventType = eventType;
		this.oldSimplePrimitiveValue = oldCharValue;
		this.newSimplePrimitiveValue = newCharValue;
		this.position = PropertyNotification.NO_INDEX;
		this.primitiveType = PRIMITIVE_TYPE_CHAR;
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldDoubleValue
	 *            the old value before the change.
	 * @param newDoubleValue
	 *            the new value after the change.
	 * @param isSetChange
	 *            the indication of whether the state has changed.
	 */
	public PropertyNotification(int eventType, double oldDoubleValue,
			double newDoubleValue, boolean isSetChange) {
		this(eventType, oldDoubleValue, newDoubleValue);
		if (isSetChange) {
			this.position = IS_SET_CHANGE_INDEX;
		}
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldDoubleValue
	 *            the old value before the change.
	 * @param newDoubleValue
	 *            the new value after the change.
	 */
	public PropertyNotification(int eventType, double oldDoubleValue,
			double newDoubleValue) {
		this.eventType = eventType;
		this.oldIEEEPrimitiveValue = oldDoubleValue;
		this.newIEEEPrimitiveValue = newDoubleValue;
		this.position = PropertyNotification.NO_INDEX;
		this.primitiveType = PRIMITIVE_TYPE_DOUBLE;
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldFloatValue
	 *            the old value before the change.
	 * @param newFloatValue
	 *            the new value after the change.
	 * @param isSetChange
	 *            the indication of whether the state has changed.
	 */
	public PropertyNotification(int eventType, float oldFloatValue,
			float newFloatValue, boolean isSetChange) {
		this(eventType, oldFloatValue, newFloatValue);
		if (isSetChange) {
			this.position = IS_SET_CHANGE_INDEX;
		}
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldFloatValue
	 *            the old value before the change.
	 * @param newFloatValue
	 *            the new value after the change.
	 */
	public PropertyNotification(int eventType, float oldFloatValue,
			float newFloatValue) {
		this.eventType = eventType;
		this.oldIEEEPrimitiveValue = oldFloatValue;
		this.newIEEEPrimitiveValue = newFloatValue;
		this.position = PropertyNotification.NO_INDEX;
		this.primitiveType = PRIMITIVE_TYPE_FLOAT;
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldIntValue
	 *            the old value before the change.
	 * @param newIntValue
	 *            the new value after the change.
	 * @param isSetChange
	 *            the indication of whether the state has changed.
	 */
	public PropertyNotification(int eventType, int oldIntValue,
			int newIntValue, boolean isSetChange) {
		this(eventType, oldIntValue, newIntValue);
		if (isSetChange) {
			this.position = IS_SET_CHANGE_INDEX;
		}
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldIntValue
	 *            the old value before the change.
	 * @param newIntValue
	 *            the new value after the change.
	 */
	public PropertyNotification(int eventType, int oldIntValue, int newIntValue) {
		this.eventType = eventType;
		this.oldSimplePrimitiveValue = oldIntValue;
		this.newSimplePrimitiveValue = newIntValue;
		this.position = PropertyNotification.NO_INDEX;
		this.primitiveType = PRIMITIVE_TYPE_INT;
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldLongValue
	 *            the old value before the change.
	 * @param newLongValue
	 *            the new value after the change.
	 * @param isSetChange
	 *            the indication of whether the state has changed.
	 */
	public PropertyNotification(int eventType, long oldLongValue,
			long newLongValue, boolean isSetChange) {
		this(eventType, oldLongValue, newLongValue);
		if (isSetChange) {
			this.position = IS_SET_CHANGE_INDEX;
		}
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldLongValue
	 *            the old value before the change.
	 * @param newLongValue
	 *            the new value after the change.
	 */
	public PropertyNotification(int eventType, long oldLongValue,
			long newLongValue) {
		this.eventType = eventType;
		this.oldSimplePrimitiveValue = oldLongValue;
		this.newSimplePrimitiveValue = newLongValue;
		this.position = PropertyNotification.NO_INDEX;
		this.primitiveType = PRIMITIVE_TYPE_LONG;
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldShortValue
	 *            the old value before the change.
	 * @param newShortValue
	 *            the new value after the change.
	 * @param isSetChange
	 *            the indication of whether the state has changed.
	 */
	public PropertyNotification(int eventType, short oldShortValue,
			short newShortValue, boolean isSetChange) {
		this(eventType, oldShortValue, newShortValue);
		if (isSetChange) {
			this.position = IS_SET_CHANGE_INDEX;
		}
	}

	/**
	 * Creates an instance.
	 * 
	 * @param eventType
	 *            the type of the change.
	 * @param oldShortValue
	 *            the old value before the change.
	 * @param newShortValue
	 *            the new value after the change.
	 */
	public PropertyNotification(int eventType, short oldShortValue,
			short newShortValue) {
		this.eventType = eventType;
		this.oldSimplePrimitiveValue = oldShortValue;
		this.newSimplePrimitiveValue = newShortValue;
		this.position = PropertyNotification.NO_INDEX;
		this.primitiveType = PRIMITIVE_TYPE_SHORT;
	}

	public PropertyNotification setSource(Object source) {
		this.source = source;
		return this;
	}

	public Object getSubject() {
		return null;
	}

	public int getEventType() {
		return eventType;
	}

	public PropertyNotification setProperty(Object property) {
		this.property = property;
		return this;
	}

	public Object getProperty() {
		return null;
	}

	public Object getOldValue() {
		if (oldValue == null) {
			switch (primitiveType) {
			case PRIMITIVE_TYPE_BOOLEAN:
				oldValue = getOldBooleanValue() ? Boolean.TRUE : Boolean.FALSE;
				break;
			case PRIMITIVE_TYPE_BYTE:
				oldValue = getOldByteValue();
				break;
			case PRIMITIVE_TYPE_CHAR:
				oldValue = getOldCharValue();
				break;
			case PRIMITIVE_TYPE_DOUBLE:
				oldValue = getOldDoubleValue();
				break;
			case PRIMITIVE_TYPE_FLOAT:
				oldValue = getOldFloatValue();
				break;
			case PRIMITIVE_TYPE_LONG:
				oldValue = getOldLongValue();
				break;
			case PRIMITIVE_TYPE_INT:
				oldValue = getOldIntValue();
				break;
			case PRIMITIVE_TYPE_SHORT:
				oldValue = getOldShortValue();
				break;
			}
		}
		return oldValue;
	}

	public Object getNewValue() {
		if (newValue == null) {
			switch (primitiveType) {
			case PRIMITIVE_TYPE_BOOLEAN:
				newValue = getNewBooleanValue() ? Boolean.TRUE : Boolean.FALSE;
				break;
			case PRIMITIVE_TYPE_BYTE:
				newValue = getNewByteValue();
				break;
			case PRIMITIVE_TYPE_CHAR:
				newValue = getNewCharValue();
				break;
			case PRIMITIVE_TYPE_DOUBLE:
				newValue = getNewDoubleValue();
				break;
			case PRIMITIVE_TYPE_FLOAT:
				newValue = getNewFloatValue();
				break;
			case PRIMITIVE_TYPE_LONG:
				newValue = getNewLongValue();
				break;
			case PRIMITIVE_TYPE_INT:
				newValue = getNewIntValue();
				break;
			case PRIMITIVE_TYPE_SHORT:
				newValue = getNewShortValue();
				break;
			}
		}
		return newValue;
	}

	public boolean isTouch() {
		switch (eventType) {
		case PropertyNotification.RESOLVE:
		case PropertyNotification.REMOVING_ADAPTER: {
			return true;
		}
		case PropertyNotification.ADD:
		case PropertyNotification.ADD_MANY:
		case PropertyNotification.REMOVE:
		case PropertyNotification.REMOVE_MANY:
		case PropertyNotification.MOVE: {
			return false;
		}
		case PropertyNotification.SET:
		case PropertyNotification.UNSET: {
			if (position == IS_SET_CHANGE_INDEX) {
				return false;
			} else {
				switch (primitiveType) {
				case PRIMITIVE_TYPE_BOOLEAN:
				case PRIMITIVE_TYPE_BYTE:
				case PRIMITIVE_TYPE_CHAR:
				case PRIMITIVE_TYPE_LONG:
				case PRIMITIVE_TYPE_INT:
				case PRIMITIVE_TYPE_SHORT: {
					return oldSimplePrimitiveValue == newSimplePrimitiveValue;
				}
				case PRIMITIVE_TYPE_DOUBLE:
				case PRIMITIVE_TYPE_FLOAT: {
					return oldIEEEPrimitiveValue == newIEEEPrimitiveValue;
				}
				default: {
					return oldValue == null ? newValue == null : oldValue
							.equals(newValue);
				}
				}
			}
		}
		default: {
			return false;
		}
		}
	}

	public boolean isReset() {
		switch (eventType) {
		case PropertyNotification.SET:
			Object defaultValue = getFeatureDefaultValue();
			switch (primitiveType) {
			case PRIMITIVE_TYPE_BOOLEAN:
				return defaultValue != null
						&& ((Boolean) defaultValue).booleanValue() == (newSimplePrimitiveValue != 0);
			case PRIMITIVE_TYPE_BYTE:
				return defaultValue != null
						&& ((Byte) defaultValue).byteValue() == (byte) newSimplePrimitiveValue;
			case PRIMITIVE_TYPE_CHAR:
				return defaultValue != null
						&& ((Character) defaultValue).charValue() == (char) newSimplePrimitiveValue;
			case PRIMITIVE_TYPE_LONG:
				return defaultValue != null
						&& ((Long) defaultValue).longValue() == newSimplePrimitiveValue;
			case PRIMITIVE_TYPE_INT:
				return defaultValue != null
						&& ((Integer) defaultValue).intValue() == (int) newSimplePrimitiveValue;
			case PRIMITIVE_TYPE_SHORT:
				return defaultValue != null
						&& ((Short) defaultValue).shortValue() == (short) newSimplePrimitiveValue;
			case PRIMITIVE_TYPE_DOUBLE:
				return defaultValue != null
						&& ((Double) defaultValue).doubleValue() == newIEEEPrimitiveValue;
			case PRIMITIVE_TYPE_FLOAT:
				return defaultValue != null
						&& ((Float) defaultValue).floatValue() == (float) newIEEEPrimitiveValue;
			default:
				return defaultValue == null ? newValue == null : defaultValue
						.equals(newValue);
			}
		case PropertyNotification.UNSET:
			return true;
		default:
			return false;
		}
	}

	public boolean wasSet() {
		switch (eventType) {
		case PropertyNotification.SET: {
			if (isFeatureUnsettable()) {
				return position != IS_SET_CHANGE_INDEX;
			}
			break;
		}
		case PropertyNotification.UNSET: {
			if (isFeatureUnsettable()) {
				return position == IS_SET_CHANGE_INDEX;
			}
			break;
		}
		case PropertyNotification.ADD:
		case PropertyNotification.ADD_MANY:
		case PropertyNotification.REMOVE:
		case PropertyNotification.REMOVE_MANY:
		case PropertyNotification.MOVE: {
			return position > IS_SET_CHANGE_INDEX;
		}
		default: {
			return false;
		}
		}

		Object defaultValue = getFeatureDefaultValue();
		switch (primitiveType) {
		case PRIMITIVE_TYPE_BOOLEAN:
			return defaultValue != null
					&& ((Boolean) defaultValue).booleanValue() != (oldSimplePrimitiveValue != 0);
		case PRIMITIVE_TYPE_BYTE:
			return defaultValue != null
					&& ((Byte) defaultValue).byteValue() != (byte) oldSimplePrimitiveValue;
		case PRIMITIVE_TYPE_CHAR:
			return defaultValue != null
					&& ((Character) defaultValue).charValue() != (char) oldSimplePrimitiveValue;
		case PRIMITIVE_TYPE_LONG:
			return defaultValue != null
					&& ((Long) defaultValue).longValue() != oldSimplePrimitiveValue;
		case PRIMITIVE_TYPE_INT:
			return defaultValue != null
					&& ((Integer) defaultValue).intValue() != (int) oldSimplePrimitiveValue;
		case PRIMITIVE_TYPE_SHORT:
			return defaultValue != null
					&& ((Short) defaultValue).shortValue() != (short) oldSimplePrimitiveValue;
		case PRIMITIVE_TYPE_DOUBLE:
			return defaultValue != null
					&& ((Double) defaultValue).doubleValue() != oldIEEEPrimitiveValue;
		case PRIMITIVE_TYPE_FLOAT:
			return defaultValue != null
					&& ((Float) defaultValue).floatValue() != (float) oldIEEEPrimitiveValue;
		default:
			return defaultValue == null ? oldValue != null : !defaultValue
					.equals(oldValue);
		}
	}

	protected boolean isFeatureUnsettable() {
		return false;
	}

	protected Object getFeatureDefaultValue() {
		return null;
	}

	public int getPosition() {
		return position < 0 ? position < IS_SET_CHANGE_INDEX ? IS_SET_CHANGE_INDEX
				- position - 1
				: NO_INDEX
				: position;
	}

	public boolean merge(INotification notification) {
		if (!(notification instanceof IPropertyNotification)) {
			return false;
		}
		IPropertyNotification propertyNotification = (IPropertyNotification) notification;

		switch (eventType) {
		case PropertyNotification.SET:
		case PropertyNotification.UNSET: {
			int notificationEventType = propertyNotification.getEventType();
			switch (notificationEventType) {
			case PropertyNotification.SET:
			case PropertyNotification.UNSET: {
				Object notificationSource = propertyNotification.getSubject();
				if (notificationSource == getSubject()
						&& getProperty().equals(
								propertyNotification.getProperty())) {
					newValue = propertyNotification.getNewValue();
					if (propertyNotification.getEventType() == PropertyNotification.SET) {
						eventType = PropertyNotification.SET;
					}
					return true;
				}
			}
			}
		}
		case PropertyNotification.REMOVE: {
			int notificationEventType = propertyNotification.getEventType();
			switch (notificationEventType) {
			case PropertyNotification.REMOVE: {
				Object notificationSource = propertyNotification.getSubject();
				if (notificationSource == getSubject()
						&& getProperty().equals(
								propertyNotification.getProperty())) {
					boolean originalWasSet = wasSet();
					int originalPosition = getPosition();
					int notificationPosition = propertyNotification
							.getPosition();

					eventType = PropertyNotification.REMOVE_MANY;
					List<Object> removedValues = new ArrayList<Object>(2);
					if (originalPosition <= notificationPosition) {
						removedValues.add(oldValue);
						removedValues.add(propertyNotification.getOldValue());
						newValue = new int[] { position = originalPosition,
								notificationPosition + 1 };
					} else {
						removedValues.add(propertyNotification.getOldValue());
						removedValues.add(oldValue);
						newValue = new int[] { position = notificationPosition,
								originalPosition };
					}
					oldValue = removedValues;

					if (!originalWasSet) {
						position = IS_SET_CHANGE_INDEX - position - 1;
					}

					return true;
				}
				break;
			}
			}
			break;
		}
		case PropertyNotification.REMOVE_MANY: {
			int notificationEventType = propertyNotification.getEventType();
			switch (notificationEventType) {
			case PropertyNotification.REMOVE: {
				Object notificationSource = propertyNotification.getSubject();
				if (notificationSource == getSubject()
						&& getProperty().equals(
								propertyNotification.getProperty())) {
					boolean originalWasSet = wasSet();
					int notificationPosition = propertyNotification
							.getPosition();

					int[] positions = (int[]) newValue;
					int[] newPositions = new int[positions.length + 1];

					int index = 0;
					while (index < positions.length) {
						int oldPosition = positions[index];
						if (oldPosition <= notificationPosition) {
							newPositions[index++] = oldPosition;
							++notificationPosition;
						} else {
							break;
						}
					}

					@SuppressWarnings("unchecked")
					List<Object> list = (List<Object>) oldValue;
					list.add(index, propertyNotification.getOldValue());
					newPositions[index] = notificationPosition;

					while (++index < newPositions.length) {
						newPositions[index] = positions[index - 1];
					}

					newValue = newPositions;

					if (!originalWasSet) {
						position = IS_SET_CHANGE_INDEX - newPositions[0];
					}

					return true;
				}
				break;
			}
			}
			break;
		}
		}

		return false;
	}

	public boolean getOldBooleanValue() {
		if (primitiveType != PRIMITIVE_TYPE_BOOLEAN)
			throw new IllegalStateException();
		return oldSimplePrimitiveValue != 0;
	}

	public boolean getNewBooleanValue() {
		if (primitiveType != PRIMITIVE_TYPE_BOOLEAN)
			throw new IllegalStateException();
		return newSimplePrimitiveValue != 0;
	}

	public byte getOldByteValue() {
		if (primitiveType != PRIMITIVE_TYPE_BYTE)
			throw new IllegalStateException();
		return (byte) oldSimplePrimitiveValue;
	}

	public byte getNewByteValue() {
		if (primitiveType != PRIMITIVE_TYPE_BYTE)
			throw new IllegalStateException();
		return (byte) newSimplePrimitiveValue;
	}

	public char getOldCharValue() {
		if (primitiveType != PRIMITIVE_TYPE_CHAR)
			throw new IllegalStateException();
		return (char) oldSimplePrimitiveValue;
	}

	public char getNewCharValue() {
		if (primitiveType != PRIMITIVE_TYPE_CHAR)
			throw new IllegalStateException();
		return (char) newSimplePrimitiveValue;
	}

	public double getOldDoubleValue() {
		if (primitiveType != PRIMITIVE_TYPE_DOUBLE)
			throw new IllegalStateException();
		return oldIEEEPrimitiveValue;
	}

	public double getNewDoubleValue() {
		if (primitiveType != PRIMITIVE_TYPE_DOUBLE)
			throw new IllegalStateException();
		return newIEEEPrimitiveValue;
	}

	public float getOldFloatValue() {
		if (primitiveType != PRIMITIVE_TYPE_FLOAT)
			throw new IllegalStateException();
		return (float) oldIEEEPrimitiveValue;
	}

	public float getNewFloatValue() {
		if (primitiveType != PRIMITIVE_TYPE_FLOAT)
			throw new IllegalStateException();
		return (float) newIEEEPrimitiveValue;
	}

	public int getOldIntValue() {
		if (primitiveType != PRIMITIVE_TYPE_INT)
			throw new IllegalStateException();
		return (int) oldSimplePrimitiveValue;
	}

	public int getNewIntValue() {
		if (primitiveType != PRIMITIVE_TYPE_INT)
			throw new IllegalStateException();
		return (int) newSimplePrimitiveValue;
	}

	public long getOldLongValue() {
		if (primitiveType != PRIMITIVE_TYPE_LONG)
			throw new IllegalStateException();
		return oldSimplePrimitiveValue;
	}

	public long getNewLongValue() {
		if (primitiveType != PRIMITIVE_TYPE_LONG)
			throw new IllegalStateException();
		return newSimplePrimitiveValue;
	}

	public short getOldShortValue() {
		if (primitiveType != PRIMITIVE_TYPE_SHORT)
			throw new IllegalStateException();
		return (short) oldSimplePrimitiveValue;
	}

	public short getNewShortValue() {
		if (primitiveType != PRIMITIVE_TYPE_SHORT)
			throw new IllegalStateException();
		return (short) newSimplePrimitiveValue;
	}

	public String getOldStringValue() {
		return oldValue == null ? null : oldValue.toString();
	}

	public String getNewStringValue() {
		return newValue == null ? null : newValue.toString();
	}

	/**
	 * Adds or merges a new notification.
	 * 
	 * @param newNotification
	 *            a notification.
	 * @return <code>true</code> when the notification is added and
	 *         <code>false</code> when it is merged.
	 */
	@SuppressWarnings("unchecked")
	public boolean add(INotification newNotification) {
		if (newNotification == null) {
			return false;
		} else {
			if (merge(newNotification)) {
				return false;
			}

			if (chain == null) {
				chain = new NotificationChain(
						(INotificationBroadcaster) getSubject());
				chain.add(this);
				return chain.add(newNotification);
			} else {
				return chain.add(newNotification);
			}
		}
	}

	/*
	 * Javadoc copied from interface.
	 */
	@SuppressWarnings("unchecked")
	public void dispatch() {
		Object source = getSubject();

		if (chain == null) {
			if (source instanceof INotificationBroadcaster<?>) {
				((INotificationBroadcaster<IPropertyNotification>) source)
						.fireNotifications(Arrays.asList(this));
			}
		} else {
			chain.dispatch();
		}
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (eventType: ");
		switch (eventType) {
		case PropertyNotification.SET: {
			result.append("SET");
			break;
		}
		case PropertyNotification.UNSET: {
			result.append("UNSET");
			break;
		}
		case PropertyNotification.ADD: {
			result.append("ADD");
			break;
		}
		case PropertyNotification.ADD_MANY: {
			result.append("ADD_MANY");
			break;
		}
		case PropertyNotification.REMOVE: {
			result.append("REMOVE");
			break;
		}
		case PropertyNotification.REMOVE_MANY: {
			result.append("REMOVE_MANY");
			break;
		}
		case PropertyNotification.MOVE: {
			result.append("MOVE");
			break;
		}
		case PropertyNotification.REMOVING_ADAPTER: {
			result.append("REMOVING_ADAPTER");
			break;
		}
		case PropertyNotification.RESOLVE: {
			result.append("RESOLVE");
			break;
		}
		default: {
			result.append(eventType);
			break;
		}
		}
		if (isTouch()) {
			result.append(", touch: true");
		}
		result.append(", position: ");
		result.append(getPosition());
		result.append(", subject: ");
		result.append(getSubject());
		result.append(", feature: ");
		result.append(getProperty());
		result.append(", oldValue: ");
		result.append(getOldValue());
		result.append(", newValue: ");
		if (eventType == PropertyNotification.REMOVE_MANY
				&& newValue instanceof int[]) {
			int[] positions = (int[]) newValue;
			result.append("[");
			for (int i = 0; i < positions.length;) {
				result.append(positions[i]);
				if (++i < positions.length) {
					result.append(", ");
				}
			}
			result.append("]");
		} else {
			result.append(getNewValue());
		}

		result.append(", isTouch: ");
		result.append(isTouch());
		result.append(", wasSet: ");
		result.append(wasSet());
		result.append(")");

		return result.toString();
	}
}
