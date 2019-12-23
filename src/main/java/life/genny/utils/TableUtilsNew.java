package life.genny.utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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
import life.genny.models.ThemeAttributeType;
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
import life.genny.rules.QRules;
import life.genny.utils.ContextUtils;

public class TableUtilsNew {

	BaseEntityUtils beUtils = null;

	public TableUtilsNew(BaseEntityUtils beUtils) {
		this.beUtils = beUtils;
	}

	/* generates all the contextListMap for card */
	public Map<String, ContextList> getTableContextListMap(Map<String, ContextList> contextListMap,
			GennyToken serviceToken) {

		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);
		TableUtilsNew bucketUtils = new TableUtilsNew(beUtils);

		try {

			// get the themes from cache

			/* table-header themes */
			Theme THM_QUESTION_GRP_LABEL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_QUESTION_GRP_LABEL",
					Theme.class, serviceToken.getToken());

			Theme THM_WIDTH_100_PERCENT_NO_INHERIT = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_WIDTH_100_PERCENT_NO_INHERIT",
					Theme.class, serviceToken.getToken());

			Theme THM_DISPLAY_HORIZONTAL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_DISPLAY_HORIZONTAL",
					Theme.class, serviceToken.getToken());

			Theme THM_TABLE_HEADER_CELL_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_TABLE_HEADER_CELL_WRAPPER",
					Theme.class, serviceToken.getToken());

			Theme THM_TABLE_HEADER_CELL_GROUP_LABEL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_TABLE_HEADER_CELL_GROUP_LABEL",
					Theme.class, serviceToken.getToken());

			Theme THM_DISPLAY_VERTICAL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_DISPLAY_VERTICAL",
					Theme.class, serviceToken.getToken());

					

			/* table-content themes */
			
			Theme THM_TABLE_BORDER = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_TABLE_BORDER",
					Theme.class, serviceToken.getToken());

			Theme THM_TABLE_ROW_CONTENT_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_TABLE_ROW_CONTENT_WRAPPER",
					Theme.class, serviceToken.getToken());

			Theme THM_TABLE_ROW = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_TABLE_ROW",
					Theme.class, serviceToken.getToken());

			Theme THM_TABLE_CONTENT = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_TABLE_CONTENT",
					Theme.class, serviceToken.getToken());

			Theme THM_TABLE_ROW_CELL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_TABLE_ROW_CELL",
					Theme.class, serviceToken.getToken());

				
			/* prepare context for progress bar */
//			Context progressVclInputContext = new Context(ContextType.THEME,
//					bucketUtils.getThemeBe(THM_PROGRESS_VCL_INPUT), VisualControlType.VCL_INPUT, 1.0);
//			progressVclInputContext.setDataType("Progress");
//			Context progressInputWrapperContext = new Context(ContextType.THEME,
//					bucketUtils.getThemeBe(THM_PROGRESS_INPUT_WRAPPER), VisualControlType.INPUT_WRAPPER, 1.0);
//			progressInputWrapperContext.setDataType("Progress");
//
//			/* cardContext */
//			List<Context> cardContext = new ArrayList<>();
//			cardContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DISPLAY_HORIZONTAL),
//					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
//			cardContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_CARD),
//					VisualControlType.GROUP_WRAPPER, 1.0));
//
//			/* cardStatusContext */
//			List<Context> cardStatusContext = new ArrayList<>();
//			cardStatusContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_FLEX_ONE),
//					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
//			cardStatusContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_FLEX_ONE_INHERITABLE),
//					VisualControlType.VCL_WRAPPER, 1.0));
//			cardStatusContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DROPDOWN_ICON_ALT),
//					VisualControlType.GROUP_ICON, 1.0));
//
//			/* cardMainContext */
//			List<Context> cardMainContext = new ArrayList<>();
//			cardMainContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DISPLAY_VERTICAL),
//					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
//			cardMainContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_FLEX_ONE),
//					VisualControlType.GROUP_WRAPPER, 1.0));
//
//			/* cardContentContext */
//			List<Context> cardContentContext = new ArrayList<>();
//			cardContentContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DISPLAY_HORIZONTAL),
//					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
//
//			/* cardLeftContext */
//			List<Context> cardLeftContext = new ArrayList<>();
//			cardLeftContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_HEADER_PROFILE_PICTURE),
//					VisualControlType.INPUT_SELECTED, 1.0));
//			cardLeftContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_BORDER_RADIUS_50),
//					VisualControlType.INPUT_FIELD, 1.0));
//			cardLeftContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_PROFILE_IMAGE),
//					VisualControlType.INPUT_SELECTED, 1.0));
//			cardLeftContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_IMAGE_PLACEHOLDER_PERSON),
//					VisualControlType.INPUT_PLACEHOLDER, 1.0));
//
//			/* cardCentreContext */
//			List<Context> cardCentreContext = new ArrayList<>();
//			cardCentreContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DISPLAY_VERTICAL),
//					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
//			cardCentreContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_FLEX_ONE),
//					VisualControlType.GROUP_WRAPPER, 1.0));
//
//			/* cardRightContext */
//			List<Context> cardRightContext = new ArrayList<>();
//			cardRightContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DISPLAY_VERTICAL),
//					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
//			cardRightContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DROPDOWN_BEHAVIOUR_GENNY),
//					VisualControlType.GROUP, 1.0));
//			cardRightContext.add(new Context(ContextType.THEME,
//					bucketUtils.getThemeBe(THM_DROPDOWN_PROFILE_BEHAVIOUR_GENNY), VisualControlType.GROUP, 1.0));
//			cardRightContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_PROJECT_COLOR_SURFACE),
//					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
//			cardRightContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DROPDOWN_ICON_MORE_HORIZ),
//					VisualControlType.GROUP_ICON, 1.0));
//
//			/* cardBottomContext */
//			List<Context> cardBottomContext = new ArrayList<>();
//			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_JUSTIFY_CONTENT_CENTRE),
//					VisualControlType.GROUP_CLICKABLE_WRAPPER, 1.0));
//			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_EXPANDABLE),
//					VisualControlType.GROUP, 1.0));
//			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_WIDTH_100_PERCENT),
//					VisualControlType.GROUP, 1.0));
//			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_PADDING_X_10),
//					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
//			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_EXPANDABLE_ICON),
//					VisualControlType.GROUP_ICON, 1.0));
//			cardBottomContext.add(progressVclInputContext);
//			cardBottomContext.add(progressInputWrapperContext);
//
//			contextListMap.put("QUE_CARD_APPLICATION_TEMPLATE_GRP", new ContextList(cardContext));
//			contextListMap.put("QUE_CARD_STATUS_GRP", new ContextList(cardStatusContext));
//			contextListMap.put("QUE_CARD_MAIN_GRP", new ContextList(cardMainContext));
//			contextListMap.put("QUE_CARD_CONTENT_GRP", new ContextList(cardContentContext));
//			contextListMap.put("QUE_CARD_LEFT_GRP", new ContextList(cardLeftContext));
//			contextListMap.put("QUE_CARD_CENTRE_GRP", new ContextList(cardCentreContext));
//			contextListMap.put("QUE_CARD_RIGHT_GRP", new ContextList(cardRightContext));
//			contextListMap.put("QUE_CARD_BOTTOM_GRP", new ContextList(cardBottomContext));

		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return contextListMap;

	}
	/* generates all the contextListMap for card */
	public Map<String, ContextList> getCardContextListMap(Map<String, ContextList> contextListMap,
			GennyToken serviceToken) {

		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);
		TableUtilsNew bucketUtils = new TableUtilsNew(beUtils);

		try {

			// get the themes from cache
			Theme THM_DISPLAY_VERTICAL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_DISPLAY_VERTICAL",
					Theme.class, serviceToken.getToken());

			Theme THM_JUSTIFY_CONTENT_FLEX_START = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_JUSTIFY_CONTENT_FLEX_START", Theme.class, serviceToken.getToken());

			Theme THM_CARD = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_CARD", Theme.class,
					serviceToken.getToken());

			Theme THM_DISPLAY_HORIZONTAL = Theme.builder("THM_DISPLAY_HORIZONTAL").addAttribute().flexDirection("row")
					.end().addAttribute(ThemeAttributeType.PRI_IS_INHERITABLE, false).end().build();

			Theme THM_DROPDOWN_ICON_ALT = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_DROPDOWN_ICON_ALT",
					Theme.class, serviceToken.getToken());

			Theme THM_DROPDOWN_BEHAVIOUR_GENNY = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_DROPDOWN_BEHAVIOUR_GENNY", Theme.class, serviceToken.getToken());

			Theme THM_BACKGROUND_NONE = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BACKGROUND_NONE",
					Theme.class, serviceToken.getToken());

			Theme THM_DROPDOWN_PROFILE_BEHAVIOUR_GENNY = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_DROPDOWN_PROFILE_BEHAVIOUR_GENNY", Theme.class, serviceToken.getToken());

			Theme THM_DROPDOWN_HEADER_WRAPPER_GENNY = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_DROPDOWN_HEADER_WRAPPER_GENNY", Theme.class, serviceToken.getToken());

			Theme THM_DROPDOWN_GROUP_LABEL_GENNY = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_DROPDOWN_GROUP_LABEL_GENNY", Theme.class, serviceToken.getToken());

			Theme THM_DROPDOWN_CONTENT_WRAPPER_GENNY = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_DROPDOWN_CONTENT_WRAPPER_GENNY", Theme.class, serviceToken.getToken());

			Theme THM_BOX_SHADOW_SM = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BOX_SHADOW_SM",
					Theme.class, serviceToken.getToken());

			Theme THM_DROPDOWN_VCL_GENNY = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_DROPDOWN_VCL_GENNY",
					Theme.class, serviceToken.getToken());

			Theme THM_IMAGE_PLACEHOLDER = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_IMAGE_PLACEHOLDER",
					Theme.class, serviceToken.getToken());
					
			Theme THM_HEADER_PROFILE_PICTURE = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_HEADER_PROFILE_PICTURE", Theme.class, serviceToken.getToken());
					
			Theme THM_PROGRESS_VCL_INPUT = Theme.builder("THM_PROGRESS_VCL_INPUT")
					.addAttribute()
						.sections(12)
						.color("green")
						.backgroundColor("green")
					.end()
					.addAttribute(ThemeAttributeType.PRI_IS_INHERITABLE, false).end()
					.build();  

			Theme THM_PROGRESS_INPUT_WRAPPER = Theme.builder("THM_PROGRESS_INPUT_WRAPPER")
								.addAttribute()
									.padding(10)
								.end()
								.addAttribute(ThemeAttributeType.PRI_IS_INHERITABLE, false).end()
								.build();   



			Theme THM_BORDER_RADIUS_50 = Theme.builder("THM_BORDER_RADIUS_50")
					.addAttribute()
						.borderRadius(50)
					.end()
					.build();
					
			Theme THM_EXPANDABLE = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_EXPANDABLE", Theme.class,
					serviceToken.getToken());
					
			Theme THM_WIDTH_100_PERCENT = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_WIDTH_100_PERCENT",
					Theme.class, serviceToken.getToken());
					
			Theme THM_JUSTIFY_CONTENT_CENTRE = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_JUSTIFY_CONTENT_CENTRE", Theme.class, serviceToken.getToken());
					
			Theme THM_IMAGE_PLACEHOLDER_PERSON = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_IMAGE_PLACEHOLDER_PERSON", Theme.class, serviceToken.getToken());
					
			Theme THM_PROFILE_IMAGE = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_PROFILE_IMAGE",
					Theme.class, serviceToken.getToken());
					
			// Theme THM_PROJECT_COLOR_SURFACE = VertxUtils.getObject(serviceToken.getRealm(), "",
			// 		"THM_PROJECT_COLOR_SURFACE", Theme.class, serviceToken.getToken());
			
			Theme THM_PROJECT_COLOR_SURFACE = Theme.builder("THM_PROJECT_COLOR_SURFACE")
					.addAttribute()
						.backgroundColor("#FFFFFF")
						.color("#000000")
					.end()
					.build();  
					
			Theme THM_PADDING_X_10 = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_PADDING_X_10", Theme.class,
					serviceToken.getToken());
					
			Theme THM_FLEX_ONE = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_FLEX_ONE", Theme.class,
					serviceToken.getToken());
					
			Theme THM_FLEX_ONE_INHERITABLE = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_FLEX_ONE_INHERITABLE", Theme.class, serviceToken.getToken());

			Theme THM_DROPDOWN_ICON_MORE_HORIZ = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_DROPDOWN_ICON_MORE_HORIZ", Theme.class, serviceToken.getToken());
			
			Theme THM_EXPANDABLE_ICON = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_EXPANDABLE_ICON", Theme.class, serviceToken.getToken());


			/* prepare context for progress bar */
			Context progressVclInputContext = new Context(ContextType.THEME,
					bucketUtils.getThemeBe(THM_PROGRESS_VCL_INPUT), VisualControlType.VCL_INPUT, 1.0);
			progressVclInputContext.setDataType("Progress");
			Context progressInputWrapperContext = new Context(ContextType.THEME,
					bucketUtils.getThemeBe(THM_PROGRESS_INPUT_WRAPPER), VisualControlType.INPUT_WRAPPER, 1.0);
			progressInputWrapperContext.setDataType("Progress");

			/* cardContext */
			List<Context> cardContext = new ArrayList<>();
			cardContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DISPLAY_HORIZONTAL),
					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
			cardContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_CARD),
					VisualControlType.GROUP_WRAPPER, 1.0));

			/* cardStatusContext */
			List<Context> cardStatusContext = new ArrayList<>();
			cardStatusContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_FLEX_ONE),
					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
			cardStatusContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_FLEX_ONE_INHERITABLE),
					VisualControlType.VCL_WRAPPER, 1.0));
			cardStatusContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DROPDOWN_ICON_ALT),
					VisualControlType.GROUP_ICON, 1.0));

			/* cardMainContext */
			List<Context> cardMainContext = new ArrayList<>();
			cardMainContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DISPLAY_VERTICAL),
					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
			cardMainContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_FLEX_ONE),
					VisualControlType.GROUP_WRAPPER, 1.0));

			/* cardContentContext */
			List<Context> cardContentContext = new ArrayList<>();
			cardContentContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DISPLAY_HORIZONTAL),
					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));

			/* cardLeftContext */
			List<Context> cardLeftContext = new ArrayList<>();
			cardLeftContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_HEADER_PROFILE_PICTURE),
					VisualControlType.INPUT_SELECTED, 1.0));
			cardLeftContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_BORDER_RADIUS_50),
					VisualControlType.INPUT_FIELD, 1.0));
			cardLeftContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_PROFILE_IMAGE),
					VisualControlType.INPUT_SELECTED, 1.0));
			cardLeftContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_IMAGE_PLACEHOLDER_PERSON),
					VisualControlType.INPUT_PLACEHOLDER, 1.0));

			/* cardCentreContext */
			List<Context> cardCentreContext = new ArrayList<>();
			cardCentreContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DISPLAY_VERTICAL),
					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
			cardCentreContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_FLEX_ONE),
					VisualControlType.GROUP_WRAPPER, 1.0));

			/* cardRightContext */
			List<Context> cardRightContext = new ArrayList<>();
			cardRightContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DISPLAY_VERTICAL),
					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
			cardRightContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DROPDOWN_BEHAVIOUR_GENNY),
					VisualControlType.GROUP, 1.0));
			cardRightContext.add(new Context(ContextType.THEME,
					bucketUtils.getThemeBe(THM_DROPDOWN_PROFILE_BEHAVIOUR_GENNY), VisualControlType.GROUP, 1.0));
			cardRightContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_PROJECT_COLOR_SURFACE),
					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
			cardRightContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DROPDOWN_ICON_MORE_HORIZ),
					VisualControlType.GROUP_ICON, 1.0));

			/* cardBottomContext */
			List<Context> cardBottomContext = new ArrayList<>();
			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_JUSTIFY_CONTENT_CENTRE),
					VisualControlType.GROUP_CLICKABLE_WRAPPER, 1.0));
			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_EXPANDABLE),
					VisualControlType.GROUP, 1.0));
			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_WIDTH_100_PERCENT),
					VisualControlType.GROUP, 1.0));
			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_PADDING_X_10),
					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_EXPANDABLE_ICON),
					VisualControlType.GROUP_ICON, 1.0));
			cardBottomContext.add(progressVclInputContext);
			cardBottomContext.add(progressInputWrapperContext);

			contextListMap.put("QUE_CARD_APPLICATION_TEMPLATE_GRP", new ContextList(cardContext));
			contextListMap.put("QUE_CARD_STATUS_GRP", new ContextList(cardStatusContext));
			contextListMap.put("QUE_CARD_MAIN_GRP", new ContextList(cardMainContext));
			contextListMap.put("QUE_CARD_CONTENT_GRP", new ContextList(cardContentContext));
			contextListMap.put("QUE_CARD_LEFT_GRP", new ContextList(cardLeftContext));
			contextListMap.put("QUE_CARD_CENTRE_GRP", new ContextList(cardCentreContext));
			contextListMap.put("QUE_CARD_RIGHT_GRP", new ContextList(cardRightContext));
			contextListMap.put("QUE_CARD_BOTTOM_GRP", new ContextList(cardBottomContext));

		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return contextListMap;

	}

	/* returns a card template */
	public Ask getCardTemplate(GennyToken serviceToken) {

		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);
		TableUtilsNew bucketUtils = new TableUtilsNew(beUtils);

		String sourceCode = "PER_SERVICE", targetCode = "PER_SERVICE";
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));

		try {

			// card ask
			Question cardQuestion = new Question("QUE_CARD_APPLICATION_TEMPLATE_GRP", "Card", questionAttribute, true);
			Ask cardAsk = new Ask(cardQuestion, sourceCode, targetCode);

				// status ask
				Question cardStatusQuestion = new Question("QUE_CARD_STATUS_GRP", "Card Status", questionAttribute, true);
				Ask cardStatusAsk = new Ask(cardStatusQuestion, sourceCode, targetCode);

				// main ask
				Question cardMainQuestion = new Question("QUE_CARD_MAIN_GRP", "Card Main", questionAttribute, true);
				Ask cardMainAsk = new Ask(cardMainQuestion, sourceCode, targetCode);
				cardMainAsk.setReadonly(true);

					// content ask
					Question cardContentQuestion = new Question("QUE_CARD_CONTENT_GRP", "Card Content", questionAttribute, true);
					Ask cardContentAsk = new Ask(cardContentQuestion, sourceCode, targetCode);

						// left ask
						Question cardLeftQuestion = new Question("QUE_CARD_LEFT_GRP", "Card Left", questionAttribute, true);
						Ask cardLeftAsk = new Ask(cardLeftQuestion, sourceCode, targetCode);

						// centre ask
						Question cardCentreQuestion = new Question("QUE_CARD_CENTRE_GRP", "Card Centre", questionAttribute, true);
						Ask cardCentreAsk = new Ask(cardCentreQuestion, sourceCode, targetCode);

						// right ask
						Question cardRightQuestion = new Question("QUE_CARD_RIGHT_GRP", "Card Right", questionAttribute, true);
						Ask cardRightAsk = new Ask(cardRightQuestion, sourceCode, targetCode);

							// forward ask
							Question cardForwardQuestion = new Question("QUE_FORWARD", "Forward", questionAttribute, true);
							Ask cardForwardAsk = new Ask(cardForwardQuestion, sourceCode, targetCode);
							
							// backward ask
							Question cardBackwardQuestion = new Question("QUE_BACKWARD", "Backward", questionAttribute, true);
							Ask cardBackwardAsk = new Ask(cardBackwardQuestion, sourceCode, targetCode);

							Ask[] cardRightChildAsks = { cardForwardAsk, cardBackwardAsk };
							cardRightAsk.setChildAsks(cardRightChildAsks);

					Ask[] cardContentChildAsks = { cardLeftAsk, cardCentreAsk, cardRightAsk };
					cardContentAsk.setChildAsks(cardContentChildAsks);

					// bottom ask
					Question cardBottomQuestion = new Question("QUE_CARD_BOTTOM_GRP", "Card Bottom", questionAttribute, true);
					Ask cardBottomAsk = new Ask(cardBottomQuestion, sourceCode, targetCode);

				Ask[] cardMainChildAsks = { cardContentAsk, cardBottomAsk };
				cardMainAsk.setChildAsks(cardMainChildAsks);					

			Ask[] cardChildAsks = { cardStatusAsk, cardMainAsk };
			cardAsk.setChildAsks(cardChildAsks);

			return cardAsk;

		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;

	}

	/* get the search BE related to bucket from cache */ 
	public List<SearchEntity> getBucketSearchBeListFromCache(GennyToken serviceToken) {

		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);
		List<SearchEntity> bucketSearchBeList = new ArrayList<SearchEntity>();

		try {
			SearchEntity SBE_AVAILABLE_INTERNS = VertxUtils.getObject(serviceToken.getRealm(), "",
							"SBE_AVAILABLE_INTERNS", SearchEntity.class, serviceToken.getToken());
			SearchEntity SBE_APPLIED_APPLICATIONS = VertxUtils.getObject(serviceToken.getRealm(), "",
					"SBE_APPLIED_APPLICATIONS", SearchEntity.class, serviceToken.getToken());
			SearchEntity SBE_SHORTLISTED_APPLICATIONS = VertxUtils.getObject(serviceToken.getRealm(), "",
					"SBE_SHORTLISTED_APPLICATIONS", SearchEntity.class, serviceToken.getToken());
			SearchEntity SBE_INTERVIEWED_APPLICATIONS = VertxUtils.getObject(serviceToken.getRealm(), "",
					"SBE_INTERVIEWED_APPLICATIONS", SearchEntity.class, serviceToken.getToken());
			SearchEntity SBE_OFFERED_APPLICATIONS = VertxUtils.getObject(serviceToken.getRealm(), "",
					"SBE_OFFERED_APPLICATIONS", SearchEntity.class, serviceToken.getToken());
			SearchEntity SBE_PLACED_APPLICATIONS = VertxUtils.getObject(serviceToken.getRealm(), "",
					"SBE_PLACED_APPLICATIONS", SearchEntity.class, serviceToken.getToken());
			SearchEntity SBE_INPROGRESS_APPLICATIONS = VertxUtils.getObject(serviceToken.getRealm(), "",
					"SBE_INPROGRESS_APPLICATIONS", SearchEntity.class, serviceToken.getToken());

			bucketSearchBeList.add(SBE_AVAILABLE_INTERNS);
			bucketSearchBeList.add(SBE_APPLIED_APPLICATIONS);
			bucketSearchBeList.add(SBE_SHORTLISTED_APPLICATIONS);
			bucketSearchBeList.add(SBE_INTERVIEWED_APPLICATIONS);
			bucketSearchBeList.add(SBE_OFFERED_APPLICATIONS);
			bucketSearchBeList.add(SBE_PLACED_APPLICATIONS);
			bucketSearchBeList.add(SBE_INPROGRESS_APPLICATIONS);

		} catch (Exception e) {

		}
		return bucketSearchBeList;
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

	/* generates the bucket-content ask */
	public Ask getBucketContentAsk(Map<String, ContextList> contextListMap, GennyToken serviceToken) {

		Attribute questionAttribute = RulesUtils.getAttribute("QQQ_QUESTION_GROUP", serviceToken.getToken());
		Question bucketContentQuestion = new Question("QUE_BUCKET_CONTENT_GRP", "", questionAttribute, true);
		Ask bucketContentAsk = new Ask(bucketContentQuestion, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");
		return bucketContentAsk;

	}

	/* generates the table-header ask */
	public Ask generateTableHeaderAsk(SearchEntity searchBe, Map<String, ContextList> contextListMap, GennyToken serviceToken){

		/* initialize empty ask list */
		List<Ask> asks = new ArrayList<>();

		/* get table columns */
		Map<String, String> columns = getTableColumns(searchBe);

		/* Validation for Search Attribute */
		Validation validation = new Validation("VLD_NON_EMPTY", "EmptyandBlankValues", "(?!^$|\\s+)");
		List<Validation> validations = new ArrayList<>();
		validations.add(validation);
		ValidationList searchValidationList = new ValidationList();
		searchValidationList.setValidationList(validations);

		/* get the attributes */
		Attribute eventAttribute = RulesUtils.getAttribute("PRI_SORT", serviceToken.getToken());
		Attribute questionAttribute = RulesUtils.getAttribute("QQQ_QUESTION_GROUP", serviceToken.getToken());
		Attribute tableCellAttribute = RulesUtils.getAttribute("QQQ_QUESTION_GROUP_TABLE_CELL", serviceToken.getToken());

		/* get the sort icon */
		Theme THM_ICON = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_ICON", Theme.class, serviceToken.getToken());
		BaseEntity ICN_SORT = beUtils.getBaseEntityByCode("ICN_SORT");

		/* loop through columns */
		for (Map.Entry<String, String> column : columns.entrySet()) {

			String attributeCode = column.getKey();
			String attributeName = column.getValue();

			Attribute searchAttribute = new Attribute(attributeCode, attributeName, new DataType("Text", searchValidationList, "Text"));

			/* Initialize Column Header Ask group */
			Question columnHeaderQues = new Question("QUE_" + attributeCode + "_GRP", attributeName, tableCellAttribute, true);
			Ask columnHeaderAsk = new Ask(columnHeaderQues, beUtils.getGennyToken().getUserCode(), searchBe.getCode());

			/* sort-ask */
			Question columnSortQues = new Question("QUE_SORT_" + attributeCode, "Sort", eventAttribute, false);
			Ask columnSortAsk = new Ask(columnSortQues, beUtils.getGennyToken().getUserCode(), searchBe.getCode());

			/* search-ask */
			Question columnSearchQues = new Question("QUE_SEARCH_" + attributeCode, "Search " + attributeName + "..", searchAttribute, false);
			Ask columnSearchAsk = new Ask(columnSearchQues, beUtils.getGennyToken().getUserCode(), searchBe.getCode());

			/* set the column child asks */
			Ask[] columnChildAsks = { columnSortAsk, columnSearchAsk };
			columnHeaderAsk.setChildAsks(columnChildAsks);

			asks.add(columnHeaderAsk);
		}

		/* Convert List to Array */
		Ask[] asksArray = asks.toArray(new Ask[0]);

		/* initialize table-header ask group */
		Question tableHeaderQuestion = new Question("QUE_TABLE_HEADER_" + searchBe.getCode() +"_GRP", searchBe.getName(), questionAttribute, true);
		Ask tableHeaderAsk = new Ask(tableHeaderQuestion, beUtils.getGennyToken().getUserCode(), searchBe.getCode());

		tableHeaderAsk.setChildAsks(asksArray);
		tableHeaderAsk.setName(searchBe.getName());

		/* sortContextList context */
		List<Context> sortContextList = new ArrayList<>();
		sortContextList.add(new Context(ContextType.THEME, this.getThemeBe(THM_ICON), VisualControlType.VCL, 1.0));
		sortContextList.add(new Context(ContextType.ICON, ICN_SORT, VisualControlType.VCL_ICON, 1.0));

		/* add the contextList to contextMap */
		//contextListMap.put(tableHeaderAsk.getQuestionCode(), new ContextList(sortContextList));

		return tableHeaderAsk;
	}
	/* generates the bucket-header ask */
	public Ask getBucketHeaderAsk(Map<String, ContextList> contextListMap, GennyToken serviceToken) {
		

		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);

		/* get the themes */
		Theme THM_QUESTION_GRP_LABEL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_QUESTION_GRP_LABEL",
				Theme.class, serviceToken.getToken());
		Theme THM_DISPLAY_VERTICAL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_DISPLAY_VERTICAL",
				Theme.class, serviceToken.getToken());
		Theme THM_DISPLAY_HORIZONTAL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_DISPLAY_HORIZONTAL",
				Theme.class, serviceToken.getToken());
		Theme THM_WIDTH_100_PERCENT = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_WIDTH_100_PERCENT",
				Theme.class, serviceToken.getToken());
		Theme THM_BH_ROW_ONE_GRP_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
				"THM_BH_ROW_ONE_GRP_WRAPPER", Theme.class, serviceToken.getToken());
		Theme THM_BH_ROW_ONE_GRP_LABEL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BH_ROW_ONE_GRP_LABEL",
				Theme.class, serviceToken.getToken());
		Theme THM_BH_ROW_ONE_GRP_CONTENT_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
				"THM_BH_ROW_ONE_GRP_CONTENT_WRAPPER", Theme.class, serviceToken.getToken());
		Theme THM_BH_ROW_ONE_VCL_INPUT = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BH_ROW_ONE_VCL_INPUT",
				Theme.class, serviceToken.getToken());
		Theme THM_BH_ROW_TWO_VCL_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
				"THM_BH_ROW_TWO_VCL_WRAPPER", Theme.class, serviceToken.getToken());
		Theme THM_BH_ROW_TWO_GRP_CONTENT_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
				"THM_BH_ROW_TWO_GRP_CONTENT_WRAPPER", Theme.class, serviceToken.getToken());
		Theme THM_BH_ROW_TWO_INPUT_FIELD = VertxUtils.getObject(serviceToken.getRealm(), "",
				"THM_BH_ROW_TWO_INPUT_FIELD", Theme.class, serviceToken.getToken());
		Theme THM_ICON = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_ICON", Theme.class,
				serviceToken.getToken());
		Theme THM_BH_GROUP_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BH_GROUP_WRAPPER",
				Theme.class, serviceToken.getToken());

		/* get the sort icon */
		BaseEntity ICN_SORT = beUtils.getBaseEntityByCode("ICN_SORT");

		/* we create context here */

		/* row1Context context */
		List<Context> row1Context = new ArrayList<>();
		row1Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_DISPLAY_HORIZONTAL),
				VisualControlType.GROUP_WRAPPER, 1.0));
		row1Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_BH_ROW_ONE_GRP_WRAPPER),
				VisualControlType.GROUP_WRAPPER, 1.0));
		row1Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_BH_ROW_ONE_GRP_LABEL),
				VisualControlType.GROUP_LABEL, 1.0));
		row1Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_BH_ROW_ONE_GRP_CONTENT_WRAPPER),
				VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
		row1Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_BH_ROW_ONE_VCL_INPUT),
				VisualControlType.VCL_INPUT, 1.0));

		/* row2Context context */
		List<Context> row2Context = new ArrayList<>();
		row2Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_DISPLAY_HORIZONTAL),
				VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
		row2Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_BH_ROW_TWO_VCL_WRAPPER),
				VisualControlType.VCL_WRAPPER, 1.0));
		row2Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_BH_ROW_TWO_GRP_CONTENT_WRAPPER),
				VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));

		/* bucketCountContextList context */
		List<Context> bucketCountContextList = new ArrayList<>();
		bucketCountContextList.add(new Context(ContextType.THEME, this.getThemeBe(THM_QUESTION_GRP_LABEL),
				VisualControlType.GROUP_WRAPPER, 1.0));

		/* bucketSearchContextList context */
		List<Context> bucketSearchContextList = new ArrayList<>();
		bucketSearchContextList.add(new Context(ContextType.THEME, this.getThemeBe(THM_BH_ROW_TWO_INPUT_FIELD),
				VisualControlType.VCL_WRAPPER, 1.0));

		/* bucketSortContextList context */
		List<Context> bucketSortContextList = new ArrayList<>();
		bucketSortContextList
				.add(new Context(ContextType.THEME, this.getThemeBe(THM_ICON), VisualControlType.VCL, 1.0));
		bucketSortContextList.add(new Context(ContextType.ICON, ICN_SORT, VisualControlType.VCL_ICON, 1.0));

		/* add the contextList to contextMap */
		contextListMap.put("QUE_BUCKET_HEADER_ROW_ONE_GRP", new ContextList(row1Context));
		contextListMap.put("QUE_BUCKET_HEADER_ROW_TWO_GRP", new ContextList(row2Context));
		contextListMap.put("QUE_BUCKET_COUNT", new ContextList(bucketCountContextList));
		contextListMap.put("QUE_BUCKET_SEARCH", new ContextList(bucketSearchContextList));
		contextListMap.put("QUE_BUCKET_SORT", new ContextList(bucketSortContextList));

		/* Validation for Search Attribute */
		Validation validation = new Validation("VLD_NON_EMPTY", "EmptyandBlankValues", "(?!^$|\\s+)");
		List<Validation> validations = new ArrayList<>();
		validations.add(validation);
		ValidationList searchValidationList = new ValidationList();
		searchValidationList.setValidationList(validations);

		Attribute searchAttribute = new Attribute("PRI_NAME", "Search",
				new DataType("Text", searchValidationList, "Text"));

		/* get the attributes */
		Attribute countAttribute = RulesUtils.getAttribute("PRI_TOTAL_RESULTS", serviceToken.getToken());
		Attribute sortAttribute = RulesUtils.getAttribute("PRI_SORT", serviceToken.getToken());
		Attribute nameAttribute = RulesUtils.getAttribute("PRI_NAME", serviceToken.getToken());
		Attribute questionAttribute = RulesUtils.getAttribute("QQQ_QUESTION_GROUP", serviceToken.getToken());
		Attribute tableCellAttribute = RulesUtils.getAttribute("QQQ_QUESTION_GROUP_TABLE_CELL",
				serviceToken.getToken());

		/* Initialize Bucket Header Ask group */
		Question bucketHeaderQuestion = new Question("QUE_BUCKET_HEADER_GRP", "Bucket Header", questionAttribute, true);
		Ask bucketHeaderAsk = new Ask(bucketHeaderQuestion, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");

		/* row-one-ask */
		Question row1Ques = new Question("QUE_BUCKET_HEADER_ROW_ONE_GRP", "Row One", tableCellAttribute, false);
		Ask row1Ask = new Ask(row1Ques, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");

		/* count ask */
		Question bucketCountQues = new Question("QUE_BUCKET_COUNT", countAttribute.getName(), countAttribute, false);
		Ask bucketCountAsk = new Ask(bucketCountQues, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");

		Ask[] row1ChildAsks = { bucketCountAsk };
		//row1Ask.setChildAsks(row1ChildAsks);

		/* row-two-ask */
		Question row2Ques = new Question("QUE_BUCKET_HEADER_ROW_TWO_GRP", "Row Two", tableCellAttribute, false);
		Ask row2Ask = new Ask(row2Ques, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");

		/* search ask */
		Question bucketSearchQues = new Question("QUE_BUCKET_SEARCH", searchAttribute.getName(), searchAttribute,
				false);
		Ask bucketSearchAsk = new Ask(bucketSearchQues, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");

		/* sort ask */
		Question bucketSortQues = new Question("QUE_BUCKET_SORT", sortAttribute.getName(), sortAttribute, false);
		Ask bucketSortAsk = new Ask(bucketSortQues, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");

		Ask[] row2ChildAsks = { bucketSearchAsk, bucketSortAsk };
		row2Ask.setChildAsks(row2ChildAsks);

		/* set the bucketHeader child asks */
		Ask[] bucketChildAsks = { row1Ask, row2Ask };
		bucketHeaderAsk.setChildAsks(bucketChildAsks);

		return bucketHeaderAsk;
	}

	/* generates the bucket-footer ask */
	public Ask getBucketFooterAsk(Map<String, ContextList> contextListMap, GennyToken serviceToken) {

		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);

		/* get the themes */
		Theme THM_DISPLAY_HORIZONTAL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_DISPLAY_HORIZONTAL",
				Theme.class, serviceToken.getToken());
		Theme THM_WIDTH_100_PERCENT = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_WIDTH_100_PERCENT",
				Theme.class, serviceToken.getToken());
		Theme THM_ICON = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_ICON", Theme.class,
				serviceToken.getToken());
		Theme THM_JUSTIFY_CONTENT_SPACE_AROUND = Theme.builder("THM_JUSTIFY_CONTENT_SPACE_AROUND").addAttribute()
				.justifyContent("space-around").end().build();

		/* get the baseentities */
		BaseEntity ICN_ARROW_FORWARD_IOS = beUtils.getBaseEntityByCode("ICN_ARROW_FORWARD_IOS");
		BaseEntity ICN_ARROW_BACK_IOS = beUtils.getBaseEntityByCode("ICN_ARROW_BACK_IOS");

		/* get the attributes */
		Attribute questionAttribute = RulesUtils.attributeMap.get("QQQ_QUESTION_GROUP");
		Attribute nextAttribute = RulesUtils.attributeMap.get("PRI_NEXT_BTN");
		Attribute prevAttribute = RulesUtils.attributeMap.get("PRI_PREVIOUS_BTN");

		/* we create context here */

		/* bucketFooter context */
		List<Context> bucketFooterContext = new ArrayList<>();
		bucketFooterContext.add(new Context(ContextType.THEME, this.getThemeBe(THM_DISPLAY_HORIZONTAL),
				VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
		bucketFooterContext.add(new Context(ContextType.THEME, this.getThemeBe(THM_WIDTH_100_PERCENT),
				VisualControlType.GROUP_WRAPPER, 1.0));
		bucketFooterContext.add(new Context(ContextType.THEME, this.getThemeBe(THM_JUSTIFY_CONTENT_SPACE_AROUND),
				VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));

		/* nextBucket context */
		List<Context> nextBucketContext = new ArrayList<>();
		nextBucketContext.add(new Context(ContextType.ICON, ICN_ARROW_FORWARD_IOS, VisualControlType.VCL_ICON, 1.0));
		nextBucketContext.add(new Context(ContextType.THEME, this.getThemeBe(THM_ICON), VisualControlType.VCL, 1.0));

		/* prevBucket context */
		List<Context> prevBucketContext = new ArrayList<>();
		prevBucketContext.add(new Context(ContextType.ICON, ICN_ARROW_BACK_IOS, VisualControlType.VCL_ICON, 1.0));
		prevBucketContext.add(new Context(ContextType.THEME, this.getThemeBe(THM_ICON), VisualControlType.VCL, 1.0));

		/* add the contextList to contextMap */
		contextListMap.put("QUE_BUCKET_FOOTER_GRP", new ContextList(bucketFooterContext));
		contextListMap.put("QUE_NEXT_BUCKET", new ContextList(nextBucketContext));
		contextListMap.put("QUE_PREV_BUCKET", new ContextList(prevBucketContext));

		/* Initialize Bucket Footer Ask group */
		Question bucketFooterQuestion = new Question("QUE_BUCKET_FOOTER_GRP", "Footer Group", questionAttribute, true);
		Ask bucketFooterAsk = new Ask(bucketFooterQuestion, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");

		/* next ask */
		Question nextBucketQues = new Question("QUE_NEXT_BUCKET", "", nextAttribute, false);
		Ask nextBucketAsk = new Ask(nextBucketQues, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");

		/* prev ask */
		Question prevBucketQues = new Question("QUE_PREV_BUCKET", "", prevAttribute, false);
		Ask prevBucketAsk = new Ask(prevBucketQues, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");

		/* set the child asks */
		Ask[] bucketChildAsksArray = { prevBucketAsk, nextBucketAsk };
		bucketFooterAsk.setChildAsks(bucketChildAsksArray);

		return bucketFooterAsk;

	}

	public void sendCards(Frame3 FRM_BUCKET_CONTENT, GennyToken userToken) {
		
		/* initialize beUtils */
		BaseEntityUtils beUtils = new BaseEntityUtils(userToken);

		/* initialize bucketUtils */
		TableUtilsNew bucketUtils = new TableUtilsNew(beUtils);

		/* initialize searchUtils */
		SearchUtils searchUtils = new SearchUtils(beUtils);

		/* initialize virtualAskMap */
		Map<String, QDataAskMessage> virtualAskMap = new HashMap<String, QDataAskMessage>();

		/* initialize ask set */
		Set<QDataAskMessage> askSet = new HashSet<QDataAskMessage>();

		/* initialize contextListMap */
		Map<String, ContextList> contextListMap = new HashMap<String, ContextList>();

		/* list to collect baseentity */
		List<BaseEntity> beList = new ArrayList<BaseEntity>();

		/* get the bucket-content ask */
		Ask FRM_BUCKET_CONTENT_ASK = bucketUtils.getBucketContentAsk(contextListMap, userToken);

		/* get the bucket-content ask */
		//Frame3 FRM_BUCKET_CONTENT = bucketUtils.getBucketContentFrame("FRM_BUCKET_CONTENT", "test", "test");

		/* get the themes */
		Theme THM_WIDTH_100_PERCENT_NO_INHERIT = VertxUtils.getObject(userToken.getRealm(), "", "THM_WIDTH_100_PERCENT_NO_INHERIT",
				Theme.class, userToken.getToken());

		/* bucketContent context */
		List<Context> bucketContentContext = new ArrayList<>();
		bucketContentContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_WIDTH_100_PERCENT_NO_INHERIT),
				VisualControlType.GROUP_WRAPPER, 1.0));


		try {

			/* get the list of bucket searchBEs from the cache */
			List<SearchEntity> searchBeList = bucketUtils.getBucketSearchBeListFromCache(userToken);
			
			/* get all the contextListMap for card */
			contextListMap = bucketUtils.getCardContextListMap(contextListMap, userToken);
			List<Context> cardContext = contextListMap.get("QUE_CARD_APPLICATION_TEMPLATE_GRP").getContextList();
			
			/* publish SBE_DUMMY */
			BaseEntity SBE_DUMMY = new BaseEntity("SBE_DUMMY", "SBE_DUMMY");

			Attribute contentAttribute = new Attribute("PRI_CONTENT", "content", new DataType(String.class));
			EntityAttribute entAttr = new EntityAttribute(SBE_DUMMY, contentAttribute, 1.0, "{  \"flex\": 1 }");
			Set<EntityAttribute> entAttrSet = new HashSet<>();
			entAttrSet.add(entAttr);
			SBE_DUMMY.setBaseEntityAttributes(entAttrSet);
			
			QDataBaseEntityMessage SBE_DUMMY_MSG = new QDataBaseEntityMessage(SBE_DUMMY);
			SBE_DUMMY_MSG.setToken(userToken.getToken());
			
			String msgJson = JsonUtils.toJson(SBE_DUMMY_MSG);
			VertxUtils.writeMsg("webcmds",msgJson);


			/* loop through the s */
			for (SearchEntity searchBe : searchBeList) {

				String code = searchBe.getCode().split("SBE_")[1];

				/* get the attributes from searchObj */
				Map<String, String> columns = searchUtils.getTableColumns(searchBe);

				/* fetch the search results */
				QDataBaseEntityMessage msg = searchUtils.fetchSearchResults(searchBe, beUtils.getGennyToken());

				/* get the application counts */
				long totalResults = msg.getItems().length;

				/* also update the searchBe with the attribute */
				Answer totalAnswer = new Answer(beUtils.getGennyToken().getUserCode(), searchBe.getCode(),
						"PRI_TOTAL_RESULTS", totalResults + "");
				beUtils.addAnswer(totalAnswer);
				beUtils.updateBaseEntity(searchBe, totalAnswer);

				/* get the applications */
				List<BaseEntity> appList = Arrays.asList(msg.getItems());

				/* add the application to the baseentity list */
				beList.addAll(appList);

				/* convert app to asks */
				List<Ask> appAsksList = searchUtils.generateQuestions(beUtils.getGennyToken(), beUtils, appList,
						columns, beUtils.getGennyToken().getUserCode());
				
				/* get the templat ask for card */
				Ask templateAsk = bucketUtils.getCardTemplate(userToken);

				/* implement template ask to appAks list */
				//List<Ask> askList = bucketUtils.implementCardTemplate(appAsksList, templateAsk, contextListMap);

				/* generate bucketContent asks for each bucket */
				Ask bucketContentAsk = Ask.clone(FRM_BUCKET_CONTENT_ASK);
				bucketContentAsk.setQuestionCode("QUE_BUCKET_CONTENT_" + code + "_GRP");
				bucketContentAsk.setName(searchBe.getName());

				/* add the contextList to contextMap */
				contextListMap.put("QUE_BUCKET_CONTENT_" + code + "_GRP", new ContextList(bucketContentContext));

				/* link bucketContentAsk to application asks */
//				bucketContentAsk.setChildAsks(askList.toArray(new Ask[askList.size()]));

				/* add the bucketContent ask to virtualAskMap */
				virtualAskMap.put("QUE_BUCKET_CONTENT_" + code + "_GRP", new QDataAskMessage(bucketContentAsk));

				/* link the bucket-content ask to bucket-content frame */
				Frame3 bucketContent = Frame3.clone(FRM_BUCKET_CONTENT);
				bucketContent.setCode("FRM_BUCKET_CONTENT_" + code);
				bucketContent.setQuestionCode("QUE_BUCKET_CONTENT_" + code + "_GRP");
				
				/* add the contextList for the cardQuestion */
				contextListMap.put("QUE_CARD_APPLICATION_TEMPLATE_GRP", new ContextList(cardContext));

				QDataBaseEntityMessage msg2 = FrameUtils2.toMessage(bucketContent, userToken, askSet, contextListMap,
						virtualAskMap);
				msg2.setToken(userToken.getToken());
				VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg2));

			}

			/* Send */
			System.out.println("Sending application entitites");

			QDataBaseEntityMessage appMsg = new QDataBaseEntityMessage(beList.toArray(new BaseEntity[0]));
			appMsg.setToken(userToken.getToken());
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson(appMsg));

			System.out.println("Sending asks from outside the loop");

			/* Send asks */
			for (QDataAskMessage askMsg : askSet) {

				askMsg.setToken(userToken.getToken());

				String json = JsonUtils.toJson(askMsg);
				VertxUtils.writeMsg("webcmds", json);

			}

			System.out.print("Completed");

		} catch (Exception e) {
			// TODO: handle exception

		}
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

		System.out.println("the Columns is :: " + columns);
		return columns;
	}

	public List<SearchEntity> getSearchBeList(){

		List<SearchEntity> bucketSearchBeList = new ArrayList<SearchEntity>();
		
		SearchEntity SBE_INTERNS = new SearchEntity("SBE_INTERNS", "Interns")
													.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "PER_%")
													.addFilter("PRI_IS_INTERN", true)
													.addColumn("PRI_INTERN_NAME", "Intern's Name")
													.addColumn("PRI_STATUS", "Status")
													.addColumn("PRI_EDU_PROVIDER_NAME", "Edu Provider")
													.addColumn("PRI_INTERN_MOBILE", "Mobile")
													.addColumn("PRI_ADDRESS_FULL", "Address")
													.setPageStart(0).setPageSize(10);
		SearchEntity SBE_AGENTS = new SearchEntity("SBE_AGENTS", "Contacts")
													.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "PER_%")
													.addFilter("PRI_IS_INTERN", true)
													.addColumn("PRI_INTERN_NAME", "Agent's Name")
													.addColumn("PRI_STATUS", "Status")
													.addColumn("PRI_EDU_PROVIDER_NAME", "Agent Company")
													.addColumn("PRI_INTERN_MOBILE", "Mobile")
													.addColumn("PRI_ADDRESS_FULL", "Address")
													.setPageStart(0).setPageSize(10);
		SearchEntity SBE_HCR = new SearchEntity("SBE_HCR", "Companies")
													.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "PER_%")
													.addFilter("PRI_IS_INTERN", true)
													.addColumn("PRI_INTERN_NAME", "Host Company Name")
													.addColumn("PRI_STATUS", "Status")
													.addColumn("PRI_EDU_PROVIDER_NAME", "Host Companies")
													.addColumn("PRI_INTERN_MOBILE", "Mobile")
													.addColumn("PRI_ADDRESS_FULL", "Address")
													.setPageStart(0).setPageSize(10);
		SearchEntity SBE_EPR = new SearchEntity("SBE_EPR", "Companies")
													.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "PER_%")
													.addFilter("PRI_IS_INTERN", true)
													.addColumn("PRI_INTERN_NAME", "Edu Provider Name")
													.addColumn("PRI_STATUS", "Status")
													.addColumn("PRI_EDU_PROVIDER_NAME", "Edu Provider")
													.addColumn("PRI_INTERN_MOBILE", "Mobile")
													.addColumn("PRI_ADDRESS_FULL", "Address")
													.setPageStart(0).setPageSize(10);

		bucketSearchBeList.add(SBE_INTERNS);
		bucketSearchBeList.add(SBE_AGENTS);
		bucketSearchBeList.add(SBE_HCR);
		bucketSearchBeList.add(SBE_EPR);
		return bucketSearchBeList;

	}

}
