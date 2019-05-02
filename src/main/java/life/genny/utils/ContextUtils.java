package life.genny.utils;

import java.util.HashSet;
import java.util.Set;

import life.genny.qwanda.Context;
import life.genny.qwanda.Context.VisualControlType;
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
		
		String borderAttribute = "{  \"borderStyle\": \"solid\", \"borderColour\" : \"#dee2e6\", \"borderWidth\" : 0 }";
		
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
		
		String bgAttribute = "{  \"backgroundColor\": \"#F2F2F2\", \"color\": \"#212529\" , \"padding\" : \"5px\", \"boxSizing\": \"borderBox\"}";
		
		Attribute att = new Attribute("PRI_CONTENT", "content", new DataType(String.class));
		EntityAttribute entAttr = new EntityAttribute(backgroundTheme, att, 1.0, bgAttribute);
		Set<EntityAttribute> entAttrSet = new HashSet<>();
		entAttrSet.add(entAttr);
		
		backgroundTheme.setBaseEntityAttributes(entAttrSet);
	
		return backgroundTheme;
	}
	
	public static BaseEntity getThemeForOddTableContent() {
		/* create context */
		BaseEntity backgroundTheme = new BaseEntity("THM_TABLE_ODD", "table background");
		
		String bgAttribute = "{  \"backgroundColor\": \"#FFFFFF\", \"color\": \"#212529\" , \"padding\" : \"5px\", \"boxSizing\": \"borderBox\"}";
		
		Attribute att = new Attribute("PRI_CONTENT", "content", new DataType(String.class));
		EntityAttribute entAttr = new EntityAttribute(backgroundTheme, att, 1.0, bgAttribute);
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

}
