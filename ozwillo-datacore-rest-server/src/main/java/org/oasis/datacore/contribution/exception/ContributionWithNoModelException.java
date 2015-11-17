package org.oasis.datacore.contribution.exception;

public class ContributionWithNoModelException extends Exception {

	private static final long serialVersionUID = -3664358732339562390L;

	public ContributionWithNoModelException() {
		super("A contribution must be linked to a model, cannot continue, contribution is refused");
	}
	
}
