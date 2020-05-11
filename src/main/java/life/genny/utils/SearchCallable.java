package life.genny.utils;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.Logger;

import life.genny.qwanda.message.QBulkMessage;

public class SearchCallable implements Callable<QBulkMessage> {
    
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    private String name;
    private long period;
    private CountDownLatch latch;
    QBulkMessage ret = new QBulkMessage();
    TableUtils tableUtils;
    private String searchBeCode;
    private BaseEntityUtils beUtils;
    
    public SearchCallable(TableUtils tableUtils,String searchBeCode,BaseEntityUtils beUtils, String name, long period, CountDownLatch latch) {
        this(tableUtils,searchBeCode, beUtils,name, period);
        this.latch = latch;
        
    }

    public SearchCallable(TableUtils tableUtils,String searchBeCode,BaseEntityUtils beUtils, String name, long period) {
        this.name = name;
        this.period = period;
        ret.setData_type(name);
        this.tableUtils = tableUtils;
        this.beUtils = beUtils;
        this.searchBeCode = searchBeCode;
    }

    public QBulkMessage call()  {
    	
        // Thread.sleep(period);
    	 log.info("Starting Search! "+searchBeCode);
            QBulkMessage qbm1 = tableUtils.performSearch(beUtils.getGennyToken(), beUtils.getServiceToken(), searchBeCode, null);
            ret.add(qbm1.getMessages());
            if (latch != null) {
                latch.countDown();
            }
            log.info("Finished Search!");
        return ret;
    }
}