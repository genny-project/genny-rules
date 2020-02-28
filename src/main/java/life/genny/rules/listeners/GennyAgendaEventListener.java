package life.genny.rules.listeners;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.Logger;
import org.drools.core.WorkingMemory;
import org.drools.core.event.ActivationCancelledEvent;
import org.drools.core.event.ActivationCreatedEvent;
import org.drools.core.event.AfterActivationFiredEvent;
import org.drools.core.event.BeforeActivationFiredEvent;
import org.kie.api.event.rule.AgendaGroupPoppedEvent;
import org.kie.api.event.rule.AgendaGroupPushedEvent;
import org.kie.api.event.rule.RuleFlowGroupActivatedEvent;
import org.kie.api.event.rule.RuleFlowGroupDeactivatedEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;

public class GennyAgendaEventListener extends DefaultAgendaEventListener{

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


        public void activationCreated(ActivationCreatedEvent event,

                WorkingMemory workingMemory){

        }

        public void activationCancelled(ActivationCancelledEvent event,

                  WorkingMemory workingMemory){

        }

        public void beforeActivationFired(BeforeActivationFiredEvent event,

                    WorkingMemory workingMemory) {

        }

        public void afterActivationFired(AfterActivationFiredEvent event,

                   WorkingMemory workingMemory) {

        }

        public void agendaGroupPopped(AgendaGroupPoppedEvent event,

                WorkingMemory workingMemory) {

        }



        public void agendaGroupPushed(AgendaGroupPushedEvent event,

                WorkingMemory workingMemory) {

        }

        public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event,

                           WorkingMemory workingMemory) {

        }

     
        public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event,

                WorkingMemory workingMemory) {
        	super.afterRuleFlowGroupActivated(event);
        	log.info("Fire All Rules in GennyAgendaEventListener");
            workingMemory.fireAllRules();

        }

        public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event,

                             WorkingMemory workingMemory) {

        }

        public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event,

                            WorkingMemory workingMemory) {

        }



}
