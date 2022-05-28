package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import life.genny.qwanda.message.QEventWorkflowMessage;
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
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.runtime.StatefulKnowledgeSession;

import io.vertx.core.json.JsonObject;
import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.Answers;
import life.genny.qwanda.ESessionType;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.datatype.Allowed;
import life.genny.qwanda.datatype.AllowedSafe;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwanda.rule.RuleDetails;
import life.genny.qwanda.utils.OutputParam;
import life.genny.qwandautils.JsonUtils;
import life.genny.rules.QRules;
import life.genny.rules.RulesLoader;
import life.genny.rules.listeners.GennyRuleTimingListener;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.CapabilityUtils;
import life.genny.utils.CapabilityUtilsRefactored;
import life.genny.utils.RulesUtils;
import life.genny.utils.VertxUtils;

public class RuleFlowGroupWorkItemHandler implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public static GennyRuleTimingListener ruleTimingListener = new GennyRuleTimingListener();
	
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

		log.info(callingWorkflow + ":pid" + workItem.getProcessInstanceId() + " Running rule flow group "
				+ ruleFlowGroup);

		final Map<String, Object> resultMap = executeRules(serviceToken, userToken, items, ruleFlowGroup,
				callingWorkflow);

		// notify manager that work item has been completed
		manager.completeWorkItem(workItem.getId(), resultMap);

	}

	/**
	 * @param workItem
	 * @return
	 */
	public Map<String, Object> executeRules(GennyToken serviceToken, GennyToken userToken, Map<String, Object> items,
			String ruleFlowGroup, String callingWorkflow) {
		/* resultMap is used to map the result Value to the output parameters */
		final Map<String, Object> resultMap = new ConcurrentHashMap<String, Object>();

		try {
			if (userToken == null) {
				userToken = serviceToken;
			}

			//log.info(callingWorkflow + " Running rule flow group " + ruleFlowGroup + " #1");

			BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken,userToken);
			CapabilityUtils oldCapabilityUtils = null;
			CapabilityUtilsRefactored newCapabilityUtils = new CapabilityUtilsRefactored(beUtils);

			// Old capability utils stuffs
			if (items.containsKey("capabilityUtils")) {
				oldCapabilityUtils = (CapabilityUtils) items.get("capabilityUtils");
			} else {
				JsonObject json = VertxUtils.readCachedJson(userToken.getRealm(), "CAPABILITIES", userToken);
				if ("OK".equalsIgnoreCase(json.getString("status"))) {
					JsonObject valueJson = json.getJsonObject("value");
					String value = valueJson.toString();
					oldCapabilityUtils = JsonUtils.fromJson(value, CapabilityUtils.class);
					if (oldCapabilityUtils == null) {
						oldCapabilityUtils = new CapabilityUtils(beUtils);
					}
				} else {
					oldCapabilityUtils = new CapabilityUtils(beUtils);
				}

			}
		
			BaseEntity user = null;
			if ((VertxUtils.cachedEnabled) && ("PER_SERVICE".equals(userToken.getUserCode()))) {
				// need to create the server user in cache if not there
				user = VertxUtils.readFromDDT(userToken.getRealm(), userToken.getUserCode(), userToken);
				if (user == null) {
					beUtils.setServiceToken(serviceToken);
					BaseEntity defBE = beUtils.getDEFByCode("DEF_USER");
					BaseEntity serviceUser = beUtils.create(defBE, "Service User", userToken.getUserCode());
					Attribute roleAttribute = RulesUtils.getAttribute("PRI_IS_ADMIN", serviceToken);

					beUtils.saveAnswer(new Answer(serviceUser, serviceUser, roleAttribute, "TRUE"));
				}
			} else {

			}
			//log.info(callingWorkflow + " Running rule flow group " + ruleFlowGroup + " #2");
			user = beUtils.getBaseEntityByCode(userToken.getUserCode());
			List<Allowed> allowable = CapabilityUtils.generateAlloweds(userToken, user);
			List<AllowedSafe> safeAllowables = CapabilityUtilsRefactored.generateAlloweds(userToken, user);
			
			// Add all alloweds into the safeAllowables as AllowedSafes
			safeAllowables.addAll(allowable.stream().map((allowed) -> AllowedSafe.fromAllowed(allowed)).collect(Collectors.toList()));

			if (StringUtils.isBlank(callingWorkflow)) {
				callingWorkflow = "";
			}

//			log.info(callingWorkflow + " Running rule flow group " + ruleFlowGroup + " #3");
			RuleDetails ruleDetails = new RuleDetails(callingWorkflow, ruleFlowGroup);

			if (serviceToken == null) {
				log.error("Must supply serviceToken!");
//			} else if (("PER_SERVICE".equals(userToken.getUserCode()))) {
//				log.error(
//						"Should not run Service Token as a normal user. (and not serviceToken");
			} else {

				// log.info("serviceToken = "+serviceToken.getCode());
				// log.info("Running rule flow group "+ruleFlowGroup);

				// log.info("ProcessInstanceId = "+workItem.getProcessInstanceId());
				KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();
				// ksconf.setOption(TimedRuleExecutionOption.YES);

				KieSession newKieSession = null;

				OutputParam output = new OutputParam();
				Answers answersToSave = new Answers();

				if (this.runtimeEngine != null) {
					KieBase kieBase = RulesLoader.getKieBaseCache().get(serviceToken.getRealm());
					newKieSession = (StatefulKnowledgeSession) kieBase.newKieSession(ksconf, RulesLoader.env);

//			newKieSession = (StatefulKnowledgeSession)this.runtimeEngine.getKieSession();
//					log.info(callingWorkflow + " Running rule flow group " + ruleFlowGroup + " #4");
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
							if (!"service".equals(gToken.getUsername())) {
								/* Generate a QRules */
								// log.info("Adding rules to facts");
								QRules rules = new QRules(serviceToken, gToken);
								newKieSession.insert(rules);
							}
						}
					}
//					log.info(callingWorkflow + " Running rule flow group " + ruleFlowGroup + " #5");
					/* INserting facts to save the output result */
					FactHandle factHandle = newKieSession.insert(output);
					FactHandle answersToSaveHandle = newKieSession.insert(answersToSave);
					FactHandle kieSessionHandle = newKieSession.insert(newKieSession);
					FactHandle beUtilsHandle = newKieSession.insert(beUtils);
					FactHandle oldCapabilityUtilsHandle = newKieSession.insert(oldCapabilityUtils);
					FactHandle newCapabilityUtilsHandle = newKieSession.insert(newCapabilityUtils);
					
					ESessionType eSessionType = ESessionType.SESSION;
					if (callingWorkflow != null) {
						if (callingWorkflow.contains("userSession")) {
							eSessionType = ESessionType.SESSION;
							// This is a userSession rules firing, so indicate that in the rules
						} else if (callingWorkflow.contains("QDataB2BMessage")){
							eSessionType = ESessionType.B2B;
						} 
					}
					
					FactHandle sessionType = newKieSession.insert(eSessionType);
					
					QBulkMessage payload = new QBulkMessage();
					newKieSession.setGlobal("payload", payload);
					
					Set<String> stringSet = new HashSet<String>();
					newKieSession.setGlobal("stringSet", stringSet);
					/* FactHandle payloadHandle = newKieSession.insert(payload); */
//					log.info(callingWorkflow + " Running rule flow group " + ruleFlowGroup + " #6");
					List<FactHandle> allowables = new ArrayList<FactHandle>();
					List<FactHandle> safeAllowableHandles = new ArrayList<FactHandle>(); 
					// get User capabilities
					// first get User Roles

					for (AllowedSafe allow : safeAllowables) {
						safeAllowableHandles.add(newKieSession.insert(allow));
					}

					// get each capability from each Role and add to allowables
					for (Allowed allow : allowable) {
						allowables.add(newKieSession.insert(allow));
					}

					/* Setting focus to rule-flow group */
					newKieSession.getAgenda().getAgendaGroup(ruleFlowGroup).setFocus();
					
					newKieSession.addEventListener(ruleTimingListener);
					newKieSession.fireAllRules();
//					log.info(callingWorkflow + " Running rule flow group " + ruleFlowGroup + " #7");
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
					/* payload = (QBulkMessage) newKieSession.getObject(payloadHandle); */
					payload = (QBulkMessage) newKieSession.getGlobal("payload");
					stringSet = (Set<String>) newKieSession.getGlobal("stringSet");
					resultMap.put("payload", payload);
					resultMap.put("stringSet", stringSet);
					resultMap.put("output", output);
					resultMap.put("answersToSave", answersToSave);
					newKieSession.retract(ruleDetailsHandle);
					newKieSession.retract(factHandle);
					newKieSession.retract(answersToSaveHandle);
					newKieSession.retract(beUtilsHandle);
					newKieSession.retract(oldCapabilityUtilsHandle);
					newKieSession.retract(newCapabilityUtilsHandle);
					newKieSession.retract(sessionType);
					newKieSession.retract(kieSessionHandle); // don't dispose
					/* newKieSession.retract(payloadHandle); */

//					log.info(callingWorkflow + " Running rule flow group " + ruleFlowGroup + " #8");

					for (FactHandle allowHandle : safeAllowableHandles) {
						newKieSession.retract(allowHandle);
					}
					for (FactHandle allow : allowables) {
						newKieSession.retract(allow);
					}

					newKieSession.dispose();

				} else {
					// TODO: This can almost definitely be abstracted into its own method and this code duplication is kinda ridiculous
					KieBase kieBase = RulesLoader.getKieBaseCache().get(serviceToken.getRealm());
					newKieSession = (StatefulKnowledgeSession) kieBase.newKieSession(ksconf, RulesLoader.env);
//					log.info(callingWorkflow + " Running rule flow group " + ruleFlowGroup + " #10a");
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
//					log.info(callingWorkflow + " Running rule flow group " + ruleFlowGroup + " #11a");
//			OutputParam output2 = new OutputParam2();
//			FactHandle output2Fact = newKieSession.insert(output2);

					/* INserting facts to save the output result */
					FactHandle factHandle = newKieSession.insert(output);
					FactHandle answersToSaveHandle = newKieSession.insert(answersToSave);
					FactHandle beUtilsHandle = newKieSession.insert(beUtils);
					FactHandle oldCapabilityUtilsHandle = newKieSession.insert(oldCapabilityUtils);
					FactHandle newCapabilityUtilsHandle = newKieSession.insert(newCapabilityUtils);
					FactHandle kieSessionHandle = newKieSession.insert(newKieSession);

					QBulkMessage payload = new QBulkMessage();
//				FactHandle payloadHandle = newKieSession.insert(payload);
					newKieSession.setGlobal("payload", payload);
					Set<String> stringSet = new HashSet<String>();
					newKieSession.setGlobal("stringSet", stringSet);

					List<FactHandle> allowedSafeHandles = new ArrayList<FactHandle>();
//					log.info(callingWorkflow + " Running rule flow group " + ruleFlowGroup + " #12a");
					List<FactHandle> allowables = new ArrayList<FactHandle>();
					// get User capabilities

					// inject kieSession
					for (Allowed allow : allowable) {
						allowables.add(newKieSession.insert(allow));
					}

					for(AllowedSafe allow : safeAllowables) {
						allowedSafeHandles.add(newKieSession.insert(allow));
					}

					/* Setting focus to rule-flow group */
					newKieSession.getAgenda().getAgendaGroup(ruleFlowGroup).setFocus();
					newKieSession.addEventListener(new GennyRuleTimingListener());
					newKieSession.fireAllRules();
//					log.info(callingWorkflow + " Running rule flow group " + ruleFlowGroup + " #13a");
//	    	output2 = (OutputParam) newKieSession.getObject(output2Fact);
					output = (OutputParam) newKieSession.getObject(factHandle);
					answersToSave = (Answers) newKieSession.getObject(answersToSaveHandle);
					oldCapabilityUtils = (CapabilityUtils) newKieSession.getObject(oldCapabilityUtilsHandle);
					newCapabilityUtils = (CapabilityUtilsRefactored) newKieSession.getObject(newCapabilityUtilsHandle);
//				payload = (QBulkMessage) newKieSession.getObject(payloadHandle);
//	    	// HACK
//	    	if (!output2.getResultCode().equalsIgnoreCase("DUMMY")) {
//	    		output = output2;
//	    	}
//					log.info(callingWorkflow + " Running rule flow group " + ruleFlowGroup + " #14a");
					Object msgObject = (Object) items.get("message");
					if ((msgObject != null) && (msgObject instanceof QEventWorkflowMessage)){
						output = ((QEventWorkflowMessage) msgObject).getOutputParam();
						log.info("Setting output from QEventWorkflowMessage!!!");
					}else {
						QEventMessage msg = (QEventMessage) items.get("message");
						if (msg != null) {
							JsonObject cachedOutputJson = VertxUtils.readCachedJson(userToken.getRealm(),
									"OUTPUT:" + msg.getData().getCode(), userToken);
							if (cachedOutputJson.getString("status").equalsIgnoreCase("ok")) {
								JsonObject valueJson = cachedOutputJson.getJsonObject("value");
								OutputParam o = JsonUtils.fromJson(valueJson.toString(), OutputParam.class);
								if (o != null) {
									output = o;
									log.info("Setting output from QEventMessage Cache!!!");
							}
						}
					}}

					if (output == null) {
						output = new OutputParam();
						log.info("Output param was NULL! Creating new output param");
					}

					resultMap.put("output", output);

					if (answersToSave != null) {
						resultMap.put("answersToSave", answersToSave);
					}
					payload = (QBulkMessage) newKieSession.getGlobal("payload");
					stringSet = (Set<String>) newKieSession.getGlobal("stringSet");
					if (payload != null) {
						resultMap.put("payload", payload);
					}
					if (oldCapabilityUtils != null) {
						resultMap.put("capabilityUtils", oldCapabilityUtils);
					}
					if (newCapabilityUtils != null) {
						resultMap.put("capabilityUtilsRefactored", newCapabilityUtils);
					}
					if (stringSet != null) {
						resultMap.put("stringSet", stringSet);
					}
					newKieSession.retract(ruleDetailsHandle);
					newKieSession.retract(factHandle);
					newKieSession.retract(answersToSaveHandle);
					newKieSession.retract(beUtilsHandle);
					newKieSession.retract(oldCapabilityUtilsHandle);
					newKieSession.retract(newCapabilityUtilsHandle);
					newKieSession.retract(kieSessionHandle);
//				newKieSession.retract(payloadHandle);

					for (FactHandle allow : allowables) {
						newKieSession.retract(allow);
					}
					for (FactHandle allow : allowedSafeHandles) {
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
