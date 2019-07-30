package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
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
import life.genny.rules.RulesLoader;

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
    // extract parameters
	  
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
		
		if (this.runtimeEngine!=null) {
			
			newKieSession = this.runtimeEngine.getKieSession();
			
			for(String key : items.keySet()) {
				newKieSession.insert(items.get(key));
			}
	    	
	    	newKieSession.getAgenda().getAgendaGroup( ruleFlowGroup ).setFocus();
	    	newKieSession.fireAllRules();
	    	// don't dispose

		} else {
			
			KieBase kieBase = RulesLoader.getKieBaseCache().get(serviceToken.getRealm());
			newKieSession = (StatefulKnowledgeSession)kieBase.newKieSession(ksconf, null);
			
			for(String key : items.keySet()) {
				newKieSession.insert(items.get(key));
			}
			
			/*ExecutionResults results = kieSession.execute(CommandFactory.newBatchExecution(cmds));

			results.getValue("msg"); // returns the inserted fact Msg
			QRules rules  = (QRules) results.getValue("qRules"); // returns the inserted fact QRules
			System.out.println(results.getValue("msg"));
			System.out.println(rules);
*/
			
	    	/*newKieSession.insert(serviceToken);*/
	    	newKieSession.getAgenda().getAgendaGroup( ruleFlowGroup ).setFocus();
	    	newKieSession.fireAllRules();
	    	newKieSession.dispose();
	    	

		}
	
    	
    }
     
    // notify manager that work item has been completed
    manager.completeWorkItem(workItem.getId(), null);


  }

  public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    // Do nothing, notifications cannot be aborted
  }

}