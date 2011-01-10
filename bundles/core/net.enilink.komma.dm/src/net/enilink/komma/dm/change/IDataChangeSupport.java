package net.enilink.komma.dm.change;

import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.URI;

public interface IDataChangeSupport extends IDataChangeListener {
	void add(IDataManager dm, IReference subj, IReference pred, IValue obj,
			IReference... contexts);

	void close(IDataManager dm);

	void commit(IDataManager dm);

	boolean isEnabled();

	void remove(IDataManager dm, IReference subj, IReference pred, IValue obj,
			IReference... contexts);

	void removeNamespace(IDataManager dm, String prefix, URI namespace);

	void rollback(IDataManager dm);

	void setNamespace(IDataManager dm, String prefix, URI oldNS, URI newNS);
}
