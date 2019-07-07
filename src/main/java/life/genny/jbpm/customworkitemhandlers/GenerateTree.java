package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.Logger;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import life.genny.models.GennyToken;

public class GenerateTree implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

  public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
    // extract parameters
    GennyToken serviceToken = (GennyToken) workItem.getParameter("serviceToken");

    if (serviceToken == null) {
    	log.error("Must supply serviceToken!");
    } else  if (!"PER_SERVICE".equals(serviceToken.getCode())) {
    	log.error("Must supply an actual serviceToken not a normal token!");
    } else {
    
    	log.info("serviceToken = "+serviceToken.getCode());
    	log.info("Generating Tree");
    }
     
    // notify manager that work item has been completed
    manager.completeWorkItem(workItem.getId(), null);


  }

  public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    // Do nothing, notifications cannot be aborted
  }

}