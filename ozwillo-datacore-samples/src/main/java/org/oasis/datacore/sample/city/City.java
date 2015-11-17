package org.oasis.datacore.sample.city;
 
import org.oasis.datacore.sdk.data.spring.GenericEntity;
import org.springframework.data.mongodb.core.mapping.Document;

// TODO zipcodes, in their own collection...
@Document(collection = "cities")
public class City extends GenericEntity<City> {
 
	private String name;
	private String country;
	private int population;
	// mayor : Contact ?!??
	// precincts, cityhall address, coords...
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public int getPopulation() {
		return population;
	}
	public void setPopulation(int population) {
		this.population = population;
	}
	
}
