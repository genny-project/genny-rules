package life.genny.jbpm.customworkitemhandlers;

import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.jbpm.services.task.utils.TaskFluent;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Task;
import org.kie.internal.runtime.manager.context.EmptyContext;

import io.vertx.core.json.JsonObject;
import life.genny.models.GennyToken;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGAttachment;
import life.genny.qwanda.message.QBaseMSGMessageType;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.VertxUtils;

/*
 * This workitem is for sending messages for JBPM Workflows.
 * message type supports email, SMS and other message services
 *
 * userToken.getRealm()
 */
public class TaskListCreatorHandler implements WorkItemHandler {

  protected static final Logger log =
      org.apache.logging.log4j.LogManager.getLogger(
          MethodHandles.lookup().lookupClass().getCanonicalName());

  public TaskListCreatorHandler() {}

  public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

    String taskName = (String) workItem.getParameter("taskName");
    String taskAdmin = (String) workItem.getParameter("taskAdmin");
    String taskPotentialGroup = (String) workItem.getParameter("taskPotentialGroup");
    String taskPotentialUser = (String) workItem.getParameter("taskPotentialUser");
    String taskProcessId = (String) workItem.getParameter("taskProcessId");
    String taskDeploymentId = (String) workItem.getParameter("taskDeploymentId");
    GennyToken userToken = (GennyToken) workItem.getParameter("userToken");

    log.info("taskName = " + taskName);
    log.info("taskAdmin = " + taskAdmin);
    log.info("taskPotentialGroup = " + taskPotentialGroup);
    log.info("taskPotentialUser = " + taskPotentialUser);
    log.info("taskProcessId = " + taskProcessId);
    log.info("taskDeploymentId = " + taskDeploymentId);
    log.info("userToken = " + userToken);

    RuntimeManager runtimeManager = null;

    RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(EmptyContext.get());
    TaskService taskService = runtimeEngine.getTaskService();

//    KieBase kieBase = RulesLoader.getKieBaseCache().get(userToken.getRealm());

    Map<String, Object> params = new HashMap<String, Object>();
    Task task =
        new TaskFluent()
            .setName("Amazing GADA Stuff")
            .addPotentialGroup("GADA")
            .setAdminUser("acrow")
            //   .addPotentialUser("acrow")
            .setProcessId("direct")
            .setDeploymentID("genny")
            .getTask();
    taskService.addTask(task, params);
    long taskId = task.getId();

    Map<String, Object> content = taskService.getTaskContent(taskId);
    System.out.println(content);
    
    // notify manager that work item has been completed
    manager.completeWorkItem(workItem.getId(), null);
  }

  public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    // Do nothing, notifications cannot be aborted
  }

  /*
   * Main Method for manipulate notification data
   *
   * @param arrRecipient	: who is going ot receieved the notification
   * @param contextMap     	: mapping the template variable to value
   * @param templateCode	: the code of the template that stored in the google spreadsheet
   * @param messageType    	: Can be "EMAIL","SMS"
   * @param attachmentList 	: Incase of email attachments
   *
   */
  public void sendNotification(
      String[] arrRecipient,
      HashMap<String, String> contextMap,
      String templateCode,
      QBaseMSGMessageType messageType,
      GennyToken userToken) {

    /* unsubscribe link for the template */
    System.out.println("GennySettings.projectUrl = " + GennySettings.projectUrl);
    String unsubscribeUrl =
        getUnsubscribeLinkForEmailTemplate(GennySettings.projectUrl, templateCode);

    QMessageGennyMSG notificatonContent = null;

    /* Adding project code to context */
    String projectCode = "PRJ_" + userToken.getRealm().toUpperCase();

    System.out.println("project code for messages ::" + projectCode);
    contextMap.put("PROJECT", projectCode);

    /* adding unsubscribe url */
    if (unsubscribeUrl != null) {
      contextMap.put("URL", unsubscribeUrl);
    }

    if (arrRecipient != null && arrRecipient.length > 0) {

      List<QBaseMSGAttachment> attachmentList = null;

      QMessageGennyMSG msgMessage =
          new QMessageGennyMSG(
              "MSG_MESSAGE", messageType, templateCode, contextMap, arrRecipient, attachmentList);
      msgMessage.setToken(userToken.getToken());

      log.info("------------------------------------------------------------------------");
      log.info("MESSAGE ::   " + msgMessage);
      log.info("------------------------------------------------------------------------");

      if (attachmentList == null) {
        notificatonContent =
            new QMessageGennyMSG(
                "MSG_MESSAGE", messageType, templateCode, contextMap, arrRecipient, attachmentList);
        msgMessage.setToken(userToken.getToken());
        System.out.println("Build QMessage : " + notificatonContent);
      }

    } else {
      log.error("Recipient array is null");
    }

    System.out.println("Passing to Vertx : " + userToken);
    // sending message to user such as cmds, webcmds, message
    VertxUtils.publish(getUser(userToken), "messages", JsonUtils.toJson(notificatonContent));
  }

  public BaseEntity getUser(GennyToken userToken) {
    BaseEntity be = null;
    BaseEntityUtils baseEntityUtils =
        new BaseEntityUtils(
            GennySettings.qwandaServiceUrl,
            userToken.getToken(),
            userToken.getAdecodedTokenMap(),
            userToken.getRealm());
    String username = (String) userToken.getAdecodedTokenMap().get("preferred_username");
    System.out.println("username : " + username);
    String code = "PER_" + QwandaUtils.getNormalisedUsername(username).toUpperCase();
    System.out.println("user code : " + code);
    try {
      be = baseEntityUtils.getBaseEntityByCode(code);
    } catch (Exception e) {
    }
    System.out.println("BaseEntity Name : " + be);
    return be;
  }

  /*
   * Provide Unsubscribe Link for email
   */
  public String getUnsubscribeLinkForEmailTemplate(String host, String templateCode) {

    JsonObject data = new JsonObject();
    data.put("loading", "Loading...");
    data.put("evt_type", "REDIRECT_EVENT");
    data.put("evt_code", "REDIRECT_UNSUBSCRIBE_MAIL_LIST");

    JsonObject dataObj = new JsonObject();
    dataObj.put("code", "REDIRECT_UNSUBSCRIBE_MAIL_LIST");
    dataObj.put("value", templateCode);

    data.put("data", dataObj);
    String redirectUrl = this.generateRedirectUrl(host, data);
    return redirectUrl;
  }

  public String generateRedirectUrl(String host, JsonObject data) {

    /* we stringify the json object */
    try {
      if (data != null) {
        /* we encode it for URL schema */
        String base64 = Base64.getEncoder().encodeToString(data.toString().getBytes());
        return host + "?state=" + base64;
      }
    } catch (Exception e) {
    }

    return null;
  }
}
