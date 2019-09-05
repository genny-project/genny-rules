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
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import life.genny.models.Frame3;
import life.genny.models.GennyToken;
import life.genny.models.TableData;
import life.genny.models.Theme;
import life.genny.models.ThemePosition;
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
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.validation.Validation;
import life.genny.qwanda.validation.ValidationList;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.utils.ContextUtils;

public class TableUtilsTest {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	BaseEntityUtils beUtils = null;

	public TableUtilsTest(BaseEntityUtils beUtils) {
		this.beUtils = beUtils;
	}

	public TableData generateTableAsks(SearchEntity searchBe, GennyToken gennyToken, QDataBaseEntityMessage msg) {

		log.info("Search Results for " + searchBe.getCode() + " and user " + gennyToken.getUserCode() + " = " + msg); // use
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

		// Show columns
		Map<String, String> columns = getTableColumns(searchBe);
		log.info(columns);

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

	public QDataBaseEntityMessage fetchSearchResults(SearchEntity searchBE, GennyToken gennyToken) {
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(new ArrayList<BaseEntity>());

		if (gennyToken == null) {
			log.error("GENNY TOKEN IS NULL!!! in getSearchResults");
			return msg;
		}
		searchBE.setRealm(gennyToken.getRealm());
		log.info("The search BE is :: " + JsonUtils.toJson(searchBE));

		if (VertxUtils.cachedEnabled) {
			List<BaseEntity> results = new ArrayList<BaseEntity>();
			results.add(createTestPerson(gennyToken, "The Phantom", "kit.walker@phantom.bg"));
			results.add(createTestPerson(gennyToken, "Phantom Menace", "menace43r@starwars.net"));
			results.add(createTestPerson(gennyToken, "The Phantom Ranger", "phantom@rangers.com"));

			msg = new QDataBaseEntityMessage(results);
			return msg;
		}

		String jsonSearchBE = JsonUtils.toJson(searchBE);
		String resultJson;
		try {
			resultJson = QwandaUtils.apiPostEntity(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/search",
					jsonSearchBE, gennyToken.getToken());
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

	static BaseEntity createTestPerson(GennyToken gennyToken, String name, String email) {
		String usercode = "PER_" + QwandaUtils.getNormalisedUsername(email);
		BaseEntity result1 = new BaseEntity(usercode, name);
		result1.setRealm(gennyToken.getRealm());

		return result1;
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
			Question columnHeaderQuestion = new Question("QUE_" + attributeCode + "_GRP", attributeName,
					tableCellAttribute, true);
			Ask columnHeaderAsk = new Ask(columnHeaderQuestion, beUtils.getGennyToken().getUserCode(),
					searchBe.getCode());

			/* creating ask for table header label-sort */
			Ask columnSortAsk = getAskForTableHeaderSort(searchBe, attributeCode, attributeName, eventAttribute,
					themeMsgList);

			/* creating Ask for table header search input */
			Question columnSearchQues = new Question("QUE_SEARCH_" + attributeCode, "Search " + attributeName + "..",
					searchAttribute, false);
			Ask columnSearchAsk = new Ask(columnSearchQues, beUtils.getGennyToken().getUserCode(), searchBe.getCode());

			/* adding label-sort & search asks to header-ask Group */
			List<Ask> tableColumnChildAsks = new ArrayList<>();
			tableColumnChildAsks.add(columnSortAsk);
			tableColumnChildAsks.add(columnSearchAsk);

			/* Convert List to Array */
			Ask[] tableColumnChildAsksArray = tableColumnChildAsks.toArray(new Ask[0]);

			/* set the child asks */
			columnHeaderAsk.setChildAsks(tableColumnChildAsksArray);

			/* set Vertical Theme to columnHeaderAsk */
			columnHeaderAsk = this.createVirtualContext(columnHeaderAsk, verticalTheme, ContextType.THEME,
					themeMsgList);
			asks.add(columnHeaderAsk);
		}

		/* Convert List to Array */
		Ask[] asksArray = asks.toArray(new Ask[0]);

		/*
		 * we create a table-header ask grp and set all the column asks as it's childAsk
		 */
		Question tableHeaderQuestion = new Question("QUE_TABLE_HEADER_GRP", searchBe.getName(),
				questionAttribute, true);

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
	public static QDataBaseEntityMessage changeQuestion( SearchEntity searchBE, final String frameCode, final String questionCode,
			GennyToken serviceToken, GennyToken userToken, Set<QDataAskMessage> askMsgs) {
		Frame3 frame = null;
		try {


			if(frameCode.equals("FRM_TABLE_CONTENT")){
				
				Validation tableRowValidation = new Validation("VLD_ANYTHING", "Anything", ".*");

        List<Validation> tableRowValidations = new ArrayList<>();
        tableRowValidations.add(tableRowValidation);

        ValidationList tableRowValidationList = new ValidationList();
        tableRowValidationList.setValidationList(tableRowValidations);

				DataType tableRowDataType = new DataType("DTT_TABLE_ROW_GRP", tableRowValidationList, "Table Row Group", "");

				frame = Frame3.builder(frameCode)
								.addTheme("THM_TABLE_BORDER", serviceToken).end()
								.addTheme("THM_TABLE_CONTENT_CENTRE", ThemePosition.CENTRE, serviceToken).end()
								.question(questionCode)
									.addTheme("THM_DISPLAY_HORIZONTAL", serviceToken).dataType(tableRowDataType).weight(1.0).end()
									.addTheme("THM_TABLE_ROW_CONTENT_WRAPPER", serviceToken).dataType(tableRowDataType).vcl(VisualControlType.GROUP).weight(1.0).end()
									.addTheme("THM_TABLE_ROW", serviceToken).dataType(tableRowDataType).weight(1.0).end()
									.addTheme("THM_TABLE_CONTENT", serviceToken).vcl(VisualControlType.GROUP).end()			
									.addTheme("THM_TABLE_ROW_CELL", serviceToken).vcl(VisualControlType.VCL_WRAPPER).end()			
								.end()
								.build();
				
			}else{
				
				System.out.println("it's a FRM_TABLE_HEADER");

				Validation tableCellValidation = new Validation("VLD_ANYTHING", "Anything", ".*");

				List<Validation> tableCellValidations = new ArrayList<>();
				tableCellValidations.add(tableCellValidation);

				ValidationList tableCellValidationList = new ValidationList();
				tableCellValidationList.setValidationList(tableCellValidations);

				DataType tableCellDataType = new DataType("DTT_TABLE_CELL_GRP", tableCellValidationList, "Table Cell Group",
						"");

				frame = Frame3.builder(frameCode)
								.addTheme("THM_TABLE_BORDER", serviceToken).end()
								.question(questionCode) // QUE_TEST_TABLE_HEADER_GRP
									.addTheme("THM_QUESTION_GRP_LABEL",serviceToken).vcl(VisualControlType.GROUP).dataType(tableCellDataType).end()
									.addTheme("THM_WIDTH_100_PERCENT_NO_INHERIT",serviceToken).vcl(VisualControlType.GROUP).end()
									.addTheme("THM_TABLE_ROW_CELL",serviceToken).dataType(tableCellDataType).vcl(VisualControlType.GROUP_WRAPPER).end()			
									.addTheme("THM_DISPLAY_HORIZONTAL", serviceToken).weight(2.0).end()
									.addTheme("THM_TABLE_HEADER_CELL_WRAPPER",serviceToken).vcl(VisualControlType.VCL_WRAPPER).end()
									.addTheme("THM_TABLE_HEADER_CELL_GROUP_LABEL",serviceToken).vcl(VisualControlType.GROUP_LABEL).end()
									.addTheme("THM_DISPLAY_VERTICAL",serviceToken).dataType(tableCellDataType).weight(1.0).end()			
								.end()
								.build();			

			}
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		QDataBaseEntityMessage msg = FrameUtils2.toMessage(frame, serviceToken, askMsgs);
		msg.setReplace(true);

		String rootFrameCode = frameCode;

		for (BaseEntity targetFrame : msg.getItems()) {
			if (targetFrame.getCode().equals(questionCode)) {

				log.info("ShowFrame : Found Targeted Frame BaseEntity : " + targetFrame);

				/* Adding the links in the targeted BaseEntity */
				Attribute attribute = new Attribute("LNK_ASK", "LNK_ASK", new DataType(String.class));

				for (BaseEntity sourceFrame : msg.getItems()) {
					if (sourceFrame.getCode().equals(rootFrameCode)) {

						log.info("ShowFrame : Found Source Frame BaseEntity : " + sourceFrame);
						EntityEntity entityEntity = new EntityEntity(sourceFrame, targetFrame, attribute, 1.0,
								"CENTRE");
						// Set<EntityEntity> entEntList = sourceFrame.getLinks();
						// entEntList.add(entityEntity);
						sourceFrame.getLinks().add(entityEntity);
						sourceFrame.setName(searchBE.getName());
						/* Adding Frame to Targeted Frame BaseEntity Message */
					//	msg.add(targetFrame);
						break;
					}
				}
				break;
			}
		}
		msg.setToken(userToken.getToken());
		return msg;
	}

	public static void performSearch(GennyToken serviceToken, BaseEntityUtils beUtils, SearchEntity searchBE) {
		TableUtilsTest tableUtils = new TableUtilsTest(beUtils);

		// Send out Search Results

		QDataBaseEntityMessage msg = tableUtils.fetchSearchResults(searchBE, beUtils.getGennyToken());
		Map<String, String> columns = tableUtils.getTableColumns(searchBE);

		VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg));

		// Now Send out Table Header Ask and Question
		TableData tableData = tableUtils.generateTableAsks(searchBE, beUtils.getGennyToken(), msg);
		Ask headerAsk = tableData.getAsk();
		Ask[] askArray = new Ask[1];
		askArray[0] = headerAsk;
		QDataAskMessage headerAskMsg = new QDataAskMessage(askArray);
		headerAskMsg.setToken(beUtils.getGennyToken().getToken());
		headerAskMsg.setReplace(true);
		//VertxUtils.writeMsg("webcmds", JsonUtils.toJson(headerAskMsg));
		
		// create virtual context

		// Now link the FRM_TABLE_HEADER to that new Question
		String headerAskCode = headerAsk.getQuestionCode();
		Set<QDataAskMessage> askMsgs = new HashSet<QDataAskMessage>();
		QDataBaseEntityMessage msg2 = null;
		msg2 = TableUtilsTest.changeQuestion(searchBE,"FRM_TABLE_HEADER", headerAskCode, serviceToken, beUtils.getGennyToken(),
				askMsgs);
		msg2.setToken(beUtils.getGennyToken().getToken());
		msg2.setReplace(true);
		VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg2));
		
		QDataAskMessage[] askMsgArr = askMsgs.toArray(new QDataAskMessage[0]);
		ContextList contextList = askMsgArr[0].getItems()[0].getContextList();
		headerAskMsg.getItems()[0].setContextList(contextList);

		VertxUtils.writeMsg("webcmds", JsonUtils.toJson(headerAskMsg));
		
		askMsgs.clear();

		/* Now to display the rows */
		
		Type setType = new TypeToken<Set<QDataAskMessage>>() {
		}.getType();

		String askMsgs2Str = VertxUtils.getObject(beUtils.getGennyToken().getRealm(), "", "FRM_TABLE_CONTENT_ASKS",
				String.class, beUtils.getGennyToken().getToken());

		Set<QDataAskMessage> askMsgs2 = JsonUtils.fromJson(askMsgs2Str, setType);
		QDataAskMessage[] askMsg2Array = askMsgs2.stream().toArray(QDataAskMessage[]::new);
		ContextList rowsContextList = askMsg2Array[0].getItems()[0].getContextList();
		
		List<BaseEntity> rowList = Arrays.asList(msg.getItems());
		List<Ask> rowAsks = generateQuestions(beUtils.getGennyToken(), beUtils, rowList, columns,
				beUtils.getGennyToken().getUserCode());

		/* converting rowAsks list to array */
		Ask[] rowAsksArr = rowAsks.stream().toArray(Ask[]::new);

		/* Now send out the question rows and themes etc */

		/* Link row asks to a single ask: QUE_TEST_TABLE_RESULTS_GRP */
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));
		Question tableResultQuestion = new Question("QUE_TABLE_RESULTS_GRP", "Table Results Question Group",
				questionAttribute, true);
		Ask tableResultAsk = new Ask(tableResultQuestion, beUtils.getGennyToken().getUserCode(),
				beUtils.getGennyToken().getUserCode());
		tableResultAsk.setChildAsks(rowAsksArr);
		tableResultAsk.setContextList(rowsContextList);
		
		askMsgs.add(new QDataAskMessage(tableResultAsk));
		
		/* link single ask QUE_TEST_TABLE_RESULTS_GRP to FRM_TABLE_CONTENT ? */
		String tableResultAskCode = tableResultAsk.getQuestionCode();
		Set<QDataAskMessage> tableResultAskMsgs = new HashSet<QDataAskMessage>();
		
		QDataBaseEntityMessage msg3 = null;
		msg3 = TableUtilsTest.changeQuestion(searchBE,"FRM_TABLE_CONTENT", tableResultAskCode, serviceToken, beUtils.getGennyToken(),
				tableResultAskMsgs);
		msg3.setToken(beUtils.getGennyToken().getToken());
		msg3.setReplace(true);


		for (QDataAskMessage askMsg : tableResultAskMsgs) {
			askMsg.setToken(beUtils.getGennyToken().getToken());
			//askMsg.getItems()[0] = headerAsk;
			askMsg.setReplace(true);
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson(askMsg));
		}

		VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg3));


	}

	/*
	 * Generate List of asks from a SearchEntity
	 */
	public static List<Ask> generateQuestions(GennyToken userToken, BaseEntityUtils beUtils, List<BaseEntity> bes,
			Map<String, String> columns, String targetCode) {

		/* initialize an empty ask list */
		List<Ask> askList = new ArrayList<>();
		List<QDataBaseEntityMessage> themeMsgList = new ArrayList<QDataBaseEntityMessage>();
		TableUtilsTest tableUtils = new TableUtilsTest(beUtils);

		if (columns != null) {
			if (bes != null && bes.isEmpty() == false) {

				/* we grab the theme for table actions */
				BaseEntity visualBaseEntity = beUtils.getBaseEntityByCode("THM_TABLE_ACTIONS_VISUAL_CONTROL");

				/* we grab the icons for actions */
				BaseEntity viewIconBe = beUtils.getBaseEntityByCode("ICN_VIEW");
				BaseEntity editIconBe = beUtils.getBaseEntityByCode("ICN_EDIT");
				BaseEntity deleteIconBe = beUtils.getBaseEntityByCode("ICN_DELETE");
				BaseEntity moreVerticalIconBe = beUtils.getBaseEntityByCode("ICN_MORE_VERTICAL");
				BaseEntity selectableTheme = beUtils.getBaseEntityByCode("THM_SELECTABLE");
				/*
				 * BaseEntity tableCellUnInheritableTheme =
				 * ContextUtils.getTableCellUnInheritableTheme();
				 */
				BaseEntity tableCellUnInheritableTheme = beUtils.getBaseEntityByCode("THM_TABLE_CELL_UNINHERITABLE");
				BaseEntity noFlexTheme = ContextUtils.getNoFlexTheme();

				List<BaseEntity> bes2 = new ArrayList<BaseEntity>();
				bes2.add(visualBaseEntity);
				bes2.add(viewIconBe);
				bes2.add(editIconBe);
				bes2.add(deleteIconBe);
				bes2.add(selectableTheme);
				bes2.add(tableCellUnInheritableTheme);
				bes2.add(noFlexTheme);
				bes2.add(moreVerticalIconBe);

				QDataBaseEntityMessage msg = new QDataBaseEntityMessage(bes2);
				msg.setToken(beUtils.getGennyToken().getToken());

				VertxUtils.writeMsg("webcmds", JsonUtils.toJson((msg)));

				log.info(visualBaseEntity.getCode());
				log.info(viewIconBe.getCode());
				log.info(editIconBe.getCode());
				log.info(deleteIconBe.getCode());
				log.info(selectableTheme.getCode());
				log.info(tableCellUnInheritableTheme.getCode());
				log.info(noFlexTheme.getCode());
				log.info(moreVerticalIconBe.getCode());

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

						/* if the column is an actions column */
						if (attributeCode.equals("QUE_TABLE_ACTIONS_GRP")) {

							/* creating actions ask group */
							Question actionGroupQuestion = new Question("QUE_" + be.getCode() + "_TABLE_ACTIONS_GRP",
									"Actions", attr, true);
							Ask childAsk = new Ask(actionGroupQuestion, targetCode, be.getCode());

							/* creating child ask for actions */
							Attribute actionAttribute = RulesUtils.attributeMap.get("PRI_EVENT");

							Question viewQues = new Question("QUE_VIEW_" + be.getCode(), "View", actionAttribute, true);
							Question editQues = new Question("QUE_EDIT_" + be.getCode(), "Edit", actionAttribute, true);
							Question deleteQues = new Question("QUE_DELETE_" + be.getCode(), "Delete", actionAttribute,
									true);

							List<Ask> actionChildAsks = new ArrayList<>();

							Ask viewAsk = new Ask(viewQues, userToken.getUserCode(), be.getCode());
							Ask editAsk = new Ask(editQues, userToken.getUserCode(), be.getCode());
							Ask deleteAsk = new Ask(deleteQues, userToken.getUserCode(), be.getCode());

							/* set the contexts to the ask */
							viewAsk = tableUtils.createVirtualContext(viewAsk, viewIconBe, ContextType.ICON,
									VisualControlType.VCL_ICON, themeMsgList);
							viewAsk = tableUtils.createVirtualContext(viewAsk, visualBaseEntity, ContextType.THEME,
									VisualControlType.VCL_INPUT, themeMsgList);

							editAsk = tableUtils.createVirtualContext(editAsk, editIconBe, ContextType.ICON,
									VisualControlType.VCL_ICON, themeMsgList);
							editAsk = tableUtils.createVirtualContext(editAsk, visualBaseEntity, ContextType.THEME,
									VisualControlType.VCL_INPUT, themeMsgList);

							deleteAsk = tableUtils.createVirtualContext(deleteAsk, deleteIconBe, ContextType.ICON,
									VisualControlType.VCL_ICON, themeMsgList);
							deleteAsk = tableUtils.createVirtualContext(deleteAsk, visualBaseEntity, ContextType.THEME,
									VisualControlType.VCL_INPUT, themeMsgList);

							actionChildAsks.add(viewAsk);
							actionChildAsks.add(editAsk);
							actionChildAsks.add(deleteAsk);

							/* converting asks list to array */
							Ask[] actionChildAsksArr = actionChildAsks.stream().toArray(Ask[]::new);
							childAsk.setChildAsks(actionChildAsksArr);

							tableUtils.createVirtualContext(childAsk, tableCellUnInheritableTheme, ContextType.THEME,
									themeMsgList);
							tableUtils.createVirtualContext(childAsk, noFlexTheme, ContextType.THEME, themeMsgList);

							/* add the entityAttribute ask to list */
							childAskList.add(childAsk);

						} else if (attributeCode.equals("QUE_CARD_GRP")) {

							/* we get the themes */
							BaseEntity horizontalThemeBe = beUtils.getBaseEntityByCode("THM_DISPLAY_HORIZONTAL");
							BaseEntity verticalThemeBe = beUtils.getBaseEntityByCode("THM_DISPLAY_VERTICAL");
							BaseEntity imageFitThemeBe = beUtils.getBaseEntityByCode("THM_IMAGE_FIT");
							BaseEntity elementHeightFitThemeBe = ContextUtils.getElementHeightFitTheme();
							BaseEntity cardContainerThemeBe = ContextUtils.getCardContainerTheme();

							QBulkMessage bulkMsg = new QBulkMessage();

							QDataBaseEntityMessage elementHeightFitThemeBeMsg = new QDataBaseEntityMessage(
									elementHeightFitThemeBe);
							QDataBaseEntityMessage cardContainerThemeBeMsg = new QDataBaseEntityMessage(
									cardContainerThemeBe);

							bulkMsg.add(elementHeightFitThemeBeMsg);
							bulkMsg.add(cardContainerThemeBeMsg);
							VertxUtils.writeMsg("webcmds", JsonUtils.toJson((bulkMsg)));

							VertxUtils.writeMsg("webcmds", JsonUtils.toJson((horizontalThemeBe)));
							VertxUtils.writeMsg("webcmds", JsonUtils.toJson((verticalThemeBe)));
							VertxUtils.writeMsg("webcmds", JsonUtils.toJson((imageFitThemeBe)));
							log.info(elementHeightFitThemeBe);
							log.info(cardContainerThemeBe);

							/* we get the attributes */
							Attribute quesGrpAttr = RulesUtils.attributeMap.get("QQQ_QUESTION_GROUP");
							Attribute imgAttr = RulesUtils.attributeMap.get("PRI_IMAGE_URL");
							Attribute fNameAttr = RulesUtils.attributeMap.get("PRI_FIRSTNAME");
							Attribute lNameAttr = RulesUtils.attributeMap.get("PRI_LASTNAME");
							Attribute emailAttr = RulesUtils.attributeMap.get("PRI_EMAIL");
							Attribute mobileAttr = RulesUtils.attributeMap.get("PRI_MOBILE");
							Attribute addressAttr = RulesUtils.attributeMap.get("PRI_ADDRESS_FULL");
							Attribute actionAttribute = RulesUtils.attributeMap.get("PRI_EVENT");

							/* creating card group */
							Question cardGrpQues = new Question("QUE_CARD_GRP", "Card Group", quesGrpAttr, true);
							Ask cardGrpAsk = new Ask(cardGrpQues, targetCode, be.getCode());
							cardGrpAsk = tableUtils.createVirtualContext(cardGrpAsk, verticalThemeBe, ContextType.THEME,
									themeMsgList);
							cardGrpAsk = tableUtils.createVirtualContext(cardGrpAsk, cardContainerThemeBe,
									ContextType.THEME, themeMsgList);
							cardGrpAsk = tableUtils.createVirtualContext(cardGrpAsk, elementHeightFitThemeBe,
									ContextType.THEME, VisualControlType.VCL_WRAPPER, themeMsgList);

							/* creating card-main group */
							Question cardMainGrpQues = new Question("QUE_CARD_MAIN_GRP", "Card Main Group", quesGrpAttr,
									true);
							Ask cardMainGrpAsk = new Ask(cardMainGrpQues, targetCode, be.getCode());
							cardMainGrpAsk = tableUtils.createVirtualContext(cardMainGrpAsk, horizontalThemeBe,
									ContextType.THEME, themeMsgList);

							/* creating card-secondary group */
							Question cardSecondaryGrpQues = new Question("QUE_CARD_SECONDARY_GRP",
									"Card Secondary Group", quesGrpAttr, true);
							Ask cardSecondaryGrpAsk = new Ask(cardSecondaryGrpQues, targetCode, be.getCode());
							cardSecondaryGrpAsk = tableUtils.createVirtualContext(cardSecondaryGrpAsk,
									horizontalThemeBe, ContextType.THEME, themeMsgList);

							/* creating left-card group */
							Question cardLeftGrpQues = new Question("QUE_CARD_LEFT_GRP", "Card Left Group", quesGrpAttr,
									true);
							Ask cardLeftGrpAsk = new Ask(cardLeftGrpQues, targetCode, be.getCode());
							cardLeftGrpAsk = tableUtils.createVirtualContext(cardLeftGrpAsk, verticalThemeBe,
									ContextType.THEME, themeMsgList);

							/* image */
							Question imgQues = new Question("QUE_IMAGE", "Image Question", imgAttr, true);
							Ask imgAsk = new Ask(imgQues, targetCode, be.getCode());
							imgAsk = tableUtils.createVirtualContext(imgAsk, imageFitThemeBe, ContextType.THEME,
									themeMsgList);
							Ask[] imgAskArr = { imgAsk };

							/* link asks */
							cardLeftGrpAsk.setChildAsks(imgAskArr);

							/* creating centre-card group */
							Question cardCentreGrpQues = new Question("QUE_CARD_CENTRE_GRP", "Card Centre Group",
									quesGrpAttr, true);
							Ask cardCentreGrpAsk = new Ask(cardCentreGrpQues, targetCode, be.getCode());
							cardCentreGrpAsk = tableUtils.createVirtualContext(cardCentreGrpAsk, verticalThemeBe,
									ContextType.THEME, themeMsgList);

							/* name-group */
							Question nameGrpQues = new Question("QUE_NAME_GRP", "Name Group", quesGrpAttr, true);
							Ask nameGrpAsk = new Ask(nameGrpQues, targetCode, be.getCode());
							nameGrpAsk = tableUtils.createVirtualContext(nameGrpAsk, horizontalThemeBe,
									ContextType.THEME, themeMsgList);

							/* first-name */
							Question fNameQues = new Question("QUE_FIRSTNAME", "First Name", fNameAttr, true);
							Ask fNameAsk = new Ask(fNameQues, targetCode, be.getCode());

							/* last-name */
							Question lNameQues = new Question("QUE_LASTNAME", "Last Name", lNameAttr, true);
							Ask lNameAsk = new Ask(lNameQues, targetCode, be.getCode());

							/* link asks */
							List<Ask> nameChildAsks = new ArrayList<>();
							nameChildAsks.add(fNameAsk);
							nameChildAsks.add(lNameAsk);
							Ask[] nameChildAsksArr = nameChildAsks.stream().toArray(Ask[]::new);
							nameGrpAsk.setChildAsks(nameChildAsksArr);

							/* email */
							Question emailQues = new Question("QUE_EMAIL", "Email Question", emailAttr, true);
							Ask emailAsk = new Ask(emailQues, targetCode, be.getCode());

							/* mobile */
							Question mobileQues = new Question("QUE_MOBILE", "Mobile Question", mobileAttr, true);
							Ask mobileAsk = new Ask(mobileQues, targetCode, be.getCode());

							/* address */
							Question addressQues = new Question("QUE_ADDRESS", "Address Question", addressAttr, true);
							Ask addressAsk = new Ask(addressQues, targetCode, be.getCode());

							/* link asks */
							List<Ask> cardCentreChildAsks = new ArrayList<>();
							cardCentreChildAsks.add(nameGrpAsk);
							cardCentreChildAsks.add(emailAsk);
							cardCentreChildAsks.add(mobileAsk);
							/* cardCentreChildAsks.add(addressAsk); */

							Ask[] cardCentreChildAsksArr = cardCentreChildAsks.stream().toArray(Ask[]::new);
							cardCentreGrpAsk.setChildAsks(cardCentreChildAsksArr);

							/* creating right-card group */
							Question cardRightGrpQues = new Question("QUE_CARD_RIGHT_GRP", "Card Right Group",
									quesGrpAttr, true);
							Ask cardRightGrpAsk = new Ask(cardRightGrpQues, targetCode, be.getCode());
							cardRightGrpAsk = tableUtils.createVirtualContext(cardRightGrpAsk, verticalThemeBe,
									ContextType.THEME, themeMsgList);

							/* options-menu */
							Question optionsQues = new Question("QUE_CARD_OPTIONS" + be.getCode(), "options",
									actionAttribute, true);
							Ask optionsAsk = new Ask(optionsQues, userToken.getUserCode(), be.getCode());
							optionsAsk = tableUtils.createVirtualContext(optionsAsk, moreVerticalIconBe,
									ContextType.ICON, VisualControlType.VCL_ICON, themeMsgList);
							optionsAsk = tableUtils.createVirtualContext(optionsAsk, visualBaseEntity,
									ContextType.THEME, VisualControlType.VCL_INPUT, themeMsgList);

							Ask[] optionsAskArr = { optionsAsk };
							cardRightGrpAsk.setChildAsks(optionsAskArr);

							/* link asks */
							List<Ask> cardMainGrpAskChildAsks = new ArrayList<>();
							cardMainGrpAskChildAsks.add(cardLeftGrpAsk);
							cardMainGrpAskChildAsks.add(cardCentreGrpAsk);
							cardMainGrpAskChildAsks.add(cardRightGrpAsk);

							Ask[] cardMainGrpAskChildAsksArr = cardMainGrpAskChildAsks.stream().toArray(Ask[]::new);

							/* link asks */
							List<Ask> cardSecondaryGrpAskChildAsks = new ArrayList<>();
							cardSecondaryGrpAskChildAsks.add(cardLeftGrpAsk);
							cardSecondaryGrpAskChildAsks.add(cardCentreGrpAsk);
							cardSecondaryGrpAskChildAsks.add(cardRightGrpAsk);

							Ask[] cardSecondaryGrpAskChildAsksArr = cardSecondaryGrpAskChildAsks.stream()
									.toArray(Ask[]::new);

							cardGrpAsk.setChildAsks(cardMainGrpAskChildAsksArr);
							cardGrpAsk.setChildAsks(cardSecondaryGrpAskChildAsksArr);

							childAskList.add(cardGrpAsk);

						} else {

							Question childQuestion = new Question("QUE_" + attributeCode + "_" + be.getCode(),
									attributeName, attr, true);
							Ask childAsk = new Ask(childQuestion, targetCode, be.getCode());

							/* add the entityAttribute ask to list */
							childAskList.add(childAsk);
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

					/* apply selectable theme to each parent ask group */
					tableUtils.createVirtualContext(parentAsk, selectableTheme, ContextType.THEME,
							VisualControlType.VCL_INPUT, themeMsgList);

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

}