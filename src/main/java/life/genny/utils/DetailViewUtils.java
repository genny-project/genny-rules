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
import java.util.Collections;

import com.google.gson.reflect.TypeToken;
import com.google.gson.internal.LinkedTreeMap;

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
import life.genny.qwanda.Link;
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

public class DetailViewUtils {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public DetailViewUtils() {}

	public static void sendDetailView(BaseEntityUtils beUtils, String code, BaseEntity target)
	{
		if (target == null) {
			log.error("Target BE is NULL");
			return;
		}
		log.info("Generating detail view " + code + " for " + target.getCode());

		String sourceCode = beUtils.getGennyToken().getUserCode();
		String token = beUtils.getGennyToken().getToken();
		
		// Grab template map from cache
		LinkedHashMap<String, Object> map = VertxUtils.getObject(beUtils.getGennyToken().getRealm(), "", code,
				LinkedHashMap.class, beUtils.getGennyToken().getToken());

		if (map == null) {
			log.error("NULL in cach for " + code);
			return;
		}

		// Build the Ask Grp
		Ask askGrp = recursivelyConfigureAskFromMap(beUtils, map, "QUE_"+code+"_GRP", sourceCode, target.getCode());

		// Find the associated values from linked BEs
		target = recursivelyFindAssociatedValues(beUtils, target, askGrp);

		// Send the Asks and Target BE
		VertxUtils.sendAskMsg(beUtils, askGrp);
		VertxUtils.sendBaseEntityMsg(beUtils, target);

	}

	/**
	* Used to configure the ask group recursively.
	* @param beUtils - The beUtils to help assist
	* @param map - The map to traverse
	* @param sourceCode - The source entity code
	* @param targetCode - The target entity code
	 */
	public static Ask recursivelyConfigureAskFromMap(BaseEntityUtils beUtils, Map<String, Object> map, String rootCode, String sourceCode, String targetCode)
	{
		Attribute questionAttribute = RulesUtils.getAttribute("QQQ_QUESTION_GROUP", beUtils.getServiceToken());
		Question grpQuestion = new Question(rootCode, questionAttribute.getName(), questionAttribute);
		Ask ask = new Ask(grpQuestion, sourceCode, targetCode);
		// Set ReadOnly True
		ask.setReadonly(true);

		List<Ask> children = new ArrayList<>();

		for (String key : map.keySet()) {
			Object value = map.get(key);

			Ask childAsk = null;

			if (value instanceof LinkedHashMap || value instanceof LinkedTreeMap) {

				Map<String, Object> nestedMap = (Map) value;

				childAsk = recursivelyConfigureAskFromMap(beUtils, nestedMap, key, sourceCode, targetCode);

			} else if (value instanceof String) {

				String attrCode = (String) value;

				String[] fields = attrCode.split("__"); 
				String linkBeCode = fields[fields.length-1];

				Attribute primaryAttribute = RulesUtils.getAttribute(linkBeCode, beUtils.getServiceToken());
				Attribute att = new Attribute(attrCode, primaryAttribute.getName(), primaryAttribute.getDataType());

				Question question = new Question(key, att.getName(), att);

				childAsk = new Ask(question, sourceCode, targetCode);
				childAsk.setReadonly(true);

			}

			if (childAsk != null) {
				children.add(childAsk);
			}
		}

		ask.setChildAsks(children.toArray(new Ask[children.size()]));

		return ask;

	}

	/**
	* Used to find the associated values
	* for the asks in a recursive fashion
	* @param beUtils - The beUtils to help assist
	* @param be - The be to find data for
	* @param ask - The ask to traverse
	 */
	public static BaseEntity recursivelyFindAssociatedValues(BaseEntityUtils beUtils, BaseEntity be, Ask ask)
	{
		if (ask == null) {
			log.error("ask is NULL");
			return be;
		}
		String attrCode = ask.getAttributeCode();

		if (attrCode.startsWith("QQQ_QUESTION_GROUP")) {

			for (Ask childAsk : ask.getChildAsks()) {
				recursivelyFindAssociatedValues(beUtils, be, childAsk);
			}
		// Only fire for assoc values, others should already be in the BE
		} else if (attrCode.startsWith("_LNK") || attrCode.startsWith("_PRI")) {

			Answer ans = TableUtils.getAssociatedColumnValue(beUtils, be, "COL_"+attrCode, beUtils.getServiceToken());

			if (ans != null) {
				try {
					be.addAnswer(ans);
				} catch (BadDataException e) {
					e.printStackTrace();
				}
			}

		}
		return be;

	}

}
