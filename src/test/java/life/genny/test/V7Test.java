package life.genny.test;

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
import life.genny.qwanda.ContextList;
import life.genny.qwanda.Link;
import life.genny.qwanda.Question;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.EntityQuestion;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QMessage;
import life.genny.qwanda.message.QSearchEntityMessage;
import life.genny.qwanda.validation.Validation;
import life.genny.qwanda.validation.ValidationList;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;

public class V7Test {
	
	private static String ENV_GENNY_BRIDGE_URL= "http://bridge.genny.life";

	
	  private static final Logger log = org.apache.logging.log4j.LogManager
		      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Test
	public void sendInitialFrame() {
		
		// Perform test only if this env exists
		String keycloaktestUrl = System.getenv("ENV_TEST_KEYCLOAK_URL");
		if (keycloaktestUrl == null) {
			log.info("Will not run local V7 test until ENV_TEST_KEYCLOAK_URL set to say .. http://keycloak.genny.life:8180");
			return ;
		}
		
		/* create table frame */
		BaseEntity frameTableBe = new BaseEntity("FRM_TABLE", "table-frame");
		
		/* create table-header frame */
		BaseEntity frameTableHeaderBe = new BaseEntity("FRM_TABLE_HEADER", "table-header");
			
		/* create table-content frame */
		BaseEntity frameTableContentBe = new BaseEntity("FRM_TABLE_CONTENT", "table-content");					
		
		/* create table-footer frame */
		BaseEntity frameTableFooterBe = new BaseEntity("FRM_TABLE_FOOTER", "table-footer");					
		
		/* creating a link */
		Attribute attribute = new Attribute("LNK_FRAME", "link", new DataType(String.class));

		/* link table-frame and table-header */
		EntityEntity headerEntityEntity = new EntityEntity(frameTableBe, frameTableHeaderBe, attribute, 1.0, "NORTH");

		/* link table-frame and table-content */
		EntityEntity ContentEntityEntity = new EntityEntity(frameTableBe, frameTableContentBe, attribute, 2.0, "CENTRE");
		
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
		
		/* link content-frame to table-frame */
		linkTableToContentFrame();
		
	}
	
	public void sendTableHeader(BaseEntity frameTableBe, BaseEntity frameTableHeaderBe) {
		
		/* Get the on-the-fly question attribute */
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));

		/* Construct a table header question: QUE_FRM_TABLE_HEADER_GRP */
		Question tableHeaderQues = new Question("QUE_" + frameTableHeaderBe.getCode() + "_GRP", frameTableHeaderBe.getName(), questionAttribute, true);
		
		/* Construct a table header Ask */
		Ask tableHeaderAsk = new Ask(tableHeaderQues, "PER_USER1", "PER_USER1", false, 1.0, false, false, true);
		
		/* initialize Ask List to store all the child asks */
		List<Ask> childAsks = new ArrayList<>();

		/* Get Search Results */
		BaseEntity[] searchResult = getCompaniesSearchResult();
		
		/* Get list of attributes we want to show in table header */
		if(searchResult != null) {
			
			BaseEntity be = searchResult[0];
			for(EntityAttribute ea : be.getBaseEntityAttributes()) {
				
				/* Construct a table column question */
				Question columnQuestion = new Question("QUE_" + ea.getAttributeCode(), ea.getAttributeName(), questionAttribute, true);
				
				/* Construct a table column Ask */
				Ask columnAsk = new Ask(columnQuestion, "PER_USER1", "PER_USER1", false, 1.0, false,
				false, true);
				
				childAsks.add(columnAsk);
			}
		}

		/* Convert childAsks List to Array  */
		Ask[] childAsksArray = childAsks.toArray(new Ask[0]);

		/* Set the childAsks to tableHeaderAsk */
		tableHeaderAsk.setChildAsks(childAsksArray);
		
		/* get the theme */
		ContextList themeContext = createHorizontalThemeForTableContent();
        tableHeaderAsk.setContextList(themeContext);

		/* Create a list of Asks */
		List<Ask> asks = new ArrayList<>();
		asks.add(tableHeaderAsk);
		
		Ask[] asksArray = asks.toArray(new Ask[0]);

		/* Creating AskMessage */
		QDataAskMessage tableHeaderAskMsg = new QDataAskMessage(asksArray);
		
		/* Send Table Header Questions */
		sendTestMsg(tableHeaderAskMsg);

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
		
		Validation validation = new Validation("VLD_NON_EMPTY", "EmptyandBlankValues", "(?!^$|\\s+)");
		List<Validation> validations = new ArrayList<>();
		validations.add(validation);
		ValidationList validationList = new ValidationList();
		validationList.setValidationList(validations);

		List<Ask> askList = new ArrayList<>();
		Set<EntityQuestion> entQuestionList = new HashSet<>();
		QBulkMessage bulkMsg = new QBulkMessage();
		
		BaseEntity[] searchResult = getCompaniesSearchResult();
		
		if(searchResult != null) {
			
			for(BaseEntity be : searchResult) {
				
				List<Ask> childAskList = new ArrayList<>();
				
				/* iterating through each attribute of baseentity and creating questions for the attribute */
				for(EntityAttribute ea : be.getBaseEntityAttributes()) {
					
					Attribute at = new Attribute(ea.getAttributeCode(), ea.getAttributeName(), new DataType("Text", validationList, "Text"));
					
					Question childQuestion = new Question("QUE_"+ea.getAttributeCode(), ea.getAttributeName(), at, true);
					Ask childAsk = new Ask(childQuestion, "PER_USER1", be.getCode());
					childAskList.add(childAsk);
				}
				
				/* We generate the question the baseentity*/
		        Question newQuestion = new Question("QUE_" + be.getCode() + "_GRP", be.getName(), questionAttribute, true);
		        
		        /* getting horizontal theme */
		        ContextList themeContext = createHorizontalThemeForTableContent();
		        
		        /* We generate the ask */
		        Ask beAsk = new Ask(newQuestion, "PER_USER1", be.getCode());
		        
		        /* adding horizontal theme to each table-row question-grp */
		        beAsk.setContextList(themeContext);
		        Ask[] childArr = childAskList.stream().toArray(Ask[]::new);
		        beAsk.setChildAsks(childArr);
		        		        
		        askList.add(beAsk);    
		        
		        Link newLink = new Link(frameTableContentBe.getCode(), newQuestion.getCode(), "LNK_ASK", "NORTH");

				/* we create the entity entity */
				EntityQuestion entityEntity = new EntityQuestion(newLink);
		        
				/* creating entity entity between table-frame and table-content */
				entQuestionList.add(entityEntity);
				
				
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
	
	private ContextList createHorizontalThemeForTableContent() {
		/* create context */
        /* getting the expandable theme baseentity */
		BaseEntity horizontalTheme = new BaseEntity("THM_DISPLAY_HORIZONTAL", "horizontal");
		
		 /* publishing theme for expanding */
		/* creating a context for the expandable-theme */
		Context horizontalThemeContext = new Context("THEME", horizontalTheme);
		List<Context> horizontalThemeContextList = new ArrayList<>();
		horizontalThemeContextList.add(horizontalThemeContext);
		
		/* add the context to the contextList */
		ContextList contextList = new ContextList(horizontalThemeContextList);
		
		return contextList;
	}

	private void linkTableToContentFrame() {
		BaseEntity frameBe = new BaseEntity("FRM_TABLE", "table-frame");
		
		BaseEntity contentBe = new BaseEntity("FRM_CONTENT", "content-frame");
		
		Attribute attribute = new Attribute("LNK_FRAME", "frame", new DataType(String.class));
		
		EntityEntity entityEntity = new EntityEntity(contentBe, frameBe, attribute, 1.0, "CENTRE");
		Set<EntityEntity> entEntList = new HashSet<>();
		entEntList.add(entityEntity);
		
		contentBe.setLinks(entEntList);
		
		QDataBaseEntityMessage contentFrameMsg = new QDataBaseEntityMessage(contentBe);
		sendTestMsg(contentFrameMsg);	 
	}
	
	/* publishes the test-messages to front-end through bridge */
	private <T extends QMessage> void sendTestMsg(T msg) {
		
		try {
			String token = KeycloakUtils.getAccessToken("http://keycloak.genny.life", "genny", "genny", "056b73c1-7078-411d-80ec-87d41c55c3b4", "user1", "password1");
			msg.setToken(token);

			log.info("cmd message ::"+msg);
			
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
			
			String serviceToken = KeycloakUtils.getAccessToken("http://keycloak.genny.life:8180", "genny", "genny", "056b73c1-7078-411d-80ec-87d41c55c3b4", "service", "Wubba!Lubba!Dub!Dub!");
			
			log.info("service token ::"+serviceToken);
			
//			SearchEntity hostCompanies = new SearchEntity("SBE_DAB_DAB", "List of All Host Companies")
//			        .addColumn("PRI_NAME", "Name")
//			        .addColumn("PRI_EMAIL", "Company email")
//			        .addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "CPY_%")
//			        .addFilter("PRI_IS_HOST_COMPANY", true)
//			        .setPageStart(0)
//			        .setPageSize(11);

			
			QSearchEntityMessage hostCompanies = new QSearchEntityMessage.Builder("SBE_DAB_DAB", "List of All Host Companies")
					   .pageStart(0)
					   .pageSize(10)
					   .sort("PRI_CREATED","Created",SearchEntity.Sort.DESC)
				        .column("PRI_NAME", "Name")
				        .column("PRI_EMAIL", "Company email")
						   .searchOr(new SearchEntity("SBE_TEST1","Test1")
								   .addFilter("PRI_CODE",SearchEntity.StringFilter.LIKE,"CPY_%"))
						   .searchOr(new SearchEntity("SBE_TEST2","Test2")
								   .addFilter("PRI_IS_HOST_COMPANY",true))
					   .build();

			
			String jsonSearchBE = JsonUtils.toJson(hostCompanies);
			resultJson = QwandaUtils.apiPostEntity("http://keycloak.genny.life:8280/qwanda/baseentitys/search2",
				jsonSearchBE, serviceToken);
				
					
			log.info("search result ::"+resultJson);
			if(resultJson != null) {
				QDataBaseEntityMessage msg = JsonUtils.fromJson(resultJson, QDataBaseEntityMessage.class);
				if(msg != null) {
					return msg.getItems();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return beArr;
	}
	
}
