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
    


    public SearchCallable(TableUtils tableUtils,String searchBeCode,BaseEntityUtils beUtils) {
        this.tableUtils = tableUtils;
        this.beUtils = beUtils;
        this.searchBeCode = searchBeCode;
    }

    public QBulkMessage call()  {
    	
    	 log.info("Starting Search! "+searchBeCode);
            QBulkMessage qbm1 = tableUtils.performSearch(beUtils.getGennyToken(), beUtils.getServiceToken(), searchBeCode, null);
            ret.add(qbm1.getMessages());

            log.info("Finished Search with ");
        return ret;
    }
}