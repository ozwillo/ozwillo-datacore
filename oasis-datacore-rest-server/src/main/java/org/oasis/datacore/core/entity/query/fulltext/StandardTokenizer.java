package org.oasis.datacore.core.entity.query.fulltext;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User: schambon
 * Date: 5/5/15
 */
@Service
public class StandardTokenizer implements Tokenizer {

	/**
	 * Tokenize a string (split) always as low case. Default.
	 * @param input : string to be spliced
	 * @return List of Strings spliced from low cased "input" String
	 */
	@Override
	public List<String> tokenize(String input) {
		return this.tokenize(input, true, true);

	}

	/**
	 * Tokenize a String (split).
	 * @param input : string to be spliced
	 * @param toLowerCase : true to put the input string in low case before split
	 * @return List of Strings spliced from original "input" String
	 */
	@Override
	public List<String> tokenize(String input, boolean toLowerCase, boolean doNormalize ) {
		if (input == null || "".equals(input)) {
			return Collections.emptyList();
		}

		return Arrays.asList(input.split("[\\p{P}\\s]+"))
				.stream()
				.map(s -> (toLowerCase ? s.toLowerCase() : s))
				.map(s -> (doNormalize ?  Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCOMBINING_DIACRITICAL_MARKS}+", "") : s))
				.collect(Collectors.toList());

	}
}
