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
package net.enilink.komma.internal.sesame;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.LiteralFactory;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.result.BooleanResult;
import org.openrdf.result.GraphResult;
import org.openrdf.result.Result;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;

import com.google.inject.Inject;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.internal.query.QueryBase;
import net.enilink.komma.internal.query.ResultInfo;
import net.enilink.komma.internal.sesame.iterators.SesameBooleanResult;
import net.enilink.komma.internal.sesame.iterators.SesameGraphResult;
import net.enilink.komma.internal.sesame.iterators.SesameProjectedGraphResult;
import net.enilink.komma.internal.sesame.iterators.SesameProjectedTupleResult;
import net.enilink.komma.internal.sesame.iterators.SesameTupleResult;
import net.enilink.komma.core.FlushModeType;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.LockModeType;
import net.enilink.komma.core.NoResultException;
import net.enilink.komma.core.NonUniqueResultException;
import net.enilink.komma.core.TemporalType;
import net.enilink.komma.core.URI;
import net.enilink.komma.sesame.ISesameManager;

/**
 * Implements {@link IQuery} for use with {@link ISesameManager}.
 * 
 * @author James Leigh
 * @author Ken Wenzel
 */
public class SesameQuery<R> extends QueryBase<IQuery<R>> implements IQuery<R> {
	@Inject
	protected ISesameManager manager;

	private WeakHashMap<IExtendedIterator<?>, Object> opened;

	protected org.openrdf.query.Query query;

	@Inject
	protected SesameResourceManager resourceManager;

	public SesameQuery(org.openrdf.query.Query query) {
		this.opened = new WeakHashMap<IExtendedIterator<?>, Object>(4);
		this.query = query;
	}

	public void close() {
		for (IExtendedIterator<?> c : opened.keySet()) {
			c.close();
		}
	}

	public IExtendedIterator<R> evaluate() {
		try {
			IExtendedIterator<R> result = evaluateQuery(null, resultInfos);
			opened.put(result, Boolean.TRUE);
			if (firstResult > 0) {
				for (int i = 0; i < firstResult && result.hasNext(); i++) {
					result.next();
				}
			}
			return result;
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	public <T> IExtendedIterator<T> evaluate(Class<T> resultType,
			Class<?>... resultTypes) {
		ResultInfo resultInfo = new ResultInfo(false,
				new Class<?>[resultTypes.length + 1]);
		resultInfo.types[0] = resultType;
		System.arraycopy(resultTypes, 0, resultInfo.types, 1,
				resultTypes.length);

		try {
			IExtendedIterator<T> result = evaluateQuery(resultType, resultInfo);
			opened.put(result, Boolean.TRUE);
			if (firstResult > 0) {
				for (int i = 0; i < firstResult && result.hasNext(); i++) {
					result.next();
				}
			}
			return result;
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> IExtendedIterator<T> evaluateQuery(Class<T> resultType,
			ResultInfo... resultInfos) throws StoreException {
		IExtendedIterator<?> iter;
		Result<?> result = query.evaluate();
		int max = maxResults <= 0 ? 0 : maxResults + firstResult;

		if (result instanceof TupleResult) {
			if (resultType == null
					&& ((TupleResult) result).getBindingNames().size() > 1) {
				iter = new SesameTupleResult(manager, (TupleResult) result,
						max, resultInfos);
			} else {
				iter = new SesameProjectedTupleResult(manager,
						(TupleResult) result, max, resultInfos);
			}
		} else if (result instanceof GraphResult) {
			if (resultType == null || IStatement.class.equals(resultType)) {
				iter = new SesameGraphResult(manager, (GraphResult) result, max);
			} else {
				iter = new SesameProjectedGraphResult(manager,
						(GraphResult) result, max);
			}
		} else {
			iter = new SesameBooleanResult((BooleanResult) result);
		}
		return (IExtendedIterator<T>) iter;
	}

	public <T> IExtendedIterator<T> evaluateRestricted(Class<T> resultType,
			Class<?>... resultTypes) {
		ResultInfo resultInfo = new ResultInfo(true,
				new Class<?>[resultTypes.length + 1]);
		resultInfo.types[0] = resultType;
		System.arraycopy(resultTypes, 0, resultInfo.types, 1,
				resultTypes.length);

		try {
			IExtendedIterator<T> result = evaluateQuery(resultType, resultInfo);
			opened.put(result, Boolean.TRUE);
			if (firstResult > 0) {
				for (int i = 0; i < firstResult && result.hasNext(); i++) {
					result.next();
				}
			}
			return result;
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	public int executeUpdate() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getBooleanResult() {
		Object result = getSingleResult();

		return Boolean.TRUE.equals(result);
	}

	@Override
	public FlushModeType getFlushMode() {
		return null;
	}

	@Override
	public Map<String, Object> getHints() {
		return null;
	}

	public boolean getIncludeInferred() {
		return query.getIncludeInferred();
	}

	@Override
	public LockModeType getLockMode() {
		return null;
	}

	public List<R> getResultList() {
		return evaluate().toList();
	}

	public Object getSingleResult() {
		IExtendedIterator<?> iter = evaluate();
		try {
			if (!iter.hasNext())
				throw new NoResultException("No results");
			Object result = iter.next();
			if (iter.hasNext())
				throw new NonUniqueResultException("More than one result");
			return result;
		} finally {
			iter.close();
		}
	}

	@Override
	public Set<String> getSupportedHints() {
		return null;
	}

	private void setBinding(String name, Value value) {
		if (query == null)
			throw new UnsupportedOperationException();
		query.setBinding(name, value);
	}

	public IQuery<R> setFlushMode(FlushModeType flushMode) {
		if (FlushModeType.AUTO.equals(flushMode)) {
			manager.flush();
		}
		return this;
	}

	public IQuery<R> setHint(String hintName, Object value) {
		return this;
	}

	public SesameQuery<R> setIncludeInferred(boolean include) {
		query.setIncludeInferred(include);
		return this;
	}

	@Override
	public IQuery<R> setLockMode(LockModeType lockMode) {
		return null;
	}

	public IQuery<R> setParameter(String name, Calendar value,
			TemporalType temporalType) {
		assert value instanceof GregorianCalendar : value;
		GregorianCalendar cal = (GregorianCalendar) value;
		try {
			DatatypeFactory factory = DatatypeFactory.newInstance();
			XMLGregorianCalendar xcal = factory.newXMLGregorianCalendar(cal);
			switch (temporalType) {
			case DATE:
				xcal.setHour(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setMinute(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setSecond(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
				break;
			case TIME:
				xcal.setYear(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setMonth(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setDay(DatatypeConstants.FIELD_UNDEFINED);
				break;
			case TIMESTAMP:
				break;
			}
			return setParameter(name, xcal);
		} catch (DatatypeConfigurationException e) {
			throw new KommaException(e);
		}
	}

	public IQuery<R> setParameter(String name, Date value,
			TemporalType temporalType) {
		int y, M, d, h, m, s, i, z;
		try {
			z = DatatypeConstants.FIELD_UNDEFINED;
			DatatypeFactory factory = DatatypeFactory.newInstance();

			Calendar calendar = Calendar.getInstance();
			calendar.setTime(value);

			XMLGregorianCalendar xcal;
			switch (temporalType) {
			case DATE:
				y = calendar.get(Calendar.YEAR);
				M = calendar.get(Calendar.MONTH) + 1;
				d = calendar.get(Calendar.DATE);
				xcal = factory.newXMLGregorianCalendarDate(y, M, d, z);
				break;
			case TIME:
				h = calendar.get(Calendar.HOUR);
				m = calendar.get(Calendar.MINUTE);
				s = calendar.get(Calendar.SECOND);
				i = (int) (value.getTime() % 1000);
				xcal = factory.newXMLGregorianCalendarTime(h, m, s, i, z);
				break;
			case TIMESTAMP:
				y = calendar.get(Calendar.YEAR);
				M = calendar.get(Calendar.MONTH) + 1;
				d = calendar.get(Calendar.DATE);
				h = calendar.get(Calendar.HOUR);
				m = calendar.get(Calendar.MINUTE);
				s = calendar.get(Calendar.SECOND);
				i = (int) (value.getTime() % 1000);
				xcal = factory.newXMLGregorianCalendar(y, M, d, h, m, s, i, z);
				break;
			default:
				throw new AssertionError();
			}
			return setParameter(name, xcal);
		} catch (DatatypeConfigurationException e) {
			throw new KommaException(e);
		}
	}

	public IQuery<R> setParameter(String name, Object value) {
		if (value == null) {
			setBinding(name, null);
		} else {
			setBinding(
					name,
					value instanceof Value ? (Value) value : manager
							.getValue(value));
		}
		return this;
	}

	public IQuery<R> setParameter(String name, String label, Locale locale) {
		RepositoryConnection conn = manager.getConnection();
		LiteralFactory literalFactory = conn.getRepository()
				.getLiteralFactory();
		if (label == null) {
			setBinding(name, null);
		} else if (locale == null) {
			setBinding(name, literalFactory.createLiteral(label));
		} else {
			String lang = locale.toString().toLowerCase().replace('_', '-');
			setBinding(name, literalFactory.createLiteral(label, lang));
		}
		return this;
	}

	public IQuery<R> setURI(String name, URI uri) {
		setBinding(name, resourceManager.createResource(uri));
		return this;
	}

	public IQuery<R> setType(String name, Class<?> concept) {
		setBinding(name, manager.getRoleMapper().findType(concept));
		return this;
	}

	public IQuery<R> setValue(String name, Value value) {
		setBinding(name, value);
		return this;
	}

	@Override
	public String toString() {
		if (query == null) {
			return super.toString();
		}
		return query.toString();
	}

	@Override
	public <T> IQuery<T> bindResultType(Class<T> resultType,
			Class<?>... resultTypes) {
		return super.doBindResultType(resultType, resultTypes);
	}

	@Override
	public <T> IQuery<T> restrictResultType(Class<T> resultType,
			Class<?>... resultTypes) {
		return super.doRestrictResultType(resultType, resultTypes);
	}
}
