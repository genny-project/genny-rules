package life.genny.utils;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.Logger;

import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.message.QBulkMessage;

public class SearchCallable implements Callable<QBulkMessage> {

    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    QBulkMessage ret = new QBulkMessage();
    TableUtils tableUtils;
    private SearchEntity searchBE;
    private BaseEntityUtils beUtils;
    Boolean cache = false;
    String filterCode = null;
    String filterValue = null;
    Boolean replace = true;

    public SearchCallable(TableUtils tableUtils, SearchEntity searchBE, BaseEntityUtils beUtils) {
        this(tableUtils, searchBE, beUtils, false);
    }

    public SearchCallable(TableUtils tableUtils, SearchEntity searchBE, BaseEntityUtils beUtils, String filterCode,
            String filterValue) {
        this(tableUtils, searchBE, beUtils, false, filterCode, filterValue);
    }

    public SearchCallable(TableUtils tableUtils, SearchEntity searchBE, BaseEntityUtils beUtils, Boolean cache) {
        this.tableUtils = tableUtils;
        this.beUtils = beUtils;
        this.searchBE = searchBE;
        this.cache = cache;
    }

    public SearchCallable(TableUtils tableUtils, SearchEntity searchBE, BaseEntityUtils beUtils, Boolean cache,
            String filterCode, String filterValue) {
        this.tableUtils = tableUtils;
        this.beUtils = beUtils;
        this.searchBE = searchBE;
        this.cache = cache;
        this.filterCode = filterCode;
        this.filterValue = filterValue;
    }

    public SearchCallable(TableUtils tableUtils, SearchEntity searchBE, BaseEntityUtils beUtils, Boolean cache,
            String filterCode, String filterValue, Boolean replace) {
        this.tableUtils = tableUtils;
        this.beUtils = beUtils;
        this.searchBE = searchBE;
        this.cache = cache;
        this.filterCode = filterCode;
        this.filterValue = filterValue;
        this.replace = replace;
    }

    public QBulkMessage call() {

        log.info("Starting Search! " + searchBE.getCode());

        QBulkMessage qbm1 = VertxUtils.getObject(beUtils.getGennyToken().getRealm(), "SPEEDUP", searchBE.getCode(),
                QBulkMessage.class);

        if ((qbm1 == null) || true) {
            qbm1 = tableUtils.performSearch(beUtils.getServiceToken(), searchBE, null, filterCode, filterValue, cache,
                    replace);
            VertxUtils.putObject(beUtils.getGennyToken().getRealm(), "SPEEDUP", searchBE.getCode(), qbm1,
                    beUtils.getGennyToken().getToken());
        } else {
            log.info("Fetching Table Search from Cache");
        }
        ret.add(qbm1);

        log.info("Finished Search with " + qbm1.getMessages().length + " items");
        return ret;
    }
}