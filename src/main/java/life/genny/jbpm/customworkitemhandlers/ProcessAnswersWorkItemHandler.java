package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.task.TaskService;

import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.Answers;
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
		
		// Loop through all the answers check their validity and save them.
		List<Answer> validAnswers = new ArrayList<Answer>();
		for (Answer answer : answersToSave.getAnswers()) {
			// check answer
			if (answer.getSourceCode().equals(userToken.getUserCode())) {
				validAnswers.add(answer);
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