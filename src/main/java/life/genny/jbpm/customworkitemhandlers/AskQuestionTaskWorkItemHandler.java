package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.drools.core.ClassObjectFilter;
import org.drools.core.process.instance.impl.WorkItemImpl;
import org.jbpm.process.core.timer.DateTimeUtils;
import org.jbpm.services.task.exception.PermissionDeniedException;
import org.jbpm.services.task.impl.util.HumanTaskHandlerHelper;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.jbpm.services.task.utils.OnErrorAction;
import org.jbpm.services.task.utils.TaskFluent;
import org.jbpm.services.task.wih.NonManagedLocalHTWorkItemHandler;
import org.jbpm.services.task.wih.util.PeopleAssignmentHelper;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.CaseData;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Attachment;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Group;
import org.kie.api.task.model.I18NText;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.PeopleAssignments;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.User;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.task.api.InternalTaskService;
import org.kie.internal.task.api.TaskModelProvider;
import org.kie.internal.task.api.model.AccessType;
import org.kie.internal.task.api.model.ContentData;
import org.kie.internal.task.api.model.InternalAttachment;
import org.kie.internal.task.api.model.InternalContent;
import org.kie.internal.task.api.model.InternalI18NText;
import org.kie.internal.task.api.model.InternalOrganizationalEntity;
import org.kie.internal.task.api.model.InternalTask;
import org.kie.internal.task.api.model.InternalTaskData;

import life.genny.models.GennyToken;
import life.genny.qwanda.Ask;
import life.genny.qwanda.TaskAsk;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwandautils.JsonUtils;

import life.genny.rules.RulesLoader;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.SessionFacts;
import life.genny.utils.TaskUtils;

public class AskQuestionTaskWorkItemHandler extends NonManagedLocalHTWorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	

	RuntimeEngine runtimeEngine;
	String wClass;
	String baseEntitySourceCode = null;
	String baseEntityTargetCode = null;
	
    public AskQuestionTaskWorkItemHandler() {
    	super();
    }
    
    public <R> AskQuestionTaskWorkItemHandler(Class<R> workflowQueryInterface,KieSession ksession, TaskService taskService) {
    	super(ksession,taskService);
    	this.wClass = workflowQueryInterface.getCanonicalName();
     }

	public <R> AskQuestionTaskWorkItemHandler(Class<R> workflowQueryInterface) {
		this.wClass = workflowQueryInterface.getCanonicalName();
	}

	public <R> AskQuestionTaskWorkItemHandler(Class<R> workflowQueryInterface, RuntimeEngine rteng, KieSession session) {
		super(session,rteng.getTaskService());
		this.runtimeEngine = rteng;
		this.wClass = workflowQueryInterface.getCanonicalName();
	}
	public <R> AskQuestionTaskWorkItemHandler(Class<R> workflowQueryInterface, RuntimeEngine rteng) {
		super(rteng.getKieSession(),rteng.getTaskService());
		this.runtimeEngine = rteng;
		this.wClass = workflowQueryInterface.getCanonicalName();
	}

	
	   @Override
	    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
			GennyToken userToken = (GennyToken) workItem.getParameter("userToken");
			
			if (this.runtimeEngine==null) {
				this.runtimeEngine = RulesLoader.runtimeManager.getRuntimeEngine(EmptyContext.get()); 
			}
			
			String callingWorkflow = (String)workItem.getParameter("callingWorkflow");
			if (StringUtils.isBlank(callingWorkflow)) {
				callingWorkflow = "";
			}

			baseEntitySourceCode = userToken.getUserCode();
			BaseEntity baseEntitySource = (BaseEntity)workItem.getParameter("baseEntitySource");
			if (baseEntitySource != null) {
				baseEntitySourceCode = baseEntitySource.getCode();
			}

			baseEntityTargetCode = userToken.getUserCode();
			BaseEntity baseEntityTarget = (BaseEntity)workItem.getParameter("baseEntityTarget");
			if (baseEntityTarget != null) {
				baseEntityTargetCode = baseEntityTarget.getCode();
			}
			
			String formCode = (String)workItem.getParameter("formCode");
			String targetCode = (String)workItem.getParameter("targetCode");
			
			
			
			if (formCode == null) {
				formCode = "FRM_QUE_TAB_VIEW";
			}

			if (targetCode == null) {
				targetCode = "FRM_CONTENT";
			}

			BaseEntityUtils beUtils = new BaseEntityUtils(userToken);

	        Task task = createTaskBasedOnWorkItemParams(this.getKsession(), workItem);
	        
	        
            // Fetch the questions and set in the task for us to tick off as they get done
            Set<QDataAskMessage> formSet = ShowFrame.fetchAskMessages(task.getFormName(), userToken);
            Map<String,Object> taskAsksMap = new ConcurrentHashMap<String,Object>();
            for (QDataAskMessage dataMsg : formSet) {
            	Boolean createOnTrigger = false;
            	for (Ask askMsg : dataMsg.getItems()) {
            		createOnTrigger = askMsg.hasTriggerQuestion();
            		processAsk(beUtils,task.getFormName(),askMsg,taskAsksMap,userToken,createOnTrigger);
             		
            	}
            }

//            Attachment attach = TaskModelProvider.getFactory().newAttachment();
//            ((InternalAttachment)attach).setAccessType(AccessType.Inline);
//            ((InternalAttachment)attach).setAttachedAt(new Date());
//            ((InternalAttachment)attach).setName(task.getFormName());
//            ((InternalAttachment)attach).setContentType("String");
//            Content content2 = TaskModelProvider.getFactory().newContent();
//            byte[] byteArray = SerializationUtils.serialize(taskAsksMap);
//            ((InternalContent)content2).setContent(byteArray);

            // Now tuck the intended after complete formcode into taskAsksMap
            taskAsksMap.put("FORM_CODE", formCode);            
            taskAsksMap.put("TARGET_CODE",targetCode);
//            InternalTask iTask = (InternalTask) task;
//			InternalTaskData iTaskData = (InternalTaskData) iTask.getTaskData();
//            iTaskData.setTaskOutputVariables(new HashMap<String,Object>());
//            task.getTaskData().getTaskOutputVariables().put("FORM_CODE", formCode);
//            task.getTaskData().getTaskOutputVariables().put("TARGET_CODE", targetCode);
	        
	        ContentData content = createTaskContentBasedOnWorkItemParams(this.getKsession(),  taskAsksMap);

	        try {
	            long taskId = ((InternalTaskService) this.getTaskService()).addTask(task, content);
	            if (isAutoClaim(this.getKsession(), workItem, task)) {
	            	 this.getTaskService().claim(taskId, (String) workItem.getParameter("SwimlaneActorId"));

	            }
	            
	            
	     
	           // ((InternalContent)content).setContent(ContentMarshallerHelper.marshallContent(task, payload, null));
	           // taskData.getAttachments().add(attach);
	                  
	         
                sendTaskSignal(userToken, task, callingWorkflow); // TODO, watch the timing as the workitem may not be ready if the target tries to do stuff.

              //  TaskUtils.sendTaskMenuItems(userToken);
                TaskUtils.sendTaskAskItems(userToken);
                
	        } catch (Exception e) {
	            if (action.equals(OnErrorAction.ABORT)) {
	                manager.abortWorkItem(workItem.getId());
	            } else if (action.equals(OnErrorAction.RETHROW)) {
	                if (e instanceof RuntimeException) {
	                    throw (RuntimeException) e;
	                } else {
	                    throw new RuntimeException(e);
	                }
	            } else if (action.equals(OnErrorAction.LOG)) {
	                StringBuilder logMsg = new StringBuilder();
	                logMsg.append(new Date()).append(": Error when creating task on task server for work item id ").append(workItem.getId());
	                logMsg.append(". Error reported by task server: ").append(e.getMessage());
	                log.error(logMsg.toString(), e);
	            }
	        } 
	    }
	
	private void processAsk(BaseEntityUtils beUtils, String formName,Ask askMsg, Map<String, Object> taskAsksMap, GennyToken userToken, Boolean createOnTrigger) {
   		// replace askMesg source and target with required src and target, initially we will use both src and target
		String json = JsonUtils.toJson(askMsg);
		json = json.replaceAll("PER_SOURCE", baseEntitySourceCode);
		json = json.replaceAll("PER_TARGET", baseEntityTargetCode);
		json = json.replaceAll("PER_SERVICE", baseEntitySourceCode);
		Ask newMsg = JsonUtils.fromJson(json, Ask.class);
		String key = baseEntitySourceCode+":"+baseEntityTargetCode+":"+newMsg.getAttributeCode();
		// work out whether an Ask has already got a value for that attribute
		Boolean answered = false;
		
		Boolean isTableRow = false;
		// TODO, if the question is a submit then 
		
		Boolean formTrigger = newMsg.getFormTrigger();
		TaskAsk taskAsk = new TaskAsk(newMsg,formName, answered, isTableRow, formTrigger, createOnTrigger);
		
		// Check if already answered ...
		BaseEntity target = beUtils.getBaseEntityByCode(newMsg.getTargetCode());
		Optional<EntityAttribute> attributeValue =  target.findEntityAttribute(taskAsk.getAsk().getAttributeCode());
		//Optional<String> value = target.getValue(taskAsk.getAsk().getAttributeCode());
		if (attributeValue.isPresent()) {
			taskAsk.setAnswered(true);
			EntityAttribute ea = attributeValue.get();
			taskAsk.setValue(ea.getAsString());
		}
		// don't add questions that are just groups
		if (!askMsg.getQuestionCode().endsWith("_GRP")) {
			taskAsksMap.put(key, taskAsk);
		}
		if ((newMsg.getChildAsks()!=null)&&(newMsg.getChildAsks().length>0)) {
			for (Ask childAsk : newMsg.getChildAsks()) {
				processAsk(beUtils,formName,childAsk,taskAsksMap,userToken,createOnTrigger);
			}
		}
		return ;
	}

	/**
	 * @param userToken
	 * @param processId
	 * @param taskId
	 */
	private void sendTaskSignal(GennyToken userToken, Task task, String callingWorkflow) {
		Long targetProcessId = null;
		

		
		QEventMessage taskMsg = new QEventMessage("EVT_MSG", "TASK");
		taskMsg.getData().setValue(task.getId()+"");
		taskMsg.setToken(userToken.getToken());
		
		
		
		SessionFacts sessionFacts = new SessionFacts(userToken,userToken,taskMsg); // Let the userSession know that there is a question Waiting

			Method m;
			Optional<Long> processIdBysessionId = Optional.empty();
			try {
				Class<?> cls = Class.forName(this.wClass); // needs filtering.
				m = cls.getDeclaredMethod("getProcessIdBySessionId", String.class);
				String param = userToken.getSessionCode(); 
				processIdBysessionId =  (Optional<Long>) m.invoke(null, (Object) param);

			} catch (NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			boolean hasProcessIdBySessionId = processIdBysessionId.isPresent();
			if (hasProcessIdBySessionId) {
				targetProcessId = processIdBysessionId.get();
			}


		if (targetProcessId != null) {
			
		    long taskId = task.getId();

			System.out.println(callingWorkflow+" "+task.getDescription()+" Sending Question Code  " + task.getFormName() + " to processId " +targetProcessId
					+ " for target user " + userToken.getUserCode()+" using TASK "+taskId);



			KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();

			KieSession newKieSession = null;

			if (this.runtimeEngine != null) {

				newKieSession = (StatefulKnowledgeSession) this.runtimeEngine.getKieSession();
				//newKieSession.signalEvent("IS_"+userToken.getSessionCode(), sessionFacts);
				newKieSession.signalEvent("internalSignal", sessionFacts, targetProcessId);
			} else {

				KieBase kieBase = RulesLoader.getKieBaseCache().get(userToken.getRealm());
				newKieSession = (StatefulKnowledgeSession) kieBase.newKieSession(ksconf, null);

				//newKieSession.signalEvent("IS_"+userToken.getSessionCode(), sessionFacts);
				newKieSession.signalEvent("internalSignal", sessionFacts, targetProcessId);

				newKieSession.dispose();

			}
		}
	}



    @Override
    protected Task createTaskBasedOnWorkItemParams(KieSession session, WorkItem workItem) {
        InternalTask task = (InternalTask) TaskModelProvider.getFactory().newTask();
        
        CaseData caseFile = null;
		GennyToken userToken = (GennyToken) workItem.getParameter("userToken");
		String userCode = userToken.getRealm()+"+"+userToken.getUserCode();
		String questionCode = (String) workItem.getParameter("questionCode");
		String callingWorkflow = (String)workItem.getParameter("callingWorkflow");
		if (StringUtils.isBlank(callingWorkflow)) {
			callingWorkflow = "";
		}
	
//		JsonObject questionObj = VertxUtils.readCachedJson(userToken.getRealm(),
//				questionCode,userToken.getToken());
//		String questionStr = questionObj.getString("value");
//		Question question = JsonUtils.fromJson(questionStr, Question.class);
		
		 workItem.getParameters().put("SwimlaneActorId",userCode);
		 workItem.getParameters().put("ActorId",userCode);
        
        String locale = (String) workItem.getParameter("Locale");
        if (locale == null) {
            locale = "en-AU";
        }
        
        if (questionCode != null) {
            List<I18NText> names = new ArrayList<I18NText>();
            I18NText text = TaskModelProvider.getFactory().newI18NText();
            ((InternalI18NText) text).setLanguage(locale);
            ((InternalI18NText) text).setText(questionCode);
            names.add(text);
            task.setNames(names);
        }
        task.setName(questionCode);
        // this should be replaced by FormName filled by designer
        // TaskName shouldn't be trimmed if we are planning to use that for the task lists
        String formName = "FRM_"+questionCode; //(String) workItem.getParameter(questionCode); 
        if(formName != null){
            task.setFormName(formName);
        }
        
        String comment = (String) workItem.getParameter("Comment");
        if (comment == null) {
            comment = "";
        }
        
        String description = (String) workItem.getParameter("Description");
        if (description == null) {
            description = questionCode;//question.getName();
        }
        
        List<I18NText> descriptions = new ArrayList<I18NText>();
        I18NText descText = TaskModelProvider.getFactory().newI18NText();
        ((InternalI18NText) descText).setLanguage(locale);
        ((InternalI18NText) descText).setText(description);
        descriptions.add(descText);
        task.setDescriptions(descriptions);
        
        task.setDescription(description);
        
        List<I18NText> subjects = new ArrayList<I18NText>();
        I18NText subjectText = TaskModelProvider.getFactory().newI18NText();
        ((InternalI18NText) subjectText).setLanguage(locale);
        ((InternalI18NText) subjectText).setText(comment);
        subjects.add(subjectText);
        task.setSubjects(subjects);
        
        task.setSubject(comment);
        
        String priorityString = (String) workItem.getParameter("Priority");
        int priority = 0;
        if (priorityString != null) {
            try {
                priority = new Integer(priorityString);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        task.setPriority(priority);
        
        InternalTaskData taskData = (InternalTaskData) TaskModelProvider.getFactory().newTaskData();        
        taskData.setWorkItemId(workItem.getId());
        taskData.setProcessInstanceId(workItem.getProcessInstanceId());
        if (session != null) {
            if (session.getProcessInstance(workItem.getProcessInstanceId()) != null) {
                taskData.setProcessId(session.getProcessInstance(workItem.getProcessInstanceId()).getProcess().getId());
                String deploymentId = ((WorkItemImpl) workItem).getDeploymentId();
                taskData.setDeploymentId(deploymentId);            
            }
            if (session instanceof KieSession) {
                taskData.setProcessSessionId(((KieSession) session).getIdentifier());
                log.info("####### askQuestion! sessionId="+taskData.getProcessSessionId());
            }
            @SuppressWarnings("unchecked")
            Collection<CaseData> caseFiles = (Collection<CaseData>) session.getObjects(new ClassObjectFilter(CaseData.class));
            if (caseFiles != null && caseFiles.size() == 1) {
                caseFile = caseFiles.iterator().next();
            }
        }
        taskData.setSkipable(!"false".equals(workItem.getParameter("Skippable")));
        //Sub Task Data
        Long parentId = (Long) workItem.getParameter("ParentId");
        if (parentId != null) {
            taskData.setParentId(parentId);
        }
        
        String createdBy = userToken.getUserCode();//(String) workItem.getParameter("CreatedBy");
        if (createdBy != null && createdBy.trim().length() > 0) {
        	User user = TaskModelProvider.getFactory().newUser();
        	((InternalOrganizationalEntity) user).setId(userToken.getRealm()+"+"+createdBy);
            taskData.setCreatedBy(user);            
        }
        String dueDateString = (String) workItem.getParameter("DueDate");
        Date date = null;
        if(dueDateString != null && !dueDateString.isEmpty()){
            if(DateTimeUtils.isPeriod(dueDateString)){
                Long longDateValue = DateTimeUtils.parseDateAsDuration(dueDateString.substring(1));
                date = new Date(System.currentTimeMillis() + longDateValue);
            }else{
                date = new Date(DateTimeUtils.parseDateTime(dueDateString));
            }
        }
        if(date != null){
            taskData.setExpirationTime(date);
        }
        
        PeopleAssignmentHelper peopleAssignmentHelper = new PeopleAssignmentHelper(caseFile);
        peopleAssignmentHelper.handlePeopleAssignments(workItem, task, taskData);
        
        PeopleAssignments peopleAssignments = task.getPeopleAssignments();
        List<OrganizationalEntity> businessAdministrators = peopleAssignments.getBusinessAdministrators();
        

        task.setTaskData(taskData);
        task.setDeadlines(HumanTaskHandlerHelper.setDeadlines(workItem.getParameters(), businessAdministrators, session.getEnvironment()));
  

        return task;
    }


    protected ContentData createTaskContentBasedOnWorkItemParams(KieSession session,  Map<String,Object> taskAsksMap) {
        ContentData content = null;
        Object contentObject = null;
            contentObject = new ConcurrentHashMap<String, Object>(taskAsksMap);
         if (contentObject != null) {
            Environment env = null;
            if(session != null){
                env = session.getEnvironment();
            }
           
            content = ContentMarshallerHelper.marshal(null, contentObject, env);
        }
        return content;
    }
}