package life.genny.test;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import life.genny.bootxport.bootx.GoogleImportService;
import life.genny.bootxport.bootx.Realm;
import life.genny.bootxport.bootx.XlsxImport;
import life.genny.bootxport.bootx.XlsxImportOnline;

public class ImportTest {
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	//@Test
	public void importTest()
	{
		 Map<String,String> fieldMapping = new HashMap<String,String>();
		 fieldMapping.put("Batch".toLowerCase(), "PRI_BATCH_NO");
		 fieldMapping.put("State".toLowerCase(), "PRI_IMPORT_STATE");
		 fieldMapping.put("Student ID".toLowerCase(), "PRI_STUDENT_ID");
		 fieldMapping.put("Disp".toLowerCase(), "PRI_IMPORT_DISP");
		 fieldMapping.put("First Name".toLowerCase(), "PRI_FIRSTNAME");
		 fieldMapping.put("Last Name".toLowerCase(), "PRI_LASTNAME");
		 fieldMapping.put("PHONE".toLowerCase(), "PRI_IMPORT_PHONE");
		 fieldMapping.put("EMAIL".toLowerCase(), "PRI_EMAIL");
		 fieldMapping.put("TARGET START DATE".toLowerCase(), "PRI_TARGET_START_DATE");
		 fieldMapping.put("ADDRESS".toLowerCase(), "PRI_IMPORT_ADDRESS");
		 fieldMapping.put("SUBURB".toLowerCase(), "PRI_IMPORT_SUBURB");
		 fieldMapping.put("Postcode".toLowerCase(), "PRI_IMPORT_POSTCODE");

		 
		Integer count = importGoogleDoc("1eDovA5TB24lUBc8ddohxAuFzMdLZ-_xkJdCf7Bs_sak",fieldMapping);
		

	}
	
	public Integer importGoogleDoc(final String id, Map<String,String> fieldMapping)
	{		
		log.info("Importing "+id);
		Integer count = 0;
		   try {
			   GoogleImportService gs = GoogleImportService.getInstance();
			    XlsxImport xlsImport = new XlsxImportOnline(gs.getService());
		//	    Realm realm = new Realm(xlsImport,id);
//			    realm.getDataUnits().stream()
//			        .forEach(data -> System.out.println(data.questions.size()));
			    Set<String> keys = new HashSet<String>();
			    for (String field : fieldMapping.keySet()) {
			    	keys.add(field);
			    }
			      Map<String, Map<String,String>> mapData = xlsImport.mappingRawToHeaderAndValuesFmt(id, "Sheet1", keys);
			      Integer rowIndex = 0;
			      for (Map<String,String> row : mapData.values()) 
			      {
			    	  String rowStr = "Row:"+rowIndex+"->";
			    	  for (String col : row.keySet()) {
			    		  String val = row.get(col.trim());
			    		  if (val!=null) {
			    			  val = val.trim();
			    		  }
			    		  String attributeCode = fieldMapping.get(col);
			    		  rowStr += attributeCode+"="+val + ",";
			    	  }
			    	  rowIndex++;
			    	  System.out.println(rowStr);
			      }
			      
			    } catch (Exception e1) {
			      return 0;
			    }

		
		return count;
	}
}
