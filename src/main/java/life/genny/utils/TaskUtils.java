package life.genny.utils;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;

import life.genny.models.GennyToken;
import life.genny.qwanda.Ask;
import life.genny.qwanda.Link;
import life.genny.qwanda.Question;
import life.genny.qwanda.TaskAsk;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
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
				BaseEntity item = new BaseEntity(task.getName() + "-" + task.getId(), task.getName());
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
				BaseEntity item = new BaseEntity(task.getName() + "-" + task.getId(), task.getName());
				item.setRealm(userToken.getRealm());
				item.setIndex(index++);
				Attribute questionDraftItemAttribute = new Attribute("QQQ_DRAFT_ITEM", "link",
						new DataType(String.class));

				Question question = new Question("QUE_TASK-" + task.getId(), task.getName(), questionDraftItemAttribute,
						true);
				Ask childAsk = new Ask(question, userToken.getUserCode(), userToken.getUserCode());

				/* add the entityAttribute ask to list */
				taskAskItemList.add(childAsk);
			}
		}

		/* converting childAsks list to array */
		Ask[] childAsArr = taskAskItemList.stream().toArray(Ask[]::new);
		/* Get the on-the-fly question attribute */
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));

		/* Generate ask for the baseentity */
		Question parentQuestion = new Question("QUE_DRAFTS_GRP", "Drafts", questionAttribute, true);
		Ask parentAsk = new Ask(parentQuestion, userToken.getUserCode(), userToken.getUserCode());

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
}
