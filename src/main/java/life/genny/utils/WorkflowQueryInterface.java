package life.genny.utils;

import java.util.Optional;

import life.genny.rules.RulesLoader;

public interface WorkflowQueryInterface {
	 // Static Method 
    public Optional<Long> getProcessIdBySessionId(String sessionId);
 
}
