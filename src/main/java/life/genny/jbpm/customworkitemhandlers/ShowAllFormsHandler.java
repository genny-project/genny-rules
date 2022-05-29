package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import com.google.gson.reflect.TypeToken;

import life.genny.models.GennyToken;
import life.genny.qwanda.Ask;
import life.genny.qwanda.ContextType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.rules.QRules;
import life.genny.utils.QuestionUtils;
import life.genny.utils.VertxUtils;

public class ShowAllFormsHandler implements WorkItemHandler {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		// extract parameters
		QRules rules = (QRules) workItem.getParameter("rules");

	//	log.info("QRules = " + rules);

		String apiUrl = GennySettings.qwandaServiceUrl + ":4242/service/forms";
		log.info("Fetching setup info from " + apiUrl);
		GennyToken userToken = rules.getToken();
		try {

			/* create a test baseentity */
			BaseEntity testBe = new BaseEntity("GRP_FORM_TEST_BE", "Forms test!");
			
			BaseEntity rootGrp = VertxUtils.readFromDDT(rules.realm(),  "GRP_ROOT", true,rules.getServiceToken());
			log.info("ROOT_GRP "+rootGrp);
			rootGrp = rules.createVirtualLink(rootGrp, testBe, "LINK_CORE", "TEST",1000.0);
			QDataBaseEntityMessage rootMsg = new QDataBaseEntityMessage(rootGrp);
			rules.publishCmd(rootMsg);
						

			/* get the theme */
			BaseEntity expandable = rules.baseEntity.getBaseEntityByCode("THM_EXPANDABLE");

			/* create an ask */
			Ask testBeAsk = QuestionUtils.createQuestionForBaseEntity(testBe, true, rules.getToken());
			rules.createVirtualContext(testBeAsk, expandable, ContextType.THEME);

			String jsonFormCodes = QwandaUtils.apiGet(apiUrl, userToken);
			if (!"You need to be a test.".equals(jsonFormCodes)) {
				Type type = new TypeToken<List<String>>() {
				}.getType();
				List<String> formCodes = JsonUtils.fromJson(jsonFormCodes, type);
				log.info("Form Codes=" + formCodes);

				List<Ask> askList = new ArrayList<>();
				for (String formCode : formCodes) {
					log.info("Asking group = "+formCode);
					/*
					 * create child Asks for the parents question to test different formats of
					 * question groups
					 */
					BaseEntity grpBe = new BaseEntity(formCode, formCode);
					Ask ask = QuestionUtils.createQuestionForBaseEntity(grpBe, false, rules.getToken());
					/* collect all child asks and set to the parent ask */
					askList.add(ask);

					}


				Ask[] childAskArr = askList.stream().toArray(Ask[]::new);
				;
				testBeAsk.setChildAsks(childAskArr);

				Ask[] askArr = { testBeAsk };

				QDataAskMessage totalAskMsg = new QDataAskMessage(askArr);
				rules.publishCmd(totalAskMsg);

				BaseEntity headerFrameBe = rules.baseEntity.getBaseEntityByCode("FRM_HEADER");

				/* link the form-testing related question and link it to sidebar */
				headerFrameBe = rules.createVirtualLink(headerFrameBe, testBeAsk, "LNK_ASK", "EAST");

				QDataBaseEntityMessage testMsg = new QDataBaseEntityMessage(headerFrameBe);
				rules.publishCmd(testMsg);

			} else {
				log.info("Ensure that the user you are using has a 'test' role ...");
			}

		} catch (Exception e) {
			log.info(e);
		}

		// notify manager that work item has been completed
		manager.completeWorkItem(workItem.getId(), null);

	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, notifications cannot be aborted
	}

}