package org.oasis.datacore.historization.exception;

/** 
 * @author agiraudon 
 */
public class HistorizationException extends Exception {

	private static final long serialVersionUID = 2615507909974732398L;

	public HistorizationException(String message) {
		super("Historization problem : " + message);
	}
	
}
