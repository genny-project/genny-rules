package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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
import life.genny.models.FramePosition;

public class ShowFrame implements WorkItemHandler {

	public ShowFrame() {
	}

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

		/* items used to save the extracted input parameters from the custom task */
		Map<String, Object> items = workItem.getParameters();

		// extract parameters
		GennyToken userToken = (GennyToken) workItem.getParameter("userToken");
		String rootFrameCode = (String) workItem.getParameter("rootFrameCode");
		String targetFrameCode = (String) workItem.getParameter("targetFrameCode");

		String callingWorkflow = (String) items.get("callingWorkflow");
		if (StringUtils.isBlank(callingWorkflow)) {
			callingWorkflow = "";
		}
		callingWorkflow += ":" + workItem.getProcessInstanceId() + ": ";

		display(userToken, rootFrameCode, targetFrameCode, callingWorkflow);

		// notify manager that work item has been completed
		if (workItem == null) {
			log.error(callingWorkflow + ": workItem is null");
		}
		manager.completeWorkItem(workItem.getId(), null);

	}

	/**
	 * @param userToken
	 * @param rootFrameCode
	 * @param targetFrameCode
	 * @param callingWorkflow
	 */
	public static void display(GennyToken userToken, String rootFrameCode, String targetFrameCode,
			String callingWorkflow) {
		if (userToken == null) {
			log.error(callingWorkflow + ": Must supply userToken!");

		} else {
			// log.info("userToken = " + userToken.getCode());

			if (rootFrameCode == null) {
				log.error(callingWorkflow + ": Must supply a root Frame Code!");
			} else {
				// log.info(callingWorkflow+": root Frame Code sent to display = " +
				// rootFrameCode);

				QDataBaseEntityMessage FRM_MSG = VertxUtils.getObject(userToken.getRealm(), "", rootFrameCode + "_MSG",
						QDataBaseEntityMessage.class, userToken.getToken());

				if (FRM_MSG == null) {
					// Construct it on the fly
					Frame3 frame = VertxUtils.getObject(userToken.getRealm(), "", rootFrameCode, Frame3.class,
							userToken.getToken());

					if (frame == null) {
						log.error("FRAME IS NOT IN CACHE  - " + rootFrameCode);

					}

					FrameUtils2.toMessage2(frame, userToken);
					FRM_MSG = VertxUtils.getObject(userToken.getRealm(), "", rootFrameCode + "_MSG",
							QDataBaseEntityMessage.class, userToken.getToken());

				}

				if (FRM_MSG != null) {

					if (targetFrameCode == null) {
						targetFrameCode = "FRM_ROOT";
					}

					QDataBaseEntityMessage TARGET_FRM_MSG = VertxUtils.getObject(userToken.getRealm(), "",
							targetFrameCode + "_MSG", QDataBaseEntityMessage.class, userToken.getToken());

					if (TARGET_FRM_MSG == null) {
						// Construct it on the fly
						Frame3 frame = VertxUtils.getObject(userToken.getRealm(), "", targetFrameCode, Frame3.class,
								userToken.getToken());

						FrameUtils2.toMessage2(frame, userToken);
						TARGET_FRM_MSG = VertxUtils.getObject(userToken.getRealm(), "", targetFrameCode + "_MSG",
								QDataBaseEntityMessage.class, userToken.getToken());
					}

					for (BaseEntity targetFrame : TARGET_FRM_MSG.getItems()) {
						if (targetFrame.getCode().equals(targetFrameCode)) {
//
//							log.info(callingWorkflow+": ShowFrame : Found Targeted Frame BaseEntity : " + targetFrame);

							/* Adding the links in the targeted BaseEntity */
							Attribute attribute = new Attribute("LNK_FRAME", "LNK_FRAME", new DataType(String.class));

							for (BaseEntity sourceFrame : FRM_MSG.getItems()) {
								if (sourceFrame.getCode().equals(rootFrameCode)) {

									// log.info(callingWorkflow+": ShowFrame : Found Source Frame BaseEntity : " +
									// sourceFrame);
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

					log.info(callingWorkflow + ": ShowFrame : " + rootFrameCode + ":" + targetFrameCode);

					FRM_MSG.setToken(userToken.getToken());

					FRM_MSG.setReplace(true);

					VertxUtils.writeMsg("webcmds", JsonUtils.toJson(FRM_MSG));

					Type setType = new TypeToken<Set<QDataAskMessage>>() {
					}.getType();

					String askMsgs2Str = (String) VertxUtils.cacheInterface.readCache(userToken.getRealm(),
							rootFrameCode + "_ASKS", userToken.getToken());
					if (askMsgs2Str == null) {
						log.error(rootFrameCode + "_ASKS is NOT IN CACHE!");
					} else {

						//
//					String askMsgs2Str = VertxUtils.getObject(userToken.getRealm(), "", rootFrameCode + "_ASKS",
//							String.class, userToken.getToken());

						Set<QDataAskMessage> askMsgs2 = JsonUtils.fromJson(askMsgs2Str, setType);

						// System.out.println("Sending Asks");
						if ((askMsgs2 != null) && (!askMsgs2.isEmpty())) {
							for (QDataAskMessage askMsg : askMsgs2) { // TODO, not needed
								for (Ask aask : askMsg.getItems()) {
									for (Validation val : aask.getQuestion().getAttribute().getDataType()
											.getValidationList()) {
										if (val.getRegex() == null) {
											log.error(callingWorkflow + ": Regex for " + aask.getQuestion().getCode()
													+ " == null");
										}

									}
								}
								askMsg.setToken(userToken.getToken());
								String json = JsonUtils.toJson(askMsg);
								String jsonStr = json.replaceAll("PER_SERVICE", userToken.getUserCode()); // set the
																											// user
								VertxUtils.writeMsg("webcmds", jsonStr); // QDataAskMessage
							}
						}
					}
				} else {
					log.error(callingWorkflow + ": " + rootFrameCode + "_MSG"
							+ " DOES NOT EXIST IN CACHE - cannot display frame");
				}

			}

		}
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, notifications cannot be aborted
	}

}