package life.genny.rules;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.Logger;

public class JBPM {
	
	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


	  private static final JBPM INSTANCE = new JBPM();

	  public static JBPM getInstance() {

	    return INSTANCE;

	  }

	  public void sayHello(String name) {

	    log.info("Hello " + name);

	  }

	}