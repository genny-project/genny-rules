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

public class ShowAllFormsHandler implements WorkItemHandler {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		// extract parameters
		QRules rules = (QRules) workItem.getParameter("rules");

		log.info("QRules = " + rules);

		String apiUrl = GennySettings.qwandaServiceUrl + ":8280/service/forms";
		log.info("Fetching setup info from " + apiUrl);
		String userToken = rules.getToken();
		try {

			/* create a test baseentity */
			BaseEntity testBe = new BaseEntity("GRP_FORM_TEST_BE", "Forms test");

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

//				List<Ask> askList = new ArrayList<>();
//				String userCode = rules.getUser().getCode();
//		//		 String formCode = "QUE_BUYER_GRP";
//				int i=0;
//				for (String formCode : formCodes) {
//					log.info("Asking group = "+formCode);
//					if ("QUE_BUYER_COMPANY_GRP".equals(formCode)) {
//					rules.sendForm(formCode, userCode, userCode);
//					/*
//					 * create child Asks for the parents question to test different formats of
//					 * question groups
//					 */
//					BaseEntity grpBe = new BaseEntity(formCode, formCode);
//					Ask ask = QuestionUtils.createQuestionForBaseEntity(grpBe, false, rules.getToken());
//					/* collect all child asks and set to the parent ask */
//					askList.add(ask);
//					}
//					i++;
//			//		if (i > 3) { break;}
//				}

//				formCode = "QUE_ADD_HOST_COMPANY_STAFF_GRP";
//				
//				rules.sendForm(formCode,userCode , userCode);
//				/* create child Asks for the parents question to test different formats of question groups */
//				grpBe = new BaseEntity(formCode, formCode);
//				ask = QuestionUtils.createQuestionForBaseEntity(grpBe, false,rules.getToken());
//				/* collect all child asks and set to the parent ask */				
//				askList.add(ask);
		
				/* create child Asks for the parents question to test different formats of question groups */
				BaseEntity buyerGroupTestBe = new BaseEntity("GRP_BUYER_GROUP_FORM_TEST_BE", "Buyer group test form");
				Ask buyerGroupAsk = QuestionUtils.createQuestionForBaseEntity(buyerGroupTestBe, false,rules.getToken());
				
				/* create child Ask for the test-seller question group to test different formats of question groups */
				BaseEntity sellerGroupTestBe = new BaseEntity("GRP_INTERN_GROUP_FORM_TEST_BE", "Intern group test form");
				Ask sellerGroupAsk = QuestionUtils.createQuestionForBaseEntity(sellerGroupTestBe, false,rules.getToken());
				
				/* create child Ask for the test-seller question group to test different formats of question groups */
				BaseEntity nestedInternshipTestBe = new BaseEntity("GRP_INTERNSHIP_NESTED_GROUP_FORM_TEST_BE", "Internship test nested form");
				Ask internshipAsk = QuestionUtils.createQuestionForBaseEntity(nestedInternshipTestBe, false,rules.getToken());
				
				/* create child Ask for the test-seller question group to test different formats of question groups */
				BaseEntity internProfileTestBe = new BaseEntity("GRP_INTERN_PROFILE_GROUP_FORM_TEST_BE", "Intern Profile test nested form");
				Ask internProfileAsk = QuestionUtils.createQuestionForBaseEntity(internProfileTestBe, false,rules.getToken());
				
				/* collect all child asks and set to the parent ask */
				List<Ask> askList = new ArrayList<>();
				askList.add(buyerGroupAsk);
				askList.add(sellerGroupAsk);
				askList.add(internshipAsk);
				askList.add(internProfileAsk);

				Ask[] childAskArr = askList.stream().toArray(Ask[]::new);
				;
				testBeAsk.setChildAsks(childAskArr);

				Ask[] askArr = { testBeAsk };

				QDataAskMessage totalAskMsg = new QDataAskMessage(askArr);
				rules.publishCmd(totalAskMsg);

				BaseEntity headerFrameBe = rules.baseEntity.getBaseEntityByCode("FRM_HEADER");

				/* link the form-testing related question and link it to sidebar */
				headerFrameBe = rules.createVirtualLink(headerFrameBe, testBeAsk, "LNK_ASK", "SOUTH");

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