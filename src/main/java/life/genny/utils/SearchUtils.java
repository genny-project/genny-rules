package life.genny.utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.gson.internal.LinkedTreeMap;

import life.genny.qwanda.*;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.Logger;
import life.genny.qwanda.exception.BadDataException;
import io.vavr.Tuple2;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import life.genny.models.GennyToken;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QEventDropdownMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.qwanda.datatype.CapabilityMode;
import life.genny.qwandautils.MergeUtil;
import life.genny.qwandautils.ANSIColour;

public class SearchUtils {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	BaseEntityUtils beUtils = null;

	public SearchUtils(BaseEntityUtils beUtils) {
		this.beUtils = beUtils;
	}

	public QDataBaseEntityMessage fetchSearchResults(SearchEntity searchBE, GennyToken gennyToken) {
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(new ArrayList<BaseEntity>());

		log.error("This should be changed to use the search24");

		if (gennyToken == null) {
			log.error("GENNY TOKEN IS NULL!!! in getSearchResults");
			return msg;
		}
		searchBE.setRealm(gennyToken.getRealm());
		//log.info("The search BE is :: " + JsonUtils.toJson(searchBE));

		String jsonSearchBE = JsonUtils.toJson(searchBE);
		String resultJson;
		try {
			resultJson = QwandaUtils.apiPostEntity(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/search",
					jsonSearchBE, beUtils.getServiceToken().getToken());
			final BaseEntity[] items = new BaseEntity[0];
			final String parentCode = "GRP_ROOT";
			final String linkCode = "LINK";
			final Long total = 0L;

			if (resultJson == null) {
				msg = new QDataBaseEntityMessage(items, parentCode, linkCode, total);
				log.info("The result of getSearchResults was null  ::  " + msg);
			} else {
				try {
					msg = JsonUtils.fromJson(resultJson, QDataBaseEntityMessage.class);
					if (msg == null) {
						msg = new QDataBaseEntityMessage(items, parentCode, linkCode, total);
						log.info("The result of getSearchResults was null Exception ::  " + msg);
					} else {
						log.info("The result of getSearchResults was " + msg.getItems().length + " items ");
					}
				} catch (Exception e) {
					log.info("The result of getSearchResults was null Exception ::  " + msg);
					msg = new QDataBaseEntityMessage(items, parentCode, linkCode, total);
				}
			}

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		msg.setToken(gennyToken.getToken());
		return msg;

	}

	/**
	 * @param serviceToken
	 * @param searchBE
	 * @param msg
	 * @return
	 */
	public QDataBaseEntityMessage searchUsingSearch25(GennyToken serviceToken, SearchEntity searchBE) {
		long starttime = System.currentTimeMillis();
		long endtime2 = starttime;

		List<EntityAttribute> cals = searchBE.findPrefixEntityAttributes("COL__");
		if (cals != null) {
			log.info("searchUsingSearch25 -> detected " + cals.size() + " CALS");

			for (EntityAttribute calEA : cals) {
				log.info("Found CAL with code: " + calEA.getAttributeCode());
			}
		}

		BaseEntity[] beArray = null;
		JsonArray result = null;
		Long total = Long.valueOf(0);

		// Check for a specific item search
		for (EntityAttribute attr : searchBE.getBaseEntityAttributes()) {
			if (attr.getAttributeCode().equals("PRI_CODE") && attr.getAttributeName().equals("_EQ_")) {
				log.info("SINGLE BASE ENTITY SEARCH DETECTED");
				result = new JsonArray("[\""+attr.getValue()+"\"]");
				break;
			}
		}

		String resultJsonStr = null;
		JsonObject resultJson = null;

		// Perform the search if specific item not found
		if (result == null) {
			try {

				Boolean useFyodor = (System.getenv("USE_FYODOR") != null && "TRUE".equalsIgnoreCase(System.getenv("USE_FYODOR"))) ? true : false;
				// Set to FALSE to use regular search
				if (false) {
					resultJsonStr = QwandaUtils.apiPostEntity2(
							GennySettings.fyodorServiceUrl + "/api/search",
							JsonUtils.toJson(searchBE), serviceToken.getToken(), null);
				} else {
					resultJsonStr = QwandaUtils.apiPostEntity2(
							GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/search25/",
							JsonUtils.toJson(searchBE), serviceToken.getToken(), null);
				}

				endtime2 = System.currentTimeMillis();
				log.info("NOT SINGLE - Time taken to fetch Data =" + (endtime2 - starttime) + " ms");

				try {
					resultJson = new JsonObject(resultJsonStr);
					result = resultJson.getJsonArray("codes");
					total = resultJson.getLong("total");
				} catch (Exception e) {
					log.error("TableUtils: SearchUsingSearch25 -> Bad Json ("+resultJsonStr+") , " +
					 "returning empty search, token:" + serviceToken.getToken());
					beArray = new BaseEntity[]{};
					result = new JsonArray();
					total = 0L;
				}


			} catch (Exception e1) {
				log.error("Exception:"  +  e1.getMessage() + " occurred, resultJsonStr:" + resultJsonStr);
				e1.printStackTrace();
				beArray = new BaseEntity[]{};
			}
		} else {
			total = Long.valueOf(1);
		}

		try {
			beArray = new BaseEntity[result.size()];

			for (int i = 0; i < result.size(); i++) {

				String code = result.getString(i);
				BaseEntity be = beUtils.getBaseEntityByCode(code);
				be.setIndex(i);
				beArray[i] = be;
			}

		} catch (Exception e1) {
			log.error("Possible Bad Json -> " + resultJsonStr);
			log.error("Exception -> " + e1.getLocalizedMessage());
			beArray = new BaseEntity[]{};
		}

		// Create and send ask grp if necessary
		EntityAttribute searchQuestionCode = searchBE.findEntityAttribute("SCH_QUESTION_CODE").orElse(null);
		if (searchQuestionCode != null) {
			QBulkMessage askEntityData = SearchUtils.getAskEntityData(beUtils, searchBE, beArray);

			if (askEntityData != null) {
				log.info("Sending bulk message");
				askEntityData.setToken(beUtils.getGennyToken().getToken());
				VertxUtils.writeMsg("webcmds", JsonUtils.toJson(askEntityData));
			} else {
				log.info("searchAskGrp is NULL, not sending!");
			}
		} else {
			// Used to disable the column privacy
			EntityAttribute columnWildcard = searchBE.findEntityAttribute("COL_*").orElse(null);
			// Find Alowed Columns
			String[] filterArray = VertxUtils.getSearchColumnFilterArray(searchBE).toArray(new String[0]);

			// Otherwise handle cals
			for (BaseEntity be : beArray) {

				// Filter unwanted attributes
				if (columnWildcard == null) {

					be = VertxUtils.privacyFilter(be, filterArray);
				}

				for (EntityAttribute calEA : cals) {

					Answer ans = getAssociatedColumnValue(beUtils, be, calEA.getAttributeCode(), serviceToken);

					if (ans != null) {
						try {
							be.addAnswer(ans);
						} catch (BadDataException e) {
							e.printStackTrace();
						}
					}

				}
			}
		}

		// Create BE msg from array
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(beArray);
		msg.setTotal(total);
		msg.setReplace(true);
		msg.setParentCode(searchBE.getCode());
		log.info("Search Results = " + beArray.length + " out of total " + total);

		long endtime3 = System.currentTimeMillis();
		log.info("Time taken to get cached Bes added to list =" + (endtime3 - endtime2) + " ms");

		return msg;
	}

	public static Answer getAssociatedColumnValue(BaseEntityUtils beUtils, BaseEntity baseBE, String calEACode, GennyToken serviceToken) {

		String[] calFields = calEACode.substring("COL__".length()).split("__");
		if (calFields.length == 1) {
			log.error("CALS length is bad for :" + calEACode);
			return null;
		}
		String linkBeCode = calFields[calFields.length-1];

		BaseEntity be = baseBE;

		Optional<EntityAttribute> associateEa = null;
		// log.info("calFields value " + calEACode);
		// log.info("linkBeCode value " + linkBeCode);

		String finalAttributeCode = calEACode.substring("COL_".length());
		// Fetch The Attribute of the last code
		String primaryAttrCode = calFields[calFields.length-1];
		Attribute primaryAttribute = RulesUtils.getAttribute(primaryAttrCode, serviceToken);

		Answer ans = new Answer(baseBE.getCode(), baseBE.getCode(), finalAttributeCode, "");
		Attribute att = new Attribute(finalAttributeCode, primaryAttribute.getName(), primaryAttribute.getDataType());
		ans.setAttribute(att);

		for (int i = 0; i < calFields.length-1; i++) {
			String attributeCode = calFields[i];
			String calBe = be.getValueAsString(attributeCode);

			if (calBe != null && !StringUtils.isBlank(calBe)) {
				String calVal = beUtils.cleanUpAttributeValue(calBe);
				String[] codeArr = calVal.split(",");
				for (String code : codeArr) {
					if (StringUtils.isBlank(code)) {
						log.error("code from Calfields is empty calVal["+calVal+"] skipping calFields=["+calFields+"] - be:"+baseBE.getCode());
						
						continue;
					}
					BaseEntity associatedBe = beUtils.getBaseEntityByCode(code);
					if (associatedBe == null) {
						log.warn("associatedBe DOES NOT exist ->" + code);
						return null;
					}

					if (i == (calFields.length-2)) {
						associateEa = associatedBe.findEntityAttribute(linkBeCode);

						if (associateEa != null && (associateEa.isPresent() || ("PRI_NAME".equals(linkBeCode)))) {
							String linkedValue = null;
							if ("PRI_NAME".equals(linkBeCode)) {
								linkedValue = associatedBe.getName();
							} else {
								linkedValue = associatedBe.getValueAsString(linkBeCode);
							}
							if (!ans.getValue().isEmpty()) {
								linkedValue = ans.getValue() + "," + linkedValue;
							}
							ans.setValue(linkedValue);
						} else {
							log.warn("TableUtils: No attribute present");
						}
					}
					be = associatedBe;
				}
			} else {
				log.warn("TableUtils: Could not find attribute value for " + attributeCode + " for entity " + be.getCode());
				return null;
			}
		}

		return ans;
	}


	/* Generate List of asks from a SearchEntity */
	public static List<Ask> generateQuestions(GennyToken userToken, BaseEntityUtils beUtils, List<BaseEntity> bes,
			Map<String, String> columns, String targetCode) {

		/* initialize an empty ask list */
		List<Ask> askList = new ArrayList<>();
		List<QDataBaseEntityMessage> themeMsgList = new ArrayList<QDataBaseEntityMessage>();

		if (columns != null) {
			if (bes != null && bes.isEmpty() == false) {

				/* loop through baseentities to generate ask */
				for (BaseEntity be : bes) {

					/* we add attributes for each be */
					beUtils.addAttributes(be);

					/* initialize child ask list */
					List<Ask> childAskList = new ArrayList<>();

					for (Map.Entry<String, String> column : columns.entrySet()) {

						String attributeCode = column.getKey();
						String attributeName = column.getValue();
						Attribute attr = RulesUtils.realmAttributeMap.get(beUtils.getGennyToken().getRealm()).get(attributeCode);

						if(attr!=null) {

							Question childQuestion = new Question("QUE_" + attributeCode + "_" + be.getCode(), attributeName, attr,
									true);
							Ask childAsk = new Ask(childQuestion, targetCode, be.getCode());

							/* add the entityAttribute ask to list */
							childAskList.add(childAsk);
						}else {
							System.out.println("The attribute " + attributeCode +" was null while fetching from RulesUtils attribute map");
						}


					}

					/* converting childAsks list to array */
					Ask[] childAsArr = childAskList.stream().toArray(Ask[]::new);

					/* Get the on-the-fly question attribute */
					Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));

					Attribute questionTableRowAttribute = new Attribute("QQQ_QUESTION_GROUP_TABLE_ROW", "link",
							new DataType(String.class));

					/* Generate ask for the baseentity */
					Question parentQuestion = new Question("QUE_" + be.getCode() + "_GRP", be.getName(),
							questionTableRowAttribute, true);
					Ask parentAsk = new Ask(parentQuestion, targetCode, be.getCode());

					/* setting weight to parent ask */
					parentAsk.setWeight(be.getIndex().doubleValue());

					/* set all the childAsks to parentAsk */
					parentAsk.setChildAsks(childAsArr);

					/* add the baseentity asks to a list */
					askList.add(parentAsk);
				}

			}

		}
		/* return list of asks */
		return askList;
	}

	public Map<String, String> getTableColumns(SearchEntity searchBe) {

		Map<String, String> columns = new LinkedHashMap<String, String>();
		List<EntityAttribute> cols = searchBe.getBaseEntityAttributes().stream().filter(x -> {
			return (x.getAttributeCode().startsWith("COL_") || x.getAttributeCode().startsWith("CAL_"));
		}).sorted(Comparator.comparing(EntityAttribute::getWeight)) // comparator - how you want to sort it
				.collect(Collectors.toList()); // collector - what you want to collect it to

		for (EntityAttribute ea : cols) {
			String attributeCode = ea.getAttributeCode();
			String attributeName = ea.getAttributeName();
			if (attributeCode.startsWith("COL__")) {
				columns.put(attributeCode.split("COL__")[1], attributeName); // TODO
			} else if (attributeCode.startsWith("COL_")) {
				columns.put(attributeCode.split("COL_")[1], attributeName);
			} else if (attributeCode.startsWith("CAL_")) {
				columns.put(attributeCode.split("CAL_")[1], attributeName);
			} else if (attributeCode.startsWith("QUE_")) {
				columns.put(attributeCode, attributeName);
			}
		}
		log.info("the Columns is :: " + columns);
		return columns;
	}

	public static SearchEntity evaluateConditionalFilters(BaseEntityUtils beUtils, SearchEntity searchBE) {

		CapabilityUtils capabilityUtils = new CapabilityUtils(beUtils);

		List<String> shouldRemove = new ArrayList<>();

		for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {
			if (!ea.getAttributeCode().startsWith("CND_")) {
				// Find Conditional Filters
				EntityAttribute cnd = searchBE.findEntityAttribute("CND_"+ea.getAttributeCode()).orElse(null);

				if (cnd != null) {
					String[] condition = cnd.getValue().toString().split(":");

					String capability = condition[0];
					String mode = condition[1];

					// Check for NOT operator
					Boolean not = capability.startsWith("!");
					capability = not ? capability.substring(1) : capability;

					// Check for Capability
					Boolean hasCap = capabilityUtils.hasCapabilityThroughPriIs(capability, CapabilityMode.valueOf(mode));

					// XNOR operator
					if (!(hasCap ^ not)) {
						shouldRemove.add(ea.getAttributeCode());
					}
				}
			}
		}

		// Remove unwanted attrs
		shouldRemove.stream().forEach(item -> {searchBE.removeAttribute(item);});

		return searchBE;
	}

	public static SearchEntity mergeFilterValueVariables(BaseEntityUtils beUtils, SearchEntity searchBE, Map<String, Object> ctxMap) {

		for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {
			// Iterate all Filters
			if (ea.getAttributeCode().startsWith("PRI_") || ea.getAttributeCode().startsWith("LNK_")) {

				// Grab the Attribute for this Code, using array in case this is an associated filter
				String[] attributeCodeArray = ea.getAttributeCode().split("\\.");
				String attributeCode = attributeCodeArray[attributeCodeArray.length-1];
				// Fetch the corresponding attribute
				Attribute att = RulesUtils.getAttribute(attributeCode, beUtils.getServiceToken());
				DataType dataType = att.getDataType();

				Object attributeFilterValue = ea.getValue();
				if (attributeFilterValue != null) {
					// Ensure EntityAttribute Dataype is Correct for Filter
					Attribute searchAtt = new Attribute(ea.getAttributeCode(), ea.getAttributeName(), dataType);
					ea.setAttribute(searchAtt);
					String attrValStr = attributeFilterValue.toString();

					// First Check if Merge is required
					Boolean requiresMerging = MergeUtil.requiresMerging(attrValStr);

					if (requiresMerging != null && requiresMerging) {
					// update Map with latest baseentity
						ctxMap.keySet().forEach(key -> {
							Object value = ctxMap.get(key);
							if (value.getClass().equals(BaseEntity.class)) {
								BaseEntity baseEntity = (BaseEntity) value;
								ctxMap.put(key, beUtils.getBaseEntityByCode(baseEntity.getCode()));
							}
						});
						// Check if contexts are present
						if (MergeUtil.contextsArePresent(attrValStr, ctxMap)) {
							// NOTE: HACK, mergeUtil should be taking care of this bracket replacement - Jasper (6/08/2021)
							Object mergedObj = MergeUtil.wordMerge(attrValStr.replace("[[", "").replace("]]", ""), ctxMap);
							// Ensure Dataype is Correct, then set Value
							ea.setValue(mergedObj);
						} else {
							log.error(ANSIColour.RED + "Not all contexts are present for " + attrValStr + ANSIColour.RESET);
							return null;
						}
					} else {
						// This should filter out any values of incorrect datatype
						ea.setValue(attributeFilterValue);
					}
				} else {
					log.error(ANSIColour.RED + "Value is NULL for entity attribute " + attributeCode + ANSIColour.RESET);
					return null;
				}
			}
		}

		return searchBE;
	}


	static public QDataBaseEntityMessage getDropdownData(BaseEntityUtils beUtils, QEventDropdownMessage message) {

		return getDropdownData(beUtils,message,GennySettings.defaultDropDownPageSize);
	}

	static public QDataBaseEntityMessage getDropdownData(BaseEntityUtils beUtils, QEventDropdownMessage message, Integer dropdownSize) {
		BaseEntity targetBe = beUtils.getBaseEntityByCode(message.getData().getTargetCode());
		BaseEntity sourceBe = beUtils.getBaseEntityByCode(message.getData().getSourceCode());
		String searchText = message.getData().getValue();
		String parentCode = message.getData().getParentCode();
		String questionCode = message.getQuestionCode();

		return getDropdownData(beUtils, sourceBe,targetBe, message.getAttributeCode(),parentCode, questionCode, searchText, dropdownSize);
	}

	static public QDataBaseEntityMessage getDropdownData(BaseEntityUtils beUtils, BaseEntity sourceBe,BaseEntity targetBe, final String attributeCode, final String parentCode, final String questionCode, final String searchText, Integer dropdownSize) {

		BaseEntity project = beUtils.getBaseEntityByCode("PRJ_" + beUtils.getServiceToken().getRealm().toUpperCase());

		//BaseEntity internBe = beUtils.getBaseEntityByCode("DEF_INTERN");
		BaseEntity defBe = beUtils.getDEF(targetBe);
		log.info("DROPDOWN :identified Dropdown Target Baseentity as "+defBe.getCode()+" : "+defBe.getName());
		log.info("DROPDOWN :identified Dropdown Attribute as "+attributeCode);

		/*
		 * Now check if this attribute is ok if
		 * (!internBe.containsEntityAttribute("ATT_"+qEventDropdownMessage.
		 * getAttributeCode())) {
		 * System.out.println("Error - Attribute "+qEventDropdownMessage.
		 * getAttributeCode()+" is not allowed for this target"); }
		 */

		/*
		 * because it is a drop down event we will search the DEF for the search
		 * attribute
		 */
		Optional<EntityAttribute> searchAtt = defBe.findEntityAttribute("SER_" + attributeCode); // SER_LNK_EDU_PROVIDER
		// String serValue = "{\"search\":\"SBE_DROPDOWN\",\"parms\":[{\"attributeCode\":\"PRI_IS_EDU_PROVIDER\",\"value\":\"true\"}]}";
		String serValue = "{\"search\":\"SBE_DROPDOWN\",\"parms\":[{\"attributeCode\":\"PRI_IS_INTERN\",\"value\":\"true\"}]}";
		if (searchAtt.isPresent()) {
			serValue = searchAtt.get().getValueString();
			log.info("DROPDOWN :Search Attribute Value = " + serValue);
		} else {
			//return new QDataBaseEntityMessage();
		}


		return DefUtils.getDropdownDataMessage(beUtils, attributeCode, parentCode, questionCode,serValue, sourceBe, targetBe, searchText, beUtils.getServiceToken().getToken());

	}

	/*
	 * Setting dynamic links between parents and child. ie. linking DropDown items
	 * to the DropDown field. Copied from DropdownUtils
	 */
	static public  QDataBaseEntityMessage setDynamicLinksToParentBe(QDataBaseEntityMessage beMsg, String parentCode, String linkCode,
			String linkValue, GennyToken gennyToken, Boolean sortByWeight) {
		BaseEntity parentBe = new BaseEntityUtils(gennyToken).getBaseEntityByCode(parentCode);
		if (parentBe != null) {
			log.error("Found parentBE");
		} else {
			log.error("Unable to fetch Parent BaseEntity : parentCode->"+parentCode);
			log.error("Creating entity instead");
			parentBe = new BaseEntity(parentCode, parentCode);
		}
		Set<EntityEntity> childLinks = new LinkedHashSet<>();
		double index = -1.0;
		/* creating a dumb attribute for linking the search results to the parent */
		Attribute attributeLink = new Attribute(linkCode, linkCode, new DataType(String.class));

		for (BaseEntity be : beMsg.getItems()) {
			// Sort items based on weight
			if (sortByWeight) {
				if (parentBe.getLinks().size() > 0) {
					List<EntityEntity> sortedChildLinks = sortChildLinksByWeight(parentBe);
					// update links
					parentBe.setLinks(new LinkedHashSet<>(sortedChildLinks));

					BaseEntity[] sortedItems = sortBaseEntityByWeight(beMsg.getItems(), parentCode, sortedChildLinks);
					beMsg.setItems(sortedItems);
				}
				beMsg.add(parentBe);
				return beMsg;
			} else {
				index++;
				childLinks = new HashSet<>();
				parentBe.setLinks(childLinks);
				EntityEntity ee = new EntityEntity(parentBe, be, attributeLink, index);
				/* creating link for child */
				Link link = new Link(parentCode, be.getCode(), attributeLink.getCode(), linkValue, index);
				/* adding link */
				ee.setLink(link);
				/* adding child link to set of links */
				childLinks.add(ee);
			}
		}
		parentBe.setLinks(childLinks);
		beMsg.add(parentBe);
		return beMsg;

	}

	static double getWeight(BaseEntity be, String parentCode) {

		double weight = 1.0;

		try {
			Set<EntityEntity> entities = be.getLinks();

			for (EntityEntity entity : entities) {

				if (entity.getLink().getSourceCode().equals(parentCode)) {

					weight = entity.getLink().getWeight();
					break;
				}
			}
		} catch (Exception e) {

			// e.printStackTrace();
		}

		return weight;
	}

	static List<EntityEntity> sortChildLinksByWeight(BaseEntity parentBe) {
		Set<EntityEntity> childLinks = parentBe.getLinks();
		List<EntityEntity> sortedChildLinks = childLinks.stream().sorted(Comparator.comparing(EntityEntity::getWeight))
				.collect(Collectors.toList());
		return sortedChildLinks;
	}

	static BaseEntity[] sortBaseEntityByWeight(BaseEntity[] items, String parentCode,
			List<EntityEntity> sortedChildLinks) {
		// Set sorted links
		BaseEntity[] newItems = new BaseEntity[0];
		ArrayList<BaseEntity> newItemsList = new ArrayList<>();

		HashMap<String, BaseEntity> beMapping = new HashMap<>();
		for (BaseEntity be : items) {
			String beCode = be.getCode();
			beMapping.put(beCode, be);
		}

		int index = 0;
		for (EntityEntity ee : sortedChildLinks) {
			String targetCode = ee.getLink().getTargetCode();
			if (beMapping.containsKey(targetCode)) {
				beMapping.get(targetCode).setIndex(index);
				newItemsList.add(beMapping.get(targetCode));
//				newItems[index] = beMapping.get(targetCode);
				index++;
			} else {
				log.error(String.format("Parent Code %s doesn't have Link code %s", parentCode, targetCode));
			}
		}
		return newItemsList.toArray(newItems);
	}


	public static QBulkMessage getAskEntityData(BaseEntityUtils beUtils, SearchEntity searchBE, String targetCode)
	{
		BaseEntity target = beUtils.getBaseEntityByCode(targetCode);
		return getAskEntityData(beUtils, searchBE, target);
	}

	public static QBulkMessage getAskEntityData(BaseEntityUtils beUtils, SearchEntity searchBE, BaseEntity target)
	{
		BaseEntity[] targets = { target };
		return getAskEntityData(beUtils, searchBE, targets);
	}

	public static QBulkMessage getAskEntityData(BaseEntityUtils beUtils, SearchEntity searchBE, BaseEntity[] targets)
	{
		if (targets == null) {
			log.error("Target array is NULL");
			return null;
		}
		if (searchBE == null) {
			log.error("SearchBE is NULL");
			return null;
		}

		EntityAttribute searchQuestionCodeEA = searchBE.findEntityAttribute("SCH_QUESTION_CODE").orElse(null);
		if (searchQuestionCodeEA == null) {
			log.error("QuestionCode EntityAttribute is NULL for " + searchBE.getCode());
			return null;
		}

		String questionCode = searchQuestionCodeEA.getValue();

		if (questionCode == null) {
			log.error("QuestionCode is NULL for " + searchBE.getCode());
			return null;
		}
		log.info("Handling search asks for " + questionCode + " with " + targets.length + " items!");

		String token = beUtils.getGennyToken().getToken();

		String sourceCode = "PER_SOURCE";
		String targetCode = "PER_SOURCE";

		if (targets.length == 1) {
			sourceCode = beUtils.getGennyToken().getUserCode();
			targetCode = targets[0].getCode();
		}

		// Fetch the asks using the SBE's questionCode
		QDataAskMessage askMsg = QuestionUtils.getAsks(sourceCode, targetCode, questionCode, token);

		if (askMsg == null || askMsg.getItems().length == 0) {
			log.error("NULL in DB for " + questionCode);
			return null;
		}

		Ask askGrp = askMsg.getItems()[0];

		// Build a map of allowed attributes for each aliased BE
		HashMap<String, String[]> filterArrayMap = new HashMap<>();
		filterArrayMap = recursivelyFindPrivacyFilters(filterArrayMap, askGrp);

		// Build the Ask Grp
		askGrp = recursivelyConfigureAsks(beUtils, askGrp);
		askMsg = new QDataAskMessage(askGrp);

		// Fetch all columns for the SBE
		List<EntityAttribute> columns = searchBE.findPrefixEntityAttributes("ALS_");

		// Find defined alias relationships for the SBE
		HashMap<String, String> aliasMap = new HashMap<>();
		columns.stream().forEach(item -> {aliasMap.put(item.getAttributeName(), item.getAttributeCode().substring("ALS_".length()));});

		QBulkMessage askEntityData = new QBulkMessage(askMsg);

		askMsg.setToken(token);
		askMsg.setReplace(true);
		VertxUtils.writeMsg("webcmds", JsonUtils.toJson(askMsg));

		// Find the associated values from linked BEs
		for (BaseEntity target : targets) {
			// Filter unwanted attributes
			target = VertxUtils.privacyFilter(target, filterArrayMap.get("SELF"));

			for (String alias : aliasMap.keySet()) {
				String[] alsFields = aliasMap.get(alias).split("\\.");
				BaseEntity associatedBE = null;
				BaseEntity be = target;
				// Fetch the BE from this relationship
				for (int i = 0; i < alsFields.length; i++) {
					associatedBE = beUtils.getBaseEntityFromLNKAttr(be, alsFields[i]);
					be = associatedBE;
				}
				if (associatedBE != null) {
					// Privacy filter for BE
					associatedBE = VertxUtils.privacyFilter(target, filterArrayMap.get(alias));
					// Set the alias
					QDataBaseEntityMessage entityMsg = new QDataBaseEntityMessage(associatedBE, target.getCode()+"."+alias);
					entityMsg.setParentCode(searchBE.getCode());
					entityMsg.setReplace(true);

					// Add to entities for sending
					askEntityData.add(entityMsg);
				} else {
					log.info("Could not find BE for relationship " + aliasMap.get(alias));
				}
			}

			BaseEntity updated = recursivelyFindAssociatedValues(beUtils, target, askGrp);
			if (updated != null) {
				target = updated;
			}
		}

		return askEntityData;

	}

	/**
	* Used to configure the ask group recursively.
	* @param beUtils - The beUtils to help assist
	* @param ask - The ask to traverse
	 */
	public static Ask recursivelyConfigureAsks(BaseEntityUtils beUtils, Ask ask)
	{
		if (ask == null) {
			log.error("ask is NULL");
			return null;
		}

		// For now, just set readonly TRUE
		ask.setReadonly(true);

		String attrCode = ask.getAttributeCode();

		if (attrCode.startsWith("QQQ_QUESTION_GROUP")) {

			for (Ask childAsk : ask.getChildAsks()) {
				childAsk = recursivelyConfigureAsks(beUtils, childAsk);
			}
		} else if (attrCode.contains(".")) {
			// Grab the alias from the attribute code
			String[] attributeFields = attrCode.split("\\.");
			String alias = attributeFields[0];
			// Create a context for this alias
			Context ctx = new Context(ContextType.ALIAS, alias);
			// Add context to ask
			List<Context> ctxList = new ArrayList<>(Arrays.asList(ctx));
			ContextList contextList = new ContextList(ctxList);
			ask.setContextList(contextList);
			// Remove alias from attributeCode
			// NOTE: may have to do this for question too
			ask.setAttributeCode(attributeFields[1]);
			ask.getQuestion().setAttributeCode(attributeFields[1]);
		}

		return ask;

	}

	public static HashMap<String, String[]> recursivelyFindPrivacyFilters(HashMap<String, String[]> filterArrayMap, Ask ask)
	{
		if (ask == null) {
			log.error("ask is NULL");
			return null;
		}

		String attrCode = ask.getAttributeCode();
		String alias = "SELF";

		if (attrCode.startsWith("QQQ_QUESTION_GROUP")) {

			for (Ask childAsk : ask.getChildAsks()) {
				filterArrayMap = recursivelyFindPrivacyFilters(filterArrayMap, childAsk);
			}

		} else if (attrCode.contains(".")) {
			// Grab the alias from the attribute code
			String[] attributeFields = attrCode.split("\\.");
			alias = attributeFields[0];
			attrCode = attributeFields[1];
		}

		List<String> list = null;
		String[] array = filterArrayMap.get(alias);
		// Init new list if one doesn't exist for this alias
		if (array == null) {
			list = new ArrayList<>();
		} else {
			list = Arrays.asList(array);
		}
		// Add our new attribute code to list of allowed codearray is nulls
		list.add(attrCode);
		// Convert back to array for saving
		array = list.toArray(new String[list.size()]);
		filterArrayMap.put(alias, array);

		return filterArrayMap;
	}

	/**
	* Used to find the associated values
	* for the asks in a recursive fashion
	* @param beUtils - The beUtils to help assist
	* @param be - The be to find data for
	* @param ask - The ask to traverse
	 */
	public static BaseEntity recursivelyFindAssociatedValues(BaseEntityUtils beUtils, BaseEntity be, Ask ask)
	{
		if (ask == null) {
			log.error("ask is NULL");
			return be;
		}
		String attrCode = ask.getAttributeCode();

		if (attrCode.startsWith("QQQ_QUESTION_GROUP")) {

			for (Ask childAsk : ask.getChildAsks()) {
				recursivelyFindAssociatedValues(beUtils, be, childAsk);
			}
		// Only fire for assoc values, others should already be in the BE
		} else if (attrCode.startsWith("_LNK") || attrCode.startsWith("_PRI")) {

			Answer ans = getAssociatedColumnValue(beUtils, be, "COL_"+attrCode, beUtils.getServiceToken());

			if (ans != null) {
				try {
					be.addAnswer(ans);
				} catch (BadDataException e) {
					e.printStackTrace();
				}
			}

		}
		return be;

	}


	public static void setupSearchCache(BaseEntityUtils beUtils, final String searchCode, final Integer pageIndex) {
		SearchEntity searchBE = beUtils.getSearchEntityByCode(searchCode);
		setupSearchCache(beUtils,searchBE,pageIndex);
	}

	public static void setupSearchCache(BaseEntityUtils beUtils, final SearchEntity searchBE, final Integer pageIndex)
	{
		/* Set up resultant intern be to be sent out */
		long startProcessingTime = System.currentTimeMillis();
		long totalProcessingTime = 0L;

		TableUtils tableUtils = new TableUtils(beUtils);

		searchBE.setPageStart(pageIndex);
		QBulkMessage qbm1   = tableUtils.performSearch(beUtils.getServiceToken(), searchBE, null, null,null, true, true);
		for(QDataAskMessage msg: qbm1.getAsks()) {
			for(Ask ask: msg.getItems()) {
				ask.getQuestion().setHtml("");
			}
		}
        VertxUtils.putObject(beUtils.getGennyToken().getRealm(), "SPEEDUP", searchBE.getCode(), qbm1,beUtils.getGennyToken().getToken());

        totalProcessingTime = System.currentTimeMillis() - startProcessingTime;

	    log.info("Search Cache finished after: " + totalProcessingTime + " milliseconds");

	}

	public static void regenerateCaches(BaseEntityUtils beUtils, BaseEntity targetBe) {
		// determine the def for the targetBe
		log.info("SearchUtils: regenerateCaches - > "+targetBe);
		// BaseEntity defBe = beUtils.getDEF(targetBe);

		// // Now get the list of searches that need to be regenerated

		// String linkedCachedSearchCodes = defBe.getValue("LNK_CACHED_SEARCHES", "[]");
		// log.info("SearchUtils: regenerateCaches -> LNK_CACHED_SEARCHES="+linkedCachedSearchCodes);
		// JsonArray linkedCacheSearchCodeJsonArray = new JsonArray(linkedCachedSearchCodes);

		// for (int i = 0; i < linkedCacheSearchCodeJsonArray.size(); i++) {

		// 	String searchCodePage = linkedCacheSearchCodeJsonArray.getString(i);

		// 	/* we try to fetch the base entity */
		// 	if (searchCodePage != null) {

		// 		String[] split = searchCodePage.split(":");
		// 		if (split.length==1) {
		// 			setupSearchCache(beUtils, split[0], 0);  // just search for the first page
		// 		} else {
		// 			if (split[1].equalsIgnoreCase("*")) {
		// 				// cache all pages
		// 				// Find total count, divide into pages
		// 				// TODO
		// 			} else {
		// 				Integer maxPages = Integer.parseInt(split[1]);
		// 				for (Integer pageIndex=0;pageIndex<=maxPages;pageIndex++) { // oldschool for-loop
		// 					setupSearchCache(beUtils, split[0], pageIndex);  // just search for the asked page
		// 					log.info("SearchUtils: regenerateCaches - setup "+split[0]);
		// 				}
		// 			}
		// 		}



		// 	}
		// }

	}

}
