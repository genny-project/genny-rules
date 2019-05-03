package life.genny.test;

import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
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
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.EntityQuestion;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataAttributeMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QMessage;
import life.genny.qwanda.validation.Validation;
import life.genny.qwanda.validation.ValidationList;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;

public class V7Test {

	private static String ENV_GENNY_BRIDGE_URL = "http://bridge.genny.life";

	private static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	public static final String SKIP_NEWQA_TEST = "TRUE";

	@Test
	public void testOnlyIfSkipIsDisabled() {
		assumeTrue("FALSE".equals(SKIP_NEWQA_TEST));
		sendInitialFrame();
	}

	public void sendInitialFrame() {

		/* create table frame */
		BaseEntity frameTableBe = new BaseEntity("FRM_TABLE", "table-frame");

		/* create table-header frame */
		BaseEntity frameTableHeaderBe = new BaseEntity("FRM_TABLE_HEADER", "table-header");

		/* create table-content frame */
		BaseEntity frameTableContentBe = new BaseEntity("FRM_TABLE_CONTENT", "table-content");

		/* create table-footer frame */
		BaseEntity frameTableFooterBe = new BaseEntity("FRM_TABLE_FOOTER", "table-footer");
		
		/* create a frame-content baseentity */
		BaseEntity contentBe = new BaseEntity("FRM_CONTENT", "content-frame");

		/* creating a link */
		Attribute attribute = new Attribute("LNK_FRAME", "link", new DataType(String.class));

		/* link table-frame and table-header */
		EntityEntity headerEntityEntity = new EntityEntity(frameTableBe, frameTableHeaderBe, attribute, 1.0, "NORTH");

		/* link table-frame and table-content */
		EntityEntity ContentEntityEntity = new EntityEntity(frameTableBe, frameTableContentBe, attribute, 2.0,
				"CENTRE");

		/* link table-frame and table-footer */
		EntityEntity FooterEntityEntity = new EntityEntity(frameTableBe, frameTableFooterBe, attribute, 3.0, "SOUTH");

		Set<EntityEntity> entEntSet = new HashSet<>();
		entEntSet.add(headerEntityEntity);
		entEntSet.add(ContentEntityEntity);
		entEntSet.add(FooterEntityEntity);
		frameTableBe.setLinks(entEntSet);

		QDataBaseEntityMessage frameMsg = new QDataBaseEntityMessage(frameTableBe);
		sendTestMsg(frameMsg);

		/* send table header */
		sendTableHeader(frameTableBe, frameTableHeaderBe);

		/* send table content */
		sendTableContent(frameTableBe, frameTableContentBe);
		
		/* send table footer */
		sendTableFooter(frameTableBe, frameTableFooterBe);
		
		/* link content-frame to table-frame */
		linkTableToContentFrame(frameTableBe, contentBe);

	}
	
	public void sendTableFooter(BaseEntity frameTableBe, BaseEntity frameTableFooterBe) {
		
		/* Get the on-the-fly question attribute */
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));

		/* Construct a table footer question: QUE_FRM_TABLE_FOOTER_GRP */
		Question tableFooterQues = new Question("QUE_TABLE_FOOTER_GRP", "Table Footer", questionAttribute, true);
		
		/* Construct a table footer Ask */
		Ask tableFooterAsk = new Ask(tableFooterQues, "PER_USER1", "SBE_HOSTCOMPANIES_7fa24b4b-a19a-4938-b363-a40fe9aa5b28", false, 1.0, false, false, true);
		
		/* Ask List to store all the table-footer and table-column child asks */
		List<Ask> tableFooterChildAsks = new ArrayList<>();

		/* Validation for Dropdown */
		Validation dropdownValidation = new Validation("VLD_SELECT_NO_OF_INTERNS", "dropdown", "GRP_ITEMS_PER_PAGE", false, false);
		dropdownValidation.setRegex(".*");

		List<Validation> dropdownValidations = new ArrayList<>();
		dropdownValidations.add(dropdownValidation);
		ValidationList dropdownValidationList = new ValidationList();
		dropdownValidationList.setValidationList(dropdownValidations);


		/* Validation for Quesion Event */
		ValidationList eventValidationList = new ValidationList();
		Attribute previousAttr = new Attribute("PRI_PREVIOUS", "PRI_PREVIOUS", new DataType("Event", eventValidationList, "Event"));
		Attribute nextAttr = new Attribute("PRI_NEXT", "PRI_NEXT", new DataType("Event", eventValidationList, "Event"));
		Attribute dropdownAttr = new Attribute("LNK_ITEMS_PER_PAGE", "No Of Items", new DataType("dropdown", dropdownValidationList, "dropdown"));
		
		List<Attribute> attributeList = new ArrayList<>();
		attributeList.add(previousAttr);
		attributeList.add(nextAttr);
		attributeList.add(dropdownAttr);

		Attribute[] attributeArray = attributeList.toArray(new Attribute[0]);

		/* Send new attributes msg */
		QDataAttributeMessage attrMsg = new QDataAttributeMessage(attributeArray);
		sendTestMsg(attrMsg);


		/* we create BEs for dropdown */
		BaseEntity grpItems = new BaseEntity("GRP_ITEMS_PER_PAGE", "Items Per Page");
		BaseEntity five = new BaseEntity("SEL_FIVE", "5");
		BaseEntity ten = new BaseEntity("SEL_TEN", "10");
		BaseEntity fifteen = new BaseEntity("SEL_FIFTEEN", "15");

		List<BaseEntity> itemList = new ArrayList<>();
		itemList.add(five);
		itemList.add(ten);
		itemList.add(fifteen);

		/* creating a link */
		Attribute linkAttribute = new Attribute("LNK_CORE", "link", new DataType(String.class));

		Set<EntityEntity> entEntSet = new HashSet<>();
		
		for(BaseEntity item : itemList ){
			EntityEntity ee = new EntityEntity(grpItems, item, linkAttribute, 1.0, "ITEMS");
			entEntSet.add(ee);
		}

		/* set all the links to GRP_ITEMS_PER_PAGE */
		grpItems.setLinks(entEntSet);

		/* we publish GRP_ITEMS_PER_PAGE */
		QDataBaseEntityMessage grpItemsMsg = new QDataBaseEntityMessage(grpItems);
		sendTestMsg(grpItemsMsg);
		
		QDataBaseEntityMessage itemsMsg = new QDataBaseEntityMessage(itemList, grpItems.getCode(), "LNK_CORE");
		sendTestMsg(itemsMsg);

		/* question for previous, next, buttons and no. of items */
		Question previousQuestion = new Question("QUE_TABLE_PREVIOUS", "Previous", previousAttr, false);
		Question nextQuestion = new Question("QUE_TABLE_NEXT", "Next", nextAttr, false);
		Question tableItemsQuestion = new Question("QUE_TABLE_ITEMS", "No. Of Items", dropdownAttr, false);

		List<Question> questions = new ArrayList<>();
		questions.add(previousQuestion);
		questions.add(tableItemsQuestion);
		questions.add(nextQuestion);

		for (Question question : questions) {
			Ask footerAsk = new Ask(question, "PER_USER1", "SBE_HOSTCOMPANIES_7fa24b4b-a19a-4938-b363-a40fe9aa5b28");
			tableFooterChildAsks.add(footerAsk);
		}

		/* Convert ask list to Array */
		Ask[] tableFooterChildAsksArray = tableFooterChildAsks.toArray(new Ask[0]);

		/* set the child asks to Table Footer */
		tableFooterAsk.setChildAsks(tableFooterChildAsksArray);
		
		/* set the horizontal theme to tableFooterAsk */
		Context horizontalTheme = getHorizontalThemeForTableContent();
		
		/* setting context to footerAsk */
		List<Context> contexts = new ArrayList<>();
		contexts.add(horizontalTheme);
		ContextList footerContext = new ContextList(contexts);
		tableFooterAsk.setContextList(footerContext);
		
		Ask[] askArr = { tableFooterAsk };

		/* Creating AskMessage */
		QDataAskMessage tableFooterAskMsg = new QDataAskMessage(askArr);

		/* Send Table Footer Questions */
		sendTestMsg(tableFooterAskMsg);
			
		/* Link Table Footer Frame and Table Footer Question */
		Link link = new Link(frameTableFooterBe.getCode(), tableFooterQues.getCode(), "LNK_ASK", "NORTH");

		/* we create the entity entity */
		EntityQuestion entityQuestion = new EntityQuestion(link);
		
		/* creating entity entity between table-frame and table-footer */
		Set<EntityQuestion> entQuestionList = new HashSet<>();
		entQuestionList.add(entityQuestion);

		/* setting questions to the frame table-footer */
		frameTableFooterBe.setQuestions(entQuestionList);

		QDataBaseEntityMessage frameTableFooterMsg = new QDataBaseEntityMessage(frameTableFooterBe);
		
		/* Send Table Footer Frame with questions attached. */
		sendTestMsg(frameTableFooterMsg);

	}
	
	public void sendTableHeader(BaseEntity frameTableBe, BaseEntity frameTableHeaderBe) {

		/* Get the on-the-fly question attribute */
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));

		/* Construct a table header question: QUE_FRM_TABLE_HEADER_GRP */
		Question tableHeaderQues = new Question("QUE_TABLE_HEADER_GRP", "Table Header", questionAttribute, true);
		
		/* Construct a table header Ask */
		Ask tableHeaderAsk = new Ask(tableHeaderQues, "PER_USER1", "PER_USER1", false, 1.0, false, false, true);
		
		/* Ask List to store all the table-header and table-column child asks */
		List<Ask> tableHeaderChildAsks = new ArrayList<>();
		
		/* Get Search Results */
		BaseEntity[] searchResult = getCompaniesSearchResult();
		
		/* Get list of attributes we want to show in table header */
		if (searchResult != null) {

			BaseEntity be = searchResult[0];
			
			/* Validation for Quesion Event */
			ValidationList eventValidationList = new ValidationList();
			Attribute eventAttr = new Attribute("PRI_SORT", "PRI_SORT", new DataType("Event", eventValidationList, "Event"));
			
			/* Validation for Quesion Label */
			ValidationList labelValidationList = new ValidationList();
			Attribute labelAttr = new Attribute("PRI_LABEL", "PRI_LABEL",new DataType("QuestionName", labelValidationList, "QuestionName"));
			
			List<Attribute> attributes = new ArrayList<>();
			attributes.add(eventAttr);
			//attributes.add(labelAttr);
			
			Attribute[] attributesArray = attributes.toArray(new Attribute[0]);
			QDataAttributeMessage attrMsg = new QDataAttributeMessage(attributesArray);

			/* Send new attributes */
			sendTestMsg(attrMsg);
		
			/* get the required themes */
			Context verticalTheme = getVerticalThemeForTableContent();
			Context labelTheme = getLabelVisualControlContext();

			/* TABLE HEADER ASKS AND CHILD ASKS */
			for(EntityAttribute ea : be.getBaseEntityAttributes()) {

				/* Validation for Text */
				Validation validation = new Validation("VLD_NON_EMPTY", "EmptyandBlankValues", "(?!^$|\\s+)");
				List<Validation> validations = new ArrayList<>();
				validations.add(validation);
				ValidationList validationList = new ValidationList();
				validationList.setValidationList(validations);
				
				Attribute attr = new Attribute(ea.getAttributeCode(), ea.getAttributeName(), new DataType("Text", validationList, "Text"));				

				/* question for column label */
				String attributeCode = ea.getAttributeCode();
				String[] parts = attributeCode.split("_");
				String questionName = parts[1];
				System.out.println("questionName    ::   " + questionName);
				
				/* question for column header group */
				Question columnHeaderQuestion = new Question("QUE_" + ea.getAttributeCode() + "_GRP", questionName, questionAttribute, true);
				
				/* ask for column header group */
				Ask columnHeaderAsk = new Ask(columnHeaderQuestion, "PER_USER1", "PER_USER1");
				
				Question columnQuestion = new Question("QUE_" + ea.getAttributeCode(), questionName, labelAttr, true);
				/* creating ask for table header topic */
				Ask columnTopicAsk = new Ask(columnQuestion, "PER_USER1", "PER_USER1");
				
				List<Context> contexts = new ArrayList<>();
				contexts.add(labelTheme);
				ContextList contextList = new ContextList(contexts);
				columnTopicAsk.setContextList(contextList);

				/* question for column SORT */
				Question columnSortQuestion = new Question("QUE_SORT_" + eventAttr.getCode(), "Sort", eventAttr, true);
				columnSortQuestion.setMandatory(false);
				
				/* question for column SEARCH */
				Question columnSearchQuestion = new Question("QUE_SEARCH_" + ea.getAttributeCode(), ea.getAttributeName(), attr, true);

				List<Question> questions = new ArrayList<>();
				questions.add(columnSortQuestion);
				questions.add(columnSearchQuestion);

				List<Ask> tableColumnChildAsks = new ArrayList<>();
				tableColumnChildAsks.add(columnTopicAsk);
				
				for (Question question : questions) {
					Ask columnAsk = new Ask(question, "PER_USER1", "PER_USER1");

					tableColumnChildAsks.add(columnAsk);
				}
				
				/* Convert ask list to Array */
				Ask[] tableColumnChildAsksArray = tableColumnChildAsks.toArray(new Ask[0]);

				/* set the child asks */
				columnHeaderAsk.setChildAsks(tableColumnChildAsksArray);
				
				/* setting all theme contexts to header Ask */
				List<Context> columnHeaderContexts = new ArrayList<>();
				columnHeaderContexts.add(verticalTheme);
				columnHeaderContexts.add(labelTheme);
				ContextList columnHeaderContextList = new ContextList(columnHeaderContexts);
				columnHeaderAsk.setContextList(columnHeaderContextList);
				/* set the theme for label visual control to column-header group */
				columnHeaderAsk.getContextList().getContexts().add(labelTheme);

				tableHeaderChildAsks.add(columnHeaderAsk);

			}

			/* set tableColumnAsk as child of tableHeaderAsk */
			Ask[] asksArray = tableHeaderChildAsks.toArray(new Ask[0]);
			tableHeaderAsk.setChildAsks(asksArray);

			/* set the horizontal theme to tableHeaderAsk */
			Context horizontalTheme = getHorizontalThemeForTableContent();
		
			/* setting theme contexts for tableHeader */
			List<Context> headerAskContexts = new ArrayList<>();
			headerAskContexts.add(horizontalTheme);
			ContextList headerAskContextList = new ContextList(headerAskContexts);
			tableHeaderAsk.setContextList(headerAskContextList);
			
			Ask[] askArr = { tableHeaderAsk };

			/* Creating AskMessage */
			QDataAskMessage tableHeaderAskMsg = new QDataAskMessage(askArr);

			/* Send Table Header Questions */
			sendTestMsg(tableHeaderAskMsg);
			
		}


		/* Link Table Header and Table Header Question */
		Link link = new Link(frameTableHeaderBe.getCode(), tableHeaderQues.getCode(), "LNK_ASK", "NORTH");

		/* we create the entity entity */
		EntityQuestion entityQuestion = new EntityQuestion(link);

		/* creating entity entity between table-frame and table-content */
		Set<EntityQuestion> entQuestionList = new HashSet<>();
		entQuestionList.add(entityQuestion);

		/* setting questions to the frame table-header */
		frameTableHeaderBe.setQuestions(entQuestionList);

		QDataBaseEntityMessage frameTableHeaderMsg = new QDataBaseEntityMessage(frameTableHeaderBe);

		/* Send Table Header Frame with questions attached. */
		sendTestMsg(frameTableHeaderMsg);

	}

	public void sendTableContent(BaseEntity frameTableBe, BaseEntity frameTableContentBe) {
		/* Get the on-the-fly question attribute */
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));

		List<Ask> askList = new ArrayList<>();
		Set<EntityQuestion> entQuestionList = new HashSet<>();

		BaseEntity[] searchResult = getCompaniesSearchResult();

		if (searchResult != null) {
			
			/* get the theme contexts */
			Context evenColumnTheme = getThemeForEvenTableContent();
			Context oddColoumnTheme = getThemeForOddTableContent();
			/* getting horizontal theme */
			Context horizontalThemeContext = getHorizontalThemeForTableContent();
			/* getting border theme */
			Context borderContext = getBorderThemeForTableContent();
			
			/*create theme context list for even column */
			List<Context> contextsForEvenColumn = new ArrayList<>();
			contextsForEvenColumn.add(horizontalThemeContext);
			contextsForEvenColumn.add(borderContext);
			contextsForEvenColumn.add(evenColumnTheme);
			ContextList contextListForEvenColumn = new ContextList(contextsForEvenColumn);
			
			/*create theme context list for odd column */
			List<Context> contextsForOddColumn = new ArrayList<>();
			contextsForOddColumn.add(horizontalThemeContext);
			contextsForOddColumn.add(borderContext);
			contextsForOddColumn.add(oddColoumnTheme);
			ContextList contextListForOddColumn = new ContextList(contextsForOddColumn);
			
			for (BaseEntity be : searchResult) {

				List<Ask> childAskList = new ArrayList<>();

				/*
				 * iterating through each attribute of baseentity and creating questions for the
				 * attribute
				 */
				
				for (EntityAttribute ea : be.getBaseEntityAttributes()) {
					
					/* get text validation */
					ValidationList validationList = getTextValidation();

					Attribute at = new Attribute(ea.getAttributeCode(), ea.getAttributeName(),
							new DataType("Text", validationList, "Text"));

					Question childQuestion = new Question("QUE_" + ea.getAttributeCode(), ea.getAttributeName(), at,
							true);
					Ask childAsk = new Ask(childQuestion, "PER_USER1", be.getCode());
					childAskList.add(childAsk);
				}

				/* We generate the question the baseentity */
				Question newQuestion = new Question("QUE_" + be.getCode() + "_GRP", be.getName(), questionAttribute,
						true);
				
				/* We generate the ask */
				Ask beAsk = new Ask(newQuestion, "PER_USER1", be.getCode());

				/* setting the evenColumn theme */
				if(be.getIndex()%2==0) {
					beAsk.setContextList(contextListForEvenColumn);
				}else {
					beAsk.setContextList(contextListForOddColumn);
				}
					
				Ask[] childArr = childAskList.stream().toArray(Ask[]::new);
				beAsk.setChildAsks(childArr);

				askList.add(beAsk);

				Link newLink = new Link(frameTableContentBe.getCode(), newQuestion.getCode(), "LNK_ASK", "NORTH");
				newLink.setWeight(be.getIndex().doubleValue());

				/* we create the entity entity */
				EntityQuestion entityEntity = new EntityQuestion(newLink);

				/* creating entity entity between table-frame and table-content */
				entQuestionList.add(entityEntity);
				System.out.println("index of "+be.getCode()+" is"+be.getIndex());
				Ask[] beAskArr = { beAsk };
				
				/* Creating AskMessage with complete asks */
				QDataAskMessage totalAskMsg = new QDataAskMessage(beAskArr);
				sendTestMsg(totalAskMsg);
			}
		}

		/* publish frameBe */
		frameTableContentBe.setQuestions(entQuestionList);

		BaseEntity verticalTheme = new BaseEntity("THM_DISPLAY_VERTICAL", "vertical");

		/* creating a link */
		Attribute attribute = new Attribute("LNK_THEME", "link", new DataType(String.class));
		/* creating entity entity between table-frame and table-content */
		EntityEntity entityEntity = new EntityEntity(frameTableContentBe, verticalTheme, attribute, 1.0, "NORTH");
		Set<EntityEntity> entEntSet = new HashSet<>();
		entEntSet.add(entityEntity);
		frameTableContentBe.setLinks(entEntSet);

		QDataBaseEntityMessage frameMsg = new QDataBaseEntityMessage(frameTableContentBe);
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(searchResult);
		sendTestMsg(msg);
		sendTestMsg(frameMsg);
	}

	private Context getHorizontalThemeForTableContent() {
		/* create context */
		/* getting the expandable theme baseentity */
		BaseEntity horizontalTheme = new BaseEntity("THM_DISPLAY_HORIZONTAL", "horizontal");

		/* publishing theme for expanding */
		/* creating a context for the expandable-theme */
		Context horizontalThemeContext = new Context(ContextType.THEME, horizontalTheme);

		return horizontalThemeContext;
	}

	private Context getVerticalThemeForTableContent() {
		/* create context */
        /* getting the vertical theme baseentity */
		BaseEntity verticalTheme = new BaseEntity("THM_DISPLAY_VERTICAL", "vertical");
		
		 /* publishing theme for vertical display */
		/* creating a context for the vertical-display */
		Context verticalThemeContext = new Context(ContextType.THEME, verticalTheme);

		return verticalThemeContext;
	}
	
	private Context getLabelVisualControlContext() {
		/* create visual baseentity for question with label */
		BaseEntity visualBaseEntity = new BaseEntity("THM_VISUAL_CONTROL_LABEL", "Theme Visual Control For Label");
		
		Attribute labelAttr = new Attribute("PRI_HAS_LABEL", "Has Label?", new DataType(Boolean.class));
		EntityAttribute labelEntityAttribute = new EntityAttribute(visualBaseEntity, labelAttr, 1.0, "TRUE");
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(labelEntityAttribute);	
		visualBaseEntity.setBaseEntityAttributes(attributeSet);
		
		QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(visualBaseEntity);
		/* send visual baseentity */
		sendTestMsg(beMsg);
		
		Context visualContext = new Context(ContextType.THEME, visualBaseEntity, VisualControlType.LABEL);
		return visualContext;
	}
	
	private Context getBorderThemeForTableContent() {
		/* create context */
        /* getting the expandable theme baseentity */
		BaseEntity borderTheme = new BaseEntity("THM_TABLE_BORDER", "table border");
		
		String borderAttribute = "{  \"borderStyle\": \"solid\", \"borderColour\" : \"#dee2e6\", \"borderWidth\" : 0.5 }";
		
		Attribute att = new Attribute("PRI_CONTENT", "content", new DataType(String.class));
		Attribute inheritableAtt = new Attribute("PRI_IS_INHERITABLE", "inheritable", new DataType(Boolean.class));
		EntityAttribute entAttr = new EntityAttribute(borderTheme, att, 1.0, borderAttribute);
		EntityAttribute inheritEntAtt = new EntityAttribute(borderTheme, inheritableAtt, 1.0, "FALSE");
		Set<EntityAttribute> entAttrSet = new HashSet<>();
		entAttrSet.add(entAttr);
		entAttrSet.add(inheritEntAtt);
		
		borderTheme.setBaseEntityAttributes(entAttrSet);
		
		QDataBaseEntityMessage borderThemeMsg = new QDataBaseEntityMessage(borderTheme);
		sendTestMsg(borderThemeMsg);
		
		 /* publishing theme for expanding */
		/* creating a context for the expandable-theme */
		Context borderThemeContext = new Context(ContextType.THEME, borderTheme);

		return borderThemeContext;
	}
	
	private Context getThemeForEvenTableContent() {
		/* create context */
		BaseEntity backgroundTheme = new BaseEntity("THM_TABLE_EVEN", "table background");
		
		String bgAttribute = "{  \"backgroundColor\": \"#F2F2F2\", \"color\": \"#212529\" , \"padding\" : \"5px\", \"boxSizing\": \"borderBox\"}";
		
		Attribute att = new Attribute("PRI_CONTENT", "content", new DataType(String.class));
		EntityAttribute entAttr = new EntityAttribute(backgroundTheme, att, 1.0, bgAttribute);
		Set<EntityAttribute> entAttrSet = new HashSet<>();
		entAttrSet.add(entAttr);
		
		backgroundTheme.setBaseEntityAttributes(entAttrSet);
		QDataBaseEntityMessage bgMessage = new QDataBaseEntityMessage(backgroundTheme);
		sendTestMsg(bgMessage);
	
		Context bgThemeContext = new Context(ContextType.THEME, backgroundTheme);

		return bgThemeContext;
	}
	
	private Context getThemeForOddTableContent() {
		/* create context */
		BaseEntity backgroundTheme = new BaseEntity("THM_TABLE_ODD", "table background");
		
		String bgAttribute = "{  \"backgroundColor\": \"#FFFFFF\", \"color\": \"#212529\" , \"padding\" : \"5px\", \"boxSizing\": \"borderBox\"}";
		
		Attribute att = new Attribute("PRI_CONTENT", "content", new DataType(String.class));
		EntityAttribute entAttr = new EntityAttribute(backgroundTheme, att, 1.0, bgAttribute);
		Set<EntityAttribute> entAttrSet = new HashSet<>();
		entAttrSet.add(entAttr);
		
		backgroundTheme.setBaseEntityAttributes(entAttrSet);
		QDataBaseEntityMessage bgMessage = new QDataBaseEntityMessage(backgroundTheme);
		sendTestMsg(bgMessage);

		Context bgThemeContext = new Context(ContextType.THEME, backgroundTheme);

		return bgThemeContext;
	}
	
	private ValidationList getTextValidation() {
		Validation validation = new Validation("VLD_NON_EMPTY", "EmptyandBlankValues", "(?!^$|\\s+)");
		List<Validation> validations = new ArrayList<>();
		validations.add(validation);
		ValidationList validationList = new ValidationList();
		validationList.setValidationList(validations);
		return validationList;
	}

	private void linkTableToContentFrame(BaseEntity tableFrameBe, BaseEntity frameContentBe) {

		Attribute attribute = new Attribute("LNK_FRAME", "frame", new DataType(String.class));

		EntityEntity entityEntity = new EntityEntity(frameContentBe, tableFrameBe, attribute, 1.0, "CENTRE");
		Set<EntityEntity> entEntList = new HashSet<>();
		entEntList.add(entityEntity);

		frameContentBe.setLinks(entEntList);

		QDataBaseEntityMessage contentFrameMsg = new QDataBaseEntityMessage(frameContentBe);
		sendTestMsg(contentFrameMsg);
	}

	/* publishes the test-messages to front-end through bridge */
	private <T extends QMessage> void sendTestMsg(T msg) {

		try {
			String token = KeycloakUtils.getAccessToken("http://keycloak.genny.life", "genny", "genny",
					"056b73c1-7078-411d-80ec-87d41c55c3b4", "user1", "password1");
			msg.setToken(token);

			log.info("cmd message ::" + msg);

			/* get the bridge url to publish the message to webcmd channel */
			String bridgetUrl = ENV_GENNY_BRIDGE_URL + "/api/service?channel=webdata";

			QwandaUtils.apiPostEntity(bridgetUrl, JsonUtils.toJson(msg), token);

		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}

	private BaseEntity[] getCompaniesSearchResult() {
		BaseEntity[] beArr = null;
		String resultJson;
		try {

			String serviceToken = KeycloakUtils.getAccessToken("http://keycloak.genny.life:8180", "genny", "genny",
					"056b73c1-7078-411d-80ec-87d41c55c3b4", "service", "Wubba!Lubba!Dub!Dub!");

			SearchEntity hostCompanies = new SearchEntity("SBE_HOSTCOMPANIES_7fa24b4b-a19a-4938-b363-a40fe9aa5b28", "List of All Host Companies")
			        .addColumn("PRI_NAME", "Name")
			        .addColumn("PRI_EMAIL", "Company email")
			        .addColumn("PRI_LANDLINE", "Phone Number")
			        .addColumn("PRI_ADDRESS_FULL", "Address")
			        .addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "CPY_%")
			        .addFilter("PRI_IS_HOST_COMPANY", true)
			        .setPageStart(0)
			        .setPageSize(11);
			
			String jsonSearchBE = JsonUtils.toJson(hostCompanies);
			resultJson = QwandaUtils.apiPostEntity("http://keycloak.genny.life:8280/qwanda/baseentitys/search",
					jsonSearchBE, serviceToken);

			log.info("search result ::" + resultJson);
			if (resultJson != null) {
				QDataBaseEntityMessage msg = JsonUtils.fromJson(resultJson, QDataBaseEntityMessage.class);
				if (msg != null) {
					return msg.getItems();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return beArr;
	}

}