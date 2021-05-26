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

import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import life.genny.models.GennyToken;
import life.genny.qwanda.Ask;
import life.genny.qwanda.Link;
import life.genny.qwanda.Question;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.entity.SearchEntity.StringFilter;
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QEventDropdownMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;

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
		// log.info("The search BE is :: " + JsonUtils.toJson(searchBE));

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

						if (attr != null) {

							Question childQuestion = new Question("QUE_" + attributeCode + "_" + be.getCode(),
									attributeName, attr, true);
							Ask childAsk = new Ask(childQuestion, targetCode, be.getCode());

							/* add the entityAttribute ask to list */
							childAskList.add(childAsk);
						} else {
							System.out.println("The attribute " + attributeCode
									+ " was null while fetching from RulesUtils attribute map");
						}

					}

					/* converting childAsks list to array */
					Ask[] childAsArr = childAskList.stream().toArray(Ask[]::new);

					/* Get the on-the-fly question attribute */
					Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link",
							new DataType(String.class));

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
		BaseEntity internBe = beUtils.getBaseEntityByCode("DEF_INTERN");
		// BaseEntity defBe = beUtils.getDEF(targetBe);

		/* targetBe = beUtils.getBaseEntityByCode(message.getData().getTargetCode()); */
		/* BaseEntity defBe = beUtils.getDEF(targetBe); */

		BaseEntity defBe = beUtils.getBaseEntityByCode("DEF_INTERN");

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
		String serValue = "{\"search\":\"SBE_DROPDOWN\",\"parms\":[{\"attributeCode\":\"PRI_IS_EDU_PROVIDER\",\"value\":\"true\"}]}";
		if (searchAtt.isPresent()) {
			serValue = searchAtt.get().getValueString();
			System.out.println("Search Attribute Value = " + serValue);
		} else {
			return new QDataBaseEntityMessage();
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
				case "org.javamoney.moneta.Money":
				case "java.lang.String":
				default:
					SearchEntity.StringFilter stringFilter = SearchEntity.convertOperatorToStringFilter(valSplit[0]);
					if (valSplit[0].contains("LIKE")) {
						val = val + "%";// just keep the front bit
					}
					searchBE.addFilter(searchBeCode, stringFilter, val);

				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error("Bad Json Value ---> " + parmValue.toString());
				continue;
			}
		}
		/* searchBE.addFilter("PRI_IS_EDU_PROVIDER", true); */
		//searchBE.addFilter("PRI_NAME", StringFilter.REGEXP, "\\\\b"+message.getData().getValue());
		
		searchBE.addFilter("PRI_NAME", SearchEntity.StringFilter.LIKE,message.getData().getValue()+"%")
		.addOr("PRI_NAME", SearchEntity.StringFilter.LIKE, "% "+message.getData().getValue()+"%");

		searchBE.setRealm(beUtils.getServiceToken().getRealm());
		searchBE.setPageStart(pageStart);
		searchBE.setPageSize(pageSize);
		pageStart += pageSize;
		
		List<BaseEntity> items = beUtils.getBaseEntitys(searchBE);
		if (!items.isEmpty()) {
			log.info("Loaded " + items.size() + " baseentitys");
		}

		for (BaseEntity item : items) {
			
			System.out.println("item: " + item.getCode() + " ===== " + item.getValueAsString("PRI_NAME"));
		}

		BaseEntity[] arrayItems = items.toArray(new BaseEntity[0]);
		System.out.println("questionCode = "+message.getQuestionCode()+" with "+Long.decode(items.size()+"")+" Items");
		System.out.println("parentCode = "+message.getData().getParentCode());
		QDataBaseEntityMessage msg =  new QDataBaseEntityMessage(arrayItems, message.getData().getParentCode(), "LINK", Long.decode(items.size()+""));
		msg.setParentCode(message.getData().getParentCode());
		msg.setQuestionCode(message.getQuestionCode()); 
		msg.setToken(beUtils.getGennyToken().getToken());
		msg.setLinkCode("LNK_CORE");
		msg.setLinkValue("DROPDOWNITEMS");
		msg.setReplace(true);
		msg.setShouldDeleteLinkedBaseEntities(false);

		/* Linking child baseEntity to the parent baseEntity */
		QDataBaseEntityMessage beMessage = setDynamicLinksToParentBe(msg, message.getData().getParentCode(), "LNK_CORE", "DROPDOWNITEMS", beUtils.getGennyToken(),
				false);

		return beMessage;
	}

	/*
	 * Setting dynamic links between parents and child. ie. linking DropDown items
	 * to the DropDown field. Copied from DropdownUtils
	 */
	static public  QDataBaseEntityMessage setDynamicLinksToParentBe(QDataBaseEntityMessage beMsg, String parentCode, String linkCode,
			String linkValue, GennyToken gennyToken, Boolean sortByWeight) {
		BaseEntity parentBe = new BaseEntityUtils(gennyToken).getBaseEntityByCode(parentCode);
		if (parentBe != null) {
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

						BaseEntity[] sortedItems = sortBaseEntityByWeight(beMsg.getItems(), parentBe.getCode(), sortedChildLinks);
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
		} else {
			log.error("Unable to fetch Parent BaseEntity : parentCode");
			return null;
		}
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

}
