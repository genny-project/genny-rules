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

import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.Logger;
import life.genny.qwanda.exception.BadDataException;
import io.vavr.Tuple2;
import java.util.regex.Matcher;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.Ask;
import life.genny.qwanda.Link;
import life.genny.qwanda.Question;
import life.genny.qwanda.Context;
import life.genny.qwanda.ContextList;
import life.genny.qwanda.ContextType;
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
import life.genny.qwandautils.MergeUtil;

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
						Attribute attr = RulesUtils.attributeMap.get(attributeCode);
						
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

	static public QDataBaseEntityMessage getDropdownData(BaseEntityUtils beUtils, QEventDropdownMessage message) {
		
		return getDropdownData(beUtils,message,GennySettings.defaultDropDownPageSize);
	}

	
	static public QDataBaseEntityMessage getDropdownData(BaseEntityUtils beUtils, QEventDropdownMessage message, Integer dropdownSize) {

		BaseEntity project = beUtils.getBaseEntityByCode("PRJ_" + beUtils.getServiceToken().getRealm().toUpperCase());

		// firstly work out what the DEF isThe Nott

		BaseEntity targetBe = beUtils.getBaseEntityByCode(message.getData().getTargetCode());
		BaseEntity sourceBe = beUtils.getBaseEntityByCode(message.getData().getSourceCode());
		//BaseEntity internBe = beUtils.getBaseEntityByCode("DEF_INTERN");
		BaseEntity defBe = beUtils.getDEF(targetBe);
		log.info("DROPDOWN :identified Dropdown Target Baseentity as "+defBe.getCode()+" : "+defBe.getName());
		log.info("DROPDOWN :identified Dropdown Attribute as "+message.getAttributeCode());

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
		Optional<EntityAttribute> searchAtt = defBe.findEntityAttribute("SER_" + message.getAttributeCode()); // SER_LNK_EDU_PROVIDER
		// String serValue = "{\"search\":\"SBE_DROPDOWN\",\"parms\":[{\"attributeCode\":\"PRI_IS_EDU_PROVIDER\",\"value\":\"true\"}]}";
		String serValue = "{\"search\":\"SBE_DROPDOWN\",\"parms\":[{\"attributeCode\":\"PRI_IS_INTERN\",\"value\":\"true\"}]}";
		if (searchAtt.isPresent()) {
			serValue = searchAtt.get().getValueString();
			log.info("DROPDOWN :Search Attribute Value = " + serValue);
		} else {
			//return new QDataBaseEntityMessage();
		}

		JsonObject searchValueJson =new JsonObject(serValue);
		Integer pageStart = 0;
		Integer pageSize = dropdownSize;

		String searchBeCode = "SBE_DROPDOWN";

		SearchEntity searchBE = new SearchEntity("SBE_DROPDOWN",
				project.getValue("PRI_NAME", project.getCode()) + " Search")
						.addSort("PRI_NAME", "Name", SearchEntity.Sort.ASC).addColumn("PRI_CODE", "Code")
						.addColumn("PRI_NAME", "Name");

	
		/*
		 * SearchEntity searchBE =
		 * beUtils.getBaseEntityByCode(searchValueJson.getString("search")
		 */
		JsonArray jsonParms = searchValueJson.getJsonArray("parms");
		for (Object parmValue : jsonParms) {

			try {
				JsonObject json = (JsonObject) parmValue;
				Attribute att = RulesUtils.getAttribute(json.getString("attributeCode"), beUtils.getServiceToken());
				String val = json.getString("value");
				String[] valSplit = new String[1];
				SearchEntity.Filter filter = SearchEntity.Filter.EQUALS;
				valSplit[0] = val;
				if (val.contains(":")) {
					valSplit = val.split(":");
					filter = SearchEntity.convertOperatorToFilter(valSplit[0]);
					val = valSplit[1];
				}
				// For using the search source and target
				String sourceCode = json.getString("sourceCode");
				String targetCode = json.getString("targetCode");

				HashMap<String, Object> ctxMap = new HashMap<>();
				ctxMap.put("SOURCE", sourceBe);
				ctxMap.put("TARGET", targetBe);

				// replace our vars using the context map of BEs
				val = MergeUtil.merge(val, ctxMap);
				sourceCode = MergeUtil.merge(sourceCode, ctxMap);
				targetCode = MergeUtil.merge(targetCode, ctxMap);

				log.info("attributeCode = " + json.getString("attributeCode"));
				log.info("val = " + val);
				log.info("link sourceCode = " + sourceCode);
				log.info("link targetCode = " + targetCode);

				final String dataType = att.getDataType().getClassName();
				switch (dataType) {
				case "java.lang.Integer":
				case "Integer":
					searchBE.addFilter(json.getString("attributeCode"), filter, Integer.parseInt(val));
					break;
				case "java.time.LocalDateTime":
				case "LocalDateTime":
					String dt = val;
					LocalDateTime dateTime = null;
					List<String> formatStrings = Arrays.asList("yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss",
							"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd HH:mm:ss.SSSZ");
					for (String formatString : formatStrings) {
						try {
							Date olddate = new SimpleDateFormat(formatString).parse(dt);
							dateTime = olddate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
							break;

						} catch (ParseException e) {
						}
					}
					searchBE.addFilter(json.getString("attributeCode"), filter, dateTime);
					break;
				case "java.time.LocalTime":
				case "LocalTime":
					dt = val;
					final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
					final LocalTime ltime = LocalTime.parse(dt, formatter);
					searchBE.addFilter(json.getString("attributeCode"), filter, ltime);
					break;
				case "java.lang.Long":
				case "Long":
					searchBE.addFilter(json.getString("attributeCode"), filter, Long.parseLong(val));
					break;
				case "java.lang.Double":
				case "Double":
					searchBE.addFilter(json.getString("attributeCode"), filter, Double.parseDouble(val));
					break;
				case "java.lang.Boolean":
				case "Boolean":
					searchBE.addFilter(json.getString("attributeCode"), "TRUE".equalsIgnoreCase(val));
					break;
				case "java.time.LocalDate":
				case "LocalDate":
					dt = val;
					Date olddate = null;
					try {
						olddate = DateUtils.parseDate(dt, "M/y", "yyyy-MM-dd", "yyyy/MM/dd", "yyyy-MM-dd'T'HH:mm:ss",
								"yyyy-MM-dd'T'HH:mm:ss.SSSZ");
					} catch (java.text.ParseException e) {
						olddate = DateUtils.parseDate(dt, "yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss",
								"yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd HH:mm:ss.SSSZ");
					}
					LocalDate ldate = olddate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
					searchBE.addFilter(json.getString("attributeCode"), filter, ldate);
					break;
					
				case "life.genny.qwanda.entity.BaseEntity":
					// The LNK means that a baseentitycode is being searched for
					// TODO handle multiples
					if (att.getCode().startsWith("LNK_")) {
						// oldschool
						searchBE.setLinkCode(att.getCode());
						searchBE.setLinkValue(val);
						if (sourceCode != null) {
							searchBE.setSourceCode(sourceCode);
						}
						if (targetCode != null) {
							searchBE.setTargetCode(targetCode);
						}
					} else {
						val = "\""+val+"\"";
						searchBE.addFilter(json.getString("attributeCode"), SearchEntity.StringFilter.LIKE, val);
					}
					break;
				case "org.javamoney.moneta.Money":
				case "java.lang.String":
				default:
					// NOTE: The percent sign is now required in the search string definition. This allows more flexibility.
					SearchEntity.StringFilter stringFilter = SearchEntity.convertOperatorToStringFilter(valSplit[0]);
					searchBE.addFilter(json.getString("attributeCode"), stringFilter, val);

				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error("DROPDOWN :Bad Json Value ---> " + parmValue.toString());
				continue;
			}
		}
		/* searchBE.addFilter("PRI_IS_EDU_PROVIDER", true); */
		//searchBE.addFilter("PRI_NAME", StringFilter.REGEXP, "\\\\b"+message.getData().getValue());
		
		searchBE.addFilter("PRI_NAME", SearchEntity.StringFilter.LIKE,message.getData().getValue()+"%")
		// NOTE: TEMPORARILY DISABLED BECAUSE OR IS BROKEN
		.addOr("PRI_NAME", SearchEntity.StringFilter.LIKE, "% "+message.getData().getValue()+"%");

		searchBE.setRealm(beUtils.getServiceToken().getRealm());
		searchBE.setPageStart(pageStart);
		searchBE.setPageSize(pageSize);
		pageStart += pageSize;
		
		List<BaseEntity> items = beUtils.getBaseEntitys(searchBE);
		if (!items.isEmpty()) {
			log.info("DROPDOWN :Loaded " + items.size() + " baseentitys");
		} else {
			log.info("DROPDOWN :Loaded NO baseentitys");
		}

		for (BaseEntity item : items) {
			
			log.info("DROPDOWN : item: " + item.getCode() + " ===== " + item.getValueAsString("PRI_NAME"));
		}

		BaseEntity[] arrayItems = items.toArray(new BaseEntity[0]);
		log.info("DROPDOWN :code = "+message.getData().getCode()+" with "+Long.decode(items.size()+"")+" Items");
		log.info("DROPDOWN :parentCode = "+message.getData().getParentCode());
		QDataBaseEntityMessage msg =  new QDataBaseEntityMessage(arrayItems, message.getData().getParentCode(), "LINK", Long.decode(items.size()+""));
		msg.setParentCode(message.getData().getParentCode());
		msg.setQuestionCode(message.getQuestionCode()); 
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

			Answer ans = TableUtils.getAssociatedColumnValue(beUtils, be, "COL_"+attrCode, beUtils.getServiceToken());

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

}
