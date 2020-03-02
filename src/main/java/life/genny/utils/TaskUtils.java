package life.genny.utils;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.StatefulKnowledgeSession;

import io.vertx.core.json.JsonObject;
import life.genny.model.OutputParam2;
import life.genny.models.GennyToken;
import life.genny.qwanda.Ask;
import life.genny.qwanda.Context;
import life.genny.qwanda.ContextList;
import life.genny.qwanda.ContextType;
import life.genny.qwanda.Link;
import life.genny.qwanda.Question;
import life.genny.qwanda.TaskAsk;
import life.genny.qwanda.VisualControlType;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.validation.Validation;
import life.genny.qwanda.validation.ValidationList;
import life.genny.qwandautils.JsonUtils;
import life.genny.rules.RulesLoader;

public class TaskUtils {
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public static List<TaskSummary> getUserTaskSummarys(GennyToken userToken) {
		List<Status> statuses = new ArrayList<Status>();
		statuses.add(Status.Ready);
		// statuses.add(Status.Completed);
		// statuses.add(Status.Created);
		// statuses.add(Status.Error);
		// statuses.add(Status.Exited);
		statuses.add(Status.InProgress);
		// statuses.add(Status.Obsolete);
		statuses.add(Status.Reserved);
		// statuses.add(Status.Suspended);

		String realm = userToken.getRealm();
		String userCode = userToken.getUserCode();
		List<TaskSummary> tasks = RulesLoader.taskServiceMap.get(userToken.getRealm())
				.getTasksOwnedByStatus(realm + "+" + userCode, statuses, null);
		log.info("Tasks=" + tasks);
		return tasks;
	}

	public static void sendTaskMenuItems(GennyToken userToken) {
		TaskService taskService = RulesLoader.taskServiceMap.get(userToken.getRealm());

		List<TaskSummary> taskSummarys = getUserTaskSummarys(userToken);

		List<BaseEntity> taskItems = new ArrayList<BaseEntity>();

		if ((taskSummarys != null) && (!taskSummarys.isEmpty())) {
			Comparator<TaskSummary> compareByPriority = (TaskSummary o1, TaskSummary o2) -> o1.getPriority()
					.compareTo(o2.getPriority());
			Collections.sort(taskSummarys, compareByPriority);
			Integer index = 0;
			for (TaskSummary ts : taskSummarys) {
				// We send an Ask to the frontend that contains the task items
				Task task = taskService.getTaskById(ts.getId());
				BaseEntity item = new BaseEntity(task.getName() + "-" + task.getId(),  task.getDescription());
				item.setRealm(userToken.getRealm());
				item.setIndex(index++);
				taskItems.add(item);
			}

			QDataBaseEntityMessage msg = new QDataBaseEntityMessage(taskItems);
			msg.setParentCode("QUE_DRAFTS_GRP");
			msg.setToken(userToken.getToken());
			msg.setLinkCode("LNK_CORE");
			msg.setLinkValue("ITEMS");
			msg.setReplace(true);
			msg.setShouldDeleteLinkedBaseEntities(true);

			/* Linking child baseEntity to the parent baseEntity */
			QDataBaseEntityMessage beMessage = setDynamicLinksToParentBe(msg, "QUE_DRAFTS_GRP", "LNK_CORE", "ITEMS",
					userToken, true);
			msg.setToken(userToken.getToken());
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg));

		}
	}

	/*
	 * Setting dynamic links between parents and child. ie. linking DropDown items
	 * to the DropDown field.
	 */
	static QDataBaseEntityMessage setDynamicLinksToParentBe(QDataBaseEntityMessage beMsg, String parentCode,
			String linkCode, String linkValue, GennyToken gennyToken, Boolean sortByWeight) {

		BaseEntity parentBe = new BaseEntityUtils(gennyToken).getBaseEntityByCode(parentCode);

		if (parentBe != null) {

			Set<EntityEntity> childLinks = new HashSet<>();
			double index = -1.0;

			/* creating a dumb attribute for linking the search results to the parent */
			Attribute attributeLink = new Attribute(linkCode, linkCode, new DataType(String.class));

			for (BaseEntity be : beMsg.getItems()) {

				if (sortByWeight) {

					childLinks = parentBe.getLinks();
					break;
				} else {

					index++;
				}

				EntityEntity ee = new EntityEntity(parentBe, be, attributeLink, index);

				/* creating link for child */
				Link link = new Link(parentCode, be.getCode(), attributeLink.getCode(), linkValue, index);

				/* adding link */
				ee.setLink(link);

				/* adding child link to set of links */
				childLinks.add(ee);

			}

			parentBe.setLinks(childLinks);
			beMsg.add(parentBe);
			return beMsg;

		} else {

			log.error("Unable to fetch Parent BaseEntity : parentCode");
			return null;
		}

	}

	public static void sendTaskAskItems(GennyToken userToken) {

		List<Ask> taskAskItemList = new ArrayList<Ask>();
		List<TaskSummary> taskSummarys = getUserTaskSummarys(userToken);
		if ((taskSummarys != null) && (!taskSummarys.isEmpty())) {
			Comparator<TaskSummary> compareByPriority = (TaskSummary o1, TaskSummary o2) -> o1.getPriority()
					.compareTo(o2.getPriority());
			Collections.sort(taskSummarys, compareByPriority);
			Integer index = 0;
			for (TaskSummary ts : taskSummarys) {
				// We send an Ask to the frontend that contains the task items
				Task task = RulesLoader.taskServiceMap.get(userToken.getRealm()).getTaskById(ts.getId());
				BaseEntity item = new BaseEntity(task.getName() + "-" + task.getId(), task.getDescription());
				item.setRealm(userToken.getRealm());
				item.setIndex(index++);
				// Attribute questionDraftItemAttribute = new Attribute("QQQ_DRAFT_ITEM",
				// "link",
				// new DataType(String.class));
				Attribute questionDraftItemAttribute = new Attribute("QQQ_QUESTION_GROUP", "link",
						new DataType(String.class));

				Question question = new Question("QUE_TASK-" + task.getId(), task.getDescription(), questionDraftItemAttribute,
						true);
				Ask childAsk = new Ask(question, userToken.getUserCode(), userToken.getUserCode());

				/* add the entityAttribute ask to list */
				taskAskItemList.add(childAsk);
			}
			
			// Now send a Clear Tasks  menu item
			BaseEntity clearItems = new BaseEntity("MEN_CLEAR_ITEMS","Clear All Tasks");
			clearItems.setRealm(userToken.getRealm());
			clearItems.setIndex(index++);
			Attribute questionDraftItemAttribute = new Attribute("QQQ_QUESTION_GROUP", "link",
					new DataType(String.class));

			Question question = new Question("QUE_CLEAR_TASKS", "Clear All Tasks", questionDraftItemAttribute,
					true);
			Ask childAsk = new Ask(question, userToken.getUserCode(), userToken.getUserCode());

			/* add the entityAttribute ask to list */
			taskAskItemList.add(childAsk);
		} else {
			// send a blank drafts menu
			// Now send a Clear Tasks  menu item
			BaseEntity clearItems = new BaseEntity("MEN_CLEAR_ITEMS","No Tasks!");
			clearItems.setRealm(userToken.getRealm());
			clearItems.setIndex(0);
			Attribute questionDraftItemAttribute = new Attribute("QQQ_QUESTION_GROUP", "link",
					new DataType(String.class));

			Question question = new Question("QUE_CLEAR_TASKS", "No Tasks!", questionDraftItemAttribute,
					true);
			Ask childAsk = new Ask(question, userToken.getUserCode(), userToken.getUserCode());

			/* add the entityAttribute ask to list */
			taskAskItemList.add(childAsk);

		}

		/* add the contextList to QUE_DRAFTS_GRP */
		Context contextDropdownItem = new Context(ContextType.THEME,
				new BaseEntity("THM_DROPDOWN_ITEM", "THM_DROPDOWN_ITEM"), VisualControlType.VCL);
		contextDropdownItem.setDataType("Form Submit Cancel");

		List<Context> contexts = new ArrayList<Context>();
		contexts.add(
				new Context(ContextType.THEME, new BaseEntity("THM_DROPDOWN_ICON_HIDE", "THM_DROPDOWN_ICON_HIDE")));
		contexts.add(new Context(ContextType.THEME, new BaseEntity("THM_BACKGROUND_NONE", "THM_BACKGROUND_NONE"),
				VisualControlType.GROUP, 2.0));
		contexts.add(new Context(ContextType.THEME,
				new BaseEntity("THM_DROPDOWN_BEHAVIOUR_GENNY", "THM_DROPDOWN_BEHAVIOUR_GENNY"),
				VisualControlType.GROUP));
		contexts.add(new Context(ContextType.THEME,
				new BaseEntity("THM_DROPDOWN_HEADER_WRAPPER_GENNY", "THM_DROPDOWN_HEADER_WRAPPER_GENNY")));
		contexts.add(new Context(ContextType.THEME,
				new BaseEntity("THM_DROPDOWN_GROUP_LABEL_GENNY", "THM_DROPDOWN_GROUP_LABEL_GENNY")));
		contexts.add(new Context(ContextType.THEME,
				new BaseEntity("THM_DROPDOWN_CONTENT_WRAPPER_GENNY", "THM_DROPDOWN_CONTENT_WRAPPER_GENNY"),
				VisualControlType.GROUP_CONTENT_WRAPPER));
		contexts.add(
				new Context(ContextType.THEME, new BaseEntity("THM_PROJECT_COLOR_SURFACE", "THM_PROJECT_COLOR_SURFACE"),
						VisualControlType.GROUP_CONTENT_WRAPPER));
		contexts.add(new Context(ContextType.THEME, new BaseEntity("THM_BOX_SHADOW_SM", "THM_BOX_SHADOW_SM"),
				VisualControlType.GROUP_CONTENT_WRAPPER));
		contexts.add(new Context(ContextType.THEME, new BaseEntity("THM_DROPDOWN_VCL_GENNY", "THM_DROPDOWN_VCL_GENNY"),
				VisualControlType.VCL));
		contexts.add(contextDropdownItem);

		ContextList contextList = new ContextList(contexts);

		/* converting childAsks list to array */
		Ask[] childAsArr = taskAskItemList.stream().toArray(Ask[]::new);
		/* Get the on-the-fly question attribute */
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));

		/* Generate ask for the baseentity */
		Question parentQuestion = new Question("QUE_DRAFTS_GRP", "Drafts", questionAttribute, true);
		Ask parentAsk = new Ask(parentQuestion, userToken.getUserCode(), userToken.getUserCode());

		/* setting the contextList to the the question */
		parentAsk.setContextList(contextList);

		/* setting weight to parent ask */
		parentAsk.setWeight(1.0);

		/* set all the childAsks to parentAsk */
		parentAsk.setChildAsks(childAsArr);

		QDataAskMessage askMsg = new QDataAskMessage(parentAsk);
		askMsg.setToken(userToken.getToken());
		askMsg.setReplace(true);
		String sendingMsg = JsonUtils.toJson(askMsg);
		Integer length = sendingMsg.length();
		VertxUtils.writeMsg("webcmds", sendingMsg);

	}

	public static OutputParam getTaskOutputParam(GennyToken userToken, Long taskId) {
		OutputParam output = new OutputParam();
		log.info("getTaskOutputParam: taskId=" + taskId);
		// Make sure that only valid taskIds are looked at
		List<TaskSummary> taskSummarys = getUserTaskSummarys(userToken);
		Boolean found = false;
		for (TaskSummary ts : taskSummarys) {
			Long tsId = ts.getId();
			if (tsId.equals(taskId)) {
				found = true;
				break;
			}
		}
		log.info("getTaskOutputParam: found=" + found);
		if (found) {
			TaskService taskService = RulesLoader.taskServiceMap.get(userToken.getRealm());
			Task task = taskService.getTaskById(taskId);
			String formCode = task.getFormName();
			output.setFormCode(formCode, "FRM_CONTENT");
			output.setTaskId(taskId);
			Long docId = task.getTaskData().getDocumentContentId();
			Content c = taskService.getContentById(docId);
			if (c == null) {
				log.error("*************** Task content is NULL *********** ABORTING");
				return output;
			}
			HashMap<String, Object> taskAsks2 = (HashMap<String, Object>) ContentMarshallerHelper
					.unmarshall(c.getContent(), null);
			ConcurrentHashMap<String, Object> taskAsks = new ConcurrentHashMap<String, Object>(taskAsks2);

			// Now find the formcode and set up the attribute:targetCode map
			for (String key : taskAsks.keySet()) {
				if (taskAsks.get(key) instanceof String) {
					continue;
				}
				TaskAsk ask = (TaskAsk) taskAsks.get(key);
				String attributeCode = ask.getAsk().getAttributeCode();
				String targetCode = ask.getAsk().getTargetCode();
				output.getAttributeTargetCodeMap().put(attributeCode, targetCode);
			}
		} else {
			log.error("taskId supplied is not allowed or valid for the user");
		}
		return output;
	}
	
	public static void clearAllTasks(GennyToken userToken)
	{
		TaskService taskService = RulesLoader.taskServiceMap.get(userToken.getRealm());

		List<TaskSummary> taskSummarys = getUserTaskSummarys(userToken);
		for (TaskSummary ts : taskSummarys) {
			Long tsId = ts.getId();
			Task task = taskService.getTaskById(tsId);
			Map<String,Object> results = new HashMap<String,Object>();
			results.put("taskid", tsId);
			results.put("status", "aborted");

		
			
			if (task.getTaskData().getStatus().equals(Status.Reserved)) {
				taskService.start(tsId, userToken.getRealm() + "+" + userToken.getUserCode()); // start!
			//	taskService.fail(tsId, userToken.getRealm() + "+" + userToken.getUserCode(), results);
				taskService.release(tsId, userToken.getRealm() + "+" + userToken.getUserCode());
			} else {
				// maybe only abort if there is no data in the tasks? So if a task is not reserved then it has some data in it!
			//	taskService.complete(tsId, userToken.getRealm() + "+" + userToken.getUserCode(), results);
				taskService.release(tsId, userToken.getRealm() + "+" + userToken.getUserCode());

			}
			log.info("Aborted Task "+tsId);
		}
		sendTaskAskItems(userToken) ;
	}
	
	public static void clearTaskType(GennyToken userToken, Question q)
	{
		TaskService taskService = RulesLoader.taskServiceMap.get(userToken.getRealm());

		List<TaskSummary> taskSummarys = getUserTaskSummarys(userToken);
		for (TaskSummary ts : taskSummarys) {
			Long tsId = ts.getId();
			Task task = taskService.getTaskById(tsId);
			
			if (task.getTaskData().getStatus().equals(Status.Reserved)) {
				if (task.getDescription().equalsIgnoreCase(q.getName())) {
					taskService.start(tsId, userToken.getRealm() + "+" + userToken.getUserCode()); // start!
					taskService.release(tsId, userToken.getRealm() + "+" + userToken.getUserCode());
				//	sendTaskAskItems(userToken) ;
					break;
				}
			} 
			log.info("Aborted Task "+tsId);
		}
		
	}
	
	static public Question getQuestion(String questionCode,GennyToken userToken)
	{

		Question q = null;
		Integer retry = 4;
		while (retry >= 0) { // Sometimes q is read properly from cache
			JsonObject jsonQ = VertxUtils.readCachedJson(userToken.getRealm(), questionCode, userToken.getToken());
			q = JsonUtils.fromJson(jsonQ.getString("value"), Question.class);
			if (q == null) {
				retry--;
				
			} else {
				break;
			}

		}
		
		if (q == null)
		{
			log.error("CANNOT READ "+questionCode+" from cache!!! Aborting (after having tried 4 times");
			return null;
		} else {
			return q;
		}
	}
	
}
