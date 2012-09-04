package net.enilink.komma.em.internal.query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import net.enilink.composition.mappers.RoleMapper;

import com.google.inject.Inject;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.dm.IDataManagerQuery;
import net.enilink.komma.em.internal.IEntityManagerInternal;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.IBooleanResult;
import net.enilink.komma.core.IGraphResult;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReferenceable;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.ITupleResult;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.LockModeType;
import net.enilink.komma.core.NoResultException;
import net.enilink.komma.core.NonUniqueResultException;
import net.enilink.komma.core.TemporalType;
import net.enilink.komma.core.URI;

public class EntityManagerQuery<R> extends QueryBase<IQuery<R>> implements
		IQuery<R> {
	protected IEntityManagerInternal manager;

	private WeakHashMap<IExtendedIterator<?>, Object> opened = new WeakHashMap<IExtendedIterator<?>, Object>(
			4);

	protected IDataManagerQuery<?> query;

	@Inject
	RoleMapper<URI> roleMapper;

	public EntityManagerQuery(IEntityManagerInternal manager,
			IDataManagerQuery<?> query) {
		this.manager = manager;
		this.query = query;
	}

	public void close() {
		for (IExtendedIterator<?> c : opened.keySet()) {
			c.close();
		}
	}

	public IExtendedIterator<R> evaluate() {
		return evaluateQuery(null, resultInfos);
	}

	public <T> IExtendedIterator<T> evaluate(Class<T> resultType,
			Class<?>... resultTypes) {
		// special case where a binding set should be returned as IBindings or
		// as Object[]
		if (IBindings.class.isAssignableFrom(resultType)
				|| resultType.isArray()) {
			return evaluateQuery(resultType, resultInfos);
		}

		ResultInfo resultInfo = new ResultInfo(false, new ArrayList<Class<?>>());
		resultInfo.types.add(resultType);
		for (Class<?> type : resultTypes) {
			resultInfo.types.add(type);
		}
		Map<String, ResultInfo> resultInfos;
		if (this.resultInfos == null) {
			resultInfos = Collections.singletonMap((String) null, resultInfo);
		} else {
			resultInfos = new HashMap<String, ResultInfo>(this.resultInfos);
			resultInfos.put(null, resultInfo);
		}
		return evaluateQuery(resultType, resultInfos);
	}

	@SuppressWarnings("unchecked")
	private <T> IExtendedIterator<T> evaluateQuery(Class<T> resultType,
			Map<String, ResultInfo> resultInfos) {
		IExtendedIterator<?> iter;
		IExtendedIterator<?> result = query.evaluate();
		int max = maxResults <= 0 ? 0 : maxResults + firstResult;

		if (result instanceof ITupleResult) {
			if (resultType != null
					&& IBindings.class.isAssignableFrom(resultType)
					|| (resultType == null && ((ITupleResult<?>) result)
							.getBindingNames().size() > 1)) {
				// returns an iterator of IBindings objects
				// this is the default behavior in case of multiple bindings
				iter = new TupleBindingsIterator(manager,
						(ITupleResult<IBindings<IValue>>) result, max,
						resultInfos);
			} else if (resultType != null && resultType.isArray()) {
				// returns an iterator of Object[]
				iter = new TupleArrayIterator(manager,
						(ITupleResult<IBindings<IValue>>) result, max,
						resultInfos);
			} else {
				iter = new ProjectedTupleIterator(manager,
						(ITupleResult<IBindings<IValue>>) result, max,
						resultInfos != null ? resultInfos.get(null) : null);
			}
		} else if (result instanceof IGraphResult) {
			if (resultType == null || IStatement.class.equals(resultType)) {
				iter = new GraphIterator(manager, (IGraphResult) result, max,
						resultInfos == null
								|| !resultInfos.get(null).typeRestricted);
			} else {
				iter = new ProjectedGraphIterator(manager,
						(IGraphResult) result, max,
						resultInfos != null ? resultInfos.get(null) : null);
			}
		} else {
			iter = new BooleanIterator(((IBooleanResult) result).asBoolean());
		}

		opened.put(iter, Boolean.TRUE);
		// skip elements if limit is used
		if (firstResult > 0) {
			for (int i = 0; i < firstResult && iter.hasNext(); i++) {
				iter.next();
			}
		}

		return (IExtendedIterator<T>) iter;
	}

	public <T> IExtendedIterator<T> evaluateRestricted(Class<T> resultType,
			Class<?>... resultTypes) {
		ResultInfo resultInfo = new ResultInfo(true, new ArrayList<Class<?>>());
		resultInfo.types.add(resultType);
		for (Class<?> type : resultTypes) {
			resultInfo.types.add(type);
		}

		return evaluateQuery(resultType,
				Collections.singletonMap((String) null, resultInfo));
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
	public Map<String, Object> getHints() {
		return null;
	}

	@Override
	public LockModeType getLockMode() {
		return null;
	}

	public List<R> getResultList() {
		return evaluate().toList();
	}

	public R getSingleResult() {
		return getSingleResult(null);
	}

	@SuppressWarnings("unchecked")
	public <T> T getSingleResult(Class<T> resultType) {
		IExtendedIterator<?> iter = resultType != null ? evaluate(resultType)
				: evaluateQuery(null, resultInfos);
		try {
			if (!iter.hasNext())
				throw new NoResultException("No results");
			T result = (T) iter.next();
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

	private void setParameter(String name, IValue value) {
		if (query == null)
			throw new UnsupportedOperationException();
		query.setParameter(name, value);
	}

	public IQuery<R> setHint(String hintName, Object value) {
		return this;
	}

	@Override
	public IQuery<R> setLockMode(LockModeType lockMode) {
		return this;
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
			setParameter(name, null);
		} else {
			if (value instanceof IReferenceable) {
				value = ((IReferenceable) value).getReference();
			}
			setParameter(name, value instanceof IValue ? (IValue) value
					: manager.createLiteral(value, null, null));
		}
		return this;
	}

	public IQuery<R> setParameter(String name, String label, Locale locale) {
		if (label == null) {
			setParameter(name, null);
		} else if (locale == null) {
			setParameter(name, manager.createLiteral(label, null, null));
		} else {
			String lang = locale.toString().toLowerCase().replace('_', '-');
			setParameter(name, manager.createLiteral(label, null, lang));
		}
		return this;
	}

	public IQuery<R> setType(String name, Class<?> concept) {
		setParameter(name, roleMapper.findType(concept));
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
