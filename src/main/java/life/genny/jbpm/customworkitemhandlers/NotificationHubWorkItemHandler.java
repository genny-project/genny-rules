package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.logging.log4j.Logger;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import life.genny.models.GennyToken;
import life.genny.notifications.EmailHelper;
import life.genny.notifications.SmsHelper;
import life.genny.qwanda.message.QBaseMSGMessageType;

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
        (QBaseMSGMessageType) workItem.getParameter("notificationSignal");
    String templateCode = (String) workItem.getParameter("notificationTemplate");
    String[] arrNotificationRecipient = {
      (String) workItem.getParameter("arrNotificationRecipient")
    };
    HashMap<String, String> contextMap =
        (HashMap<String, String>) workItem.getParameter("templateMap");
    GennyToken userToken = (GennyToken) workItem.getParameter("userToken");

    log.info("notificationSignal = " + messageType);
    log.info("notificationTemplate = " + templateCode);
    log.info("arrNotificationRecipient = " + arrNotificationRecipient[0]);
    log.info("templateMap = " + contextMap);
    log.info("userToken = " + userToken);

    String recipientEmail = null;
    String recipientSms = null;

    /*
     *
     * SEND EMAIL
     *
     */
    if (messageType.toString() == "EMAIL") {
      EmailHelper emailHelper = new EmailHelper();
      String emailBody =
          emailHelper.prepareMessageBody(
              arrNotificationRecipient, contextMap, templateCode, messageType, userToken);

      try {
        recipientEmail =
            emailHelper.resolveRecipient(arrNotificationRecipient, contextMap, userToken);
      } catch (Exception e) { // TODO Auto-generated catch block
        e.printStackTrace();
      }
      /*
       * This function sends the email to recipient mailbox,
       * sending message to user such as cmds, webcmds, message
       */

      try {
        emailHelper.deliverEmailMsg("christopher.pyke@gada.io"/*recipientEmail*/, emailBody);
      } catch (AddressException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (MessagingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    /*
     *
     * SEND SMS
     *
     */
    if (messageType.toString() == "SMS") {
      SmsHelper smsHelper = new SmsHelper();
      String smsBody =
          smsHelper.prepareMessageBody(
              arrNotificationRecipient, contextMap, templateCode, messageType, userToken);

      try {
        recipientSms = smsHelper.resolveRecipient(arrNotificationRecipient, contextMap, userToken);
      } catch (Exception e1) { // TODO Auto-generated catch block
        e1.printStackTrace();
      }

      // This will send a SMS to the user
      recipientSms = "+61433501177";
      smsHelper.deliverSmsMsg(recipientSms, smsBody);
    }
    // notify manager that work item has been completed
    manager.completeWorkItem(workItem.getId(), null);
  }

  public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    // Do nothing, notifications cannot be aborted
  }
}
