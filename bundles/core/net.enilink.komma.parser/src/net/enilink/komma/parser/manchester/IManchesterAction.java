package net.enilink.komma.parser.manchester;

public interface IManchesterAction {

	public abstract boolean createStmt(Object subject, Object predicate,
			Object object);

}