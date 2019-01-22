package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is a data model for an Address.  It may be serialized/deserialized to JSON using the Jackson library.
 * 
 * Line3 attribute is currently unused and was intended for use for Addresses outside the United States.
 * 
 * Known ERROR: JsonIgnoreProperties annotation not working.
 *     Possibly be due to conflicting Jackson libraries.
 *     (Jersey Maven dependency creates a 2nd reference to the Jackson libraries.)
 * 
 * @author Rebecca Chandler
 *
 */
@JsonIgnoreProperties({ "line3, id"})
public class Address {
	int id;
	
	@JsonProperty
	protected String line1;
	
	@JsonProperty
	protected String line2;
	
	protected String line3;
	
	@JsonProperty
	protected String city;
	
	@JsonProperty
	protected String state;
	
	@JsonProperty
	protected String zip;
	
	@JsonProperty
	protected String country;
	
	public Address() { }
	
	public Address(int id) {
		this.id = id;
	}
	
	public Address(int id, String line1, String line2, String line3,
			String city, String state, String zip, String country) {
		this.id = id;
		this.line1 = line1;
		this.line2 = line2;
		this.line3 = line3;
		this.city = city;
		this.state = state;
		this.zip = zip;
		this.country = country;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getLine1() {
		return line1;
	}

	public void setLine1(String line1) {
		this.line1 = line1;
	}

	public String getLine2() {
		return line2;
	}

	public void setLine2(String line2) {
		this.line2 = line2;
	}

	public String getLine3() {
		return line3;
	}

	public void setLine3(String line3) {
		this.line3 = line3;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}
}
