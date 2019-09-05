package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import com.google.gson.reflect.TypeToken;

import io.vertx.core.json.JsonObject;
import life.genny.models.Frame3;
import life.genny.models.GennyToken;
import life.genny.qwanda.Ask;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.validation.Validation;
import life.genny.qwandautils.JsonUtils;
import life.genny.rules.QRules;
import life.genny.utils.FrameUtils2;
import life.genny.utils.VertxUtils;
import life.genny.models.FramePosition;

public class SendMessage implements WorkItemHandler {

	KieSession kieSession = null;

	public SendMessage(KieSession kieSession) {
		this.kieSession = kieSession;
	}

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		RuleFlowProcessInstance message = (RuleFlowProcessInstance) workItem.getParameter("Message");
        log.info("Sending message hello: {}", message);
        String name= "safal";
        this.kieSession.signalEvent("Message-"+name,message);
        manager.completeWorkItem(workItem.getId(), null);

	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, notifications cannot be aborted
	}

}