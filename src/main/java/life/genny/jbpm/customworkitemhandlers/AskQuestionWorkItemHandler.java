package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
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

public class AskQuestionWorkItemHandler implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	KieSession kieSession;
	RuntimeEngine runtimeEngine;

	public AskQuestionWorkItemHandler(KieSession kieSession) {
		this.kieSession = kieSession;
	}

	public AskQuestionWorkItemHandler(KieSession kieSession, RuntimeEngine rteng) {
		this.kieSession = kieSession;
		this.runtimeEngine = rteng;
	}

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

		/* resultMap is used to map the result Value to the output parameters */
		final Map<String, Object> resultMap = new HashMap<String, Object>();

		/* items used to save the extracted input parameters from the custom task */
		Map<String, Object> items = workItem.getParameters();

		GennyToken userToken = (GennyToken) items.get("userToken");
		String questionCode = (String) items.get("questionCode");
		Long processId = (Long) items.get("processId");

		QEventMessage questionMsg = new QEventMessage("EVT_MSG", "ASK");
		questionMsg.getData().setCode(questionCode);
		questionMsg.setToken(userToken.getToken());

		if (processId == null) {
			Optional<Long> processIdBysessionId = RulesLoader.getProcessIdBysessionId(userToken.getSessionCode());
			boolean hasProcessIdBySessionId = processIdBysessionId.isPresent();
			if (hasProcessIdBySessionId) {
				processId = processIdBysessionId.get();
			}
		}

		if (processId != null) {
			System.out.println("Sending Question Code  " + questionCode + " to processId " + processId
					+ " for target user " + userToken.getUserCode());

			KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();

			KieSession newKieSession = null;

			if (this.runtimeEngine != null) {

				newKieSession = (StatefulKnowledgeSession) this.runtimeEngine.getKieSession();

				newKieSession.signalEvent("internalSignal", questionMsg, processId);

			} else {

				KieBase kieBase = RulesLoader.getKieBaseCache().get(userToken.getRealm());
				newKieSession = (StatefulKnowledgeSession) kieBase.newKieSession(ksconf, null);

				newKieSession.signalEvent("internalSignal", questionMsg, processId);

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