package models;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import json.LocalDateDeserializer;
import json.LocalDateSerializer;

/**
 * This class is a data model for a Contact.  It may be serialized/deserialized to JSON using the Jackson library.
 * 
 * All its attributes are Strings and primitive data types except the Address object.
 * 
 * @author Rebecca Chandler
 *
 */
@JsonIgnoreProperties({ "profileImage"})
public class Contact {
	
	protected int id;
	
	@JsonProperty
	protected String name;
	
	@JsonProperty
	protected String company;
	
	protected byte[] profileImage;
	
	@JsonProperty
	protected String email;
	
	// @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy") 
	@JsonProperty
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
	protected LocalDate birthdate;
	
	@JsonProperty
	protected String workPhone;
	
	@JsonProperty
	protected String personalPhone;
	
	@JsonProperty
	protected Address address;
	
	public Contact() { }
	
	public Contact(int id) {
		this.id = id;
	}
	
	public Contact(int id, String name, String company, byte[] profileImage, 
			String email, LocalDate birthdate, String workPhone, String personalPhone,
			Address address) {
		this.id = id;
		this.name = name;
		this.company = company;
		this.profileImage = profileImage;
		this.email = email;
		this.birthdate = birthdate;
		this.workPhone = workPhone;
		this.personalPhone = personalPhone;
		this.address = address;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public byte[] getProfileImage() {
		return profileImage;
	}

	public void setProfileImage(byte[] profileImage) {
		this.profileImage = profileImage;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LocalDate getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(LocalDate birthdate) {
		this.birthdate = birthdate;
	}

	public String getWorkPhone() {
		return workPhone;
	}

	public void setWorkPhone(String workPhone) {
		this.workPhone = workPhone;
	}

	public String getPersonalPhone() {
		return personalPhone;
	}

	public void setPersonalPhone(String personalPhone) {
		this.personalPhone = personalPhone;
	}
	
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}
}
