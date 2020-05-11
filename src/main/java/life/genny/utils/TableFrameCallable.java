package life.genny.utils;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.Logger;

import life.genny.jbpm.customworkitemhandlers.ShowFrame;
import life.genny.qwanda.message.QBulkMessage;

public class TableFrameCallable implements Callable<QBulkMessage> {
    
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


    QBulkMessage ret = new QBulkMessage();
    BaseEntityUtils beUtils;
    
 

    public TableFrameCallable(BaseEntityUtils beUtils) {

        this.beUtils = beUtils;
    }

    public QBulkMessage call() {
    	
        // Thread.sleep(period);
    	 log.info("Starting Table Frame construction ");
            QBulkMessage qbm1 = ShowFrame.display(beUtils.getGennyToken(), "FRM_QUE_TAB_VIEW", "FRM_CONTENT", "Test");
            QBulkMessage qbm2 = ShowFrame.display(beUtils.getGennyToken(), "FRM_TABLE_VIEW", "FRM_TAB_CONTENT", "Test");
            ret.add(qbm1.getMessages());
            ret.add(qbm2.getMessages());

            log.info("Finished Table Frame construction");
        return ret;
    }
}