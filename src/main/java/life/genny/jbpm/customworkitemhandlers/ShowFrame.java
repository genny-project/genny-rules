package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.Logger;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import life.genny.models.GennyToken;

public class ShowFrame implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

  public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
    // extract parameters
    GennyToken userToken = (GennyToken) workItem.getParameter("userToken");
    String rootFrameCode = (String) workItem.getParameter("rootFrameCode");
    
    if (userToken == null) {
    	log.error("Must supply userToken!");
    
    } else {
    
    	log.info("userToken = "+userToken.getCode());
    	
    	if (rootFrameCode == null) {
    		log.error("Must supply a root Frame Code!");
    	} else {
    		log.info("root Frame Code = "+rootFrameCode);
    	}
    	
    }
     
    // notify manager that work item has been completed
    manager.completeWorkItem(workItem.getId(), null);


  }

  public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    // Do nothing, notifications cannot be aborted
  }

}