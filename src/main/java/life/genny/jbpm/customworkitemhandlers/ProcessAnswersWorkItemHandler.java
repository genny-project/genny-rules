package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Attachment;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;

import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.Answers;
import life.genny.qwanda.Ask;
import life.genny.qwanda.TaskAsk;
import life.genny.utils.BaseEntityUtils;

public class ProcessAnswersWorkItemHandler implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	RuntimeEngine runtimeEngine;
	String wClass;
	TaskService taskService;
	

	public <R> ProcessAnswersWorkItemHandler(Class<R> workflowQueryInterface) {
		this.wClass = workflowQueryInterface.getCanonicalName();
	}

	public <R> ProcessAnswersWorkItemHandler(Class<R> workflowQueryInterface, RuntimeEngine rteng) {
		this.runtimeEngine = rteng;
		this.wClass = workflowQueryInterface.getCanonicalName();
		this.taskService = rteng.getTaskService();
	}

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

		/* resultMap is used to map the result Value to the output parameters */
		final Map<String, Object> resultMap = new HashMap<String, Object>();

		/* items used to save the extracted input parameters from the custom task */
		Map<String, Object> items = workItem.getParameters();

		GennyToken userToken = (GennyToken) items.get("userToken");
		GennyToken serviceToken = (GennyToken) items.get("serviceToken");
		
		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);
		
		Answers answersToSave = (Answers) items.get("answersToSave");
		String callingWorkflow = (String)items.get("callingWorkflow");
		if (StringUtils.isBlank(callingWorkflow)) {
			callingWorkflow = "";
		}

		// Extract all the current questions from all the users Tasks
		List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Ready);
      //  statuses.add(Status.Completed);
      //  statuses.add(Status.Created);
      //  statuses.add(Status.Error);
      //  statuses.add(Status.Exited);
        statuses.add(Status.InProgress);
      //  statuses.add(Status.Obsolete);
        statuses.add(Status.Reserved);
      //  statuses.add(Status.Suspended);

		String realm = userToken.getRealm();
		String userCode = userToken.getUserCode();
		List<TaskSummary> tasks = taskService.getTasksOwnedByStatus(realm+"+"+userCode, statuses, null);
		log.info("Tasks="+tasks);
		Map<String,Object> asks = new HashMap<String,Object>();

		for (TaskSummary taskSummary : tasks) {
			Task task = taskService.getTaskById(taskSummary.getId());
	        Content c = taskService.getContentById(task.getTaskData().getDocumentContentId());
			HashMap<String, Object> taskAsks = (HashMap<String, Object>) ContentMarshallerHelper.unmarshall(c.getContent(), null);
			asks.putAll( taskAsks);
//			List<Attachment> attachments = task.getTaskData().getAttachments();
//			if (!attachments.isEmpty()) {
//				for (Attachment attachment : attachments) {
//					long firstAttachContentId = attachment.getAttachmentContentId();
//			        Content firstAttachContent = taskService.getContentById(firstAttachContentId);
//			        HashMap<String,Object> taskAsks = SerializationUtils.deserialize(firstAttachContent.getContent());
//			        
//				//	byte[] byteArray = attachment.
//			//Map<String,Object> taskAsks = task.getTaskData().getTaskInputVariables();
//			if (taskAsks != null)  {
//				asks.putAll( taskAsks);
//			}
//				}
//			}
		}

		
		// Loop through all the answers check their validity and save them.
		List<Answer> validAnswers = new ArrayList<Answer>();
		for (Answer answer : answersToSave.getAnswers()) {
			// check answer
			if (answer.getSourceCode().equals(userToken.getUserCode())) {
				String key = answer.getSourceCode()+":"+answer.getTargetCode()+":"+answer.getAttributeCode();
				TaskAsk ask = (TaskAsk) asks.get(key);
				if (ask != null) {
					validAnswers.add(answer);
				} else {
					log.error("Not a valid ASK! "+key);
				}
			}
		}
		log.info(callingWorkflow+" Saving Valid Answers ...");
		beUtils.saveAnswers(validAnswers); // save answers in one big thing
		
		// check all the tasks to see if any are completed and if so then complete them!
		

		// notify manager that work item has been completed
		manager.completeWorkItem(workItem.getId(), resultMap);

	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, notifications cannot be aborted
	}



}