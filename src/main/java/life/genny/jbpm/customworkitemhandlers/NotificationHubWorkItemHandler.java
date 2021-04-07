package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import life.genny.models.GennyToken;
import life.genny.notifications.EmailHelper;
import life.genny.notifications.SmsHelper;
import life.genny.qwanda.message.QBaseMSGMessageType;
import life.genny.utils.BaseEntityUtils;

/*
 * This workitem is for sending messages for JBPM Workflows.
 * message type supports email, SMS and other message services
 *
 * userToken.getRealm()
 */
public class NotificationHubWorkItemHandler implements WorkItemHandler {

  protected static final Logger log =
      org.apache.logging.log4j.LogManager.getLogger(
          MethodHandles.lookup().lookupClass().getCanonicalName());

  //  private Properties mailServerProperties;
  //  private Session getMailSession;
  //  private MimeMessage generateMailMessage;

  public NotificationHubWorkItemHandler() {}

  public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

    // extract parameters
    QBaseMSGMessageType messageType =
        (QBaseMSGMessageType) workItem.getParameter("notificationType");
    String template_id = (String) workItem.getParameter("templateID");
    String notificationRecipient = (String) workItem.getParameter("notificationRecipient");
    String[] ccArray = {
      (String) workItem.getParameter("ccArray")
    };
    String[] bccArray = {
      (String) workItem.getParameter("bccArray")
    };
    HashMap<String, String> templateData =
        (HashMap<String, String>) workItem.getParameter("templateData");
    GennyToken userToken = (GennyToken) workItem.getParameter("userToken");

	BaseEntityUtils beUtils = new BaseEntityUtils(userToken);

    log.info("notificationType = " + messageType);
    log.info("templateID = " + template_id);
    log.info("notificationRecipient = " + notificationRecipient);
    log.info("ccArray = " + Arrays.toString(ccArray));
    log.info("bccArray = " + bccArray.toString());
    log.info("templateData = " + templateData);
    log.info("userToken = " + userToken);

	List<String> ccList = Arrays.asList(ccArray);
	List<String> bccList = Arrays.asList(bccArray);

	if (ccList.get(0).isEmpty()) {
		ccList = null;
	}
	if (bccList.get(0).isEmpty()) {
		bccList = null;
	}

	try {
			
		if (messageType.toString() == "EMAIL") {

			log.info("Sending EMAIL to " + notificationRecipient);
			EmailHelper.sendGrid(beUtils, notificationRecipient, ccList, bccList, "", template_id, templateData);

		} else if (messageType.toString() == "SMS") {
			
			SmsHelper smsHelper = new SmsHelper();
			String smsBody = templateData.get("smsBody");
			log.info("Sending SMS to " + notificationRecipient);
			smsHelper.deliverSmsMsg(notificationRecipient, smsBody);

		}
	} catch (IOException e) {
		e.printStackTrace();	
	}
    // notify manager that work item has been completed
    manager.completeWorkItem(workItem.getId(), null);
  }

  public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    // Do nothing, notifications cannot be aborted
  }
}
