package org.oasis.social.data;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

import java.util.Set;

/**
 * Copied from GRU prototype's. Used in copied parts of social test.
 * TODO use it in container & auth demo tests, or remove it.
 * 
 * Basic city (or local government entity)
 */
@NodeEntity
public class City {

    @GraphId Long id;

    /**
     * Short name of the organization. This is unique; e.g. "Valence" or "Turin"
     */
    @Indexed(unique = true) String name;

    @RelatedTo(type = "LIVES_IN", direction = Direction.INCOMING)
    Set<Person> citizens;

    @RelatedTo(type = "WORKS_IN", direction = Direction.INCOMING)
    Set<Person> employees;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public City() {

    }

    public City(String name) {
        this.name = name;
    }

    public Set<Person> getCitizens() {
        return citizens;
    }

    public Set<Person> getEmployees() {
        return employees;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof City)) return false;

        City city = (City) o;

        if (id != null ? !id.equals(city.id) : city.id != null) return false;
        if (name != null ? !name.equals(city.name) : city.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}

