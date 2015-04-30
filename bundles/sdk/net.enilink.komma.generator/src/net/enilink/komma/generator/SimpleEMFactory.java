package net.enilink.komma.generator;

import java.util.Locale;

import net.enilink.komma.core.IDialect;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.IProvider;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.dm.IDataManagerFactory;

import com.google.inject.Inject;

public class SimpleEMFactory implements IEntityManagerFactory {
	@Inject
	IEntityManager em;

	@Inject
	IDataManagerFactory dmFactory;

	@Inject
	IDialect dialect;

	@Inject
	IUnitOfWork uow;

	@Override
	public boolean isOpen() {
		return dmFactory != null;
	}

	@Override
	public void close() {
		em.close();
		if (dmFactory != null) {
			dmFactory.close();
			dmFactory = null;
		}
	}

	@Override
	public IEntityManager create() {
		return get();
	}

	@Override
	public IEntityManager create(IEntityManager scope) {
		return create();
	}

	@Override
	public IEntityManagerFactory createChildFactory(KommaModule... modules) {
		return this;
	}

	@Override
	public IEntityManagerFactory createChildFactory(IProvider<Locale> locale,
			KommaModule... modules) {
		return this;
	}

	@Override
	public IEntityManager get() {
		return em;
	}

	@Override
	public IDialect getDialect() {
		return dialect;
	}

	@Override
	public IEntityManagerFactory getParent() {
		return null;
	}

	@Override
	public KommaModule getModule() {
		return null;
	}

	@Override
	public IUnitOfWork getUnitOfWork() {
		return uow;
	}

}
