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

public class ProcessAnswersWorkItemHandler implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	RuntimeEngine runtimeEngine;
	String wClass;
	TaskService taskService;
	KieSession kieSession;

	public <R> ProcessAnswersWorkItemHandler(Class<R> workflowQueryInterface) {
		this.wClass = workflowQueryInterface.getCanonicalName();
	}

	public <R> ProcessAnswersWorkItemHandler(Class<R> workflowQueryInterface, KieSession ksession,
			TaskService taskService) {
		this.taskService = taskService;
		this.kieSession = ksession;
		this.wClass = workflowQueryInterface.getCanonicalName();
	}

	public <R> ProcessAnswersWorkItemHandler(Class<R> workflowQueryInterface, RuntimeEngine rteng,
			KieSession kieSession) {
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

		Answers answersToSave = (Answers) items.get("answersToSave");
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
		log.info("Tasks=" + tasks);

		for (TaskSummary taskSummary : tasks) {

			Task task = taskService.getTaskById(taskSummary.getId());
			if (task.getTaskData().getStatus().equals(Status.Reserved)) {
				taskService.start(taskSummary.getId(), realm + "+" + userCode); // start!
			}
			Long docId = task.getTaskData().getDocumentContentId();
			Content c = taskService.getContentById(docId);
			HashMap<String, Object> taskAsks = (HashMap<String, Object>) ContentMarshallerHelper
					.unmarshall(c.getContent(), null);
			// Loop through all the answers check their validity and save them.
			List<Answer> validAnswers = new ArrayList<Answer>();
			List<TaskAsk> taskAsksProcessed = new ArrayList<TaskAsk>();
			if (answersToSave != null) {
				for (Answer answer : answersToSave.getAnswers()) {
					// check answer
					if (answer.getSourceCode().equals(userToken.getUserCode())) {
						String key = answer.getSourceCode() + ":" + answer.getTargetCode() + ":"
								+ answer.getAttributeCode();
						TaskAsk ask = (TaskAsk) taskAsks.get(key);
						if (ask != null) {

							// Now confirm the validity of the value
							Boolean validated = validate(answer);

							if (validated) {
								ask.setValue(answer.getValue());
								// checek
								taskAsksProcessed.add(ask); // save for later updating
								validAnswers.add(answer);

							} else {
								if (ask.getAsk().getMandatory()) { // if an invalid result sent, then clear this mandatory ask
									ask.setAnswered(false); // revert to unanswered
									// do not save this invalid answer so we save an empty value
									ask.setValue("");
									taskAsksProcessed.add(ask); // save for later updating
									answer.setValue("");
									validAnswers.add(answer);
								}
							}
						} else {
							log.error("Not a valid ASK! " + key);
						}
					}
				}
				log.info(callingWorkflow + " Saving " + validAnswers.size() + " Valid Answers ...");
				if (validAnswers.size() > 0) {
					beUtils.saveAnswers(validAnswers); // save answers in one big thing

					// tick off the valid answers
					for (TaskAsk ta : taskAsksProcessed) {
						ta.setAnswered(true);
					}

					// check if all mandatory answers done
					Boolean allMandatoryTicked = true;
					Boolean isCreateOnTrigger = false;
					Boolean isNowTriggered = false;
					for (String key : taskAsks.keySet()) {
						TaskAsk ask = (TaskAsk) taskAsks.get(key);
						if (ask.getAsk().getMandatory()) {
							if (!ask.getAnswered()) {
								allMandatoryTicked = false;
							}
						}
						if (ask.getCreateOnTrigger()) {
							isCreateOnTrigger = true; // ok, so if any trigger questions are answered and all mandatory
														// questions are answered then create!
						}
						if (ask.getFormTrigger() && ask.getAnswered()) {
							isNowTriggered = true;
						}
						// HACK TODO TODO TODO
						if (ask.getAsk().getAttributeCode().startsWith("QQQ_QUESTION_GROUP_BUT")) {
							isNowTriggered = true;
						}
						log.info(callingWorkflow + " TASK-ASK:" + ask);
					}
					// check all the tasks to see if any are completed and if so then complete them!
					if (allMandatoryTicked && isNowTriggered) {
						if (isCreateOnTrigger) {
							// Okay, so grab the temporary target BE and make it the final target BE!
							log.info(callingWorkflow + " Creating actual Target BE from Temporary BE for "
									+ task.getFormName());
						}
						// Now complete the Task!!
						log.info(callingWorkflow + " Completed TASK " + task.getFormName());
						taskAskMap.put(taskSummary, taskAsks);
					}
					// Now save back to Task
					// ((InternalTaskService) taskService).deleteContent(task.getId(), c.getId());
					// ((InternalTaskService) taskService).addContent(task.getId(), taskAsks);
					EntityManager em = (EntityManager) this.kieSession.getEnvironment()
							.get(EnvironmentName.APP_SCOPED_ENTITY_MANAGER);
					// ContentData cd = createTaskContentBasedOnWorkItemParams(this.kieSession,
					// taskAsks);
					Object contentObject = null;
					contentObject = new HashMap<String, Object>(taskAsks);
					Environment env = null;
					if (kieSession != null) {
						env = kieSession.getEnvironment();
					}

					ContentData contentData = ContentMarshallerHelper.marshal(task, contentObject, env);
//	            persistenceContext.persistContent(content);
//			  persistenceContext.setOutputToTask(content, content, task);
//             em.persist(content);
					// taskService..addContent(task.getId(), content);
					Content content = TaskModelProvider.getFactory().newContent();
					((InternalContent) content).setContent(contentData.getContent());
					// kieSession.
					// TaskContext ctx = (TaskContext) context;
					// TaskPersistenceContext persistenceContext = ctx.getPersistenceContext();
					// persistenceContext.persistContent(content);
					em.persist(content);
					InternalTask iTask = (InternalTask) task;
					InternalTaskData iTaskData = (InternalTaskData) iTask.getTaskData();
					iTaskData.setDocument(content.getId(), contentData);
					iTask.setTaskData(iTaskData);
					// persistenceContext.setDocumentToTask(content, contentData, task);
					// task.getTaskData().setDocument(content.getId(), content);

					// em.merge(cd);
					Task task2 = taskService.getTaskById(taskSummary.getId());

					Long docId2 = task2.getTaskData().getDocumentContentId();
					Content c2 = taskService.getContentById(docId2);
					HashMap<String, Object> taskAsks2 = (HashMap<String, Object>) ContentMarshallerHelper
							.unmarshall(content.getContent(), this.kieSession.getEnvironment());
					System.out.println(taskAsks2);
				}
			}
		}
		// Now complete the tasks if done
		manager.completeWorkItem(workItem.getId(), resultMap);

		for (TaskSummary taskSummary : tasks) {
			Map<String, Object> results = taskAskMap.get(taskSummary);
			if (results != null) {
				InternalTask iTask = (InternalTask) taskService.getTaskById(taskSummary.getId());
				System.out.println(callingWorkflow+" closing task with status "+iTask.getTaskData().getStatus());
				taskService.complete(taskSummary.getId(), realm + "+" + userCode, results);
			}
		}
		return;

		// notify manager that work item has been completed
		// manager.completeWorkItem(workItem.getId(), resultMap);

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