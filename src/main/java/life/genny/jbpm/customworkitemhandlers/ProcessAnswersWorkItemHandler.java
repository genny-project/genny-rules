package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
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
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.task.api.TaskModelProvider;
import org.kie.internal.task.api.model.ContentData;
import org.kie.internal.task.api.model.InternalContent;
import org.kie.internal.task.api.model.InternalTask;
import org.kie.internal.task.api.model.InternalTaskData;

import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.Answers;
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
		validator = factory.getValidator();

	}

	public <R> ProcessAnswersWorkItemHandler(Class<R> workflowQueryInterface, Environment env,
			TaskService taskService) {
		this.taskService = taskService;
		this.env = env;
		this.wClass = workflowQueryInterface.getCanonicalName();
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();

	}

	public <R> ProcessAnswersWorkItemHandler(Class<R> workflowQueryInterface, RuntimeEngine rteng,
			KieSession kieSession) {
		this.runtimeEngine = rteng;
		this.wClass = workflowQueryInterface.getCanonicalName();
		this.taskService = rteng.getTaskService();
		this.kieSession = kieSession;
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();

	}

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		
		String callingWorkflow = (String) workItem.getParameter("callingWorkflow");
		if (StringUtils.isBlank(callingWorkflow)) {
			callingWorkflow = "";
		}

		log.info(callingWorkflow+" PROCESS ANSWERS WorkItem Handler *************************");
		/* resultMap is used to map the result Value to the output parameters */
		final Map<String, Object> resultMap = new ConcurrentHashMap<String, Object>();
		Map<TaskSummary, ConcurrentHashMap<String, Object>> taskAskMap = new ConcurrentHashMap<TaskSummary, ConcurrentHashMap<String, Object>>();

		/* items used to save the extracted input parameters from the custom task */
		Map<String, Object> items = workItem.getParameters();
		Map<Long, KieSession> kieSessionMap = new HashMap<Long, KieSession>();
		Map<Long,Boolean> mandatoryDoneMap = new ConcurrentHashMap<Long,Boolean>();

		OutputParam output = (OutputParam) items.get("output");
		
		if ((output==null) || (output.getTypeOfResult().equalsIgnoreCase("NO_PROCESSING")))
		{
			output = new OutputParam();
			output.setFormCode("NONE", "NONE");
			output.setResultCode("NONE");
			output.setTypeOfResult("NO_PROCESSING");
			resultMap.put("output", output);

			if (kieSession != null) {
				kieSession.getWorkItemManager().completeWorkItem(workItem.getId(), resultMap);
			} else {
				manager.completeWorkItem(workItem.getId(), resultMap);
			}

			return;
		}
		
		GennyToken userToken = (GennyToken) items.get("userToken");
		GennyToken serviceToken = (GennyToken) items.get("serviceToken");
		String formCode = "NONE";
		String targetCode = "NONE";

		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);

		Answers answersToSave = (Answers) items.get("answersToSave");

		// Extract all the current questions from all the users Tasks
		List<Status> statuses = new CopyOnWriteArrayList<Status>();
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

		KieServices ks = KieServices.Factory.get();
		KieBase kieBase = RulesLoader.getKieBaseCache().get(realm); // kieContainer.getKieBase("defaultKieBase"); //
																	// this name is supposedly found in the kmodule.xml
																	// in META-INF in widlfy-rulesservice

		List<TaskSummary> tasks = taskService.getTasksOwnedByStatus(realm + "+" + userCode, statuses, null);
		log.info("Tasks=" + tasks);
		// Quick answer validation
		Boolean validAnswersExist = false;
		Map<String, Answer> answerMap = new ConcurrentHashMap<String, Answer>();
		Map<String, Answer> answerMap2 = new ConcurrentHashMap<String, Answer>();
		Boolean exitOut = false;
		Boolean submitDetected = false;
		
		if ((answersToSave == null)||(answersToSave.getAnswers()==null)||(answersToSave.getAnswers().isEmpty())) {
			output = new OutputParam();
			output.setTypeOfResult("NO_PROCESSING");
			output.setFormCode("NONE", "NONE");
			resultMap.put("output", output);
			if (kieSession != null) {
				kieSession.getWorkItemManager().completeWorkItem(workItem.getId(), resultMap);
			} else {
				manager.completeWorkItem(workItem.getId(), resultMap);
			}
			return;
		}
		
		
		List<Answer> answersToSave2 = new CopyOnWriteArrayList<>(answersToSave.getAnswers());
		for (Answer answer : answersToSave2) {
			Boolean validAnswer = validate(answer, userToken);
			// Quick and dirty ...
			if (validAnswer) {
				validAnswersExist = true;
				String key = answer.getSourceCode() + ":" + answer.getTargetCode() + ":" + answer.getAttributeCode();
				answerMap2.put(key, answer);
				if (answer.getAttributeCode().equals("PRI_SUBMIT")) {
					submitDetected = true;
				}
				if (answer.getAttributeCode().equals("PRI_ADDRESS_FULL")) {
					/* send full address back to frontend */

					BaseEntity be = beUtils.getBaseEntityByCode(answer.getTargetCode());
					try {
						Attribute attribute = RulesUtils.getAttribute("PRI_ADDRESS_FULL", userToken.getToken());
						answer.setAttribute(attribute);
						be.addAnswer(answer);
						QDataBaseEntityMessage msg = new QDataBaseEntityMessage(be);
						msg.setToken(userToken.getToken());
						VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg));
					} catch (BadDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			}

		}
		if ((!validAnswersExist) || exitOut || (answerMap2.isEmpty())) {
			log.error(callingWorkflow + " exiting out early due to no valid answers nor pri_submit");
			output.setResultCode("NONE");
			output.setTypeOfResult("NONE");
			resultMap.put("output", output);
			manager.completeWorkItem(workItem.getId(), resultMap);
			return;
		}

		for (TaskSummary taskSummary : tasks) {
			Boolean hackTrigger = false; // used for debugging and testing trigger
			mandatoryDoneMap.put(taskSummary.getId(), false);
			Task task = taskService.getTaskById(taskSummary.getId());
			if (task.getTaskData().getStatus().equals(Status.Reserved)) {
				taskService.start(taskSummary.getId(), realm + "+" + userCode); // start!
			}
			if (kieSession != null) {
				env = kieSession.getEnvironment();
			}
			// Save the kieSession for this task
			KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();
			KieSession kSession = null;
			if (VertxUtils.cachedEnabled) {
				kSession = kieSession;
			} else {
				Long sessionId = task.getTaskData().getProcessSessionId();
				try {
					if(GennySettings.useSingleton) {
						kSession = RulesLoader.kieSessionMap.get(userToken.getRealm());
					} else {
						kSession = RulesLoader.kieSessionMap.get(userToken.getSessionCode());
					}
//					KieSession kSession = ks.getStoreServices().loadKieSession(sessionId, kieBase,
//						ksconf, env);
				} catch (Exception ke) {
					log.error("kieSession could not be loaded "+sessionId);
				}
			}
			if (kSession == null) {
				log.error("Cannot find session to restore for ksid=" + task.getTaskData().getProcessSessionId());
				log.error(callingWorkflow + " exiting out early due to nokSession");
				output.setResultCode("NONE");
				resultMap.put("output", output);
				manager.completeWorkItem(workItem.getId(), resultMap);
				return;
			} else {
				log.info("Found session to restore for ksid=" + task.getTaskData().getProcessSessionId());
			}
			kieSessionMap.put(task.getId(), kSession);

			Long docId = task.getTaskData().getDocumentContentId();
			Content c = taskService.getContentById(docId);
			if (c == null) {
				log.error("*************** Task content is NULL *********** ABORTING");
				return;
			}
			HashMap<String, Object> taskAsks2 = null;
			ConcurrentHashMap<String, Object> taskAsks = null;
			synchronized (this) {
				taskAsks2 = (HashMap<String, Object>) ContentMarshallerHelper.unmarshall(c.getContent(), null);

				taskAsks = new ConcurrentHashMap<String, Object>(taskAsks2);
			}

			formCode = (String) taskAsks.get("FORM_CODE");
			targetCode = (String) taskAsks.get("TARGET_CODE");
			// Loop through all the answers check their validity and save them.
			List<Answer> validAnswers = new CopyOnWriteArrayList<Answer>();
			List<TaskAsk> taskAsksProcessed = new CopyOnWriteArrayList<TaskAsk>();

			if (!answerMap2.isEmpty()) {
				answerMap.putAll(answerMap2);
				for (Answer answer : answerMap2.values()) {
					// check answer
					if (answer.getSourceCode().equals(userToken.getUserCode())) {
						// HACK! TODO
//						if (answer.getAttributeCode().equals("PRI_EVENT")) {
//							answer.setAttributeCode("PRI_SUBMIT");
//							finishUp = true;
//							hackTrigger = true;
//						}
						String key = answer.getSourceCode() + ":" + answer.getTargetCode() + ":"
								+ answer.getAttributeCode();
						TaskAsk ask = (TaskAsk) taskAsks.get(key);
						if (ask != null) {

							// Now confirm the validity of the value
							Boolean validated = validate(answer, userToken);

							if (validated) {
								ask.setValue(answer.getValue());
								// check
								taskAsksProcessed.add(ask); // save for later updating
								validAnswers.add(answer);
								//if (!answer.getAttributeCode().equals("PRI_SUBMIT")) {
								//	validAnswers.add(answer);
								//}
								answerMap.remove(key);

							} else {
								if (ask.getAsk().getMandatory()) { // if an invalid result sent, then clear this
																	// mandatory ask
									ask.setAnswered(false); // revert to unanswered
									// do not save this invalid answer so we save an empty value
									ask.setValue("");
									taskAsksProcessed.add(ask); // save for later updating
									answer.setValue("");
									validAnswers.add(answer);
									//if (!answer.getAttributeCode().equals("PRI_SUBMIT")) {
									//	validAnswers.add(answer);
									//}
									answerMap.remove(key);
								}
							}
						} else {
							if (answer.getInferred()) {
								// This is a valid answer but has not come from the frondend and is not going to
								// be expected in the task list */
								validAnswers.add(answer);
								//if (!answer.getAttributeCode().equals("PRI_SUBMIT")) {
								//	validAnswers.add(answer);
								//}
								answerMap.remove(key);
							} else {
								log.error("Not a valid ASK! " + key);
							}
						}
					}
				}

				log.info(callingWorkflow + " Saving " + validAnswers.size() + " Valid Answers ...");

				if (validAnswers.size() > 0) {
					if (!submitDetected) { // dont save if submit
						beUtils.saveAnswers(validAnswers); // save answers in one big thing
					}

					// tick off the valid answers
					for (TaskAsk ta : taskAsksProcessed) {
						ta.setAnswered(true);
					}

					// check if all mandatory answers done
					Boolean allMandatoryTicked = true;
					Boolean isCreateOnTrigger = false;
					Boolean isNowTriggered = false;
					mandatoryDoneMap.put(taskSummary.getId(), true);  // set as default true to be falsified if a mandatory field not answered
					for (String key : taskAsks.keySet()) {

						if (taskAsks.get(key) instanceof String) {
							continue;
						}
						TaskAsk ask = (TaskAsk) taskAsks.get(key);
						if (((ask.getAsk().getSourceCode() + ":" + ask.getAsk().getTargetCode() + ":FORM_CODE")
								.equals(key))
								|| ((ask.getAsk().getSourceCode() + ":" + ask.getAsk().getTargetCode() + ":TARGET_CODE")
										.equals(key))) {
							continue;
						}
						if (ask.getAsk().getMandatory()) {
							
							if (!ask.getAnswered()) {
								// check if already in Be, shouldn't happen but has! wheree value in be but not picked up in form
								BaseEntity be = beUtils.getBaseEntityByCode(ask.getAsk().getTargetCode());
								String attributeCode = ask.getAsk().getAttributeCode();
								Optional<EntityAttribute> optEa = be.findEntityAttribute(attributeCode);
								if (optEa.isPresent()) {
									EntityAttribute ea = optEa.get();
									if (StringUtils.isBlank(ea.getAsString())) {
										allMandatoryTicked = false;
										mandatoryDoneMap.put(taskSummary.getId(), false);
									} else {
										ask.setAnswered(true);
										ask.setValue(ea.getAsString());
									}
								} else {
									allMandatoryTicked = false;
									mandatoryDoneMap.put(taskSummary.getId(), false);
								}
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
						if ((ask.getAsk().getAttributeCode().equals("PRI_SUBMIT")
								&& (ask.getAnswered())/* ||ask.getAsk().getAttributeCode().equals("PRI_SUBMIT") */)) {
							log.info(callingWorkflow + " PRI_SUBMIT detected ");
							hackTrigger = true;
							isNowTriggered = true;
						}
//						if ((ask.getAsk().getAttributeCode().equals("PRI_ADDRESS_FULL")) && (ask.getAnswered())) {
//							log.info(callingWorkflow+" PRI_FULL_ADDRESS detected and is answered");
//							hackTrigger = true;
//						}

						log.info(callingWorkflow + " TASK-ASK:" + ask);
					}
					// check all the tasks to see if any are completed and if so then complete them!
					if ((mandatoryDoneMap.get(taskSummary.getId()) && isNowTriggered) && hackTrigger) {
						if (isCreateOnTrigger) {
							// Okay, so grab the temporary target BE and make it the final target BE!
							log.info(callingWorkflow + " Creating actual Target BE from Temporary BE for "
									+ task.getFormName());
						}
						// Now complete the Task!!
						log.info(callingWorkflow + " Completed TASK " + task.getFormName()+"  hackTrigger = "+(hackTrigger?"TRUE":"FALSE"));
						taskAskMap.put(taskSummary, taskAsks);
					}
					Map<String, Object> results = taskAskMap.get(taskSummary);
					if ((results == null) && (!hackTrigger)) {
						// Now save back to Task
//						EntityManager em = (EntityManager) kSession.getEnvironment()
//								.get(EnvironmentName.APP_SCOPED_ENTITY_MANAGER);
						// taskAsks);
						Object contentObject = null;
						contentObject = new ConcurrentHashMap<String, Object>(taskAsks);
						Environment env2 = null;
						if (kSession != null) {
							env2 = kSession.getEnvironment();
						}
						//synchronized (this) {
							ContentData contentData = ContentMarshallerHelper.marshal(task, contentObject, env2);

							Content content = TaskModelProvider.getFactory().newContent();
							((InternalContent) content).setContent(contentData.getContent());
							Set<ConstraintViolation<Content>> constraintViolations = validator.validate(content);
							if (constraintViolations.size() == 0) {
								log.info("ProcessAnswers: Persisting taskContent");
								persist(content,env2);
								//Object tx = joinTransaction(em);
								//em.persist(content);
								//leaveTransaction(em, tx);
								InternalTask iTask = (InternalTask) taskService.getTaskById(task.getId());
								InternalTaskData iTaskData = (InternalTaskData) iTask.getTaskData();
								iTaskData.setDocument(content.getId(), contentData);
							//	iTask.setDescription(q.getName()); TODO Add custom task name here
								iTask.setTaskData(iTaskData);
							} else {
								// Hibernate validation error!
								for (ConstraintViolation<Content> constraintViolation : constraintViolations) {
									log.error(constraintViolation.getMessage());
								}
							}
					//	}
					}
				}
			}
			if (mandatoryDoneMap.get(taskSummary.getId())) {
				log.info("processAnswers: ALL MANDATORY FIELDS HAVE BEEN ANSWERED! for "+task.getName());
			}
		}

		answerMap2.keySet().removeAll(answerMap.keySet());

		if (validAnswersExist) {
			// Check that the form answer is allowed
			// ugly hack
			Boolean uglySkip = false;
			if (answerMap2.values().size() == 1) {
				String key = answerMap2.keySet().toArray(new String[0])[0];
				String[] keySplit = key.split(":");
				if ("PRI_SUBMIT".equals(keySplit[2])) {
					uglySkip = true;
				}
			}
			if ((!uglySkip) && (!answerMap2.values().isEmpty())) { // don't save submit button
				synchronized (this) {
					log.info("processAnswers: Saving Answers :" + answerMap2.values());
					List<Answer> saveAnswers = new CopyOnWriteArrayList<>(answerMap2.values());
					saveAnswers.parallelStream().forEach((i) -> {i.setChangeEvent(false);}); // do not feed back into rules with attribbute change
					beUtils.saveAnswers(saveAnswers);
				}
			}
		}

		// synchronized (this) {
		// Now complete the tasks if done

		for (TaskSummary taskSummary : tasks) {
			Map<String, Object> results = taskAskMap.get(taskSummary);
			if (results != null) {
				InternalTask iTask = (InternalTask) taskService.getTaskById(taskSummary.getId());
				log.info(callingWorkflow + " CLOSING task with status " + iTask.getTaskData().getStatus());
//				log.info("####### processAnswers! sessionId=" + iTask.getTaskData().getProcessSessionId()
//						+ " with status " + iTask.getTaskData().getStatus());

				KieSession kSession = kieSessionMap.get(iTask.getId());
				if (kSession == null) {
					log.error("NULL kSession when trying to retrieve kSession for kieSesswionId="
							+ iTask.getTaskData().getProcessSessionId());
				}
				results.put("taskid", iTask.getId());
				results.put("userToken", userToken);  /* save the latest userToken that actually completes the form */
				results.put("serviceToken", serviceToken);  /* save the latest userToken that actually completes the form */

				if (mandatoryDoneMap.get(taskSummary.getId())) {
					taskService.complete(iTask.getId(), realm + "+" + userCode, results);
					TaskUtils.sendTaskAskItems(userToken);
					// kSession.signalEvent("closeTask", facts);
					output = new OutputParam();
//					if ("NONE".equals(formCode)) {
						output.setTypeOfResult("NONE");
//					} else {
//						output.setTypeOfResult("FORMCODE");
//					}
//					output.setFormCode(formCode, targetCode);

				} else {
					// kSession.signalEvent("closeTask", facts);
					output = new OutputParam();
					output.setTypeOfResult("NONE");
					output.setFormCode("NONE", "NONE");
				}
			}
		}

		resultMap.put("output", output);
		// check if result has nulls in it
		for (String key : resultMap.keySet()) {
			if (key == null) {
				log.error("processAnswers: BAD NULL KEY IN RESULT SET");
			} else if (resultMap.get(key) == null) {
				log.error("processAnswers: BAD NULL MAP ENTRY IN RESULT SET for KEY = " + key);
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
						log.error("processAnswers :  OutputParam map is ok");
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

		return;

		// notify manager that work item has been completed
		// manager.completeWorkItem(workItem.getId(), resultMap);

	}

	private Boolean validate(Answer answer, GennyToken userToken) {
		// TODO - check value using regexs
		if (!answer.getSourceCode().equals(userToken.getUserCode())) {
			if (userToken.hasRole("admin")) {
				return true;
			}
			return false;
		}
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
//        else { 
//            EntityTransaction tx = em.getTransaction();
//            if( ! tx.isActive() ) { 
//               tx.begin();
//               return tx;
//            }
//        }
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

//		Environment env = event.getKieRuntime().getEnvironment();

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