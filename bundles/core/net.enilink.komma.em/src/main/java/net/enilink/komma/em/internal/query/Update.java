package net.enilink.komma.em.internal.query;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import net.enilink.composition.mappers.RoleMapper;
import net.enilink.komma.core.IReferenceable;
import net.enilink.komma.core.IUpdate;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.URI;
import net.enilink.komma.dm.IDataManagerUpdate;
import net.enilink.komma.em.internal.IEntityManagerInternal;

import com.google.inject.Inject;

public class Update implements IUpdate {
	protected IEntityManagerInternal manager;

	protected IDataManagerUpdate update;

	@Inject
	RoleMapper<URI> roleMapper;

	public Update(IEntityManagerInternal manager, IDataManagerUpdate update) {
		this.manager = manager;
		this.update = update;
	}

	private void doSetParameter(String name, IValue value) {
		update.setParameter(name, value);
	}

	@Override
	public void execute() {
		update.execute();
	}

	@Override
	public Map<String, Object> getHints() {
		return Collections.emptyMap();
	}

	@Override
	public Set<String> getSupportedHints() {
		return Collections.emptySet();
	}

	@Override
	public IUpdate setHint(String hintName, Object value) {
		return this;
	}

	public IUpdate setParameter(String name, Object value) {
		if (value == null) {
			doSetParameter(name, null);
		} else {
			if (value instanceof IReferenceable) {
				value = ((IReferenceable) value).getReference();
			}
			doSetParameter(name, value instanceof IValue ? (IValue) value
					: manager.toValue(value));
		}
		return this;
	}

	public IUpdate setTypeParameter(String name, Class<?> concept) {
		doSetParameter(name, roleMapper.findType(concept));
		return this;
	}
}
