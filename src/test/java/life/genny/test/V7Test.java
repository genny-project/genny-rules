package life.genny.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import life.genny.qwanda.Ask;
import life.genny.qwanda.Link;
import life.genny.qwanda.Question;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.EntityQuestion;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;

public class V7Test {
	
	private static String ENV_GENNY_BRIDGE_URL= "http://bridge.genny.life";
	
	//@Test
	public void sendInitialFrame() {
		
		/* create table frame */
		BaseEntity frameBe = new BaseEntity("FRM_TABLE", "table-frame");
			
		/* create table-content frame */
		BaseEntity contentFrameBe = new BaseEntity("FRM_TABLE_CONTENT", "table-content");					
		
		/* creating a link */
		Attribute attribute = new Attribute("LNK_FRAME", "link", new DataType(String.class));
		/* creating entity entity between table-frame and table-content */
		EntityEntity entityEntity = new EntityEntity(frameBe, contentFrameBe, attribute, 1.0, "CENTRE");
		Set<EntityEntity> entEntSet = new HashSet<>();
		entEntSet.add(entityEntity);
		frameBe.setLinks(entEntSet);
		
		System.out.println("frameBe message ::"+JsonUtils.toJson(frameBe));
		
		QDataBaseEntityMessage contentFrameMsg = new QDataBaseEntityMessage(contentFrameBe);
		sendTestMsg(contentFrameMsg);
		
		QDataBaseEntityMessage frameMsg = new QDataBaseEntityMessage(frameBe);
		sendTestMsg(frameMsg);
		
        /* Get the on-the-fly question attribute */
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));
        
        /* creating suffix according to value of isQuestionGroup. If it is a question-group, suffix "_GRP" is required" */  
        String questionCode = "QUE_" + frameBe.getCode() + "_GRP";
        
        /* We generate the question */
        Question newQuestion = new Question(questionCode, frameBe.getName(), questionAttribute, true);
        
        /* We generate the ask */
        Ask newAsk = new Ask(newQuestion, frameBe.getCode(), frameBe.getCode(), false, 1.0, false, false, true);
        
        Ask[] completeAsk = { newAsk };
        
        /* Creating AskMessage with complete asks */
		QDataAskMessage askMsg = new QDataAskMessage(completeAsk);
		
		/* send through message */
		sendTestMsg(askMsg);
		
		/* send table content */
		sendTableContent();
		
		/* link content-frame to table-frame */
		linkTableToContentFrame();
		
	}
	
	public void sendTableContent() {
		
		/* Get the on-the-fly question attribute */
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));	
		
		/* create table-content frame */
		BaseEntity contentFrameBe = new BaseEntity("FRM_TABLE_CONTENT", "table-content");		
		
		 /* We generate the question */
        Question contentFrameQuestion = new Question("QUE_" + contentFrameBe.getCode() + "_GRP", contentFrameBe.getName(), questionAttribute, true);
        
        /* We generate the ask */
        Ask contentFrameAsk = new Ask(contentFrameQuestion, "FRM_TABLE", contentFrameQuestion.getCode(), false, 1.0, false, false, true);
		 		
		BaseEntity be1 = new BaseEntity("TABLE_ROW1", "row1");
		BaseEntity be2 = new BaseEntity("TABLE_ROW2", "row2");
		List<BaseEntity> beList = new ArrayList<>();
		beList.add(be1);
		beList.add(be2);
		List<Ask> askList = new ArrayList<>();
		Set<EntityQuestion> entQuestionList = new HashSet<>();
				
		for(BaseEntity be : beList) {
			 /* We generate the question */
	        Question newQuestion = new Question("QUE_" + be.getCode() + "_GRP", be.getName(), questionAttribute, true);
	        
	        /* We generate the ask */
	        //Ask newAsk = new Ask(newQuestion, contentFrameBe.getCode(), newQuestion.getCode(), false, 1.0, false, false, true);
	        Ask newAsk = new Ask(newQuestion, contentFrameBe.getCode(), newQuestion.getCode());
	        askList.add(newAsk);    
	        
	        Link newLink = new Link(contentFrameBe.getCode(), newQuestion.getCode(), "LNK_ASK", "NORTH");

			/* we create the entity entity */
			EntityQuestion entityEntity = new EntityQuestion(newLink);
	        
			/* creating entity entity between table-frame and table-content */
			entQuestionList.add(entityEntity);
		}
		
		Ask[] childAskArr = askList.stream().toArray(Ask[]::new);
		contentFrameAsk.setChildAsks(childAskArr);
		
		askList.add(contentFrameAsk);
		Ask[] tableContentAsk = askList.stream().toArray(Ask[]::new);
        
        /* Creating AskMessage with complete asks */
		QDataAskMessage tableContentMsg = new QDataAskMessage(tableContentAsk);
		sendTestMsg(tableContentMsg);
		
		/* sending content-frame with links */
		contentFrameBe.setQuestions(entQuestionList);
		QDataBaseEntityMessage frameMsg = new QDataBaseEntityMessage(contentFrameBe);
		sendTestMsg(frameMsg);			
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

			System.out.println("cmd message ::"+msg);
			
			/* get the bridge url to publish the message to webcmd channel */
			String bridgetUrl = ENV_GENNY_BRIDGE_URL + "/api/service?channel=webdata";
			
			String response = QwandaUtils.apiPostEntity(bridgetUrl, JsonUtils.toJson(msg), token);
			
			System.out.println("test bridge response ::"+response);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
	}
	
	private void search() {		
		
		String resultJson;
		try {
			
			String serviceToken = KeycloakUtils.getAccessToken("http://keycloak.genny.life:8180", "genny", "genny", "056b73c1-7078-411d-80ec-87d41c55c3b4", "service", "Wubba!Lubba!Dub!Dub!");
			
			System.out.println("service token ::"+serviceToken);
			
			SearchEntity hostCompanies = new SearchEntity("SBE_DAB_DAB", "List of All Host Companies")
			        .addColumn("PRI_NAME", "Name")
			        .addColumn("PRI_IMAGE_URL", "Company Logo")
			        .addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "CPY_%")
			        .addFilter("PRI_IS_HOST_COMPANY", true)
			        .setPageStart(0)
			        .setPageSize(10000);
			
			String jsonSearchBE = JsonUtils.toJson(hostCompanies);
			resultJson = QwandaUtils.apiPostEntity("http://keycloak.genny.life:8280/qwanda/baseentitys/search",
				jsonSearchBE, serviceToken);
					
			
					
			System.out.println("search result ::"+resultJson);
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}
	
}
