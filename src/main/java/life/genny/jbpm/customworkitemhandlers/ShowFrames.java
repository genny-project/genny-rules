package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import com.google.gson.reflect.TypeToken;

import io.vertx.core.json.JsonObject;
import life.genny.model.OutputParamTreeSet;
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
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.FrameUtils2;
import life.genny.qwanda.utils.OutputParam;
import life.genny.utils.VertxUtils;
import life.genny.models.FramePosition;

public class ShowFrames implements WorkItemHandler {


	public ShowFrames() {
	}

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		/* items used to save the extracted input parameters from the custom task */
		if (workItem == null) {
			log.error("ShowFrames has null workItem -  aborting");
			return;
		}
		Map<String, Object> items = workItem.getParameters();

		// extract parameters
		GennyToken userToken = (GennyToken) workItem.getParameter("userToken");
		GennyToken serviceToken = (GennyToken) workItem.getParameter("serviceToken");
		OutputParamTreeSet dom = (OutputParamTreeSet) workItem.getParameter("dom");
		
		String callingWorkflow = (String)items.get("callingWorkflow");
		if (StringUtils.isBlank(callingWorkflow)) {
			callingWorkflow = "";
		}
		callingWorkflow += ":"+workItem.getProcessInstanceId()+": "; 


		if (userToken == null) {
			log.error(callingWorkflow+": Must supply userToken!");

		} else {
			// log.info("userToken = " + userToken.getCode());

			if (dom == null) {
				log.error(callingWorkflow+": Must supply a dom (Document Object Model treeSet)!");
			} else {
			//	log.info(callingWorkflow+": root Frame Code sent to display  = " + rootFrameCode);

				for (OutputParam outputParam : dom.getTree2()) {
				
					String rootFrameCode = outputParam.getResultCode();
					String targetFrameCode = outputParam.getTargetCode();
					BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken,userToken);
					//if (!StringUtils.isBlank(targetFrameCode)) {
						ShowFrame.display(beUtils, rootFrameCode, targetFrameCode, callingWorkflow,outputParam);
					//}
				}
			}

		}

		// notify manager that work item has been completed
		if (workItem == null) {
			log.error(callingWorkflow+": workItem is null");
		}
		manager.completeWorkItem(workItem.getId(), null);

	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, notifications cannot be aborted
	}

}