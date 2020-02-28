package life.genny.utils;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.apache.logging.log4j.Logger;

public class DateUtils {
	
	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	/*
	 * Returns UTC DateTime 
	 */
	public static String getCurrentUTCDateTime() {
		
		ZonedDateTime now = ZonedDateTime.now( ZoneOffset.UTC );
		String dateTimeString = now.toString();
		log.info("UTC datetime is ::" + dateTimeString);

		return dateTimeString;
	}
	
	/*
	 * Returns Local systems DateTime
	 */
	public static String getZonedCurrentLocalDateTime() {

		LocalDateTime ldt = LocalDateTime.now();
		ZonedDateTime zdt = ldt.atZone(ZoneOffset.systemDefault());
		String iso8601DateString = ldt.toString(); // zdt.toString(); MUST USE UMT!!!!

		log.info("datetime ::" + iso8601DateString);

		return iso8601DateString;

	}

}
