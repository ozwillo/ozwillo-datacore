package org.oasis.datacore.contribution.exception;

public class ContributionWithoutResourcesException extends Exception {

	private static final long serialVersionUID = -5074974735403779092L;

	public ContributionWithoutResourcesException() {
		super("A contribution must have resources attached, cannot continue, contribution is refused");
	}
	
}
