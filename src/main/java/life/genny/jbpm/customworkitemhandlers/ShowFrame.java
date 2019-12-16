package life.genny.jbpm.customworkitemhandlers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.Logger;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.json.JSONObject;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Task;

import com.google.gson.reflect.TypeToken;

import io.vertx.core.json.JsonObject;
import life.genny.models.Frame3;
import life.genny.models.GennyToken;
import life.genny.qwanda.Ask;
import life.genny.qwanda.ContextList;
import life.genny.qwanda.TaskAsk;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.validation.Validation;
import life.genny.qwanda.validation.ValidationList;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.rules.QRules;
import life.genny.rules.RulesLoader;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.DropdownUtils;
import life.genny.utils.FrameUtils2;
import life.genny.utils.OutputParam;
import life.genny.utils.RulesUtils;
import life.genny.utils.VertxUtils;
import life.genny.models.FramePosition;

import org.mentaregex.*;
import static org.mentaregex.Regex.*;

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
		OutputParam output = (OutputParam) workItem.getParameter("output");

		if (rootFrameCode.equals("NONE")) { // Do not change anything
			return;
		}

		String callingWorkflow = (String) items.get("callingWorkflow");
		if (StringUtils.isBlank(callingWorkflow)) {
			callingWorkflow = "";
		}
		callingWorkflow += ":" + workItem.getProcessInstanceId() + ": ";

		display(userToken, rootFrameCode, targetFrameCode, callingWorkflow, output);

		// notify manager that work item has been completed
		if (workItem == null) {
			log.error(callingWorkflow + ": workItem is null");
		}
		manager.completeWorkItem(workItem.getId(), null);

	}

	public static void display(GennyToken userToken, String rootFrameCode, String targetFrameCode,
			String callingWorkflow) {
		OutputParam output = new OutputParam();

		output.setTypeOfResult("FORMCODE");
		output.setResultCode(rootFrameCode);
		output.setTargetCode(targetFrameCode);
		display(userToken, rootFrameCode, targetFrameCode, callingWorkflow, output);
	}

	/**
	 * @param userToken
	 * @param rootFrameCode
	 * @param targetFrameCode
	 * @param callingWorkflow
	 */
	public static void display(GennyToken userToken, String rootFrameCode, String targetFrameCode,
			String callingWorkflow, OutputParam output) {

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
						if (VertxUtils.cachedEnabled) {
							return; // don't worry about it
						}
						log.error("FRAME IS NOT IN CACHE  - " + rootFrameCode);
						// ok, grab frame from rules
						BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
						BaseEntity rule = beUtils.getBaseEntityByCode("RUL_" + rootFrameCode);
						if (rule != null) {
							String frameStr = (String) rule.getValue("PRI_FRM").get(); // assume always
							if (frameStr != null) {
								VertxUtils.cacheInterface.writeCache(userToken.getRealm(), rootFrameCode + "_FRM",
										frameStr, userToken.getToken(), 0);
							} else {
								log.error(rootFrameCode + " HAS NOT BEEN GENERATED BY THE RULES, abort display");
								return;
							}
						} else {
							log.error("RUL_" + rootFrameCode + " HAS NOT BEEN GENERATED BY THE RULES, abort display");
							return;
						}
					}
					if (frame.getCode() == null) {
						frame = VertxUtils.getObject(userToken.getRealm(), "", rootFrameCode, Frame3.class,
								userToken.getToken());

						log.error("frame.getCode() in display  is null ");
						// return;
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

					log.info(callingWorkflow + ": ShowFrame !!!!! : " + rootFrameCode + ":" + targetFrameCode);

					FRM_MSG.setToken(userToken.getToken());

					FRM_MSG.setReplace(true);

					// Minify
					String payload = JsonUtils.toJson(FRM_MSG);
					JSONObject js = new JSONObject(payload);
					String payload2 = js.toString();
					if (payload2 != null) {
						VertxUtils.writeMsg("webcmds", payload2);
					}

					sendAsks(rootFrameCode, userToken, callingWorkflow, output);

				} else {
					log.error(callingWorkflow + ": " + rootFrameCode + "_MSG"
							+ " DOES NOT EXIST IN CACHE - cannot display frame");
				}

			}

		}
	}

	/**
	 * @param rootFrameCode
	 * @param userToken
	 * @param callingWorkflow
	 */
	private static void sendAsks(String rootFrameCode, GennyToken userToken, String callingWorkflow,
			OutputParam output) {

		if (VertxUtils.cachedEnabled) {
			// No point sending asks
			return;
		}

		TaskService taskService;
		Task task = null;
		Map<String, Object> taskAsks = null;
		Map<String, TaskAsk> attributeTaskAskMap = null;
		String sourceCode = null;
		String targetCode = null;

		if ((output != null)) {
			if ((output.getTaskId() != null) && (output.getTaskId() > 0L)) {
				taskService = RulesLoader.taskServiceMap.get(userToken.getRealm());
				task = taskService.getTaskById(output.getTaskId());
				// Now get the TaskAsk that relates to this specific Ask
				// assume that all attributes have the same source and target
				Long docId = task.getTaskData().getDocumentContentId();
				Content c = taskService.getContentById(docId);
				if (c == null) {
					log.error("*************** Task content is NULL *********** ABORTING");
					return;
				}
				taskAsks = (HashMap<String, Object>) ContentMarshallerHelper.unmarshall(c.getContent(), null);
				for (String key : taskAsks.keySet()) {
					Object obj = taskAsks.get(key);
					if (obj instanceof TaskAsk) { // This gets around my awful formcode values
						TaskAsk taskAsk = (TaskAsk) taskAsks.get(key);
						String attributeStr = taskAsk.getAsk().getAttributeCode();
						// attributeTaskAskMap.put(attributeStr,taskAsk);
						sourceCode = taskAsk.getAsk().getSourceCode();
						targetCode = taskAsk.getAsk().getTargetCode();
					}
				}
			}
		}
		Set<QDataAskMessage> askMsgs2 = fetchAskMessages(rootFrameCode, userToken);

		// System.out.println("Sending Asks");
		if ((askMsgs2 != null) && (!askMsgs2.isEmpty())) {
			for (QDataAskMessage askMsg : askMsgs2) { // TODO, not needed
				for (Ask aask : askMsg.getItems()) {
					for (Validation val : aask.getQuestion().getAttribute().getDataType().getValidationList()) {
						if (val.getRegex() == null) {
							log.error(callingWorkflow + ": Regex for " + aask.getQuestion().getCode() + " == null");
						}

					}
				}
				askMsg.setToken(userToken.getToken());

				String jsonStr = updateSourceAndTarget(askMsg, sourceCode, targetCode, output, userToken);

				// Find all the target be's to send
				Set<String> beCodes = new HashSet<String>();
				// The user will already be there
				if ((targetCode != null)) {
					beCodes.add(targetCode);
				}
				if (!output.getAttributeTargetCodeMap().keySet().isEmpty()) {
					beCodes.addAll(output.getAttributeTargetCodeMap().values());
				}
				beCodes.remove(userToken.getUserCode()); // no need to send the user
				if (!beCodes.isEmpty()) {
					List<BaseEntity> besToSend = new ArrayList<BaseEntity>();
					BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
					for (String tCode : beCodes) {
						BaseEntity be = beUtils.getBaseEntityByCode(tCode);
						besToSend.add(be);
					}
					QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(besToSend);
					beMsg.setToken(userToken.getToken());
					beMsg.setReplace(true);
					VertxUtils.writeMsg("webdata", JsonUtils.toJson(beMsg));

				}

				// find any select Attributes, find their selection Baseentities and send
				String[] dropdownCodes = match(jsonStr, "/(\\\"LNK_\\S+\\\")/g");
				if ((dropdownCodes != null) && (dropdownCodes.length > 0)) {
					for (String dropdownCode : dropdownCodes) {
						dropdownCode = dropdownCode.replaceAll("\"","");
						sendSelectionItems(dropdownCode, userToken);
					}
				}

				VertxUtils.writeMsg("webcmds", jsonStr); // QDataAskMessage
			}
		}
	}

	private static void updateTargetInAsk(Ask ask, String sourceCode, String targetCode, OutputParam output,
			GennyToken userToken) {
		String attributeCode = ask.getAttributeCode();
		String askTargetCode = output.getAttributeTargetCodeMap().get(attributeCode);
		if (askTargetCode != null) {
			ask.setTargetCode(askTargetCode);
		} else if (targetCode != null) {
			ask.setTargetCode(targetCode);
		} else {
			ask.setTargetCode(userToken.getUserCode());
		}

		if ((ask.getChildAsks() != null) && (ask.getChildAsks().length > 0)) {
			for (Ask childAsk : ask.getChildAsks()) {
				updateTargetInAsk(childAsk, sourceCode, targetCode, output, userToken);
			}
		}
	}

	private static String updateSourceAndTarget(QDataAskMessage askMsg, String sourceCode, String targetCode,
			OutputParam output, GennyToken userToken) {

		if (!output.getAttributeTargetCodeMap().keySet().isEmpty()) {
			for (Ask ask : askMsg.getItems()) {
				updateTargetInAsk(ask, sourceCode, targetCode, output, userToken);
			}

		}

		String json = JsonUtils.toJson(askMsg);

		String jsonStr = json.replaceAll("PER_SERVICE", userToken.getUserCode()); // set the

		log.info("ShowFrame: Setting outgoing Asks to have " + sourceCode + ":" + targetCode);
		if (sourceCode != null) { // user
			jsonStr = jsonStr.replaceAll("PER_SOURCE", sourceCode);
		} else {
			jsonStr = jsonStr.replaceAll("PER_SOURCE", userToken.getUserCode());
		}
		if (targetCode != null) { // user
			jsonStr = jsonStr.replaceAll("PER_TARGET", targetCode);
		} else {
			jsonStr = jsonStr.replaceAll("PER_TARGET", userToken.getUserCode());
		}
		return jsonStr;
	}

	/**
	 * @param rootFrameCode
	 * @param userToken
	 * @param setType
	 * @return
	 */
	public static Set<QDataAskMessage> fetchAskMessages(String rootFrameCode, GennyToken userToken) {
		Type setType = new TypeToken<Set<QDataAskMessage>>() {
		}.getType();

		String askMsgs2Str = null;
		if (GennySettings.forceCacheApi) { // if in junit then use the bridge to fetch
											// cache data
			log.info("Forcing ASKS to be read from api call to cache");
//						askMsgs2Str = VertxUtils.getObject(userToken.getRealm(), "", rootFrameCode + "_ASKS",
//						String.class, userToken.getToken());
			try {
				askMsgs2Str = QwandaUtils.apiGet(
						GennySettings.ddtUrl + "/read/" + userToken.getRealm() + "/" + rootFrameCode + "_ASKS",
						userToken.getToken());
				JsonObject json = new JsonObject(askMsgs2Str);
				askMsgs2Str = json.getString("value"); // TODO - assumes always works.....not always case
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {

			askMsgs2Str = (String) VertxUtils.cacheInterface.readCache(userToken.getRealm(), rootFrameCode + "_ASKS",
					userToken.getToken());

			if (askMsgs2Str == null) {
				try {
					askMsgs2Str = QwandaUtils.apiGet(
							GennySettings.ddtUrl + "/read/" + userToken.getRealm() + "/" + rootFrameCode + "_ASKS",
							userToken.getToken());
					JsonObject json = new JsonObject(askMsgs2Str);
					askMsgs2Str = json.getString("value"); // TODO - assumes always works.....not always case
					if (askMsgs2Str == null) {
						log.error("No Asks in cache - asking api to generate and refresh cache for " + rootFrameCode
								+ "_ASKS");
						String frameStr = (String) VertxUtils.cacheInterface.readCache(userToken.getRealm(),
								rootFrameCode, userToken.getToken());
						Frame3 rootFrame = JsonUtils.fromJson(frameStr, Frame3.class);
						if (rootFrame.getCode().startsWith("FRM_QUE_")) {

							FrameUtils2.toMessage2(rootFrame, userToken, "PER_SOURCE", "PER_TARGET");
						} else {
							Map<String, ContextList> contextListMap = new HashMap<String, ContextList>();
							FrameUtils2.toMessage(rootFrame, userToken, contextListMap, "PER_SERVICE", "PER_SERVICE",
									true);

							// FrameUtils2.toMessage(rootFrame, userToken,"PER_SERVICE","PER_SERVICE",true);
						}
						askMsgs2Str = (String) VertxUtils.cacheInterface.readCache(userToken.getRealm(),
								rootFrameCode + "_ASKS", userToken.getToken());
						if (askMsgs2Str == null) {
							log.error("Frame ASKS for " + rootFrameCode + " is just not happening...");
							return new HashSet<QDataAskMessage>();
						}
					}
				} catch (ClientProtocolException e) {

					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

		askMsgs2Str = askMsgs2Str.replaceAll(Pattern.quote("\\n"), Matcher.quoteReplacement("\n"));
		askMsgs2Str = askMsgs2Str.replaceAll(Pattern.quote("\\\""), Matcher.quoteReplacement("\""));
		askMsgs2Str = askMsgs2Str.replaceAll(Pattern.quote("\"["), Matcher.quoteReplacement("["));
		askMsgs2Str = askMsgs2Str.replaceAll(Pattern.quote("]\""), Matcher.quoteReplacement("]"));
		askMsgs2Str = askMsgs2Str.replaceAll(Pattern.quote("\\n"), "");

		Set<QDataAskMessage> askMsgs2 = null;

		try {
			log.debug("About to do deserialization!");
			askMsgs2 = JsonUtils.fromJson(askMsgs2Str, setType);
		} catch (Exception e) {
			log.error("Bad Json deserialization ..." + askMsgs2Str);
			BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
			BaseEntity rule = beUtils.getBaseEntityByCode("RUL_" + rootFrameCode);
			askMsgs2Str = (String) rule.getValue("PRI_ASKS").get(); // assume always

			if (askMsgs2Str == null) {
				String fr = (String) rule.getValue("PRI_FRM").get(); // assume always
				Frame3 frame3 = JsonUtils.fromJson(fr, Frame3.class);
				FrameUtils2.toMessage2(frame3, userToken);
				askMsgs2Str = (String) VertxUtils.cacheInterface.readCache(userToken.getRealm(),
						rootFrameCode + "_ASKS", userToken.getToken());
			}

			VertxUtils.cacheInterface.writeCache(userToken.getRealm(), rootFrameCode + "_ASKS", askMsgs2Str,
					userToken.getToken(), 0);
			log.info("About to do deserialization2!");
			askMsgs2 = JsonUtils.fromJson(askMsgs2Str, setType);
		}
		return askMsgs2;
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, notifications cannot be aborted
	}

	private static void sendSelectionItems(String attributeCode, GennyToken userToken) {
		Attribute attribute = RulesUtils.getAttribute(attributeCode, userToken.getToken());
		if (attribute != null) {
			DataType dt = attribute.getDataType();
			log.info("DATATYPE IS " + dt);
			List<Validation> vl = dt.getValidationList();
			if ((vl!=null)&&(vl.get(0)!=null)) {
					Validation val = vl.get(0);
					if ((val.getSelectionBaseEntityGroupList()!=null)&&(!val.getSelectionBaseEntityGroupList().isEmpty())) {
						String groupCode = val.getSelectionBaseEntityGroupList().get(0);
					
			
			DropdownUtils dropDownUtils = new DropdownUtils();
			dropDownUtils.setNewSearch("Dropdown", "Fetch Dropdown Items")
					.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "SEL_%")
					.setSourceCode(groupCode).setPageStart(0).setPageSize(10000);
			try {
				dropDownUtils.sendSearchResults(groupCode, "LNK_CORE", "DEGREE", userToken);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
		}
		}
	}

}