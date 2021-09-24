package life.genny.rules.processor;

import io.vavr.Tuple3;
import life.genny.rules.RulesLoader;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

public class RequestProcessor extends Thread {
    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
    RulesLoader rulesLoader;

    public RequestProcessor(RulesLoader rulesLoader) {
        this.rulesLoader = rulesLoader;
    }

    @Override
    public void run() {
        while (true) {
            try {
                //Retrieves and removes the head of this queue, waiting if necessary until an element becomes
                //     available.
                Tuple3<Object, String, UUID> tuple = rulesLoader.getLinkedBlockingQueue().take();
                long startTime = System.currentTimeMillis();
                log.info("~~~~~~~~~~~~~~~~~ REQUEST PROC START .   UUID:"+tuple._3.toString()
                        + ", Linked session state:" + rulesLoader.getLinkedSessionState());
                rulesLoader.processMsg(tuple._1, tuple._2);
                long endTime = System.currentTimeMillis();
                log.info("~~~~~~~~~~~~~~~~~ REQUEST PROC END "+(endTime-startTime)+" ms  UUID:" + tuple._3.toString()
                      //  + ", RulesLoader instance:" + rulesLoader.toString()
                        + ", Linked session state:" + rulesLoader.getLinkedSessionState());
                // Only for debug, disable when in production
                log.debug("Queue size is:" + rulesLoader.getLinkedBlockingQueue().size());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
