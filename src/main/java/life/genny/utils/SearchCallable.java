package life.genny.utils;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.Logger;

import life.genny.qwanda.message.QBulkMessage;

public class SearchCallable implements Callable<QBulkMessage> {
    
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    QBulkMessage ret = new QBulkMessage();
    TableUtils tableUtils;
    private String searchBeCode;
    private BaseEntityUtils beUtils;
    Boolean cache= false;


    public SearchCallable(TableUtils tableUtils,String searchBeCode,BaseEntityUtils beUtils) {
        this(tableUtils, searchBeCode, beUtils, false);
    }
    public SearchCallable(TableUtils tableUtils,String searchBeCode,BaseEntityUtils beUtils, Boolean cache) {
        this.tableUtils = tableUtils;
        this.beUtils = beUtils;
        this.searchBeCode = searchBeCode;
        this.cache = cache;
    }

    public QBulkMessage call()  {
    	
    	 log.info("Starting Search! "+searchBeCode);
            QBulkMessage qbm1 = tableUtils.performSearch(beUtils.getGennyToken(), beUtils.getServiceToken(), searchBeCode, null, cache);
            ret.add(qbm1);

            log.info("Finished Search with "+qbm1.getMessages().length+" items");
        return ret;
    }
}