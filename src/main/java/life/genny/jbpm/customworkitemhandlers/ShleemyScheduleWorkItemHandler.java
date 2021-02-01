package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import life.genny.models.GennyToken;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.notifications.EmailHelper;
import life.genny.notifications.SmsHelper;
import life.genny.qwanda.message.QBaseMSGMessageType;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.ShleemyUtils;

import java.time.LocalDateTime;

/*
 * This workitem is for scheduling Shleemy messages 
 * from the workflows.
 *
 */
public class ShleemyScheduleWorkItemHandler implements WorkItemHandler {

  protected static final Logger log =
      org.apache.logging.log4j.LogManager.getLogger(
          MethodHandles.lookup().lookupClass().getCanonicalName());


  public ShleemyScheduleWorkItemHandler() {}

  public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

    GennyToken userToken = (GennyToken) workItem.getParameter("userToken");

    LocalDateTime triggerTime = (LocalDateTime) workItem.getParameter("triggerTime");

    String cron = (String) workItem.getParameter("cron");

	BaseEntity baseEntityTarget = (BaseEntity) workItem.getParameter("baseEntityTarget");
	String targetCode = baseEntityTarget.getCode();

	String eventMsgCode = (String) workItem.getParameter("eventMsgCode");
	
	String scheduleMsgCode = (String) workItem.getParameter("scheduleMsgCode");

    log.info("userToken = " + userToken);
    log.info("triggerTime = " + triggerTime.toString());
    log.info("cron = " + cron);
    log.info("baseEntityTarget = " + baseEntityTarget);
    log.info("eventMsgCode = " + eventMsgCode);
    log.info("scheduleMsgCode = " + scheduleMsgCode);

	ShleemyUtils.scheduleMessage(userToken, eventMsgCode, scheduleMsgCode, triggerTime, targetCode);
		
    // notify manager that work item has been completed
    manager.completeWorkItem(workItem.getId(), null);
  }

  public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    // Do nothing, notifications cannot be aborted
  }
}
