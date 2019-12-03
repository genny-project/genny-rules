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
import life.genny.rules.QRules;
import life.genny.utils.ContextUtils;

public class BucketUtils {

	BaseEntityUtils beUtils = null;

	public BucketUtils(BaseEntityUtils beUtils) {
		this.beUtils = beUtils;
	}

	/* Generate List of asks from a SearchEntity */
	public static List<Ask> generateQuestions(GennyToken userToken, BaseEntityUtils beUtils, List<BaseEntity> bes, Map<String, String> columns, String targetCode) {

		/* initialize an empty ask list */
		List<Ask> askList = new ArrayList<>();
		List<QDataBaseEntityMessage> themeMsgList = new ArrayList<QDataBaseEntityMessage>();
		BucketUtils bucketUtils = new BucketUtils(beUtils);

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

						Question childQuestion = new Question("QUE_" + attributeCode + "_" + be.getCode(), attributeName, attr,
								true);
						Ask childAsk = new Ask(childQuestion, targetCode, be.getCode());

						/* add the entityAttribute ask to list */
						childAskList.add(childAsk);

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

	/* get the search BE related to bucket from cache */
	public List<SearchEntity> getBucketSearchBeListFromCache(GennyToken serviceToken) {

		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);

		List<SearchEntity> bucketSearchBeList = new ArrayList<SearchEntity>();

		try {
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
	
	/* returns baseentity of a theme  */
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

	/* generates the bucket-content ask */
	public Ask getBucketContentAsk(Map<String, ContextList> contextListMap, GennyToken serviceToken) {

		Attribute questionAttribute = RulesUtils.getAttribute("QQQ_QUESTION_GROUP", serviceToken.getToken());
		Question bucketContentQuestion = new Question("QUE_BUCKET_CONTENT_GRP", "",questionAttribute, true);
		Ask bucketContentAsk = new Ask(bucketContentQuestion, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");
		return bucketContentAsk;
	
	}

	/* generates the bucket-header ask */
	public Ask getBucketHeaderAsk(Map<String, ContextList> contextListMap, GennyToken serviceToken) {

		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);

		Theme THM_QUESTION_GRP_LABEL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_QUESTION_GRP_LABEL",
				Theme.class, serviceToken.getToken());
		Theme THM_DISPLAY_VERTICAL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_DISPLAY_VERTICAL", Theme.class,
				serviceToken.getToken());
		Theme THM_DISPLAY_HORIZONTAL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_DISPLAY_HORIZONTAL",
				Theme.class, serviceToken.getToken());
		Theme THM_WIDTH_100_PERCENT = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_WIDTH_100_PERCENT",
				Theme.class, serviceToken.getToken());
		Theme THM_BH_ROW_ONE_GRP_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BH_ROW_ONE_GRP_WRAPPER",
				Theme.class, serviceToken.getToken());
		Theme THM_BH_ROW_ONE_GRP_LABEL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BH_ROW_ONE_GRP_LABEL",
				Theme.class, serviceToken.getToken());
		Theme THM_BH_ROW_ONE_GRP_CONTENT_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
				"THM_BH_ROW_ONE_GRP_CONTENT_WRAPPER", Theme.class, serviceToken.getToken());
		Theme THM_BH_ROW_ONE_VCL_INPUT = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BH_ROW_ONE_VCL_INPUT",
				Theme.class, serviceToken.getToken());
		Theme THM_BH_ROW_TWO_VCL_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BH_ROW_TWO_VCL_WRAPPER",
				Theme.class, serviceToken.getToken());
		Theme THM_BH_ROW_TWO_GRP_CONTENT_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
				"THM_BH_ROW_TWO_GRP_CONTENT_WRAPPER", Theme.class, serviceToken.getToken());
		Theme THM_BH_ROW_TWO_INPUT_FIELD = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BH_ROW_TWO_INPUT_FIELD",
				Theme.class, serviceToken.getToken());
		Theme THM_ICON = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_ICON", Theme.class,
				serviceToken.getToken());
		Theme THM_BH_GROUP_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BH_GROUP_WRAPPER", Theme.class,
				serviceToken.getToken());
		
		/* get the sort icon */
		BaseEntity ICN_SORT = beUtils.getBaseEntityByCode("ICN_SORT");

		/* we create context here */

		/* row1Context context */
		List<Context> row1Context = new ArrayList<>();
		row1Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_DISPLAY_HORIZONTAL), VisualControlType.GROUP_WRAPPER, 1.0));
		row1Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_BH_ROW_ONE_GRP_WRAPPER), VisualControlType.GROUP_WRAPPER, 1.0));
		row1Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_BH_ROW_ONE_GRP_LABEL), VisualControlType.GROUP_LABEL, 1.0));
		row1Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_BH_ROW_ONE_GRP_CONTENT_WRAPPER), VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
		row1Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_BH_ROW_ONE_VCL_INPUT), VisualControlType.VCL_INPUT, 1.0));
		
		/* row2Context context */
		List<Context> row2Context = new ArrayList<>();
		row2Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_DISPLAY_HORIZONTAL), VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
		row2Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_BH_ROW_TWO_VCL_WRAPPER), VisualControlType.VCL_WRAPPER, 1.0));
		row2Context.add(new Context(ContextType.THEME, this.getThemeBe(THM_BH_ROW_TWO_GRP_CONTENT_WRAPPER), VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
		
		/* bucketCountContextList context */
		List<Context> bucketCountContextList = new ArrayList<>();
		bucketCountContextList.add(new Context(ContextType.THEME, this.getThemeBe(THM_QUESTION_GRP_LABEL), VisualControlType.GROUP_WRAPPER, 1.0));
		
		/* bucketSearchContextList context */
		List<Context> bucketSearchContextList = new ArrayList<>();
		bucketSearchContextList.add(new Context(ContextType.THEME, this.getThemeBe(THM_BH_ROW_TWO_INPUT_FIELD), VisualControlType.VCL_WRAPPER, 1.0));
		
		/* bucketSortContextList context */
		List<Context> bucketSortContextList = new ArrayList<>();
		bucketSortContextList.add(new Context(ContextType.THEME, this.getThemeBe(THM_ICON), VisualControlType.VCL, 1.0));
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
		
		Attribute searchAttribute = new Attribute("PRI_NAME", "Search", new DataType("Text", searchValidationList, "Text"));

		/* get the attributes */
		Attribute countAttribute = RulesUtils.getAttribute("PRI_TOTAL_RESULTS", serviceToken.getToken());
		Attribute sortAttribute = RulesUtils.getAttribute("PRI_SORT", serviceToken.getToken());
		Attribute nameAttribute = RulesUtils.getAttribute("PRI_NAME", serviceToken.getToken());
		Attribute questionAttribute = RulesUtils.getAttribute("QQQ_QUESTION_GROUP", serviceToken.getToken());
		Attribute tableCellAttribute = RulesUtils.getAttribute("QQQ_QUESTION_GROUP_TABLE_CELL", serviceToken.getToken());

		/* Initialize Bucket Header Ask group */
		Question bucketHeaderQuestion = new Question("QUE_BUCKET_HEADER_GRP", "Bucket Header", questionAttribute, true);
		Ask bucketHeaderAsk = new Ask(bucketHeaderQuestion, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");

		/* row-one-ask */
		Question row1Ques = new Question("QUE_BUCKET_HEADER_ROW_ONE_GRP", "Row One", tableCellAttribute,false);
		Ask row1Ask = new Ask(row1Ques, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");

		/* count ask */
		Question bucketCountQues = new Question("QUE_BUCKET_COUNT", countAttribute.getName(), countAttribute, false);
		Ask bucketCountAsk = new Ask(bucketCountQues, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");

		Ask[] row1ChildAsks = { bucketCountAsk };
		row1Ask.setChildAsks(row1ChildAsks);

		/* row-two-ask */
		Question row2Ques = new Question("QUE_BUCKET_HEADER_ROW_TWO_GRP", "Row Two", tableCellAttribute, false);
		Ask row2Ask = new Ask(row2Ques, beUtils.getGennyToken().getUserCode(), "SBE_DUMMY");

		/* search ask */
		Question bucketSearchQues = new Question("QUE_BUCKET_SEARCH", searchAttribute.getName(), searchAttribute, false);
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
		Theme THM_DISPLAY_HORIZONTAL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_DISPLAY_HORIZONTAL",Theme.class, serviceToken.getToken());
		Theme THM_WIDTH_100_PERCENT = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_WIDTH_100_PERCENT",Theme.class, serviceToken.getToken());
		Theme THM_ICON = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_ICON", Theme.class,serviceToken.getToken());
		Theme THM_JUSTIFY_CONTENT_SPACE_AROUND = Theme.builder("THM_JUSTIFY_CONTENT_SPACE_AROUND").addAttribute().justifyContent("space-around").end().build();

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
		bucketFooterContext.add(new Context(ContextType.THEME, this.getThemeBe(THM_DISPLAY_HORIZONTAL), VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
		bucketFooterContext.add(new Context(ContextType.THEME, this.getThemeBe(THM_WIDTH_100_PERCENT), VisualControlType.GROUP_WRAPPER, 1.0));
		bucketFooterContext.add(new Context(ContextType.THEME, this.getThemeBe(THM_JUSTIFY_CONTENT_SPACE_AROUND), VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));

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

}
