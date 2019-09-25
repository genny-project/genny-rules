package life.genny.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import life.genny.model.OutputParamTreeSet;
import life.genny.utils.OutputParam;

public class OutputParamTest {
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
	
}
