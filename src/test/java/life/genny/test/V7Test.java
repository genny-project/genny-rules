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

//	@Test
	public void sendInitialFrame() {
		
		/* create table frame */
		BaseEntity frameTableBe = new BaseEntity("FRM_TABLE", "table-frame");
			
		/* create table-content frame */
		BaseEntity contentFrameBe = new BaseEntity("FRM_TABLE_CONTENT", "table-content");					
		
		/* creating a link */
		Attribute attribute = new Attribute("LNK_FRAME", "link", new DataType(String.class));
		/* creating entity entity between table-frame and table-content */
		EntityEntity entityEntity = new EntityEntity(frameTableBe, contentFrameBe, attribute, 1.0, "CENTRE");
		Set<EntityEntity> entEntSet = new HashSet<>();
		entEntSet.add(entityEntity);
		frameTableBe.setLinks(entEntSet);
				
		QDataBaseEntityMessage frameMsg = new QDataBaseEntityMessage(frameTableBe);
		sendTestMsg(frameMsg);
		
		/* send table content */
		sendTableContent(frameTableBe, contentFrameBe);
		
		/* link content-frame to table-frame */
		linkTableToContentFrame();
		
	}
	
	public void sendTableContent(BaseEntity frameTableBe, BaseEntity contentFrameBe) {
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
				
				for(EntityAttribute ea : be.getBaseEntityAttributes()) {
					
					Attribute at = new Attribute(ea.getAttributeCode(), ea.getAttributeName(), new DataType("Text", validationList, "Text"));
					
					Question childQuestion = new Question("QUE_"+ea.getAttributeCode(), ea.getAttributeName(), at, true);
					Ask childAsk = new Ask(childQuestion, "PER_USER1", be.getCode());
					childAskList.add(childAsk);
				}
				
				/* We generate the question */
		        Question newQuestion = new Question("QUE_" + be.getCode() + "_GRP", be.getName(), questionAttribute, true);
		        
		        ContextList themeContext = createHorizontalThemeForTableContent();
		        //newQuestion.setContextList(themeContext);
		        
		        /* We generate the ask */
		        Ask beAsk = new Ask(newQuestion, "PER_USER1", be.getCode());
		        beAsk.setContextList(themeContext);
		        Ask[] childArr = childAskList.stream().toArray(Ask[]::new);
		        beAsk.setChildAsks(childArr);
		        		        
		        askList.add(beAsk);    
		        
		        Link newLink = new Link(contentFrameBe.getCode(), newQuestion.getCode(), "LNK_ASK", "NORTH");

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
		contentFrameBe.setQuestions(entQuestionList);
		
		BaseEntity verticalTheme = new BaseEntity("THM_DISPLAY_VERTICAL", "vertical");
		
		/* creating a link */
		Attribute attribute = new Attribute("LNK_THEME", "link", new DataType(String.class));
		/* creating entity entity between table-frame and table-content */
		EntityEntity entityEntity = new EntityEntity(contentFrameBe, verticalTheme, attribute, 1.0, "NORTH");
		Set<EntityEntity> entEntSet = new HashSet<>();
		entEntSet.add(entityEntity);
		contentFrameBe.setLinks(entEntSet);
		
		QDataBaseEntityMessage frameMsg = new QDataBaseEntityMessage(contentFrameBe);
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
