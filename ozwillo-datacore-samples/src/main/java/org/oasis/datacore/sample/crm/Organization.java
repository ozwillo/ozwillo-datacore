package org.oasis.datacore.sample.crm;
 
import org.oasis.datacore.sample.city.City;
import org.oasis.datacore.sdk.data.spring.GenericEntity;
import org.springframework.data.mongodb.core.mapping.Document;

// TODO GenericEntity
@Document(collection = "organizations")
public class Organization extends GenericEntity<City> {
 
	private String shortName;
	private String legalName;
	private String kind;
	private String address;
	private String zip;
	private String city; // TODO unify with City app cities
	private String country;
	private String tel;
	private String website;
	// TODO contacts : embedded using path, or another collection
	//@Field private String path;
 
	
	public String getShortName() {
		return shortName;
	}
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}
	public String getLegalName() {
		return legalName;
	}
	public void setLegalName(String legalName) {
		this.legalName = legalName;
	}
	public String getKind() {
		return kind;
	}
	public void setKind(String kind) {
		this.kind = kind;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getZip() {
		return zip;
	}
	public void setZip(String zip) {
		this.zip = zip;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getTel() {
		return tel;
	}
	public void setTel(String tel) {
		this.tel = tel;
	}
	public String getWebsite() {
		return website;
	}
	public void setWebsite(String website) {
		this.website = website;
	}
	
}
