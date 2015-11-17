package org.oasis.datacore.contribution.enumeration;

/**
 * A contribution has different status : 
 *	- REFUSED_BY_DATAOWNER (the data owner refuse the contribution and explain why)
 *	- APPROVED_BY_DATAOWNER (the contribution is approved and the owner is merging the data by himself)
 *	- MERGED (the contribution has been merged into the original model)
 * @author agiraudon
 *
 */
public enum ContributionStatus {
	
	REFUSED_BY_DATAOWNER,
	APPROVED_BY_DATAOWNER,
	MERGED;	
	
}
