package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jbpm.kie.services.impl.query.SqlQueryDefinition;
import org.jbpm.kie.services.impl.query.mapper.ProcessInstanceQueryMapper;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.jbpm.services.api.query.QueryAlreadyRegisteredException;
import org.jbpm.services.api.query.QueryService;
import org.jbpm.services.api.query.model.QueryParam;
import org.jbpm.services.api.utils.KieServiceConfigurator;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.identity.IdentityProvider;
import org.kie.internal.query.QueryContext;
import org.kie.internal.task.api.UserGroupCallback;

import life.genny.models.GennyToken;
import life.genny.utils.OutputParam;


public class GetProcessesUsingVariable implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	private static QueryService queryService;
	private static KieServiceConfigurator serviceConfigurator;

	
    public GetProcessesUsingVariable() {
        super();
    }

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.info("Executing GetProcessesUsingVariable handler. Requested PrcoessId By : " + workItem.getProcessInstanceId());
        final Map<String,Object> resultMap = new HashMap<String,Object>();
        
        /* Configuring query Service and registering query statement */
         
        configureServices();
        registerQuery();
        
        /* Extracting the input parameter passed from workflow through workItem */
        
		String variableName = (String) workItem.getParameter("variableName");
		String variableValue = (String) workItem.getParameter("variableValue");
		String callingWorkflow = (String) workItem.getParameter("callingWorkflow");
		GennyToken userToken = (GennyToken) workItem.getParameter("userToken");
		
    	if (StringUtils.isBlank(callingWorkflow)) {
    		
    		callingWorkflow = "";
    	}
		
    	log.info("Calling Workflow :: " + callingWorkflow);
    	log.info("User Requesting :: " + userToken.getUserCode());
    	log.info("ColumnName / VariableName = " + variableName );
		log.info("Value / VariableValue = " + variableValue );
		
		QueryContext ctx = new QueryContext(0, 100);
		
		if( variableName != null && variableValue != null ) {
				
			Collection<ProcessInstanceDesc> instances = queryService.query("getAllProcessInstancesByVariable",ProcessInstanceQueryMapper.get(), ctx,
														QueryParam.equalsTo(variableName, variableValue));	
			
			List<Long> resultArray = instances.stream().map(d -> d.getId()).collect(Collectors.toList());
			OutputParam output = new OutputParam();
			output.setResult(resultArray);
			
			resultMap.put("output", output);
			manager.completeWorkItem(workItem.getId(), resultMap);
			log.info("Successful Executing GetProcessesUsingVariable handler");
			
		}else {
			
			abortWorkItem(workItem,manager); 
		}
    }
    

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    	log.warn("Aborting");
        log.info("Error When Executing QueryProcessInstanceIDByVariable Work Item Handler: Missing Parameters");
        manager.abortWorkItem(workItem.getId());
    }
    
    
    /*
     *  this method registers SQL query statement, to be used for fetching data from persistence database
     */
    private static void registerQuery() {
    	
    	SqlQueryDefinition query = new SqlQueryDefinition("getAllProcessInstancesByVariable", "java:jboss/datasources/gennyDS");
		query.setExpression("select * from nodestatus");
		try {
			queryService.registerQuery(query);
		} catch (QueryAlreadyRegisteredException e) {
			log.warn(query.getName() + " is already registered");
		}
    }

    /*
     * This Method configures query Service
     */
    private static void configureServices() {
    	
		serviceConfigurator = ServiceLoader.load(KieServiceConfigurator.class).iterator().next();

		IdentityProvider identityProvider = new IdentityProvider() {

			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return "";
			}

			@Override
			public List<String> getRoles() {
				// TODO Auto-generated method stub
				return new ArrayList<String>();
			}

			@Override
			public boolean hasRole(String role) {
				// TODO Auto-generated method stub
				return true;
			}
		};

		UserGroupCallback userGroupCallback = new UserGroupCallback() {

			@Override
			public boolean existsUser(String userId) {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public boolean existsGroup(String groupId) {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public List<String> getGroupsForUser(String userId) {
				// TODO Auto-generated method stub
				return new ArrayList<String>();
			}
		};

		serviceConfigurator.configureServices("genny-persistence-jbpm-jpa", identityProvider, userGroupCallback);
		queryService = serviceConfigurator.getQueryService();
	}
}