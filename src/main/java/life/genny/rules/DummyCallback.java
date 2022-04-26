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

public class DummyCallback implements UserGroupCallback {

	public DummyCallback() {

	}

	@Override
	public boolean existsUser(String userId) {
		return true;
	}

	@Override
	public boolean existsGroup(String groupId) {
		return true;
	}

	@Override
	public List<String> getGroupsForUser(String userId) {
		// groupID must supply realm+groupId e.g. internmatch+GRP_USERS
		List<String> ret = new ArrayList<String>();

		ret.add("GRP_USERS");
		return ret;
	}

	private String getServiceToken(String realm) {
		JsonObject tokenObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM, "TOKEN" + realm.toUpperCase());
		String token = tokenObj.getString("value");
		return token;

	}

	private String getUserToken(String realm, String userCode, GennyToken serviceToken) {
		userCode = userCode.toUpperCase();
		JsonObject tokenObj = VertxUtils.readCachedJson(realm, userCode, serviceToken);
		String token = tokenObj.getString("value");
		return token;

	}

}
