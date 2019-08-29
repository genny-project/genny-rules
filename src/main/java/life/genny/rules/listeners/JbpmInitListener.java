package life.genny.rules.listeners;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TransactionRequiredException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.apache.logging.log4j.Logger;
import org.drools.core.audit.WorkingMemoryLogger;
import org.drools.core.audit.event.LogEvent;
import org.drools.core.audit.event.RuleFlowLogEvent;
import org.drools.core.audit.event.RuleFlowNodeLogEvent;
import org.drools.core.audit.event.RuleFlowVariableLogEvent;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.event.process.ProcessVariableChangedEvent;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieRuntime;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkflowProcessInstance;

import life.genny.model.NodeStatus;
import life.genny.models.GennyToken;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.rules.QRules;
import life.genny.utils.CallingProcessToken;
import life.genny.utils.RulesUtils;
import life.genny.utils.VertxUtils;

public class JbpmInitListener  /*extends WorkingMemoryLogger*/ implements ProcessEventListener {
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	
    private static final String[] KNOWN_UT_JNDI_KEYS = new String[] {"UserTransaction", "java:jboss/UserTransaction", System.getProperty("jbpm.ut.jndi.lookup")};
    
    protected Environment env;
     
    private boolean isJTA = true;
    private boolean sharedEM = false;


	long processStartTime = 0;
	GennyToken serviceToken;
	GennyToken userToken;
	VertxUtils vertxUtils;

	public JbpmInitListener(final GennyToken serviceToken, final GennyToken userToken) {
		this.serviceToken = serviceToken;
		this.userToken = userToken;

	}

	public JbpmInitListener(final GennyToken serviceToken) {
		this.serviceToken = serviceToken;
	}

	public JbpmInitListener(VertxUtils vertxUtils) {
		this.vertxUtils = vertxUtils;
	}
	
//    public JbpmInitListener(KnowledgeRuntimeEventManager session) {
//        super(session);
//        if (session instanceof KieRuntime) {
//            env = ((KnowledgeRuntime) session).getEnvironment();
//        } else if (session instanceof StatelessKnowledgeSessionImpl) {
//            env = ((StatelessKnowledgeSessionImpl) session).getEnvironment();
//        } else {
//            throw new IllegalArgumentException(
//                "Not supported session in logger: " + session.getClass());
//        }
//        Boolean bool = (Boolean) env.get("IS_JTA_TRANSACTION");
//        if (bool != null) {
//            isJTA = bool.booleanValue();
//        }
//    }

	@Override
	public void beforeProcessStarted(ProcessStartedEvent event) {
		processStartTime = System.nanoTime();
		WorkflowProcessInstance process = (WorkflowProcessInstance) event.getProcessInstance();

		process.setVariable("serviceToken", serviceToken);

		if (this.userToken != null) {
			GennyToken existing = (GennyToken) process.getVariable("userToken");
			if ((existing == null) || ((!existing.getSessionCode().equals(userToken.getSessionCode())))) { // save
																											// adding a
																											// duplicate

				process.setVariable("userToken", userToken);
			}
		}

////		log.info("jBPM event 'beforeProcessStarted'. Process ID: " + process.getId()
////				+ ", Process definition ID: " + process.getProcessId() + ", Process name: "
////				+ process.getProcessName() + ", Process state: " + process.getState() + ", Parent process ID: "
////				+ process.getParentProcessInstanceId());
//		processStart(process, gennyToken);
//		printProcessText(process, gennyToken,
//				"Number of passed objs =" + event.getKieRuntime().getEntryPoint("DEFAULT").getObjects().size());
		event.getKieRuntime().getEntryPoint("DEFAULT").getObjects().forEach(obj -> {

			if (obj instanceof Long) {
				process.setVariable("callingProcessId", (Long) obj); // TODO, use a class!
				System.out.println("FOUND LONG " + (Long) obj);
			} else if (obj instanceof String) {
				process.setVariable("name", (String) obj);
				/* System.out.println("FOUND STRING"); */
			} else if (obj instanceof QEventMessage) {
				QEventMessage msg = (QEventMessage) obj;
				process.setVariable("message", msg);
//				printProcessText(process, gennyToken,
//						"FOUND QEventMessage  " + msg.getEvent_type() + ":" + msg.getMsg_type());

			} else if (obj instanceof QRules) {
				process.setVariable("rules", (QRules) obj);
				// printProcessText(process, gennyToken, "FOUND QRULE ");
			} else if (obj instanceof CallingProcessToken) {
				process.setVariable("callingProcessToken", (CallingProcessToken) obj);
				// printProcessText(process, gennyToken, "FOUND QRULE ");

			} else if (obj instanceof GennyToken) {
				GennyToken gennyToken = (GennyToken) obj;
				if (("PER_SERVICE".equals(gennyToken.getCode())) || ("serviceToken".equals(gennyToken.getCode()))) {
					// System.out.println("JbpmListener: serviceToken "+gennyToken.getUserCode()+"
					// processId="+process.getProcessId()+" -> session_state:
					// "+gennyToken.getSessionCode());
					process.setVariable("serviceToken", gennyToken);
				} else {
					GennyToken existing = (GennyToken) process.getVariable("userToken");
					if ((existing == null) || ((!existing.getSessionCode().equals(gennyToken.getSessionCode())))) { // save
																													// adding
																													// a
																													// duplicate
						process.setVariable("userToken", gennyToken);
						String processId = process.getProcessId();
						if (processId.contains("ession")) { // only bother with session type workflows
							System.out.println("JbpmListener: userToken " + gennyToken.getUserCode() + " processId="
									+ process.getProcessId() + ":" + process.getId() + " -> session_state: "
									+ gennyToken.getSessionCode());
							VertxUtils.writeCachedJson(gennyToken.getRealm(), gennyToken.getSessionCode(),
									process.getId() + "", gennyToken.getToken());
							// System.out.println("JbpmListener: userToken "+gennyToken.getUserCode()+"
							// processId="+process.getProcessId()+" -> session_state:
							// "+gennyToken.getSessionCode()+" written to Cache");

						}
					}
				}
//				printProcessText(process, gennyToken, "FOUND GennyToken  " + gennyToken.getCode());

//			} else if (obj instanceof org.apache.logging.log4j.Logger) {
//				org.apache.logging.log4j.Logger log = (org.apache.logging.log4j.Logger) obj;
//				process.setVariable("log", log);
//				printProcessText(process, gennyToken, "FOUND Logger  ");

			} else {
//				printProcessText(process, gennyToken, "FOUND OBJ " + obj.getClass().getSimpleName());
			}
		});

		// event.getKieRuntime().insert(process);
		// System.out.println("Number of passed objs =" +
		// event.getKieRuntime().getEntryPoint("DEFAULT").getObjects().size());

		// Now save this session_state to the Cache associated with the processId

	}

	@Override
	public void afterProcessStarted(ProcessStartedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeProcessCompleted(ProcessCompletedEvent event) {
		WorkflowProcessInstance process = (WorkflowProcessInstance) event.getProcessInstance();
//      log.info("jBPM event 'beforeProcessCompleted'. Process ID: " + process.getId()
//              + ", Process definition ID: " + process.getProcessId() + ", Process name: "
//              + process.getProcessName() + ", Process state: " + process.getState() + ", Parent process ID: "
//              + process.getParentProcessInstanceId());
//

		long endTime = System.nanoTime();
		double difference = (endTime - processStartTime) / 1e6; // get ms
//		processEnd(process, gennyToken, difference);

	}

	@Override
	public void afterProcessCompleted(ProcessCompletedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
		String workflowCode = (String)event.getNodeInstance().getVariable("workflowcode");
		if (workflowCode != null) {
		NodeInstance nodeInstance = event.getNodeInstance();
		Date eventDate = event.getEventDate();
		ProcessInstance processInstance = event.getProcessInstance();
		String processId = processInstance.getProcessId();
		Long processInstanceId = processInstance.getId();
		String realm = userToken.getRealm();
		String nodeName = nodeInstance.getNodeName();
		String nodeId = nodeInstance.getNode().getId()+"";
		GennyToken userToken = (GennyToken)nodeInstance.getVariable("userToken");
		String userCode = userToken.getUserCode();
		NodeStatus nodeStatus = new NodeStatus(userCode, nodeName, nodeId, realm, processInstanceId,
				processId, workflowCode);
		
		}
	}

	@Override
	public void beforeNodeLeft(ProcessNodeLeftEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterNodeLeft(ProcessNodeLeftEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeVariableChanged(ProcessVariableChangedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterVariableChanged(ProcessVariableChangedEvent event) {
		// TODO Auto-generated method stub

	}

	private void processStart(WorkflowProcessInstance process, GennyToken gennyToken) {

		try {
			// Check if parent process exists , if so then indent .. (To indicate it is a
			// sub process)
			String indent = process.getParentProcessInstanceId() < 0 ? ""
					: (process.getParentProcessInstanceId() + ">>>>>>>>>>");

			String starttext = RulesUtils.executeRuleLogger(indent + ">>>>>>>>>> START PROCESS ",
					processDetails(process, gennyToken), RulesUtils.ANSI_RED, RulesUtils.ANSI_YELLOW)
					+ (GennySettings.devMode ? "" : RulesUtils.ANSI_RED)
					+ (GennySettings.devMode ? "" : RulesUtils.ANSI_RESET);

			RulesUtils.println(starttext);

		} catch (NullPointerException e) {
			RulesUtils.println("Error in process: " + processDetails(process, gennyToken), "ANSI_RED");
		}

	}

	private void processEnd(WorkflowProcessInstance process, GennyToken gennyToken, double differenceMs) {

		try {
			String indent = process.getParentProcessInstanceId() < 0 ? ""
					: (process.getParentProcessInstanceId() + ">>>>>>>>>>");

			String text = processDetails(process, gennyToken) + "  time=" + differenceMs + " ms"; // This is
																									// faster
																									// than
																									// calling
																									// getUser()
			String starttext = RulesUtils.executeRuleLogger(indent + ">>>>>>>>>> END PROCESS", text,
					RulesUtils.ANSI_RED, RulesUtils.ANSI_YELLOW) + (GennySettings.devMode ? "" : RulesUtils.ANSI_RED)
					+ (GennySettings.devMode ? "" : RulesUtils.ANSI_RESET);

			RulesUtils.println(starttext);

		} catch (NullPointerException e) {
			RulesUtils.println("Error in process: " + gennyToken.getRealm() + ":" + process.getProcessName(),
					"ANSI_RED");
		}

	}

	private void printProcessText(WorkflowProcessInstance process, GennyToken gennyToken, final String text) {

		try {
			String indent = process.getParentProcessInstanceId() < 0 ? ""
					: (process.getParentProcessInstanceId() + ">>>>>>>>>>");

			String starttext = RulesUtils.executeRuleLogger(
					indent + ">>>>>>>>>>     PROCESS:" + processDetails(process, gennyToken), text, RulesUtils.ANSI_RED,
					RulesUtils.ANSI_YELLOW) + (GennySettings.devMode ? "" : RulesUtils.ANSI_RED)
					+ (GennySettings.devMode ? "" : RulesUtils.ANSI_RESET);

			RulesUtils.println(starttext);

		} catch (NullPointerException e) {
			RulesUtils.println("Error in process: " + processDetails(process, gennyToken), "ANSI_RED");
		}

	}

	private String processDetails(WorkflowProcessInstance process, GennyToken gennyToken) {
		return gennyToken.getRealm() + ":" + process.getId() + ":" + process.getProcessId() + ":"
				+ gennyToken.getString("preferred_username");
	}

    /**
     * This method creates a entity manager. 
     */
    private EntityManager getEntityManager() {
        EntityManager em = (EntityManager) env.get(EnvironmentName.CMD_SCOPED_ENTITY_MANAGER);
        if (em != null) {
            sharedEM = true;
            return em;
        }
        EntityManagerFactory emf = (EntityManagerFactory) env.get(EnvironmentName.ENTITY_MANAGER_FACTORY);
        if (emf != null) {
            return emf.createEntityManager();
        }
        throw new RuntimeException("Could not find EntityManager, both command-scoped EM and EMF in environment are null");
    }
 
    /**
     * This method persists the entity given to it. 
     * <p></p>
     * This method also makes sure that the entity manager used for persisting the entity, joins the existing JTA transaction. 
     * @param entity An entity to be persisted.
     */
    private void persist(Object entity) { 
        EntityManager em = getEntityManager();
        UserTransaction ut = joinTransaction(em);
        em.persist(entity);
        if (!sharedEM) {
            flush(em, ut);
        }
    }
     
    /**
     * This method opens a new transaction, if none is currently running, and joins the entity manager/persistence context
     * to that transaction. 
     * @param em The entity manager we're using. 
     * @return {@link UserTransaction} If we've started a new transaction, then we return it so that it can be closed. 
     * @throws NotSupportedException 
     * @throws SystemException 
     * @throws Exception if something goes wrong. 
     */
    private UserTransaction joinTransaction(EntityManager em) {
        boolean newTx = false;
        UserTransaction ut = null;
 
        if (isJTA) {
            try {
                em.joinTransaction();
             
            } catch (TransactionRequiredException e) {
                ut = findUserTransaction();
                try {
                    if( ut != null && ut.getStatus() == Status.STATUS_NO_TRANSACTION ) { 
                        ut.begin();
                        newTx = true;
                        // since new transaction was started em must join it
                        em.joinTransaction();
                    } 
                } catch(Exception ex) {
                    throw new IllegalStateException("Unable to find or open a transaction: " + ex.getMessage(), ex);
                }
                 
                if (!newTx) {
                    // rethrow TransactionRequiredException if UserTransaction was not found or started
                    throw e;
                }
 
            }
            
            if( newTx ) { 
                return ut;
            }
        }
        return null;
    }
 
    /**
     * This method closes the entity manager and transaction. It also makes sure that any objects associated 
     * with the entity manager/persistence context are detached. 
     * <p></p>
     * Obviously, if the transaction returned by the {@link #joinTransaction(EntityManager)} method is null, 
     * nothing is done with the transaction parameter.
     * @param em The entity manager.
     * @param ut The (user) transaction.
     */
    private static void flush(EntityManager em, UserTransaction ut) {
        em.flush(); // This saves any changes made
        em.clear(); // This makes sure that any returned entities are no longer attached to this entity manager/persistence context
        em.close(); // and this closes the entity manager
        try { 
            if( ut != null ) { 
                // There's a tx running, close it.
                ut.commit();
            }
        } catch(Exception e) { 
            log.error("Unable to commit transaction: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    protected static UserTransaction findUserTransaction() {
        InitialContext context = null;
        try {
            context = new InitialContext();
            return (UserTransaction) context.lookup( "java:comp/UserTransaction" );
        } catch ( NamingException ex ) {
             
            for (String utLookup : KNOWN_UT_JNDI_KEYS) {
                if (utLookup != null) {
                    try {
                        UserTransaction ut = (UserTransaction) context.lookup(utLookup);
                        return ut;
                    } catch (NamingException e) {
                        log.debug("User Transaction not found in JNDI under " + utLookup);
                         
                    }
                }
            }
            log.warn("No user transaction found under known names");
            return null;
        }
    }

//	@Override
//	public void logEventCreated(LogEvent logEvent) {
//        switch (logEvent.getType()) {
//        case LogEvent.BEFORE_RULEFLOW_CREATED:
//        case LogEvent.AFTER_RULEFLOW_COMPLETED:
//              break;
//        case LogEvent.BEFORE_RULEFLOW_NODE_TRIGGERED:
//        	// nodeEvent = (RuleFlowNodeLogEvent) logEvent;
//        	log.info("triggering on before ruleflow node");
//             break;
//        case LogEvent.BEFORE_RULEFLOW_NODE_EXITED:
//            break;
//        case LogEvent.AFTER_VARIABLE_INSTANCE_CHANGED:
//            RuleFlowVariableLogEvent variableEvent = (RuleFlowVariableLogEvent) logEvent;
//        //    addVariableLog(variableEvent.getProcessInstanceId(), variableEvent.getProcessId(), variableEvent.getVariableInstanceId(), variableEvent.getVariableId(), variableEvent.getObjectToString());
//            break;
//        default:
//            // ignore all other events
//    }
//		
//	}
}
