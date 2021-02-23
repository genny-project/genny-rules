package life.genny.utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.gson.reflect.TypeToken;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.types.CommandlineJava.SysProperties;
import org.hibernate.loader.plan.build.internal.returns.EntityAttributeFetchImpl;

import life.genny.jbpm.customworkitemhandlers.ShowFrame;
import life.genny.models.Frame3;
import life.genny.models.GennyToken;
import life.genny.models.TableData;
import life.genny.models.Theme;
import life.genny.models.ThemeAttribute;
import life.genny.qwanda.Answer;
import life.genny.qwanda.Ask;
import life.genny.qwanda.Context;
import life.genny.qwanda.ContextList;
import life.genny.qwanda.ContextType;
import life.genny.qwanda.Question;
import life.genny.qwanda.VisualControlType;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.message.QCmdMessage;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.validation.Validation;
import life.genny.qwanda.validation.ValidationList;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.jbpm.customworkitemhandlers.RuleFlowGroupWorkItemHandler;

import java.util.concurrent.ConcurrentHashMap;

public class TableUtils {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public static Boolean searchAlt = true;

	static Integer MAX_SEARCH_HISTORY_SIZE = 10;
	static Integer MAX_SEARCH_BAR_TEXT_SIZE = 20;

	BaseEntityUtils beUtils = null;

	public TableUtils(BaseEntityUtils beUtils) {
		this.beUtils = beUtils;
	}

	public QBulkMessage performSearch(GennyToken serviceToken, final SearchEntity searchBE, Answer answer,
			final String filterCode, final String filterValue, Boolean cache, Boolean replace) {
		QBulkMessage ret = new QBulkMessage();
		long starttime = System.currentTimeMillis();

		beUtils.setServiceToken(serviceToken);

		// Send out Search Results
		QDataBaseEntityMessage msg = null;

		List<EntityAttribute> filters = getUserFilters(serviceToken, searchBE);

		if (!filters.isEmpty()) {
			log.info("User Filters are NOT empty");
			log.info("Adding User Filters to searchBe  ::  " + searchBE.getCode());
			for (EntityAttribute filter : filters) {
				searchBE.getBaseEntityAttributes().add(filter);// ????
			}
			/* log.info("searchBE after adding filters"); */
			// log.info(searchBE);
		} else {
			log.info("User Filters are empty");
		}

		if (searchAlt && (GennySettings.searchAlt)) {
			log.info("searchCode   ::   " + searchBE.getCode());
			msg = searchUsingHql(serviceToken, searchBE, msg);
		} else {
			log.info("Old Search");
			msg = fetchSearchResults(searchBE);
		}
		long endtime1 = System.currentTimeMillis();
		log.info(
				"Time taken to search Results from SearchBE =" + (endtime1 - starttime) + " ms with total=" + msg.getTotal());

		msg.setReplace(replace);

		if (cache) {
			/* Add baseentity msg after search is done */
			ret.add(msg);
		} else {
			msg.setToken(beUtils.getGennyToken().getToken());
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg));
		}
		long endtime2 = System.currentTimeMillis();
		log.info("Time taken to send Results =" + (endtime2 - endtime1) + " ms");
		
		Long totalResultCount = msg.getTotal();

		// Perform count for any combined search attributes
		for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {
			if (ea.getAttributeCode().startsWith("CMB_")) {
				String combinedSearchCode = ea.getAttributeCode().substring("CMB_".length());
				Long subTotal = performCount(combinedSearchCode);
				if (subTotal != null) {
					totalResultCount += subTotal;
				} else {
					log.info("subTotal count for " + combinedSearchCode + " is NULL");
				}
			}
		}

		/* publishing the searchBE to frontEnd */
		updateBaseEntity(searchBE, "PRI_TOTAL_RESULTS", totalResultCount + ""); // if result
		long endtime3 = System.currentTimeMillis();
		log.info("Time taken to updateBE =" + (endtime3 - endtime2) + " ms");

		// count = 0
		// then
		// frontend
		// not
		// showing
		// anything
		QDataBaseEntityMessage searchBeMsg = new QDataBaseEntityMessage(searchBE);

		if (cache) {
			/* Add the searchBe msg */
			ret.add(searchBeMsg);
		} else {
			searchBeMsg.setToken(beUtils.getGennyToken().getToken());
			/* searchBeMsg.setReplace(true); */
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson((searchBeMsg)));
			// Now send the end_process msg
			QCmdMessage msgend = new QCmdMessage("END_PROCESS", "END_PROCESS");

			VertxUtils.writeMsgEnd(beUtils.getGennyToken());

		}
		// long endtime4 = System.currentTimeMillis();
		// log.info("Time taken to send Results =" + (endtime4 - endtime3) + " ms");

		// Map<String, String> columns = getTableColumns(searchBE);

		// long endtime5 = System.currentTimeMillis();
		// log.info("Time taken to getTableColumns =" + (endtime5 - endtime4) + " ms");

		// /*
		// * Display the table header
		// */

		// /* QDataAskMessage headerAskMsg = showTableHeader(searchBE, columns, msg); */
		// log.info("calling showTableContent");
		// QBulkMessage qb = showTableContent(serviceToken, searchBE, msg, columns,
		// cache);
		// /* Adds the rowAsk and table title ask message */
		// ret.add(qb);
		// log.info("calling sendTableContexts");

		/*
		 * QDataBaseEntityMessage qm = sendTableContexts(cache); ret.add(qm);
		 */
		/* showTableFooter(searchBE); */

		return ret;
	}

	public List<EntityAttribute> getUserFilters(GennyToken serviceToken, final SearchEntity searchBE) {
		List<EntityAttribute> filters = new ArrayList<EntityAttribute>();

		Map<String, Object> facts = new ConcurrentHashMap<String, Object>();
		facts.put("serviceToken", beUtils.getServiceToken());
		facts.put("userToken", beUtils.getGennyToken());
		facts.put("searchBE", searchBE);

		/* log.info("facts   ::  " +facts); */
		RuleFlowGroupWorkItemHandler ruleFlowGroupHandler = new RuleFlowGroupWorkItemHandler();

		log.info("serviceToken " + beUtils.getServiceToken());
		Map<String, Object> results = ruleFlowGroupHandler.executeRules(beUtils.getServiceToken(), beUtils.getGennyToken(),
				facts, "SearchFilters", "TableUtils:GetFilters");

		Object obj = results.get("payload");
		/* log.info("obj   ::   " +obj); */

		if (obj instanceof QBulkMessage) {
			QBulkMessage bulkMsg = (QBulkMessage) results.get("payload");

			// Check if bulkMsg not empty
			if (bulkMsg.getMessages().length > 0) {

				// Get the first QDataBaseEntityMessage from bulkMsg
				QDataBaseEntityMessage msg = bulkMsg.getMessages()[0];

				// Check if msg is not empty
				if (msg.getItems().length > 0) {

					// Extract the baseEntityAttributes from the first BaseEntity
					Set<EntityAttribute> filtersSet = msg.getItems()[0].getBaseEntityAttributes();
					filters.addAll(filtersSet);
				}
			}
		}
		return filters;

	}

	/**
	 * @param serviceToken
	 * @param searchBE
	 * @param msg
	 * @return
	 */
	public QDataBaseEntityMessage searchUsingHql(GennyToken serviceToken, final SearchEntity searchBE,
			QDataBaseEntityMessage msg) {
		long starttime = System.currentTimeMillis();
		long endtime2 = starttime;

		List<EntityAttribute> cals = searchBE.findPrefixEntityAttributes("COL__");
		
		
		Tuple2<String, List<String>> data = beUtils.getHql(searchBE);
		long endtime1 = System.currentTimeMillis();
		log.info("Time taken to getHql from SearchBE =" + (endtime1 - starttime) + " ms");

		
		String[] filterArray = data._2.toArray(new String[0]);
		// Add the associated columns
		

		for (EntityAttribute attr : searchBE.getBaseEntityAttributes()) {
			if (attr.getAttributeCode().equals("PRI_CODE") && attr.getAttributeName().equals("_EQ_")) {
				// This means we are searching for a single entity				
				BaseEntity be = beUtils.getBaseEntityByCode(attr.getValue());

				if (be != null) {
					log.info("BE found with code " + be.getCode());
					be = VertxUtils.privacyFilter(be, filterArray);
					
					// Get any CAL attributes 
					for (EntityAttribute calEA : cals) {
						String[] calFields = calEA.getAttributeCode().substring("COL__".length()).split("__"); // this separates the indirect lnk field from the end field
						if (calFields.length!=2) {
							continue;
						}
						String attributeCode = calFields[0];
						String calBe = be.getValueAsString(attributeCode);
						String linkBeCode = calEA.getValueString();
						if (!StringUtils.isBlank(calBe)) {
							if (calBe.startsWith("[")) {
								calBe = calBe.substring(2, calBe.length()-2);
							}
							BaseEntity associatedBe = beUtils.getBaseEntityByCode(calBe);
							Optional<EntityAttribute> associateEa = associatedBe.findEntityAttribute(linkBeCode);
							if (associateEa.isPresent()||("PRI_NAME".equals(linkBeCode))) {
								String linkedValue = null;
								if ("PRI_NAME".equals(linkBeCode)) {
									linkedValue = associatedBe.getName();
								} else {
									linkedValue = associatedBe.getValueAsString(linkBeCode);
								}
								try {
									Answer ans = new Answer(be.getCode(),be.getCode(),calEA.getAttributeCode(),linkedValue);
									Attribute att = null;
									if ("PRI_NAME".equals(linkBeCode)) {
										att = RulesUtils.getAttribute("PRI_NAME", serviceToken);
										if (att == null) {
											att = associateEa.get().getAttribute();
										}
									} else {
										att = associateEa.get().getAttribute();
									}
									att.setCode("_"+attributeCode+"__"+linkBeCode);
									ans.setAttribute(att);
									be.addAnswer(ans);
								
								} catch (BadDataException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
					
					msg = new QDataBaseEntityMessage(be);
					msg.setTotal(1L);

				} else {
					log.error("Could not find BE");
					msg = new QDataBaseEntityMessage(new ArrayList<BaseEntity>());
					Long total = 0L;
					msg.setTotal(total);
				}
				msg.setReplace(true);
				msg.setParentCode(searchBE.getCode());

				return msg;
			}
		}

		String hql = data._1;
		log.info("hql = " + hql);

		hql = Base64.getUrlEncoder().encodeToString(hql.getBytes());
		try {
			String resultJsonStr = QwandaUtils.apiGet(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/search24/" + hql
					+ "/" + searchBE.getPageStart(0) + "/" + searchBE.getPageSize(GennySettings.defaultPageSize),
					serviceToken.getToken(), 120);

			endtime2 = System.currentTimeMillis();
			log.info("Time taken to fetch Data =" + (endtime2 - endtime1) + " ms");

			JsonObject resultJson = null;

			try {
				resultJson = new JsonObject(resultJsonStr);
				JsonArray result = resultJson.getJsonArray("codes");
				List<String> resultCodes = new ArrayList<String>();

				BaseEntity[] beArray = new BaseEntity[result.size()];

				for (int i = 0; i < result.size(); i++) {
					String code = result.getString(i);
					resultCodes.add(code);
					BaseEntity be = beUtils.getBaseEntityByCode(code);
					be = VertxUtils.privacyFilter(be, filterArray);

					
					// Get any CAL attributes 
					for (EntityAttribute calEA : cals) {
						String[] calFields = calEA.getAttributeCode().substring("COL__".length()).split("__"); // this separates the indirect lnk field from the end field
						if (calFields.length!=2) {
							continue;
						}
						String attributeCode = calFields[0];
						String calBe = be.getValueAsString(attributeCode);
						String linkBeCode = calEA.getValueString();
						if (!StringUtils.isBlank(calBe)) {
							if (calBe.startsWith("[")) {
								calBe = calBe.substring(2, calBe.length()-2);
							}
							BaseEntity associatedBe = beUtils.getBaseEntityByCode(calBe);
							Optional<EntityAttribute> associateEa = associatedBe.findEntityAttribute(linkBeCode);
							if (associateEa.isPresent()) {
								String linkedValue = associatedBe.getValueAsString(linkBeCode);
								try {
									Answer ans = new Answer(be.getCode(),be.getCode(),calEA.getAttributeCode(),linkedValue);
									Attribute att = associateEa.get().getAttribute();
									att.setCode("_"+attributeCode+"__"+linkBeCode);
									ans.setAttribute(att);
									be.addAnswer(ans);
								
								} catch (BadDataException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
					be.setIndex(i);
					beArray[i] = be;
				}
				
				msg = new QDataBaseEntityMessage(beArray);
				Long total = resultJson.getLong("total");
				msg.setTotal(total);
				msg.setReplace(true);
				msg.setParentCode(searchBE.getCode());
				log.info("Search Results = " + resultCodes.size() + " out of total " + total);

			} catch (Exception e1) {
				log.error("Bad Json -> [" + resultJsonStr);
				msg = new QDataBaseEntityMessage(new ArrayList<BaseEntity>());
				Long total = 0L;
				msg.setTotal(total);
				msg.setReplace(true);
				msg.setParentCode(searchBE.getCode());

			}

		} catch (Exception e1) {
			e1.printStackTrace();
		}
		long endtime3 = System.currentTimeMillis();
		log.info("Time taken to get cached Bes added to list =" + (endtime3 - endtime2) + " ms");

		return msg;
	}

	private static void updateColIndex(SearchEntity searchBE ) {
		Integer index = 1;
		for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {
			if (ea.getAttributeCode().startsWith("COL_")) {
			    index++;
			}
		}
		searchBE.setColIndex(index.doubleValue());
	}

	private static void updateActIndex(SearchEntity searchBE ) {
		Integer index = 1;
		for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {
			if (ea.getAttributeCode().startsWith("ACT_")) {
				index++;
			}
		}
		searchBE.setActionIndex(index.doubleValue());
	}

	public SearchEntity getSessionSearch(final String searchCode) {
		return getSessionSearch(searchCode, null, null);
	}
	
	public SearchEntity getSessionSearch(final String searchCode, final String filterCode, final String filterValue) {

		SearchEntity searchBE = VertxUtils.getObject(beUtils.getGennyToken().getRealm(), "", searchCode, SearchEntity.class,
				beUtils.getGennyToken().getToken());


		return getSessionSearch(searchBE, filterCode, filterValue);
	}

	public SearchEntity getSessionSearch(final SearchEntity searchBE) {
		return getSessionSearch(searchBE, null, null);
	}

	public SearchEntity getSessionSearch(final SearchEntity searchBE, final String filterCode, final String filterValue) {

		if (!searchBE.getCode().contains(beUtils.getGennyToken().getSessionCode().toUpperCase())) {
			/* we need to set the searchBe's code to session Search Code */
			String sessionSearchCode = searchBE.getCode() + "_" + beUtils.getGennyToken().getSessionCode().toUpperCase();
			searchBE.setCode(sessionSearchCode);
		}
		log.info("sessionSearchCode  ::  " + searchBE.getCode());

		for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {
			ea.setBaseEntityCode(searchBE.getCode());
			if (ea.getAttributeCode().startsWith("SBE_")) {
				ea.setAttributeCode(ea.getAttributeCode() + "_" + beUtils.getGennyToken().getSessionCode().toUpperCase());
			}
		}

		/*
		 * Save Session Search in cache , ideally this should be in OutputParam and
		 * saved to workflow
		 */
		if (filterCode != null && filterValue != null) {
			System.out.println("Adding filterCode and filterValue to searchBE");
			searchBE.addFilter(filterCode, SearchEntity.StringFilter.EQUAL, filterValue);
		}

		/*
		 * Save Session Search in cache , ideally this should be in OutputParam and
		 * saved to workflow
		 */
		VertxUtils.putObject(beUtils.getGennyToken().getRealm(), "", searchBE.getCode(), searchBE,
				beUtils.getGennyToken().getToken());
		SearchEntity searchEntity = VertxUtils.getObject(beUtils.getGennyToken().getRealm(), "", searchBE.getCode(), SearchEntity.class,
				beUtils.getGennyToken().getToken());

		return searchEntity;
	}

	private SearchEntity processSearchString(Answer answer, final String searchBarCode, final String filterCode,
			final String filterValue) {

		/* Perform a search bar search */
		String searchBarString = null;
		if (answer != null) {
			searchBarString = answer.getValue();

			// Clean up search Text
			searchBarString = searchBarString.trim();
			searchBarString = searchBarString.replaceAll("[^a-zA-Z0-9\\ ]", "");
			Integer max = searchBarString.length();
			Integer realMax = (max > MAX_SEARCH_BAR_TEXT_SIZE) ? MAX_SEARCH_BAR_TEXT_SIZE : max;
			searchBarString.substring(0, realMax);
			log.info("Search text = [" + searchBarString + "]");
		}

		/* Get the SearchBE */
		SearchEntity searchBE = getSessionSearch(searchBarCode);

		BaseEntity user = VertxUtils.getObject(beUtils.getGennyToken().getRealm(), "",
				beUtils.getGennyToken().getUserCode(), BaseEntity.class, beUtils.getServiceToken().getToken());

		log.info("search code coming from searchBE getCode  :: " + searchBE.getCode());

		/* fetch Session SearchBar List from User */
		Type type = new TypeToken<List<String>>() {
		}.getType();
		List<String> defaultList = new ArrayList<String>();
		String defaultListString = JsonUtils.toJson(defaultList);
		String historyStr = user.getValue("PRI_SEARCH_HISTORY", defaultListString);
		List<String> searchHistory = JsonUtils.fromJson(historyStr, type);

		/* Add new SearchBarString to Session SearchBar List */
		/* look for existing search term and bring to front - slow */
		if (answer != null) { // no need to set history if no data sent
			int index = searchHistory.indexOf(searchBarString);
			if (index >= 0) {
				searchHistory.remove(index);
			}
			searchHistory.add(0, searchBarString);
			if (searchHistory.size() > MAX_SEARCH_HISTORY_SIZE) {
				searchHistory.remove(MAX_SEARCH_HISTORY_SIZE);
			}
			String newHistoryString = JsonUtils.toJson(searchHistory);
			Answer history = new Answer(beUtils.getGennyToken().getUserCode(), beUtils.getGennyToken().getUserCode(),
					"PRI_SEARCH_HISTORY", newHistoryString);
			beUtils.saveAnswer(history);
			log.info("Search History for " + beUtils.getGennyToken().getUserCode() + " = " + searchHistory.toString());
		}
		if (searchBarString != null) {
			searchBE.addFilter("PRI_NAME", SearchEntity.StringFilter.LIKE, "%" + searchBarString + "%");
		}
		if (filterCode != null && filterValue != null) {
			searchBE.addFilter(filterCode, SearchEntity.StringFilter.EQUAL, filterValue);
		}

		/*
		 * Save Session Search in cache , ideally this should be in OutputParam and
		 * saved to workflow
		 */
		VertxUtils.putObject(beUtils.getGennyToken().getRealm(), "", searchBE.getCode(), searchBE,
				beUtils.getGennyToken().getToken());
		searchBE = VertxUtils.getObject(beUtils.getGennyToken().getRealm(), "", searchBE.getCode(), SearchEntity.class,
				beUtils.getGennyToken().getToken());

		return searchBE;
	}

	/*
	 * private QDataAskMessage showTableHeader( SearchEntity searchBE, Map<String,
	 * String> columns, QDataBaseEntityMessage msg) {
	 * 
	 * GennyToken userToken = beUtils.getGennyToken();
	 * 
	 * // Now Send out Table Header Ask and Question TableData tableData =
	 * generateTableAsks(searchBE); Ask headerAsk = tableData.getAsk(); Ask[]
	 * askArray = new Ask[1]; askArray[0] = headerAsk; QDataAskMessage headerAskMsg
	 * = new QDataAskMessage(askArray); headerAskMsg.setToken(userToken.getToken());
	 * headerAskMsg.setReplace(true);
	 * 
	 * // create virtual context
	 * 
	 * // Now link the FRM_TABLE_HEADER to that new Question Set<QDataAskMessage>
	 * askMsgs = new HashSet<QDataAskMessage>();
	 * 
	 * QDataBaseEntityMessage msg2 = changeQuestion(searchBE, "FRM_TABLE_HEADER",
	 * headerAsk, beUtils.getGennyToken(), userToken, askMsgs);
	 * msg2.setToken(userToken.getToken()); msg2.setReplace(true);
	 * 
	 * QDataAskMessage[] askMsgArr = askMsgs.toArray(new QDataAskMessage[0]); if
	 * ((askMsgArr.length > 0) && (askMsgArr[0].getItems().length > 0)) {
	 * ContextList contextList = askMsgArr[0].getItems()[0].getContextList();
	 * headerAskMsg.getItems()[0].setContextList(contextList);
	 * headerAskMsg.getItems()[0].setRealm(userToken.getRealm()); }
	 * 
	 * VertxUtils.writeMsg("webcmds", JsonUtils.toJson(headerAskMsg));
	 * 
	 * VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg2));
	 * 
	 * // Set the table title sendQuestion("QUE_TABLE_TITLE_TEST",
	 * beUtils.getGennyToken().getUserCode(), searchBE.getCode(), "SCH_TITLE",
	 * beUtils.getGennyToken());
	 * 
	 * return headerAskMsg; }
	 */

	/**
	 * @param serviceToken
	 * @param searchBE
	 * @param msg
	 * @param columns
	 */
	private QBulkMessage showTableContent(GennyToken serviceToken, SearchEntity searchBE, QDataBaseEntityMessage msg,
			Map<String, String> columns) {
		return showTableContent(serviceToken, searchBE, msg, columns, false);
	}

	private QBulkMessage showTableContent(GennyToken serviceToken, SearchEntity searchBE, QDataBaseEntityMessage msg,
			Map<String, String> columns, Boolean cache) {
		QBulkMessage ret = new QBulkMessage();
		log.info("inside showTableContent");

		/* get the baseentity results */
		List<BaseEntity> rowList = Arrays.asList(msg.getItems());

		TableData tableData = generateTableAsks(searchBE);

		Ask headerAsk = getHeaderAsk(searchBE);

		List<Ask> rowAsks = new ArrayList<Ask>();
		rowAsks.add(headerAsk);
		rowAsks.addAll(generateQuestions(rowList, columns, beUtils.getGennyToken().getUserCode()));

		/* converting rowAsks list to array */
		Ask[] rowAsksArr = rowAsks.stream().toArray(Ask[]::new);

		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP_TABLE_RESULTS", "link", new DataType(String.class));
		Question tableResultQuestion = new Question("QUE_TABLE_RESULTS_GRP", "Table Results Question Group",
				questionAttribute, true);

		Ask tableResultAsk = new Ask(tableResultQuestion, beUtils.getGennyToken().getUserCode(),
				beUtils.getGennyToken().getUserCode());
		tableResultAsk.setChildAsks(rowAsksArr);
		tableResultAsk.setReadonly(true);
		tableResultAsk.setRealm(beUtils.getGennyToken().getRealm());

		Set<QDataAskMessage> tableResultAskMsgs = new HashSet<QDataAskMessage>();
		tableResultAskMsgs.add(new QDataAskMessage(tableResultAsk));

		/* send the results questionGroup */
		log.info("*************** Sending the QUE_TABLE_RESULTS_GRP askMsg ***************");
		QDataAskMessage askMsg = new QDataAskMessage(tableResultAsk);
		askMsg.setReplace(true);

		if (cache) {
			/* Add table row ask msg */
			ret.add(askMsg);
		} else {
			askMsg.setToken(beUtils.getGennyToken().getToken());
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson(askMsg));
		}

		log.info("*************** Sending table title question ***************");
		QDataAskMessage qAskMsg = sendQuestion("QUE_TABLE_TITLE_TEST", beUtils.getGennyToken().getUserCode(),
				searchBE.getCode(), "SCH_TITLE", beUtils.getGennyToken(), cache);
		if (cache) {
			/* Add the title askMsg */
			ret.add(qAskMsg);
		}
		return ret;
	}

	// /**
	// * @param searchBE
	// */
	// private void showTableFooter(SearchEntity searchBE) {
	// /* need to send the footer question again here */
	// Attribute totalAttribute = new Attribute("PRI_TOTAL_RESULTS", "link", new
	// DataType(String.class));
	// Attribute indexAttribute = new Attribute("PRI_INDEX", "link", new
	// DataType(String.class));
	//
	// /* create total count ask */
	// Question totalQuestion = new Question("QUE_TABLE_TOTAL_RESULT_COUNT", "Total
	// Results", totalAttribute, true);
	//
	// Ask totalAsk = new Ask(totalQuestion, beUtils.getGennyToken().getUserCode(),
	// searchBE.getCode());
	// totalAsk.setReadonly(true);
	// totalAsk.setRealm(beUtils.getGennyToken().getRealm());
	// /* create index ask */
	// Question indexQuestion = new Question("QUE_TABLE_PAGE_INDEX", "Page Number",
	// indexAttribute, true);
	//
	// Ask indexAsk = new Ask(indexQuestion, beUtils.getGennyToken().getUserCode(),
	// searchBE.getCode());
	// indexAsk.setReadonly(true);
	// indexAsk.setRealm(beUtils.getGennyToken().getRealm());
	//
	// /* collect the asks to be sent out */
	// Set<QDataAskMessage> footerAskMsgs = new HashSet<QDataAskMessage>();
	// footerAskMsgs.add(new QDataAskMessage(totalAsk));
	// footerAskMsgs.add(new QDataAskMessage(indexAsk));
	//
	// /* publish the new asks with searchBe set as targetCode */
	// for (QDataAskMessage footerAskMsg : footerAskMsgs) {
	// footerAskMsg.setToken(beUtils.getGennyToken().getToken());
	// footerAskMsg.setReplace(false);
	// VertxUtils.writeMsg("webcmds", JsonUtils.toJson(footerAskMsg));
	// }
	// }

	public TableData generateTableAsks(SearchEntity searchBe) {

		List<QDataBaseEntityMessage> themeMsgList = new ArrayList<QDataBaseEntityMessage>();

		Ask tableHeaderAsk = generateTableHeaderAsk(searchBe, themeMsgList);

		log.info("*** ThemeMsgList *****");
		log.info(themeMsgList);

		TableData tableData = new TableData(themeMsgList, tableHeaderAsk);
		return tableData;
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
			if (attributeCode.startsWith("COL_")) {
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

	public QDataBaseEntityMessage fetchSearchResults(SearchEntity searchBE) {

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(new ArrayList<BaseEntity>());
		msg.setReplace(true);
		if (beUtils.getGennyToken() == null) {
			log.error("GENNY TOKEN IS NULL!!! in getSearchResults");
			return msg;
		}
		searchBE.setRealm(beUtils.getGennyToken().getRealm());
		log.debug("The search BE is :: " + JsonUtils.toJson(searchBE));

		if (VertxUtils.cachedEnabled) {
			List<BaseEntity> results = new ArrayList<BaseEntity>();
			Integer pageStart = searchBE.getValue("SCH_PAGE_START", 0);
			Integer pageSize = searchBE.getValue("SCH_PAGE_SIZE", 10);

			List<BaseEntity> tests = new ArrayList<>();

			tests.add(createTestCompany("Melbourne University", "0398745321", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Monash University", "0398744421", "support@melbuni.edu.au", "CLAYTON", "Victoria",
					"3142"));
			tests.add(createTestCompany("Latrobe University", "0398733321", "support@melbuni.edu.au", "MELBOURNE", "Victoria",
					"3001"));
			tests.add(createTestCompany("University Of Warracknabeal", "0392225321", "support@melbuni.edu.au",
					"WARRACKNABEAL", "Victoria", "3993"));
			tests.add(createTestCompany("Ashburton University", "0398741111", "support@melbuni.edu.au", "ASHBURTON",
					"Victoria", "3147"));
			tests.add(createTestCompany("Outcome Academy", "0398745777", "support@melbuni.edu.au", "MELBOURNE", "Victoria",
					"3001"));
			tests.add(createTestCompany("Holland University", "0298555521", "support@melbuni.edu.au", "MELBOURNE", "Victoria",
					"3001"));
			tests.add(createTestCompany("University of Greenvale", "0899995321", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Crow University", "0398749999", "support@melbuni.edu.au", "MELBOURNE", "Victoria",
					"3001"));
			tests.add(createTestCompany("RMIT University", "0398748787", "support@melbuni.edu.au", "MELBOURNE", "Victoria",
					"3001"));
			tests.add(createTestCompany("Mt Buller University", "0398836421", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Australian National University", "0198876541", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Dodgy University", "0390000001", "support@melbuni.edu.au", "MELBOURNE", "Victoria",
					"3001"));
			tests.add(createTestCompany("Australian Catholic University", "0398711121", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Australian Jedi University", "0798788881", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Brisbane Lions University", "0401020319", "support@melbuni.edu.au", "BRISBANE",
					"Queensland", "4000"));
			tests.add(
					createTestCompany("AFL University", "0390000001", "support@melbuni.edu.au", "MELBOURNE", "Victoria", "3001"));
			tests.add(createTestCompany("Uluru University", "0398711441", "support@melbuni.edu.au", "MELBOURNE", "Victoria",
					"3001"));
			tests.add(createTestCompany("University Of Hard Knocks", "0798744881", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Scam University", "0705020319", "support@melbuni.edu.au", "MELBOURNE", "Victoria",
					"3001"));

			for (Integer pageIndex = pageStart; pageIndex < (pageStart + pageSize); pageIndex++) {
				if (pageIndex < tests.size()) {
					results.add(tests.get(pageIndex));
				}
			}

			msg = new QDataBaseEntityMessage(results);
			return msg;
		}

		String jsonSearchBE = JsonUtils.toJson(searchBE);
		String resultJson;
		try {
			// GennyToken gToken = beUtils.getGennyToken();
			// if (beUtils.getGennyToken().hasRole("admin")) {
			// gToken = beUtils.getServiceToken();
			// }
			// if (gToken == null) {
			// log.error
			// }
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
						msg = new QDataBaseEntityMessage(items, searchBE.getCode(), linkCode, total);
						log.info(msg);
						log.info("The result of getSearchResults was " + msg.getItems().length + " items , with total="
								+ msg.getTotal());
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
		msg.setToken(beUtils.getGennyToken().getToken());
		msg.setReplace(true);
		/* get the total count of the results */
		long totalResults = msg.getTotal();

		int pageNumber = 1;

		Answer totalAnswer = new Answer(beUtils.getGennyToken().getUserCode(), searchBE.getCode(), "PRI_TOTAL_RESULTS",
				totalResults + "");

		Answer pageNumberAnswer = new Answer(beUtils.getGennyToken().getUserCode(), searchBE.getCode(), "PRI_INDEX",
				pageNumber + "");
		Answer totalAnswer2 = new Answer(beUtils.getGennyToken().getUserCode(), searchBE.getCode(), "SCH_TOTAL",
				totalResults + "");

		Answer pageNumberAnswer2 = new Answer(beUtils.getGennyToken().getUserCode(), searchBE.getCode(), "SCH_INDEX",
				pageNumber + "");

		beUtils.addAnswer(totalAnswer);
		beUtils.addAnswer(pageNumberAnswer);

		beUtils.updateBaseEntity(searchBE, totalAnswer);
		beUtils.updateBaseEntity(searchBE, pageNumberAnswer);

		beUtils.addAnswer(totalAnswer2);
		beUtils.addAnswer(pageNumberAnswer2);

		beUtils.updateBaseEntity(searchBE, totalAnswer2);
		beUtils.updateBaseEntity(searchBE, pageNumberAnswer2);

		log.info("Search Results for " + searchBE.getCode() + " and user " + beUtils.getGennyToken().getUserCode()); // use
		// QUE_TABLE_VIEW_TEST
		log.info("Search result items = " + msg.getReturnCount());
		if (msg.getReturnCount() > 0) {
			BaseEntity result0 = msg.getItems()[0];
			log.info("Search first result = " + result0);
			if (msg.getReturnCount() > 1) {
				BaseEntity result1 = msg.getItems()[1];
				log.info("Search second result = " + result1);
			}
		}

		return msg;

	}

	public BaseEntity createTestPerson(String name, String email) {
		String usercode = "PER_" + QwandaUtils.getNormalisedUsername(email);
		BaseEntity result1 = new BaseEntity(usercode, name);
		result1.setRealm(beUtils.getGennyToken().getRealm());

		return result1;
	}

	public BaseEntity createTestCompany(String name, String phone, String email, String city, String state,
			String postcode) {
		String usercode = "CPY_" + UUID.randomUUID().toString().substring(0, 15).toUpperCase().replaceAll("-", "");

		BaseEntity result1 = new BaseEntity(usercode, name);
		result1.setRealm(beUtils.getGennyToken().getRealm());
		try {
			result1.addAnswer(new Answer(result1, result1, attribute("PRI_EMAIL", beUtils.getGennyToken()), email));
			result1.addAnswer(new Answer(result1, result1, attribute("PRI_ADDRESS_STATE", beUtils.getGennyToken()), state));
			result1.addAnswer(new Answer(result1, result1, attribute("PRI_ADDRESS_CITY", beUtils.getGennyToken()), city));
			result1.addAnswer(
					new Answer(result1, result1, attribute("PRI_ADDRESS_POSTCODE", beUtils.getGennyToken()), postcode));
			result1.addAnswer(new Answer(result1, result1, attribute("PRI_LANDLINE", beUtils.getGennyToken()), phone));
		} catch (BadDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result1;
	}

	public Attribute attribute(final String attributeCode, GennyToken gToken) {
		Attribute attribute = RulesUtils.getAttribute(attributeCode, gToken.getToken());
		return attribute;
	}

	public Ask generateTableHeaderAsk(SearchEntity searchBe, List<QDataBaseEntityMessage> themeMsgList) {

		List<Ask> asks = new ArrayList<>();

		/* Validation for Search Attribute */
		Validation validation = new Validation("VLD_NON_EMPTY", "EmptyandBlankValues", "(?!^$|\\s+)");
		List<Validation> validations = new ArrayList<>();
		validations.add(validation);
		ValidationList searchValidationList = new ValidationList();
		searchValidationList.setValidationList(validations);

		Attribute eventAttribute = RulesUtils.attributeMap.get("PRI_SORT");
		Attribute questionAttribute = RulesUtils.attributeMap.get("QQQ_QUESTION_GROUP");
		Attribute tableCellAttribute = RulesUtils.attributeMap.get("QQQ_QUESTION_GROUP_TABLE_CELL");
		Attribute priEvent = RulesUtils.attributeMap.get("PRI_TEXT");

		/* get table columns */
		Map<String, String> columns = getTableColumns(searchBe);

		for (Map.Entry<String, String> column : columns.entrySet()) {

			String attributeCode = column.getKey();
			String attributeName = column.getValue();

			Attribute searchAttribute = new Attribute(attributeCode, attributeName,
					new DataType("Text", searchValidationList, "Text"));

			/* Initialize Column Header Ask group */
			Question columnHeaderQuestion = new Question("QUE_" + attributeCode, attributeName, priEvent, true);
			Ask columnHeaderAsk = new Ask(columnHeaderQuestion, beUtils.getGennyToken().getUserCode(), searchBe.getCode());

			/* creating ask for table header label-sort */
			/*
			 * Ask columnSortAsk = getAskForTableHeaderSort(searchBe, attributeCode,
			 * attributeName, eventAttribute, themeMsgList);
			 */

			/* creating Ask for table header search input */
			/*
			 * Question columnSearchQues = new Question("QUE_SEARCH_" + attributeCode,
			 * "Search " + attributeName + "..", searchAttribute, false); Ask
			 * columnSearchAsk = new Ask(columnSearchQues,
			 * beUtils.getGennyToken().getUserCode(), searchBe.getCode());
			 */

			/* adding label-sort & search asks to header-ask Group */
			// List<Ask> tableColumnChildAsks = new ArrayList<>();
			/* tableColumnChildAsks.add(columnSortAsk); */
			/* tableColumnChildAsks.add(columnSearchAsk); */
			/* Convert List to Array */
			// Ask[] tableColumnChildAsksArray = tableColumnChildAsks.toArray(new Ask[0]);

			/* set the child asks */
			// columnHeaderAsk.setChildAsks(tableColumnChildAsksArray);

			asks.add(columnHeaderAsk);
		}

		/* Convert List to Array */
		Ask[] asksArray = asks.toArray(new Ask[0]);

		/*
		 * we create a table-header ask grp and set all the column asks as it's childAsk
		 */
		Question tableHeaderQuestion = new Question("QUE_TABLE_HEADER_GRP", searchBe.getName(), questionAttribute, true);

		Ask tableHeaderAsk = new Ask(tableHeaderQuestion, beUtils.getGennyToken().getUserCode(), searchBe.getCode());
		tableHeaderAsk.setChildAsks(asksArray);
		tableHeaderAsk.setName(searchBe.getName());

		return tableHeaderAsk;
	}

	public Ask createVirtualContext(Ask ask, BaseEntity theme, ContextType linkCode,
			List<QDataBaseEntityMessage> themeMsgList) {
		List<BaseEntity> themeList = new ArrayList<>();
		themeList.add(theme);
		return createVirtualContext(ask, themeList, linkCode, VisualControlType.VCL_INPUT, themeMsgList);
	}

	public Ask createVirtualContext(Ask ask, List<BaseEntity> themeList, ContextType linkCode,
			List<QDataBaseEntityMessage> themeMsgList) {
		return createVirtualContext(ask, themeList, linkCode, VisualControlType.VCL_INPUT, themeMsgList);
	}

	public Ask createVirtualContext(Ask ask, BaseEntity theme, ContextType linkCode, VisualControlType visualControlType,
			List<QDataBaseEntityMessage> themeMsgList) {
		List<BaseEntity> themeList = new ArrayList<>();
		themeList.add(theme);
		return createVirtualContext(ask, themeList, linkCode, visualControlType, themeMsgList);
	}

	public Ask createVirtualContext(Ask ask, BaseEntity theme, ContextType linkCode, VisualControlType visualControlType,
			Double weight, List<QDataBaseEntityMessage> themeMsgList) {
		List<BaseEntity> themeList = new ArrayList<>();
		themeList.add(theme);
		return createVirtualContext(ask, themeList, linkCode, visualControlType, weight, themeMsgList);
	}

	public Ask createVirtualContext(Ask ask, List<BaseEntity> themes, ContextType linkCode,
			VisualControlType visualControlType, List<QDataBaseEntityMessage> themeMsgList) {
		return createVirtualContext(ask, themes, linkCode, visualControlType, 2.0, themeMsgList);
	}

	/**
	 * Embeds the list of contexts (themes, icon) into an ask and also publishes the
	 * themes
	 *
	 * @param ask
	 * @param themes
	 * @param linkCode
	 * @param weight
	 * @return
	 */
	public Ask createVirtualContext(Ask ask, List<BaseEntity> themes, ContextType linkCode,
			VisualControlType visualControlType, Double weight, List<QDataBaseEntityMessage> themeMsgList) {

		List<Context> completeContext = new ArrayList<>();

		for (BaseEntity theme : themes) {
			Context context = new Context(linkCode, theme, visualControlType, weight);
			completeContext.add(context);

			/* publish the theme baseentity message */
			QDataBaseEntityMessage themeMsg = new QDataBaseEntityMessage(theme);
			themeMsgList.add(themeMsg);
		}

		ContextList contextList = ask.getContextList();
		if (contextList != null) {
			List<Context> contexts = contextList.getContexts();
			if (contexts.isEmpty()) {
				contexts = new ArrayList<>();
				contexts.addAll(completeContext);
			} else {
				contexts.addAll(completeContext);
			}
			contextList = new ContextList(contexts);
		} else {
			List<Context> contexts = new ArrayList<>();
			contexts.addAll(completeContext);
			contextList = new ContextList(contexts);
		}
		ask.setContextList(contextList);
		return ask;
	}

	/**
	 * @param serviceToken
	 * @return
	 */
	public QDataBaseEntityMessage changeQuestion(SearchEntity searchBE, final String frameCode, final Ask ask,
			GennyToken serviceToken, GennyToken userToken, Set<QDataAskMessage> askMsgs) {
		Frame3 frame = null;
		Attribute priEvent = RulesUtils.attributeMap.get("PRI_EVENT");

		try {

			// if (ask.getQuestionCode().equals("QUE_TABLE_RESULTS_GRP")) {

			log.info("getting the FRM_TABLE_CONTENT from cache");

			frame = VertxUtils.getObject(serviceToken.getRealm(), "", frameCode, Frame3.class, serviceToken.getToken());

			// Validation tableRowValidation = new Validation("VLD_ANYTHING", "Anything",
			// ".*");

			// List<Validation> tableRowValidations = new ArrayList<>();
			// tableRowValidations.add(tableRowValidation);

			// ValidationList tableRowValidationList = new ValidationList();
			// tableRowValidationList.setValidationList(tableRowValidations);

			// DataType tableRowDataType = new DataType("DTT_TABLE_ROW_GRP",
			// tableRowValidationList, "Table Row Group", "");

			// frame = Frame3.builder(ask.getQuestionCode()).addTheme("THM_TABLE_BORDER",
			// serviceToken).end()
			// .addTheme("THM_TABLE_CONTENT_CENTRE", ThemePosition.CENTRE,
			// serviceToken).end()
			// .question(ask.getQuestionCode())
			// .addTheme("THM_DISPLAY_HORIZONTAL", serviceToken)
			// //.dataType(tableRowDataType)
			// .weight(1.0).end().addTheme("THM_TABLE_ROW_CONTENT_WRAPPER", serviceToken)
			// //.dataType(tableRowDataType)
			// .vcl(VisualControlType.GROUP).weight(1.0).end().addTheme("THM_TABLE_ROW",
			// serviceToken)
			// //.dataType(tableRowDataType)
			// .weight(1.0).end()
			// .addTheme("THM_TABLE_CONTENT_BORDER", serviceToken)
			// //.dataType(tableRowDataType)
			// .vcl(VisualControlType.GROUP_WRAPPER).weight(1.0).end()
			// .addTheme("THM_TABLE_CONTENT", serviceToken)
			// .vcl(VisualControlType.GROUP).end().addTheme("THM_TABLE_ROW_CELL",
			// serviceToken)
			// .vcl(VisualControlType.VCL_WRAPPER).end().end().build();

			// }
			// if (ask.getQuestionCode().equals("FRM_TABLE_CONTENT")) {

			// Validation tableRowValidation = new Validation("VLD_ANYTHING", "Anything",
			// ".*");

			// List<Validation> tableRowValidations = new ArrayList<>();
			// tableRowValidations.add(tableRowValidation);

			// ValidationList tableRowValidationList = new ValidationList();
			// tableRowValidationList.setValidationList(tableRowValidations);

			// DataType tableRowDataType = new DataType("DTT_TABLE_ROW_GRP",
			// tableRowValidationList, "Table Row Group", "");

			// frame = Frame3.builder(ask.getQuestionCode()).addTheme("THM_TABLE_BORDER",
			// serviceToken).end()
			// .addTheme("THM_TABLE_CONTENT_CENTRE", ThemePosition.CENTRE,
			// serviceToken).end()
			// .question(ask.getQuestionCode())
			// .addTheme("THM_DISPLAY_HORIZONTAL", serviceToken)
			// //.dataType(tableRowDataType)
			// .weight(1.0).end().addTheme("THM_TABLE_ROW_CONTENT_WRAPPER", serviceToken)
			// //.dataType(tableRowDataType)
			// .vcl(VisualControlType.GROUP).weight(1.0).end().addTheme("THM_TABLE_ROW",
			// serviceToken)
			// //.dataType(tableRowDataType)
			// .weight(1.0).end()
			// .addTheme("THM_TABLE_CONTENT_BORDER", serviceToken)
			// //.dataType(tableRowDataType)
			// .vcl(VisualControlType.GROUP_WRAPPER).weight(1.0).end()
			// .addTheme("THM_TABLE_CONTENT", serviceToken)
			// .vcl(VisualControlType.GROUP).end().addTheme("THM_TABLE_ROW_CELL",
			// serviceToken)
			// .vcl(VisualControlType.VCL_WRAPPER).end().end().build();

			// }

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		QDataBaseEntityMessage msg = FrameUtils2.toMessage(frame, serviceToken, askMsgs, true);
		msg.setReplace(true);

		msg.setToken(userToken.getToken());

		for (BaseEntity targetFrame : msg.getItems()) {
			if (targetFrame.getCode().equals(ask.getQuestionCode())) {

				log.info("ShowFrame : Found Targeted Frame BaseEntity : " + targetFrame);

				/* Adding the links in the targeted BaseEntity */
				Attribute attribute = new Attribute("LNK_ASK", "LNK_ASK", new DataType(String.class));

				for (BaseEntity sourceFrame : msg.getItems()) {
					if (sourceFrame.getCode().equals(ask.getQuestionCode())) {

						log.info("ShowFrame : Found Source Frame BaseEntity : " + sourceFrame);
						EntityEntity entityEntity = new EntityEntity(targetFrame, sourceFrame, attribute, 1.0, "CENTRE");
						Set<EntityEntity> entEntList = targetFrame.getLinks();
						entEntList.add(entityEntity);
						targetFrame.setLinks(entEntList);

						/* Adding Frame to Targeted Frame BaseEntity Message */
						msg.add(targetFrame);
						msg.setReplace(true);

						break;
					}
				}
				break;
			}
		}

		return msg;
	}

	/*
	 * Generate List of asks from a SearchEntity
	 */
	public List<Ask> generateQuestions(List<BaseEntity> bes, Map<String, String> columns, String targetCode) {

		/* initialize an empty ask list */
		List<Ask> askList = new ArrayList<>();
		TableUtils tableUtils = new TableUtils(beUtils);

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
							Question childQuestion = new Question("QUE_" + attributeCode + "_" + be.getCode(), attributeName, attr,
									true);
							Ask childAsk = new Ask(childQuestion, targetCode, be.getCode());
							childAsk.setReadonly(true);
							childAskList.add(childAsk);
						} else {
							log.error("Attribute : " + attributeCode + " DOES NOT EXIST IN AttributeMap");
						}
					}

					/* converting childAsks list to array */
					Ask[] childAsArr = childAskList.stream().toArray(Ask[]::new);

					// Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new
					// DataType(String.class));

					// Attribute questionTableRowAttribute = new
					// Attribute("QQQ_QUESTION_GROUP_TABLE_ROW", "link",
					// new DataType(String.class));

					/* Get the attribute */
					Attribute tableRowAttribute = RulesUtils.attributeMap.get("QQQ_QUESTION_GROUP_TABLE_ROW");

					/* Generate ask for the baseentity */
					Question parentQuestion = new Question("QUE_" + be.getCode() + "_GRP", be.getName(), tableRowAttribute, true);
					Ask parentAsk = new Ask(parentQuestion, targetCode, be.getCode());

					/* set readOnly to true */
					parentAsk.setReadonly(true);

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

	public QDataAskMessage sendQuestion(String titleQuestionCode, String sourceCode, String targetCode,
			String attributeCode, GennyToken userToken) {
		return sendQuestion(titleQuestionCode, sourceCode, targetCode, attributeCode, userToken, false);
	}

	public QDataAskMessage sendQuestion(String titleQuestionCode, String sourceCode, String targetCode,
			String attributeCode, GennyToken userToken, Boolean cache) {
		QDataAskMessage ret = null;
		// Set the table title
		Attribute nameAttribute = RulesUtils.getAttribute(attributeCode, userToken.getToken());
		Question titleQuestion = new Question(titleQuestionCode, titleQuestionCode, nameAttribute, true);

		Ask titleAsk = new Ask(titleQuestion, sourceCode, targetCode);
		titleAsk.setRealm(userToken.getRealm());
		titleAsk.setReadonly(true);
		Ask[] askArray1 = new Ask[1];
		askArray1[0] = titleAsk;
		QDataAskMessage titleAskMsg = new QDataAskMessage(askArray1);
		titleAskMsg.setReplace(false);
		log.info("Inside sendQuestion method");
		if (cache) {
			ret = titleAskMsg;
		} else {
			titleAskMsg.setToken(userToken.getToken());
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson(titleAskMsg));
		}
		return ret;
	}

	private void updateBaseEntity(BaseEntity be, String attributeCode, String value) {
		Attribute attribute = RulesUtils.getAttribute(attributeCode, beUtils.getGennyToken().getToken());
		try {
			be.addAnswer(new Answer(be, be, attribute, value));
			VertxUtils.putObject(beUtils.getGennyToken().getRealm(), "", be.getCode(), be,
					beUtils.getGennyToken().getToken());

		} catch (BadDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Ask getHeaderAsk(SearchEntity searchBe) {

		/* get table columns */
		Map<String, String> columns = getTableColumns(searchBe);
		List<Ask> asks = new ArrayList<Ask>();

		/* get the required attributes */
		Attribute nameAttr = RulesUtils.attributeMap.get("PRI_TEXT_HEADER");
		Attribute tableHeaderAttribute = RulesUtils.attributeMap.get("QQQ_QUESTION_GROUP_TABLE_HEADER");

		for (Map.Entry<String, String> column : columns.entrySet()) {

			String attributeCode = column.getKey();
			String attributeName = column.getValue();

			// Attribute headerAttr;
			// headerAttr = RulesUtils.attributeMap.get(attributeCode + "_HEADER");
			// if (headerAttr == null) {
			// log.info("Header attribute is null");
			// log.info(attributeCode + "_HEADER is null");
			// headerAttr = nameAttr;
			// }

			/* Initialize Column Header Ask group */
			Question headerQues = new Question("QUE_" + attributeCode, attributeName, nameAttr, true);
			Ask headerAsk = new Ask(headerQues, beUtils.getGennyToken().getUserCode(), searchBe.getCode());
			asks.add(headerAsk);

			// /* Initialize Column Header Ask group */
			// Question ques = new Question("QUE_" + attributeCode, attributeName, nameAttr,
			// true);
			// Ask ask = new Ask(ques, beUtils.getGennyToken().getUserCode(),
			// searchBe.getCode());
			// asks.add(ask);
		}

		Ask[] childAsksArr = asks.toArray(new Ask[0]);

		/* generate header ask */
		Question headerAskQues = new Question("QUE_TABLE_GRP", searchBe.getName(), tableHeaderAttribute, true);
		Ask headerAsk = new Ask(headerAskQues, beUtils.getGennyToken().getUserCode(), searchBe.getCode());

		headerAsk.setChildAsks(childAsksArr);
		headerAsk.setName(searchBe.getName());

		return headerAsk;
	}

	public void awaitTerminationAfterShutdown(ExecutorService threadPool) {
		threadPool.shutdown();
		try {
			if (!threadPool.awaitTermination(90, TimeUnit.SECONDS)) {
				threadPool.shutdownNow();
			}
		} catch (InterruptedException ex) {
			threadPool.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	static public long searchTable(BaseEntityUtils beUtils, String code, Boolean cache) {
		return searchTable(beUtils, code, cache, null, null, true);
	}

	static public long searchTable(BaseEntityUtils beUtils, String code, Boolean cache, Boolean replace) {
		return searchTable(beUtils, code, cache, null, null, replace);
	}

	static public long searchTable(BaseEntityUtils beUtils, String code, Boolean cache, String filterCode,
			String filterValue) {
		return searchTable(beUtils, code, cache, filterCode, filterValue, true);
	}

	static public long searchTable(BaseEntityUtils beUtils, String code, Boolean cache, String filterCode,
			String filterValue, Boolean replace) {

		String searchBeCode = code;
		if (searchBeCode.startsWith("CNS_")) {
			searchBeCode = searchBeCode.substring(4);
		} else if (!searchBeCode.startsWith("SBE_")) {
			searchBeCode = "SBE_" + searchBeCode;
		}
		System.out.println("SBE CODE   ::   " + searchBeCode);

		SearchEntity searchBE = VertxUtils.getObject(beUtils.getGennyToken().getRealm(), "", searchBeCode, SearchEntity.class,
		beUtils.getGennyToken().getToken());
		
		if (searchBE != null) {
				
			if (code.startsWith("CNS_")) {
				searchBE.setCode(code);
			}

			return searchTable(beUtils, searchBE, cache, filterCode, filterValue, replace);
		} else {
			System.out.println("Could not fetch " + searchBeCode + " from cache!!!");
			return -1L;
		}
	}

	static public long searchTable(BaseEntityUtils beUtils, SearchEntity searchBE, Boolean cache) {
		return searchTable(beUtils, searchBE, cache, null, null, true);
	}

	static public long searchTable(BaseEntityUtils beUtils, SearchEntity searchBE, Boolean cache, Boolean replace) {
		return searchTable(beUtils, searchBE, cache, null, null, replace);
	}

	static public long searchTable(BaseEntityUtils beUtils, SearchEntity searchBE, Boolean cache, String filterCode,
			String filterValue, Boolean replace) {

		TableUtils tableUtils = new TableUtils(beUtils);

		if (searchBE.getCode().startsWith("CNS_")) {
			// Remove CNS_ prefix
			searchBE.setCode(searchBE.getCode().substring(4));
			// Perform Count
			return tableUtils.performAndSendCount(searchBE);
		} else {
	
			try {
				searchBE = tableUtils.getSessionSearch(searchBE, filterCode, filterValue);
			} catch (Exception e1) {
				return -1L;
			}
			
			long starttime = System.currentTimeMillis();
			
			if (searchBE == null) {
				System.out.println("SearchBE is null");
				return -1L;
			}
			
			/* get current search */
			long s2time = System.currentTimeMillis();
		/* 	if (replace) { // user has clicked on fresh search
			Answer pageAnswer = new Answer(beUtils.getGennyToken().getUserCode(), searchBE.getCode(), "SCH_PAGE_START", "0");
			Answer pageNumberAnswer = new Answer(beUtils.getGennyToken().getUserCode(), searchBE.getCode(), "PRI_INDEX", "1");
			
			searchBE = beUtils.updateBaseEntity(searchBE, pageAnswer, SearchEntity.class);
			searchBE = beUtils.updateBaseEntity(searchBE, pageNumberAnswer, SearchEntity.class);
			
			VertxUtils.putObject(beUtils.getGennyToken().getRealm(), "", searchBE.getCode(), searchBE,
			beUtils.getGennyToken().getToken());
			} */
			
			VertxUtils.putObject(beUtils.getGennyToken().getRealm(), "LAST-SEARCH", beUtils.getGennyToken().getSessionCode(),
			searchBE, beUtils.getGennyToken().getToken());

			updateActIndex(searchBE);
			updateColIndex(searchBE);

			long s3time = System.currentTimeMillis();
			
			ExecutorService WORKER_THREAD_POOL = Executors.newFixedThreadPool(10);
			CompletionService<QBulkMessage> service = new ExecutorCompletionService<>(WORKER_THREAD_POOL);
			
			// TableFrameCallable tfc = new TableFrameCallable(beUtils, cache);
			SearchCallable sc = new SearchCallable(tableUtils, searchBE, beUtils, cache, filterCode, filterValue, replace);

			List<Callable<QBulkMessage>> callables = Arrays.asList(sc);

			QBulkMessage aggregatedMessages = new QBulkMessage();

			long startProcessingTime = System.currentTimeMillis();
			long totalProcessingTime;

			if (GennySettings.useConcurrencyMsgs) {
				System.out.println("useConcurrencyMsgs is enabled");

				for (Callable<QBulkMessage> callable : callables) {
					service.submit(callable);
				}
				try {
					Future<QBulkMessage> future = service.take();
					QBulkMessage firstThreadResponse = future.get();
					aggregatedMessages.add(firstThreadResponse);
					totalProcessingTime = System.currentTimeMillis() - startProcessingTime;

					/*
					 * assertTrue("First response should be from the fast thread",
					 * "fast thread".equals(firstThreadResponse.getData_type()));
					 * assertTrue(totalProcessingTime >= 100 && totalProcessingTime < 1000);
					 */
					System.out.println("Thread finished after: " + totalProcessingTime + " milliseconds");

					future = service.take();
					QBulkMessage secondThreadResponse = future.get();
					aggregatedMessages.add(secondThreadResponse);
					System.out.println("2nd Thread finished after: " + totalProcessingTime + " milliseconds");
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}

				WORKER_THREAD_POOL.shutdown();
				try {
					if (!WORKER_THREAD_POOL.awaitTermination(90, TimeUnit.SECONDS)) {
						WORKER_THREAD_POOL.shutdownNow();
					}
				} catch (InterruptedException ex) {
					WORKER_THREAD_POOL.shutdownNow();
					Thread.currentThread().interrupt();
				}
			} else {
				// aggregatedMessages.add(tfc.call());
				aggregatedMessages.add(sc.call());

			}
			totalProcessingTime = System.currentTimeMillis() - startProcessingTime;
			System.out.println("All threads finished after: " + totalProcessingTime + " milliseconds");
			aggregatedMessages.setToken(beUtils.getGennyToken().getToken());

			if (cache) {
				String json = JsonUtils.toJson(aggregatedMessages);
				VertxUtils.writeMsg("webcmds", json);
				// Now send the end_process msg

			}
			
			QCmdMessage msgend = new QCmdMessage("END_PROCESS", "END_PROCESS");
			msgend.setToken(beUtils.getGennyToken().getToken());
			msgend.setSend(true);
			VertxUtils.writeMsg("webcmds", msgend);


			/* update(output); */
			long endtime = System.currentTimeMillis();
			// System.out.println("init setup took " + (s1time - starttime) + " ms");
			// System.out.println("search session setup took " + (s2time - s1time) + " ms");
			System.out.println("update searchBE BE setup took " + (s3time - s2time) + " ms");
			return (endtime - starttime);
		}
	}

	static public Tuple2<String, List<String>> getHql(GennyToken serviceToken, SearchEntity searchBE) {
		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);
		return beUtils.getHql(searchBE);
	}

	public long performAndSendCount(String searchCode) {

		log.info("Fetching SearchBE " + searchCode + " from cache");

		SearchEntity searchBE = VertxUtils.getObject(this.beUtils.getGennyToken().getRealm(), "", searchCode, SearchEntity.class,
		beUtils.getGennyToken().getToken());

		return performAndSendCount(searchBE);
	}

	public long performAndSendCount(SearchEntity searchBE) {

		System.out.println("SBE CODE   ::   " + searchBE.getCode());
		long startTime = System.currentTimeMillis();
		// Add the sessionCode to the SBE code
		searchBE = this.getSessionSearch(searchBE, null, null);

		Long total = performCount(searchBE);

		// Add PRI_TOTAL_RESULTS to SBE too
		updateBaseEntity(searchBE, "PRI_TOTAL_RESULTS", total + "");

		/* Create a QMsg with the Search BE */
		QDataBaseEntityMessage searchMsg = new QDataBaseEntityMessage(searchBE);
		searchMsg.setToken(this.beUtils.getGennyToken().getToken());
		searchMsg.setReplace(true);

		// Send to frontend
		String json = JsonUtils.toJson(searchMsg);
		VertxUtils.writeMsg("webcmds", json);

		long endTime = System.currentTimeMillis();
		long totalTime = (endTime - startTime);
		System.out.println("duration was " + totalTime + "ms");
		return totalTime;
	}

	public Long performCount(String searchCode) {

		log.info("Fetching SearchBE " + searchCode + " from cache");

		SearchEntity searchBE = VertxUtils.getObject(this.beUtils.getGennyToken().getRealm(), "", searchCode, SearchEntity.class,
		beUtils.getGennyToken().getToken());

		return performCount(searchBE);
	}

	public Long performCount(SearchEntity searchBE) {

		System.out.println("SBE CODE   ::   " + searchBE.getCode());
		long startTime = System.currentTimeMillis();

		// Attach any extra filters from SearchFilters rulegroup
		List<EntityAttribute> filters = getUserFilters(this.beUtils.getServiceToken(), searchBE);

		if (!filters.isEmpty()) {
			log.info("User Filters are NOT empty");
			log.info("Adding User Filters to searchBe  ::  " + searchBE.getCode());
			for (EntityAttribute filter : filters) {
				searchBE.getBaseEntityAttributes().add(filter);
			}
		} else {
			log.info("User Filters are empty");
		}
		Long total = null;

		// Convert SBE to hql for searching the database
		Tuple2<String, List<String>> data = this.beUtils.getHql(searchBE);
		String hql = data._1;
		String hql2 = Base64.getUrlEncoder().encodeToString(hql.getBytes());
		log.info("hql = " + hql);
		try {
			/* Hit the api for a count */
			String resultJsonStr = QwandaUtils.apiGet(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/count24/" + hql2,
					this.beUtils.getServiceToken().getToken(), 120);

			System.out.println("Count = " + resultJsonStr);
			total = Long.parseLong(resultJsonStr);

		} catch (Exception e) {
			System.out.println("EXCEPTION RUNNING COUNT: " + e.toString());
		}

		// Perform count for any combined search attributes
		for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {
			if (ea.getAttributeCode().startsWith("CMB_")) {
				String combinedSearchCode = ea.getAttributeCode().substring("CMB_".length());
				Long subTotal = performCount(combinedSearchCode);
				if (subTotal != null) {
					total += subTotal;
				} else {
					log.info("subTotal count for " + combinedSearchCode + " is NULL");
				}
			}
		}
		return total;
	}

	static public void moveEntity(String code, String sourceCode, String targetCode, BaseEntityUtils beUtils){
		QCmdMessage msg = new QCmdMessage("MOVE_ENTITY", code);
		if(sourceCode != null){
			msg.setSourceCode(sourceCode + "_" + beUtils.getGennyToken().getSessionCode().toUpperCase());
		}
		if(targetCode != null){
			msg.setTargetCode(targetCode + "_" + beUtils.getGennyToken().getSessionCode().toUpperCase());
		}
		msg.setToken(beUtils.getGennyToken().getToken());
		msg.setSend(true);  		
		VertxUtils.writeMsg("webcmds",msg);
}
}
