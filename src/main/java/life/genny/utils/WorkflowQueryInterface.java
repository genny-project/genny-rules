package life.genny.utils;

import java.util.Optional;


public interface WorkflowQueryInterface {
	 // Static Method 
    public Optional<Long> getProcessIdBySessionId(String sessionId);
 
}
