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
import life.genny.qwanda.Context.VisualControlType;
import life.genny.qwanda.ContextList;
import life.genny.qwanda.ContextType;
import life.genny.qwanda.Link;
import life.genny.qwanda.Question;
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

public class V7FormQuestionTest {
	
	public static final String SKIP_NEWQA_TEST = "TRUE";
	
	private static String ENV_GENNY_BRIDGE_URL = "http://bridge.genny.life";
	
	@Test
	public void testOnlyIfSkipIsDisabled() {
		assumeTrue("FALSE".equals(SKIP_NEWQA_TEST));
		sendQuestionContentFrame();
	}
	
	public void sendQuestionContentFrame() {
		
		/* create a frame-content baseentity */
		BaseEntity contentBe = new BaseEntity("FRM_CONTENT", "content-frame");
		
		Ask testQuestion = generateQuestion();
		
		Ask[] testAskArr = { testQuestion };
		/* Creating AskMessage with complete asks */
		QDataAskMessage totalAskMsg = new QDataAskMessage(testAskArr);
		sendTestMsg(totalAskMsg);
		
		String questionCode = testQuestion.getQuestion().getCode();
		
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
	
	private Ask generateQuestion() {
		
		BaseEntity testBe = new BaseEntity("BEG_TEST_BE", "Question Group");
		
		/* Get the on-the-fly question attribute */
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));
		Attribute firstNameAttribute = new Attribute("PRI_FIRSTNAME", "link", new DataType(String.class));
		Attribute lastNameAttribute = new Attribute("PRI_LASTNAME", "link", new DataType(String.class));
		Attribute emailAttribute = new Attribute("PRI_EMAIL", "link", new DataType(String.class));
		
		/* We generate the question-group from test baseentity */
		Question testQuestionGroup = new Question("QUE_" + testBe.getCode() + "_GRP", testBe.getName(), questionAttribute,
				true);
		Ask totalQuestionGrp = new Ask(testQuestionGroup, "PER_USER1", testBe.getCode());
		totalQuestionGrp.setContextList(getContextListForForm());
		
		/* creating child question */
		Question firstNameQuestion = new Question("QUE_FIRSTNAME", "First Name", firstNameAttribute, true);
		Ask firstNameAsk = new Ask(firstNameQuestion, "PER_USER1", testBe.getCode());
		firstNameAsk.setMandatory(true);
		
		Question lastNameQuestion = new Question("QUE_LASTNAME", "Last Name", lastNameAttribute, true);
		Ask lastNameAsk = new Ask(lastNameQuestion, "PER_USER1", testBe.getCode());
		lastNameAsk.setMandatory(true);
		
		Question emailQuestion = new Question("QUE_EMAIL", "Email", emailAttribute, true);
		Ask emailAsk = new Ask(emailQuestion, "PER_USER1", testBe.getCode());
		emailAsk.setContextList(getContextListForFormQuestion());
		emailAsk.setMandatory(true);
		
		List<Ask> askList = new ArrayList<>();
		askList.add(firstNameAsk);
		askList.add(lastNameAsk);
		askList.add(emailAsk);
		
		Ask[] childAskArr = askList.stream().toArray(Ask[]::new);
		
		totalQuestionGrp.setChildAsks(childAskArr);
		
		return totalQuestionGrp;
	}
	
	
	private ContextList getContextListForFormQuestion() {
		
		BaseEntity visualTheme = getVisualBaseEntityForEmailInput();
		Context visualContext = new Context(ContextType.THEME, visualTheme, VisualControlType.DEFAULT);
	
		/* create theme for label */
		BaseEntity labelThemeBe = new BaseEntity("THM_COLOR_RED", "red");
		Context labelContext = new Context(ContextType.THEME, labelThemeBe, VisualControlType.LABEL);
		
		/* create default theme for question */
		BaseEntity defaultThemeBe = new BaseEntity("THM_COLOR_BLACK", "black");
		Context defaultContext = new Context(ContextType.THEME, defaultThemeBe, VisualControlType.DEFAULT);
		
		/* create theme for input */
		BaseEntity inputColorThemeBe = new BaseEntity("THM_COLOR_WHITE", "white");
		Context inputColorContext = new Context(ContextType.THEME, inputColorThemeBe, VisualControlType.INPUT);
		
		//BaseEntity inputBackgroundThemeBe = new BaseEntity("THM_BACKGROUND_WHITE", "white bg");
		
		/* create theme for icon */
		//Context iconContext = new Context("THEME", inputBackgroundThemeBe, VisualControlType.DEFAULT);
		
		/* creating list of contexts */
		List<Context> contexts = new ArrayList<>();
		contexts.add(labelContext);
		contexts.add(defaultContext);
		contexts.add(visualContext);
		contexts.add(inputColorContext);

		/* add the context to the contextList */
		ContextList contextList = new ContextList(contexts);

		return contextList;
	}
	
	private ContextList getContextListForForm() {
		/* create context */
		/* getting the visual theme baseentity */
		BaseEntity visualTheme = getVisualBaseEntity();
		Context visualContext = new Context(ContextType.THEME, visualTheme, VisualControlType.DEFAULT);
	
		/* create theme for input */
		BaseEntity inputBackgroundColorThemeBe = new BaseEntity("THM_BACKGROUND_GRAY", "gray bg");
		Context inputBackgroundColorContext = new Context(ContextType.THEME, inputBackgroundColorThemeBe, VisualControlType.INPUT);
		
		BaseEntity inputColorThemeBe = new BaseEntity("THM_COLOR_WHITE", "white");
		Context inputColorContext = new Context(ContextType.THEME, inputColorThemeBe, VisualControlType.INPUT);
		
		BaseEntity iconThemeBe = new BaseEntity("THM_BACKGROUND_WHITE", "white bg");
		Context iconContext = new Context(ContextType.THEME, iconThemeBe, VisualControlType.ICON);
		
		/* creating list of contexts */
		List<Context> contexts = new ArrayList<>();
		contexts.add(visualContext);
		contexts.add(inputColorContext);
		contexts.add(inputBackgroundColorContext);
		contexts.add(iconContext);

		/* add the context to the contextList */
		ContextList contextList = new ContextList(contexts);

		return contextList;
	}

	public BaseEntity getVisualBaseEntity() {
		BaseEntity visualBaseEntity = new BaseEntity("THM_VISUAL_CONTROL_ONE", "Theme Visual Control One");
		
		Attribute labelAttr = new Attribute("PRI_HAS_LABEL", "Has Label?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute labelEntityAttribute = new EntityAttribute(visualBaseEntity, labelAttr, 1.0, "TRUE");
		
		Attribute isRequiredAttr = new Attribute("PRI_HAS_REQUIRED", "Has Required?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute isRequiredEntityAttribute = new EntityAttribute(visualBaseEntity, isRequiredAttr, 1.0, "TRUE");
		
		/*Attribute hintAttr = new Attribute("PRI_HAS_HINT", "Has Hint?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute hintEntityAttribute = new EntityAttribute(visualBaseEntity, hintAttr, 1.0, "FALSE");
		
		Attribute descriptionAttr = new Attribute("PRI_HAS_DESCRIPTION", "Has Description?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute descriptionEntityAttribute = new EntityAttribute(visualBaseEntity, descriptionAttr, 1.0, "FALSE"); 
		
		Attribute iconAttr = new Attribute("PRI_HAS_ICON", "Has Icon?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute iconEntityAttribute = new EntityAttribute(visualBaseEntity, iconAttr, 1.0, true); */
		
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(labelEntityAttribute);
		attributeSet.add(isRequiredEntityAttribute);
		//attributeSet.add(hintEntityAttribute);
		//attributeSet.add(descriptionEntityAttribute);
		//attributeSet.add(iconEntityAttribute);
		
		visualBaseEntity.setBaseEntityAttributes(attributeSet);
		
		QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(visualBaseEntity);
		sendTestMsg(beMsg);
		
		return visualBaseEntity;
	}
	
	public BaseEntity getVisualBaseEntityForEmailInput() {
		BaseEntity visualBaseEntity = new BaseEntity("THM_VISUAL_CONTROL_TWO", "Theme Visual Control Two");
		
		Attribute labelAttr = new Attribute("PRI_HAS_LABEL", "Has Label?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute labelEntityAttribute = new EntityAttribute(visualBaseEntity, labelAttr, 1.0, "TRUE");
		
		Attribute isRequiredAttr = new Attribute("PRI_HAS_REQUIRED", "Has Required?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute isRequiredEntityAttribute = new EntityAttribute(visualBaseEntity, isRequiredAttr, 1.0, "TRUE");
		
		/*Attribute hintAttr = new Attribute("PRI_HAS_HINT", "Has Hint?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute hintEntityAttribute = new EntityAttribute(visualBaseEntity, hintAttr, 1.0, "FALSE");
		
		Attribute descriptionAttr = new Attribute("PRI_HAS_DESCRIPTION", "Has Description?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute descriptionEntityAttribute = new EntityAttribute(visualBaseEntity, descriptionAttr, 1.0, "FALSE"); */
		
		Attribute iconAttr = new Attribute("PRI_HAS_ICON", "Has Icon?", new DataType("Text", getTextValidation(), "Text"));
		EntityAttribute iconEntityAttribute = new EntityAttribute(visualBaseEntity, iconAttr, 1.0, "TRUE");
		
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(labelEntityAttribute);
		attributeSet.add(isRequiredEntityAttribute);
		//attributeSet.add(hintEntityAttribute);
		//attributeSet.add(descriptionEntityAttribute);
		attributeSet.add(iconEntityAttribute);
		
		visualBaseEntity.setBaseEntityAttributes(attributeSet);
		
		QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(visualBaseEntity);
		sendTestMsg(beMsg);
		
		return visualBaseEntity;
	}
		
	private ValidationList getTextValidation() {
		Validation validation = new Validation("VLD_NON_EMPTY", "EmptyandBlankValues", "(?!^$|\\s+)");
		List<Validation> validations = new ArrayList<>();
		validations.add(validation);
		ValidationList validationList = new ValidationList();
		validationList.setValidationList(validations);
		return validationList;
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
