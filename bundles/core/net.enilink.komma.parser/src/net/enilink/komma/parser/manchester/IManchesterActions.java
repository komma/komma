package net.enilink.komma.parser.manchester;

public interface IManchesterActions {
	boolean createStmt(Object subject, Object predicate, Object object);

	boolean isObjectProperty(Object property);

	boolean isDataProperty(Object property);
}