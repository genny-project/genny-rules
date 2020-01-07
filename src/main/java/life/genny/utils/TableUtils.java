package life.genny.utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import io.vertx.core.json.JsonObject;
import life.genny.models.Frame3;
import life.genny.models.GennyToken;
import life.genny.models.TableData;
import life.genny.models.Theme;
import life.genny.models.ThemeAttribute;
import life.genny.models.ThemePosition;
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
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.validation.Validation;
import life.genny.qwanda.validation.ValidationList;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;

public class TableUtils {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	static Integer MAX_SEARCH_HISTORY_SIZE = 10;
	static Integer MAX_SEARCH_BAR_TEXT_SIZE = 20;

	BaseEntityUtils beUtils = null;

	public TableUtils(BaseEntityUtils beUtils) {
		this.beUtils = beUtils;
	}
	
	public void performSearch(GennyToken userToken, GennyToken serviceToken, final String searchBarCode,
			Answer answer) {
		
		beUtils.setGennyToken(userToken);
		this.performSearch(serviceToken, searchBarCode, answer);
	}
	
	public void performSearch(GennyToken serviceToken, final String searchBarCode,
			Answer answer) {
		beUtils.setServiceToken(serviceToken);

		SearchEntity searchBE = processSearchString(answer, searchBarCode);

		// Send out Search Results
		QDataBaseEntityMessage msg = fetchSearchResults(searchBE);

		VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg));

		/* publishing the searchBE to frontEnd */
		updateBaseEntity(searchBE, "PRI_TOTAL_RESULTS", (msg.getTotal()) + ""); // if result
		// count = 0
		// then
		// frontend
		// not
		// showing
		// anything
		QDataBaseEntityMessage searchBeMsg = new QDataBaseEntityMessage(searchBE);
		searchBeMsg.setToken(beUtils.getGennyToken().getToken());
		//searchBeMsg.setToken(serviceToken.getToken());
		VertxUtils.writeMsg("webcmds", JsonUtils.toJson((searchBeMsg)));

		Map<String, String> columns = getTableColumns(searchBE);

		/*
		 * Display the table header
		 */

		/*QDataAskMessage headerAskMsg = showTableHeader(searchBE, columns, msg);*/

		showTableContent(serviceToken,  searchBE, msg, columns);

		showTableFooter(searchBE);

	}

	public  SearchEntity getSessionSearch(final String searchCode) {
		String sessionSearchCode = searchCode + "_" + beUtils.getGennyToken().getSessionCode().toUpperCase();

		SearchEntity searchBE = VertxUtils.getObject(beUtils.getGennyToken().getRealm(), "", searchCode, SearchEntity.class, beUtils.getGennyToken().getToken());
		
		/* we need to set the searchBe's code to session Search Code */
		searchBE.setCode(sessionSearchCode);
		for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {
			ea.setBaseEntityCode(searchBE.getCode());
		}

		/*
		 * Save Session Search in cache , ideally this should be in OutputParam and
		 * saved to workflow
		 */
		VertxUtils.putObject(beUtils.getGennyToken().getRealm(), "", searchBE.getCode(), searchBE, beUtils.getGennyToken().getToken());

		return searchBE;
	}

	private SearchEntity processSearchString(Answer answer, final String searchBarCode) {
		
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
		if(searchBarString != null){
			searchBE.addFilter("PRI_NAME", SearchEntity.StringFilter.LIKE, "%" + searchBarString + "%");
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

	/*private  QDataAskMessage showTableHeader( SearchEntity searchBE,
			Map<String, String> columns, QDataBaseEntityMessage msg) {

		GennyToken userToken = beUtils.getGennyToken();

		// Now Send out Table Header Ask and Question
		TableData tableData = generateTableAsks(searchBE);
		Ask headerAsk = tableData.getAsk();
		Ask[] askArray = new Ask[1];
		askArray[0] = headerAsk;
		QDataAskMessage headerAskMsg = new QDataAskMessage(askArray);
		headerAskMsg.setToken(userToken.getToken());
		headerAskMsg.setReplace(true);

		// create virtual context

		// Now link the FRM_TABLE_HEADER to that new Question
		Set<QDataAskMessage> askMsgs = new HashSet<QDataAskMessage>();

		QDataBaseEntityMessage msg2 = changeQuestion(searchBE, "FRM_TABLE_HEADER", headerAsk,
				beUtils.getGennyToken(), userToken, askMsgs);
		msg2.setToken(userToken.getToken());
		msg2.setReplace(true);

		QDataAskMessage[] askMsgArr = askMsgs.toArray(new QDataAskMessage[0]);
		if ((askMsgArr.length > 0) && (askMsgArr[0].getItems().length > 0)) {
			ContextList contextList = askMsgArr[0].getItems()[0].getContextList();
			headerAskMsg.getItems()[0].setContextList(contextList);
			headerAskMsg.getItems()[0].setRealm(userToken.getRealm());
		}

		VertxUtils.writeMsg("webcmds", JsonUtils.toJson(headerAskMsg));

		VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg2));

		// Set the table title
		sendQuestion("QUE_TABLE_TITLE_TEST", beUtils.getGennyToken().getUserCode(), searchBE.getCode(),
				"SCH_TITLE", beUtils.getGennyToken());

		return headerAskMsg;
	}*/

	/**
	 * @param serviceToken
	 * @param beUtils
	 * @param searchBE
	 * @param msg
	 * @param columns
	 * @param askMsgs
	 * @param headerAskMsg
	 */
	private void showTableContent(GennyToken serviceToken,  SearchEntity searchBE,
			QDataBaseEntityMessage msg,
			Map<String, String> columns) {

		Validation tableRowValidation = new Validation("VLD_ANYTHING", "Anything", ".*");

		List<Validation> tableRowValidations = new ArrayList<>();
		tableRowValidations.add(tableRowValidation);

		ValidationList tableRowValidationList = new ValidationList();
		tableRowValidationList.setValidationList(tableRowValidations);

		Context CTX_THM_TABLE_BORDER = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_BORDER", "THM_TABLE_BORDER"), VisualControlType.GROUP, 1.0);
		CTX_THM_TABLE_BORDER.setDataType("Table Row Group");

		DataType tableRowDataType = new DataType("DTT_TABLE_ROW_GRP", tableRowValidationList, "Table Row Group", "");
		
		List<Context> contexts = new ArrayList<Context>();
		contexts.add(new Context(ContextType.THEME,
				new BaseEntity("THM_WIDTH_100_PERCENT_NO_INHERIT", "THM_WIDTH_100_PERCENT_NO_INHERIT"),
				VisualControlType.GROUP_WRAPPER, 1.0));
		contexts.add(CTX_THM_TABLE_BORDER);
		contexts.add(
				new Context(ContextType.THEME, new BaseEntity("THM_TABLE_ROW_CONTENT_WRAPPER", "THM_TABLE_ROW_CONTENT_WRAPPER"),
						VisualControlType.GROUP, 1.0));
		contexts.add(new Context(ContextType.THEME, new BaseEntity("THM_DISPLAY_HORIZONTAL", "THM_DISPLAY_HORIZONTAL"),
				VisualControlType.VCL_DEFAULT, 1.0));
		contexts.add(new Context(ContextType.THEME, new BaseEntity("THM_TABLE_ROW", "THM_TABLE_ROW"),
				VisualControlType.VCL_DEFAULT, 1.0));
		contexts.add(new Context(ContextType.THEME, new BaseEntity("THM_TABLE_ROW_CELL", "THM_TABLE_ROW_CELL"),
				VisualControlType.VCL_WRAPPER, 1.0));
		contexts.add(new Context(ContextType.THEME, new BaseEntity("THM_TABLE_CONTENT", "THM_TABLE_CONTENT"),
				VisualControlType.GROUP, 1.0));
		
		contexts.add(new Context(ContextType.THEME, new BaseEntity("THM_TABLE_CONTENT_BORDER", "THM_TABLE_CONTENT_BORDER"),
				VisualControlType.GROUP_WRAPPER, 1.0));
		
		
			
		for (Context x : contexts) {
			x.setDataType("Table Row Group");
		}
		ContextList rowsContextList = new ContextList(contexts);
		
		List<BaseEntity> rowList = Arrays.asList(msg.getItems());

		/* Wt aadddddddddddddddddddddd*/
		List<Ask> rowAsks = new ArrayList<Ask>();
		TableData tableData = generateTableAsks(searchBE);

		Ask headerAsk = getMyHeaderAsk(searchBE);

		List<Context> headerContexts = new ArrayList<Context>();
		headerContexts.add(new Context(ContextType.THEME, new BaseEntity("THM_TABLE_HEADER_FONT", "THM_TABLE_HEADER_FONT"),
		VisualControlType.INPUT_FIELD, 1.0));
		headerAsk.setContextList(new ContextList(headerContexts));

		rowAsks.add(headerAsk);
		rowAsks.addAll(generateQuestions( rowList, columns,
				beUtils.getGennyToken().getUserCode()));
		

		/* converting rowAsks list to array */
		Ask[] rowAsksArr = rowAsks.stream().toArray(Ask[]::new);

		/* Now send out the question rows and themes etc */

		/* Link row asks to a single ask: QUE_TEST_TABLE_RESULTS_GRP */
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP_TABLE_RESULTS", "link", new DataType(String.class));
		Question tableResultQuestion = new Question("QUE_TABLE_RESULTS_GRP", "Table Results Question Group",
				questionAttribute, true);
		Ask tableResultAsk = new Ask(tableResultQuestion, beUtils.getGennyToken().getUserCode(),
				beUtils.getGennyToken().getUserCode());
		tableResultAsk.setChildAsks(rowAsksArr);
		tableResultAsk.setContextList(rowsContextList);
		tableResultAsk.setReadonly(true);
		tableResultAsk.setRealm(beUtils.getGennyToken().getRealm());
		Set<QDataAskMessage> tableResultAskMsgs = new HashSet<QDataAskMessage>();
		tableResultAskMsgs.add(new QDataAskMessage(tableResultAsk));

		QDataBaseEntityMessage msg3 = changeQuestion(searchBE, "FRM_TABLE_CONTENT", tableResultAsk, serviceToken,
				beUtils.getGennyToken(), tableResultAskMsgs);
		msg3.setToken(beUtils.getGennyToken().getToken());
		msg3.setReplace(true);
		VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg3));

		QDataAskMessage askMsg = new QDataAskMessage(tableResultAsk);
		askMsg.setToken(beUtils.getGennyToken().getToken());
		askMsg.setReplace(true);
		String sendingMsg = JsonUtils.toJson(askMsg);
		Integer length = sendingMsg.length();
		VertxUtils.writeMsg("webcmds", sendingMsg);

	}

	/**
	 * @param beUtils
	 * @param searchBE
	 */
	private void showTableFooter(SearchEntity searchBE) {
		/* need to send the footer question again here */
		Attribute totalAttribute = new Attribute("PRI_TOTAL_RESULTS", "link", new DataType(String.class));
		Attribute indexAttribute = new Attribute("PRI_INDEX", "link", new DataType(String.class));

		/* create total count ask */
		Question totalQuestion = new Question("QUE_TABLE_TOTAL_RESULT_COUNT", "Total Results", totalAttribute, true);

		Ask totalAsk = new Ask(totalQuestion, beUtils.getGennyToken().getUserCode(), searchBE.getCode());
		totalAsk.setReadonly(true);
		totalAsk.setRealm(beUtils.getGennyToken().getRealm());
		/* create index ask */
		Question indexQuestion = new Question("QUE_TABLE_PAGE_INDEX", "Page Number", indexAttribute, true);

		Ask indexAsk = new Ask(indexQuestion, beUtils.getGennyToken().getUserCode(), searchBE.getCode());
		indexAsk.setReadonly(true);
		indexAsk.setRealm(beUtils.getGennyToken().getRealm());

		/* collect the asks to be sent out */
		Set<QDataAskMessage> footerAskMsgs = new HashSet<QDataAskMessage>();
		footerAskMsgs.add(new QDataAskMessage(totalAsk));
		footerAskMsgs.add(new QDataAskMessage(indexAsk));

		/* publish the new asks with searchBe set as targetCode */
		for (QDataAskMessage footerAskMsg : footerAskMsgs) {
			footerAskMsg.setToken(beUtils.getGennyToken().getToken());
			footerAskMsg.setReplace(false);
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson(footerAskMsg));
		}
	}

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

			tests.add(createTestCompany("Melbourne University", "0398745321", "support@melbuni.edu.au",
					"MELBOURNE", "Victoria", "3001"));
			tests.add(createTestCompany("Monash University", "0398744421", "support@melbuni.edu.au", "CLAYTON",
					"Victoria", "3142"));
			tests.add(createTestCompany("Latrobe University", "0398733321", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("University Of Warracknabeal", "0392225321", "support@melbuni.edu.au",
					"WARRACKNABEAL", "Victoria", "3993"));
			tests.add(createTestCompany("Ashburton University", "0398741111", "support@melbuni.edu.au",
					"ASHBURTON", "Victoria", "3147"));
			tests.add(createTestCompany("Outcome Academy", "0398745777", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Holland University", "0298555521", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("University of Greenvale", "0899995321", "support@melbuni.edu.au",
					"MELBOURNE", "Victoria", "3001"));
			tests.add(createTestCompany("Crow University", "0398749999", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("RMIT University", "0398748787", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Mt Buller University", "0398836421", "support@melbuni.edu.au",
					"MELBOURNE", "Victoria", "3001"));
			tests.add(createTestCompany("Australian National University", "0198876541", "support@melbuni.edu.au",
					"MELBOURNE", "Victoria", "3001"));
			tests.add(createTestCompany("Dodgy University", "0390000001", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Australian Catholic University", "0398711121", "support@melbuni.edu.au",
					"MELBOURNE", "Victoria", "3001"));
			tests.add(createTestCompany("Australian Jedi University", "0798788881", "support@melbuni.edu.au",
					"MELBOURNE", "Victoria", "3001"));
			tests.add(createTestCompany("Brisbane Lions University", "0401020319", "support@melbuni.edu.au",
					"BRISBANE", "Queensland", "4000"));
			tests.add(createTestCompany("AFL University", "0390000001", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Uluru University", "0398711441", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("University Of Hard Knocks", "0798744881", "support@melbuni.edu.au",
					"MELBOURNE", "Victoria", "3001"));
			tests.add(createTestCompany("Scam University", "0705020319", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));

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
//			GennyToken gToken = beUtils.getGennyToken();
//			if (beUtils.getGennyToken().hasRole("admin")) {
//				gToken = beUtils.getServiceToken();
//			}
//			if (gToken == null) {
//				log.error
//			}
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

		beUtils.addAnswer(totalAnswer);
		beUtils.addAnswer(pageNumberAnswer);

		beUtils.updateBaseEntity(searchBE, totalAnswer);
		beUtils.updateBaseEntity(searchBE, pageNumberAnswer);

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

	public BaseEntity createTestCompany(String name, String phone, String email, String city,
			String state, String postcode) {
		String usercode = "CPY_" + UUID.randomUUID().toString().substring(0, 15).toUpperCase().replaceAll("-", "");

		BaseEntity result1 = new BaseEntity(usercode, name);
		result1.setRealm(beUtils.getGennyToken().getRealm());
		try {
			result1.addAnswer(new Answer(result1, result1, attribute("PRI_EMAIL", beUtils.getGennyToken()), email));
			result1.addAnswer(new Answer(result1, result1, attribute("PRI_ADDRESS_STATE", beUtils.getGennyToken()), state));
			result1.addAnswer(new Answer(result1, result1, attribute("PRI_ADDRESS_CITY", beUtils.getGennyToken()), city));
			result1.addAnswer(new Answer(result1, result1, attribute("PRI_ADDRESS_POSTCODE", beUtils.getGennyToken()), postcode));
			result1.addAnswer(new Answer(result1, result1, attribute("PRI_LANDLINE", beUtils.getGennyToken()), phone));
		} catch (BadDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result1;
	}

	public  Attribute attribute(final String attributeCode, GennyToken gToken) {
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
		Attribute priEvent = RulesUtils.attributeMap.get("PRI_EVENT");

		/* get table columns */
		Map<String, String> columns = getTableColumns(searchBe);

		/* get vertical display theme */
		BaseEntity verticalTheme = beUtils.getBaseEntityByCode("THM_DISPLAY_VERTICAL");

		for (Map.Entry<String, String> column : columns.entrySet()) {

			String attributeCode = column.getKey();
			String attributeName = column.getValue();

			Attribute searchAttribute = new Attribute(attributeCode, attributeName,
					new DataType("Text", searchValidationList, "Text"));

			/* Initialize Column Header Ask group */
			Question columnHeaderQuestion = new Question("QUE_" + attributeCode, attributeName, priEvent,
					true);
			Ask columnHeaderAsk = new Ask(columnHeaderQuestion, beUtils.getGennyToken().getUserCode(), searchBe.getCode());

			/* creating ask for table header label-sort */
			/*Ask columnSortAsk = getAskForTableHeaderSort(searchBe, attributeCode, attributeName, eventAttribute,
					themeMsgList);*/

			/* creating Ask for table header search input */
			/*Question columnSearchQues = new Question("QUE_SEARCH_" + attributeCode, "Search " + attributeName + "..",
					searchAttribute, false);
			Ask columnSearchAsk = new Ask(columnSearchQues, beUtils.getGennyToken().getUserCode(), searchBe.getCode());
			*/

			/* adding label-sort & search asks to header-ask Group */
			//List<Ask> tableColumnChildAsks = new ArrayList<>();
			/* tableColumnChildAsks.add(columnSortAsk); */
			/* tableColumnChildAsks.add(columnSearchAsk); */
			/* Convert List to Array */
			//Ask[] tableColumnChildAsksArray = tableColumnChildAsks.toArray(new Ask[0]);

			/* set the child asks */
			//columnHeaderAsk.setChildAsks(tableColumnChildAsksArray);

			/* get paddingX theme */
			BaseEntity paddingXTheme = beUtils.getBaseEntityByCode("THM_PADDING_X_10");

			QDataBaseEntityMessage paddingXThemeMsg = new QDataBaseEntityMessage(paddingXTheme);
			paddingXThemeMsg.setToken(beUtils.getGennyToken().getToken());

			/* publish paddingXTheme */
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson((paddingXThemeMsg)));

			/* set Vertical Theme to columnHeaderAsk */
			columnHeaderAsk = this.createVirtualContext(columnHeaderAsk, paddingXTheme, ContextType.THEME,
					VisualControlType.GROUP_CONTENT_WRAPPER, themeMsgList);

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

		tableHeaderAsk = this.createVirtualContext(tableHeaderAsk, verticalTheme, ContextType.THEME, themeMsgList);

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

	private Ask getAskForTableHeaderSort(SearchEntity searchBe, String attributeCode, String attributeName,
			Attribute eventAttribute, List<QDataBaseEntityMessage> themeMsgList) {

		/* creating Ask for table header column sort */
		Question columnSortQues = new Question("QUE_SORT_" + attributeCode, attributeName, eventAttribute, false);
		Ask columnSortAsk = new Ask(columnSortQues, beUtils.getGennyToken().getUserCode(), searchBe.getCode());

		/* ADDING DEFAULT TABLE HEADER THEMES */

		/* showing the icon */
		BaseEntity sortIconBe = beUtils.getBaseEntityByCode("ICN_SORT");

		/* create visual baseentity for question with label */
		BaseEntity visualBaseEntity = beUtils.getBaseEntityByCode("THM_TABLE_HEADER_VISUAL_CONTROL");

		/* get the BaseEntity for wrapper context */
		BaseEntity horizontalWrapperBe = beUtils.getBaseEntityByCode("THM_DISPLAY_HORIZONTAL");

		/* get the theme for Label and Sort */
		BaseEntity headerLabelSortThemeBe = beUtils.getBaseEntityByCode("THM_TABLE_HEADER_SORT_THEME");

		/* set the contexts to the ask */
		createVirtualContext(columnSortAsk, horizontalWrapperBe, ContextType.THEME, VisualControlType.VCL_WRAPPER,
				themeMsgList);
		createVirtualContext(columnSortAsk, sortIconBe, ContextType.ICON, VisualControlType.VCL_ICON, themeMsgList);
		createVirtualContext(columnSortAsk, visualBaseEntity, ContextType.THEME, VisualControlType.VCL_INPUT, themeMsgList);
		createVirtualContext(columnSortAsk, headerLabelSortThemeBe, ContextType.THEME, VisualControlType.VCL_LABEL,
				themeMsgList);

		return columnSortAsk;
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

			if (ask.getQuestionCode().equals("FRM_TABLE_CONTENT")) {

				Validation tableRowValidation = new Validation("VLD_ANYTHING", "Anything", ".*");

				List<Validation> tableRowValidations = new ArrayList<>();
				tableRowValidations.add(tableRowValidation);

				ValidationList tableRowValidationList = new ValidationList();
				tableRowValidationList.setValidationList(tableRowValidations);

				DataType tableRowDataType = new DataType("DTT_TABLE_ROW_GRP", tableRowValidationList, "Table Row Group", "");

				frame = Frame3.builder(ask.getQuestionCode()).addTheme("THM_TABLE_BORDER", serviceToken).end()
						.addTheme("THM_TABLE_CONTENT_CENTRE", ThemePosition.CENTRE, serviceToken).end()
						.question(ask.getQuestionCode())
						.addTheme("THM_DISPLAY_HORIZONTAL", serviceToken)
						//.dataType(tableRowDataType)
						.weight(1.0).end().addTheme("THM_TABLE_ROW_CONTENT_WRAPPER", serviceToken)
						//.dataType(tableRowDataType)
						.vcl(VisualControlType.GROUP).weight(1.0).end().addTheme("THM_TABLE_ROW", serviceToken)
						//.dataType(tableRowDataType)
						.weight(1.0).end()
						.addTheme("THM_TABLE_CONTENT_BORDER", serviceToken)
						//.dataType(tableRowDataType)
						.vcl(VisualControlType.GROUP_WRAPPER).weight(1.0).end()
		                .addTheme("THM_TABLE_CONTENT", serviceToken)
						.vcl(VisualControlType.GROUP).end().addTheme("THM_TABLE_ROW_CELL", serviceToken)
						.vcl(VisualControlType.VCL_WRAPPER).end().end().build();

			} else {

				System.out.println("it's a FRM_TABLE_HEADER");

				Validation tableCellValidation = new Validation("VLD_ANYTHING", "Anything", ".*");

				List<Validation> tableCellValidations = new ArrayList<>();
				tableCellValidations.add(tableCellValidation);

				ValidationList tableCellValidationList = new ValidationList();
				tableCellValidationList.setValidationList(tableCellValidations);

				DataType tableCellDataType = new DataType("DTT_TABLE_CELL_GRP", tableCellValidationList, "Table Cell Group",
						"");

				frame = Frame3.builder(ask.getQuestionCode()).addTheme("THM_TABLE_BORDER", serviceToken).end()
						.addTheme("THM_TABLE_CONTENT_CENTRE", ThemePosition.CENTRE, serviceToken).end()
						.question(ask.getQuestionCode()) // QUE_TEST_TABLE_HEADER_GRP
						.addTheme("THM_QUESTION_GRP_LABEL", serviceToken).vcl(VisualControlType.GROUP).dataType(tableCellDataType)
						.end().addTheme("THM_WIDTH_100_PERCENT_NO_INHERIT", serviceToken).vcl(VisualControlType.GROUP).end()
						.addTheme("THM_TABLE_ROW_CELL", serviceToken).dataType(tableCellDataType)
						.vcl(VisualControlType.GROUP_WRAPPER).end().addTheme("THM_DISPLAY_HORIZONTAL", serviceToken).weight(2.0)
						.end().addTheme("THM_TABLE_HEADER_CELL_WRAPPER", serviceToken).vcl(VisualControlType.VCL_WRAPPER).end()
						.addTheme("THM_TABLE_HEADER_CELL_GROUP_LABEL", serviceToken).vcl(VisualControlType.GROUP_LABEL).end()
						.addTheme("THM_TABLE_HEADER_FONT", serviceToken).vcl(VisualControlType.INPUT_FIELD).dataType(priEvent.getDataType()).end()
						.addTheme("THM_TABLE_HEADER_JUSTIFY_CONTENT", serviceToken).vcl(VisualControlType.INPUT_WRAPPER).dataType(priEvent.getDataType()).end()
						.addTheme("THM_DISPLAY_VERTICAL", serviceToken).dataType(tableCellDataType).weight(1.0).end().end().build();
			}

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		QDataBaseEntityMessage msg = FrameUtils2.toMessage(frame, serviceToken, askMsgs,true);
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
	public  List<Ask> generateQuestions( List<BaseEntity> bes,
			Map<String, String> columns, String targetCode) {

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
						Question childQuestion = null;
						
						if(attributeCode.equals("PRI_EVENT")){
							childQuestion = new Question("QUE_" + attributeCode + "_" + be.getCode(), attributeName, attr, true);

						}else{
							childQuestion = new Question("QUE_" + attributeCode + "_" + be.getCode(), attributeName, attr, true);

						}

						Ask childAsk = new Ask(childQuestion, targetCode, be.getCode());

						/* add the entityAttribute ask to list */
						childAskList.add(childAsk);
						} else {
							log.error("Attribute : "+attributeCode+" DOES NOT EXIST IN AttributeMap");
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

	private void sendQuestion(String titleQuestionCode, String sourceCode, String targetCode, String attributeCode,
			GennyToken userToken) {
		// Set the table title
		Attribute nameAttribute = RulesUtils.getAttribute(attributeCode, userToken.getToken());
		Question titleQuestion = new Question(titleQuestionCode, titleQuestionCode, nameAttribute, true);

		Ask titleAsk = new Ask(titleQuestion, sourceCode, targetCode);
		titleAsk.setRealm(userToken.getRealm());
		titleAsk.setReadonly(true);
		Ask[] askArray1 = new Ask[1];
		askArray1[0] = titleAsk;
		QDataAskMessage titleAskMsg = new QDataAskMessage(askArray1);
		titleAskMsg.setToken(userToken.getToken());
		titleAskMsg.setReplace(false);

		VertxUtils.writeMsg("webcmds", JsonUtils.toJson(titleAskMsg));

	}

	private void updateBaseEntity(BaseEntity be, String attributeCode, String value) {
		Attribute attribute = RulesUtils.getAttribute(attributeCode, beUtils.getGennyToken().getToken());
		try {
			be.addAnswer(new Answer(be, be, attribute, value));
			VertxUtils.putObject(beUtils.getGennyToken().getRealm(), "", be.getCode(), be, beUtils.getGennyToken().getToken());

		} catch (BadDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public BaseEntity getThemeBe(Theme theme) {

		BaseEntity themeBe = null;
		themeBe = theme.getBaseEntity();
		if (theme.getAttributes() != null) {
			for (ThemeAttribute themeAttribute : theme.getAttributes()) {

				try {
					themeBe.addAttribute(new EntityAttribute(themeBe,
							new Attribute(themeAttribute.getCode(), themeAttribute.getCode(), new DataType("DTT_THEME")), 1.0,
							themeAttribute.getJson()));
				} catch (BadDataException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return themeBe;
	}

	public Ask getMyHeaderAsk(SearchEntity searchBe){
		/* get table columns */
		Map<String, String> columns = getTableColumns(searchBe);
		List<Ask> asks = new ArrayList<Ask>();
		Attribute nameAttr = RulesUtils.attributeMap.get("PRI_EVENT");
		Attribute questionAttribute = RulesUtils.attributeMap.get("QQQ_QUESTION_GROUP");
		Attribute questionTableRowAttribute = new Attribute("QQQ_QUESTION_GROUP_TABLE_ROW", "link",
							new DataType(String.class));
		

		for (Map.Entry<String, String> column : columns.entrySet()) {

			String attributeCode = column.getKey();
			String attributeName = column.getValue();

			/* Initialize Column Header Ask group */
			Question ques = new Question("QUE_" + attributeCode, attributeName, nameAttr, true);
			Ask ask = new Ask(ques, beUtils.getGennyToken().getUserCode(), searchBe.getCode());

			asks.add(ask);
		}

		Ask[] childAsksArr = asks.toArray(new Ask[0]);


		Question headerAskQues = new Question("QUE_TABLE_GRP", searchBe.getName(), questionTableRowAttribute, true);
		Ask headerAsk =  new Ask(headerAskQues, beUtils.getGennyToken().getUserCode(), searchBe.getCode());
		headerAsk.setChildAsks(childAsksArr);
		headerAsk.setName(searchBe.getName());
		

		return headerAsk;
	}

}
