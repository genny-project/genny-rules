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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

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

	public QBulkMessage performSearch(GennyToken userToken, GennyToken serviceToken, String searchBeCode,
			Answer answer) {
		SearchEntity searchBE = getSessionSearch(searchBeCode);
		return performSearch(userToken, serviceToken, searchBE,
				answer);
	}
	public QBulkMessage performSearch(GennyToken userToken, GennyToken serviceToken, SearchEntity searchBE,
			Answer answer) {
		QBulkMessage ret = new QBulkMessage();
		beUtils.setGennyToken(userToken);
		ret = this.performSearch(serviceToken, searchBE, answer, null, null);
		return ret;
	}

	public QBulkMessage performSearch(GennyToken userToken, GennyToken serviceToken, SearchEntity searchBE,
			Answer answer, Boolean cache) {
		QBulkMessage ret = new QBulkMessage();
		beUtils.setGennyToken(userToken);
		ret = this.performSearch(serviceToken, searchBE, answer, null, null, cache);
		return ret;
	}

	public void performSearch(GennyToken userToken, GennyToken serviceToken, String searchBeCode, Answer answer,
			final String filterCode, final String filterValue) {
		SearchEntity searchBE = getSessionSearch(searchBeCode);

		performSearch(userToken, serviceToken, searchBE, answer, filterCode, filterValue);
	}
	
	public void performSearch(GennyToken userToken, GennyToken serviceToken, SearchEntity searchBE, Answer answer,
			final String filterCode, final String filterValue) {
		beUtils.setGennyToken(userToken);
		this.performSearch(serviceToken, searchBE, answer, filterCode, filterValue);
	}

	public QBulkMessage performSearch(GennyToken serviceToken, String searchBeCode, Answer answer,
			final String filterCode, final String filterValue) {
		SearchEntity searchBE = getSessionSearch(searchBeCode);
		
		return performSearch(serviceToken, searchBE, answer, filterCode, filterValue, false);
	}

	public QBulkMessage performSearch(GennyToken serviceToken, SearchEntity searchBE, Answer answer,
			final String filterCode, final String filterValue) {
		return performSearch(serviceToken, searchBE, answer, filterCode, filterValue, false);
	}

	public QBulkMessage performSearch(GennyToken serviceToken, final SearchEntity searchBE, Answer answer,
			final String filterCode, final String filterValue, Boolean cache) {
		QBulkMessage ret = new QBulkMessage();
		
		
		
		
		beUtils.setServiceToken(serviceToken);


		// Send out Search Results
		QDataBaseEntityMessage msg = fetchSearchResults(searchBE);
		
		if (searchBE.getBaseEntityAttributes()==null) { // we fetched from search faster by specifying no columns
			// fetch results from cache
			String realm = serviceToken.getRealm();
			List<BaseEntity> beList = new ArrayList<BaseEntity>();
			for (BaseEntity be : msg.getItems()) {
				BaseEntity fromCacheBe = VertxUtils.getObject(realm, "", be.getCode(), BaseEntity.class, serviceToken.getToken());
				beList.add(fromCacheBe);
			}
			msg.setItems(beList.toArray(new BaseEntity[0]));
		}
		
		if (cache) {
			ret.add(msg);
		} else {
			msg.setToken(beUtils.getGennyToken().getToken());
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg));
		}

		/* publishing the searchBE to frontEnd */
		updateBaseEntity(searchBE, "PRI_TOTAL_RESULTS", (msg.getTotal()) + ""); // if result
		// count = 0
		// then
		// frontend
		// not
		// showing
		// anything
		QDataBaseEntityMessage searchBeMsg = new QDataBaseEntityMessage(searchBE);

		if (cache) {
			ret.add(searchBeMsg);
		} else {
			searchBeMsg.setToken(beUtils.getGennyToken().getToken());
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson((searchBeMsg)));
		}
		Map<String, String> columns = getTableColumns(searchBE);

		/*
		 * Display the table header
		 */

		/* QDataAskMessage headerAskMsg = showTableHeader(searchBE, columns, msg); */
		log.info("calling showTableContent");
		QBulkMessage qb = showTableContent(serviceToken, searchBE, msg, columns, cache);
		ret.add(qb);
		log.info("calling sendTableContexts");
		
		QDataBaseEntityMessage qm = sendTableContexts(cache);
		ret.add(qm);
		/* showTableFooter(searchBE); */
		return ret;
	}

	public QDataBaseEntityMessage sendTableContexts(Boolean cache) {
		QDataBaseEntityMessage ret = new QDataBaseEntityMessage();
		log.info("Sending contexts for table");

		Theme THM_ICON = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "", "THM_ICON", Theme.class,
				beUtils.getServiceToken().getToken());
		Theme THM_ICON_ONLY = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "", "THM_ICON_ONLY",
				Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_HEADER_FONT = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_HEADER_FONT", Theme.class, beUtils.getServiceToken().getToken());

		Theme THM_TABLE_CONTENT_BORDER = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_CONTENT_BORDER", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_DISPLAY_HORIZONTAL = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_DISPLAY_HORIZONTAL", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CONTENT_WRAPPER = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CONTENT_WRAPPER", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_WIDTH_100_PERCENT_NO_INHERIT = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_WIDTH_100_PERCENT_NO_INHERIT", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "", "THM_TABLE_ROW",
				Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "", "THM_TABLE_ROW_CELL",
				Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_CONTENT = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "", "THM_TABLE_CONTENT",
				Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_BORDER = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "", "THM_TABLE_BORDER",
				Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_WIDTH_100_PERCENT = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_WIDTH_100_PERCENT", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_FOOTER_BORDER = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_FOOTER_BORDER", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_EVENT = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_EVENT", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_TEXT = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_TEXT", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_TEXT_HEADER = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_TEXT_HEADER", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_LANDLINE = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_LANDLINE", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_PHONE = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_PHONE", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_MOBILE = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_MOBILE", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_ADDRESS = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_ADDRESS", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_EMAIL = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_EMAIL", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_VIEW = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_VIEW", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_VIEW_JOURNAL = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_VIEW_JOURNAL", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_APPLY = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_APPLY", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_STATUS = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_STATUS", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_NAME = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_NAME", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_DATE = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_DATE", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_HOURS = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_HOURS", Theme.class, beUtils.getServiceToken().getToken());
		Theme THM_TABLE_ROW_CELL_HTML = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
				"THM_TABLE_ROW_CELL_HTML", Theme.class, beUtils.getServiceToken().getToken());
		// Theme THM_TABLE_HEADER_CELL_EVENT =
		// VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "",
		// "THM_TABLE_HEADER_CELL_EVENT", Theme.class,
		// beUtils.getServiceToken().getToken());

		/* get the icons */
		BaseEntity ICN_VIEW = beUtils.getBaseEntityByCode("ICN_VIEW");
		BaseEntity ICN_ADD = beUtils.getBaseEntityByCode("ICN_ADD");
		BaseEntity ICN_DESCRIPTION = beUtils.getBaseEntityByCode("ICN_DESCRIPTION");

		/* get the themes */
		BaseEntity THM_ICON_BE = this.getThemeBe(THM_ICON);
		BaseEntity THM_ICON_ONLY_BE = this.getThemeBe(THM_ICON_ONLY);
		BaseEntity THM_TABLE_HEADER_FONT_BE = this.getThemeBe(THM_TABLE_HEADER_FONT);
		BaseEntity THM_TABLE_CONTENT_BORDER_BE = this.getThemeBe(THM_TABLE_CONTENT_BORDER);
		BaseEntity THM_DISPLAY_HORIZONTAL_BE = this.getThemeBe(THM_DISPLAY_HORIZONTAL);
		BaseEntity THM_TABLE_ROW_CONTENT_WRAPPER_BE = this.getThemeBe(THM_TABLE_ROW_CONTENT_WRAPPER);
		BaseEntity THM_WIDTH_100_PERCENT_NO_INHERIT_BE = this.getThemeBe(THM_WIDTH_100_PERCENT_NO_INHERIT);
		BaseEntity THM_TABLE_ROW_BE = this.getThemeBe(THM_TABLE_ROW);
		BaseEntity THM_TABLE_ROW_CELL_BE = this.getThemeBe(THM_TABLE_ROW_CELL);
		BaseEntity THM_TABLE_CONTENT_BE = this.getThemeBe(THM_TABLE_CONTENT);
		BaseEntity THM_TABLE_BORDER_BE = this.getThemeBe(THM_TABLE_BORDER);
		BaseEntity THM_WIDTH_100_PERCENT_BE = this.getThemeBe(THM_WIDTH_100_PERCENT);
		BaseEntity THM_TABLE_FOOTER_BORDER_BE = this.getThemeBe(THM_TABLE_FOOTER_BORDER);
		BaseEntity THM_TABLE_ROW_CELL_EVENT_BE = this.getThemeBe(THM_TABLE_ROW_CELL_EVENT);
		BaseEntity THM_TABLE_ROW_CELL_TEXT_BE = this.getThemeBe(THM_TABLE_ROW_CELL_TEXT);
		BaseEntity THM_TABLE_ROW_CELL_TEXT_HEADER_BE = this.getThemeBe(THM_TABLE_ROW_CELL_TEXT_HEADER);
		BaseEntity THM_TABLE_ROW_CELL_LANDLINE_BE = this.getThemeBe(THM_TABLE_ROW_CELL_LANDLINE);
		BaseEntity THM_TABLE_ROW_CELL_MOBILE_BE = this.getThemeBe(THM_TABLE_ROW_CELL_MOBILE);
		BaseEntity THM_TABLE_ROW_CELL_PHONE_BE = this.getThemeBe(THM_TABLE_ROW_CELL_PHONE);
		BaseEntity THM_TABLE_ROW_CELL_ADDRESS_BE = this.getThemeBe(THM_TABLE_ROW_CELL_ADDRESS);
		BaseEntity THM_TABLE_ROW_CELL_EMAIL_BE = this.getThemeBe(THM_TABLE_ROW_CELL_EMAIL);
		BaseEntity THM_TABLE_ROW_CELL_VIEW_BE = this.getThemeBe(THM_TABLE_ROW_CELL_VIEW);
		BaseEntity THM_TABLE_ROW_CELL_VIEW_JOURNAL_BE = this.getThemeBe(THM_TABLE_ROW_CELL_VIEW_JOURNAL);
		BaseEntity THM_TABLE_ROW_CELL_APPLY_BE = this.getThemeBe(THM_TABLE_ROW_CELL_APPLY);
		BaseEntity THM_TABLE_ROW_CELL_STATUS_BE = this.getThemeBe(THM_TABLE_ROW_CELL_STATUS);
		BaseEntity THM_TABLE_ROW_CELL_NAME_BE = this.getThemeBe(THM_TABLE_ROW_CELL_NAME);
		BaseEntity THM_TABLE_ROW_CELL_DATE_BE = this.getThemeBe(THM_TABLE_ROW_CELL_DATE);
		BaseEntity THM_TABLE_ROW_CELL_HOURS_BE = this.getThemeBe(THM_TABLE_ROW_CELL_HOURS);
		BaseEntity THM_TABLE_ROW_CELL_HTML_BE = this.getThemeBe(THM_TABLE_ROW_CELL_HTML);

		List<BaseEntity> themes = new ArrayList<BaseEntity>();
		themes.add(THM_ICON_BE);
		themes.add(THM_ICON_ONLY_BE);
		themes.add(THM_TABLE_HEADER_FONT_BE);
		themes.add(THM_TABLE_CONTENT_BORDER_BE);
		themes.add(THM_DISPLAY_HORIZONTAL_BE);
		themes.add(THM_TABLE_ROW_CONTENT_WRAPPER_BE);
		themes.add(THM_WIDTH_100_PERCENT_NO_INHERIT_BE);
		themes.add(THM_TABLE_ROW_BE);
		themes.add(THM_TABLE_ROW_CELL_BE);
		themes.add(THM_TABLE_CONTENT_BE);
		themes.add(THM_TABLE_BORDER_BE);
		themes.add(THM_WIDTH_100_PERCENT_BE);
		themes.add(THM_TABLE_ROW_CELL_EVENT_BE);
		themes.add(THM_TABLE_ROW_CELL_TEXT_BE);
		themes.add(THM_TABLE_ROW_CELL_TEXT_HEADER_BE);
		themes.add(THM_TABLE_ROW_CELL_LANDLINE_BE);
		themes.add(THM_TABLE_ROW_CELL_MOBILE_BE);
		themes.add(THM_TABLE_ROW_CELL_PHONE_BE);
		themes.add(THM_TABLE_ROW_CELL_ADDRESS_BE);
		themes.add(THM_TABLE_ROW_CELL_EMAIL_BE);
		themes.add(THM_TABLE_ROW_CELL_VIEW_BE);
		themes.add(THM_TABLE_ROW_CELL_VIEW_JOURNAL_BE);
		themes.add(THM_TABLE_ROW_CELL_APPLY_BE);
		themes.add(THM_TABLE_ROW_CELL_STATUS_BE);
		themes.add(THM_TABLE_ROW_CELL_NAME_BE);
		themes.add(THM_TABLE_ROW_CELL_DATE_BE);
		themes.add(THM_TABLE_ROW_CELL_HOURS_BE);
		themes.add(THM_TABLE_ROW_CELL_HTML_BE);

		/* also add the icon BE to the list */
		themes.add(ICN_VIEW);
		themes.add(ICN_ADD);
		themes.add(ICN_DESCRIPTION);

		log.info("**************** Themes in the list :: " + themes.size() + " ********************");

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(themes);
		msg.setReplace(true);
		if (cache) {
			ret = msg;
		} else {
			msg.setToken(beUtils.getGennyToken().getToken());
			log.info("**************** Sending all the themes and icons now ********************");
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg));
			log.info("**************** Sent all the themes and icons ********************");
		}
		return ret;
	}

	public SearchEntity getSessionSearch(final String searchCode) {
		String sessionSearchCode = searchCode + "_" + beUtils.getGennyToken().getSessionCode().toUpperCase();

		SearchEntity searchBE = VertxUtils.getObject(beUtils.getGennyToken().getRealm(), "", searchCode,
				SearchEntity.class, beUtils.getGennyToken().getToken());

		/* we need to set the searchBe's code to session Search Code */
		searchBE.setCode(sessionSearchCode);
		for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {
			ea.setBaseEntityCode(searchBE.getCode());
		}

		/*
		 * Save Session Search in cache , ideally this should be in OutputParam and
		 * saved to workflow
		 */
		VertxUtils.putObject(beUtils.getGennyToken().getRealm(), "", searchBE.getCode(), searchBE,
				beUtils.getGennyToken().getToken());

		return searchBE;
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
	 * @param beUtils
	 * @param searchBE
	 * @param msg
	 * @param columns
	 * @param askMsgs
	 * @param headerAskMsg
	 */
	private QBulkMessage showTableContent(GennyToken serviceToken, SearchEntity searchBE, QDataBaseEntityMessage msg,
			Map<String, String> columns) {
		return showTableContent(serviceToken, searchBE, msg, columns, false);
	}

	private QBulkMessage showTableContent(GennyToken serviceToken, SearchEntity searchBE, QDataBaseEntityMessage msg,
			Map<String, String> columns, Boolean cache) {
		QBulkMessage ret = new QBulkMessage();
		log.info("inside showTableContent");

		Validation tableRowValidation = new Validation("VLD_ANYTHING", "Anything", ".*");

		List<Validation> tableRowValidations = new ArrayList<>();
		tableRowValidations.add(tableRowValidation);

		ValidationList tableRowValidationList = new ValidationList();
		tableRowValidationList.setValidationList(tableRowValidations);

		Context CTX_THM_TABLE_BORDER = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_CONTENT_BORDER", "THM_TABLE_CONTENT_BORDER"), VisualControlType.GROUP_WRAPPER,
				1.0);
		CTX_THM_TABLE_BORDER.setDataType("Table Row Group");

		DataType tableRowDataType = new DataType("DTT_TABLE_ROW_GRP", tableRowValidationList, "Table Row Group", "");

		Context horizontalContext = new Context(ContextType.THEME,
				new BaseEntity("THM_DISPLAY_HORIZONTAL", "THM_DISPLAY_HORIZONTAL"), VisualControlType.VCL_DEFAULT, 1.0);
		horizontalContext.setDataType("Table Header Group");

		Context tableRowContentWrapperContext = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CONTENT_WRAPPER", "THM_TABLE_ROW_CONTENT_WRAPPER"),
				VisualControlType.GROUP, 1.0);
		tableRowContentWrapperContext.setDataType("Table Row Group");

		Context tableRowContext = new Context(ContextType.THEME, new BaseEntity("THM_TABLE_ROW", "THM_TABLE_ROW"),
				VisualControlType.GROUP_WRAPPER, 1.0);
		tableRowContext.setDataType("Table Row Group");

		Context tableRowCellContext = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL", "THM_TABLE_ROW_CELL"), VisualControlType.VCL_WRAPPER, 1.0);
		tableRowCellContext.setDataType("Event");
		tableRowCellContext.setWeight(3.0);

		Context tableRowCellName = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_NAME", "THM_TABLE_ROW_CELL_NAME"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellName.setDttCode("DTT_TEXT_NAME");

		Context tableRowCellLandline = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_LANDLINE", "THM_TABLE_ROW_CELL_LANDLINE"),
				VisualControlType.VCL_WRAPPER, 1.0);
		tableRowCellLandline.setDataType("Landline");

		Context tableRowCellMobile = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_MOBILE", "THM_TABLE_ROW_CELL_MOBILE"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellMobile.setDataType("Mobile");

		Context tableRowCellPhone = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_PHONE", "THM_TABLE_ROW_CELL_PHONE"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellPhone.setDttCode("DTT_PHONE");

		Context tableRowCellAddress = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_ADDRESS", "THM_TABLE_ROW_CELL_ADDRESS"),
				VisualControlType.VCL_WRAPPER, 1.0);
		tableRowCellAddress.setDataType("Address");

		Context tableRowCellEmail = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_EMAIL", "THM_TABLE_ROW_CELL_EMAIL"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellEmail.setDataType("Email");

		Context tableRowCellView = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_VIEW", "THM_TABLE_ROW_CELL_VIEW"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellView.setDttCode("DTT_EVENT_VIEW");
		tableRowCellView.setWeight(2.0);

		Context tableRowCellViewJournal = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_VIEW_JOURNAL", "THM_TABLE_ROW_CELL_VIEW_JOURNAL"),
				VisualControlType.VCL_WRAPPER, 1.0);
		tableRowCellViewJournal.setDttCode("DTT_EVENT_VIEW_JOURNAL");
		tableRowCellViewJournal.setWeight(2.0);

		Context tableRowCellApply = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_APPLY", "THM_TABLE_ROW_CELL_APPLY"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellApply.setDttCode("DTT_EVENT_APPLY");

		Context tableRowCellStatus = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_STATUS", "THM_TABLE_ROW_CELL_STATUS"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellStatus.setDttCode("DTT_TEXT_STATUS");

		Context tableRowCellDate = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_DATE", "THM_TABLE_ROW_CELL_DATE"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellDate.setDttCode("DTT_PAST_DATE");

		Context tableRowCellHours = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_HOURS", "THM_TABLE_ROW_CELL_HOURS"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellHours.setDttCode("DTT_DOUBLE");

		Context tableRowCellHtml = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_HTML", "THM_TABLE_ROW_CELL_HTML"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellHtml.setDttCode("DTT_HTMLAREA");

		Context tableRowCellText = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_TEXT", "THM_TABLE_ROW_CELL_TEXT"), VisualControlType.VCL_WRAPPER,
				2.0);
		tableRowCellText.setDataType("Text");

		List<Context> contexts = new ArrayList<Context>();
		contexts.add(new Context(ContextType.THEME,
				new BaseEntity("THM_WIDTH_100_PERCENT_NO_INHERIT", "THM_WIDTH_100_PERCENT_NO_INHERIT"),
				VisualControlType.GROUP_WRAPPER, 1.0));
		contexts.add(CTX_THM_TABLE_BORDER);
		contexts.add(tableRowContentWrapperContext);
		contexts.add(horizontalContext);
		contexts.add(tableRowContext);
		contexts.add(tableRowCellName);
		contexts.add(tableRowCellLandline);
		contexts.add(tableRowCellMobile);
		contexts.add(tableRowCellPhone);
		contexts.add(tableRowCellAddress);
		contexts.add(tableRowCellEmail);
		contexts.add(tableRowCellView);
		contexts.add(tableRowCellViewJournal);
		contexts.add(tableRowCellApply);
		contexts.add(tableRowCellStatus);
		contexts.add(tableRowCellText);
		contexts.add(tableRowCellDate);
		contexts.add(tableRowCellHours);
		contexts.add(tableRowCellHtml);
		contexts.add(new Context(ContextType.THEME, new BaseEntity("THM_TABLE_CONTENT", "THM_TABLE_CONTENT"),
				VisualControlType.GROUP, 1.0));

		log.info("outside loop before the loop");

		ContextList rowsContextList = new ContextList(contexts);

		/* get the baseentity results */
		List<BaseEntity> rowList = Arrays.asList(msg.getItems());

		List<Ask> rowAsks = new ArrayList<Ask>();
		TableData tableData = generateTableAsks(searchBE);

		Ask headerAsk = getHeaderAsk(searchBE);

		List<Context> headerContexts = new ArrayList<Context>();

		Context tableRowCellNameHeader = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_NAME", "THM_TABLE_ROW_CELL_NAME"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellNameHeader.setDttCode("DTT_TEXT_NAME_HEADER");

		Context tableRowCellLandlineHeader = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_LANDLINE", "THM_TABLE_ROW_CELL_LANDLINE"),
				VisualControlType.VCL_WRAPPER, 1.0);
		tableRowCellLandlineHeader.setDttCode("DTT_LANDLINE_HEADER");

		Context tableRowCellMobileHeader = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_MOBILE", "THM_TABLE_ROW_CELL_MOBILE"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellMobileHeader.setDttCode("DTT_MOBILE_HEADER");

		Context tableRowCellPhoneHeader = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_PHONE", "THM_TABLE_ROW_CELL_PHONE"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellPhoneHeader.setDttCode("DTT_PHONE_HEADER");

		Context tableRowCellAddressHeader = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_ADDRESS", "THM_TABLE_ROW_CELL_ADDRESS"),
				VisualControlType.VCL_WRAPPER, 1.0);
		tableRowCellAddressHeader.setDttCode("DTT_ADDRESS_HEADER");

		Context tableRowCellEmailHeader = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_EMAIL", "THM_TABLE_ROW_CELL_EMAIL"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellEmailHeader.setDttCode("DTT_EMAIL_HEADER");

		Context tableRowCellViewHeader = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_VIEW", "THM_TABLE_ROW_CELL_VIEW"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellViewHeader.setDttCode("DTT_EVENT_VIEW_HEADER");
		tableRowCellViewHeader.setWeight(2.0);

		Context tableRowCellViewJournalHeader = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_VIEW_JOURNAL", "THM_TABLE_ROW_CELL_VIEW_JOURNAL"),
				VisualControlType.VCL_WRAPPER, 1.0);
		tableRowCellViewJournalHeader.setDttCode("DTT_EVENT_VIEW_JOURNAL_HEADER");
		// tableRowCellViewJournalHeader.setWeight(2.0);

		Context tableRowCellApplyHeader = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_APPLY", "THM_TABLE_ROW_CELL_APPLY"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellApplyHeader.setDttCode("DTT_EVENT_APPLY_HEADER");

		Context tableRowCellStatusHeader = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_STATUS", "THM_TABLE_ROW_CELL_STATUS"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellStatusHeader.setDttCode("DTT_TEXT_STATUS_HEADER");

		Context tableRowCellTextHeader = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_TEXT", "THM_TABLE_ROW_CELL_TEXT"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellTextHeader.setDttCode("DTT_TEXT_HEADER");

		Context tableRowCellDateHeader = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_DATE", "THM_TABLE_ROW_CELL_DATE"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellDateHeader.setDttCode("DTT_PAST_DATE_HEADER");

		Context tableRowCellHoursHeader = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_HOURS", "THM_TABLE_ROW_CELL_HOURS"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellHoursHeader.setDttCode("DTT_DOUBLE_HEADER");

		Context tableRowCellHtmlHeader = new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_ROW_CELL_HTML", "THM_TABLE_ROW_CELL_HTML"), VisualControlType.VCL_WRAPPER,
				1.0);
		tableRowCellHtmlHeader.setDttCode("DTT_HTMLAREA_HEADER");

		/* newly added contexts to header */
		headerContexts.add(tableRowCellNameHeader);
		headerContexts.add(tableRowCellLandlineHeader);
		headerContexts.add(tableRowCellMobileHeader);
		headerContexts.add(tableRowCellPhoneHeader);
		headerContexts.add(tableRowCellAddressHeader);
		headerContexts.add(tableRowCellEmailHeader);
		headerContexts.add(tableRowCellViewHeader);
		headerContexts.add(tableRowCellViewJournalHeader);
		headerContexts.add(tableRowCellApplyHeader);
		headerContexts.add(tableRowCellStatusHeader);
		headerContexts.add(tableRowCellTextHeader);
		headerContexts.add(tableRowCellDateHeader);
		headerContexts.add(tableRowCellHoursHeader);
		headerContexts.add(tableRowCellHtmlHeader);

		headerContexts.add(new Context(ContextType.THEME,
				new BaseEntity("THM_TABLE_HEADER_FONT", "THM_TABLE_HEADER_FONT"), VisualControlType.INPUT_FIELD, 1.0));
		headerContexts.add(new Context(ContextType.THEME, new BaseEntity("THM_TABLE_BORDER", "THM_TABLE_BORDER"),
				VisualControlType.GROUP_WRAPPER, 1.0));
		headerContexts
				.add(new Context(ContextType.THEME, new BaseEntity("THM_WIDTH_100_PERCENT", "THM_WIDTH_100_PERCENT"),
						VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
		headerContexts.add(new Context(ContextType.THEME, new BaseEntity("THM_TABLE_ROW", "THM_TABLE_ROW"),
				VisualControlType.GROUP_WRAPPER, 1.0));
		headerAsk.setContextList(new ContextList(headerContexts));

		rowAsks.add(headerAsk);
		rowAsks.addAll(generateQuestions(rowList, columns, beUtils.getGennyToken().getUserCode()));

		/* converting rowAsks list to array */
		Ask[] rowAsksArr = rowAsks.stream().toArray(Ask[]::new);

		/* Now send out the question rows and themes etc */

		/* Link row asks to a single ask: QUE_TEST_TABLE_RESULTS_GRP */
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP_TABLE_RESULTS", "link",
				new DataType(String.class));
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

		/* send the frame FRM_TABLE_CONTENT */
		log.info("*************** Sending the FRM_TABLE_CONTENT msg after toMessage ***************");

		QDataBaseEntityMessage msg3 = changeQuestion(searchBE, "FRM_TABLE_CONTENT", tableResultAsk, serviceToken,
				beUtils.getGennyToken(), tableResultAskMsgs);
		msg3.setReplace(true);

		if (cache) {
			ret.add(msg3);
		} else {
			msg3.setToken(beUtils.getGennyToken().getToken());
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg3));
		}

		/* send the results questionGroup */
		log.info("*************** Sending the QUE_TABLE_RESULTS_GRP askMsg ***************");
		QDataAskMessage askMsg = new QDataAskMessage(tableResultAsk);
		askMsg.setReplace(true);

		if (cache) {
			ret.add(askMsg);
		} else {
			askMsg.setToken(beUtils.getGennyToken().getToken());
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson(askMsg));
		}

		log.info("*************** Sending table title question ***************");
		QDataAskMessage qAskMsg = sendQuestion("QUE_TABLE_TITLE_TEST", beUtils.getGennyToken().getUserCode(),
				searchBE.getCode(), "SCH_TITLE", beUtils.getGennyToken(), cache);
		if (cache) {
			ret.add(qAskMsg);
		}
		return ret;
	}

	/**
	 * @param beUtils
	 * @param searchBE
	 */
//	private void showTableFooter(SearchEntity searchBE) {
//		/* need to send the footer question again here */
//		Attribute totalAttribute = new Attribute("PRI_TOTAL_RESULTS", "link", new DataType(String.class));
//		Attribute indexAttribute = new Attribute("PRI_INDEX", "link", new DataType(String.class));
//
//		/* create total count ask */
//		Question totalQuestion = new Question("QUE_TABLE_TOTAL_RESULT_COUNT", "Total Results", totalAttribute, true);
//
//		Ask totalAsk = new Ask(totalQuestion, beUtils.getGennyToken().getUserCode(), searchBE.getCode());
//		totalAsk.setReadonly(true);
//		totalAsk.setRealm(beUtils.getGennyToken().getRealm());
//		/* create index ask */
//		Question indexQuestion = new Question("QUE_TABLE_PAGE_INDEX", "Page Number", indexAttribute, true);
//
//		Ask indexAsk = new Ask(indexQuestion, beUtils.getGennyToken().getUserCode(), searchBE.getCode());
//		indexAsk.setReadonly(true);
//		indexAsk.setRealm(beUtils.getGennyToken().getRealm());
//
//		/* collect the asks to be sent out */
//		Set<QDataAskMessage> footerAskMsgs = new HashSet<QDataAskMessage>();
//		footerAskMsgs.add(new QDataAskMessage(totalAsk));
//		footerAskMsgs.add(new QDataAskMessage(indexAsk));
//
//		/* publish the new asks with searchBe set as targetCode */
//		for (QDataAskMessage footerAskMsg : footerAskMsgs) {
//			footerAskMsg.setToken(beUtils.getGennyToken().getToken());
//			footerAskMsg.setReplace(false);
//			VertxUtils.writeMsg("webcmds", JsonUtils.toJson(footerAskMsg));
//		}
//	}

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
			tests.add(createTestCompany("Monash University", "0398744421", "support@melbuni.edu.au", "CLAYTON",
					"Victoria", "3142"));
			tests.add(createTestCompany("Latrobe University", "0398733321", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("University Of Warracknabeal", "0392225321", "support@melbuni.edu.au",
					"WARRACKNABEAL", "Victoria", "3993"));
			tests.add(createTestCompany("Ashburton University", "0398741111", "support@melbuni.edu.au", "ASHBURTON",
					"Victoria", "3147"));
			tests.add(createTestCompany("Outcome Academy", "0398745777", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Holland University", "0298555521", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("University of Greenvale", "0899995321", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Crow University", "0398749999", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("RMIT University", "0398748787", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Mt Buller University", "0398836421", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Australian National University", "0198876541", "support@melbuni.edu.au",
					"MELBOURNE", "Victoria", "3001"));
			tests.add(createTestCompany("Dodgy University", "0390000001", "support@melbuni.edu.au", "MELBOURNE",
					"Victoria", "3001"));
			tests.add(createTestCompany("Australian Catholic University", "0398711121", "support@melbuni.edu.au",
					"MELBOURNE", "Victoria", "3001"));
			tests.add(createTestCompany("Australian Jedi University", "0798788881", "support@melbuni.edu.au",
					"MELBOURNE", "Victoria", "3001"));
			tests.add(createTestCompany("Brisbane Lions University", "0401020319", "support@melbuni.edu.au", "BRISBANE",
					"Queensland", "4000"));
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

	public BaseEntity createTestCompany(String name, String phone, String email, String city, String state,
			String postcode) {
		String usercode = "CPY_" + UUID.randomUUID().toString().substring(0, 15).toUpperCase().replaceAll("-", "");

		BaseEntity result1 = new BaseEntity(usercode, name);
		result1.setRealm(beUtils.getGennyToken().getRealm());
		try {
			result1.addAnswer(new Answer(result1, result1, attribute("PRI_EMAIL", beUtils.getGennyToken()), email));
			result1.addAnswer(
					new Answer(result1, result1, attribute("PRI_ADDRESS_STATE", beUtils.getGennyToken()), state));
			result1.addAnswer(
					new Answer(result1, result1, attribute("PRI_ADDRESS_CITY", beUtils.getGennyToken()), city));
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

		/* get vertical display theme */
		BaseEntity verticalTheme = beUtils.getBaseEntityByCode("THM_DISPLAY_VERTICAL");

		for (Map.Entry<String, String> column : columns.entrySet()) {

			String attributeCode = column.getKey();
			String attributeName = column.getValue();

			Attribute searchAttribute = new Attribute(attributeCode, attributeName,
					new DataType("Text", searchValidationList, "Text"));

			/* Initialize Column Header Ask group */
			Question columnHeaderQuestion = new Question("QUE_" + attributeCode, attributeName, priEvent, true);
			Ask columnHeaderAsk = new Ask(columnHeaderQuestion, beUtils.getGennyToken().getUserCode(),
					searchBe.getCode());

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

			/* get paddingX theme */
			// BaseEntity paddingXTheme = beUtils.getBaseEntityByCode("THM_PADDING_X_10");

			// QDataBaseEntityMessage paddingXThemeMsg = new QDataBaseEntityMessage(paddingXTheme);
			// paddingXThemeMsg.setToken(beUtils.getGennyToken().getToken());

			// /* publish paddingXTheme */
			// VertxUtils.writeMsg("webcmds", JsonUtils.toJson((paddingXThemeMsg)));

			/* set Vertical Theme to columnHeaderAsk */
			// columnHeaderAsk = this.createVirtualContext(columnHeaderAsk, paddingXTheme, ContextType.THEME,
			// 		VisualControlType.GROUP_CONTENT_WRAPPER, themeMsgList);

			asks.add(columnHeaderAsk);
		}

		/* Convert List to Array */
		Ask[] asksArray = asks.toArray(new Ask[0]);

		/*
		 * we create a table-header ask grp and set all the column asks as it's childAsk
		 */
		Question tableHeaderQuestion = new Question("QUE_TABLE_HEADER_GRP", searchBe.getName(), questionAttribute,
				true);

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

	public Ask createVirtualContext(Ask ask, BaseEntity theme, ContextType linkCode,
			VisualControlType visualControlType, List<QDataBaseEntityMessage> themeMsgList) {
		List<BaseEntity> themeList = new ArrayList<>();
		themeList.add(theme);
		return createVirtualContext(ask, themeList, linkCode, visualControlType, themeMsgList);
	}

	public Ask createVirtualContext(Ask ask, BaseEntity theme, ContextType linkCode,
			VisualControlType visualControlType, Double weight, List<QDataBaseEntityMessage> themeMsgList) {
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
		createVirtualContext(columnSortAsk, visualBaseEntity, ContextType.THEME, VisualControlType.VCL_INPUT,
				themeMsgList);
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
						EntityEntity entityEntity = new EntityEntity(targetFrame, sourceFrame, attribute, 1.0,
								"CENTRE");
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

		/* get the themes */
		Theme THM_ICON = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "", "THM_ICON", Theme.class,
				beUtils.getServiceToken().getToken());
		Theme THM_ICON_ONLY = VertxUtils.getObject(beUtils.getServiceToken().getRealm(), "", "THM_ICON_ONLY",
				Theme.class, beUtils.getServiceToken().getToken());

		/* get the sort icon */
		BaseEntity ICN_VIEW = beUtils.getBaseEntityByCode("ICN_VIEW");
		BaseEntity ICN_ADD = beUtils.getBaseEntityByCode("ICN_ADD");
		BaseEntity ICN_DESCRIPTION = beUtils.getBaseEntityByCode("ICN_DESCRIPTION");

		List<Context> viewContextList = new ArrayList<>();
		viewContextList.add(new Context(ContextType.THEME, this.getThemeBe(THM_ICON_ONLY), VisualControlType.VCL, 1.0));
		viewContextList.add(new Context(ContextType.ICON, ICN_VIEW, VisualControlType.VCL_ICON, 1.0));

		List<Context> applyContextList = new ArrayList<>();
		applyContextList
				.add(new Context(ContextType.THEME, this.getThemeBe(THM_ICON_ONLY), VisualControlType.VCL, 1.0));
		applyContextList.add(new Context(ContextType.ICON, ICN_ADD, VisualControlType.VCL_ICON, 1.0));

		List<Context> documentContextList = new ArrayList<>();
		documentContextList
				.add(new Context(ContextType.THEME, this.getThemeBe(THM_ICON_ONLY), VisualControlType.VCL, 1.0));
		documentContextList.add(new Context(ContextType.ICON, ICN_DESCRIPTION, VisualControlType.VCL_ICON, 1.0));

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
							childAsk.setReadonly(true);

							/* switch case to add icons */
							switch (attr.getCode()) {
							case "PRI_EVENT_VIEW":
								// log.info("attribute code is PRI_EVENT_VIEW attaching the context now");
								childAsk.setContextList(new ContextList(viewContextList));
								childAsk.setReadonly(false);

								break;
							case "PRI_EVENT_APPLY":
								// log.info("attribute code is PRI_EVENT_APPLY attaching the context now");
								childAsk.setContextList(new ContextList(applyContextList));
								childAsk.setReadonly(false);

								break;
							case "PRI_EVENT_JOURNAL_VIEW":
								// log.info("attribute code is PRI_EVENT_APPLY attaching the context now");
								childAsk.setContextList(new ContextList(documentContextList));
								childAsk.setReadonly(false);

								break;

							default:
								break;
							}

							/* add the entityAttribute ask to list */
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
					Question parentQuestion = new Question("QUE_" + be.getCode() + "_GRP", be.getName(),
							tableRowAttribute, true);
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

			Attribute headerAttr;
			headerAttr = RulesUtils.attributeMap.get(attributeCode + "_HEADER");
			if (headerAttr == null) {
				log.info("Header attribute is null");
				log.info(attributeCode + "_HEADER is null");
				headerAttr = nameAttr;
			}

			/* Initialize Column Header Ask group */
			Question headerQues = new Question("QUE_" + attributeCode, attributeName, headerAttr, true);
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

	/* returns baseentity of a theme */
	public BaseEntity getThemeBe(Theme theme) {

		BaseEntity themeBe = null;
		themeBe = theme.getBaseEntity();
		if (theme.getAttributes() != null) {
			for (ThemeAttribute themeAttribute : theme.getAttributes()) {

				try {
					themeBe.addAttribute(new EntityAttribute(themeBe, new Attribute(themeAttribute.getCode(),
							themeAttribute.getCode(), new DataType("DTT_THEME")), 1.0, themeAttribute.getJson()));
				} catch (BadDataException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return themeBe;
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
		long starttime = System.currentTimeMillis();
		
		System.out.println("Cache enabled ? ::" + cache);
		System.out.println("FINAL CODE   ::   " + code);

		String searchBeCode = "SBE_" + code;
		System.out.println("SBE CODE   ::   " + searchBeCode);

		String frameCode = "FRM_TABLE_" + code;
		System.out.println("FRAME CODE   ::   " + frameCode);

		long s1time = System.currentTimeMillis();
		/* get current search */
		TableUtils tableUtils = new TableUtils(beUtils);
		SearchEntity searchBE = tableUtils.getSessionSearch(searchBeCode);

		long s2time = System.currentTimeMillis();
		Answer pageAnswer = new Answer(beUtils.getGennyToken().getUserCode(), searchBE.getCode(), "SCH_PAGE_START",
				"0");
		Answer pageNumberAnswer = new Answer(beUtils.getGennyToken().getUserCode(), searchBE.getCode(), "PRI_INDEX",
				"1");

		searchBE = beUtils.updateBaseEntity(searchBE, pageAnswer, SearchEntity.class);
		searchBE = beUtils.updateBaseEntity(searchBE, pageNumberAnswer, SearchEntity.class);

		VertxUtils.putObject(beUtils.getGennyToken().getRealm(), "", searchBE.getCode(), searchBE,
				beUtils.getGennyToken().getToken());
		
		VertxUtils.putObject(beUtils.getGennyToken().getRealm(), "LAST-SEARCH", beUtils.getGennyToken().getSessionCode(), searchBE,
				beUtils.getGennyToken().getToken());

		long s3time = System.currentTimeMillis();

		ExecutorService WORKER_THREAD_POOL = Executors.newFixedThreadPool(10);
		CompletionService<QBulkMessage> service = new ExecutorCompletionService<>(WORKER_THREAD_POOL);

		TableFrameCallable tfc = new TableFrameCallable(beUtils, cache);
		SearchCallable sc = new SearchCallable(tableUtils, searchBE, beUtils, cache);

		List<Callable<QBulkMessage>> callables = Arrays.asList(tfc, sc);

		QBulkMessage aggregatedMessages = new QBulkMessage();

		long startProcessingTime = System.currentTimeMillis();
		long totalProcessingTime;

		if (GennySettings.useConcurrencyMsgs) {
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
			aggregatedMessages.add(tfc.call());
			aggregatedMessages.add(sc.call());

		}
		totalProcessingTime = System.currentTimeMillis() - startProcessingTime;
		System.out.println("All threads finished after: " + totalProcessingTime + " milliseconds");
		aggregatedMessages.setToken(beUtils.getGennyToken().getToken());

        // Before V3.1.0-AC-rules
//		QDataAskMessage[] asks = aggregatedMessages.getAsks();
//		aggregatedMessages.setAsks(asks); 
//
		QDataAskMessage[] asks = aggregatedMessages.getAsks();
		
		log.info("Checking asks included in QBulk  :: " + aggregatedMessages.getAsks().length);
		
		/* aggregatedMessages.setAsks(null); */

		if (cache) {
			System.out.println("Cache is enabled ! Sending Qbulk message with QDataBaseEntityMessage and QDataAskMessage !!!");
			String json = JsonUtils.toJson(aggregatedMessages);
			VertxUtils.writeMsg("webcmds", json);
			for (QDataAskMessage askMsg : asks) {
				askMsg.setToken(beUtils.getGennyToken().getToken());
				VertxUtils.writeMsg("webcmds", JsonUtils.toJson(askMsg));
			}
		}

		/* update(output); */
		long endtime = System.currentTimeMillis();
		System.out.println("init setup took " + (s1time - starttime) + " ms");
		System.out.println("search session setup took " + (s2time - s1time) + " ms");
		System.out.println("update searchBE BE setup took " + (s3time - s2time) + " ms");
		return (endtime - starttime);
	}
	
	static public long searchTable(BaseEntityUtils beUtils, SearchEntity searchBE, Boolean cache) {
		long starttime = System.currentTimeMillis();
		

		long s1time = System.currentTimeMillis();
		/* get current search */
		TableUtils tableUtils = new TableUtils(beUtils);

		long s2time = System.currentTimeMillis();

		ExecutorService WORKER_THREAD_POOL = Executors.newFixedThreadPool(10);
		CompletionService<QBulkMessage> service = new ExecutorCompletionService<>(WORKER_THREAD_POOL);

		TableFrameCallable tfc = new TableFrameCallable(beUtils, cache);
		SearchCallable sc = new SearchCallable(tableUtils, searchBE, beUtils, cache);

		List<Callable<QBulkMessage>> callables = Arrays.asList(tfc, sc);

		QBulkMessage aggregatedMessages = new QBulkMessage();

		long startProcessingTime = System.currentTimeMillis();
		long totalProcessingTime;

		if (GennySettings.useConcurrencyMsgs) {
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
			aggregatedMessages.add(tfc.call());
			aggregatedMessages.add(sc.call());

		}
		totalProcessingTime = System.currentTimeMillis() - startProcessingTime;
		System.out.println("All threads finished after: " + totalProcessingTime + " milliseconds");
		aggregatedMessages.setToken(beUtils.getGennyToken().getToken());

		if (cache) {
			System.out.println("Cache is enabled ! Sending Qbulk message with QDataBaseEntityMessage and QDataAskMessage !!!");
			String json = JsonUtils.toJson(aggregatedMessages);
			VertxUtils.writeMsg("webcmds", json);
		}

		/* update(output); */
		long endtime = System.currentTimeMillis();
		System.out.println("init setup took " + (s1time - starttime) + " ms");
		System.out.println("search session setup took " + (s2time - s1time) + " ms");
		return (endtime - starttime);
	}
}
