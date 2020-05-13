package life.genny.utils;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;

import life.genny.jbpm.customworkitemhandlers.ShowFrame;
import life.genny.qwanda.message.QBulkMessage;

public class TableFrameCallable implements Callable<QBulkMessage> {
    
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


    QBulkMessage ret = new QBulkMessage();
    BaseEntityUtils beUtils;
    Boolean cache = false;
 

    public TableFrameCallable(BaseEntityUtils beUtils, Boolean cache) {

        this.beUtils = beUtils;
        this.cache = cache;
    }

    public QBulkMessage call() {
    	
        // Thread.sleep(period);
    	 log.info("Starting Table Frame construction ");
            QBulkMessage qbm1 = ShowFrame.display(beUtils.getGennyToken(), "FRM_QUE_TAB_VIEW", "FRM_CONTENT", "Test",cache);
            QBulkMessage qbm2 = ShowFrame.display(beUtils.getGennyToken(), "FRM_TABLE_VIEW", "FRM_TAB_CONTENT", "Test",cache);
            ret.add(qbm1);
            ret.add(qbm2);

            log.info("Finished Table Frame construction");
        return ret;
    }
}