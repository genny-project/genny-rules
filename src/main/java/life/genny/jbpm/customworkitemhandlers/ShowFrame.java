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
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.validation.Validation;
import life.genny.qwandautils.JsonUtils;
import life.genny.rules.QRules;
import life.genny.utils.FrameUtils2;
import life.genny.utils.VertxUtils;

public class ShowFrame implements WorkItemHandler {

	KieSession kieSession = null;

	public ShowFrame(KieSession kieSession) {
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
			// log.info("userToken = " + userToken.getCode());

			if (rootFrameCode == null) {
				log.error("Must supply a root Frame Code!");
			} else {
				log.info(p.getProcessName() + ": root Frame Code sent to display  = " + rootFrameCode);

				QDataBaseEntityMessage FRM_MSG = VertxUtils.getObject(userToken.getRealm(), "", rootFrameCode + "-MSG",
						QDataBaseEntityMessage.class, userToken.getToken());

				if (FRM_MSG != null) {

					if (targetFrameCode == null) {
						targetFrameCode = "FRM_ROOT";
					}

					QDataBaseEntityMessage TARGET_FRM_MSG = VertxUtils.getObject(userToken.getRealm(), "",
							targetFrameCode + "-MSG", QDataBaseEntityMessage.class, userToken.getToken());

					for (BaseEntity targetFrame : TARGET_FRM_MSG.getItems()) {
						if (targetFrame.getCode().equals(targetFrameCode)) {

							System.out.println("ShowFrame : Found Targeted Frame BaseEntity : " + targetFrame);

							/* Adding the links in the targeted BaseEntity */
							Attribute attribute = new Attribute("LNK_FRAME", "LNK_FRAME", new DataType(String.class));

							for (BaseEntity sourceFrame : FRM_MSG.getItems()) {
								if (sourceFrame.getCode().equals(rootFrameCode)) {

									System.out.println("ShowFrame : Found Source Frame BaseEntity : " + sourceFrame);
									EntityEntity entityEntity = new EntityEntity(targetFrame, sourceFrame, attribute,
											1.0, "CENTRE");
									Set<EntityEntity> entEntList = targetFrame.getLinks();
									entEntList.add(entityEntity);
									targetFrame.setLinks(entEntList);

									/* Adding Frame to Targeted Frame BaseEntity Message */
									FRM_MSG.add(targetFrame);
									FRM_MSG.setReplace(true);
									break;
								}
							}
							break;
						}
					}

					FRM_MSG.setToken(userToken.getToken());

					FRM_MSG.setReplace(true);

					VertxUtils.writeMsg("webcmds", JsonUtils.toJson(FRM_MSG));

					Type setType = new TypeToken<Set<QDataAskMessage>>() {
					}.getType();

					String askMsgs2Str = VertxUtils.getObject(userToken.getRealm(), "", rootFrameCode + "-ASKS",
							String.class, userToken.getToken());

					Set<QDataAskMessage> askMsgs2 = JsonUtils.fromJson(askMsgs2Str, setType);

					// System.out.println("Sending Asks");
					if ((askMsgs2 != null) && (!askMsgs2.isEmpty())) {
						for (QDataAskMessage askMsg : askMsgs2) { // TODO, not needed
							for (Ask aask : askMsg.getItems()) {
								for (Validation val : aask.getQuestion().getAttribute().getDataType()
										.getValidationList()) {
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
				} else {
					log.error(rootFrameCode + "-MSG" + " DOES NOT EXIST IN CACHE - cannot display frame");
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