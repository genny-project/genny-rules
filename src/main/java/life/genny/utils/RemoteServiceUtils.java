package life.genny.utils;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import life.genny.models.GennyToken;
import life.genny.qwanda.Ask;
import life.genny.qwanda.Link;
import life.genny.qwanda.Question;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QEventDropdownMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;




public class RemoteServiceUtils {

    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    BaseEntityUtils beUtils = null;

//    Write the constructor
    public RemoteServiceUtils(BaseEntityUtils beUtils) {
        this.beUtils = beUtils;
    }

//    First method
    public String invokeRemoteService(final String remoteServiceCode, final String... searchEntities) {
        List<SearchEntity> searchEntityList = new ArrayList<SearchEntity>();
        for (String searchBECode : searchEntities) {
            if (searchBECode != null) {
                if (searchBECode.startsWith("SBE_")) {
                    SearchEntity se = beUtils.getSearchEntityByCode(searchBECode);
                    if (se != null) {
                        searchEntityList.add(se);
                    } else {
                        log.error("Search entity does not exist for search code " + searchBECode);
                    }
                } else {
                    log.error("Bad search code passed to remote service utils " + searchBECode);
                }

            }
        }
        String uuid = null;
        if (!searchEntityList.isEmpty()) {
            String[] searchEntitiesArray = searchEntityList.toArray(new String[0]);
            this.invokeRemoteService(remoteServiceCode, Arrays.toString(searchEntitiesArray));
            uuid = UUID.randomUUID().toString().substring(0, 35);

        } else {
            log.error("No valid search entities for remote invocation");
        }
        return uuid;
    }




}
