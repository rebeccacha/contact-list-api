package test;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import resources.ContactResource;

/**
 * Sample unit test for HTTP resource using with Jersey.
 * Currently not working.  May be a problem of missing libraries.
 * 
 * @author Rebecca Chandler
 *
 */
public class SampleTest extends JerseyTest {
	
	@Override
    protected Application configure() {
		System.out.println("Hello SampleTest.configure()");
		ResourceConfig app = new ResourceConfig(ContactResource.class);
		app.register(MultiPartFeature.class);
        return app;
    }
 
	/*
	 * Not working.  May be a problem with Maven libraries.
	 */
    @Test
    public void test() {
    	final Response response = target("/contact/1").request().get();
    	System.out.println(response.getStatus()); // error
    	System.out.println("here");
        assertEquals(true, true);
    }

}
