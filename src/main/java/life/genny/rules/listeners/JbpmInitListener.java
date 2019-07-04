package life.genny.rules.listeners;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.Logger;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.event.process.ProcessVariableChangedEvent;
import org.kie.api.runtime.process.WorkflowProcessInstance;

import life.genny.models.GennyToken;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.rules.QRules;
import life.genny.utils.RulesUtils;

public class JbpmInitListener implements ProcessEventListener {
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	long processStartTime = 0;
	GennyToken gennyToken;

	public JbpmInitListener(final GennyToken gennyToken) {
		this.gennyToken = gennyToken;
	}

	@Override
	public void beforeProcessStarted(ProcessStartedEvent event) {
		processStartTime = System.nanoTime();
		WorkflowProcessInstance process = (WorkflowProcessInstance) event.getProcessInstance();
////		log.info("jBPM event 'beforeProcessStarted'. Process ID: " + process.getId()
////				+ ", Process definition ID: " + process.getProcessId() + ", Process name: "
////				+ process.getProcessName() + ", Process state: " + process.getState() + ", Parent process ID: "
////				+ process.getParentProcessInstanceId());
//		processStart(process, gennyToken);
//		printProcessText(process, gennyToken,
//				"Number of passed objs =" + event.getKieRuntime().getEntryPoint("DEFAULT").getObjects().size());
		event.getKieRuntime().getEntryPoint("DEFAULT").getObjects().forEach(obj -> {

			if (obj instanceof String) {
				process.setVariable("name", (String) obj);
				/* System.out.println("FOUND STRING"); */
			} else if (obj instanceof QEventMessage) {
				QEventMessage msg = (QEventMessage) obj;
				process.setVariable("message", msg);
//				printProcessText(process, gennyToken,
//						"FOUND QEventMessage  " + msg.getEvent_type() + ":" + msg.getMsg_type());

			} else if (obj instanceof QRules) {
				process.setVariable("rules", (QRules) obj);
			//	printProcessText(process, gennyToken, "FOUND QRULE ");

			} else if (obj instanceof GennyToken) {
				GennyToken gennyToken = (GennyToken) obj;
				if ("PER_SERVICE".equals(gennyToken.getCode())) {
					process.setVariable("serviceToken", gennyToken);
				} else {
					process.setVariable("userToken", gennyToken);
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

		//event.getKieRuntime().insert(process);
		//System.out.println("Number of passed objs =" + event.getKieRuntime().getEntryPoint("DEFAULT").getObjects().size());

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
		// TODO Auto-generated method stub

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
			// Check if parent process exists , if so then indent .. (To indicate it is a sub process)
			String indent= process.getParentProcessInstanceId()<0?"":(process.getParentProcessInstanceId()+">>>>>>>>>>");
			
			String starttext = RulesUtils.executeRuleLogger(indent+">>>>>>>>>> START PROCESS ",
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
			String indent= process.getParentProcessInstanceId()<0?"":(process.getParentProcessInstanceId()+">>>>>>>>>>");

			String text = processDetails(process, gennyToken) + "  time=" + differenceMs + " ms"; // This is
																									// faster
																									// than
																									// calling
																									// getUser()
			String starttext = RulesUtils.executeRuleLogger(indent+">>>>>>>>>> END PROCESS", text, RulesUtils.ANSI_RED,
					RulesUtils.ANSI_YELLOW) + (GennySettings.devMode ? "" : RulesUtils.ANSI_RED)
					+ (GennySettings.devMode ? "" : RulesUtils.ANSI_RESET);

			RulesUtils.println(starttext);

		} catch (NullPointerException e) {
			RulesUtils.println("Error in process: " + gennyToken.getRealm() + ":" + process.getProcessName(),
					"ANSI_RED");
		}

	}

	private void printProcessText(WorkflowProcessInstance process, GennyToken gennyToken, final String text) {

		try {
			String indent= process.getParentProcessInstanceId()<0?"":(process.getParentProcessInstanceId()+">>>>>>>>>>");

			String starttext = RulesUtils.executeRuleLogger(indent+">>>>>>>>>>     PROCESS:" + processDetails(process, gennyToken), text,
					RulesUtils.ANSI_RED, RulesUtils.ANSI_YELLOW) + (GennySettings.devMode ? "" : RulesUtils.ANSI_RED)
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

}
