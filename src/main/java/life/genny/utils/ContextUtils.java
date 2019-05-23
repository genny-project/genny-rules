package life.genny.utils;

import java.util.HashSet;
import java.util.Set;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;


public class ContextUtils {


	public static BaseEntity getLabelVisualControlContext() {
		/* create visual baseentity for question with label */
		BaseEntity visualBaseEntity = new BaseEntity("THM_VISUAL_CONTROL_LABEL", "Theme Visual Control For Label");

		Attribute labelAttr = new Attribute("PRI_HAS_LABEL", "Has Label?", new DataType(Boolean.class));
		EntityAttribute labelEntityAttribute = new EntityAttribute(visualBaseEntity, labelAttr, 1.0, "TRUE");
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(labelEntityAttribute);
		visualBaseEntity.setBaseEntityAttributes(attributeSet);

		return visualBaseEntity;
	}

	public static BaseEntity getBorderThemeForTableContent() {
		/* create context */
        /* getting the expandable theme baseentity */
		BaseEntity borderTheme = new BaseEntity("THM_TABLE_BORDER", "table border");

		String borderAttribute = "{  \"borderStyle\": \"solid\", \"borderColour\" : \"#dee2e6\", \"borderTop\" : 2 }";

		Attribute att = new Attribute("PRI_CONTENT", "content", new DataType(String.class));
		Attribute inheritableAtt = new Attribute("PRI_IS_INHERITABLE", "inheritable", new DataType(Boolean.class));
		EntityAttribute entAttr = new EntityAttribute(borderTheme, att, 1.0, borderAttribute);
		EntityAttribute inheritEntAtt = new EntityAttribute(borderTheme, inheritableAtt, 1.0, "FALSE");
		Set<EntityAttribute> entAttrSet = new HashSet<>();
		entAttrSet.add(entAttr);
		entAttrSet.add(inheritEntAtt);

		borderTheme.setBaseEntityAttributes(entAttrSet);

		return borderTheme;
	}

	public static BaseEntity getThemeForEvenTableContent() {
		/* create context */
		BaseEntity backgroundTheme = new BaseEntity("THM_TABLE_EVEN", "table background");

		String bgAttribute = "{  \"backgroundColor\": \"#F2F2F2\", \"color\": \"#212529\" , \"boxSizing\": \"borderBox\"}";

		Attribute att = new Attribute("PRI_CONTENT", "content", new DataType(String.class));
		EntityAttribute entAttr = new EntityAttribute(backgroundTheme, att, 2.0, bgAttribute);
		Set<EntityAttribute> entAttrSet = new HashSet<>();
		entAttrSet.add(entAttr);

		backgroundTheme.setBaseEntityAttributes(entAttrSet);

		return backgroundTheme;
	}

	public static BaseEntity getThemeForOddTableContent() {
		/* create context */
		BaseEntity backgroundTheme = new BaseEntity("THM_TABLE_ODD", "table background");

		String bgAttribute = "{  \"backgroundColor\": \"#FFFFFF\", \"color\": \"#212529\", \"boxSizing\": \"borderBox\"}";

		Attribute att = new Attribute("PRI_CONTENT", "content", new DataType(String.class));
		EntityAttribute entAttr = new EntityAttribute(backgroundTheme, att, 2.0, bgAttribute);
		Set<EntityAttribute> entAttrSet = new HashSet<>();
		entAttrSet.add(entAttr);

		backgroundTheme.setBaseEntityAttributes(entAttrSet);

		return backgroundTheme;
	}


	public static BaseEntity getFullWidthTheme() {
		/* create context */
		BaseEntity fullWidthTheme = new BaseEntity("THM_WIDTH_FULL", "Width 100 Percent");

		String bgAttribute = "{ \"width\": \"100%\", \"justifyContent\": \"space-around\"}";

		Attribute att = new Attribute("PRI_CONTENT", "content", new DataType(String.class));
		Attribute inheritableAtt = new Attribute("PRI_IS_INHERITABLE", "inheritable", new DataType(Boolean.class));
		EntityAttribute entAttr = new EntityAttribute(fullWidthTheme, att, 1.0, bgAttribute);
		EntityAttribute inheritEntAtt = new EntityAttribute(fullWidthTheme, inheritableAtt, 1.0, "FALSE");
		Set<EntityAttribute> entAttrSet = new HashSet<>();
		entAttrSet.add(entAttr);
		entAttrSet.add(inheritEntAtt);

		fullWidthTheme.setBaseEntityAttributes(entAttrSet);

		return fullWidthTheme;
	}

	public static BaseEntity getThemeForTableScroll() {
		/* create context */
		BaseEntity tableScrollTheme = new BaseEntity("THM_VERTICAL_SCROLL", "table scroll");

		String bgAttribute = "{  \"overflowY\": \"auto\"}";

		Attribute att = new Attribute("PRI_CONTENT", "content", new DataType(String.class));
		EntityAttribute entAttr = new EntityAttribute(tableScrollTheme, att, 1.0, bgAttribute);
		Set<EntityAttribute> entAttrSet = new HashSet<>();
		entAttrSet.add(entAttr);

		tableScrollTheme.setBaseEntityAttributes(entAttrSet);

		return tableScrollTheme;
	}
	public static BaseEntity getThemeForDelimiter() {
		/* create context */
		BaseEntity tableDelimiterTheme = new BaseEntity("THM_TABLE_COLOMN_DELIMITER", "table column delimiter");

		String bgAttribute = "{  \"backgroundColor\": \"red\" , \"padding\": 1 }";

		Attribute att = new Attribute("PRI_CONTENT", "content", new DataType(String.class));
		Attribute inheritableAtt = new Attribute("PRI_IS_INHERITABLE", "inheritable", new DataType(Boolean.class));

		EntityAttribute entAttr = new EntityAttribute(tableDelimiterTheme, att, 1.0, bgAttribute);
		EntityAttribute inheritEntAtt = new EntityAttribute(tableDelimiterTheme, inheritableAtt, 1.0, "FALSE");

		Set<EntityAttribute> entAttrSet = new HashSet<>();
		entAttrSet.add(entAttr);
		entAttrSet.add(inheritEntAtt);

		tableDelimiterTheme.setBaseEntityAttributes(entAttrSet);

		return tableDelimiterTheme;
	}
	
/* START - THEMES FOR FORMS */
	
	public static BaseEntity getDefaultFormInputTheme() {
		
		BaseEntity formInputThemeBe = new BaseEntity("THM_FORM_DEFAULT_INPUT", "Theme Form Default Input");
		
		Attribute contentAttribute = new Attribute("PRI_CONTENT", "Content", new DataType(String.class));
		String themeContent = "{ \"borderBottomWidth\": 1,\"borderColor\": \"#ddd\", \"borderStyle\": \"solid\"}";
		
		Attribute hoverAttribute = new Attribute("PRI_CONTENT_HOVER", "Content Hover", new DataType(String.class));
		String hoverContent = "{ \"borderColor\": \"#aaa\" }";
		
		Attribute activeAttribute = new Attribute("PRI_CONTENT_ACTIVE", "Content Active", new DataType(String.class));
		String activeContent = "{ \"borderColor\": \"green\" }";
		
		Attribute errorAttribute = new Attribute("PRI_CONTENT_ERROR", "Content Error", new DataType(String.class));
		String errorContent = "{ \"borderColor\": \"red\" }";
		
		EntityAttribute contentEntityAttribute = new EntityAttribute(formInputThemeBe, contentAttribute, 1.0, themeContent);
		EntityAttribute hoverEntityAttribute = new EntityAttribute(formInputThemeBe, hoverAttribute, 1.0, hoverContent);
		EntityAttribute activeEntityAttribute = new EntityAttribute(formInputThemeBe, activeAttribute, 1.0, activeContent);
		EntityAttribute errorEntityAttribute = new EntityAttribute(formInputThemeBe, errorAttribute, 1.0, errorContent);
		
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(contentEntityAttribute);
		attributeSet.add(hoverEntityAttribute);
		attributeSet.add(activeEntityAttribute);
		attributeSet.add(errorEntityAttribute);
		formInputThemeBe.setBaseEntityAttributes(attributeSet);
		
		return formInputThemeBe;
	}
	
	public static BaseEntity getDefaultFormWrapperTheme() {
		
		BaseEntity formDefaultWrappeBaseEntity = new BaseEntity("THM_FORM_DEFAULT_WRAPPER", "Theme Form Default Wrapper");
		
		Attribute contentAttribute = new Attribute("PRI_CONTENT", "Content", new DataType(String.class));
		String themeContent = "{ \"backgroundColor\": \"#fff\",\"marginBottom\": 10, \"padding\": 10}";
		
		Attribute errorAttribute = new Attribute("PRI_CONTENT_ERROR", "Content Error", new DataType(String.class));
		String errorContent = "{ \"backgroundColor\": \"#fce8e6\" }";
		
		EntityAttribute contentEntityAttribute = new EntityAttribute(formDefaultWrappeBaseEntity, contentAttribute, 1.0, themeContent);
		EntityAttribute errorEntityAttribute = new EntityAttribute(formDefaultWrappeBaseEntity, errorAttribute, 1.0, errorContent);
		
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(contentEntityAttribute);
		attributeSet.add(errorEntityAttribute);
		formDefaultWrappeBaseEntity.setBaseEntityAttributes(attributeSet);

		return formDefaultWrappeBaseEntity;
	}
	
	public static BaseEntity getDefaultFormLabelTheme() {
		
		BaseEntity formDefaultLebelBe = new BaseEntity("THM_FORM_DEFAULT_LABEL", "Theme Form Default Label");
		
		Attribute contentAttribute = new Attribute("PRI_CONTENT", "Content", new DataType(String.class));
		String themeContent = "{ \"bold\": true,\"size\": \"md\"}";
		
		EntityAttribute contentEntityAttribute = new EntityAttribute(formDefaultLebelBe, contentAttribute, 1.0, themeContent);
		
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(contentEntityAttribute);
		formDefaultLebelBe.setBaseEntityAttributes(attributeSet);

		return formDefaultLebelBe;
	}
	
	public static BaseEntity getDefaultFormErrorTheme() {
		
		BaseEntity formDefaultErrorThemeBe = new BaseEntity("THM_FORM_DEFAULT_ERROR", "Theme Form Default Error");
		
		Attribute contentAttribute = new Attribute("PRI_CONTENT", "Content", new DataType(String.class));
		String themeContent = "{ \"color\": red }";
		
		EntityAttribute contentEntityAttribute = new EntityAttribute(formDefaultErrorThemeBe, contentAttribute, 1.0, themeContent);
		
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(contentEntityAttribute);
		formDefaultErrorThemeBe.setBaseEntityAttributes(attributeSet);

		return formDefaultErrorThemeBe;
	}
	
	public static BaseEntity getDefaultFormContainerTheme() {
		
		BaseEntity formDefaultContainerThemeBe = new BaseEntity("THM_FORM_DEFAULT_CONTAINER", "Theme Form Default Container");
		
		Attribute contentAttribute = new Attribute("PRI_CONTENT", "Content", new DataType(String.class));
		String themeContent = "{ \"backgroundColor\": \"white\", \"padding\": 10, \"maxWidth\": 700, \"width\": \"100%\", \"shadowColor\": \"#000\", \"shadowOpacity\": 0.4, \"shadowRadius\" : 5, \"shadowOffset\": {\"width\": 0, \"height\": 0} }";
		
		Attribute isInheritableAttribute = new Attribute("PRI_IS_INHERITABLE", "Is Group Inheritable?", new DataType(Boolean.class));
		
		EntityAttribute contentEntityAttribute = new EntityAttribute(formDefaultContainerThemeBe, contentAttribute, 1.0, themeContent);
		EntityAttribute inheritableEntityAttribute = new EntityAttribute(formDefaultContainerThemeBe, isInheritableAttribute, 1.0, "FALSE");
		
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(contentEntityAttribute);
		attributeSet.add(inheritableEntityAttribute);
		formDefaultContainerThemeBe.setBaseEntityAttributes(attributeSet);

		return formDefaultContainerThemeBe;
	}
	
	public static BaseEntity getDefaultFormTheme() {
		
		BaseEntity formDefaultThemeBe = new BaseEntity("THM_FORM_DEFAULT", "Theme Form Default");
		
		Attribute contentAttribute = new Attribute("PRI_CONTENT", "Content", new DataType(String.class));
		String themeContent = "{ \"backgroundColor\": \"none\" }";
		
		Attribute hasTitleAttribute = new Attribute("PRI_HAS_QUESTION_GRP_TITLE", "Has Question Group Title?", new DataType(Boolean.class));
		Attribute hasDescriptionAttribute = new Attribute("PRI_HAS_QUESTION_GRP_DESCRIPTION", "Has Question Group Description?", new DataType(Boolean.class));
		Attribute hasLabelAttribute = new Attribute("PRI_HAS_LABEL", "Has Label?", new DataType(Boolean.class));
		Attribute hasRequiredAttribute = new Attribute("PRI_HAS_REQUIRED", "Has Required?", new DataType(Boolean.class));
		Attribute hasIconAttribute = new Attribute("PRI_HAS_ICON", "Has Icon?", new DataType(Boolean.class));
		
		EntityAttribute contentEntityAttribute = new EntityAttribute(formDefaultThemeBe, contentAttribute, 1.0, themeContent);
		EntityAttribute hasTitleEntityAttribute = new EntityAttribute(formDefaultThemeBe, hasTitleAttribute, 1.0, "TRUE");
		EntityAttribute hasDescriptionEntityAttribute = new EntityAttribute(formDefaultThemeBe, hasDescriptionAttribute, 1.0, "TRUE");
		EntityAttribute hasLabelEntityAttribute = new EntityAttribute(formDefaultThemeBe, hasLabelAttribute, 1.0, "TRUE");
		EntityAttribute hasRequiredEntityAttribute = new EntityAttribute(formDefaultThemeBe, hasRequiredAttribute, 1.0, "TRUE");
		EntityAttribute hasIconEntityAttribute = new EntityAttribute(formDefaultThemeBe, hasIconAttribute, 1.0, "TRUE");
		
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(contentEntityAttribute);
		attributeSet.add(hasTitleEntityAttribute);
		attributeSet.add(hasDescriptionEntityAttribute);
		attributeSet.add(hasLabelEntityAttribute);
		attributeSet.add(hasRequiredEntityAttribute);
		attributeSet.add(hasIconEntityAttribute);
		formDefaultThemeBe.setBaseEntityAttributes(attributeSet);

		return formDefaultThemeBe;
	}
	
	public static BaseEntity getFormButtonTheme() {
		
		BaseEntity formButtonThemeBe = new BaseEntity("THM_FORM_BUTTON", "Theme Form button");
		
		Attribute contentAttribute = new Attribute("PRI_CONTENT", "Content", new DataType(String.class));
		String themeContent = "{ \"backgroundColor\": \"#1A73E8\", \"padding\": 10, \"borderRadius\": 5, \"color\": \"white\", \"weight\": \"fit-content\", \"justifyContent\": \"center\" }";
		Attribute hasLabelAttribute = new Attribute("PRI_HAS_LABEL", "Has Label?", new DataType(Boolean.class));
		
		EntityAttribute contentEntityAttribute = new EntityAttribute(formButtonThemeBe, contentAttribute, 1.0, themeContent);
		EntityAttribute hasLabelEntityAttribute = new EntityAttribute(formButtonThemeBe, hasLabelAttribute, 1.0, "FALSE");
		
		Set<EntityAttribute> attributeSet = new HashSet<>();
		attributeSet.add(contentEntityAttribute);
		attributeSet.add(hasLabelEntityAttribute);
		formButtonThemeBe.setBaseEntityAttributes(attributeSet);

		return formButtonThemeBe;
	}
	
	/* END - THEMES FOR FORMS */

}