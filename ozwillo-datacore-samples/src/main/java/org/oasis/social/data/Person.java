package org.oasis.social.data;

import org.joda.time.LocalDate;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Copied from GRU prototype's. Used in copied parts of social test.
 * TODO use it in container & auth demo tests, or remove it.
 * 
 * Person, who may :LIVE_IN or :WORK_FOR an {@link City}.
 * A person may also :HAVE_AUTHORITY_OVER another person (e.g. parent -> child relationship)
 */
@NodeEntity
public class Person {
    @GraphId Long id;

    @Indexed(unique = true)
    String username;

    String firstName;
    String lastName;
    LocalDate dateOfBirth;

    @RelatedTo(type="LIVES_IN")
    @Fetch
    City placeOfResidence;

    @RelatedTo(type="WORKS_FOR")
    @Fetch
    City placeOfEmployment;

    @RelatedTo(type="HAS_AUTHORITY_OVER")
    @Fetch
    Set<Person> guardees = new HashSet<Person>();

    @RelatedTo(type="HAS_AUTHORITY_OVER", direction = Direction.INCOMING)
    @Fetch
    Person guardian;

    public Person() {

    }

    public Person(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public City getPlaceOfResidence() {
        return placeOfResidence;
    }

    public void setPlaceOfResidence(City placeOfResidence) {
        this.placeOfResidence = placeOfResidence;
    }

    public City getPlaceOfEmployment() {
        return placeOfEmployment;
    }

    public void setPlaceOfEmployment(City placeOfEmployment) {
        this.placeOfEmployment = placeOfEmployment;
    }

    public Person getGuardian() {
        return guardian;
    }

    public void setGuardian(Person guardian) {
        this.guardian = guardian;
    }

    public Set<Person> getGuardees() {
        return guardees;
    }

    public void addGuardee(Person guardee) {
        guardees.add(guardee);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;

        Person person = (Person) o;

        if (id != null ? !id.equals(person.id) : person.id != null) return false;
        if (username != null ? !username.equals(person.username) : person.username != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}
