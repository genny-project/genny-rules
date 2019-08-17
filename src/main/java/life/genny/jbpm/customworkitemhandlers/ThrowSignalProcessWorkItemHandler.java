package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.ObjectFilter;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.runtime.StatefulKnowledgeSession;

import life.genny.models.GennyToken;
import life.genny.rules.QRules;
import life.genny.rules.RulesLoader;
import life.genny.utils.OutputParam;

public class ThrowSignalProcessWorkItemHandler implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	
	KieSession kieSession;
	RuntimeEngine runtimeEngine;

	
	public ThrowSignalProcessWorkItemHandler(KieSession kieSession) {
		this.kieSession = kieSession;
	}
	
	public ThrowSignalProcessWorkItemHandler(KieSession kieSession,RuntimeEngine rteng) {
		this.kieSession = kieSession;
		this.runtimeEngine = rteng;
	}
	
 

  public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

	    
	/* resultMap is used to map the result Value to the output parameters */
	final Map<String,Object> resultMap = new HashMap<String,Object>();
	
	/* items used to save the extracted input parameters from the custom task  */
	Map<String,Object> items = workItem.getParameters();
	
    GennyToken serviceToken = (GennyToken) items.get("serviceToken");
    Object payload = (Object) items.get("payload");
    String signalCode = (String) items.get("signalCode");
    
    Long processId= (Long) items.get("processId");

    	System.out.println("Sending signal Code  "+signalCode+ " to processId "+processId);
    	 
		KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();

		KieSession newKieSession = null;
		
		OutputParam output =  new OutputParam();
		
		if (this.runtimeEngine!=null) {
			
			newKieSession = (StatefulKnowledgeSession)this.runtimeEngine.getKieSession();
						
			newKieSession.signalEvent(signalCode, payload, processId);

		} else {
			
			KieBase kieBase = RulesLoader.getKieBaseCache().get(serviceToken.getRealm());
			newKieSession = (StatefulKnowledgeSession)kieBase.newKieSession(ksconf, null);
			
			newKieSession.signalEvent(signalCode, payload, processId);

	    	newKieSession.dispose();
 	
    }
     
    // notify manager that work item has been completed
    manager.completeWorkItem(workItem.getId(), resultMap);

  }

  public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    // Do nothing, notifications cannot be aborted
  }

 
 
}