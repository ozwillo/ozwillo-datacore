package org.oasis.datacore.core.entity.query.fulltext;

import java.util.List;

/**
 * Transforms a text into a list of tokens in normalized format (eg no accents, all lower case, etc.)
 * <p>
 * User: schambon
 * Date: 5/5/15
 */
public interface Tokenizer {

	List<String> tokenize(String input, boolean toLowerCase,boolean doNormalize);
	List<String> tokenize(String input);

}
