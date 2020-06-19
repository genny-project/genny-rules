package life.genny.rules.processor;

import io.vavr.Tuple2;
import io.vavr.Tuple3;
import life.genny.qwandautils.Tuple;
import life.genny.rules.RulesLoader;
import org.apache.commons.digester.Rules;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.UUID;
import java.util.concurrent.SynchronousQueue;

public class RequestProcessor extends Thread {
    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
    RulesLoader rulesLoader;

    public RequestProcessor(RulesLoader rulesLoader) {
        this.rulesLoader = rulesLoader;
    }

    @Override
    public void run() {
        log.info("RequestProcessor started. RulesLoader instance:" + rulesLoader.toString());
        while (true) {
            try {
                Tuple3<Object, String, UUID> tuple = rulesLoader.getConcurrentLinkedQueue().poll();
                if (tuple != null) {
                    log.info("Process request uuid:" + tuple._3.toString() + ", RulesLoader instance:" + rulesLoader.toString());
                    rulesLoader.processMsg(tuple._1, tuple._2);
                    Thread.sleep(100);
                }
            } catch (InterruptedException | IllegalArgumentException e) {
                log.error("Exception occurred: RulesLoader instance:" + rulesLoader.toString());
                e.printStackTrace();
            }
        }
    }
}
