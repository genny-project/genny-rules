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
        log.info("RequestProcessor started. RulesLoader instance:" + rulesLoader.toString()
                + ", Linked session state:" + rulesLoader.getLinkedSessionState());
        Tuple3<Object, String, UUID> tuple = rulesLoader.getConcurrentLinkedQueue().poll();
        if (tuple != null) {
            rulesLoader.processMsg(tuple._1, tuple._2);
            log.info("Finished process request uuid:" + tuple._3.toString()
                    + ", RulesLoader instance:" + rulesLoader.toString()
                    + ", Linked session state:" + rulesLoader.getLinkedSessionState());
        }
    }
}
