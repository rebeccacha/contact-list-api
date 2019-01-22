package resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import dbaccess.ContactDAO;
import models.Address;
import models.Contact;

import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
// import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * RESTful web resource for Contact data.
 * Includes CRUD operations and search tools.
 * 
 * @author Rebecca Chandler
 *
 */
@Path("/contact")
public class ContactResource {
	
	/**
	 * Expected format of birthdate field
	 */
	private static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	/**
	 * Maximum size allowed in a profile image upload
	 */
	private static int MAX_IMG_SIZE = 64000;
	
	/**
	 * Retrieve a list of all Contacts.  No parameters returns the entire list.  Use parameters to search.  
	 * 
	 * Search parameter combinations:
	 * - Email and Name may both be searched on.  Results are entries that match on both criteria.
	 * - City and State may both be searched on.  Results are entries that match on both criteria.
	 * 
	 * @param city - full name of a city on which to match contacts' addresses
	 * @param state - full name of a state on which to match contacts' addresses
	 * @param email - partial email of contact on which to match
	 * @param phone - partial phone number of contact on which to match contacts' work and personal numbers
	 * @param context - servlet context used to retrieve database login credentials
	 * @return - a List of Contacts matching the search criteria
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Contact> getContacts( @QueryParam("city") String city, 
			@QueryParam("state") String state, @QueryParam("email") String email,
			@QueryParam("phone") String phone,  @Context ServletContext context) {
		
		ContactDAO contactDAO = new ContactDAO(context.getInitParameter("jdbcURL"),
        		context.getInitParameter("jdbcUsername"), context.getInitParameter("jdbcPassword"));
        
		List<Contact> contacts = null;
		try {
			contacts = contactDAO.searchAllContacts(email, phone);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return contacts; 
	}
	
	/**
	 * Retrieve a single contact record identified by its Id.
	 * 
	 * @param id - Number uniquely identifying a contact. Contact.id field
	 * @param context - servlet context used to retrieve database login credentials
	 * @return Contact identified by the given Id.  Null if not found.
	 */
	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Contact getContact(@PathParam("id") String id, @Context ServletContext context) {
        ContactDAO contactDAO = new ContactDAO(context.getInitParameter("jdbcURL"),
        		context.getInitParameter("jdbcUsername"), context.getInitParameter("jdbcPassword"));
        
		Contact contact = null;
		try {
			contact = contactDAO.getContact(Integer.parseInt(id));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (contact == null) {
			throw new NotFoundException();
		}
		return contact; 
	}
	
	/**
	 * Retrieves the profile image for a given Contact.
	 * 
	 * Note the returned file is only presumed to be a JPG.
	 * Although this resource produces an "image/jpg",
	 * there may not be checks when the file was uploaded.
	 * 
	 * @param id - Number uniquely identifying a contact. Contact.id field
	 * @param context - servlet context used to retrieve database login credentials
	 * @return - writes the image file to response's output stream, 404 error if no file was saved for this contact
	 */
	@GET
	@Path("{id}/profile_img")
	@Produces("image/jpg")
	public StreamingOutput getImage(@PathParam("id") String id, @Context ServletContext context) {
        ContactDAO contactDAO = new ContactDAO(context.getInitParameter("jdbcURL"),
        		context.getInitParameter("jdbcUsername"), context.getInitParameter("jdbcPassword"));
		try {
			final Contact contact = contactDAO.getContact(Integer.parseInt(id));
			return new StreamingOutput() {

				// @Override
				public void write(OutputStream os) throws IOException, WebApplicationException {
					
					if(contact.getProfileImage() != null) {
						IOUtils.copy(new ByteArrayInputStream(contact.getProfileImage()),os);
					} else {
						throw new WebApplicationException(404);
					}
				}
				
			};
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Deletes the Contact entry identified by the given Id.
	 * 
	 * @param id - Number uniquely identifying a contact. Contact.id field
	 * @param context - servlet context used to retrieve database login credentials
	 * @return mirrors back the given Id on success, returns -1 on failure
	 */
	@DELETE
	@Path("{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public int removeContact(@PathParam("id") int id, @Context ServletContext context) {
		ContactDAO contactDAO = new ContactDAO(context.getInitParameter("jdbcURL"),
        		context.getInitParameter("jdbcUsername"), context.getInitParameter("jdbcPassword"));
        
		boolean deleted = false;
		try {
			deleted = contactDAO.deleteContact(id);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return deleted?id:-1; 
	}
	
	/**
	 * Inserts a new contact record into the data store.
	 * 
	 * Data is accepted as a multipart form.
	 * All fields are primitive/string values except a field "file" which contains a profile image < 65 kB.
	 * Any field may be left null/blank.
	 * 
	 * @param formDataBodyPart - the "file" field which contains a profile image < 65 kB
	 * @param name - Contact's name
	 * @param company - Contact's company
	 * @param email - Contact's email
	 * @param workPhone - Contact's work phone
	 * @param personalPhone - Contact's personal phone
	 * @param birthdate - Contact's birthdate formatted yyyy-mm-dd
	 * @param line1 - first line of Conact's address
	 * @param line2 - second line of Conact's address
	 * @param city - city of Conact's address
	 * @param state - state of Conact's address (intended to be abbreviated e.g. WI)
	 * @param zip - zipcode of Conact's address
	 * @param country  - country of Conact's address
	 * @param context - servlet context used to retrieve database login credentials
	 * @return a JSON describing the newly created contact.  The Contact's id attribute will be invalid (-1)
	 */
	@POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Contact newContact(@FormDataParam("file") final FormDataBodyPart formDataBodyPart, @FormDataParam("name") String name,
    		@FormDataParam("company") String company, @FormDataParam("email") String email,
    		@FormDataParam("workPhone") String workPhone, @FormDataParam("personalPhone") String personalPhone,
    		@FormDataParam("birthdate") String birthdate, @FormDataParam("line1") String line1, 
    		@FormDataParam("line2") String line2, @FormDataParam("city") String city, 
    		@FormDataParam("state") String state, @FormDataParam("zip") String zip, 
    		@FormDataParam("country") String country, @Context ServletContext context) {
		
		byte[] imgFile = uploadFile(formDataBodyPart);
		LocalDate date = parseDate(birthdate);

		Address address = new Address(-1, line1, line2, "", city, state, zip, country);
        Contact contact = new Contact(-1, name, company, imgFile, email, date, workPhone, personalPhone, address);
        
        ContactDAO contactDAO = new ContactDAO(context.getInitParameter("jdbcURL"),
        		context.getInitParameter("jdbcUsername"), context.getInitParameter("jdbcPassword"));
        
        try {
			contactDAO.insertContact(contact);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		return contact; 
	}
	
	/**
	 * Update the record of an existing Contact identified by its unique Id.
	 * 
	 * Data is accepted as a multipart form.
	 * All fields are primitive/string values except a field "file" which contains a profile image < 65 kB.
	 * Any field may be left null/blank.
	 * All fields in the Contact's record are replaced.
	 * 
	 * @param formDataBodyPart - the "file" field which contains a profile image < 65 kB
	 * @param name - Contact's name
	 * @param company - Contact's company
	 * @param email - Contact's email
	 * @param workPhone - Contact's work phone
	 * @param personalPhone - Contact's personal phone
	 * @param birthdate - Contact's birthdate formatted yyyy-mm-dd
	 * @param line1 - first line of Conact's address
	 * @param line2 - second line of Conact's address
	 * @param city - city of Conact's address
	 * @param state - state of Conact's address (intended to be abbreviated e.g. WI)
	 * @param zip - zipcode of Conact's address
	 * @param country  - country of Conact's address
	 * @param context - servlet context used to retrieve database login credentials
	 * @return a JSON describing the updated contact record
	 */
	@PUT
	@Path("{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Contact updateContact(@FormDataParam("file") final FormDataBodyPart formDataBodyPart, @PathParam("id") int id,
    		@FormDataParam("name") String name, @FormDataParam("company") String company, @FormDataParam("email") String email,
    		@FormDataParam("workPhone") String workPhone, @FormDataParam("personalPhone") String personalPhone,
    		@FormDataParam("birthdate") String birthdate, @FormDataParam("line1") String line1, 
    		@FormDataParam("line2") String line2, @FormDataParam("city") String city, 
    		@FormDataParam("state") String state, @FormDataParam("zip") String zip, 
    		@FormDataParam("country") String country, @Context ServletContext context) {
		
		byte[] imgFile = uploadFile(formDataBodyPart);
		LocalDate date = parseDate(birthdate);
			
        Address address = new Address(-1 , line1, line2, "", city, state, zip, country);
        Contact contact = new Contact(id, name, company, imgFile, email, date, workPhone, personalPhone, address);
        
        ContactDAO contactDAO = new ContactDAO(context.getInitParameter("jdbcURL"),
        		context.getInitParameter("jdbcUsername"), context.getInitParameter("jdbcPassword"));
        
        boolean updated = false;
        try {
			updated = contactDAO.updateContact(contact);
		} catch (SQLException e) {
			e.printStackTrace();
		}
        
		return updated?contact:null; 
	}
	
	/**
	 * Helper function reads a file from a FormDataBodyPart into a byte array.
	 * Reads only up to 65 kB.
	 * 
	 * @param formDataBodyPart - the formDataBodyPart, usually expected in a form's "file" field
	 * @return a byte array containing the file if file<65kB,
	 * 			null if formDataBodyPart is null OR
	 * 				 if file>=65kB
	 * 
	 */
	private static byte[] uploadFile(FormDataBodyPart formDataBodyPart) { 
		byte[] img = null;
		// do file type checks?
		// formDataBodyPart.getFormDataContentDisposition().getSize() always -1.  Instead check during saving
		if(formDataBodyPart != null) {
			img = new byte[MAX_IMG_SIZE]; // max 64 kB
			InputStream is = formDataBodyPart.getEntityAs(InputStream.class);
			
			int totalBytesRead = 0;
			int bytesRead = 0;
			do {
				try {
					bytesRead = is.read(img, totalBytesRead, img.length-totalBytesRead);
				} catch (IOException e) {
					e.printStackTrace();
					bytesRead = 0;
					img = null;
				}
				totalBytesRead += bytesRead;
			} while(bytesRead > 0);
			
			if(totalBytesRead == MAX_IMG_SIZE) {
				img = null;
				System.out.println("Img upload greater than 64 kB. Upload request ignored.  Null value for img.");
			}
		}
		return img;
	}
	
	/**
	 * Helper function parses a date string to a LocalDate object
	 * 
	 * @param birthdate - string formatted as DATE_TIME_FORMATTER (expected yyyy-MM-dd)
	 * @return LocalDate object
	 */
	private static LocalDate parseDate(String birthdate) {
		LocalDate date = null;
		if(birthdate != null) {
			date = LocalDate.parse(birthdate, DATE_TIME_FORMATTER);
		}
		return date;
	}

}
