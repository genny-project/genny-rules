package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jbpm.kie.services.impl.query.mapper.ProcessInstanceQueryMapper;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.jbpm.services.api.query.QueryService;
import org.jbpm.services.api.query.model.QueryParam;
import org.jbpm.services.api.utils.KieServiceConfigurator;
import org.jbpm.services.task.utils.TaskFluent;
import org.jbpm.services.task.wih.NonManagedLocalHTWorkItemHandler;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.ObjectFilter;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.task.TaskLifeCycleEventListener;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Task;
import org.kie.internal.identity.IdentityProvider;
import org.kie.internal.query.QueryContext;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.task.api.UserGroupCallback;
import org.slf4j.LoggerFactory;

import life.genny.models.GennyToken;
import life.genny.qwanda.message.QEventMessage;
import life.genny.rules.QRules;
import life.genny.rules.RulesLoader;
import life.genny.rules.listeners.GennyTaskEventListener;
import life.genny.utils.OutputParam;
import life.genny.utils.SessionFacts;
import life.genny.utils.VertxUtils;
import life.genny.utils.WorkflowQueryInterface;

public class AskQuestionTaskWorkItemHandler extends NonManagedLocalHTWorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	
    private KieSession ksession;
    private TaskService taskService;
    private GennyTaskEventListener listener;
    private boolean initialized = false;
 

	RuntimeEngine runtimeEngine;
	String wClass;
	
    public AskQuestionTaskWorkItemHandler() {
    	super();
    }
    
    public AskQuestionTaskWorkItemHandler(KieSession ksession, TaskService taskService) {
    	super(ksession,taskService);
     }

	public <R> AskQuestionTaskWorkItemHandler(Class<R> workflowQueryInterface) {
		this.wClass = workflowQueryInterface.getCanonicalName();
	}

	public <R> AskQuestionTaskWorkItemHandler(Class<R> workflowQueryInterface, RuntimeEngine rteng) {
		this.runtimeEngine = rteng;
		this.wClass = workflowQueryInterface.getCanonicalName();
	}

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

		/* resultMap is used to map the result Value to the output parameters */
		final Map<String, Object> resultMap = new HashMap<String, Object>();

		/* items used to save the extracted input parameters from the custom task */
		Map<String, Object> items = workItem.getParameters();

		GennyToken userToken = (GennyToken) items.get("userToken");
		String questionCode = (String) items.get("questionCode");
		String callingWorkflow = (String)items.get("callingWorkflow");
		if (StringUtils.isBlank(callingWorkflow)) {
			callingWorkflow = "";
		}

		Long processId = null;
		
		
		// Create Task
	    TaskService taskService = runtimeEngine.getTaskService();

	    Map<String, Object> params = new HashMap<String, Object>();
	    Task task =
	        new TaskFluent()
	            .setName(questionCode)
	           // .addPotentialGroup("GADA")
	            .setAdminUser("Administrator")
	            .addPotentialUser(userToken.getUserCode())   // only offer to the actual interacting user
	            .setProcessId(workItem.getName())
	            .setDeploymentID(userToken.getRealm())
	            .getTask();
	    
	    // TODO, from the Questions find the expiry time and set as the task deadline....
	    
	    taskService.addTask(task, params);
	    long taskId = task.getId();

	    Map<String, Object> content = taskService.getTaskContent(taskId);
		System.out.println(callingWorkflow+" Sending Question Code  " + questionCode + " to processId " + processId
				+ " for target user " + userToken.getUserCode()+" using TASK "+taskId+":"+content);


		QEventMessage taskMsg = new QEventMessage("EVT_MSG", "TASK");
		taskMsg.getData().setValue(taskId+"");
		taskMsg.setToken(userToken.getToken());
		
		
		
		SessionFacts sessionFacts = new SessionFacts(userToken,userToken,taskMsg); // Let the userSession know that there is a question Waiting

		if (processId == null) {
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
				processId = processIdBysessionId.get();
			}
		}

		if (processId != null) {

			KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();

			KieSession newKieSession = null;

			if (this.runtimeEngine != null) {

				newKieSession = (StatefulKnowledgeSession) this.runtimeEngine.getKieSession();

				newKieSession.signalEvent("internalSignal", sessionFacts, processId);
			} else {

				KieBase kieBase = RulesLoader.getKieBaseCache().get(userToken.getRealm());
				newKieSession = (StatefulKnowledgeSession) kieBase.newKieSession(ksconf, null);

				newKieSession.signalEvent("internalSignal", sessionFacts, processId);

				newKieSession.dispose();

			}
		}
		// notify manager that work item has been completed
	//	manager.completeWorkItem(workItem.getId(), resultMap);

	}



}