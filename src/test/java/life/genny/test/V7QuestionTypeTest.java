package life.genny.test;

import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import life.genny.qwanda.Ask;
import life.genny.qwanda.Context;
import life.genny.qwanda.ContextList;
import life.genny.qwanda.Link;
import life.genny.qwanda.Question;
import life.genny.qwanda.Context.VisualControlType;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityQuestion;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QMessage;
import life.genny.qwanda.validation.Validation;
import life.genny.qwanda.validation.ValidationList;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;

public class V7QuestionTypeTest {
	
public static final String SKIP_NEWQA_TEST = "TRUE";
	
	private static String ENV_GENNY_BRIDGE_URL = "http://bridge.genny.life";
	
	@Test
	public void testOnlyIfSkipIsDisabled() {
		assumeTrue("FALSE".equals(SKIP_NEWQA_TEST));
		
		BaseEntity testBe = new BaseEntity("BEG_TEST_BE", "Question Group");	
		
		/* create a frame-content baseentity */
		BaseEntity contentBe = new BaseEntity("FRM_CONTENT", "content-frame");
		
		
		/* Get the on-the-fly question attribute */
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));
		
		/* We generate the question-group from test baseentity */
		Question testQuestionGroup = new Question("QUE_" + testBe.getCode() + "_GRP", testBe.getName(), questionAttribute,
				true);
		
		Ask totalQuestionGrp = new Ask(testQuestionGroup, "PER_USER1", testBe.getCode());
		
		Attribute firstNameAttribute = new Attribute("PRI_FIRSTNAME", "link", new DataType(String.class));
				
		/* put the method which you want to test */
		Ask testQuestion1 = getQuestionWithLabel(firstNameAttribute);
		Ask testQuestion2 = getQuestionWithNoLabel(firstNameAttribute);
		Ask testQuestion3 = getQuestionWithLabelAndMandatory(firstNameAttribute);
		Ask testQuestion4 = getQuestionWithLabelHintMandatory(firstNameAttribute);
		Ask testQuestion5 = getQuestionWithLabelAndDescription(firstNameAttribute);
		Ask testQuestion6 = getQuestionWithLabelAndIcon(firstNameAttribute);
		
		List<Ask> questionList = new ArrayList<>();
		questionList.add(testQuestion1);
		questionList.add(testQuestion2);
		questionList.add(testQuestion3);
		questionList.add(testQuestion4);
		questionList.add(testQuestion5);
		questionList.add(testQuestion6);
		
		/* adding the child ask to the parent question group */
		Ask[] testAskArr = questionList.stream().toArray(Ask[]::new);	
		totalQuestionGrp.setChildAsks(testAskArr);
		
		Ask[] parentAsk = { totalQuestionGrp };
		
		/* Creating AskMessage with complete asks */
		QDataAskMessage totalAskMsg = new QDataAskMessage(parentAsk);
		sendTestMsg(totalAskMsg);
		
		String questionCode = totalQuestionGrp.getQuestion().getCode();
		
		Link testLink = new Link(contentBe.getCode(), questionCode, "LNK_ASK", "CENTRE");

		/* we create the entity entity */
		EntityQuestion entityQuestion = new EntityQuestion(testLink);
		
		/* creating entity entity between content-frame and question-group */
		Set<EntityQuestion> entQuestionList = new HashSet<>();
		entQuestionList.add(entityQuestion);
		
		/* publish frameBe */
		contentBe.setQuestions(entQuestionList);
		QDataBaseEntityMessage contentBeMsg = new QDataBaseEntityMessage(contentBe);
		sendTestMsg(contentBeMsg);	
	}
	
	/* Simple question which displays 1 question with label */
	public Ask getQuestionWithLabel(Attribute att) {
		
		Question firstNameQuestion = new Question("QUE_QUE1", "Last Name", att, true);
		Ask firstNameAsk = new Ask(firstNameQuestion, "PER_USER1", "BEG_TEST_BE");
		
		/* create visual baseentity for question with label */
		BaseEntity visualBaseEntity = new BaseEntity("THM_VISUAL_CONTROL_TWO", "Theme Visual Control Two");
		
		Attribute labelAttr = new Attribute("PRI_HAS_LABEL", "Has Label?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute labelEntityAttribute = new EntityAttribute(visualBaseEntity, labelAttr, 1.0, "TRUE");
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(labelEntityAttribute);	
		visualBaseEntity.setBaseEntityAttributes(attributeSet);
		
		QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(visualBaseEntity);
		/* send visual baseentity */
		sendTestMsg(beMsg);
		
		Context visualContext = new Context("THEME", visualBaseEntity, VisualControlType.DEFAULT);
		
		/* create theme for input */
		BaseEntity inputBackgroundColorThemeBe = new BaseEntity("THM_BACKGROUND_GRAY", "gray bg");
		Context inputBackgroundColorContext = new Context("THEME", inputBackgroundColorThemeBe, VisualControlType.INPUT);
		
		BaseEntity inputColorThemeBe = new BaseEntity("THM_COLOR_WHITE", "white");
		Context inputColorContext = new Context("THEME", inputColorThemeBe, VisualControlType.INPUT);
		
		/* creating list of contexts */
		List<Context> contexts = new ArrayList<>();
		contexts.add(visualContext);
		contexts.add(inputBackgroundColorContext);
		contexts.add(inputColorContext);
		
		/* add the context to the contextList */
		ContextList contextList = new ContextList(contexts);
		firstNameAsk.setContextList(contextList);
		
		return firstNameAsk;
	}
	
	/* Simple question which displays 1 question (displaying value for the attribute) */
	public Ask getQuestionWithNoLabel(Attribute att) { 
		
		Question question = new Question("QUE_QUE2", "Last Name", att);
		Ask ask = new Ask(question, "PER_USER1", "BEG_TEST_BE");
		
		return ask;
	}
	
	/* Simple question which displays 1 question with label, mandatory themes */
	public Ask getQuestionWithLabelAndMandatory(Attribute att) { 
		
		Question question = new Question("QUE_QUE3", "Last Name", att, true);
		Ask ask = new Ask(question, "PER_USER1", "BEG_TEST_BE");
		
		/* create visual baseentity for question with label */
		BaseEntity visualBaseEntity = new BaseEntity("THM_VISUAL_CONTROL_ONE", "Theme Visual Control One");
		
		Attribute labelAttr = new Attribute("PRI_HAS_LABEL", "Has Label?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute labelEntityAttribute = new EntityAttribute(visualBaseEntity, labelAttr, 1.0, "TRUE");
		
		Attribute isRequiredAttr = new Attribute("PRI_HAS_REQUIRED", "Has Required?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute isRequiredEntityAttribute = new EntityAttribute(visualBaseEntity, isRequiredAttr, 1.0, "TRUE");
		
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(labelEntityAttribute);	
		attributeSet.add(isRequiredEntityAttribute);
		visualBaseEntity.setBaseEntityAttributes(attributeSet);
		
		QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(visualBaseEntity);
		/* send visual baseentity */
		sendTestMsg(beMsg);
		
		Context visualContext = new Context("THEME", visualBaseEntity, VisualControlType.DEFAULT);
		
		/* create theme for input */
		BaseEntity inputBackgroundColorThemeBe = new BaseEntity("THM_BACKGROUND_GRAY", "gray bg");
		Context inputBackgroundColorContext = new Context("THEME", inputBackgroundColorThemeBe, VisualControlType.INPUT);
		
		BaseEntity inputColorThemeBe = new BaseEntity("THM_COLOR_WHITE", "white");
		Context inputColorContext = new Context("THEME", inputColorThemeBe, VisualControlType.INPUT);
		
		/* creating list of contexts */
		List<Context> contexts = new ArrayList<>();
		contexts.add(visualContext);
		contexts.add(inputColorContext);
		contexts.add(inputBackgroundColorContext);
		/* add the context to the contextList */
		ContextList contextList = new ContextList(contexts);
		
		ask.setContextList(contextList);
		ask.setMandatory(true);
		return ask;
	}

	/* Simple question which displays 1 question with label and an indication for mandatory and hint */
	public Ask getQuestionWithLabelHintMandatory(Attribute att) { 
		
		att.setHelp("Example hint");
		Question question = new Question("QUE_QUE4", "Last Name", att, true);
		Ask ask = new Ask(question, "PER_USER1", "BEG_TEST_BE");
		
		/* create visual baseentity for question with label */
		BaseEntity visualBaseEntity = new BaseEntity("THM_VISUAL_CONTROL_4", "Theme Visual Control One");
		
		Attribute labelAttr = new Attribute("PRI_HAS_LABEL", "Has Label?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute labelEntityAttribute = new EntityAttribute(visualBaseEntity, labelAttr, 1.0, "TRUE");
		
		Attribute isRequiredAttr = new Attribute("PRI_HAS_REQUIRED", "Has Required?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute isRequiredEntityAttribute = new EntityAttribute(visualBaseEntity, isRequiredAttr, 1.0, "TRUE");
		
		Attribute hintAttr = new Attribute("PRI_HAS_HINT", "Has Hint?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute hintEntityAttribute = new EntityAttribute(visualBaseEntity, hintAttr, 1.0, "TRUE");
		
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(labelEntityAttribute);	
		attributeSet.add(isRequiredEntityAttribute);
		attributeSet.add(hintEntityAttribute);
		visualBaseEntity.setBaseEntityAttributes(attributeSet);
		
		QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(visualBaseEntity);
		/* send visual baseentity */
		sendTestMsg(beMsg);
		
		Context visualContext = new Context("THEME", visualBaseEntity, VisualControlType.DEFAULT);
		
		/* create theme for input */
		BaseEntity inputBackgroundColorThemeBe = new BaseEntity("THM_BACKGROUND_GRAY", "gray bg");
		Context inputBackgroundColorContext = new Context("THEME", inputBackgroundColorThemeBe, VisualControlType.INPUT);
		
		BaseEntity inputColorThemeBe = new BaseEntity("THM_COLOR_WHITE", "white");
		Context inputColorContext = new Context("THEME", inputColorThemeBe, VisualControlType.INPUT);
		
		/* creating list of contexts */
		List<Context> contexts = new ArrayList<>();
		contexts.add(visualContext);
		contexts.add(inputColorContext);
		contexts.add(inputBackgroundColorContext);
		/* add the context to the contextList */
		ContextList contextList = new ContextList(contexts);
		
		ask.setContextList(contextList);
		ask.setMandatory(true);
		return ask;
	}
	
	private ValidationList getTextValidation() {
		Validation validation = new Validation("VLD_NON_EMPTY", "EmptyandBlankValues", "(?!^$|\\s+)");
		List<Validation> validations = new ArrayList<>();
		validations.add(validation);
		ValidationList validationList = new ValidationList();
		validationList.setValidationList(validations);
		return validationList;
	}
	
	/* Simple question which displays 1 question with label and description */
	public Ask getQuestionWithLabelAndDescription(Attribute att) { 
		
		att.setDescription("Example Description");
		att.setHelp("Example help");
		Question question = new Question("QUE_QUE5", "Last Name", att, true);
		Ask ask = new Ask(question, "PER_USER1", "BEG_TEST_BE");
		
		/* create visual baseentity for question with label */
		BaseEntity visualBaseEntity = new BaseEntity("THM_VISUAL_CONTROL_5", "Theme Visual Control One");
		
		Attribute labelAttr = new Attribute("PRI_HAS_LABEL", "Has Label?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute labelEntityAttribute = new EntityAttribute(visualBaseEntity, labelAttr, 1.0, "TRUE");
		
		Attribute descriptionAttr = new Attribute("PRI_HAS_DESCRIPTION", "Has Description?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute descriptionEntityAttribute = new EntityAttribute(visualBaseEntity, descriptionAttr, 1.0, "FALSE");
		
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(labelEntityAttribute);	
		attributeSet.add(descriptionEntityAttribute);
		visualBaseEntity.setBaseEntityAttributes(attributeSet);
		
		QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(visualBaseEntity);
		/* send visual baseentity */
		sendTestMsg(beMsg);
		
		Context visualContext = new Context("THEME", visualBaseEntity, VisualControlType.DEFAULT);
		
		/* create theme for input */
		BaseEntity inputBackgroundColorThemeBe = new BaseEntity("THM_BACKGROUND_GRAY", "gray bg");
		Context inputBackgroundColorContext = new Context("THEME", inputBackgroundColorThemeBe, VisualControlType.INPUT);
		
		BaseEntity inputColorThemeBe = new BaseEntity("THM_COLOR_WHITE", "white");
		Context inputColorContext = new Context("THEME", inputColorThemeBe, VisualControlType.INPUT);
		
		/* creating list of contexts */
		List<Context> contexts = new ArrayList<>();
		contexts.add(visualContext);
		contexts.add(inputColorContext);
		contexts.add(inputBackgroundColorContext);
		/* add the context to the contextList */
		ContextList contextList = new ContextList(contexts);
		
		ask.setContextList(contextList);
		ask.setMandatory(true);
		return ask;
	}
	
	/* Simple question which displays 1 question with label and icon */
	public Ask getQuestionWithLabelAndIcon(Attribute att) { 
		
		att.setDescription("Example Description");
		att.setHelp("Example help");
		Question question = new Question("QUE_QUE6", "Last Name", att, true);
		Ask ask = new Ask(question, "PER_USER1", "BEG_TEST_BE");
		
		/* create visual baseentity for question with label */
		BaseEntity visualBaseEntity = new BaseEntity("THM_VISUAL_CONTROL_6", "Theme Visual Control One");
		
		Attribute labelAttr = new Attribute("PRI_HAS_LABEL", "Has Label?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute labelEntityAttribute = new EntityAttribute(visualBaseEntity, labelAttr, 1.0, "TRUE");
		
		Attribute iconAttr = new Attribute("PRI_HAS_ICON", "Has Icon?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute iconEntityAttribute = new EntityAttribute(visualBaseEntity, iconAttr, 1.0, "TRUE");
		
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(labelEntityAttribute);	
		attributeSet.add(iconEntityAttribute);
		visualBaseEntity.setBaseEntityAttributes(attributeSet);
		
		QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(visualBaseEntity);
		/* send visual baseentity */
		sendTestMsg(beMsg);
		
		Context visualContext = new Context("THEME", visualBaseEntity, VisualControlType.DEFAULT);		
		
		/* create theme for input */
		BaseEntity inputBackgroundColorThemeBe = new BaseEntity("THM_BACKGROUND_GRAY", "gray bg");
		Context inputBackgroundColorContext = new Context("THEME", inputBackgroundColorThemeBe, VisualControlType.INPUT);
		
		BaseEntity inputColorThemeBe = new BaseEntity("THM_COLOR_WHITE", "white");
		Context inputColorContext = new Context("THEME", inputColorThemeBe, VisualControlType.INPUT);
		
		/* create theme for icon */
		//Context iconContext = new Context("THEME", inputBackgroundColorThemeBe, VisualControlType.ICON);
		
		/* creating list of contexts */
		List<Context> contexts = new ArrayList<>();
		contexts.add(visualContext);
		contexts.add(inputColorContext);
		//contexts.add(iconContext);
		contexts.add(inputBackgroundColorContext);
		/* add the context to the contextList */
		ContextList contextList = new ContextList(contexts);
		
		ask.setContextList(contextList);
		ask.setMandatory(true);
		return ask;
	}
	
	/* publishes the test-messages to front-end through bridge */
	private <T extends QMessage> void sendTestMsg(T msg) {

		try {
			String token = KeycloakUtils.getAccessToken("http://keycloak.genny.life", "genny", "genny",
					"056b73c1-7078-411d-80ec-87d41c55c3b4", "user1", "password1");
			msg.setToken(token);

			/* get the bridge url to publish the message to webcmd channel */
			String bridgetUrl = ENV_GENNY_BRIDGE_URL + "/api/service?channel=webdata";

			QwandaUtils.apiPostEntity(bridgetUrl, JsonUtils.toJson(msg), token);

		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}

}
