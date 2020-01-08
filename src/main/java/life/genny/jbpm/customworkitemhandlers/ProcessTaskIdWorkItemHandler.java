package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Task;

import life.genny.qwanda.message.QEventMessage;
import life.genny.utils.OutputParam;

public class ProcessTaskIdWorkItemHandler implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	RuntimeEngine runtimeEngine;
	String wClass;
	KieSession kieSession;
	TaskService taskService;

	public ProcessTaskIdWorkItemHandler() {

	}

	public <R> ProcessTaskIdWorkItemHandler(Class<R> workflowQueryInterface, RuntimeEngine rteng) {
		this.kieSession = rteng.getKieSession();
		this.taskService = rteng.getTaskService();
		this.runtimeEngine = rteng;
		this.wClass = workflowQueryInterface.getCanonicalName();

	}

	public <R> ProcessTaskIdWorkItemHandler(Class<R> workflowQueryInterface, RuntimeEngine rteng, KieSession session) {
		this.kieSession = session;
		this.taskService = rteng.getTaskService();
		this.runtimeEngine = rteng;
		this.wClass = workflowQueryInterface.getCanonicalName();

	}

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		Map<String, Object> resultMap = new ConcurrentHashMap<String, Object>();

		String callingWorkflow = (String)workItem.getParameter("callingWorkflow");
		if (StringUtils.isBlank(callingWorkflow)) {
			callingWorkflow = "";
		}


		QEventMessage msg = (QEventMessage) workItem.getParameter("message");
		String taskIdStr = msg.getData().getValue();
		Long taskId = Long.parseLong(taskIdStr);
		
		log.info(callingWorkflow+" Processing TaskId");


		Task task = taskService.getTaskById(taskId);
		OutputParam output = new OutputParam();
		if (task != null) {
			String formcode = task.getFormName();
			output.setFormCode(formcode, "FRM_CONTENT");
			output.setTypeOfResult("FORMCODE");
			output.setTaskId(task.getId());
			resultMap.put("output", output);
		} else {
			log.error("Task is null for task Id=" + taskId);
		}

		manager.completeWorkItem(workItem.getId(), resultMap);
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		log.warn("Aborting");
	}

}