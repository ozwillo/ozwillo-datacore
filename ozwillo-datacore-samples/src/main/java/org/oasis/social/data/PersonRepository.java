package org.oasis.social.data;

import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copied from GRU prototype's. Used in copied parts of social test.
 * TODO use it in container & auth demo tests, or remove it.
 */
@Transactional
public interface PersonRepository extends GraphRepository<Person> {

    Person findByUsername(String username);


}
