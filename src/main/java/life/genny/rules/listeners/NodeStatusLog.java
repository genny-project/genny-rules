package life.genny.rules.listeners;

import java.util.Date;

/**
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TransactionRequiredException;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.drools.core.WorkingMemory;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.runtime.process.InternalProcessRuntime;
import org.drools.persistence.api.TransactionManager;
import org.jbpm.process.audit.AbstractAuditLogger;
import org.jbpm.process.audit.JPAWorkingMemoryDbLogger;
import org.jbpm.process.audit.NodeInstanceLog;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.jbpm.process.audit.variable.ProcessIndexerManager;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.process.instance.context.variable.VariableScopeInstance;
import org.jbpm.process.instance.impl.ProcessInstanceImpl;
import org.jbpm.workflow.instance.NodeInstance;
import org.jbpm.workflow.instance.impl.NodeInstanceImpl;
import org.kie.api.event.KieRuntimeEvent;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.event.process.ProcessVariableChangedEvent;
import org.kie.api.event.process.SLAViolatedEvent;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.utils.Log;

import life.genny.model.NodeStatus;
import life.genny.model.SessionPid;
import life.genny.models.GennyToken;
import life.genny.qwandautils.GennySettings;
import life.genny.utils.SessionFacts;

/**
 * Enables history log via JPA.
 * 
 */
public class NodeStatusLog extends AbstractAuditLogger {

	private static final Logger logger = LoggerFactory.getLogger(NodeStatusLog.class);

	private static final String[] KNOWN_UT_JNDI_KEYS = new String[] { "UserTransaction", "java:jboss/UserTransaction",
			System.getProperty("jbpm.ut.jndi.lookup") };

	private boolean isJTA = true;
	private boolean sharedEM = false;

	private EntityManagerFactory emf;

	private ProcessIndexerManager indexManager = ProcessIndexerManager.get();

	private Validator validator;
  
	/*
	 * for backward compatibility
	 */
	public NodeStatusLog(WorkingMemory workingMemory) {
		super(workingMemory);
		InternalProcessRuntime processRuntime = ((InternalWorkingMemory) workingMemory).getProcessRuntime();
		if (processRuntime != null) {
			processRuntime.addEventListener((ProcessEventListener) this);
		}
		  ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		    validator = factory.getValidator();

	}

	public NodeStatusLog(KieSession session) {
		Environment env = session.getEnvironment();
		internalSetIsJTA(env);
		session.addEventListener(this);
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();

	}

	/*
	 * end of backward compatibility
	 */

	public NodeStatusLog(EntityManagerFactory emf) {
		this.emf = emf;
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();

	}

	public NodeStatusLog() {
		// default constructor when this is used with a persistent KieSession
	}

	public NodeStatusLog(EntityManagerFactory emf, Environment env) {
		this.emf = emf;
		internalSetIsJTA(env);
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		 validator = factory.getValidator();

	}

	public NodeStatusLog(Environment env) {
		internalSetIsJTA(env);
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();

	}

	private void internalSetIsJTA(Environment env) {
		Boolean bool = (Boolean) env.get("IS_JTA_TRANSACTION");
		if (bool != null) {
			isJTA = bool.booleanValue();
		}
	}

	@Override
	@Transactional(dontRollbackOn = { org.hibernate.AssertionFailure.class })
	public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
		if (GennySettings.logWorkflows) {
			NodeInstanceLog log = (NodeInstanceLog) builder.buildEvent(event);
		//	persist(log, event);

			((NodeInstanceImpl) event.getNodeInstance()).getMetaData().put("NodeInstanceLog", log);
			Date eventDate = event.getEventDate();
		}
		String workflowStage = (String) event.getNodeInstance().getVariable("workflowStage");

		if (workflowStage != null) {

			org.kie.api.runtime.process.ProcessInstance processInstance = event.getProcessInstance();
			Long processInstanceId = processInstance.getId();

			EntityManager em = getEntityManager(event);
			/* Object tx = joinTransaction(em); */

			try {

				NodeStatus record = em.find(NodeStatus.class, processInstanceId);
				if (record != null) {

					if (record.getWorkflowStage() != workflowStage) {

						logger.info(" UPDATING " + record.toString() + " ENTRY IN NODESTATUS DB");
						record.setWorkflowStage(workflowStage);
						em.flush();
					}

				} else {

					org.kie.api.runtime.process.NodeInstance nodeInstance = event.getNodeInstance();

					String processId = processInstance.getProcessId();
					String nodeName = nodeInstance.getNodeName();
					String nodeId = nodeInstance.getNode().getId() + "";
					GennyToken userToken = (GennyToken) nodeInstance.getVariable("userToken");
					String realm = userToken.getRealm();
					String userCode = userToken.getUserCode();
					String workflowBeCode = (String) nodeInstance.getVariable("workflowBeCode");

					NodeStatus nodeStatus = new NodeStatus(userCode, nodeName, nodeId, realm, processInstanceId,
							processId, workflowStage, workflowBeCode);

					logger.info(nodeStatus.toString() + " NOT FOUND IN NODESTATUS DB !!");
					// em.persist(nodeStatus);
					persist(nodeStatus, event);
				}

			} catch (Exception e) {

				logger.error("Error in persisting nodeStatus");

			}
		}
	}

	@Override
	public void afterNodeLeft(ProcessNodeLeftEvent event) {
	}

	@Override
	@Transactional(dontRollbackOn = { Exception.class })
	public void afterVariableChanged(ProcessVariableChangedEvent event) {
		if (GennySettings.logWorkflows) {

			List<org.kie.api.runtime.manager.audit.VariableInstanceLog> variables = indexManager.index(getBuilder(),
					event);
			for (org.kie.api.runtime.manager.audit.VariableInstanceLog log : variables) {
				//persist(log, event);
			}
		}
	}

	@Override
	@Transactional(dontRollbackOn = { Exception.class })

	public void beforeProcessStarted(ProcessStartedEvent event) {
		if (GennySettings.logWorkflows) {

			ProcessInstanceLog log = (ProcessInstanceLog) builder.buildEvent(event);
			//persist(log, event);
			((ProcessInstanceImpl) event.getProcessInstance()).getMetaData().put("ProcessInstanceLog", log);
		}
		org.kie.api.runtime.process.ProcessInstance processInstance = event.getProcessInstance();
		
		String processName =  (String) event.getProcessInstance().getProcessName();
		
		if ("userSession".equals(processName)) {
			try {
				//Map<String, Object> variables =  processInstance.getProcess().getMetaData();  
				VariableScopeInstance variableScope = (VariableScopeInstance) 
		                   ((org.jbpm.process.instance.ProcessInstance) processInstance)
		                       .getContextInstance(VariableScope.VARIABLE_SCOPE);
				SessionFacts sessionFacts = (SessionFacts)variableScope.getVariable("sessionFacts");
				GennyToken userToken = sessionFacts.getUserToken();
				Long processInstanceId = processInstance.getId();
				String sessionCode = userToken.getSessionCode();
				String realm = userToken.getRealm(); //TODO
				SessionPid sessionPid = new SessionPid(userToken.getUserCode(),realm,sessionCode,processInstanceId);
				persist(sessionPid,event);
	
				logger.info("After Process Started");
			} catch (Exception e) {

				logger.error("Error in persisting nodeStatus");
			}
		}
	}

	@Override
	@Transactional(dontRollbackOn = { Exception.class })
	public void afterProcessCompleted(ProcessCompletedEvent event) {
		if (GennySettings.logWorkflows) {

			long processInstanceId = event.getProcessInstance().getId();
			EntityManager em = getEntityManager(event);
			Object tx = joinTransaction(em);

			ProcessInstanceLog log = (ProcessInstanceLog) ((ProcessInstanceImpl) event.getProcessInstance())
					.getMetaData().get("ProcessInstanceLog");
			if (log == null) {
				List<ProcessInstanceLog> result = em.createQuery(
						"from ProcessInstanceLog as log where log.processInstanceId = :piId and log.end is null")
						.setParameter("piId", processInstanceId).getResultList();
				if (result != null && result.size() != 0) {
					log = result.get(result.size() - 1);
				}
			}
			if (log != null) {
				log = (ProcessInstanceLog) builder.buildEvent(event, log);
				em.merge(log);
				
				// save the end date time to SessionPid
				
				logger.info("After Process Completed");
				List<SessionPid> result = em.createQuery(
						"from session_pid as spid where spid.pid = :piId")
						.setParameter("piId", processInstanceId).getResultList();
				if (result != null && result.size() != 0) {
					SessionPid  spid = result.get(result.size() - 1);
					spid.setSessionend(new Date());
					em.merge(spid);
				}
		
			}
			leaveTransaction(em, tx);
		}
	}

	@Override
	@Transactional(dontRollbackOn = { Exception.class })
	public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
		// trigger this to record some of the data (like work item id) after activity
		// was triggered
		if (GennySettings.logWorkflows) {

			NodeInstanceLog log = (NodeInstanceLog) ((NodeInstanceImpl) event.getNodeInstance()).getMetaData()
					.get("NodeInstanceLog");
			builder.buildEvent(event, log);
		}
	}

	@Override
	@Transactional(dontRollbackOn = { Exception.class })

	public void afterSLAViolated(SLAViolatedEvent event) {
		if (GennySettings.logWorkflows) {

			EntityManager em = getEntityManager(event);
			Object tx = joinTransaction(em);
			if (event.getNodeInstance() != null) {
				// since node instance is set this is SLA violation for node instance
				long nodeInstanceId = event.getNodeInstance().getId();
				long processInstanceId = event.getProcessInstance().getId();
				NodeInstanceLog log = (NodeInstanceLog) ((NodeInstanceImpl) event.getNodeInstance()).getMetaData()
						.get("NodeInstanceLog");
				if (log == null) {
					List<NodeInstanceLog> result = em.createQuery(
							"from NodeInstanceLog as log where log.nodeInstanceId = :niId and log.processInstanceId = :piId and log.type = 0")
							.setParameter("niId", Long.toString(nodeInstanceId)).setParameter("piId", processInstanceId)
							.getResultList();
					if (result != null && !result.isEmpty()) {
						log = result.get(result.size() - 1);
					}
				}
				if (log != null) {
					log.setSlaCompliance(((NodeInstance) event.getNodeInstance()).getSlaCompliance());
					em.merge(log);
				}
			} else {
				// SLA violation for process instance
				long processInstanceId = event.getProcessInstance().getId();
				ProcessInstanceLog log = (ProcessInstanceLog) ((ProcessInstanceImpl) event.getProcessInstance())
						.getMetaData().get("ProcessInstanceLog");
				if (log == null) {
					List<ProcessInstanceLog> result = em.createQuery(
							"from ProcessInstanceLog as log where log.processInstanceId = :piId and log.end is null")
							.setParameter("piId", processInstanceId).getResultList();
					if (result != null && !result.isEmpty()) {
						log = result.get(result.size() - 1);
					}
				}
				if (log != null) {
					log.setSlaCompliance(((ProcessInstance) event.getProcessInstance()).getSlaCompliance());
					em.merge(log);
				}
			}
			leaveTransaction(em, tx);
		}
	}

	@Override
	public void beforeNodeLeft(ProcessNodeLeftEvent event) {
		if (GennySettings.logWorkflows) {

			NodeInstanceLog log = (NodeInstanceLog) builder.buildEvent(event, null);
			persist(log, event);
		}
	}

	@Override
	public void beforeVariableChanged(ProcessVariableChangedEvent event) {

	}

	@Override
	@Transactional(dontRollbackOn = { org.hibernate.AssertionFailure.class })
	public void afterProcessStarted(ProcessStartedEvent event) {

	}

	@Override
	public void beforeProcessCompleted(ProcessCompletedEvent event) {
	}

	public void dispose() {
	}

	/**
	 * This method persists the entity given to it.
	 * </p>
	 * This method also makes sure that the entity manager used for persisting the
	 * entity, joins the existing JTA transaction.
	 * 
	 * @param entity An entity to be persisted.
	 */
	private void persist(Object entity, KieRuntimeEvent event) {
		EntityManager em = getEntityManager(event);
		Object tx = joinTransaction(em);
		if (entity instanceof NodeStatus) {
			Set<ConstraintViolation<NodeStatus>> constraintViolations =  validator.validate( (NodeStatus)entity );
			if (!constraintViolations.isEmpty()) {
				Log.error("NodeStatus constraint error: "+constraintViolations);
				leaveTransaction(em,tx);
				return;
			}
		} /*else {
			logger.warn("NON NodeStatus Object being persisted");
		}*/
		em.persist(entity);
		leaveTransaction(em, tx);
	}

	/**
	 * This method creates a entity manager.
	 */
	private EntityManager getEntityManager(KieRuntimeEvent event) {

		Environment env = event.getKieRuntime().getEnvironment();

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
					if (ut != null && ut.getStatus() == Status.STATUS_NO_TRANSACTION) {
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
	 * @param transaction The (user) transaction.
	 */
	private void leaveTransaction(EntityManager em, Object transaction) {
		if (isJTA) {
			try {
				if (transaction != null) {
					// There's a tx running, close it.
					((UserTransaction) transaction).commit();
				}
			} catch (Exception e) {
				logger.error("Unable to commit transaction: ", e);
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
				logger.error("Unable to close created EntityManager: {}", e.getMessage(), e);
			}
		}
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
						logger.debug("User Transaction not found in JNDI under {}", utLookup);

					}
				}
			}
			logger.warn("No user transaction found under known names");
			return null;
		}
	}

}