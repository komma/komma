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
package net.enilink.composition.properties.komma;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import net.enilink.composition.properties.exceptions.PropertyException;

import com.google.inject.Inject;

import net.enilink.commons.iterator.ConvertingIterator;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.ILiteralFactory;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.KommaException;

/**
 * {@link KommaPropertySet} used for localized properties. Only the best set of
 * literals are included in the results.
 * 
 */
public class LocalizedKommaPropertySet extends KommaPropertySet<String> {
	@Inject
	ILiteralFactory lf;

	Map<String, List<ILiteral>> cache;

	public LocalizedKommaPropertySet(IReference subject, IReference property) {
		super(subject, property, String.class, null);
	}

	private int addBestValues(ILiteral literal, String language, int best,
			Collection<ILiteral> values) {
		int score = best;
		String l = literal.getLanguage();
		if (language == l || language != null && language.equals(l)) {
			if (score < Integer.MAX_VALUE)
				values.clear();
			values.add(literal);
			score = Integer.MAX_VALUE;
		} else if (l != null && language != null && language.startsWith(l)) {
			if (score < l.length())
				values.clear();
			values.add(literal);
			score = l.length();
		} else if (l != null && language != null && score <= 1
				&& l.length() > 2 && language.startsWith(l.substring(0, 2))) {
			if (score < 1)
				values.clear();
			values.add(literal);
			score = 1;
		} else if (l != null && l.startsWith("en") && score <= 1) {
			if (score < 1)
				values.clear();
			values.add(literal);
			score = 1;
		} else if (l == null) {
			if (score < 0)
				values.clear();
			values.add(literal);
			score = 0;
		} else if (score < 0) {
			values.add(literal);
		}
		return score;
	}

	protected Collection<ILiteral> bestValues() {
		String language = manager.getLocale().getLanguage();

		List<ILiteral> values = cache != null ? cache.get(language) : null;
		if (values != null) {
			return values;
		}

		int score = -1;
		values = new ArrayList<ILiteral>();
		try {
			IExtendedIterator<ILiteral> literals = manager.createQuery(QUERY)
					.setParameter("s", subject).setParameter("p", property)
					.evaluateRestricted(ILiteral.class);
			try {
				for (ILiteral literal : literals) {
					score = addBestValues(literal, language, score, values);
				}
			} finally {
				literals.close();
			}
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
		if (cache == null) {
			cache = Collections
					.synchronizedMap(new HashMap<String, List<ILiteral>>());
		}
		cache.put(language, values);
		return values;
	}

	@Override
	public void clear() {
		ITransaction transaction = manager.getTransaction();
		try {
			boolean active = transaction.isActive();
			if (!active) {
				transaction.begin();
			}
			for (ILiteral literal : bestValues()) {
				remove(literal);
			}
			if (!active) {
				transaction.commit();
			}
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
	}

	@Override
	protected ILiteral convertInstance(Object instance) {
		if (instance instanceof ILiteral) {
			return (ILiteral) instance;
		}
		String language = manager.getLocale().getLanguage();
		return lf.createLiteral(instance.toString(), null, language);
	}

	@Override
	public String getSingle() {
		Iterator<ILiteral> iter = bestValues().iterator();
		if (iter.hasNext()) {
			return iter.next().getLabel();
		}
		return null;
	}

	@Override
	public boolean isEmpty() {
		return bestValues().isEmpty();
	}

	@Override
	public IExtendedIterator<String> iterator() {
		return new ConvertingIterator<ILiteral, String>(bestValues().iterator()) {
			ILiteral current;

			@Override
			protected String convert(ILiteral value) {
				current = value;
				return value.getLabel();
			}

			public void remove() {
				if (current == null) {
					throw new NoSuchElementException();
				}
				LocalizedKommaPropertySet.this.remove(current);
			}
		};
	}

	@Override
	public void refresh() {
		cache = null;
	}

	@Override
	public void setAll(Set<String> set) {
		if (this == set) {
			return;
		}
		Set<String> c = new HashSet<String>(set);
		ITransaction transaction = manager.getTransaction();
		try {
			boolean active = transaction.isActive();
			if (!active) {
				transaction.begin();
			}
			String language = manager.getLocale().getLanguage();

			IExtendedIterator<ILiteral> literals = manager.createQuery(QUERY)
					.setParameter("s", subject).setParameter("p", property)
					.evaluateRestricted(ILiteral.class);
			try {
				for (ILiteral literal : literals) {
					String l = literal.getLanguage();
					if (language == l || language != null && language.equals(l)) {
						Object label = literal.getLabel();
						if (c.contains(label)) {
							c.remove(label);
						} else {
							remove(literal);
						}
					}
				}
			} finally {
				literals.close();
			}
			if (c.size() > 0) {
				addAll(c);
			}
			if (!active) {
				transaction.commit();
			}
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
	}

	@Override
	public void setSingle(String o) {
		if (o == null) {
			clear();
		} else {
			setAll(Collections.singleton(o));
		}
	}

	@Override
	public int size() {
		return bestValues().size();
	}

	@Override
	public Object[] toArray() {
		return bestValues().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return bestValues().toArray(a);
	}
}
