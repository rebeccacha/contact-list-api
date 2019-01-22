package dbaccess;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
// import java.sql.Date; // identify with java.sql in code because eclipse still infers java.util.Date?
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import models.Address;
import models.Contact;

/**
 * This class maintains Contact data in a MySQL database.  This includes CRUD operations and searching/listing all contacts.
 * 
 * Each operation attempts to create a new database connection and closes this connection before returning.
 * 
 * @author Rebecca Chandler
 *
 */
public class ContactDAO {
	private String jdbcURL;
	private String jdbcUsername;
	private String jdbcPassword;
	private Connection jdbcConnection;
	
	/**
	 * Conversion used by date parser
	 */
	private static int MILLISEC_PER_DAY = 86400000;
	
	/**
	 * Initialize a ContactDAO with credentials necessary to connect to the database.
	 * @param jdbcURL
	 * @param jdbcUsername
	 * @param jdbcPassword
	 */
	public ContactDAO(String jdbcURL, String jdbcUsername, String jdbcPassword) {
		this.jdbcURL = jdbcURL;
		this.jdbcUsername = jdbcUsername;
		this.jdbcPassword = jdbcPassword;
	}
	
	/**
	 * Attempts to connect to the MySQL database.
	 * @throws SQLException
	 */
	protected void connect() throws SQLException {
		if(jdbcConnection == null || jdbcConnection.isClosed()) {
			try {
				Class.forName("com.mysql.cj.jdbc.Driver");
			} catch (ClassNotFoundException e) {
				throw new SQLException(e);
			}
			
			Properties properties = new Properties();
			properties.setProperty("user", jdbcUsername);
			properties.setProperty("password", jdbcPassword);
			properties.setProperty("autoReconnect", "true");
			
			jdbcConnection = DriverManager.getConnection(jdbcURL, properties);
		}
	}
	
	/**
	 * Closes the connection to the database, if it is active.
	 * @throws SQLException
	 */
	protected void disconnect() throws SQLException {
		if(jdbcConnection != null && !jdbcConnection.isClosed()) {
			jdbcConnection.close();
		}
	}
	
	/**
	 * Inserts a new contact into the database.
	 * 
	 * Null fields will result in null values in the database.
	 * If the Conact's Address is null, an Address object with empty fields is created.
	 * 
	 * @param contact - the contact to insert into the database
	 * @return true on success
	 * @throws SQLException
	 */
	public boolean insertContact(Contact contact) throws SQLException {
		String sql1 = "INSERT INTO address(line1, line2, city, state, zip, country) " + 
				"VALUES(?, ?, ?, ?, ?, ?)";
		String sql2 = "INSERT INTO contact(name, company, profile_img, email, birthdate, phone_work, phone_personal, address_id) " + 
				"VALUES (?, ?, ?, ?, ?, ?, ?, LAST_INSERT_ID())";
		// LAST_INSERT_ID() refers to last auto-increment PK inserted using this connection
			
		connect();
		PreparedStatement statement;
		
		Address address = contact.getAddress();
		if (address == null) {
			address = new Address(0, "", "", "", "", "", "", "") ;
			contact.setAddress(address);
		}

		statement = jdbcConnection.prepareStatement(sql1);
		
		statement.setString(1, address.getLine1());
		statement.setString(2, address.getLine2());
		statement.setString(3, address.getCity());
		statement.setString(4, address.getState());
		statement.setString(5, address.getZip());
		statement.setString(6, address.getCountry());
		
		int result1 = statement.executeUpdate();
		statement = jdbcConnection.prepareStatement(sql2);

		statement.setString(1, contact.getName());
		statement.setString(2, contact.getCompany());

		if(contact.getProfileImage() != null) {
			statement.setBlob(3, new ByteArrayInputStream(contact.getProfileImage()));
		} else {
			statement.setNull(3, java.sql.Types.BLOB);
		}

		statement.setString(4, contact.getEmail());
		
		if(contact.getBirthdate() != null) {
			statement.setDate(5, new java.sql.Date(contact.getBirthdate().toEpochDay()*MILLISEC_PER_DAY));
		} else {
			statement.setNull(5, java.sql.Types.DATE);
		}
		
		statement.setString(6, contact.getWorkPhone());
		statement.setString(7, contact.getPersonalPhone());
	
		int result2 = statement.executeUpdate();
		
		statement.close();		
		disconnect();
		
		return (result1 > 0)&&(result2 > 0);
	}
	
	/**
	 * List all contacts in the database.
	 * 
	 * @return a List<Contact> containing all contacts
	 * @throws SQLException
	 */
	public List<Contact> listAllContacts() throws SQLException {
		String whereClause = "WHERE c.address_id = a.id ";
		return searchAll(whereClause);
	}
	
	/**
	 * Search for contacts with name and/or email fields matching given strings.  Searches for partial matches.
	 * If both fields are given, then returned list of contacts match both criteria.
	 * 
	 * Currently case-sensitive!
	 * 
	 * @param emailPart - part of Contact's email on which to match
	 * @param phonePart - part of a Contact's name on which to match
	 * 
	 * @return a List<Contact> of Contacts matching the search criteria
	 * @throws SQLException
	 */
	public List<Contact> searchAllContacts(String emailPart, String phonePart) throws SQLException {
		String whereClause = "WHERE c.address_id = a.id ";
		if(emailPart != null) {
			whereClause = "AND email like \"%" + emailPart + "%\" ";
		}
		if(phonePart != null) {
			whereClause += "AND phone like \"%" + phonePart + "%\" ";
		}
		 
		return searchAll(whereClause);
	}
	
	/**
	 * Search for contacts with city and/or state fields matching given strings.  Searches for full match on strings.
	 * If both fields are given, then returned list of contacts match both criteria.
	 * 
	 * Note that:
	 * - if the state field of a Contact is null, it may be overlooked in a (city,state) search
	 * - state names are expected to be shorthand (e.g. IL) but there are not checks to ensure this
	 * 
	 * Currently case-sensitive!
	 * 
	 * @param city - full name of a city on which to match
	 * @param state - full name of state on which to match
	 * @return a List<Contact> of Contacts matching the search criteria
	 * @throws SQLException
	 */
	public List<Contact> searchAllContactsByLocation(String city, String state) throws SQLException {
		
		String whereClause = "WHERE c.address_id = a.id ";
		if(state != null) {
			whereClause = "AND state = " + state + " ";
		}
		if(city != null) {
			whereClause += "AND city = " + city + " ";
		}
		
		return searchAll(whereClause);
	}
	
	private List<Contact> searchAll(String whereClause) throws SQLException {
		List<Contact> listContact = new ArrayList<Contact>();
		
		String sql = "SELECT c.*, a.line1, a.line2, a.city, a.state,  a.zip, a.country "
				+ "FROM contact c, address a "
				+  whereClause;
		
		connect();
		
		Statement statement = jdbcConnection.createStatement();
		ResultSet resultSet = statement.executeQuery(sql);
		
		Contact contact = null;
		Address address = null;
		
		while (resultSet.next()) {
			int id = resultSet.getInt("id");
			String name = resultSet.getString("name");
			String company = resultSet.getString("company");
			Blob profileImgBlob = resultSet.getBlob("profile_img");
			String email = resultSet.getString("email");
			Date birthdateSQL = resultSet.getDate("birthdate");
			String workPhone = resultSet.getString("phone_work");
			String personalPhone = resultSet.getString("phone_personal");
			int addressId = resultSet.getInt("address_id");
			
			String line1 = resultSet.getString("line1");
			String line2 = resultSet.getString("line2");
			String city = resultSet.getString("city");
			String state = resultSet.getString("state");
			String zip = resultSet.getString("zip");
			String country = resultSet.getString("country");

			byte[] profileImg = parseBlob(profileImgBlob);
			LocalDate birthdate = parseDate(birthdateSQL);
			
			address = new Address(addressId, line1, line2, "", city, state, zip, country);
			contact = new Contact(id, name, company, profileImg, email,
					birthdate, workPhone, personalPhone, address);
			
			listContact.add(contact);
		}
		
		resultSet.close();
		statement.close();
		
		disconnect();
		
		return listContact;
	}
	
	/**
	 * Delete the Contact identified by an id
	 * 
	 * @param id - Contact.id attribute value.  Refers to a contact's primary key.
	 * @return true on success
	 * @throws SQLException
	 */
	public boolean deleteContact(int id) throws SQLException {
		String sql = "DELETE c, a "
				+ "FROM contact c, address a "
				+ "WHERE c.id = ? AND c.address_id = a.id ";
		
		connect();
		
		PreparedStatement statement = jdbcConnection.prepareStatement(sql);
		statement.setInt(1, id);
		
		boolean rowDeleted = statement.executeUpdate() > 0;
		statement.close();
		disconnect();
		return rowDeleted;
	}
	
	/**
	 * Update the attribute values of a given Contact identified by Contact.id
	 * 
	 * @param contact - the Contact whose updates will be pushed to the database.  Identified by Contact.id.
	 * @return true on success
	 * @throws SQLException
	 */
	public boolean updateContact(Contact contact) throws SQLException {
		String sql = "UPDATE contact c, address a " + 
				"SET c.name = ?, c.company = ?, c.profile_img = ?, c.email = ?, " + 
				"c.birthdate = ?, c.phone_work = ?, c.phone_personal = ?, " +
				"a.line1 = ?, a.line2 = ?, a.city = ?, a.state = ?, a.zip = ?, a.country = ? " + 
				"WHERE c.id = ? AND a.id = c.address_id";
		
		connect();
		PreparedStatement statement = jdbcConnection.prepareStatement(sql);
		
		Address address = contact.getAddress();
		if (address == null) {
			address = new Address(0, "", "", "", "", "", "", "");
			contact.setAddress(address);
		}

		statement.setString(1, contact.getName());
		statement.setString(2,  contact.getCompany());
		
		if(contact.getProfileImage() != null) {
			statement.setBlob(3, new ByteArrayInputStream(contact.getProfileImage()));
		} else {
			statement.setNull(3, java.sql.Types.BLOB);
		}

		statement.setString(4, contact.getEmail());
		
		if(contact.getBirthdate() != null) {
			statement.setDate(5, new java.sql.Date(contact.getBirthdate().toEpochDay()*MILLISEC_PER_DAY));
		} else {
			statement.setNull(5, java.sql.Types.DATE);
		}
		statement.setString(6, contact.getWorkPhone());
		statement.setString(7, contact.getPersonalPhone());

		
		statement.setString(8, address.getLine1());
		statement.setString(9, address.getLine2());
		statement.setString(10, address.getCity());
		statement.setString(11, address.getState());
		statement.setString(12, address.getZip());
		statement.setString(13, address.getCountry());
		
		statement.setInt(14, contact.getId());
		
		boolean rowUpdated = statement.executeUpdate() > 0;
		statement.close();
		disconnect();
		return rowUpdated;
	}
	
	/**
	 * Retrieve a Contact referred to by its Id
	 * @param id -  Contact.id attribute value.  Refers to a contact's primary key.
	 * @return a Contact record
	 * @throws SQLException
	 */
	public Contact getContact(int id) throws SQLException {
		Contact contact = null;
		Address address = null;
		String sql = "SELECT c.*, a.line1, a.line2, a.city, a.state,  a.zip, a.country "
				+ "FROM contact c, address a "
				+ "WHERE c.id = ? and c.address_id = a.id";
		
		connect();
		
		PreparedStatement statement = jdbcConnection.prepareStatement(sql);
		statement.setInt(1, id);
		
		ResultSet resultSet = statement.executeQuery();
		
		if (resultSet.next()) {
			// int contact_id = resultSet.getInt("id");
			String name = resultSet.getString("name");
			String company = resultSet.getString("company");
			Blob profileImg = resultSet.getBlob("profile_img");
			String email = resultSet.getString("email");
			Date birthdateSQL = resultSet.getDate("birthdate");
			String workPhone = resultSet.getString("phone_work");
			String personalPhone = resultSet.getString("phone_personal");
			int addressId = resultSet.getInt("address_id");
			String line1 = resultSet.getString("line1");
			String line2 = resultSet.getString("line2");
			String city = resultSet.getString("city");
			String state = resultSet.getString("state");
			String zip = resultSet.getString("zip");
			String country = resultSet.getString("country");
			
			byte[] img = parseBlob(profileImg);
			LocalDate birthdate = parseDate(birthdateSQL);
			
			address = new Address(addressId, line1, line2, "", city, state, zip, country);
			contact = new Contact(id, name, company, img, email,
					birthdate, workPhone, personalPhone, address);
		}
		
		resultSet.close();
		statement.close();
		disconnect();
		return contact;
	}
	
	/**
	 * Helper function parses a date if not null.
	 * @param date - java.sql.Date;
	 * @return - LocalDate if date not null, else returns null
	 */
	private static LocalDate parseDate(Date date) {
		LocalDate parsed = null;
		if(date != null) {
			parsed = Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
		}
		return parsed;
		
	}
	
	/**
	 * Helper function parses blob if not null
	 * @param blob - sql blob
	 * @return - byte[] if Blob was not null, else returns null
	 * @throws SQLException
	 */
	private static byte[] parseBlob(Blob blob) throws SQLException {
		byte[] parsed = null;
		if(blob != null) {
			parsed = blob.getBytes(1, (int) blob.length());
		}
		return parsed;
	}
}