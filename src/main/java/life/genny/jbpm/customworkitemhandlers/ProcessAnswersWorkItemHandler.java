package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TransactionRequiredException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.validation.ConstraintViolation;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import life.genny.qwandautils.GennySettings;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.drools.persistence.api.TransactionManager;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.event.KieRuntimeEvent;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskData;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.task.api.TaskModelProvider;
import org.kie.internal.task.api.model.ContentData;
import org.kie.internal.task.api.model.InternalContent;
import org.kie.internal.task.api.model.InternalTask;
import org.kie.internal.task.api.model.InternalTaskData;

import io.vavr.Tuple2;
import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.Answers;
import life.genny.qwanda.Ask;
import life.genny.qwanda.Question;
import life.genny.qwanda.TaskAsk;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwandautils.JsonUtils;
import life.genny.rules.RulesLoader;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.OutputParam;
import life.genny.utils.RulesUtils;
import life.genny.utils.TaskUtils;
import life.genny.utils.VertxUtils;

public class ProcessAnswersWorkItemHandler implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	private static final String[] KNOWN_UT_JNDI_KEYS = new String[] { "UserTransaction", "java:jboss/UserTransaction",
			System.getProperty("jbpm.ut.jndi.lookup") };

	private boolean isJTA = true;
	private boolean sharedEM = false;

	private EntityManagerFactory emf;

	RuntimeEngine runtimeEngine;
	String wClass;
	TaskService taskService;
	KieSession kieSession;
	Environment env = null;

	private static Validator validator;

	public <R> ProcessAnswersWorkItemHandler(Class<R> workflowQueryInterface) {
		this.wClass = workflowQueryInterface.getCanonicalName();
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		if (kieSession != null) {
			env = kieSession.getEnvironment();
		}
		validator = factory.getValidator();

	}

	public <R> ProcessAnswersWorkItemHandler(Class<R> workflowQueryInterface, Environment env,
			TaskService taskService) {
		this.taskService = taskService;
		this.env = env;
		this.wClass = workflowQueryInterface.getCanonicalName();
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		if (kieSession != null) {
			env = kieSession.getEnvironment();
		}
		validator = factory.getValidator();

	}

	public <R> ProcessAnswersWorkItemHandler(Class<R> workflowQueryInterface, RuntimeEngine rteng,
			KieSession kieSession) {
		this.runtimeEngine = rteng;
		this.wClass = workflowQueryInterface.getCanonicalName();
		this.taskService = rteng.getTaskService();
		this.kieSession = kieSession;
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		if (kieSession != null) {
			env = kieSession.getEnvironment();
		}
		validator = factory.getValidator();

	}

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		/* items used to save the extracted input parameters from the custom task */
		Map<String, Object> items = workItem.getParameters();
		OutputParam output = (OutputParam) items.get("output");
		Answers answersToSave = (Answers) items.get("answersToSave");
		GennyToken userToken = (GennyToken) items.get("userToken");
		GennyToken serviceToken = (GennyToken) items.get("serviceToken");
		String callingWorkflow = (String) workItem.getParameter("callingWorkflow");
		if (StringUtils.isBlank(callingWorkflow)) {
			callingWorkflow = "";
		}

		// If no need to process anything then exit
		if ((output == null) || ("NO_PROCESSING".equalsIgnoreCase(output.getTypeOfResult())) || ((answersToSave == null)
				|| (answersToSave.getAnswers() == null) || (answersToSave.getAnswers().isEmpty()))) {
			exitOut(workItem, manager, OutputParam.getNone());
			return;
		}

		// If the answers do not come from the specified source or an admin then ignore.
		// This is just to be quick
		if (!TaskUtils.doValidAnswersExist(answersToSave, userToken)) {
			exitOut(workItem, manager, OutputParam.getNone());
			return;
		}

		BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
		beUtils.setServiceToken(serviceToken);

		// Send back the 'validated' answers
		BaseEntity originalTarget = beUtils.getBaseEntityByCode(answersToSave.getAnswers().get(0).getTargetCode());
		if (originalTarget == null) {
			log.error(callingWorkflow+" Target BaseEntity does not exist ! %s", answersToSave.getAnswers().get(0).getTargetCode());
			exitOut(workItem, manager, OutputParam.getNone());
			return;

		}

		Tuple2<List<Answer>, BaseEntity> answerBes = TaskUtils.gatherValidAnswers(beUtils, answersToSave, userToken); // TODO,
																														// use
																														// an
																														// exception
		// to flushout invalids
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(answerBes._2);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		VertxUtils.writeMsg("webcmds",JsonUtils.toJson(msg)); // send back all the non inferred answers to frontend

		List<Answer> validAnswers = answerBes._1;

		log.info(callingWorkflow + " PROCESS VALID ANSWERS WorkItem Handler *************************");

		// Construct a set of unique AnswerCodes for the incoming Answers
		Map<String, Answer> answerMap2 = constructAnswerMap(answersToSave, userToken); // original
		Map<String, Answer> answerMap = new ConcurrentHashMap<>();
		answerMap.putAll(answerMap2); // working
		Boolean submitDetected = submitDetected(answerMap2); // check if the submit button detected
		String submitCode = getSubmitCode(answerMap2); // check if the submit button detected

		/* resultMap is used to map the result Value to the output parameters */
		Map<TaskSummary, ConcurrentHashMap<String, Object>> taskAskMap = new ConcurrentHashMap<>();

		Map<Long, KieSession> kieSessionMap = new HashMap<>();
		Map<Long, Boolean> mandatoryDoneMap = new ConcurrentHashMap<>();

		List<TaskSummary> tasks = taskService.getTasksOwnedByStatus(userToken.getRealmUserCode(),
				TaskUtils.getTaskStatusList(), null);
		log.info(callingWorkflow+" Tasks="+tasks);

		KieSession kSession = getKieSession(workItem, manager, userToken, callingWorkflow);

		List<TaskAsk> taskAsksProcessed = new CopyOnWriteArrayList<>();

		Boolean primaryFieldDetected = false; // This is set if a PRI_NAME or or PRI_ABN set
		Boolean hasAnsweredTask = false;
		// Save all inferred Answers

		for (TaskSummary taskSummary : tasks) {
			// If the taskSummary is not related to the incoming targetCode then ignore
			if (!originalTarget.getCode().equals(taskSummary.getSubject())) {
				continue; // don't process tasks that are not related to the targetCode
			}
			
			log.info(callingWorkflow+" Checking Task Summary "+taskSummary.getSubject());
			
			Boolean hackTrigger = false; // used for debugging and testing trigger
			mandatoryDoneMap.put(taskSummary.getId(), false);

			Task task = taskService.getTaskById(taskSummary.getId());
			if (task.getTaskData().getStatus().equals(Status.Reserved)) {
				taskService.start(taskSummary.getId(), userToken.getRealmUserCode()); // start!
			}

			TaskData td = task.getTaskData();
	//		Map<String, Object> inputVarsMap = td.getTaskInputVariables();
	//		if (inputVarsMap != null) {
				Boolean liveQuestions = "SEND_INFERRED".equals(td.getFaultName()); //(Boolean) inputVarsMap.get("liveQuestions");
				if (liveQuestions) {
					// send inferred
					BaseEntity inferredBe = new BaseEntity(originalTarget.getCode(), originalTarget.getCode());
					for (Answer ans : answerBes._1) {
						try {
							inferredBe.addAnswer(ans);
						} catch (BadDataException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					log.info(callingWorkflow+" Sending Inferred BE to frontend");
					beUtils.writeMsg(inferredBe);
				}
	//		}

			kieSessionMap.put(task.getId(), kSession);

			ConcurrentHashMap<String, Object> taskAsks = null;

			try {
				taskAsks = TaskUtils.getTaskAsks(taskService, task);
			} catch (Exception e) {
				log.error(callingWorkflow+" Bad TaskAsk issue");
				exitOut(workItem, manager, OutputParam.getNone());
			}

			// Loop through all the answers check their validity and save them.

			answerMap.putAll(answerMap2);
			for (Answer answer : answerMap2.values()) {
				// check answer
				String key = answer.getUniqueCode();
				TaskAsk ask = (TaskAsk) taskAsks.get(key);

				if (ask != null) {
					ask.setValue(answer.getValue());
					if (StringUtils.isBlank(ask.getValue())) {
						ask.setAnswered(false);
					} else {
						ask.setAnswered(true);
					}
					taskAsksProcessed.add(ask); // save for later updating
					validAnswers.add(answer);

					BaseEntity answerTarget = beUtils.getBaseEntityByCode(answer.getTargetCode());
					BaseEntity defBe = beUtils.getDEF(answerTarget);

					log.info("Target DEF identified as "+defBe.getCode()+"!! "+defBe.getName());
					List<String> dependants = beUtils.getDependants(answer.getAttributeCode(), defBe);

					if (dependants != null) {
						for (String dep : dependants) {
							/* Clear Dependant Attributes */
							Answer ans = new Answer(userToken.getUserCode(), answerTarget.getCode(), dep, "");
							validAnswers.add(ans);
							VertxUtils.sendToFrontEnd(userToken, ans);
						}
					}

					if (Boolean.TRUE.equals(answer.getInferred())) {
						// This is a valid answer but has not come from the frondend and is not going to
						// be expected in the task list */
						validAnswers.add(answer);
					}

				} else {
					log.error(callingWorkflow+" Not a valid ASK! %s", key);
				}
			}

			log.info(callingWorkflow+" Saving "+validAnswers.size()+" Valid Answers ...");

			if (Boolean.FALSE.equals(submitDetected)) { // dont save if submit
				beUtils.saveAnswers(validAnswers); // save answers in one big thing
			}

			// check if all mandatory answers done
			Boolean isNowTriggered = false;
			mandatoryDoneMap.put(taskSummary.getId(), true); // set as default true to be falsified if a mandatory field

			TaskAsk submitTaskAsk = null;

			// not answered
			for (Map.Entry<String, Object> entry : taskAsks.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				if (value instanceof String) {
					continue;
				}
				TaskAsk ask = (TaskAsk) value;
				if (((ask.getAsk().getSourceCode() + ":" + ask.getAsk().getTargetCode() + ":FORM_CODE").equals(key))
						|| ((ask.getAsk().getSourceCode() + ":" + ask.getAsk().getTargetCode() + ":TARGET_CODE")
								.equals(key))) {
					continue;
				}

				if (Boolean.TRUE.equals(ask.getAsk().getMandatory()) && Boolean.FALSE.equals(ask.getAnswered())
						&& (!ask.getAsk().getAttributeCode().equals("PRI_SUBMIT"))) {
					// check if already in Be, shouldn't happen but has! where value in be but not
					// picked up in form
					String attributeCode = ask.getAsk().getAttributeCode();
					Optional<EntityAttribute> optEa = originalTarget.findEntityAttribute(attributeCode);
					if (optEa.isPresent()) {
						EntityAttribute ea = optEa.get();
						if (StringUtils.isBlank(ea.getAsString())) {
							mandatoryDoneMap.put(taskSummary.getId(), false);
						} else {
							ask.setAnswered(true);
							ask.setValue(ea.getAsString());
							hasAnsweredTask = true;
						}
					} else {
						mandatoryDoneMap.put(taskSummary.getId(), false);
					}
				}

				if (ask.getFormTrigger() && ask.getAnswered()) {
					isNowTriggered = true;
				}

				if (ask.getAsk().getAttributeCode().equals("PRI_SUBMIT")) {
					submitTaskAsk = ask;
				}

				// These fields are like the Justice League member of all the fields, if they
				// change then we need to change the Drafts menu item names
				InternalTask iTask = (InternalTask) task;

				String processId = iTask.getTaskData().getProcessId();
				processId = processId.replaceAll("_", " ");
				BaseEntity tsBe = beUtils.getBaseEntityByCode(taskSummary.getSubject());
				BaseEntity defBe = beUtils.getDEF(tsBe);
				String beType = "";
				if (defBe != null) {
					beType = defBe.getName();//.capitalize(processId.toLowerCase());
				}

				String description = task.getDescription();
		
				switch (ask.getAsk().getAttributeCode()) {
				case "PRI_NAME":
				case "PRI_ABN":
				case "PRI_FIRSTNAME":
				case "PRI_TRADING_NAME":
						description = setDescription(beType, originalTarget, ask);
					if (!StringUtils.isBlank(description)) {
						iTask.setDescription(description);
					}
					break;
				default:

				}

				// Enable if Dependencies have been met
				Boolean dependenciesMet = beUtils.dependenciesMet(ask.getAsk().getAttributeCode(), validAnswers, originalTarget, defBe);
				if (dependenciesMet != null) {
					TaskUtils.hideTaskQuestion(ask.getAsk(), dependenciesMet, userToken);

					// NOTE: Should be passing a parentCode but don't have access to it
					ShowFrame.sendDefSelectionItems(new BaseEntity[0], defBe, ask.getAsk().getAttributeCode(), userToken, serviceToken, 
							false, originalTarget.getCode(), task.getName(), ask.getAsk().getQuestionCode());
				}

				log.info("TASK-ASK: " + ask);

			}

			if (submitDetected) {
				isNowTriggered = true;
			}

			// check all the tasks to see if any are completed and if so then complete them!
			if ((mandatoryDoneMap.get(taskSummary.getId()) && isNowTriggered)) {
				// Now complete the Task!!
				log.info(callingWorkflow + " Completed TASK " + task.getFormName() + "  hackTrigger = "
						+ (Boolean.TRUE.equals(hackTrigger) ? "TRUE" : "FALSE"));
				taskAskMap.put(taskSummary, taskAsks);
			}


			if (Boolean.TRUE.equals(mandatoryDoneMap.get(taskSummary.getId()))) {
				log.info(callingWorkflow+"  ALL MANDATORY FIELDS HAVE BEEN ANSWERED! for " + task.getName());
				// if there is a submit button then enable it
				if (submitTaskAsk != null) {
					Ask submitAsk = submitTaskAsk.getAsk();
					if (submitAsk.getDisabled()) {
							TaskUtils.enableTaskQuestion(submitAsk, true, userToken);
							log.info(callingWorkflow+" : ***SENT*** ENABLE SUBMIT submit now "+(submitAsk.getDisabled()?"DISABLED":"ENABLED") );
							
					} else {
						log.info(callingWorkflow+" : NO SEND ENABLE SUBMIT -> submit now "+(submitAsk.getDisabled()?"DISABLED":"ENABLED"));
					}
				}

				if (submitDetected) {
					log.info(callingWorkflow+" : ALL MANDATORY FIELDS HAVE BEEN ANSWERED AND SUBMIT DETECTED!");
				}
				// TaskUtils.enableTaskQuestion(Question question,true,
				// originalTarget.getCode(), userToken);
			} else {
				Ask submitAsk = submitTaskAsk.getAsk();
				if (!submitAsk.getDisabled()) {
					TaskUtils.enableTaskQuestion(submitAsk, false, userToken);
					log.info(callingWorkflow+" : ****SENT**** DISABLE SUBMIT -> submit now "+(submitAsk.getDisabled()?"DISABLED":"ENABLED"));
				} else {
					log.info(callingWorkflow+" : NO SEND DISABLE SUBMIT -> submit now "+(submitAsk.getDisabled()?"DISABLED":"ENABLED"));
				}
			}

			Map<String, Object> results = saveTaskData(taskAskMap, kSession, taskSummary, hackTrigger, task, taskAsks);

			if (primaryFieldDetected) {
				TaskUtils.sendTaskAskItems(userToken);
			}

			if (hasAnsweredTask) {
				log.info("Answer has serviced a task... breaking loop!!!");
				break;
			}

		}

		// Now complete the tasks if done
		for (TaskSummary taskSummary : tasks) {
			Map<String, Object> results = taskAskMap.get(taskSummary);
			if (results != null) {
				InternalTask iTask = (InternalTask) taskService.getTaskById(taskSummary.getId());
				log.info(callingWorkflow+" CLOSING task with status "+ iTask.getTaskData().getStatus());

				KieSession taskSession = kieSessionMap.get(iTask.getId());
				if (taskSession == null) {
					log.error(callingWorkflow+" NULL kSession when trying to retrieve kSession for kieSesswionId="+
							iTask.getTaskData().getProcessSessionId());
				}
				results.put("taskid", iTask.getId());
				results.put("userToken", userToken); /* save the latest userToken that actually completes the form */
				results.put("serviceToken",
						serviceToken); /* save the latest userToken that actually completes the form */
				results.put("submitCode", submitCode); /* save the quesiton code of the submit button */
				log.info("submitCode = " + results.get("submitCode"));

				Boolean mandatorysAllDone = mandatoryDoneMap.get(taskSummary.getId());
				if (Boolean.TRUE.equals(mandatorysAllDone)) {
					log.info(callingWorkflow+" : SAVING FORM ANSWERS! " + iTask.getFormName() + ":" + iTask.getSubject()
							+ ":" + userToken.getRealmUserCode());

					saveAnswers(beUtils, answerMap2);
					// delete submit button
					if (submitDetected) {
						beUtils.removeEntityAttribute(originalTarget,"PRI_SUBMIT");
					}

					TaskUtils.sendTaskAskItems(userToken);
					try {
						taskService.complete(iTask.getId(), userToken.getRealmUserCode(), results);
					} catch (Exception e) {
						log.error(callingWorkflow +" ProcessAnswers "+ iTask.getFormName() + ":" + iTask.getSubject()
						+ ":" + userToken.getRealmUserCode()+" Finishing Error "+e.getLocalizedMessage());
					}
					
				}
			}
		}

		finishUp(workItem, manager, output, userToken);
	}

	/**
	 * @param originalTarget
	 * @param ask
	 * @param description
	 * @return
	 */
	private String setDescription(String beCategory, BaseEntity originalTarget, TaskAsk ask) {
		String description = null;
		String name = originalTarget.getValue("PRI_NAME", null);
		if (StringUtils.isBlank(name) && !ask.getAsk().getAttributeCode().equals("PRI_NAME")) {
			if (!StringUtils.isBlank(ask.getValue())) {
				description = beCategory + " - " + ask.getValue();
			}
		} else {
			if (ask.getAsk().getAttributeCode().equals("PRI_NAME")) {
				if (!StringUtils.isBlank(ask.getValue())) {
					description = beCategory + " - " + ask.getValue();
					return description;
				}
			} else {
				if (StringUtils.isBlank(name)) {
					description = beCategory + " - " + ask.getValue();
				}
			}
		}
		if (!StringUtils.isBlank(name)) {
			description = beCategory + " - " + name;
		}
		return description;
	}

	/**
	 * @param taskAskMap
	 * @param kSession
	 * @param taskSummary
	 * @param hackTrigger
	 * @param task
	 * @param taskAsks
	 * @return
	 */
	private Map<String, Object> saveTaskData(Map<TaskSummary, ConcurrentHashMap<String, Object>> taskAskMap,
			KieSession kSession, TaskSummary taskSummary, Boolean hackTrigger, Task task,
			ConcurrentHashMap<String, Object> taskAsks) {
		Map<String, Object> results = taskAskMap.get(taskSummary);
		if ((results == null) && (!hackTrigger)) {
			// Now save back to Task
			Object contentObject = null;
			contentObject = new ConcurrentHashMap<String, Object>(taskAsks);
			Environment env2 = null;
			if (kSession != null) {
				env2 = kSession.getEnvironment();
			}
			ContentData contentData = ContentMarshallerHelper.marshal(task, contentObject, env2);

			Content content = TaskModelProvider.getFactory().newContent();
			((InternalContent) content).setContent(contentData.getContent());
			Set<ConstraintViolation<Content>> constraintViolations = validator.validate(content);
			if (constraintViolations.isEmpty()) {
				log.info("ProcessAnswers: Persisting taskContent");
				persist(content, env2);
				InternalTask iTask = (InternalTask) taskService.getTaskById(task.getId());
				InternalTaskData iTaskData = (InternalTaskData) iTask.getTaskData();
				iTaskData.setDocument(content.getId(), contentData);
				// iTask.setDescription(q.getName()); TODO Add custom task name here
				iTask.setTaskData(iTaskData);
			} else {
				// Hibernate validation error!
				for (ConstraintViolation<Content> constraintViolation : constraintViolations) {
					log.error(constraintViolation.getMessage());
				}
			}

		}
		return results;
	}

	/**
	 * @param beUtils
	 * @param answerMap2
	 */
	private void saveAnswers(BaseEntityUtils beUtils, Map<String, Answer> answerMap2) {
		synchronized (this) {
			log.info("processAnswers: Saving Answers :" + answerMap2.values());
			List<Answer> saveAnswers = new CopyOnWriteArrayList<>(answerMap2.values());
			saveAnswers.parallelStream().forEach((i) -> {
				i.setChangeEvent(false);
			}); // do not feed back into rules with attribbute change
			beUtils.saveAnswers(saveAnswers);
		}
	}

	/**
	 * @param workItem
	 * @param manager
	 * @param userToken
	 * @param callingWorkflow
	 * @param task
	 * @return
	 */
	private KieSession getKieSession(WorkItem workItem, WorkItemManager manager, GennyToken userToken,
			String callingWorkflow) {
		// Save the kieSession for this task
		KieSession kSession = null;
		if (VertxUtils.cachedEnabled) {
			kSession = kieSession;
		} else {
			try {
				if (Boolean.TRUE.equals(GennySettings.useSingleton)) {
					kSession = RulesLoader.kieSessionMap.get(userToken.getRealm());
				} else {
					kSession = RulesLoader.kieSessionMap.get(userToken.getSessionCode());
				}
			} catch (Exception ke) {
				log.error("kieSession could not be loaded ");
			}
		}
		if (kSession == null) {
			log.error("%s exiting out early due to nokSession", callingWorkflow);
			this.exitOut(workItem, manager, OutputParam.getNone());
			return null;
		} else {
			log.info("Found session to restore for ksid=");
		}
		return kSession;
	}

	private Map<String, Answer> constructAnswerMap(Answers answersToSave, GennyToken userToken) {
		Map<String, Answer> answerMap = new ConcurrentHashMap<>();

		for (Answer answer : answersToSave.getAnswers()) {
			Boolean validAnswer = TaskUtils.validate(answer, userToken);
			if (Boolean.TRUE.equals(validAnswer)) {
				String key = answer.getSourceCode() + ":" + answer.getTargetCode() + ":" + answer.getAttributeCode();
				answerMap.put(key, answer);
			}
		}
		return answerMap;

	}

	private Boolean submitDetected(Map<String, Answer> answerMap) {
		for (Answer answer : answerMap.values()) {
			if ("PRI_SUBMIT".equals(answer.getAttributeCode())) {
				return true;
			}
		}
		return false;

	}

	private String getSubmitCode(Map<String, Answer> answerMap) {
		for (Answer answer : answerMap.values()) {
			if ("PRI_SUBMIT".equals(answer.getAttributeCode())) {
				return answer.getValue();
			}
		}
		return null;

	}

	private void exitOut(WorkItem workItem, WorkItemManager manager, OutputParam output) {
		final Map<String, Object> resultMap = new ConcurrentHashMap<>();
		resultMap.put("output", output);

		if (kieSession != null) {
			kieSession.getWorkItemManager().completeWorkItem(workItem.getId(), resultMap);
		} else {
			manager.completeWorkItem(workItem.getId(), resultMap);
		}
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, notifications cannot be aborted
	}

	public void finishUp(WorkItem workItem, WorkItemManager manager, OutputParam output, GennyToken userToken) {
		final Map<String, Object> resultMap = new ConcurrentHashMap<>();

		resultMap.put("output", output);
		// check if result has nulls in it
		for (String key : resultMap.keySet()) {
			if (key == null) {
				log.error("processAnswers: BAD NULL KEY IN RESULT SET");
			} else if (resultMap.get(key) == null) {
				log.error("processAnswers: BAD NULL MAP ENTRY IN RESULT SET for KEY = %s", key);
			} else if (resultMap.get(key) instanceof OutputParam) {
				OutputParam o = (OutputParam) resultMap.get(key);
				Map<String, String> map = o.getAttributeTargetCodeMap();
				if (map == null) {
					log.error("processAnswers: BAD NULL KEY IN OUTPUT PARAM MAP");
					o.setAttributeTargetCodeMap(new HashMap<String, String>());
				} else {
					boolean ok = true;
					for (String okey : map.keySet()) {
						if (okey == null) {
							ok = false;
							log.error("processAnswers : BAD NULL KEY IN RESULT SET - OutputParam map ");
						} else if (map.get(key) == null) {
							log.error("processAnswers : BAD NULL KEY IN RESULT SET VALUE - OutputParam map - key= "
									+ okey);
							ok = false;
						}
					}
					if (ok) {
						log.info("processAnswers :  OutputParam map is ok");
					}

					log.info("processAnswers: resultMap OPutputParam seems fine");
				}
			} else {
				log.info("processAnswers: resultMap  seems fine");
			}
		}
		if (kieSession != null) {
			kieSession.getWorkItemManager().completeWorkItem(workItem.getId(), resultMap);
		} else {
			manager.completeWorkItem(workItem.getId(), resultMap);
		}
		TaskUtils.sendTaskAskItems(userToken);
	}

	/**
	 * This method opens a new transaction, if none is currently running, and joins
	 * the entity manager/persistence context to that transaction.
	 * 
	 * @param em The entity manager we're using.
	 * @return {@link UserTransaction} If we've started a new transaction, then we
	 *         return it so that it can be closed.
	 * @throws NotSupportedException
	 * @throws SystemException
	 * @throws Exception             if something goes wrong.
	 */
	private Object joinTransaction(EntityManager em) {
		boolean newTx = false;
		UserTransaction ut = null;

		if (isJTA) {
			try {
				em.joinTransaction();
			} catch (TransactionRequiredException e) {
				ut = findUserTransaction();
				try {
					if (ut != null && ut.getStatus() == javax.transaction.Status.STATUS_NO_TRANSACTION) {
						ut.begin();
						newTx = true;
						// since new transaction was started em must join it
						em.joinTransaction();
					}
				} catch (Exception ex) {
					throw new IllegalStateException("Unable to find or open a transaction: " + ex.getMessage(), ex);
				}

				if (!newTx) {
					// rethrow TransactionRequiredException if UserTransaction was not found or
					// started
					throw e;
				}
			}

			if (newTx) {
				return ut;
			}
		}
		// else {
		// EntityTransaction tx = em.getTransaction();
		// if( ! tx.isActive() ) {
		// tx.begin();
		// return tx;
		// }
		// }
		return null;
	}

	protected static UserTransaction findUserTransaction() {
		InitialContext context = null;
		try {
			context = new InitialContext();
			return (UserTransaction) context.lookup("java:comp/UserTransaction");
		} catch (NamingException ex) {

			for (String utLookup : KNOWN_UT_JNDI_KEYS) {
				if (utLookup != null) {
					try {
						UserTransaction ut = (UserTransaction) context.lookup(utLookup);
						return ut;
					} catch (NamingException e) {
						log.debug("User Transaction not found in JNDI under {}", utLookup);

					}
				}
			}
			log.warn("No user transaction found under known names");
			return null;
		}
	}

	/**
	 * This method closes the entity manager and transaction. It also makes sure
	 * that any objects associated with the entity manager/persistence context are
	 * detached.
	 * </p>
	 * Obviously, if the transaction returned by the
	 * {@link #joinTransaction(EntityManager)} method is null, nothing is done with
	 * the transaction parameter.
	 * 
	 * @param em The entity manager.
	 * @param ut The (user) transaction.
	 */
	private void leaveTransaction(EntityManager em, Object transaction) {
		if (isJTA) {
			try {
				if (transaction != null) {
					// There's a tx running, close it.
					((UserTransaction) transaction).commit();
				}
			} catch (Exception e) {
				log.error("Unable to commit transaction: ", e);
			}
		} else {
			if (transaction != null) {
				((EntityTransaction) transaction).commit();
			}
		}

		if (!sharedEM) {
			try {
				em.flush();
				em.close();
			} catch (Exception e) {
				log.error("Unable to close created EntityManager: {}", e.getMessage(), e);
			}
		}
	}

	/**
	 * This method persists the entity given to it.
	 * </p>
	 * This method also makes sure that the entity manager used for persisting the
	 * entity, joins the existing JTA transaction.
	 * 
	 * @param entity An entity to be persisted.
	 */
	private void persist(Object entity, Environment env) {
		EntityManager em = getEntityManager(env);
		Object tx = joinTransaction(em);
		em.persist(entity);
		leaveTransaction(em, tx);
	}

	/**
	 * This method creates a entity manager.
	 */
	private EntityManager getEntityManager(Environment env) {

		// Environment env = event.getKieRuntime().getEnvironment();

		/**
		 * It's important to set the sharedEM flag with _every_ operation otherwise,
		 * there are situations where: 1. it can be set to "true" 2. something can
		 * happen 3. the "true" value can no longer apply (I've seen this in debugging
		 * logs.. )
		 */
		sharedEM = false;
		if (emf != null) {
			return emf.createEntityManager();
		} else if (env != null) {
			EntityManagerFactory emf = (EntityManagerFactory) env.get(EnvironmentName.ENTITY_MANAGER_FACTORY);

			// first check active transaction if it contains entity manager
			EntityManager em = getEntityManagerFromTransaction(env);

			if (em != null && em.isOpen() && em.getEntityManagerFactory().equals(emf)) {
				sharedEM = true;
				return em;
			}
			// next check the environment itself
			em = (EntityManager) env.get(EnvironmentName.CMD_SCOPED_ENTITY_MANAGER);
			if (em != null) {
				sharedEM = true;
				return em;
			}
			// lastly use entity manager factory
			if (emf != null) {
				return emf.createEntityManager();
			}
		}
		throw new RuntimeException("Could not find or create a new EntityManager!");
	}

	protected EntityManager getEntityManagerFromTransaction(Environment env) {
		if (env.get(EnvironmentName.TRANSACTION_MANAGER) instanceof TransactionManager) {
			TransactionManager txm = (TransactionManager) env.get(EnvironmentName.TRANSACTION_MANAGER);
			EntityManager em = (EntityManager) txm.getResource(EnvironmentName.CMD_SCOPED_ENTITY_MANAGER);
			return em;
		}

		return null;
	}

}
