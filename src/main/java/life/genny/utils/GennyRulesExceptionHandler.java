package life.genny.utils;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.Logger;
import org.kie.api.runtime.rule.ConsequenceExceptionHandler;
import org.kie.api.runtime.rule.Match;
import org.kie.api.runtime.rule.RuleRuntime;

import life.genny.qwandautils.ANSIColour;

public class GennyRulesExceptionHandler implements ConsequenceExceptionHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    @Override
    public void handleException(Match match, RuleRuntime rr, Exception e) {
        //Do whatever you want
    	log.error(ANSIColour.RED+"RULES EXCEPTION (1) :match ->  "+match+ANSIColour.RESET);
    	log.error(ANSIColour.RED+"RULES EXCEPTION (2) :RuleRuntime ->  "+rr+ANSIColour.RESET);
    	log.error(ANSIColour.RED+"RULES EXCEPTION (3) in " + match.getRule().getName() + ": exception ->  "+e+ANSIColour.RESET);
    }

}
