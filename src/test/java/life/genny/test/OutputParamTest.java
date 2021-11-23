package life.genny.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;
import org.javamoney.moneta.Money;
import org.junit.Test;

import com.google.gson.Gson;

import life.genny.model.OutputParamTreeSet;
import life.genny.qwanda.DateTimeDeserializer;
import life.genny.qwanda.MoneyDeserializer;
import life.genny.qwanda.datatype.LocalDateConverter;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwanda.utils.OutputParam;

public class OutputParamTest {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	
	@Test
	public void outputParamEqualsTest()
	{
		OutputParam o1 = new OutputParam();
		o1.setFormCode("FRM_APP", "FRM_ROOT");
		
		OutputParam o2 = new OutputParam();
		o2.setFormCode("FRM_APP2", "FRM_ROOT");
		
		
		// 
		assertNotEquals(o1,o2);
		
	}
	
	@Test
	public void outputParamTreeTest()
	{
		OutputParam o1 = new OutputParam();
		o1.setFormCode("FRM_APP", "FRM_ROOT");
		
		
		OutputParam o2 = new OutputParam();
		o2.setFormCode("FRM_APP2", "FRM_ROOT");
		
		OutputParamTreeSet set = new OutputParamTreeSet();
		set.add(o1);
		set.add(o1);
		set.add(o2);
		// 
		
		
	}
	
	@Test
	public void outputParamJsonTest()
	{
		OutputParam o = new OutputParam();
		o.setFormCode("FRM_APP", "FRM_ROOT");
		o.setTaskId(2L);
		Map<String,String> map = new ConcurrentHashMap<String,String>();
		map.put("PRI_NAME", "PER_USER1");
		map.put("PRI_LASTNAME", "PER_USER1");
		o.setAttributeTargetCodeMap(map);
		
		log.info(o);
		String json = JsonUtils.toJson(o);
		log.info(json);

		
	}
	
}
