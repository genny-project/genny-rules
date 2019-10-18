package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.Logger;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

public class QueryNodeStatus implements WorkItemHandler {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    public QueryNodeStatus() {
        super();
    }

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.info("Executing Awesome handler");
        manager.completeWorkItem(workItem.getId(), null);
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.warn("Aborting");
    }
}