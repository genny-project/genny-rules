package life.genny.utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;
import life.genny.models.GennyToken;
import life.genny.qwanda.Link;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.entity.SearchEntity.Filter;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;

public class DropdownUtils {
	

	private SearchEntity searchEntity;
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	
	public SearchEntity setNewSearch(String code, String name) {
		searchEntity = new SearchEntity(code,name);
		return this.searchEntity;
	}
	
	public void sendSearchResults( String parentCode, String linkCode, String linkValue,
			GennyToken gennyToken) throws IOException {
		
		this.sendSearchResults( parentCode, linkCode, linkValue, false,  false, gennyToken);
	}
	
	public void sendSearchResults( String parentCode, String linkCode, String linkValue,
			Boolean replace, Object shouldDeleteLinkedBaseEntities, GennyToken gennyToken) throws IOException {
		
		QDataBaseEntityMessage beMessage = getSearchResults(this.searchEntity,parentCode, linkCode, linkValue,replace,
															shouldDeleteLinkedBaseEntities,gennyToken);
		if(beMessage == null) {
			
			log.error("Warning: no results from search " + this.searchEntity.getCode());
			
		}else {
			
			/* Writing to Vert.x EventBus*/
			writeToVertx("webcmds", beMessage);
		}
	}
	
	/*
	 * Get Search results. Returns  QDataBaseEntityMessage
	 */
	private QDataBaseEntityMessage getSearchResults(SearchEntity searchBE, String parentCode, String linkCode, String linkValue,
			Boolean replace, Object shouldDeleteLinkedBaseEntities, GennyToken gennyToken) throws IOException {
		
		String token = gennyToken.getToken();
		String jsonSearchBE = JsonUtils.toJson(searchBE);
		String resultJson = QwandaUtils.apiPostEntity(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/search",
				jsonSearchBE, token);

		QDataBaseEntityMessage msg = JsonUtils.fromJson(resultJson, QDataBaseEntityMessage.class);

		if (msg != null) {
			
			msg.setParentCode(parentCode);
			msg.setToken(token);
			msg.setLinkCode(linkCode);
			msg.setLinkValue(linkValue);
			msg.setReplace(replace);
			msg.setShouldDeleteLinkedBaseEntities(shouldDeleteLinkedBaseEntities);

			/* Linking child baseEntity to the parent baseEntity*/
			QDataBaseEntityMessage beMessage = setDynamicLinksToParentBe(msg, parentCode, linkCode, linkValue,gennyToken);
			return beMessage;
			
		} else {
			return null;
		}
	}
	
	/*
	 * Setting dynamic links between parents and child. ie. linking DropDown items to the DropDown field.
	 */
	private QDataBaseEntityMessage setDynamicLinksToParentBe(QDataBaseEntityMessage beMsg, String parentCode, String linkCode,
			String linkValue, GennyToken gennyToken) {
		
		BaseEntity parentBe = new BaseEntityUtils(gennyToken).getBaseEntityByCode("PER_USER1");
		Set<EntityEntity> childLinks = new HashSet<>();
		double index = 0.0;

		/* creating a dumb attribute for linking the search results to the parent */
		Attribute attributeLink = new Attribute(linkCode, linkCode, new DataType(String.class));

		for (BaseEntity be : beMsg.getItems()) {
			
			EntityEntity ee = new EntityEntity(parentBe, be, attributeLink, index);

			/* creating link for child */
			Link link = new Link(parentCode, be.getCode(), attributeLink.getCode(), linkValue, index);

			/* adding link */
			ee.setLink(link);

			/* adding child link to set of links */
			childLinks.add(ee);

			index++;
		}

		parentBe.setLinks(childLinks);
		beMsg.add(parentBe);
		return beMsg;
	}
	
	private void writeToVertx(String channel, QDataBaseEntityMessage msg){
		
		/* ATM sending form here might have to update later*/
		VertxUtils.writeMsg(channel,JsonUtils.toJson(msg));
	}
	
}
