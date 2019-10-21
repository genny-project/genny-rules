package life.genny.rules;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.kie.internal.task.api.UserGroupCallback;

import com.google.gson.reflect.TypeToken;

import io.vertx.core.json.JsonObject;
import life.genny.models.GennyToken;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.VertxUtils;

public class GennyUsersCallback implements UserGroupCallback  {

	
	public GennyUsersCallback ()
	{
		
	}
	
	@Override
	public boolean existsUser(String userId) {
		// userID must supply realm+userId e.g.  internmatch+PER_USER1
		String[] str = null;
		try {
			str = userId.split("\\+");
		} catch (Exception e) {
			return false;
		}
		if (str.length != 2) {
			if (userId.equals("Administrator")) {
				return true;
			}
			return false;
		}
		String realm = str[0];
		String userCode = str[1];
		// Ideally we look up a keycloak 
		String serviceToken = getServiceToken(realm);
		if (serviceToken != null) {
			BaseEntityUtils beUtils = new BaseEntityUtils(new GennyToken(serviceToken));
			BaseEntity user = beUtils.getBaseEntityByCode(userCode);
			if (user != null) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean existsGroup(String groupId) {
		// groupID must supply realm+groupId e.g.  internmatch+GRP_USERS
		String[] str = null;
		try {
			str = groupId.split("\\+");
		} catch (Exception e) {
			return false;
		}
		if (str.length != 2) {
			if (groupId.equals("Administrators")) {
				return true;
			}
			return false;
		}
		String realm = str[0];
		String groupCode = str[1];
		// Ideally we look up a keycloak group check,
		// but for now check if group exists in cache
		String serviceToken = getServiceToken(realm);
		if (serviceToken != null) {
			BaseEntityUtils beUtils = new BaseEntityUtils(new GennyToken(serviceToken));
			BaseEntity group = beUtils.getBaseEntityByCode(groupCode);
			if (group != null) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public List<String> getGroupsForUser(String userId) {
		// groupID must supply realm+groupId e.g.  internmatch+GRP_USERS
		List<String> ret = new ArrayList<String>();
		
		String[] str = null;
		try {
			str = userId.split("\\+");
		} catch (Exception e) {
			return new ArrayList<String>();
		}
		if (str.length != 2) {
			return new ArrayList<String>();
		}
		String realm = str[0];
		String userCode = str[1];
		// Ideally we look up a keycloak user check,
		// but for now check if group exists in cache
		String serviceToken = getServiceToken(realm);
		if (serviceToken != null) {
			BaseEntityUtils beUtils = new BaseEntityUtils(new GennyToken(serviceToken));
			BaseEntity user = beUtils.getBaseEntityByCode(userCode);
			if (user != null) {
				Type type = new TypeToken<List<String>>() {
				}.getType();
				Optional<String> groupsStrOptional = user.getValue("PRI_GROUPS");
				if (groupsStrOptional.isPresent()) {
					ret = JsonUtils.fromJson(groupsStrOptional.get(), type);
				}
			//	Set<EntityEntity> links = user.getLinks();
			//	ret = links.parallelStream().filter(p -> p.getLink().getLinkValue().equals("LINK")) .map(p -> p.getLink().getSourceCode()).collect(Collectors.toCollection(ArrayList::new));
				return ret;
			}
		}
		ret.add("GRP_USERS");
		return ret;
	}
	
	
	private String getServiceToken(String realm)
	{
		JsonObject tokenObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM,
				"TOKEN" + realm.toUpperCase());
		String token = tokenObj.getString("value");
		return token;

	}
	
	private String getUserToken(String realm,String userCode, String serviceToken)
	{
		userCode = userCode.toUpperCase();
		JsonObject tokenObj = VertxUtils.readCachedJson(realm,
				userCode,serviceToken);
		String token = tokenObj.getString("value");
		return token;

	}
	
	
	
}
