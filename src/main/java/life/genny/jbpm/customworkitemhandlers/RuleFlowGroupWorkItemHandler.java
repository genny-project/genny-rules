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
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.Allowed;
import life.genny.qwanda.datatype.CapabilityMode;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwanda.rule.RuleDetails;
import life.genny.qwandautils.JsonUtils;
import life.genny.rules.QRules;

import life.genny.rules.RulesLoader;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.CapabilityUtils;
import life.genny.utils.OutputParam;
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

		/* resultMap is used to map the result Value to the output parameters */
		final Map<String, Object> resultMap = new ConcurrentHashMap<String, Object>();

		/* items used to save the extracted input parameters from the custom task */
		Map<String, Object> items = workItem.getParameters();

		GennyToken serviceToken = (GennyToken) items.get("serviceToken");

		GennyToken userToken = (GennyToken) items.get("userToken");
		if (userToken == null) {
			userToken = serviceToken;
		}
		BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
		CapabilityUtils capabilityUtils = new CapabilityUtils(beUtils);
		String userCode = userToken.getUserCode();
		BaseEntity user = beUtils.getBaseEntityByCode(userCode);
		List<EntityAttribute> roles = user.findPrefixEntityAttributes("PRI_IS_");
		List<Allowed> allowable = new CopyOnWriteArrayList<Allowed>();
		for (EntityAttribute role : roles) { // should store in cached map
			Boolean value = role.getValue();
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

		String ruleFlowGroup = (String) items.get("ruleFlowGroup");

		String callingWorkflow = (String) items.get("callingWorkflow");
		if (StringUtils.isBlank(callingWorkflow)) {
			callingWorkflow = "";
		}

		RuleDetails ruleDetails = new RuleDetails(callingWorkflow, ruleFlowGroup);

		if (serviceToken == null) {
			log.error("Must supply serviceToken!");
		} else if ((!"PER_SERVICE".equals(serviceToken.getCode()))) {
			log.error(
					"Must supply an actual serviceToken not a normal token! check PER_SERVICE is the code (and not serviceToken");
		} else {

			// log.info("serviceToken = "+serviceToken.getCode());
			// log.info("Running rule flow group "+ruleFlowGroup);
			System.out.println(callingWorkflow + ":pid" + workItem.getProcessInstanceId() + " Running rule flow group "
					+ ruleFlowGroup);

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

				/* INserting facts to save the output result */
				FactHandle factHandle = newKieSession.insert(output);
				FactHandle answersToSaveHandle = newKieSession.insert(answersToSave);
				FactHandle kieSessionHandle = newKieSession.insert(newKieSession);
				FactHandle beUtilsHandle = newKieSession.insert(beUtils);
				FactHandle capabilityUtilsHandle = newKieSession.insert(capabilityUtils);

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
				resultMap.put("output", output);
				resultMap.put("answersToSave", answersToSave);
				newKieSession.retract(ruleDetailsHandle);
				newKieSession.retract(factHandle);
				newKieSession.retract(answersToSaveHandle);
				newKieSession.retract(beUtilsHandle);
				newKieSession.retract(capabilityUtilsHandle);
				newKieSession.retract(kieSessionHandle); // don't dispose

				for (FactHandle allow : allowables) {
					newKieSession.retract(allow);
				}

				newKieSession.dispose();

			} else {

				KieBase kieBase = RulesLoader.getKieBaseCache().get(serviceToken.getRealm());
				newKieSession = (StatefulKnowledgeSession) kieBase.newKieSession(ksconf, RulesLoader.env);

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

//			OutputParam output2 = new OutputParam2();
//			FactHandle output2Fact = newKieSession.insert(output2);

				/* INserting facts to save the output result */
				FactHandle factHandle = newKieSession.insert(output);
				FactHandle answersToSaveHandle = newKieSession.insert(answersToSave);
				FactHandle beUtilsHandle = newKieSession.insert(beUtils);
				FactHandle capabilityUtilsHandle = newKieSession.insert(capabilityUtils);
				FactHandle kieSessionHandle = newKieSession.insert(newKieSession);

				List<FactHandle> allowables = new ArrayList<FactHandle>();
				// get User capabilities

				// inject kieSession
				for (Allowed allow : allowable) {
					allowables.add(newKieSession.insert(allow));
				}

				/* Setting focus to rule-flow group */
				newKieSession.getAgenda().getAgendaGroup(ruleFlowGroup).setFocus();

				newKieSession.fireAllRules();

//	    	output2 = (OutputParam) newKieSession.getObject(output2Fact);
				output = (OutputParam) newKieSession.getObject(factHandle);
				answersToSave = (Answers) newKieSession.getObject(answersToSaveHandle);
//	    	// HACK
//	    	if (!output2.getResultCode().equalsIgnoreCase("DUMMY")) {
//	    		output = output2;
//	    	}
//
				QEventMessage msg = (QEventMessage) items.get("message");
				if (msg != null) {

					JsonObject cachedOutputJson = VertxUtils.readCachedJson(userToken.getRealm(),
							"OUTPUT:" + msg.getData().getCode(), userToken.getToken());
					if (cachedOutputJson.getString("status").equalsIgnoreCase("ok")) {
						OutputParam o = JsonUtils.fromJson(cachedOutputJson.getString("status"), OutputParam.class);
						if (o != null) {
							output = o;
						}
					}
				}

				resultMap.put("output", output);
				resultMap.put("answersToSave", answersToSave);
				newKieSession.retract(ruleDetailsHandle);
				newKieSession.retract(factHandle);
				newKieSession.retract(answersToSaveHandle);
				newKieSession.retract(beUtilsHandle);
				newKieSession.retract(capabilityUtilsHandle);
				newKieSession.retract(kieSessionHandle);

				for (FactHandle allow : allowables) {
					newKieSession.retract(allow);
				}

				newKieSession.dispose();
			}
		}

		// notify manager that work item has been completed
		manager.completeWorkItem(workItem.getId(), resultMap);

	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, notifications cannot be aborted
	}

}