package net.enilink.komma.model;

/**
 * Defines status codes relevant to the KOMMA Model plug-in. When a Core
 * exception is thrown, it contain a status object describing the cause of the
 * exception. The status objects originating from the document UI plug-in use
 * the codes defined in this interface.
 */
public interface IModelStatusConstants {
	public static final int INTERNAL_ERROR = 10001;
	public static final int INTERNAL_WARNING = 10002;
}
