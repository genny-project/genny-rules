package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;

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
    GennyToken serviceToken = (GennyToken) workItem.getParameter("serviceToken");
    String ruleFlowGroup= (String) workItem.getParameter("ruleFlowGroup");

    if (serviceToken == null) {
    	log.error("Must supply serviceToken!");
    } else  if (!"PER_SERVICE".equals(serviceToken.getCode())) {
    	log.error("Must supply an actual serviceToken not a normal token!");
    } else {
    
    //	log.info("serviceToken = "+serviceToken.getCode());
    //	log.info("Running rule flow group "+ruleFlowGroup);
    	System.out.println("Running rule flow group "+ruleFlowGroup);
    	
    	System.out.println("ProcessInstanceId = "+workItem.getProcessInstanceId());
		KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();
		// ksconf.setOption(TimedRuleExecutionOption.YES);

		KieSession newKieSession = null;
		if (this.runtimeEngine!=null) {
			newKieSession = this.runtimeEngine.getKieSession();
	    	newKieSession.insert(serviceToken);
	    	newKieSession.getAgenda().getAgendaGroup( ruleFlowGroup ).setFocus();
	    	newKieSession.fireAllRules();
	    	// don't dispose

		} else {
			KieBase kieBase = RulesLoader.getKieBaseCache().get(serviceToken.getRealm());
			newKieSession = (StatefulKnowledgeSession)kieBase.newKieSession(ksconf, null);
	    	newKieSession.insert(serviceToken);
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