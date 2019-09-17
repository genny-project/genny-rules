package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.apache.logging.log4j.Logger;
import org.jbpm.kie.services.impl.query.mapper.ProcessInstanceQueryMapper;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.jbpm.services.api.query.QueryService;
import org.jbpm.services.api.query.model.QueryParam;
import org.jbpm.services.api.utils.KieServiceConfigurator;
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
import org.kie.internal.identity.IdentityProvider;
import org.kie.internal.query.QueryContext;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.kie.internal.task.api.UserGroupCallback;

import life.genny.models.GennyToken;
import life.genny.qwanda.message.QEventMessage;
import life.genny.rules.QRules;
import life.genny.rules.RulesLoader;
import life.genny.utils.OutputParam;
import life.genny.utils.SessionFacts;
import life.genny.utils.VertxUtils;
import life.genny.utils.WorkflowQueryInterface;

public class SendSignalWorkItemHandler implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	KieSession kieSession;
	RuntimeEngine runtimeEngine;
	String wClass;
	

	public <R> SendSignalWorkItemHandler(Class<R> workflowQueryInterface,KieSession kieSession) {
		this.kieSession = kieSession;
		this.wClass = workflowQueryInterface.getCanonicalName();
	}

	public <R> SendSignalWorkItemHandler(Class<R> workflowQueryInterface,KieSession kieSession, RuntimeEngine rteng) {
		this.kieSession = kieSession;
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
		Long processId = null;

		
		SessionFacts sessionFacts = new SessionFacts(userToken,userToken,payload);

		if (processId == null) {
			Method m;
			Optional<Long> processIdBysessionId = Optional.empty();
			try {
				Class<?> cls = Class.forName(this.wClass); // needs filtering.
				m = cls.getDeclaredMethod("getProcessIdBySessionId", String.class);
				String param = userToken.getSessionCode(); 
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
			System.out.println("Sending Payload Object  to processId " + processId
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