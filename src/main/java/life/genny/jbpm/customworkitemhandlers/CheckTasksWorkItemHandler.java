package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieSession;
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
import org.kie.internal.task.api.InternalTaskService;
import org.kie.internal.task.api.TaskContext;
import org.kie.internal.task.api.TaskModelProvider;
import org.kie.internal.task.api.TaskPersistenceContext;
import org.kie.internal.task.api.model.ContentData;
import org.kie.internal.task.api.model.InternalContent;
import org.kie.internal.task.api.model.InternalTask;
import org.kie.internal.task.api.model.InternalTaskData;

import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.Answers;
import life.genny.qwanda.Ask;
import life.genny.qwanda.TaskAsk;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.OutputParam;

public class CheckTasksWorkItemHandler implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	RuntimeEngine runtimeEngine;
	String wClass;
	TaskService taskService;
	KieSession kieSession;

	public <R> CheckTasksWorkItemHandler(Class<R> workflowQueryInterface) {
		this.wClass = workflowQueryInterface.getCanonicalName();
	}

	public <R> CheckTasksWorkItemHandler(Class<R> workflowQueryInterface, KieSession ksession,
			TaskService taskService) {
		this.taskService = taskService;
		this.kieSession = ksession;
		this.wClass = workflowQueryInterface.getCanonicalName();
	}

	public <R> CheckTasksWorkItemHandler(Class<R> workflowQueryInterface, RuntimeEngine rteng, KieSession kieSession) {
		this.runtimeEngine = rteng;
		this.wClass = workflowQueryInterface.getCanonicalName();
		this.taskService = rteng.getTaskService();
		this.kieSession = kieSession;
	}

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

		/* resultMap is used to map the result Value to the output parameters */
		final Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<TaskSummary, Map<String, Object>> taskAskMap = new HashMap<TaskSummary, Map<String, Object>>();

		/* items used to save the extracted input parameters from the custom task */
		Map<String, Object> items = workItem.getParameters();

		GennyToken userToken = (GennyToken) items.get("userToken");
		GennyToken serviceToken = (GennyToken) items.get("serviceToken");

		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);
		OutputParam output = (OutputParam) items.get("output");

		String callingWorkflow = (String) items.get("callingWorkflow");
		if (StringUtils.isBlank(callingWorkflow)) {
			callingWorkflow = "";
		}

		// Extract all the current questions from all the users Tasks
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
		List<TaskSummary> tasks = taskService.getTasksOwnedByStatus(realm + "+" + userCode, statuses, null);

		if (tasks.isEmpty()) {
			resultMap.put("output", output);  // if no tasks then ensure output passed in is passed through
		} else {
			for (TaskSummary taskSummary : tasks) {

				Task task = taskService.getTaskById(taskSummary.getId());
				log.info(callingWorkflow + " Pending Task for " + userToken.getUserCode() + " = " + task.getFormName());
				String formName = task.getFormName();
				output = new OutputParam();
				output.setTypeOfResult("FORMCODE");
				output.setResultCode(formName);
				output.setTargetCode("FRM_CONTENT");
				resultMap.put("output", output);
			}
			if (resultMap.get("oputput")==null) {
				resultMap.put("output", output); // TODO, ugly
			}

		}

		// notify manager that work item has been completed
		try {
			manager.completeWorkItem(workItem.getId(), resultMap);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private Boolean validate(Answer answer) {
		// TODO - check value using regexs
		return true;
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, notifications cannot be aborted
	}

	protected ContentData createTaskContentBasedOnWorkItemParams(KieSession session,
			HashMap<String, Object> taskAsksMap) {
		ContentData content = null;
		Object contentObject = null;
		contentObject = new HashMap<String, Object>(taskAsksMap);
		if (contentObject != null) {
			Environment env = null;
			if (session != null) {
				env = session.getEnvironment();
			}

			content = ContentMarshallerHelper.marshal(null, contentObject, env);
		}
		return content;
	}
}