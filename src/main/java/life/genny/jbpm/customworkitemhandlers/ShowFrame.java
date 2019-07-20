package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import com.google.gson.reflect.TypeToken;

import io.vertx.core.json.JsonObject;
import life.genny.models.Frame3;
import life.genny.models.GennyToken;
import life.genny.qwanda.Ask;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.validation.Validation;
import life.genny.qwandautils.JsonUtils;
import life.genny.rules.QRules;
import life.genny.utils.FrameUtils2;
import life.genny.utils.VertxUtils;

public class ShowFrame implements WorkItemHandler {
	
	KieSession kieSession = null;
	
	public ShowFrame(KieSession kieSession)
	{
		this.kieSession = kieSession;
	}

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		// extract parameters
		GennyToken userToken = (GennyToken) workItem.getParameter("userToken");
		String rootFrameCode = (String) workItem.getParameter("rootFrameCode");
		String targetFrameCode = (String) workItem.getParameter("targetFrameCode");
		
		ProcessInstance p = this.kieSession.getProcessInstance(workItem.getProcessInstanceId());

		if (userToken == null) {
			log.error("Must supply userToken!");

		} else {

			//log.info("userToken = " + userToken.getCode());

			if (rootFrameCode == null) {
				log.error("Must supply a root Frame Code!");
			} else {
				log.info(p.getProcessName()+": root Frame Code sent to display  = " + rootFrameCode);

				QDataBaseEntityMessage FRM_MSG = VertxUtils.getObject(userToken.getRealm(), "", rootFrameCode + "-MSG",
						QDataBaseEntityMessage.class, userToken.getToken());
				FRM_MSG.setToken(userToken.getToken());
				String frmStr = JsonUtils.toJson(FRM_MSG);
				if (targetFrameCode == null) {
					targetFrameCode = "FRM_ROOT";
				}
				frmStr = frmStr.replaceAll(rootFrameCode, targetFrameCode);
				QDataBaseEntityMessage FRM_MSG_ROOT = JsonUtils.fromJson(frmStr, QDataBaseEntityMessage.class);
				VertxUtils.writeMsg("webcmds", JsonUtils.toJson(FRM_MSG_ROOT));
				Type setType = new TypeToken<Set<QDataAskMessage>>() {
				}.getType();

				String askMsgs2Str = VertxUtils.getObject(userToken.getRealm(), "", rootFrameCode + "-ASKS",
						String.class, userToken.getToken());

				Set<QDataAskMessage> askMsgs2 = JsonUtils.fromJson(askMsgs2Str, setType);

			//	System.out.println("Sending Asks");
				if ((askMsgs2 != null) && (!askMsgs2.isEmpty())) {
					for (QDataAskMessage askMsg : askMsgs2) { //TODO, not needed
						for (Ask aask : askMsg.getItems()) {
							for (Validation val : aask.getQuestion().getAttribute().getDataType().getValidationList()) {
								if (val.getRegex() == null) {
									log.error("Regex for " + aask.getQuestion().getCode() + " == null");
								}

							}
						}
						askMsg.setToken(userToken.getToken());
						String json = JsonUtils.toJson(askMsg);
						String jsonStr = json.replaceAll("PER_SERVICE", userToken.getUserCode()); // set the user

						VertxUtils.writeMsg("webcmds", jsonStr); // QDataAskMessage
					}
				}

			}

		}

		// notify manager that work item has been completed
		manager.completeWorkItem(workItem.getId(), null);

	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, notifications cannot be aborted
	}

}