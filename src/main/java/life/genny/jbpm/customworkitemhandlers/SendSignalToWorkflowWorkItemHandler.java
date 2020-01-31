package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
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
import life.genny.utils.SessionFacts;

public class SendSignalToWorkflowWorkItemHandler implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	RuntimeEngine runtimeEngine;
	String wClass;
	

	public <R> SendSignalToWorkflowWorkItemHandler(Class<R> workflowQueryInterface) {
		this.wClass = workflowQueryInterface.getCanonicalName();
	}

	public <R> SendSignalToWorkflowWorkItemHandler(Class<R> workflowQueryInterface, RuntimeEngine rteng) {
		this.runtimeEngine = rteng;
		this.wClass = workflowQueryInterface.getCanonicalName();
	}

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

		/* resultMap is used to map the result Value to the output parameters */
		final Map<String, Object> resultMap = new HashMap<String, Object>();

		/* items used to save the extracted input parameters from the custom task */
		Map<String, Object> items = workItem.getParameters();

		Object payload = (Object) items.get("payloadObject");
		GennyToken userToken = (GennyToken) items.get("userToken");
		String signalCode = (String) items.get("signalCode");
		String callingWorkflow = (String)items.get("callingWorkflow");
		String workflowBeCode = (String) items.get("workflowBeCode");
		if (StringUtils.isBlank(callingWorkflow)) {
			callingWorkflow = "";
		}
		Long processId = null;

		
		SessionFacts sessionFacts = new SessionFacts(userToken,userToken,payload);

		if (processId == null) {
			Method m;
			Optional<Long> processIdBysessionId = Optional.empty();
			try {
				Class<?> cls = Class.forName(this.wClass); // needs filtering.
				m = cls.getDeclaredMethod("getProcessIdByWorkflowBeCode", String.class);
				String param = workflowBeCode; 
				processIdBysessionId =  (Optional<Long>) m.invoke(null, (Object) param);

			} catch (NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//Optional<Long> processIdBysessionId = w.getProcessIdBysessionId(userToken.getSessionCode());
			boolean hasProcessIdBySessionId = processIdBysessionId.isPresent();
			if (hasProcessIdBySessionId) {
				processId = processIdBysessionId.get();
			}
		}

		if (processId != null) {

			
			log.info(callingWorkflow+": Sending Payload Object  to processId " + processId
					+ " for target user " + userToken.getUserCode()+" for sessionCode: "+userToken.getSessionCode());

			KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();

			KieSession newKieSession = null;

			if (this.runtimeEngine != null) {

				newKieSession = (StatefulKnowledgeSession) this.runtimeEngine.getKieSession();

				newKieSession.signalEvent(signalCode, sessionFacts, processId);
			} else {

				KieBase kieBase = RulesLoader.getKieBaseCache().get(userToken.getRealm());
				newKieSession = (StatefulKnowledgeSession) kieBase.newKieSession(ksconf, null);

				newKieSession.signalEvent(signalCode, sessionFacts, processId);

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