package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.runtime.StatefulKnowledgeSession;

import io.vertx.core.json.JsonObject;
import life.genny.model.OutputParam2;
import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.Answers;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.Allowed;
import life.genny.qwanda.datatype.CapabilityMode;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwanda.rule.RuleDetails;
import life.genny.qwandautils.JsonUtils;
import life.genny.rules.QRules;

import life.genny.rules.RulesLoader;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.CapabilityUtils;
import life.genny.utils.OutputParam;
import life.genny.utils.RulesUtils;
import life.genny.utils.VertxUtils;

public class RuleFlowGroupWorkItemHandler implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	RuntimeEngine runtimeEngine;

	public RuleFlowGroupWorkItemHandler() {

	}

	public RuleFlowGroupWorkItemHandler(RuntimeEngine rteng) {
		this.runtimeEngine = rteng;
	}

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		
		
		/* items used to save the extracted input parameters from the custom task */
		Map<String, Object> items = workItem.getParameters();
		GennyToken userToken = (GennyToken) items.get("userToken");
		GennyToken serviceToken = (GennyToken) items.get("serviceToken");
		String ruleFlowGroup = (String) items.get("ruleFlowGroup");
		String callingWorkflow = (String) items.get("callingWorkflow");

		System.out.println(callingWorkflow + ":pid" + workItem.getProcessInstanceId() + " Running rule flow group "
				+ ruleFlowGroup);

		final Map<String, Object> resultMap = executeRules(serviceToken,userToken,items,ruleFlowGroup,callingWorkflow);
		
		// notify manager that work item has been completed
		manager.completeWorkItem(workItem.getId(), resultMap);

	}

	/**
	 * @param workItem
	 * @return
	 */
	public Map<String, Object> executeRules(GennyToken serviceToken, GennyToken userToken, Map<String, Object> items,String ruleFlowGroup, String callingWorkflow) {
		/* resultMap is used to map the result Value to the output parameters */
		final Map<String, Object> resultMap = new ConcurrentHashMap<String, Object>();

		try {
		if (userToken == null) {
			userToken = serviceToken;
		}
		
		System.out.println(callingWorkflow + " Running rule flow group "
				+ ruleFlowGroup+" #1");

		BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
		CapabilityUtils capabilityUtils = new CapabilityUtils(beUtils);
		String userCode = userToken.getUserCode();
		BaseEntity user = null;
		if ((VertxUtils.cachedEnabled)&&("PER_SERVICE".equals(userCode))) {
			// need to create the server user in cache if not there
			user = VertxUtils.readFromDDT(userToken.getRealm(), "PER_SERVICE", userToken.getToken());
			if (user == null) {
				beUtils.setServiceToken(serviceToken);
				BaseEntity serviceUser = beUtils.create("PER_SERVICE", "Service User");
				Attribute roleAttribute = RulesUtils.getAttribute("PRI_IS_ADMIN", serviceToken);

				beUtils.saveAnswer(new Answer(serviceUser,serviceUser,roleAttribute,"TRUE"));
			}
		} else {
			
		}
		System.out.println(callingWorkflow + " Running rule flow group "
				+ ruleFlowGroup+" #2");
		user = beUtils.getBaseEntityByCode(userCode);
		List<EntityAttribute> roles = user.findPrefixEntityAttributes("PRI_IS_");
		List<Allowed> allowable = new CopyOnWriteArrayList<Allowed>();
		for (EntityAttribute role : roles) { // should store in cached map
			Boolean value = false;
			if (role.getValue() instanceof Boolean) {
				value = role.getValue();
			} else {
				if (role.getValue() instanceof String) {
					value = "TRUE".equalsIgnoreCase(role.getValue());
					System.out.println(callingWorkflow + " Running rule flow group "
							+ ruleFlowGroup+" #2.5 role value = "+role.getValue());
				} else {
					System.out.println(callingWorkflow + " Running rule flow group "
							+ ruleFlowGroup+" #2.6 role value = "+role.getValue());
				}
			}
			if (value) {
				String roleBeCode = "ROL_" + role.getAttributeCode().substring("PRI_IS_".length());
				BaseEntity roleBE = VertxUtils.readFromDDT(userToken.getRealm(), roleBeCode, userToken.getToken());
				if (roleBE == null) {
					continue;
				}
				List<EntityAttribute> capabilities = user.findPrefixEntityAttributes("PRM_");
				for (EntityAttribute ea : capabilities) {
					String modeString = ea.getValue();
					CapabilityMode mode = CapabilityMode.getMode(modeString);
					// This is my cunning switch statement that takes into consideration the
					// priority order of the modes... (note, no breaks and it relies upon the fall
					// through)
					switch (mode) {
					case DELETE:
						allowable.add(new Allowed(ea.getAttributeCode().substring(4), CapabilityMode.DELETE));
					case ADD:
						allowable.add(new Allowed(ea.getAttributeCode().substring(4), CapabilityMode.ADD));
					case EDIT:
						allowable.add(new Allowed(ea.getAttributeCode().substring(4), CapabilityMode.EDIT));
					case VIEW:
						allowable.add(new Allowed(ea.getAttributeCode().substring(4), CapabilityMode.VIEW));
					case NONE:
						allowable.add(new Allowed(ea.getAttributeCode().substring(4), CapabilityMode.NONE));
					}

				}
			}
		}


		if (StringUtils.isBlank(callingWorkflow)) {
			callingWorkflow = "";
		}

		System.out.println(callingWorkflow + " Running rule flow group "
				+ ruleFlowGroup+" #3");
		RuleDetails ruleDetails = new RuleDetails(callingWorkflow, ruleFlowGroup);

		if (serviceToken == null) {
			log.error("Must supply serviceToken!");
		} else if ((!"PER_SERVICE".equals(serviceToken.getCode()))) {
			log.error(
					"Must supply an actual serviceToken not a normal token! check PER_SERVICE is the code (and not serviceToken");
		} else {

			// log.info("serviceToken = "+serviceToken.getCode());
			// log.info("Running rule flow group "+ruleFlowGroup);

			// System.out.println("ProcessInstanceId = "+workItem.getProcessInstanceId());
			KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();
			// ksconf.setOption(TimedRuleExecutionOption.YES);

			KieSession newKieSession = null;

			OutputParam output = new OutputParam();
			Answers answersToSave = new Answers();

			if (this.runtimeEngine != null) {
				KieBase kieBase = RulesLoader.getKieBaseCache().get(serviceToken.getRealm());
				newKieSession = (StatefulKnowledgeSession) kieBase.newKieSession(ksconf, RulesLoader.env);

//			newKieSession = (StatefulKnowledgeSession)this.runtimeEngine.getKieSession();
				System.out.println(callingWorkflow + " Running rule flow group "
						+ ruleFlowGroup+" #4");
				FactHandle ruleDetailsHandle = newKieSession.insert(ruleDetails);

				/* Inserting all the parameters in the working memory ad a facts */
				for (String key : items.keySet()) {
					newKieSession.insert(items.get(key));
					if (items.get(key) instanceof GennyToken) {
						GennyToken gToken = (GennyToken) items.get(key);
						String code = gToken.getCode();
						if (code == null) {
							code = gToken.getUserCode();
						}
						if (!code.equals("PER_SERVICE")) {
							/* Generate a QRules */
							// log.info("Adding rules to facts");
							QRules rules = new QRules(serviceToken, gToken);
							newKieSession.insert(rules);
						}
					}
				}
				System.out.println(callingWorkflow + " Running rule flow group "
						+ ruleFlowGroup+" #5");
				/* INserting facts to save the output result */
				FactHandle factHandle = newKieSession.insert(output);
				FactHandle answersToSaveHandle = newKieSession.insert(answersToSave);
				FactHandle kieSessionHandle = newKieSession.insert(newKieSession);
				FactHandle beUtilsHandle = newKieSession.insert(beUtils);
				FactHandle capabilityUtilsHandle = newKieSession.insert(capabilityUtils);
				
				QBulkMessage payload = new QBulkMessage();
				newKieSession.setGlobal("payload", payload);
			/*	FactHandle payloadHandle = newKieSession.insert(payload); */
				System.out.println(callingWorkflow + " Running rule flow group "
						+ ruleFlowGroup+" #6");
				List<FactHandle> allowables = new ArrayList<FactHandle>();
				// get User capabilities
				// first get User Roles

				// get each capability from each Role and add to allowables
				for (Allowed allow : allowable) {
					allowables.add(newKieSession.insert(allow));
				}

				/* Setting focus to rule-flow group */
				newKieSession.getAgenda().getAgendaGroup(ruleFlowGroup).setFocus();

				newKieSession.fireAllRules();
				System.out.println(callingWorkflow + " Running rule flow group "
						+ ruleFlowGroup+" #7");
//	    	ObjectFilter filter = new ObjectFilter() {
//	    	    @Override
//	    	        public boolean accept( Object object ) {
//	    	            return object.getClass().getSimpleName().equals( "OutputParam" );
//	    	        }
//	    	    };
//	    	Collection<? extends Object> results = newKieSession.getObjects( filter );
				/* saving result from rule-task in map */
				output = (OutputParam) newKieSession.getObject(factHandle);
				answersToSave = (Answers) newKieSession.getObject(answersToSaveHandle);
			/*	payload = (QBulkMessage) newKieSession.getObject(payloadHandle); */
				payload = (QBulkMessage) newKieSession.getGlobal("payload");
				resultMap.put("payload", payload);
				
				resultMap.put("output", output);
				resultMap.put("answersToSave", answersToSave);
				newKieSession.retract(ruleDetailsHandle);
				newKieSession.retract(factHandle);
				newKieSession.retract(answersToSaveHandle);
				newKieSession.retract(beUtilsHandle);
				newKieSession.retract(capabilityUtilsHandle);
				newKieSession.retract(kieSessionHandle); // don't dispose
			/*	newKieSession.retract(payloadHandle); */
				
				System.out.println(callingWorkflow + " Running rule flow group "
						+ ruleFlowGroup+" #8");
				for (FactHandle allow : allowables) {
					newKieSession.retract(allow);
				}

				newKieSession.dispose();

			} else {

				KieBase kieBase = RulesLoader.getKieBaseCache().get(serviceToken.getRealm());
				newKieSession = (StatefulKnowledgeSession) kieBase.newKieSession(ksconf, RulesLoader.env);
				System.out.println(callingWorkflow + " Running rule flow group "
						+ ruleFlowGroup+" #10a");
				FactHandle ruleDetailsHandle = newKieSession.insert(ruleDetails);

				/* Inserting all the parameters in the working memory ad a facts */
				for (String key : items.keySet()) {
					newKieSession.insert(items.get(key));
					if (items.get(key) instanceof GennyToken) {
						GennyToken gToken = (GennyToken) items.get(key);
						if (!gToken.getCode().equals("PER_SERVICE")) {
							/* Generate a QRules */
							// log.info("Adding rules to facts");
							QRules rules = new QRules(serviceToken, gToken);
							newKieSession.insert(rules);
						}
					}

				}
				System.out.println(callingWorkflow + " Running rule flow group "
						+ ruleFlowGroup+" #11a");
//			OutputParam output2 = new OutputParam2();
//			FactHandle output2Fact = newKieSession.insert(output2);

				/* INserting facts to save the output result */
				FactHandle factHandle = newKieSession.insert(output);
				FactHandle answersToSaveHandle = newKieSession.insert(answersToSave);
				FactHandle beUtilsHandle = newKieSession.insert(beUtils);
				FactHandle capabilityUtilsHandle = newKieSession.insert(capabilityUtils);
				FactHandle kieSessionHandle = newKieSession.insert(newKieSession);
				
				QBulkMessage payload = new QBulkMessage();
//				FactHandle payloadHandle = newKieSession.insert(payload);
			//	newKieSession.setGlobal("payload", payload);

				System.out.println(callingWorkflow + " Running rule flow group "
						+ ruleFlowGroup+" #12a");
				List<FactHandle> allowables = new ArrayList<FactHandle>();
				// get User capabilities

				// inject kieSession
				for (Allowed allow : allowable) {
					allowables.add(newKieSession.insert(allow));
				}

				/* Setting focus to rule-flow group */
				newKieSession.getAgenda().getAgendaGroup(ruleFlowGroup).setFocus();

				newKieSession.fireAllRules();
				System.out.println(callingWorkflow + " Running rule flow group "
						+ ruleFlowGroup+" #13a");
//	    	output2 = (OutputParam) newKieSession.getObject(output2Fact);
				output = (OutputParam) newKieSession.getObject(factHandle);
				answersToSave = (Answers) newKieSession.getObject(answersToSaveHandle);
//				payload = (QBulkMessage) newKieSession.getObject(payloadHandle);
//	    	// HACK
//	    	if (!output2.getResultCode().equalsIgnoreCase("DUMMY")) {
//	    		output = output2;
//	    	}
				System.out.println(callingWorkflow + " Running rule flow group "
+ ruleFlowGroup+" #14a");
				QEventMessage msg = (QEventMessage) items.get("message");
				if (msg != null) {

					JsonObject cachedOutputJson = VertxUtils.readCachedJson(userToken.getRealm(),
							"OUTPUT:" + msg.getData().getCode(), userToken.getToken());
					if (cachedOutputJson.getString("status").equalsIgnoreCase("ok")) {
						OutputParam o = JsonUtils.fromJson(cachedOutputJson.getString("value"), OutputParam.class);
						if (o != null) {
							output = o;
						}
					}
				}

				resultMap.put("output", output);
				resultMap.put("answersToSave", answersToSave);
		//		payload = (QBulkMessage) newKieSession.getGlobal("payload");
		//		resultMap.put("payload", payload);
				
				newKieSession.retract(ruleDetailsHandle);
				newKieSession.retract(factHandle);
				newKieSession.retract(answersToSaveHandle);
				newKieSession.retract(beUtilsHandle);
				newKieSession.retract(capabilityUtilsHandle);
				newKieSession.retract(kieSessionHandle);
//				newKieSession.retract(payloadHandle);

				for (FactHandle allow : allowables) {
					newKieSession.retract(allow);
				}

				newKieSession.dispose();
			}
		}
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
		}
		return resultMap;
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, notifications cannot be aborted
	}

}