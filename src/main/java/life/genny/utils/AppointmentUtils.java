package life.genny.utils;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.Logger;

public class AppointmentUtils {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	BaseEntityUtils beUtils = null;

	public AppointmentUtils(BaseEntityUtils beUtils) {
		this.beUtils = beUtils;
	}
	
	

}
