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

/**
 * Filter which may be used for INotifications
 */
public abstract class NotificationFilter<T extends INotification> {
	/**
	 * Answer true iff the object <code>o</code> is acceptable. This method may
	 * also throw an exception if the argument is of a wrong type; it is not
	 * required to return <code>false</code> in such a case.
	 */
	public abstract boolean accept(T o);

	public static NotificationFilter<INotification> instanceOf(
			final Class<? extends INotification> clazz) {
		return new NotificationFilter<INotification>() {
			public boolean accept(INotification x) {
				return clazz.isAssignableFrom(x.getClass());
			}
		};
	}

	public NotificationFilter<T> and(final NotificationFilter<? super T> other) {
		return other == any() ? this : new NotificationFilter<T>() {
			public boolean accept(T x) {
				return NotificationFilter.this.accept(x) && other.accept(x);
			}
		};
	}

	@SuppressWarnings("unchecked")
	public NotificationFilter<T> or(final NotificationFilter<? super T> other) {
		return other == any() ? (NotificationFilter<T>) other
				: new NotificationFilter<T>() {
					public boolean accept(T x) {
						return NotificationFilter.this.accept(x)
								|| other.accept(x);
					}
				};
	}

	/**
	 * Creates a new filter that is the boolean negation of me.
	 * 
	 * @return the opposite of me
	 */
	public NotificationFilter<T> negated() {
		return new NotificationFilter<T>() {
			@Override
			public boolean accept(T notification) {
				return !NotificationFilter.this.accept(notification);
			}
		};
	}

	/**
	 * A Filter that accepts everything it's offered.
	 */
	private static final NotificationFilter<?> anyFilter = new NotificationFilter<INotification>() {
		public final boolean accept(INotification n) {
			return true;
		}

		@SuppressWarnings("unchecked")
		public NotificationFilter<INotification> and(
				NotificationFilter<? super INotification> other) {
			return (NotificationFilter<INotification>) other;
		}

		public NotificationFilter<INotification> or(
				NotificationFilter<? super INotification> other) {
			return this;
		}
	};

	@SuppressWarnings("unchecked")
	public static <T extends INotification> NotificationFilter<T> any() {
		return (NotificationFilter<T>) anyFilter;
	}
}