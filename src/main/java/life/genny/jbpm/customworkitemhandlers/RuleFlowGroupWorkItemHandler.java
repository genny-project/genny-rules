package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.runtime.StatefulKnowledgeSession;

import life.genny.models.GennyToken;
import life.genny.rules.QRules;
import life.genny.rules.RulesLoader;
import life.genny.utils.OutputParam;

public class RuleFlowGroupWorkItemHandler implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	
	KieSession kieSession;
	RuntimeEngine runtimeEngine;

	
	public RuleFlowGroupWorkItemHandler(KieSession kieSession) {
		this.kieSession = kieSession;
	}
	
	public RuleFlowGroupWorkItemHandler(KieSession kieSession,RuntimeEngine rteng) {
		this.kieSession = kieSession;
		this.runtimeEngine = rteng;
	}
	
 

  public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

	    
	/* resultMap is used to map the result Value to the output parameters */
	final Map<String,Object> resultMap = new HashMap<String,Object>();
	
	/* items used to save the extracted input parameters from the custom task  */
	Map<String,Object> items = workItem.getParameters();
	
    GennyToken serviceToken = (GennyToken) items.get("serviceToken");
    
    String ruleFlowGroup= (String) items.get("ruleFlowGroup");

    if (serviceToken == null) {
    	log.error("Must supply serviceToken!");
    } else  if ((!"PER_SERVICE".equals(serviceToken.getCode()))) {
    	log.error("Must supply an actual serviceToken not a normal token! check PER_SERVICE is the code (and not serviceToken");
    } else {
    
    //	log.info("serviceToken = "+serviceToken.getCode());
    //	log.info("Running rule flow group "+ruleFlowGroup);
    	System.out.println("Running rule flow group "+ruleFlowGroup);
    	 
    //	System.out.println("ProcessInstanceId = "+workItem.getProcessInstanceId());
		KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();
		// ksconf.setOption(TimedRuleExecutionOption.YES);

		KieSession newKieSession = null;
		
		OutputParam output =  new OutputParam();
		
		if (this.runtimeEngine!=null) {
			
			newKieSession = (StatefulKnowledgeSession)this.runtimeEngine.getKieSession();
						
			/* Inserting all the parameters in the working memory ad a facts */
			for(String key : items.keySet()) {
				newKieSession.insert(items.get(key));
				if (items.get(key) instanceof GennyToken) {
					GennyToken gToken = (GennyToken)items.get(key);
					if (!gToken.getCode().equals("PER_SERVICE")) {
						/* Generate a QRules */
						log.info("Adding rules to facts");
					    QRules rules = new QRules(serviceToken, gToken); 
					    newKieSession.insert(rules);
					}
				}
			}
			
			/* INserting facts to save the output result*/
			newKieSession.insert(output);
			
			

			
			/* Setting focus to rule-flow group */ 
	    	newKieSession.getAgenda().getAgendaGroup( ruleFlowGroup ).setFocus();
	    	
	    	newKieSession.fireAllRules();	
	    	
	    	/* saving result from rule-task in map*/
	    	resultMap.put("output", output.getResult());
	    	
	    	// don't dispose

		} else {
			
			KieBase kieBase = RulesLoader.getKieBaseCache().get(serviceToken.getRealm());
			newKieSession = (StatefulKnowledgeSession)kieBase.newKieSession(ksconf, null);
			
			/* Inserting all the parameters in the working memory ad a facts */
			for(String key : items.keySet()) {
				newKieSession.insert(items.get(key));
				if (items.get(key) instanceof GennyToken) {
					GennyToken gToken = (GennyToken)items.get(key);
					if (!gToken.getCode().equals("PER_SERVICE")) {
						/* Generate a QRules */
						log.info("Adding rules to facts");
					    QRules rules = new QRules(serviceToken, gToken); 
					    newKieSession.insert(rules);
					}
				}

			}
			
			/* INserting facts to save the output result*/
			newKieSession.insert(output);
			
			/* Setting focus to rule-flow group */
	    	newKieSession.getAgenda().getAgendaGroup( ruleFlowGroup ).setFocus();
	    	
	    	newKieSession.fireAllRules();
	    	
	    	/* saving result from rule-task in map*/
	    	resultMap.put("output", output.getResult());
	    	newKieSession.dispose();
		}    	
    }
     
    // notify manager that work item has been completed
    manager.completeWorkItem(workItem.getId(), resultMap);

  }

  public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    // Do nothing, notifications cannot be aborted
  }

 
 
}