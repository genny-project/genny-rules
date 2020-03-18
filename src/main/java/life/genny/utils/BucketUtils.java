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

import org.apache.commons.lang.SerializationUtils;
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

public class BucketUtils {
	
	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


	BaseEntityUtils beUtils = null;

	public BucketUtils(BaseEntityUtils beUtils) {
		this.beUtils = beUtils;
	}

	public void sendBucketContexts(GennyToken serviceToken){

		System.out.println("Sending contexts for buckets");

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

		Theme THM_PROGRESS_VCL_INPUT = VertxUtils.getObject(serviceToken.getRealm(), "",
				"THM_PROGRESS_VCL_INPUT", Theme.class, serviceToken.getToken());

		Theme THM_PROGRESS_INPUT_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
				"THM_PROGRESS_INPUT_WRAPPER", Theme.class, serviceToken.getToken());
				
		Theme THM_BORDER_RADIUS_50 = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BORDER_RADIUS_50",
				Theme.class, serviceToken.getToken());
				
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
				
		Theme THM_PROJECT_COLOR_SURFACE = VertxUtils.getObject(serviceToken.getRealm(), "",
				"THM_PROJECT_COLOR_SURFACE", Theme.class, serviceToken.getToken());
				
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

		/* new themes */
		Theme THM_CARD_STATUS_INPUT_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_CARD_STATUS_INPUT_WRAPPER", Theme.class, serviceToken.getToken());

		Theme THM_CARD_THUMBNAIL_VCL_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
		"THM_CARD_THUMBNAIL_VCL_WRAPPER", Theme.class, serviceToken.getToken());

		Theme THM_CARD_THUMBNAIL_INPUT_PLACEHOLDER = VertxUtils.getObject(serviceToken.getRealm(), "",
		"THM_CARD_THUMBNAIL_INPUT_PLACEHOLDER", Theme.class, serviceToken.getToken());

		Theme THM_CARD_MAIN_GROUP_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
		"THM_CARD_MAIN_GROUP_WRAPPER", Theme.class, serviceToken.getToken());

		Theme THM_CARD_CENTRE_GROUP_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
		"THM_CARD_CENTRE_GROUP_WRAPPER", Theme.class, serviceToken.getToken());
		
		Theme THM_DROPDOWN_ICON_MORE_VERT = VertxUtils.getObject(serviceToken.getRealm(), "",
		"THM_DROPDOWN_ICON_MORE_VERT", Theme.class, serviceToken.getToken());

		Theme THM_CARD_BOTTOM_GROUP_CLICKABLE_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
		"THM_CARD_BOTTOM_GROUP_CLICKABLE_WRAPPER", Theme.class, serviceToken.getToken());

		Theme THM_CARD_HEADER_INPUT_FIELD = VertxUtils.getObject(serviceToken.getRealm(), "",
		"THM_CARD_HEADER_INPUT_FIELD", Theme.class, serviceToken.getToken());

		Theme THM_CARD_HEADER_INPUT_FIELD_INTERN = VertxUtils.getObject(serviceToken.getRealm(), "",
		"THM_CARD_HEADER_INPUT_FIELD_INTERN", Theme.class, serviceToken.getToken());

		Theme THM_CARD_BOTTOM_VCL_ICON = VertxUtils.getObject(serviceToken.getRealm(), "",
		"THM_CARD_BOTTOM_VCL_ICON", Theme.class, serviceToken.getToken());

		/* ICON TYPES */
		Theme THM_ICON_WORK = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_ICON_WORK", Theme.class, serviceToken.getToken());

		Theme THM_ICON_LOCATION_ON = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_ICON_LOCATION_ON", Theme.class, serviceToken.getToken());

		Theme THM_ICON_STAR = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_ICON_STAR", Theme.class, serviceToken.getToken());

		Theme THM_ICON_DIRECTIONS_BUS = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_ICON_DIRECTIONS_BUS", Theme.class, serviceToken.getToken());

		Theme THM_ICON_PHONE = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_ICON_PHONE", Theme.class, serviceToken.getToken());

		Theme THM_ICON_EMAIL = VertxUtils.getObject(serviceToken.getRealm(), "",
				"THM_ICON_EMAIL", Theme.class, serviceToken.getToken());
	
		Theme THM_ICON = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_ICON", Theme.class, serviceToken.getToken());
		
		Theme THM_HIDE_INPUT = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_HIDE_INPUT", Theme.class, serviceToken.getToken());

		Theme THM_CARD_DATE = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_CARD_DATE", Theme.class, serviceToken.getToken());
		
		Theme THM_CARD_DATE_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_CARD_DATE_WRAPPER", Theme.class, serviceToken.getToken());
		
		Theme THM_CARD_MENU_GROUP_CONTENT_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_CARD_MENU_GROUP_CONTENT_WRAPPER", Theme.class, serviceToken.getToken());

		Theme THM_QUESTION_GRP_LABEL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_QUESTION_GRP_LABEL",
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
		Theme THM_BH_GROUP_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BH_GROUP_WRAPPER",
				Theme.class, serviceToken.getToken());

		Theme THM_WIDTH_100_PERCENT_NO_INHERIT = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_WIDTH_100_PERCENT_NO_INHERIT", Theme.class, serviceToken.getToken());
		Theme THM_BUCKET_HEADER_GROUP_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BUCKET_HEADER_GROUP_WRAPPER", Theme.class, serviceToken.getToken());
		Theme THM_BUCKET_HEADER_GROUP_HEADER_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BUCKET_HEADER_GROUP_HEADER_WRAPPER", Theme.class, serviceToken.getToken());
		Theme THM_BUCKET_HEADER_GROUP_LABEL = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BUCKET_HEADER_GROUP_LABEL", Theme.class, serviceToken.getToken());


		/* get the theme baseentioty */
		BaseEntity THM_DISPLAY_VERTICAL_BE = this.getThemeBe(THM_DISPLAY_VERTICAL);
		BaseEntity THM_JUSTIFY_CONTENT_FLEX_START_BE = this.getThemeBe(THM_JUSTIFY_CONTENT_FLEX_START);
		BaseEntity THM_CARD_BE = this.getThemeBe(THM_CARD);
		BaseEntity THM_DROPDOWN_ICON_ALT_BE = this.getThemeBe(THM_DROPDOWN_ICON_ALT);
		BaseEntity THM_DROPDOWN_BEHAVIOUR_GENNY_BE = this.getThemeBe(THM_DROPDOWN_BEHAVIOUR_GENNY);
		BaseEntity THM_BACKGROUND_NONE_BE = this.getThemeBe(THM_BACKGROUND_NONE);
		BaseEntity THM_DROPDOWN_PROFILE_BEHAVIOUR_GENNY_BE = this.getThemeBe(THM_DROPDOWN_PROFILE_BEHAVIOUR_GENNY);
		BaseEntity THM_DROPDOWN_HEADER_WRAPPER_GENNY_BE = this.getThemeBe(THM_DROPDOWN_HEADER_WRAPPER_GENNY);
		BaseEntity THM_DROPDOWN_GROUP_LABEL_GENNY_BE = this.getThemeBe(THM_DROPDOWN_GROUP_LABEL_GENNY);
		BaseEntity THM_DROPDOWN_CONTENT_WRAPPER_GENNY_BE = this.getThemeBe(THM_DROPDOWN_CONTENT_WRAPPER_GENNY);
		BaseEntity THM_BOX_SHADOW_SM_BE = this.getThemeBe(THM_BOX_SHADOW_SM);
		BaseEntity THM_DROPDOWN_VCL_GENNY_BE = this.getThemeBe(THM_DROPDOWN_VCL_GENNY);
		BaseEntity THM_IMAGE_PLACEHOLDER_BE = this.getThemeBe(THM_IMAGE_PLACEHOLDER);
		BaseEntity THM_HEADER_PROFILE_PICTURE_BE = this.getThemeBe(THM_HEADER_PROFILE_PICTURE);
		BaseEntity THM_PROGRESS_VCL_INPUT_BE = this.getThemeBe(THM_PROGRESS_VCL_INPUT);
		BaseEntity THM_PROGRESS_INPUT_WRAPPER_BE = this.getThemeBe(THM_PROGRESS_INPUT_WRAPPER);
		BaseEntity THM_BORDER_RADIUS_50_BE = this.getThemeBe(THM_BORDER_RADIUS_50);
		BaseEntity THM_EXPANDABLE_BE = this.getThemeBe(THM_EXPANDABLE);
		BaseEntity THM_WIDTH_100_PERCENT_BE = this.getThemeBe(THM_WIDTH_100_PERCENT);
		BaseEntity THM_JUSTIFY_CONTENT_CENTRE_BE = this.getThemeBe(THM_JUSTIFY_CONTENT_CENTRE);
		BaseEntity THM_IMAGE_PLACEHOLDER_PERSON_BE = this.getThemeBe(THM_IMAGE_PLACEHOLDER_PERSON);
		BaseEntity THM_PROFILE_IMAGE_BE = this.getThemeBe(THM_PROFILE_IMAGE);
		BaseEntity THM_PROJECT_COLOR_SURFACE_BE = this.getThemeBe(THM_PROJECT_COLOR_SURFACE);
		BaseEntity THM_PADDING_X_10_BE = this.getThemeBe(THM_PADDING_X_10);
		BaseEntity THM_FLEX_ONE_BE = this.getThemeBe(THM_FLEX_ONE);
		BaseEntity THM_FLEX_ONE_INHERITABLE_BE = this.getThemeBe(THM_FLEX_ONE_INHERITABLE);
		BaseEntity THM_DROPDOWN_ICON_MORE_HORIZ_BE = this.getThemeBe(THM_DROPDOWN_ICON_MORE_HORIZ);
		BaseEntity THM_EXPANDABLE_ICON_BE = this.getThemeBe(THM_EXPANDABLE_ICON);
		BaseEntity THM_CARD_STATUS_INPUT_WRAPPER_BE = this.getThemeBe(THM_CARD_STATUS_INPUT_WRAPPER);
		BaseEntity THM_CARD_THUMBNAIL_VCL_WRAPPER_BE = this.getThemeBe(THM_CARD_THUMBNAIL_VCL_WRAPPER);
		BaseEntity THM_CARD_THUMBNAIL_INPUT_PLACEHOLDER_BE = this.getThemeBe(THM_CARD_THUMBNAIL_INPUT_PLACEHOLDER);
		BaseEntity THM_CARD_MAIN_GROUP_WRAPPER_BE = this.getThemeBe(THM_CARD_MAIN_GROUP_WRAPPER);
		BaseEntity THM_CARD_CENTRE_GROUP_WRAPPER_BE = this.getThemeBe(THM_CARD_CENTRE_GROUP_WRAPPER);
		BaseEntity THM_DROPDOWN_ICON_MORE_VERT_BE = this.getThemeBe(THM_DROPDOWN_ICON_MORE_VERT);
		BaseEntity THM_CARD_BOTTOM_GROUP_CLICKABLE_WRAPPER_BE = this.getThemeBe(THM_CARD_BOTTOM_GROUP_CLICKABLE_WRAPPER);
		BaseEntity THM_CARD_HEADER_INPUT_FIELD_BE = this.getThemeBe(THM_CARD_HEADER_INPUT_FIELD);
		BaseEntity THM_CARD_HEADER_INPUT_FIELD_INTERN_BE = this.getThemeBe(THM_CARD_HEADER_INPUT_FIELD_INTERN);
		BaseEntity THM_CARD_BOTTOM_VCL_ICON_BE = this.getThemeBe(THM_CARD_BOTTOM_VCL_ICON);
		BaseEntity THM_ICON_WORK_BE = this.getThemeBe(THM_ICON_WORK);
		BaseEntity THM_ICON_LOCATION_ON_BE = this.getThemeBe(THM_ICON_LOCATION_ON);
		BaseEntity THM_ICON_STAR_BE = this.getThemeBe(THM_ICON_STAR);
		BaseEntity THM_ICON_DIRECTIONS_BUS_BE = this.getThemeBe(THM_ICON_DIRECTIONS_BUS);
		BaseEntity THM_ICON_PHONE_BE = this.getThemeBe(THM_ICON_PHONE);
		BaseEntity THM_ICON_EMAIL_BE = this.getThemeBe(THM_ICON_EMAIL);
		BaseEntity THM_ICON_BE = this.getThemeBe(THM_ICON);
		BaseEntity THM_HIDE_INPUT_BE = this.getThemeBe(THM_HIDE_INPUT);
		BaseEntity THM_CARD_DATE_BE = this.getThemeBe(THM_CARD_DATE);
		BaseEntity THM_CARD_DATE_WRAPPER_BE = this.getThemeBe(THM_CARD_DATE_WRAPPER);
		BaseEntity THM_CARD_MENU_GROUP_CONTENT_WRAPPER_BE = this.getThemeBe(THM_CARD_MENU_GROUP_CONTENT_WRAPPER);
		BaseEntity THM_QUESTION_GRP_LABEL_BE = this.getThemeBe(THM_QUESTION_GRP_LABEL);
		BaseEntity THM_BH_ROW_ONE_GRP_WRAPPER_BE = this.getThemeBe(THM_BH_ROW_ONE_GRP_WRAPPER);
		BaseEntity THM_BH_ROW_ONE_GRP_LABEL_BE = this.getThemeBe(THM_BH_ROW_ONE_GRP_LABEL);
		BaseEntity THM_BH_ROW_ONE_GRP_CONTENT_WRAPPER_BE = this.getThemeBe(THM_BH_ROW_ONE_GRP_CONTENT_WRAPPER);
		BaseEntity THM_BH_ROW_ONE_VCL_INPUT_BE = this.getThemeBe(THM_BH_ROW_ONE_VCL_INPUT);
		BaseEntity THM_BH_ROW_TWO_VCL_WRAPPER_BE = this.getThemeBe(THM_BH_ROW_TWO_VCL_WRAPPER);
		BaseEntity THM_BH_ROW_TWO_GRP_CONTENT_WRAPPER_BE = this.getThemeBe(THM_BH_ROW_TWO_GRP_CONTENT_WRAPPER);
		BaseEntity THM_BH_ROW_TWO_INPUT_FIELD_BE = this.getThemeBe(THM_BH_ROW_TWO_INPUT_FIELD);
		BaseEntity THM_BH_GROUP_WRAPPER_BE = this.getThemeBe(THM_BH_GROUP_WRAPPER);
		
		BaseEntity THM_WIDTH_100_PERCENT_NO_INHERIT_BE = this.getThemeBe(THM_WIDTH_100_PERCENT_NO_INHERIT);
		BaseEntity THM_BUCKET_HEADER_GROUP_WRAPPER_BE = this.getThemeBe(THM_BUCKET_HEADER_GROUP_WRAPPER);
		BaseEntity THM_BUCKET_HEADER_GROUP_HEADER_WRAPPER_BE = this.getThemeBe(THM_BUCKET_HEADER_GROUP_HEADER_WRAPPER);
		BaseEntity THM_BUCKET_HEADER_GROUP_LABEL_BE = this.getThemeBe(THM_BUCKET_HEADER_GROUP_LABEL);

		List<BaseEntity> themes = new ArrayList<BaseEntity>();		
		themes.add(THM_DISPLAY_VERTICAL_BE);
		themes.add(THM_JUSTIFY_CONTENT_FLEX_START_BE);
		themes.add(THM_CARD_BE);
		themes.add(THM_DROPDOWN_ICON_ALT_BE);
		themes.add(THM_DROPDOWN_BEHAVIOUR_GENNY_BE);
		themes.add(THM_BACKGROUND_NONE_BE);
		themes.add(THM_DROPDOWN_PROFILE_BEHAVIOUR_GENNY_BE);
		themes.add(THM_DROPDOWN_HEADER_WRAPPER_GENNY_BE);
		themes.add(THM_DROPDOWN_GROUP_LABEL_GENNY_BE);
		themes.add(THM_DROPDOWN_CONTENT_WRAPPER_GENNY_BE);
		themes.add(THM_BOX_SHADOW_SM_BE);
		themes.add(THM_DROPDOWN_VCL_GENNY_BE);
		themes.add(THM_IMAGE_PLACEHOLDER_BE);
		themes.add(THM_HEADER_PROFILE_PICTURE_BE);
		themes.add(THM_PROGRESS_VCL_INPUT_BE);
		themes.add(THM_PROGRESS_INPUT_WRAPPER_BE);
		themes.add(THM_BORDER_RADIUS_50_BE);
		themes.add(THM_EXPANDABLE_BE);
		themes.add(THM_WIDTH_100_PERCENT_BE);
		themes.add(THM_JUSTIFY_CONTENT_CENTRE_BE);
		themes.add(THM_IMAGE_PLACEHOLDER_PERSON_BE);
		themes.add(THM_PROFILE_IMAGE_BE);
		themes.add(THM_PROJECT_COLOR_SURFACE_BE);
		themes.add(THM_PADDING_X_10_BE);
		themes.add(THM_FLEX_ONE_BE);
		themes.add(THM_FLEX_ONE_INHERITABLE_BE);
		themes.add(THM_DROPDOWN_ICON_MORE_HORIZ_BE);
		themes.add(THM_EXPANDABLE_ICON_BE);
		themes.add(THM_CARD_STATUS_INPUT_WRAPPER_BE);
		themes.add(THM_CARD_THUMBNAIL_VCL_WRAPPER_BE);
		themes.add(THM_CARD_THUMBNAIL_INPUT_PLACEHOLDER_BE);
		themes.add(THM_CARD_MAIN_GROUP_WRAPPER_BE);
		themes.add(THM_CARD_CENTRE_GROUP_WRAPPER_BE);
		themes.add(THM_DROPDOWN_ICON_MORE_VERT_BE);
		themes.add(THM_CARD_BOTTOM_GROUP_CLICKABLE_WRAPPER_BE);
		themes.add(THM_CARD_HEADER_INPUT_FIELD_BE);
		themes.add(THM_CARD_HEADER_INPUT_FIELD_INTERN_BE);
		themes.add(THM_CARD_BOTTOM_VCL_ICON_BE);
		themes.add(THM_ICON_WORK_BE);
		themes.add(THM_ICON_LOCATION_ON_BE);
		themes.add(THM_ICON_STAR_BE);
		themes.add(THM_ICON_DIRECTIONS_BUS_BE);
		themes.add(THM_ICON_PHONE_BE);
		themes.add(THM_ICON_EMAIL_BE);
		themes.add(THM_ICON_BE);
		themes.add(THM_HIDE_INPUT_BE);
		themes.add(THM_CARD_DATE_BE);
		themes.add(THM_CARD_DATE_WRAPPER_BE);
		themes.add(THM_CARD_MENU_GROUP_CONTENT_WRAPPER_BE);
		themes.add(THM_QUESTION_GRP_LABEL_BE);
		themes.add(THM_BH_ROW_ONE_GRP_WRAPPER_BE);
		themes.add(THM_BH_ROW_ONE_GRP_LABEL_BE);
		themes.add(THM_BH_ROW_ONE_GRP_CONTENT_WRAPPER_BE);
		themes.add(THM_BH_ROW_ONE_VCL_INPUT_BE);
		themes.add(THM_BH_ROW_TWO_VCL_WRAPPER_BE);
		themes.add(THM_BH_ROW_TWO_GRP_CONTENT_WRAPPER_BE);
		themes.add(THM_BH_ROW_TWO_INPUT_FIELD_BE);
		themes.add(THM_BH_GROUP_WRAPPER_BE);
		themes.add(THM_WIDTH_100_PERCENT_NO_INHERIT_BE);
		themes.add(THM_BUCKET_HEADER_GROUP_WRAPPER_BE);
		themes.add(THM_BUCKET_HEADER_GROUP_HEADER_WRAPPER_BE);
		themes.add(THM_BUCKET_HEADER_GROUP_LABEL_BE);

		System.out.println("**************** Themes in the list :: "+ themes.size() +" ********************");

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(themes);
		msg.setToken(beUtils.getGennyToken().getToken());
		System.out.println("**************** Sending all the bucket themes ********************");
		VertxUtils.writeMsg("webcmds",JsonUtils.toJson(msg));
		System.out.println("**************** Sent all the bucket themes ********************");

	}


	/* generates all the contextListMap for card */
	public Map<String, ContextList> getCardContextListMap(Map<String, ContextList> contextListMap,
			GennyToken serviceToken) {

		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);
		BucketUtils bucketUtils = new BucketUtils(beUtils);

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

			Theme THM_PROGRESS_VCL_INPUT = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_PROGRESS_VCL_INPUT", Theme.class, serviceToken.getToken());

			Theme THM_PROGRESS_INPUT_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_PROGRESS_INPUT_WRAPPER", Theme.class, serviceToken.getToken());
					
			Theme THM_BORDER_RADIUS_50 = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_BORDER_RADIUS_50",
					Theme.class, serviceToken.getToken());
					
			Theme THM_EXPANDABLE = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_EXPANDABLE", Theme.class,
					serviceToken.getToken());

			/* NOTE: if the card is not expanding:  Uncomment the following code, to cache the theme again and comment out again*/
			/* Theme THM_EXPANDABLE = Theme.builder("THM_EXPANDABLE")
													.addAttribute(ThemeAttributeType.PRI_IS_EXPANDABLE, true).end()
													.addAttribute(ThemeAttributeType.PRI_IS_INHERITABLE, false).end()
													.build();
			THM_EXPANDABLE.setRealm(serviceToken.getRealm());

			VertxUtils.putObject(serviceToken.getRealm(), "", THM_EXPANDABLE.getCode(),
			THM_EXPANDABLE,
			serviceToken.getToken()); */
					
			Theme THM_WIDTH_100_PERCENT = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_WIDTH_100_PERCENT",
					Theme.class, serviceToken.getToken());
					
			Theme THM_JUSTIFY_CONTENT_CENTRE = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_JUSTIFY_CONTENT_CENTRE", Theme.class, serviceToken.getToken());
					
			Theme THM_IMAGE_PLACEHOLDER_PERSON = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_IMAGE_PLACEHOLDER_PERSON", Theme.class, serviceToken.getToken());
					
			Theme THM_PROFILE_IMAGE = VertxUtils.getObject(serviceToken.getRealm(), "", "THM_PROFILE_IMAGE",
					Theme.class, serviceToken.getToken());
					
			Theme THM_PROJECT_COLOR_SURFACE = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_PROJECT_COLOR_SURFACE", Theme.class, serviceToken.getToken());
					
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

			/* new themes */
			Theme THM_CARD_STATUS_INPUT_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
						"THM_CARD_STATUS_INPUT_WRAPPER", Theme.class, serviceToken.getToken());

			Theme THM_CARD_THUMBNAIL_VCL_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
			"THM_CARD_THUMBNAIL_VCL_WRAPPER", Theme.class, serviceToken.getToken());

			Theme THM_CARD_THUMBNAIL_INPUT_PLACEHOLDER = VertxUtils.getObject(serviceToken.getRealm(), "",
			"THM_CARD_THUMBNAIL_INPUT_PLACEHOLDER", Theme.class, serviceToken.getToken());

			Theme THM_CARD_MAIN_GROUP_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
			"THM_CARD_MAIN_GROUP_WRAPPER", Theme.class, serviceToken.getToken());

			Theme THM_CARD_CENTRE_GROUP_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
			"THM_CARD_CENTRE_GROUP_WRAPPER", Theme.class, serviceToken.getToken());
			
			Theme THM_DROPDOWN_ICON_MORE_VERT = VertxUtils.getObject(serviceToken.getRealm(), "",
			"THM_DROPDOWN_ICON_MORE_VERT", Theme.class, serviceToken.getToken());

			Theme THM_CARD_BOTTOM_GROUP_CLICKABLE_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
			"THM_CARD_BOTTOM_GROUP_CLICKABLE_WRAPPER", Theme.class, serviceToken.getToken());

			Theme THM_CARD_HEADER_INPUT_FIELD = VertxUtils.getObject(serviceToken.getRealm(), "",
			"THM_CARD_HEADER_INPUT_FIELD", Theme.class, serviceToken.getToken());

			Theme THM_CARD_HEADER_INPUT_FIELD_INTERN = VertxUtils.getObject(serviceToken.getRealm(), "",
			"THM_CARD_HEADER_INPUT_FIELD_INTERN", Theme.class, serviceToken.getToken());

			Theme THM_CARD_BOTTOM_VCL_ICON = VertxUtils.getObject(serviceToken.getRealm(), "",
			"THM_CARD_BOTTOM_VCL_ICON", Theme.class, serviceToken.getToken());

			/* ICON TYPES */
			Theme THM_ICON_WORK = VertxUtils.getObject(serviceToken.getRealm(), "",
						"THM_ICON_WORK", Theme.class, serviceToken.getToken());

			Theme THM_ICON_LOCATION_ON = VertxUtils.getObject(serviceToken.getRealm(), "",
						"THM_ICON_LOCATION_ON", Theme.class, serviceToken.getToken());

			Theme THM_ICON_STAR = VertxUtils.getObject(serviceToken.getRealm(), "",
						"THM_ICON_STAR", Theme.class, serviceToken.getToken());

			Theme THM_ICON_DIRECTIONS_BUS = VertxUtils.getObject(serviceToken.getRealm(), "",
						"THM_ICON_DIRECTIONS_BUS", Theme.class, serviceToken.getToken());

			Theme THM_ICON_PHONE = VertxUtils.getObject(serviceToken.getRealm(), "",
						"THM_ICON_PHONE", Theme.class, serviceToken.getToken());

			Theme THM_ICON_EMAIL = VertxUtils.getObject(serviceToken.getRealm(), "",
					"THM_ICON_EMAIL", Theme.class, serviceToken.getToken());
		
			Theme THM_ICON = VertxUtils.getObject(serviceToken.getRealm(), "",
						"THM_ICON", Theme.class, serviceToken.getToken());
			
			Theme THM_HIDE_INPUT = VertxUtils.getObject(serviceToken.getRealm(), "",
						"THM_HIDE_INPUT", Theme.class, serviceToken.getToken());

			Theme THM_CARD_DATE = VertxUtils.getObject(serviceToken.getRealm(), "",
						"THM_CARD_DATE", Theme.class, serviceToken.getToken());
			
			Theme THM_CARD_DATE_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
						"THM_CARD_DATE_WRAPPER", Theme.class, serviceToken.getToken());
			
			Theme THM_CARD_MENU_GROUP_CONTENT_WRAPPER = VertxUtils.getObject(serviceToken.getRealm(), "",
						"THM_CARD_MENU_GROUP_CONTENT_WRAPPER", Theme.class, serviceToken.getToken());

			/* end new themes */

			/* prepare context for progress bar */
			Context progressVclInputContext = new Context(ContextType.THEME,
					bucketUtils.getThemeBe(THM_PROGRESS_VCL_INPUT), VisualControlType.VCL_INPUT, 1.0);
			progressVclInputContext.setDataType("Progress");
			Context progressInputWrapperContext = new Context(ContextType.THEME,
					bucketUtils.getThemeBe(THM_PROGRESS_INPUT_WRAPPER), VisualControlType.INPUT_WRAPPER, 1.0);
			progressInputWrapperContext.setDataType("Progress");

			/* prepare context for status bar */
			Context statusContext = new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_CARD_STATUS_INPUT_WRAPPER),
					VisualControlType.INPUT_WRAPPER, 1.0);
			statusContext.setDataType("Color");
			
			/* prepare context for image-thumbnail */
			Context thumbnailVclWrapperContext = new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_CARD_THUMBNAIL_VCL_WRAPPER),
					VisualControlType.VCL_WRAPPER, 1.0);
			thumbnailVclWrapperContext.setDataType("Image");
			
			Context thumbnailInputPlaceholderContext = new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_CARD_THUMBNAIL_INPUT_PLACEHOLDER),
					VisualControlType.INPUT_PLACEHOLDER, 1.0);
			thumbnailInputPlaceholderContext.setDataType("Image");
			
			/* cardContext */
			List<Context> cardContext = new ArrayList<>();
			cardContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DISPLAY_HORIZONTAL),
					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
			cardContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_CARD),
					VisualControlType.GROUP_WRAPPER, 1.0));

			/* NEW */
			Context hideContext = new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_HIDE_INPUT),
			VisualControlType.GROUP_CONTENT_WRAPPER, 1.0);
			hideContext.setDttCode("DTT_DUMMY");

			Context dateContext = new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_CARD_DATE),
			VisualControlType.VCL_DEFAULT, 1.0);
			dateContext.setDataType("java.time.LocalDate");

			Context dateWrapperContext = new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_CARD_DATE_WRAPPER),
			VisualControlType.VCL_WRAPPER, 1.0);
			dateContext.setDataType("java.time.LocalDate");

			cardContext.add(hideContext);
			cardContext.add(dateContext);
			cardContext.add(dateWrapperContext);
			cardContext.add(statusContext);
			cardContext.add(thumbnailVclWrapperContext);
			cardContext.add(thumbnailInputPlaceholderContext);

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

			/* NEW */
			Context cardMainGrpWrapperContext = new Context(ContextType.THEME,
					bucketUtils.getThemeBe(THM_CARD_MAIN_GROUP_WRAPPER), VisualControlType.GROUP_WRAPPER, 1.0);
			cardMainGrpWrapperContext.setDataType("Card Main Group");	
			cardMainContext.add(cardMainGrpWrapperContext);

			/* NEW END */

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
			
			/* NEW */
			Context cardheaderInputFieldContext = new Context(ContextType.THEME,
			bucketUtils.getThemeBe(THM_CARD_HEADER_INPUT_FIELD), VisualControlType.INPUT_FIELD, 1.0);
			cardheaderInputFieldContext.setDttCode("DTT_TEXT_NAME");	
			cardCentreContext.add(cardheaderInputFieldContext);
			
			Context cardheaderInputFieldInternContext = new Context(ContextType.THEME,
			bucketUtils.getThemeBe(THM_CARD_HEADER_INPUT_FIELD_INTERN), VisualControlType.INPUT_FIELD, 1.0);
			cardheaderInputFieldInternContext.setDttCode("DTT_TEXT_INTERN_NAME");	
			cardCentreContext.add(cardheaderInputFieldInternContext);

			/* NEW END */

			cardCentreContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_FLEX_ONE),
					VisualControlType.GROUP_WRAPPER, 1.0));

						
			/* NEW */
			Context cardCentreGrpWrapperContext = new Context(ContextType.THEME,
					bucketUtils.getThemeBe(THM_CARD_CENTRE_GROUP_WRAPPER), VisualControlType.GROUP_WRAPPER, 1.0);
			cardCentreGrpWrapperContext.setDataType("Card Centre Group");	
			cardCentreContext.add(cardCentreGrpWrapperContext);

			/* NEW END */

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
			cardRightContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DROPDOWN_ICON_MORE_VERT),
					VisualControlType.GROUP_ICON, 1.0));
			cardRightContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_CARD_MENU_GROUP_CONTENT_WRAPPER),
					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));

			/* cardBottomContext */
			List<Context> cardBottomContext = new ArrayList<>();
			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_CARD_BOTTOM_GROUP_CLICKABLE_WRAPPER),
					VisualControlType.GROUP_CLICKABLE_WRAPPER, 1.0));
			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_EXPANDABLE),
					VisualControlType.GROUP, 1.0));
			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_WIDTH_100_PERCENT),
					VisualControlType.GROUP, 1.0));
			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_DISPLAY_VERTICAL),
					VisualControlType.GROUP_CONTENT_WRAPPER, 1.0));
			cardBottomContext.add(progressVclInputContext);
			cardBottomContext.add(progressInputWrapperContext);		

			/* adding icons here */	

			Context workContext = new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_ICON_WORK),
			VisualControlType.VCL_ICON, 1.0);
			workContext.setDttCode("DTT_TEXT_OCCUPATION");
			Context locationOnContext = new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_ICON_LOCATION_ON),
			VisualControlType.VCL_ICON, 1.0);
			locationOnContext.setDttCode("DTT_TEXT_COMPASS");
			Context starContext = new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_ICON_STAR),
			VisualControlType.VCL_ICON, 1.0);
			starContext.setDttCode("DTT_DOUBLE_STAR_RATING");
			Context directionsBusContext = new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_ICON_DIRECTIONS_BUS),
			VisualControlType.VCL_ICON, 1.0);
			directionsBusContext.setDttCode("DTT_TEXT_TRANSPORT");
			Context phoneContext = new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_ICON_PHONE),
			VisualControlType.VCL_ICON, 1.0);
			phoneContext.setDttCode("DTT_MOBILE");
			Context emailContext = new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_ICON_EMAIL),
			VisualControlType.VCL_ICON, 1.0);
			emailContext.setDttCode("DTT_EMAIL");

			cardBottomContext.add(workContext);
			cardBottomContext.add(locationOnContext);
			cardBottomContext.add(starContext);
			cardBottomContext.add(directionsBusContext);
			cardBottomContext.add(phoneContext);
			cardBottomContext.add(emailContext);

			
			/* NEW */
			Context iconContext = new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_CARD_BOTTOM_VCL_ICON),
					VisualControlType.VCL_ICON, 1.0);
			cardBottomContext.add(iconContext);
			cardBottomContext.add(new Context(ContextType.THEME, bucketUtils.getThemeBe(THM_ICON),
					VisualControlType.VCL_INPUT, 1.0));
			/* NEW END */



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

		public HashMap<String, Ask[]> getOptionTemplate(String targetCode) {
		
		/* iniitialize empty HashMap */
		HashMap<String, Ask[]> map = new HashMap<String, Ask[]>(); 
		
		String sourceCode = "PER_SERVICE";
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));

		/* create childAsks */
		// view-profile ask
		Question viewProfileQues = new Question("QUE_VIEW_PROFILE", "View Profile", questionAttribute, true);
		Ask viewProfileAsk = new Ask(viewProfileQues, sourceCode, targetCode);

		// view-application ask
		Question viewApplicationQues = new Question("QUE_VIEW_APP", "View Application", questionAttribute, true);
		Ask viewApplicationAsk = new Ask(viewApplicationQues, sourceCode, targetCode);
		
		// reactivate ask
		Question reactivateQues = new Question("QUE_REACTIVATE_APP", "Reactivate", questionAttribute, true);
		Ask reactivateAsk = new Ask(reactivateQues, sourceCode, targetCode);
		
		// onHold ask
		Question onHoldQues = new Question("QUE_ON_HOLD_APP", "On Hold", questionAttribute, true);
		Ask onHoldAsk = new Ask(onHoldQues, sourceCode, targetCode);
		
		// delete ask
		Question deleteQues = new Question("QUE_DELETE_APP", "Delete", questionAttribute, true);
		Ask deleteAsk = new Ask(deleteQues, sourceCode, targetCode);
	
		// withdrawn ask
		Question withdrawnQues = new Question("QUE_WITHDRAWN_APP", "Withdrawn", questionAttribute, true);
		Ask withdrawnAsk = new Ask(withdrawnQues, sourceCode, targetCode);
		
		// reject ask
		Question rejectQues = new Question("QUE_REJECT_APP", "Reject", questionAttribute, true);
		Ask rejectAsk = new Ask(rejectQues, sourceCode, targetCode);
		
		// shortlist ask
		Question shortlistQues = new Question("QUE_SHORTLIST_APP", "Shortlist", questionAttribute, true);
		Ask shortlistAsk = new Ask(shortlistQues, sourceCode, targetCode);
		
		// interview ask
		Question interviewQues = new Question("QUE_INTERVIEW_APP", "Interview", questionAttribute, true);
		Ask interviewAsk = new Ask(interviewQues, sourceCode, targetCode);
		
		// offer ask
		Question offerQues = new Question("QUE_OFFER_APP", "Offer", questionAttribute, true);
		Ask offerAsk = new Ask(offerQues, sourceCode, targetCode);
		
		// view-internship-offer ask
		//Question startInternshipQues = new Question("QUE_START_INTERNSHIP", "Start Internship", questionAttribute, true);
		//Ask startInternshipAsk = new Ask(startInternshipQues, sourceCode, targetCode);
		
		// view-internship-offer ask
		Question viewInternshipOfferQues = new Question("QUE_VIEW_INTERNSHIP_OFFER", "View Internship Offer", questionAttribute, true);
		Ask viewInternshipOfferAsk = new Ask(viewInternshipOfferQues, sourceCode, targetCode);
		
		// view-induction-offer ask
		Question viewInductionOfferQues = new Question("QUE_VIEW_INDUCTION_OFFER", "View Induction Offer", questionAttribute, true);
		Ask viewInductionOfferAsk = new Ask(viewInductionOfferQues, sourceCode, targetCode);
		
		// finish-internship ask
		Question finishInternshipQues = new Question("QUE_FINISH_INTERNSHIP", "Finish Internship", questionAttribute, true);
		Ask finishInternshipAsk = new Ask(finishInternshipQues, sourceCode, targetCode);
		
		// logBooks ask
		Question logbooksQues = new Question("QUE_LOGBOOKS", "Logbooks", questionAttribute, true);
		Ask logbooksAsk = new Ask(logbooksQues, sourceCode, targetCode);

		/* createChildAsk array */
		Ask[] availableAsk= {viewProfileAsk, reactivateAsk, onHoldAsk, deleteAsk, withdrawnAsk};
		
		Ask[] appliedAsk= {viewProfileAsk, viewApplicationAsk, reactivateAsk, onHoldAsk, withdrawnAsk, rejectAsk, shortlistAsk};

		Ask[] shortlistedAsk= {viewProfileAsk, viewApplicationAsk, reactivateAsk, onHoldAsk, withdrawnAsk, rejectAsk, interviewAsk};

		Ask[] interviewedAsk= {viewProfileAsk, viewApplicationAsk, reactivateAsk, onHoldAsk, withdrawnAsk, rejectAsk, offerAsk};

		Ask[] offeredAsk= {viewProfileAsk, viewApplicationAsk, reactivateAsk, onHoldAsk, withdrawnAsk, rejectAsk, viewInternshipOfferAsk};
		
		Ask[] placedAsk= {viewProfileAsk, viewApplicationAsk, reactivateAsk, onHoldAsk, withdrawnAsk, rejectAsk, viewInternshipOfferAsk};

		Ask[] inProgressAsk = {viewProfileAsk, viewApplicationAsk, reactivateAsk, onHoldAsk, withdrawnAsk, rejectAsk, viewInternshipOfferAsk, viewInductionOfferAsk, finishInternshipAsk, logbooksAsk};

		map.put("AVAILABLE_INTERNS",availableAsk);
		map.put("APPLIED_APPLICATIONS",appliedAsk);
		map.put("SHORTLISTED_APPLICATIONS",shortlistedAsk);
		map.put("INTERVIEWED_APPLICATIONS",interviewedAsk);
		map.put("OFFERED_APPLICATIONS",offeredAsk);
		map.put("PLACED_APPLICATIONS",placedAsk);
		map.put("INPROGRESS_APPLICATIONS",inProgressAsk);
		
		return map;
	}

	/* returns a card template */
	public Ask getCardTemplate() {

		String sourceCode = "PER_SERVICE", targetCode = "PER_SERVICE";
		Attribute questionAttribute = new Attribute("QQQ_QUESTION_GROUP", "link", new DataType(String.class));
		Attribute statusAttribute = new Attribute("QQQ_QUESTION_GROUP_CARD_STATUS", "link", new DataType(String.class));
		Attribute mainAttribute = new Attribute("QQQ_QUESTION_GROUP_CARD_MAIN", "link", new DataType(String.class));
		Attribute contentAttribute = new Attribute("QQQ_QUESTION_GROUP_CARD_CONTENT", "link",
				new DataType(String.class));
		Attribute leftAttribute = new Attribute("QQQ_QUESTION_GROUP_CARD_LEFT", "link", new DataType(String.class));
		Attribute centreAttribute = new Attribute("QQQ_QUESTION_GROUP_CARD_CENTRE", "link", new DataType(String.class));
		Attribute rightAttribute = new Attribute("QQQ_QUESTION_GROUP_CARD_RIGHT", "link", new DataType(String.class));
		Attribute bottomAttribute = new Attribute("QQQ_QUESTION_GROUP_CARD_BOTTOM", "link", new DataType(String.class));

		try {

			// card ask
			Question cardQuestion = new Question("QUE_CARD_APPLICATION_TEMPLATE_GRP", "Card", questionAttribute, true);
			Ask cardAsk = new Ask(cardQuestion, sourceCode, targetCode);

			// status ask
			Question cardStatusQuestion = new Question("QUE_CARD_STATUS_GRP", "Card Status", statusAttribute, true);
			Ask cardStatusAsk = new Ask(cardStatusQuestion, sourceCode, targetCode);

			// main ask
			Question cardMainQuestion = new Question("QUE_CARD_MAIN_GRP", "Card Main", mainAttribute, true);
			Ask cardMainAsk = new Ask(cardMainQuestion, sourceCode, targetCode);
			cardMainAsk.setReadonly(true);

			// content ask
			Question cardContentQuestion = new Question("QUE_CARD_CONTENT_GRP", "Card Content", contentAttribute, true);
			Ask cardContentAsk = new Ask(cardContentQuestion, sourceCode, targetCode);

			// left ask
			Question cardLeftQuestion = new Question("QUE_CARD_LEFT_GRP", "Card Left", leftAttribute, true);
			Ask cardLeftAsk = new Ask(cardLeftQuestion, sourceCode, targetCode);

			// centre ask
			Question cardCentreQuestion = new Question("QUE_CARD_CENTRE_GRP", "Card Centre", centreAttribute, true);
			Ask cardCentreAsk = new Ask(cardCentreQuestion, sourceCode, targetCode);

			// right ask
			Question cardRightQuestion = new Question("QUE_CARD_RIGHT_GRP", "Card Right", rightAttribute, true);
			Ask cardRightAsk = new Ask(cardRightQuestion, sourceCode, targetCode);

			// // forward ask
			// Question cardForwardQuestion = new Question("QUE_FORWARD", "Forward",
			// questionAttribute, true);
			// Ask cardForwardAsk = new Ask(cardForwardQuestion, sourceCode, targetCode);
			//
			// // backward ask
			// Question cardBackwardQuestion = new Question("QUE_BACKWARD", "Backward",
			// questionAttribute, true);
			// Ask cardBackwardAsk = new Ask(cardBackwardQuestion, sourceCode, targetCode);
			//
			// Ask[] cardRightChildAsks = { cardForwardAsk, cardBackwardAsk };
			// cardRightAsk.setChildAsks(cardRightChildAsks);

			Ask[] cardContentChildAsks = { cardLeftAsk, cardCentreAsk, cardRightAsk };
			cardContentAsk.setChildAsks(cardContentChildAsks);

			// bottom ask
			Question cardBottomQuestion = new Question("QUE_CARD_BOTTOM_GRP", "Card Bottom", bottomAttribute, true);
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

	/* implements a template ask to the supplied askList */
	public List<Ask> implementCardTemplate(String code, List<Ask> askList, Ask tempAsk, Map<String, ContextList> contextListMap) {

		ContextList contextList = tempAsk.getContextList();
		List<Context> cardContext = contextListMap.get("QUE_CARD_APPLICATION_TEMPLATE_GRP").getContextList();

		for (Ask app : askList) {
			/* get the option menu item asks */
			Map<String, Ask[]> map = getOptionTemplate(app.getTargetCode());
			Ask[] optionAsks = map.get(code);

			Ask templateAsk = getCardTemplate();
			Ask[] templateAsks = templateAsk.getChildAsks();
			
			Ask cardStatusAsk = templateAsks[0];
			Ask cardMainAsk = templateAsks[1];
			Ask cardContentAsk = cardMainAsk.getChildAsks()[0];
			Ask cardLeftAsk = cardContentAsk.getChildAsks()[0];
			Ask cardCentreAsk = cardContentAsk.getChildAsks()[1];
			Ask cardRightAsk = cardContentAsk.getChildAsks()[2];
			Ask cardBottomAsk = cardMainAsk.getChildAsks()[1];

			/* setting the targetCode to options question */
			cardRightAsk.setChildAsks(optionAsks);

			Ask[] attributeArr = app.getChildAsks();

			List<Ask> attributeList = new ArrayList<Ask>(Arrays.asList(attributeArr));

			Ask[] cardStatusChildAsks = { attributeList.get(0) };
			cardStatusAsk.setChildAsks(cardStatusChildAsks);
			cardStatusAsk.setTargetCode(app.getTargetCode());
			
			cardMainAsk.setTargetCode(app.getTargetCode());
			cardContentAsk.setTargetCode(app.getTargetCode());
			cardRightAsk.setTargetCode(app.getTargetCode());
			
			// set the image ask read only
			Ask image = attributeList.get(1);
			image.setReadonly(true);
			
			Ask[] cardLeftChildAsks = { image };
			cardLeftAsk.setReadonly(true);
			cardLeftAsk.setChildAsks(cardLeftChildAsks);
			cardLeftAsk.setTargetCode(app.getTargetCode());

			Ask[] cardCentreChildAsks = { 
				attributeList.get(2), 
				attributeList.get(3), 
				attributeList.get(4),
				attributeList.get(5), 
				attributeList.get(6), 
				attributeList.get(7), 
				attributeList.get(8),  
			};
			cardCentreAsk.setReadonly(true);
			cardCentreAsk.setChildAsks(cardCentreChildAsks);
			cardCentreAsk.setTargetCode(app.getTargetCode());

			Ask[] cardBottomChildAsks = { 
				attributeList.get(9),
				attributeList.get(10),
				attributeList.get(11),
				attributeList.get(12),
				attributeList.get(13),
				attributeList.get(14),
			};
			cardBottomAsk.setReadonly(true);
			cardBottomAsk.setChildAsks(cardBottomChildAsks);
			cardBottomAsk.setTargetCode(app.getTargetCode());

			// we create a new ask with all the new groups
			app.setTargetCode(app.getTargetCode());
			app.setChildAsks(templateAsks);

			// we set the themes from cardAsk group to the the appAsk
			app.setContextList(contextList);
			app.setReadonly(true);
			
			/* add the contextList for the cardQuestion */
			contextListMap.put(app.getQuestionCode(), new ContextList(cardContext));

		}
		// return the updated askList
		return askList;
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

			// SearchEntity SBE_AVAILABLE_INTERNS = new SearchEntity("SBE_AVAILABLE_INTERNS", "Available")
			// 		.addSort("PRI_CREATED", "Created", SearchEntity.Sort.ASC)
			// 		.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "PER_%").addFilter("PRI_DISABLED", false)
			// 		.addFilter("PRI_IS_INTERN", true)

			// 		/* status and image */
			// 		.addColumn("PRI_STATUS_COLOR", "Status")
			// 		.addColumn("PRI_INTERN_IMAGE_URL", "Image")

			// 		/* Centre Card */
			// 		.addColumn("PRI_NAME", "Name")
			// 		.addColumn("PRI_STUDENT_ID", "Student ID")
			// 		.addColumn("PRI_INDUSTRY", "Industry")
			// 		.addColumn("PRI_EDU_PROVIDER_NAME", "Edu Provider")
			// 		.addColumn("PRI_DUMMY", "Dummy")
			// 		.addColumn("PRI_DUMMY_ONE", "Dummy")
			// 		.addColumn("PRI_DUMMY_TWO", "Dummy")

			// 		/* Bottom Card */
			// 		.addColumn("PRI_OCCUPATION", "Occupation").addColumn("PRI_COMPASS", "Address")
			// 		.addColumn("PRI_STAR_RATING", "Rating").addColumn("PRI_TRANSPORT", "Transport")
			// 		.addColumn("PRI_MOBILE", "Mobile").addColumn("PRI_EMAIL", "Email")
			// 		.addColumn("PRI_INTERN_EMAIL", "Dummy").setPageStart(0).setPageSize(1000);

			// SearchEntity SBE_APPLIED_APPLICATIONS = new SearchEntity("SBE_APPLIED_APPLICATIONS", "Applied")
			// 		.addSort("PRI_CREATED", "Created", SearchEntity.Sort.DESC)
			// 		.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "APP_%")
			// 		.addFilter("PRI_STATUS", SearchEntity.StringFilter.EQUAL, "APPLIED")
			// 		.addFilter("PRI_DISABLED", false)

			// 		/* status and image */
			// 		.addColumn("PRI_STATUS_COLOR", "Status")
			// 		.addColumn("PRI_INTERN_IMAGE_URL", "Image")

			// 		/* Centre Card */
			// 		.addColumn("PRI_INTERN_NAME", "Name").addColumn("PRI_INTERN_STUDENT_ID", "Student ID")
			// 		.addColumn("PRI_INDUSTRY", "Industry").addColumn("PRI_EDU_PROVIDER_NAME", "Edu Provider")
			// 		.addColumn("PRI_HOST_COMPANY_NAME", "Host Company").addColumn("PRI_DUMMY", "Dummy")
			// 		.addColumn("PRI_DUMMY_ONE", "Dummy")

			// 		/* Bottom Card */
			// 		.addColumn("PRI_OCCUPATION", "Occupation").addColumn("PRI_COMPASS", "Address")
			// 		.addColumn("PRI_STAR_RATING", "Rating").addColumn("PRI_TRANSPORT", "Transport")
			// 		.addColumn("PRI_INTERN_MOBILE", "Mobile").addColumn("PRI_INTERN_EMAIL", "Email")

			// 		.setPageStart(0).setPageSize(1000);

			// SearchEntity SBE_SHORTLISTED_APPLICATIONS = new SearchEntity("SBE_SHORTLISTED_APPLICATIONS", "Shortlisted")
			// 		.addSort("PRI_CREATED", "Created", SearchEntity.Sort.DESC)
			// 		.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "APP_%")
			// 		.addFilter("PRI_STATUS", SearchEntity.StringFilter.EQUAL, "SHORLISTED")
			// 		.addFilter("PRI_DISABLED", false)

			// 		/* status and image */
			// 		.addColumn("PRI_STATUS_COLOR", "Status")
			// 		.addColumn("PRI_INTERN_IMAGE_URL", "Image")

			// 		/* Centre Card */
			// 		.addColumn("PRI_INTERN_NAME", "Name").addColumn("PRI_INTERN_STUDENT_ID", "Student ID")
			// 		.addColumn("PRI_INDUSTRY", "Industry").addColumn("PRI_EDU_PROVIDER_NAME", "Edu Provider")
			// 		.addColumn("PRI_HOST_COMPANY_NAME", "Host Company").addColumn("PRI_DUMMY", "Dummy")
			// 		.addColumn("PRI_DUMMY_ONE", "Dummy")

			// 		/* Bottom Card */
			// 		.addColumn("PRI_OCCUPATION", "Occupation").addColumn("PRI_COMPASS", "Address")
			// 		.addColumn("PRI_STAR_RATING", "Rating").addColumn("PRI_TRANSPORT", "Transport")
			// 		.addColumn("PRI_INTERN_MOBILE", "Mobile").addColumn("PRI_INTERN_EMAIL", "Email")

			// 		.setPageStart(0).setPageSize(1000);

			// SearchEntity SBE_INTERVIEWED_APPLICATIONS = new SearchEntity("SBE_INTERVIEWED_APPLICATIONS", "Interviewed")
			// 		.addSort("PRI_CREATED", "Created", SearchEntity.Sort.DESC)
			// 		.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "APP_%")
			// 		.addFilter("PRI_STATUS", SearchEntity.StringFilter.EQUAL, "INTERVIEWED")
			// 		.addFilter("PRI_DISABLED", false)

			// 		/* status and image */
			// 		.addColumn("PRI_STATUS_COLOR", "Status")
			// 		.addColumn("PRI_INTERN_IMAGE_URL", "Image")

			// 		/* Centre Card */
			// 		.addColumn("PRI_INTERN_NAME", "Name").addColumn("PRI_INTERN_STUDENT_ID", "Student ID")
			// 		.addColumn("PRI_INDUSTRY", "Industry").addColumn("PRI_EDU_PROVIDER_NAME", "Edu Provider")
			// 		.addColumn("PRI_HOST_COMPANY_NAME", "Host Company")
			// 		.addColumn("PRI_INTERVIEW_START_DATETIME", "Interview Date:  ").addColumn("PRI_DUMMY", "Dummy")

			// 		/* Bottom Card */
			// 		.addColumn("PRI_OCCUPATION", "Occupation").addColumn("PRI_COMPASS", "Address")
			// 		.addColumn("PRI_STAR_RATING", "Rating").addColumn("PRI_TRANSPORT", "Transport")
			// 		.addColumn("PRI_INTERN_MOBILE", "Mobile").addColumn("PRI_INTERN_EMAIL", "Email")

			// 		.setPageStart(0).setPageSize(1000);

			// SearchEntity SBE_OFFERED_APPLICATIONS = new SearchEntity("SBE_OFFERED_APPLICATIONS", "Offered")
			// 		.addSort("PRI_CREATED", "Created", SearchEntity.Sort.DESC)
			// 		.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "APP_%")
			// 		.addFilter("PRI_STATUS", SearchEntity.StringFilter.EQUAL, "OFFERED")
			// 		.addFilter("PRI_DISABLED", false)

			// 		/* status and image */
			// 		.addColumn("PRI_STATUS_COLOR", "Status")
			// 		.addColumn("PRI_INTERN_IMAGE_URL", "Image")

			// 		/* Centre Card */
			// 		.addColumn("PRI_INTERN_NAME", "Name").addColumn("PRI_INTERN_STUDENT_ID", "Student ID")
			// 		.addColumn("PRI_INDUSTRY", "Industry").addColumn("PRI_EDU_PROVIDER_NAME", "Edu Provider")
			// 		.addColumn("PRI_HOST_COMPANY_NAME", "Host Company")
			// 		.addColumn("PRI_INTERNSHIP_START_DATE", "Start Date:  ").addColumn("PRI_DUMMY", "Dummy")

			// 		/* Bottom Card */
			// 		.addColumn("PRI_OCCUPATION", "Occupation").addColumn("PRI_COMPASS", "Address")
			// 		.addColumn("PRI_STAR_RATING", "Rating").addColumn("PRI_TRANSPORT", "Transport")
			// 		.addColumn("PRI_INTERN_MOBILE", "Mobile").addColumn("PRI_INTERN_EMAIL", "Email")

			// 		.setPageStart(0).setPageSize(1000);

			// SearchEntity SBE_PLACED_APPLICATIONS = new SearchEntity("SBE_PLACED_APPLICATIONS", "Placed")
			// 		.addSort("PRI_CREATED", "Created", SearchEntity.Sort.DESC)
			// 		.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "APP_%")
			// 		.addFilter("PRI_STATUS", SearchEntity.StringFilter.EQUAL, "PLACED").addFilter("PRI_DISABLED", false)

			// 		/* status and image */
			// 		.addColumn("PRI_STATUS_COLOR", "Status")
			// 		.addColumn("PRI_INTERN_IMAGE_URL", "Image")

			// 		/* Centre Card */
			// 		.addColumn("PRI_INTERN_NAME", "Name").addColumn("PRI_INTERN_STUDENT_ID", "Student ID")
			// 		.addColumn("PRI_INDUSTRY", "Industry").addColumn("PRI_EDU_PROVIDER_NAME", "Edu Provider")
			// 		.addColumn("PRI_HOST_COMPANY_NAME", "Host Company")
			// 		.addColumn("PRI_INTERNSHIP_START_DATE", "Start Date:  ").addColumn("PRI_DUMMY", "Dummy")

			// 		/* Bottom Card */
			// 		.addColumn("PRI_OCCUPATION", "Occupation").addColumn("PRI_COMPASS", "Address")
			// 		.addColumn("PRI_STAR_RATING", "Rating").addColumn("PRI_TRANSPORT", "Transport")
			// 		.addColumn("PRI_INTERN_MOBILE", "Mobile").addColumn("PRI_INTERN_EMAIL", "Email")

			// 		.setPageStart(0).setPageSize(1000);

			// SearchEntity SBE_INPROGRESS_APPLICATIONS = new SearchEntity("SBE_INPROGRESS_APPLICATIONS", "In Progress")
			// 		.addSort("PRI_CREATED", "Created", SearchEntity.Sort.DESC)
			// 		.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "APP_%")
			// 		.addFilter("PRI_STATUS", SearchEntity.StringFilter.EQUAL, "IN_PROGRESS")
			// 		.addFilter("PRI_DISABLED", false)

			// 		/* status and image */
			// 		.addColumn("PRI_STATUS_COLOR", "Status").addColumn("PRI_INTERN_IMAGE_URL", "Image")

			// 		/* Centre Card */
			// 		.addColumn("PRI_INTERN_NAME", "Name").addColumn("PRI_INTERN_STUDENT_ID", "Student ID")
			// 		.addColumn("PRI_INDUSTRY", "Industry").addColumn("PRI_EDU_PROVIDER_NAME", "Edu Provider")
			// 		.addColumn("PRI_HOST_COMPANY_NAME", "Host Company")
			// 		.addColumn("PRI_INTERNSHIP_START_DATE", "Start Date: ").addColumn("PRI_PROGRESS", "Progress")

			// 		/* Bottom Card */
			// 		.addColumn("PRI_OCCUPATION", "Occupation").addColumn("PRI_COMPASS", "Address")
			// 		.addColumn("PRI_STAR_RATING", "Rating").addColumn("PRI_TRANSPORT", "Transport")
			// 		.addColumn("PRI_INTERN_MOBILE", "Mobile").addColumn("PRI_INTERN_EMAIL", "Email")

			// 		.setPageStart(0).setPageSize(1000);

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

	/* generates the bucket-header ask */
	public Ask getBucketHeaderAsk(Map<String, ContextList> contextListMap, GennyToken serviceToken) {

		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);

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

		/* Validation for Search Attribute */
		Validation validation = new Validation("VLD_NON_EMPTY", "EmptyandBlankValues", "(?!^$|\\s+)");
		List<Validation> validations = new ArrayList<>();
		validations.add(validation);
		ValidationList searchValidationList = new ValidationList();
		searchValidationList.setValidationList(validations);

		/* get the attributes */
		Attribute countAttribute = RulesUtils.getAttribute("PRI_TOTAL_RESULTS", serviceToken.getToken());
		Attribute sortAttribute = RulesUtils.getAttribute("PRI_SORT", serviceToken.getToken());
		Attribute nameAttribute = RulesUtils.getAttribute("PRI_NAME", serviceToken.getToken());
		
		Attribute searchAttribute = new Attribute("PRI_NAME", "Search",
				new DataType("Text", searchValidationList, "Text"));

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
		row1Ask.setChildAsks(row1ChildAsks);

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
		//Ask[] bucketChildAsks = { row1Ask, row2Ask };
		Ask[] bucketChildAsks = { row1Ask };
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

	public void sendCards(Frame3 FRM_BUCKET_CONTENT, GennyToken userToken, GennyToken serviceToken) {
		
		/* initialize beUtils */
		BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
		beUtils.setServiceToken(serviceToken);

		/* initialize bucketUtils */
		BucketUtils bucketUtils = new BucketUtils(beUtils);

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

				log.info("inside search loop  ::");
				String code = searchBe.getCode().split("SBE_")[1];
				log.info("code  ::" +code );

				/* get the attributes from searchObj */
				Map<String, String> columns = searchUtils.getTableColumns(searchBe);

				/* fetch the search results */
				QDataBaseEntityMessage msg = searchUtils.fetchSearchResults(searchBe, serviceToken);

				if (msg == null) {
					System.out.println("The msg in " +code+" was null");
				} else {
					System.out.println("The items in " + code + " was " + msg.getItems().length + " items , with total="
					+ msg.getTotal());
				}

				/* get the application counts */
				long totalResults = msg.getItems().length;
				log.info("items in bucket " + code + " is :: " + totalResults );

				/* also update the searchBe with the attribute */
				Answer totalAnswer = new Answer(beUtils.getGennyToken().getUserCode(), searchBe.getCode(),
						"PRI_TOTAL_RESULTS", totalResults + "");
				beUtils.addAnswer(totalAnswer);
				beUtils.updateBaseEntity(searchBe, totalAnswer);

				/* get the applications */
				List<BaseEntity> appList = Arrays.asList(msg.getItems());

				/* add the application to the baseentity list */
				beList.addAll(appList);

				/* add the updated searchBe as well */
				beList.add(searchBe);

				/* convert app to asks */
				List<Ask> appAsksList = searchUtils.generateQuestions(beUtils.getGennyToken(), beUtils, appList,
						columns, beUtils.getGennyToken().getUserCode());
				
				/* get the templat ask for card */
				Ask templateAsk = bucketUtils.getCardTemplate();

				/* implement template ask to appAks list */
				List<Ask> askList = bucketUtils.implementCardTemplate(code, appAsksList, templateAsk, contextListMap);

				/* generate bucketContent asks for each bucket */
				Ask bucketContentAsk = Ask.clone(FRM_BUCKET_CONTENT_ASK);
				bucketContentAsk.setQuestionCode("QUE_BUCKET_CONTENT_" + code + "_GRP");
				bucketContentAsk.setName(searchBe.getName());

				/* link bucketContentAsk to application asks */
				bucketContentAsk.setChildAsks(askList.toArray(new Ask[askList.size()]));

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
				
				/* Send asks */
				for (QDataAskMessage askMsg : askSet) {
					
					log.info("Cards in the bucket :: " + askMsg.getItems()[0].getName() + " are  :: " + askMsg.getItems()[0].getChildAsks().length);

					askMsg.setToken(userToken.getToken());

					String json = JsonUtils.toJson(askMsg);
					VertxUtils.writeMsg("webcmds", json);

				}

			}

			/* Send */
			log.info("Sending application entitites");

			QDataBaseEntityMessage appMsg = new QDataBaseEntityMessage(beList.toArray(new BaseEntity[0]));
			appMsg.setToken(userToken.getToken());
			VertxUtils.writeMsg("webcmds", JsonUtils.toJson(appMsg));

			log.info("Sending asks from outside the loop");

			
			System.out.print("Completed");

		} catch (Exception e) {
			// TODO: handle exception

		}
	}

}
