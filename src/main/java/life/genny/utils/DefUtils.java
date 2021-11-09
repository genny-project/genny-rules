package life.genny.utils;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeText;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QDataAttributeMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwandautils.ANSIColour;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.MergeUtil;

public class DefUtils {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


	/**
	 * @param realm
	 */
	public static void loadDEFS(String realm) {
		log.info("Loading in DEFS for realm "+realm);
		
		SearchEntity searchBE = new SearchEntity("SBE_DEF", "DEF test")
				.addSort("PRI_NAME", "Created", SearchEntity.Sort.ASC)
				.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "DEF_%")
				
				.addColumn("PRI_NAME", "Name");

		searchBE.setRealm(realm);
		searchBE.setPageStart(0);
		searchBE.setPageSize(10000);

		JsonObject tokenObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM, "TOKEN" + realm.toUpperCase());
		String sToken = tokenObj.getString("value");
		GennyToken serviceToken = new GennyToken("PER_SERVICE", sToken);

		if ((serviceToken == null) || ("DUMMY".equalsIgnoreCase(serviceToken.getToken()))) {
			log.error("NO SERVICE TOKEN FOR " + realm + " IN CACHE");
			return;
		}

		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken,serviceToken);

		// build map for realm during rule start up
		//if(!RulesUtils.realmAttributeMap.containsKey(serviceToken.getRealm())) {
			RulesUtils.loadAllAttributesIntoCache(serviceToken);
		//}

		List<BaseEntity> items = Collections.synchronizedList(beUtils.getBaseEntitys(searchBE));
			log.info("Loaded "+items.size()+" DEF baseentitys");

			Map<String,BaseEntity> newDefs = new ConcurrentHashMap<String,BaseEntity>()	;
			Set<Attribute> newDefAttributes = new HashSet<>()	;
		//RulesUtils.defs.put(realm,new ConcurrentHashMap<String,BaseEntity>());	
			
		for (BaseEntity item : items) {
			
			// Now go through all the searches and see what the total is of the searches.
			// if less than or equal to dropdown size then generate the dropdown items message and save into an attribute called "DDI_<attributeCode>"
			
			Iterator<EntityAttribute> eaIterator = item.getBaseEntityAttributes().iterator();
			Set<EntityAttribute> newEas = new HashSet<>();
			
			while (eaIterator.hasNext()) {
				EntityAttribute defEa = eaIterator.next();
		//	for (EntityAttribute defEa : item.getBaseEntityAttributes()) {
				if (defEa.getAttributeCode().startsWith("SER_")) {
					log.info("DEF Processing "+defEa.getAttributeCode());
					// This is a dropdown attribute!
					// Now if this search has a parental dependency then we cannot do anything so we skip
					String searchValue = defEa.getValueString();
					JsonObject json = null;
					try {
						json = new JsonObject(searchValue);
					} catch (Exception e1) {
						log.error("Bad Search DEF json found, baseentityCode: " + defEa.getBaseEntityCode()
						+ ", attributeCode:" + defEa.getAttributeCode() + ", valueString:" + searchValue);
						continue;
					}
					Boolean cached = false;
					if (json.containsKey("cached")) {
						cached = json.getBoolean("cached");
					} else {
						cached = true;
					}
					
					if (StringUtils.isBlank(searchValue) || (searchValue.contains("[[")) || !cached ) { // could be faster with finding firt index
						continue;
					}
					// If the json of the search has "cache" set to true then immediately create a cached version
					log.info("Adding Cached Dropdown Data for "+defEa.getAttributeCode());
					QDataBaseEntityMessage cachedMessage =  DefUtils.getDropdownDataMessage(beUtils, defEa.getAttributeCode().substring("SER_".length()), "@@PARENTCODE@@", "@@QUESTIONCODE@@",searchValue, null, null, "", "@@TOKEN@@");
					Attribute cacheAttribute = new AttributeText("DDC_"+defEa.getAttributeCode().substring("SER_".length()),"DDC_"+defEa.getAttributeCode().substring("SER_".length()));
					
					EntityAttribute newDdcEa = new EntityAttribute(item, cacheAttribute, 1.0, JsonUtils.toJson(cachedMessage));
					newEas.add(newDdcEa);
					
				} else if (defEa.getAttributeCode().startsWith("ATT_")) {
					String normalAttributeCode = defEa.getAttributeCode().substring("ATT_".length());
					Attribute normalAttribute = RulesUtils.realmAttributeMap.get(realm).get(normalAttributeCode);
					newDefAttributes.add(normalAttribute);
				}
			}
			
			item.getBaseEntityAttributes().addAll(newEas);
			item.setFastAttributes(true); // make fast
	
			newDefs.put(item.getCode(),item);

			log.info("Saving ("+realm+") DEF "+item.getCode());
		}
		
		// Now switch in the defs
			RulesUtils.defs.put(realm, newDefs);
			
		// Now switch in the QDataAttributesMessage
			 QDataAttributeMessage defAttributesMsg = new QDataAttributeMessage(newDefAttributes.toArray(new Attribute[0]));
			 RulesUtils.defAttributesMap.put(realm, defAttributesMsg);
		
		log.info("Saved "+items.size()+" yummy DEFs!");
	}
	
	
	
	static public QDataBaseEntityMessage getDropdownDataMessage(BaseEntityUtils beUtils, final String dropdownCode, final String parentCode, final String questionCode,final String serValue, final BaseEntity sourceBe, final BaseEntity targetBe, final String searchText, final String token)
	{
		JsonObject searchValueJson =new JsonObject(serValue);
		Integer pageStart = 0;
		Integer pageSize = searchValueJson.containsKey("dropdownSize")?searchValueJson.getInteger("dropdownSize"):GennySettings.defaultDropDownPageSize;
		Boolean searchingOnLinks = false;

		SearchEntity searchBE = new SearchEntity("SBE_DROPDOWN", " Search")
						.addColumn("PRI_CODE", "Code")
						.addColumn("PRI_NAME", "Name");

		Map<String, Object> ctxMap = new ConcurrentHashMap<>();
		if (sourceBe!=null) {
			ctxMap.put("SOURCE", sourceBe);
		}
		if (targetBe!=null) {
			ctxMap.put("TARGET", targetBe);
		}
		
		

		JsonArray jsonParms = searchValueJson.getJsonArray("parms");
		for (Object parmValue : jsonParms) {

			try {
				JsonObject json = (JsonObject) parmValue;
				String attributeCode = json.getString("attributeCode");

				// Filters
				if (attributeCode != null) {

					Attribute att = RulesUtils.getAttribute(attributeCode, beUtils.getServiceToken());
					String val = json.getString("value");

					String filterStr = null;
					if (val.contains(":")) {
						String[] valSplit = val.split(":");
						filterStr = valSplit[0];
						val = valSplit[1];
					}

					DataType dataType = att.getDataType();

					if (dataType.getClassName().equals("life.genny.qwanda.entity.BaseEntity")) {
						if (attributeCode.equals("LNK_CORE") || attributeCode.equals("LNK_IND")) {  // These represent EntityEntity
						// This is used for the sort defaults
						searchingOnLinks = true;

						// For using the search source and target
						String sourceCode = json.getString("sourceCode");
						String targetCode = json.getString("targetCode");

						// These will return True by default if source or target are null
						if (!MergeUtil.contextsArePresent(sourceCode, ctxMap)) {
							log.error(ANSIColour.RED+"A Parent value is missing for " + sourceCode + ", Not sending dropdown results"+ANSIColour.RESET);
							return null;
						}
						if (!MergeUtil.contextsArePresent(targetCode, ctxMap)) {
							log.error(ANSIColour.RED+"A Parent value is missing for " + targetCode + ", Not sending dropdown results"+ANSIColour.RESET);
							return null;
						}

						// Merge any data for source and target
						sourceCode = MergeUtil.merge(sourceCode, ctxMap);
						targetCode = MergeUtil.merge(targetCode, ctxMap);

						log.info("attributeCode = " + json.getString("attributeCode"));
						log.info("val = " + val);
						log.info("link sourceCode = " + sourceCode);
						log.info("link targetCode = " + targetCode);

						// Set Source and Target if found it parameter
						if (sourceCode != null) {
							searchBE.setSourceCode(sourceCode);
						}
						if (targetCode != null) {
							searchBE.setTargetCode(targetCode);
						}

						// Set LinkCode and LinkValue
						searchBE.setLinkCode(att.getCode());
						searchBE.setLinkValue(val);
						} else {
							// This is a DTT_LINK style that has class = baseentity --> Baseentity_Attribute
							SearchEntity.StringFilter stringFilter = SearchEntity.StringFilter.LIKE;  // TODO equals?
							if (filterStr != null) {
								stringFilter = SearchEntity.convertOperatorToStringFilter(filterStr);
							}
							searchBE.addFilter(attributeCode, stringFilter, val);
							
						}
						
						
						
						
					} else if (dataType.getClassName().equals("java.lang.String")) {
						SearchEntity.StringFilter stringFilter = SearchEntity.StringFilter.LIKE;
						if (filterStr != null) {
							stringFilter = SearchEntity.convertOperatorToStringFilter(filterStr);
						}
						searchBE.addFilter(attributeCode, stringFilter, val);
					} else {
						SearchEntity.Filter filter = SearchEntity.Filter.EQUALS;
						if (filterStr != null) {
							filter = SearchEntity.convertOperatorToFilter(filterStr);
						}
						searchBE.addFilterAsString(attributeCode, filter, val);
					}
				}

				// Sorts
				String sortBy = json.getString("sortBy");
				if (sortBy != null) {
					String order = json.getString("order");
					SearchEntity.Sort sortOrder = order.equals("DESC") ? SearchEntity.Sort.DESC : SearchEntity.Sort.ASC;
					searchBE.addSort(sortBy, sortBy, sortOrder);
				}

				// Conditionals
				if (json.containsKey("conditions")) {
					JsonArray conditions = json.getJsonArray("conditions");
					for (Object cond : conditions) {
						searchBE.addConditional(attributeCode, cond.toString());
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error("DROPDOWN :Bad Json Value ---> " + parmValue.toString());
				continue;
			}
		}

		// Default to sorting by name if no sorts were specified and if not searching for EntityEntitys
		Boolean hasSort = searchBE.getBaseEntityAttributes().stream().anyMatch(item -> item.getAttributeCode().startsWith("SRT_"));
		if (!hasSort && !searchingOnLinks) {
			searchBE.addSort("PRI_NAME", "Name", SearchEntity.Sort.ASC);
		}
		
		// Filter by name wildcard provided by user
		searchBE.addFilter("PRI_NAME", SearchEntity.StringFilter.LIKE,searchText+"%")
		.addOr("PRI_NAME", SearchEntity.StringFilter.LIKE, "% "+searchText+"%");

		searchBE.setRealm(beUtils.getServiceToken().getRealm());
		searchBE.setPageStart(pageStart);
		searchBE.setPageSize(pageSize);
		pageStart += pageSize;

		// Capability Based Conditional Filters
		searchBE = SearchUtils.evaluateConditionalFilters(beUtils, searchBE);

		// Merge required attribute values
		// NOTE: This should correct any wrong datatypes too
		searchBE = SearchUtils.mergeFilterValueVariables(beUtils, searchBE, ctxMap);
		if (searchBE == null) {
			log.error(ANSIColour.RED + "Cannot Perform Search!!!" + ANSIColour.RESET);
			return null;
		}

		// Perform search and evaluate columns
		SearchUtils searchUtils = new SearchUtils(beUtils);
		QDataBaseEntityMessage msg = null;
		
		msg = searchUtils.searchUsingSearch25(beUtils.getServiceToken(), searchBE);
		
		if (msg == null) {
			log.error(ANSIColour.RED + "Dropdown search returned NULL!" + ANSIColour.RESET);
			return null;

		} else if (msg.getItems().length > 0) {
			log.info("DROPDOWN :Loaded " + msg.getItems().length + " baseentitys");

			for (BaseEntity item : msg.getItems()) {
				if ( item.getValueAsString("PRI_NAME") == null ) {
					log.warn("DROPDOWN : item: " + item.getCode() + " ===== " + item.getValueAsString("PRI_NAME"));
				} else {
					log.info("DROPDOWN : item: " + item.getCode() + " ===== " + item.getValueAsString("PRI_NAME"));
				}
			}
		} else {
			log.info("DROPDOWN :Loaded NO baseentitys");
		}

		// BaseEntity[] arrayItems = items.toArray(new BaseEntity[0]);
		// QDataBaseEntityMessage msg =  new QDataBaseEntityMessage(arrayItems, message.getData().getParentCode(), "LINK", Long.decode(items.size()+""));
//		log.info("DROPDOWN :code = "+dropdownCode+" with "+Long.decode(msg.getItems().length+"")+" Items");
//		log.info("DROPDOWN :parentCode = "+message.getData().getParentCode());
		msg.setParentCode(parentCode);
		msg.setQuestionCode(questionCode); 
		msg.setToken(beUtils.getGennyToken().getToken());
		msg.setLinkCode("LNK_CORE");
		msg.setLinkValue("ITEMS");
		msg.setReplace(true);
		msg.setShouldDeleteLinkedBaseEntities(false);

		/* Linking child baseEntity to the parent baseEntity */
		// QDataBaseEntityMessage beMessage = setDynamicLinksToParentBe(msg, message.getData().getParentCode(), "LNK_CORE", "DROPDOWNITEMS", beUtils.getGennyToken(),
		// 		false);
		return msg;
		// return beMessage;
	}
	
}
