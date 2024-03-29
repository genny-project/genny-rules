package life.genny.jbpm.customworkitemhandlers;

import com.google.gson.reflect.TypeToken;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import life.genny.models.Frame3;
import life.genny.models.FramePosition;
import life.genny.models.GennyToken;
import life.genny.qwanda.Ask;
import life.genny.qwanda.ContextList;
import life.genny.qwanda.TaskAsk;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QMessage.MsgOption;
import life.genny.qwanda.utils.OutputParam;
import life.genny.qwanda.validation.Validation;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.rules.RulesLoader;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.DefUtils;
import life.genny.utils.DropdownUtils;
import life.genny.utils.FrameUtils2;
import life.genny.utils.RulesUtils;
import life.genny.utils.SearchUtils;
import life.genny.utils.TaskUtils;
import life.genny.utils.VertxUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Task;

public class ShowFrame implements WorkItemHandler {

  public ShowFrame() {}

  protected static final Logger log =
      org.apache.logging.log4j.LogManager.getLogger(
          MethodHandles.lookup().lookupClass().getCanonicalName());

  public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
    /* items used to save the extracted input parameters from the custom task */
    Map<String, Object> items = workItem.getParameters();

    // extract parameters
    GennyToken userToken = (GennyToken) workItem.getParameter("userToken");
    GennyToken serviceToken = (GennyToken) workItem.getParameter("serviceToken");
    String rootFrameCode = (String) workItem.getParameter("rootFrameCode");
    String targetFrameCode = (String) workItem.getParameter("targetFrameCode");
    OutputParam output = (OutputParam) workItem.getParameter("output");

    BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken, userToken);

    if (rootFrameCode.equals("NONE")) { // Do not change anything
      return;
    }

    String callingWorkflow = (String) items.get("callingWorkflow");
    if (StringUtils.isBlank(callingWorkflow)) {
      callingWorkflow = "";
    }
    callingWorkflow += ":" + workItem.getProcessInstanceId() + ": ";

    Boolean cache = false;
    QBulkMessage qBulkMessage =
        display(beUtils, rootFrameCode, targetFrameCode, callingWorkflow, output, cache);

    if (cache) {
      qBulkMessage.setToken(userToken.getToken());
      VertxUtils.writeMsg("webcmds", JsonUtils.toJson(qBulkMessage));
    }

    VertxUtils.writeMsgEnd(userToken);

    // notify manager that work item has been completed
    if (workItem == null) {
      log.error(callingWorkflow + ": workItem is null");
    }
    manager.completeWorkItem(workItem.getId(), null);
  }

  public static QBulkMessage display(
      BaseEntityUtils beUtils,
      String rootFrameCode,
      String targetFrameCode,
      String callingWorkflow) {
    return display(beUtils, rootFrameCode, targetFrameCode, callingWorkflow, false);
  }

  public static QBulkMessage display(
      BaseEntityUtils beUtils,
      String rootFrameCode,
      String targetFrameCode,
      String callingWorkflow,
      Boolean cache) {
    QBulkMessage qBulkMessage = new QBulkMessage();

    OutputParam output = new OutputParam();

    output.setTypeOfResult("FORMCODE");
    output.setResultCode(rootFrameCode);
    output.setTargetCode(targetFrameCode);
    qBulkMessage = display(beUtils, rootFrameCode, targetFrameCode, callingWorkflow, output, cache);
    return qBulkMessage;
  }

  /**
   * @param userToken
   * @param rootFrameCode
   * @param targetFrameCode
   * @param callingWorkflow
   */
  public static QBulkMessage display(
      BaseEntityUtils beUtils,
      String rootFrameCode,
      String targetFrameCode,
      String callingWorkflow,
      OutputParam output) {
    return display(beUtils, rootFrameCode, targetFrameCode, callingWorkflow, output, false);
  }

  public static QBulkMessage display(
      BaseEntityUtils beUtils,
      String rootFrameCode,
      String targetFrameCode,
      String callingWorkflow,
      OutputParam output,
      Boolean cache) {
    QBulkMessage qBulkMessage = new QBulkMessage();

    GennyToken userToken = beUtils.getGennyToken();

    if (beUtils.getGennyToken() == null) {
      log.error(callingWorkflow + ": Must supply userToken!");

    } else {
      // log.info("userToken = " + userToken.getCode());

      if (rootFrameCode == null) {
        log.error(callingWorkflow + ": Must supply a root Frame Code!");
      } else {
        // log.info(callingWorkflow+": root Frame Code sent to display = " +
        // rootFrameCode);

        QDataBaseEntityMessage FRM_MSG =
            VertxUtils.getObject(
                userToken.getRealm(),
                "",
                rootFrameCode + "_MSG",
                QDataBaseEntityMessage.class,
                userToken.getToken());

        if (FRM_MSG == null) {
          // Construct it on the fly
          Frame3 frame =
              VertxUtils.getObject(
                  userToken.getRealm(), "", rootFrameCode, Frame3.class, userToken.getToken());

          if (frame == null) {
            if (VertxUtils.cachedEnabled) {
              return qBulkMessage; // don't worry about it
            }
            log.error(callingWorkflow + ": FRAME IS NOT IN CACHE  - " + rootFrameCode);
            // ok, grab frame from rules

            BaseEntity rule = beUtils.getBaseEntityByCode("RUL_" + rootFrameCode);
            if (rule != null) {
              Optional<String> optionFrameStr = rule.getValue("PRI_FRM"); // assume always
              if (optionFrameStr.isPresent()) {
                String frameStr = optionFrameStr.get();
                if (frameStr != null) {
                  VertxUtils.cacheInterface.writeCache(
                      userToken.getRealm(),
                      rootFrameCode + "_FRM",
                      frameStr,
                      userToken.getToken(),
                      0);
                } else {
                  log.error(rootFrameCode + " HAS NOT BEEN GENERATED BY THE RULES, abort display");
                  // return;
                }
              } else {
                log.error(
                    rootFrameCode
                        + " HAS NOT BEEN GENERATED BY THE RULES, abort display - No frame option"
                        + " --> NEED TO REGENERATE FRAMES!!! - exiting display frame");
                return qBulkMessage;
              }
            } else {
              log.error(
                  "RUL_" + rootFrameCode + " HAS NOT BEEN GENERATED BY THE RULES, abort display");
              // return;
            }
          }
          if (frame != null) {
            if (frame.getCode() == null) {
              frame =
                  VertxUtils.getObject(
                      userToken.getRealm(), "", rootFrameCode, Frame3.class, userToken.getToken());

              log.error(callingWorkflow + ": frame.getCode() in display  is null ");
              // return;
            }
          }
          FrameUtils2.toMessage2(frame, userToken);
          FRM_MSG =
              VertxUtils.getObject(
                  userToken.getRealm(),
                  "",
                  rootFrameCode + "_MSG",
                  QDataBaseEntityMessage.class,
                  userToken.getToken());
        }

        if (FRM_MSG != null) {

          if (targetFrameCode == null) {
            targetFrameCode = "FRM_ROOT";
          }

          QDataBaseEntityMessage TARGET_FRM_MSG =
              VertxUtils.getObject(
                  userToken.getRealm(),
                  "",
                  targetFrameCode + "_MSG",
                  QDataBaseEntityMessage.class,
                  userToken.getToken());

          if (TARGET_FRM_MSG == null) {
            // Construct it on the fly
            Frame3 frame =
                VertxUtils.getObject(
                    userToken.getRealm(), "", targetFrameCode, Frame3.class, userToken.getToken());

            FrameUtils2.toMessage2(frame, userToken);
            TARGET_FRM_MSG =
                VertxUtils.getObject(
                    userToken.getRealm(),
                    "",
                    targetFrameCode + "_MSG",
                    QDataBaseEntityMessage.class,
                    userToken.getToken());
          }

          for (BaseEntity targetFrame : TARGET_FRM_MSG.getItems()) {
            if (targetFrame.getCode().equals(targetFrameCode)) {
              //
              //							log.info(callingWorkflow+": ShowFrame : Found Targeted Frame BaseEntity : " +
              // targetFrame);

              /* Adding the links in the targeted BaseEntity */
              Attribute attribute =
                  new Attribute("LNK_FRAME", "LNK_FRAME", new DataType(String.class));

              for (BaseEntity sourceFrame : FRM_MSG.getItems()) {
                if (sourceFrame.getCode().equals(rootFrameCode)) {

                  // log.info(callingWorkflow+": ShowFrame : Found Source Frame BaseEntity : " +
                  // sourceFrame);
                  EntityEntity entityEntity =
                      new EntityEntity(targetFrame, sourceFrame, attribute, 1.0, "CENTRE");
                  Set<EntityEntity> entEntList = targetFrame.getLinks();
                  entEntList.add(entityEntity);
                  targetFrame.setLinks(entEntList);

                  /* Adding Frame to Targeted Frame BaseEntity Message */
                  FRM_MSG.add(targetFrame);
                  FRM_MSG.setReplace(true);
                  break;
                }
              }
              break;
            }
          }

          log.info(
              callingWorkflow + ": ShowFrame !!!!! : " + rootFrameCode + ":" + targetFrameCode);

          FRM_MSG.setReplace(true);
          if (cache) {
            qBulkMessage.add(FRM_MSG);
          } else {
            FRM_MSG.setToken(userToken.getToken());
            log.info(callingWorkflow + ": Sending FRM_MSG");
            VertxUtils.writeMsg("webcmds", FRM_MSG);
          }

          // Minify
          //					String payload = JsonUtils.toJson(FRM_MSG);
          //					try {
          //						JSONObject js = new JSONObject(payload);
          //						js.put("token", userToken.getToken());
          //						String payload2 = js.toString();
          //						if (payload2 != null) {
          //						//	VertxUtils.writeMsg("webcmds", payload2);
          //						VertxUtils.writeMsg("webcmds", FRM_MSG);
          //
          //						}
          //					} catch (JSONException e) {
          //						// TODO Auto-generated catch block
          //						e.printStackTrace();
          //					}
          QBulkMessage asks = sendAsks(rootFrameCode, beUtils, callingWorkflow, output, cache);
          qBulkMessage.add(asks);
        } else {
          log.error(
              callingWorkflow
                  + ": "
                  + rootFrameCode
                  + "_MSG"
                  + " DOES NOT EXIST IN CACHE - cannot display frame");
        }
      }
    }
    log.info("Sending the EndMsg Now !!!");

    return qBulkMessage;
  }

  /**
   * @param aask
   * @param callingWorkflow
   */
  private static void checkAskValidation(Ask aask, String callingWorkflow) {
    if (aask.getAttributeCode().equals("QQQ_QUESTION_GROUP")) {
      for (Ask childAsk : aask.getChildAsks()) {
        checkAskValidation(childAsk, callingWorkflow);
      }
    } else {
      for (Validation val : aask.getQuestion().getAttribute().getDataType().getValidationList()) {
        if (val != null) {
          if (val.getRegex() == null) {
            log.error(callingWorkflow + ": Regex for " + aask.getQuestion().getCode() + " == null");
          }
        } else {
          log.error("Validation is null for " + aask.getQuestion().getAttribute().getCode());
        }
      }
    }
  }

  /**
   * @param rootFrameCode
   * @param userToken
   * @param callingWorkflow
   */
  private static QBulkMessage sendSmallDropdowns(
      BaseEntity defBe,
      String rootFrameCode,
      GennyToken userToken,
      String callingWorkflow,
      OutputParam output,
      Boolean cache) {
    QBulkMessage qBulkMessage = new QBulkMessage();
    BaseEntityUtils beUtils = new BaseEntityUtils(userToken);

    if (VertxUtils.cachedEnabled) {
      // No point sending asks
      return qBulkMessage;
    }

    TaskService taskService;
    Task task = null;
    Map<String, Object> taskAsks = null;
    Map<String, TaskAsk> attributeTaskAskMap = null;
    String sourceCode = null;
    String targetCode = null;
    Boolean enabledSubmit = false;

    BaseEntity target = null;
    BaseEntity source = null;

    if ((output != null)) {
      log.info("Ouput Task ID = " + output.getTaskId());
      if ((output.getTaskId() != null) && (output.getTaskId() > 0L)) {
        taskService = RulesLoader.taskServiceMap.get(userToken.getJTI());
        task = taskService.getTaskById(output.getTaskId());
        // Now get the TaskAsk that relates to this specific Ask
        // assume that all attributes have the same source and target
        Long docId = task.getTaskData().getDocumentContentId();
        Content c = taskService.getContentById(docId);
        if (c == null) {
          log.error(
              callingWorkflow + ": *************** Task content is NULL *********** ABORTING");
          return qBulkMessage;
        }
        taskAsks =
            (HashMap<String, Object>) ContentMarshallerHelper.unmarshall(c.getContent(), null);
        for (String key : taskAsks.keySet()) {
          Object obj = taskAsks.get(key);
          if (obj instanceof TaskAsk) { // This gets around my awful formcode values
            TaskAsk taskAsk = (TaskAsk) taskAsks.get(key);
            String attributeStr = taskAsk.getAsk().getAttributeCode();
            // attributeTaskAskMap.put(attributeStr,taskAsk);
            sourceCode = taskAsk.getAsk().getSourceCode();
            targetCode = taskAsk.getAsk().getTargetCode();
          }
        }

        target = beUtils.getBaseEntityByCode(targetCode);
        source = beUtils.getBaseEntityByCode(sourceCode);

        defBe = beUtils.getDEF(target);
        enabledSubmit = TaskUtils.areAllMandatoryQuestionsAnswered(target, taskAsks);

      } else {
        sourceCode = output.getAskSourceCode();
        targetCode = output.getAskTargetCode();
        target = beUtils.getBaseEntityByCode(targetCode);
        source = beUtils.getBaseEntityByCode(sourceCode);
      }
    }

    Set<QDataAskMessage> askMsgs2 = fetchAskMessages(rootFrameCode, userToken);

    // log.info("Sending Asks");
    if ((askMsgs2 != null) && (!askMsgs2.isEmpty())) {
      for (QDataAskMessage askMsg : askMsgs2) { // TODO, not needed
        for (Ask aask : askMsg.getItems()) {
          log.info(callingWorkflow + ": aask: " + aask);

          /* recursively check validations */
          checkAskValidation(aask, callingWorkflow);
          TaskUtils.enableAttribute("PRI_SUBMIT", aask, callingWorkflow, enabledSubmit);
          aask.setId(output.getTaskId());
        }
        askMsg.setToken(userToken.getToken());

        /* call the ask filters */
        log.info(callingWorkflow + ": Calling getAskFilters");
        // HACK, because setting source and target first isnt working for some reason
        Ask itemZero = askMsg.getItems()[0];
        itemZero.setTargetCode(targetCode);
        Ask filteredAsk = getAskFilters(beUtils, itemZero);
        if (filteredAsk != null) {
          log.info(callingWorkflow + ": filteredAsk is not null. Using filteredAsk");
          Ask[] filteredAskArr = {filteredAsk};
          askMsg.setItems(filteredAskArr);
        }

        String jsonStr = updateSourceAndTarget(askMsg, sourceCode, targetCode, output, userToken);
        QDataAskMessage updated = JsonUtils.fromJson(jsonStr, QDataAskMessage.class);

        // Find all the target be's to send
        Set<String> beCodes = new HashSet<String>();
        // The user will already be there
        if ((targetCode != null)) {
          beCodes.add(targetCode);
        }
        if (!output.getAttributeTargetCodeMap().keySet().isEmpty()) {
          beCodes.addAll(output.getAttributeTargetCodeMap().values());
        }
        beCodes.remove(userToken.getUserCode()); // no need to send the user
        if (!beCodes.isEmpty()) {
          List<BaseEntity> besToSend = new ArrayList<BaseEntity>();
          for (String tCode : beCodes) {
            BaseEntity be = beUtils.getBaseEntityByCode(tCode);
            besToSend.add(be);
          }
          QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(besToSend);
          beMsg.setReplace(true);
          if (cache) {
            qBulkMessage.add(beMsg);
          } else {
            beMsg.setToken(userToken.getToken());
            VertxUtils.writeMsg("webdata", beMsg);
          }
        }

        // find any select Attributes, find their selection Baseentities and send
        GennyToken serviceToken = null;
        String serviceTokenStr =
            VertxUtils.getObject(
                userToken.getRealm(), "CACHE", "SERVICE_TOKEN", String.class, userToken.getToken());
        if (serviceTokenStr == null) {
          log.error(callingWorkflow + ": SERVICE TOKEN FETCHED FROM CACHE IS NULL");
          return qBulkMessage;
        } else {
          serviceToken = new GennyToken("PER_SERVICE", serviceTokenStr);
        }

        // String[] dropdownCodes = match(jsonStr, "/(\\\"LNK_\\S+\\\")/g");
        log.info("This is the 9.7.0 version of ShowFrame , and update is " + updated);

        // Iterate child asks
        if (updated != null
            && updated.getItems() != null
            && (updated.getItems().length > 0)
            && (updated.getItems()[0].getChildAsks() != null)) {
          for (Ask childAsk : updated.getItems()[0].getChildAsks()) {

            // Only Attempt if it is a dropdown
            if (childAsk.getAttributeCode().startsWith("LNK_")) {
              // Grab attribute and quesiotn code
              String dropdownCode = childAsk.getAttributeCode();
              String questionCode = childAsk.getQuestionCode();

              Boolean defDropdownExists = false;

              // Determine whether there is a DEF attribute and target type that has a new DEF
              // search for this combination
              try {
                if (defBe != null) {
                  defDropdownExists = beUtils.hasDropdown(dropdownCode, defBe);
                } else {
                  log.error("No DEF identified for target " + targetCode);
                }
              } catch (Exception e) {
                log.error(
                    "Error determining dropdown - "
                        + e.getLocalizedMessage()
                        + " defBecode = "
                        + defBe.getCode());
              }

              log.info(
                  callingWorkflow
                      + ": dropdownCode:"
                      + dropdownCode
                      + " and an enabled dropdown search was "
                      + (defDropdownExists ? "FOUND" : "NOT FOUND"));

              if (defDropdownExists) {
                log.info("Dropdown code that exists :: " + dropdownCode);

                // test
                if (defDropdownExists) {
                  // find the selected edu provider if exists
                  String val = target.getValue(dropdownCode, null);
                  log.info("Selected Existing val for " + dropdownCode + " = " + val);
                  Set<BaseEntity> beItems = new HashSet<>();
                  if (!StringUtils.isBlank(val)) {
                    JsonArray jaItems = new JsonArray(val);
                    for (Object jItem : jaItems) {
                      String beCode = (String) jItem;
                      BaseEntity selectionBe = beUtils.getBaseEntityByCode(beCode);
                      if (selectionBe != null) {
                        beItems.add(selectionBe);
                      }
                    }
                  }

                  String groupCode = askMsg.getItems()[0].getQuestionCode();

                  QBulkMessage qb =
                      sendDefSelectionItems(
                          beItems.toArray(new BaseEntity[0]),
                          defBe,
                          dropdownCode,
                          userToken,
                          serviceToken,
                          cache,
                          targetCode,
                          groupCode,
                          questionCode);
                  qBulkMessage.add(qb);

                  // Now send all the cached dropdown items IF RADIO
                  Attribute dropdownAttribute =
                      RulesUtils.getAttribute(dropdownCode, beUtils.getGennyToken());
                  if (("radio".equalsIgnoreCase(dropdownAttribute.getDataType().getComponent()))
                      || ("checkbox"
                          .equalsIgnoreCase(dropdownAttribute.getDataType().getComponent()))) {
                    QDataBaseEntityMessage listItems =
                        SearchUtils.getDropdownData(
                            beUtils,
                            source,
                            target,
                            dropdownCode,
                            groupCode,
                            questionCode,
                            "",
                            GennySettings.defaultDropDownPageSize);
                    qBulkMessage.add(listItems);
                    if (!cache) {
                      listItems.setToken(userToken.getToken());
                      VertxUtils.writeMsg("webcmds", JsonUtils.toJson(listItems));
                    }
                  }
                  // Check Dependencies, and disable if not met
                  Boolean dependenciesMet =
                      beUtils.dependenciesMet(dropdownCode, null, target, defBe);
                  if (dependenciesMet != null && !dependenciesMet) {
                    if (updated.getItems() != null
                        && updated.getItems().length > 0
                        && updated.getItems()[0] != null) {
                      Ask[] newAsk = {updated.getItems()[0]};
                      for (Ask childAskDep : newAsk[0].getChildAsks()) {
                        if (childAskDep.getAttributeCode().equals(dropdownCode)) {
                          childAskDep.setDisabled(true);
                          log.info("Setting " + dropdownCode + " to DISABLED!");
                        }
                      }
                      askMsg.setItems(newAsk);
                    }
                  }
                }

                continue;
              }

              log.info("OLD Dropdown code :: " + dropdownCode);
              // don't waste time sending stuff for a null target
              if (target != null) {
                QBulkMessage qb =
                    sendSelectionItems(dropdownCode, userToken, serviceToken, cache, targetCode);
                qBulkMessage.add(qb);
              }
            }
          }
        }

        qBulkMessage.add(updated);
        if (!cache) {
          log.info("Sending the Asks Now !!!");
          VertxUtils.writeMsg("webcmds", JsonUtils.toJson(updated)); // QDataAskMessage
        }
      }
    }
    return qBulkMessage;
  }

  /**
   * @param rootFrameCode
   * @param userToken
   * @param callingWorkflow
   */
  private static QBulkMessage sendAsks(
      String rootFrameCode,
      BaseEntityUtils beUtils,
      String callingWorkflow,
      OutputParam output,
      Boolean cache) {
    QBulkMessage qBulkMessage = new QBulkMessage();
    GennyToken userToken = beUtils.getGennyToken();
    GennyToken serviceToken = beUtils.getServiceToken();

    if (VertxUtils.cachedEnabled) {
      // No point sending asks
      log.info("CacheEnabled = true. Not sending Asks");
      return qBulkMessage;
    }

    TaskService taskService;
    Task task = null;
    Map<String, Object> taskAsks = null;
    Map<String, TaskAsk> attributeTaskAskMap = null;
    String sourceCode = null;
    String targetCode = null;
    Boolean enabledSubmit = false;
    BaseEntity defBe = null;

    BaseEntity target = null;
    BaseEntity source = null;

    if ((output != null)) {
      if (output.getAskTargetCode() == null) {
        log.warn("No SendAsks needed to be sent as no targetCode supplied");
        // return qBulkMessage;
      }
      log.info("Output Task ID = " + output.getTaskId());
      if ((output.getTaskId() != null) && (output.getTaskId() > 0L)) {
        taskService = RulesLoader.taskServiceMap.get(userToken.getJTI());
        if (taskService == null) {
          log.error("taskService is NULL for sessioncode " + userToken.getJTI());
          return qBulkMessage; // cannot do anything
        }
        task = taskService.getTaskById(output.getTaskId());
        // Now get the TaskAsk that relates to this specific Ask
        // assume that all attributes have the same source and target
        Long docId = task.getTaskData().getDocumentContentId();
        Content c = taskService.getContentById(docId);
        if (c == null) {
          log.error(
              callingWorkflow + ": *************** Task content is NULL *********** ABORTING");
          return qBulkMessage;
        }
        taskAsks =
            (HashMap<String, Object>) ContentMarshallerHelper.unmarshall(c.getContent(), null);
        if (taskAsks.isEmpty()) {
          log.error("ShowFrame: sendAsks. No TaskAsks in Task - returning");
          return qBulkMessage;
        }
        for (String key : taskAsks.keySet()) {
          Object obj = taskAsks.get(key);
          if (obj instanceof TaskAsk) { // This gets around my awful formcode values
            TaskAsk taskAsk = (TaskAsk) taskAsks.get(key);
            // String attributeStr = taskAsk.getAsk().getAttributeCode();
            // attributeTaskAskMap.put(attributeStr,taskAsk);
            sourceCode = taskAsk.getAsk().getSourceCode();
            targetCode = taskAsk.getAsk().getTargetCode();
          }
        }

        if (StringUtils.isBlank(targetCode)) {
          log.error("ShowFrame: sendAsks. No TargetCode in Task - returning");
          return qBulkMessage;
        }
        if (StringUtils.isBlank(sourceCode)) {
          log.error("ShowFrame: sendAsks. No SourceCode in Task - returning");
          return qBulkMessage;
        }

        Boolean waitForNonPerson = true;
        while (waitForNonPerson) {
          target = beUtils.getBaseEntityByCode(targetCode);
          source = beUtils.getBaseEntityByCode(sourceCode);

          if (target == null) {
            log.error(
                callingWorkflow
                    + " ShowFrame: sendAsks. No Target existing in Task  for "
                    + targetCode
                    + " - returning");
            return qBulkMessage;
          }

          log.info("Target BE = " + target);
          for (EntityAttribute ea : target.getBaseEntityAttributes()) {
            log.info(target.getCode() + ": " + ea.getAttributeCode() + ":\t\t" + ea.getValue());
          }
          defBe = beUtils.getDEF(target);
          if ("DEF_PERSON".equals(defBe.getCode())) {
            log.error("DEF identified as DEF_PERSON! - " + targetCode);
            // TODO HACK: another timing hack. The target does not have everything yet.
            try {
              Thread.sleep(2000);
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          } else {
            log.info("DEF identified as " + defBe.getCode() + " - " + targetCode);
            waitForNonPerson = false;
          }
        }
        enabledSubmit = TaskUtils.areAllMandatoryQuestionsAnswered(target, taskAsks);

      } else {
        sourceCode = output.getAskSourceCode();
        targetCode = output.getAskTargetCode();
        if (targetCode != null) {
          target = beUtils.getBaseEntityByCode(targetCode);
          source = beUtils.getBaseEntityByCode(sourceCode);
          if (target != null) {
            defBe = beUtils.getDEF(target);
            enabledSubmit = TaskUtils.areAllMandatoryQuestionsAnswered(target, taskAsks);
          } else {
            log.error(callingWorkflow + " ShowFrame: sendAsks -> Target is NULL");
          }
        } else {
          log.error(callingWorkflow + " ShowFrame: sendAsks -> TargetCode is NULL");
        }
      }
    }

    Set<QDataAskMessage> askMsgs2 = fetchAskMessages(rootFrameCode, userToken);

    // log.info("Sending Asks");
    if ((askMsgs2 != null) && (!askMsgs2.isEmpty())) {
      for (QDataAskMessage askMsg : askMsgs2) { // TODO, not needed
        for (Ask aask : askMsg.getItems()) {
          log.info(callingWorkflow + ": aask: " + aask);

          /* recursively check validations */
          checkAskValidation(aask, callingWorkflow);
          TaskUtils.enableAttribute("PRI_SUBMIT", aask, callingWorkflow, enabledSubmit);
          aask.setId(output.getTaskId());
        }
        askMsg.setToken(userToken.getToken());

        /* call the ask filters */
        log.info(callingWorkflow + ": Calling getAskFilters");
        // HACK, because setting source and target first isnt working for some reason
        Ask itemZero = askMsg.getItems()[0];
        itemZero.setTargetCode(targetCode);
        Ask filteredAsk = getAskFilters(beUtils, itemZero);
        if (filteredAsk != null) {
          log.info(callingWorkflow + ": filteredAsk is not null. Using filteredAsk");
          Ask[] filteredAskArr = {filteredAsk};
          askMsg.setItems(filteredAskArr);
        }

        String jsonStr = updateSourceAndTarget(askMsg, sourceCode, targetCode, output, userToken);
        QDataAskMessage updated = JsonUtils.fromJson(jsonStr, QDataAskMessage.class);

        // Find all the target be's to send
        Set<String> beCodes = new HashSet<String>();
        // The user will already be there
        if ((targetCode != null)) {
          beCodes.add(targetCode);
        }
        if (!output.getAttributeTargetCodeMap().keySet().isEmpty()) {
          beCodes.addAll(output.getAttributeTargetCodeMap().values());
        }
        beCodes.remove(userToken.getUserCode()); // no need to send the user
        if (!beCodes.isEmpty()) {
          List<BaseEntity> besToSend = new ArrayList<BaseEntity>();
          for (String tCode : beCodes) {
            BaseEntity be = beUtils.getBaseEntityByCode(tCode);
            besToSend.add(be);
          }
          QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(besToSend);
          beMsg.setReplace(true);
          if (cache) {
            qBulkMessage.add(beMsg);
          } else {
            beMsg.setToken(userToken.getToken());
            VertxUtils.writeMsg("webdata", beMsg);
          }
        }

        // find any select Attributes, find their selection Baseentities and send
        //				GennyToken serviceToken = null;
        //				String serviceTokenStr = VertxUtils.getObject(userToken.getRealm(), "CACHE",
        // "SERVICE_TOKEN",
        //						String.class, userToken.getToken());
        //				if (serviceTokenStr == null) {
        //					log.error(callingWorkflow + ": SERVICE TOKEN FETCHED FROM CACHE IS NULL");
        //					return qBulkMessage;
        //				} else {
        //					serviceToken = new GennyToken("PER_SERVICE", serviceTokenStr);
        //				}

        // Iterate child asks
        if (updated != null
            && updated.getItems() != null
            && (updated.getItems().length > 0)
            && (updated.getItems()[0].getChildAsks() != null)) {
          for (Ask childAsk : updated.getItems()[0].getChildAsks()) {

            // Only Attempt if it is a dropdown
            if (childAsk.getAttributeCode().startsWith("LNK_")) {
              // Grab attribute and quesiotn code
              String dropdownCode = childAsk.getAttributeCode();
              String questionCode = childAsk.getQuestionCode();

              Boolean defDropdownExists = false;

              // Determine whether there is a DEF attribute and target type that has a new DEF
              // search for this combination
              try {
                if (defBe != null) {
                  defDropdownExists = beUtils.hasDropdown(dropdownCode, defBe);
                } else {
                  log.error("No DEF identified for target " + targetCode);
                }
              } catch (Exception e) {
                log.error(
                    "Error determining dropdown - "
                        + e.getLocalizedMessage()
                        + " defBecode = "
                        + defBe.getCode());
              }

              log.info(
                  callingWorkflow
                      + ": dropdownCode:"
                      + dropdownCode
                      + " and an enabled dropdown search was "
                      + (defDropdownExists ? "FOUND" : "NOT FOUND"));

              if (defDropdownExists) {
                log.info("Dropdown code :: " + dropdownCode);

                // test
                if (defDropdownExists) {

                  String val = target.getValue(dropdownCode, null);
                  log.info("val = " + val);
                  Set<BaseEntity> beItems = new HashSet<>();
                  if (!StringUtils.isBlank(val)) {
                    JsonArray jaItems = new JsonArray(val);
                    for (Object jItem : jaItems) {
                      String beCode = (String) jItem;
                      BaseEntity selectionBe = beUtils.getBaseEntityByCode(beCode);
                      if (selectionBe != null) {
                        beItems.add(selectionBe);
                      }
                    }
                  }

                  String groupCode = askMsg.getItems()[0].getQuestionCode();

                  // Only check for DDC if not items are selected already
                  Attribute dropdownAttribute =
                      RulesUtils.getAttribute(dropdownCode, beUtils.getGennyToken());

                  if ((beItems.size() == 0)
                      || (("radio".equalsIgnoreCase(dropdownAttribute.getDataType().getComponent()))
                          || ("checkbox"
                              .equalsIgnoreCase(dropdownAttribute.getDataType().getComponent())))) {
                    Optional<EntityAttribute> cacheAtt =
                        defBe.findEntityAttribute("DDC_" + dropdownCode);
                    if (cacheAtt.isPresent()) {
                      String jsonMsg = cacheAtt.get().getValueString();
                      // Replacement for @@TOKEN@@, @@PARENTCODE@@, @@QUESTIONCODE@@
                      jsonMsg = jsonMsg.replaceAll("@@TOKEN@@", userToken.getToken());
                      jsonMsg = jsonMsg.replaceAll("@@PARENTCODE@@", groupCode);
                      jsonMsg = jsonMsg.replaceAll("@@QUESTIONCODE@@", questionCode);

                      QDataBaseEntityMessage msg =
                          JsonUtils.fromJson(jsonMsg, QDataBaseEntityMessage.class);
                      msg.setAttributeCode(dropdownCode);
                      qBulkMessage.add(msg);
                      if (!cache) {
                        msg.setToken(userToken.getToken());
                        VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg));
                      }
                    } else if ((("radio"
                            .equalsIgnoreCase(dropdownAttribute.getDataType().getComponent()))
                        || ("checkbox"
                            .equalsIgnoreCase(dropdownAttribute.getDataType().getComponent())))) {
                      String searchLookup = defBe.getValue("SER_" + dropdownCode, null);
                      if (searchLookup != null) {
                        // fetch from database
                        QDataBaseEntityMessage msg =
                            DefUtils.getDropdownDataMessage(
                                beUtils,
                                dropdownCode,
                                groupCode,
                                questionCode,
                                searchLookup,
                                null,
                                null,
                                "",
                                userToken.getToken());
                        msg.setAttributeCode(dropdownCode);
                        qBulkMessage.add(msg);
                        if (!cache) {
                          msg.setToken(userToken.getToken());
                          VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg));
                        }
                      }
                    }
                  } else {
                    QBulkMessage qb =
                        sendDefSelectionItems(
                            beItems.toArray(new BaseEntity[0]),
                            defBe,
                            dropdownCode,
                            userToken,
                            serviceToken,
                            cache,
                            targetCode,
                            groupCode,
                            questionCode);
                    qBulkMessage.add(qb);
                    // Now send all the cached dropdown items IF RADIO
                    if (("radio".equalsIgnoreCase(dropdownAttribute.getDataType().getComponent()))
                        || ("checkbox"
                            .equalsIgnoreCase(dropdownAttribute.getDataType().getComponent()))) {
                      QDataBaseEntityMessage listItems =
                          SearchUtils.getDropdownData(
                              beUtils,
                              source,
                              target,
                              dropdownCode,
                              groupCode,
                              questionCode,
                              "",
                              GennySettings.defaultDropDownPageSize);
                      qBulkMessage.add(listItems);
                      if (!cache) {
                        listItems.setToken(userToken.getToken());
                        VertxUtils.writeMsg("webcmds", JsonUtils.toJson(listItems));
                      }
                    }
                  }

                  // Check Dependencies, and disable if not met
                  Boolean dependenciesMet =
                      beUtils.dependenciesMet(dropdownCode, null, target, defBe);
                  if (dependenciesMet != null && !dependenciesMet) {
                    if (updated.getItems() != null
                        && updated.getItems().length > 0
                        && updated.getItems()[0] != null) {
                      Ask[] newAsk = {updated.getItems()[0]};
                      for (Ask childAskDep : newAsk[0].getChildAsks()) {
                        if (childAskDep.getAttributeCode().equals(dropdownCode)) {
                          childAskDep.setDisabled(true);
                          log.info("Setting " + dropdownCode + " to DISABLED!");
                        }
                      }
                      askMsg.setItems(newAsk);
                    }
                  }
                }

                continue;
              }

              log.info("OLD Dropdown code :: " + dropdownCode);
              // don't waste time sending stuff for a null target
              if (target != null) {
                QBulkMessage qb =
                    sendSelectionItems(dropdownCode, userToken, serviceToken, cache, targetCode);
                qBulkMessage.add(qb);
              }
            }
          }
        }

        qBulkMessage.add(updated);
        if (!cache) {
          log.info("Sending the Asks Now !!!");
          updated.setToken(userToken.getToken());
          VertxUtils.writeMsg("webcmds", JsonUtils.toJson(updated)); // QDataAskMessage
        }
      }
    }
    return qBulkMessage;
  }

  private static void updateTargetInAsk(
      Ask ask, String sourceCode, String targetCode, OutputParam output, GennyToken userToken) {
    String attributeCode = ask.getAttributeCode();
    String askTargetCode = output.getAttributeTargetCodeMap().get(attributeCode);
    if (askTargetCode != null) {
      ask.setTargetCode(askTargetCode);
    } else if (targetCode != null) {
      ask.setTargetCode(targetCode);
    } else {
      ask.setTargetCode(userToken.getUserCode());
    }

    if ((ask.getChildAsks() != null) && (ask.getChildAsks().length > 0)) {
      for (Ask childAsk : ask.getChildAsks()) {
        updateTargetInAsk(childAsk, sourceCode, targetCode, output, userToken);
      }
    }
  }

  private static String updateSourceAndTarget(
      QDataAskMessage askMsg,
      String sourceCode,
      String targetCode,
      OutputParam output,
      GennyToken userToken) {

    if (!output.getAttributeTargetCodeMap().keySet().isEmpty()) {
      for (Ask ask : askMsg.getItems()) {
        updateTargetInAsk(ask, sourceCode, targetCode, output, userToken);
      }
    }

    String json = JsonUtils.toJson(askMsg);

    String jsonStr = json.replaceAll("PER_SERVICE", userToken.getUserCode()); // set the

    if (sourceCode != null) { // user
      jsonStr = jsonStr.replaceAll("PER_SOURCE", sourceCode);
    } else {
      jsonStr = jsonStr.replaceAll("PER_SOURCE", userToken.getUserCode());
      sourceCode = userToken.getUserCode();
    }
    if (targetCode != null) { // user
      jsonStr = jsonStr.replaceAll("PER_TARGET", targetCode);
    } else {
      jsonStr = jsonStr.replaceAll("PER_TARGET", userToken.getUserCode());
      targetCode = userToken.getUserCode();
    }
    // log.info("ShowFrame: Setting outgoing Asks to have " + sourceCode + ":" +
    // targetCode);
    return jsonStr;
  }

  private static QDataAskMessage updateSourceAndTargetMsg(
      QDataAskMessage askMsg,
      String sourceCode,
      String targetCode,
      OutputParam output,
      GennyToken userToken) {

    QDataAskMessage ret = askMsg;

    if (!output.getAttributeTargetCodeMap().keySet().isEmpty()) {
      for (Ask ask : askMsg.getItems()) {
        updateTargetInAsk(ask, sourceCode, targetCode, output, userToken);
      }
    }

    String json = JsonUtils.toJson(askMsg);

    String jsonStr = json.replaceAll("PER_SERVICE", userToken.getUserCode()); // set the

    if (sourceCode != null) { // user
      jsonStr = jsonStr.replaceAll("PER_SOURCE", sourceCode);
    } else {
      jsonStr = jsonStr.replaceAll("PER_SOURCE", userToken.getUserCode());
      sourceCode = userToken.getUserCode();
    }
    if (targetCode != null) { // user
      jsonStr = jsonStr.replaceAll("PER_TARGET", targetCode);
    } else {
      jsonStr = jsonStr.replaceAll("PER_TARGET", userToken.getUserCode());
      targetCode = userToken.getUserCode();
    }
    // log.info("ShowFrame: Setting outgoing Asks to have " + sourceCode + ":" +
    // targetCode);
    ret = JsonUtils.fromJson(jsonStr, QDataAskMessage.class);
    return ret;
  }

  /**
   * @param rootFrameCode
   * @param userToken
   * @return
   */
  public static Set<QDataAskMessage> fetchAskMessages(String rootFrameCode, GennyToken userToken) {
    Type setType = new TypeToken<Set<QDataAskMessage>>() {}.getType();

    String askMsgs2Str = null;
    Set<QDataAskMessage> askMsgs2 = Collections.emptySet();
    if (GennySettings.forceCacheApi) { // if in junit then use the bridge to fetch
      // cache data
      log.info("Forcing ASKS to be read from api call to cache");
      //						askMsgs2Str = VertxUtils.getObject(userToken.getRealm(), "", rootFrameCode + "_ASKS",
      //						String.class, userToken.getToken());
      //			try {
      //				askMsgs2Str = QwandaUtils.apiGet(
      //						GennySettings.ddtUrl + "/read/" + userToken.getRealm() + "/" + rootFrameCode +
      // "_ASKS",
      //						userToken.getToken());
      //				JsonObject json = new JsonObject(askMsgs2Str);
      //				askMsgs2Str = json.getString("value"); // TODO - assumes always works.....not always
      // case
      //			} catch (ClientProtocolException e) {
      //				// TODO Auto-generated catch block
      //				e.printStackTrace();
      //			} catch (IOException e) {
      //				// TODO Auto-generated catch block
      //				e.printStackTrace();
      //			}

      askMsgs2Str =
          (String)
              VertxUtils.cacheInterface.readCache(
                  userToken.getRealm(), rootFrameCode + "_ASKS", userToken.getToken());

      if (askMsgs2Str == null) {
        log.error(
            "No Asks: " + rootFrameCode + "_ASKS in cache, search cache for " + rootFrameCode);
        String frameStr =
            (String)
                VertxUtils.cacheInterface.readCache(
                    userToken.getRealm(), rootFrameCode, userToken.getToken());
        if (StringUtils.isBlank(frameStr)) {
          String questionCode = rootFrameCode.substring("FRM_".length());
          String realm = userToken.getRealm();
          Frame3 frame2 =
              Frame3.builder("FRM_CONTENT_" + questionCode).question(questionCode).end().build();

          frame2.setRealm(realm);

          Frame3 frame =
              Frame3.builder("FRM_" + questionCode)
                  .addFrame(frame2, FramePosition.CENTRE)
                  .end()
                  .build();

          frame.setRealm(realm);
          log.info(frame.getCode());
          FrameUtils2.toMessage(frame, userToken);
          frameStr =
              (String)
                  VertxUtils.cacheInterface.readCache(
                      userToken.getRealm(), rootFrameCode, userToken.getToken());
        }
        Frame3 rootFrame = JsonUtils.fromJson(frameStr, Frame3.class);
        if (rootFrame != null) {
          if (rootFrame.getCode().startsWith("FRM_QUE_")) {

            FrameUtils2.toMessage2(rootFrame, userToken, "PER_SOURCE", "PER_TARGET");
          } else {
            Map<String, ContextList> contextListMap = new HashMap<String, ContextList>();
            FrameUtils2.toMessage(
                rootFrame, userToken, contextListMap, "PER_SERVICE", "PER_SERVICE", true);

            // FrameUtils2.toMessage(rootFrame, userToken,"PER_SERVICE","PER_SERVICE",true);
          }
          askMsgs2Str =
              (String)
                  VertxUtils.cacheInterface.readCache(
                      userToken.getRealm(), rootFrameCode + "_ASKS", userToken.getToken());
          if (askMsgs2Str == null) {
            log.error("Frame ASKS for " + rootFrameCode + " is just not happening...");
            return new HashSet<QDataAskMessage>();
          }
        } else {
          log.error(rootFrameCode + " is not in cache");
          return askMsgs2;
        }
      }
    } else {

      //			askMsgs2Str = (String) VertxUtils.cacheInterface.readCache(userToken.getRealm(),
      // rootFrameCode + "_ASKS",
      //					userToken.getToken());

      JsonObject askMsgJson =
          VertxUtils.readCachedJson(
              userToken.getRealm(), rootFrameCode + "_ASKS", userToken.getToken());
      if ("OK".equalsIgnoreCase(askMsgJson.getString("status"))) {
        askMsgs2Str = askMsgJson.getString("value");
        if (askMsgs2Str.contains("\"items\": [],")) {
          askMsgs2Str = null;
        }
      } else {
        askMsgs2Str = null;
      }

      // askMsgs2Str = null; //TODO FORCE HACK
      if (askMsgs2Str == null) {
        log.error(
            "No Asks: " + rootFrameCode + "_ASKS in cache, search cache for " + rootFrameCode);
        String frameStr =
            (String)
                VertxUtils.cacheInterface.readCache(
                    userToken.getRealm(), rootFrameCode, userToken.getToken());
        if (StringUtils.isBlank(frameStr)) {
          String questionCode = rootFrameCode.substring("FRM_".length());
          String realm = userToken.getRealm();
          Frame3 frame2 =
              Frame3.builder("FRM_CONTENT_" + questionCode).question(questionCode).end().build();

          frame2.setRealm(realm);

          Frame3 frame =
              Frame3.builder("FRM_" + questionCode)
                  .addFrame(frame2, FramePosition.CENTRE)
                  .end()
                  .build();

          frame.setRealm(realm);
          log.info(frame.getCode());
          FrameUtils2.toMessage(frame, userToken);
          frameStr =
              (String)
                  VertxUtils.cacheInterface.readCache(
                      userToken.getRealm(), rootFrameCode, userToken.getToken());
        }
        Frame3 rootFrame = JsonUtils.fromJson(frameStr, Frame3.class);
        if (rootFrame != null) {
          if (rootFrame.getCode().startsWith("FRM_QUE_")) {

            FrameUtils2.toMessage2(rootFrame, userToken, "PER_SOURCE", "PER_TARGET");
          } else {
            Map<String, ContextList> contextListMap = new HashMap<String, ContextList>();
            FrameUtils2.toMessage(
                rootFrame, userToken, contextListMap, "PER_SERVICE", "PER_SERVICE", true);

            // FrameUtils2.toMessage(rootFrame, userToken,"PER_SERVICE","PER_SERVICE",true);
          }
          askMsgs2Str =
              (String)
                  VertxUtils.cacheInterface.readCache(
                      userToken.getRealm(), rootFrameCode + "_ASKS", userToken.getToken());
          if (askMsgs2Str == null) {
            log.error("Frame ASKS for " + rootFrameCode + " is just not happening...");
            return new HashSet<QDataAskMessage>();
          }
        } else {
          log.error(rootFrameCode + " is not in cache");
          return askMsgs2;
        }
      }
    }

    log.debug("About to do deserialization!");
    // try first time, don't print log if failed
    askMsgs2 = JsonUtils.fromJson(askMsgs2Str, setType, true);

    if (askMsgs2 == null) {
      askMsgs2Str = askMsgs2Str.replaceAll(Pattern.quote("\\n"), Matcher.quoteReplacement("\n"));
      askMsgs2Str = askMsgs2Str.replaceAll(Pattern.quote("\\\""), Matcher.quoteReplacement("\""));
      askMsgs2Str = askMsgs2Str.replaceAll(Pattern.quote("\"["), Matcher.quoteReplacement("["));
      askMsgs2Str = askMsgs2Str.replaceAll(Pattern.quote("]\""), Matcher.quoteReplacement("]"));
      askMsgs2Str = askMsgs2Str.replaceAll(Pattern.quote("\\n"), "");
      // try again , print error log
      askMsgs2 = JsonUtils.fromJson(askMsgs2Str, setType);
    }

    if (askMsgs2 == null) {
      // NOTE: Can't convert JSON string to target type, may casued by incorrect
      // replacement
      // Don't try to print askMsgs2 as it has 20K lines
      BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
      BaseEntity rule = beUtils.getBaseEntityByCode("RUL_" + rootFrameCode);
      if (rule.getValue("PRI_ASKS").isPresent()) {
        askMsgs2Str = (String) rule.getValue("PRI_ASKS").get(); // assume always
      } else {
        askMsgs2Str = null;
      }
      if (askMsgs2Str == null) {
        String fr = (String) rule.getValue("PRI_FRM").get(); // assume always
        Frame3 frame3 = JsonUtils.fromJson(fr, Frame3.class);
        FrameUtils2.toMessage2(frame3, userToken);
        askMsgs2Str =
            (String)
                VertxUtils.cacheInterface.readCache(
                    userToken.getRealm(), rootFrameCode + "_ASKS", userToken.getToken());
      }

      VertxUtils.cacheInterface.writeCache(
          userToken.getRealm(), rootFrameCode + "_ASKS", askMsgs2Str, userToken.getToken(), 0);
      log.info("About to do deserialization2!");
      askMsgs2 = JsonUtils.fromJson(askMsgs2Str, setType);
    }
    return askMsgs2;
  }

  public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    // Do nothing, notifications cannot be aborted
  }

  public static Ask getAskFilters(BaseEntityUtils beUtils, final Ask ask) {

    Ask ret = null;

    Map<String, Object> facts = new ConcurrentHashMap<String, Object>();
    facts.put("serviceToken", beUtils.getServiceToken());
    facts.put("userToken", beUtils.getGennyToken());
    facts.put("ask", ask);

    /* log.info("facts   ::  " +facts); */
    RuleFlowGroupWorkItemHandler ruleFlowGroupHandler = new RuleFlowGroupWorkItemHandler();

    log.info("serviceToken " + beUtils.getServiceToken());
    Map<String, Object> results =
        ruleFlowGroupHandler.executeRules(
            beUtils.getServiceToken(),
            beUtils.getGennyToken(),
            facts,
            "AskFilters",
            "ShowFrame:GetAskFilters");

    Object obj = results.get("payload");
    if (obj instanceof QBulkMessage) {
      QBulkMessage bulkMsg = (QBulkMessage) results.get("payload");

      // Check if bulkMsg not empty
      if (bulkMsg.getAsks().length > 0) {
        log.info("bulkMsg is not empty");

        // Get the first QDataBaseEntityMessage from bulkMsg
        QDataAskMessage msg = bulkMsg.getAsks()[0];

        // Check if msg is not empty
        if (msg.getItems().length > 0) {
          log.info("QDataAskMessage's msg is not empty");
          ret = msg.getItems()[0];
        }
      }
    }
    return ret;
  }

  public static QBulkMessage sendSelectionItems(
      String attributeCode,
      GennyToken userToken,
      GennyToken serviceToken,
      Boolean cache,
      String dropdownTarget) {
    QBulkMessage qBulkMessage = new QBulkMessage();
    Attribute attribute = RulesUtils.getAttribute(attributeCode, userToken);
    DropdownUtils dropDownUtils = new DropdownUtils(serviceToken);

    try {
      if (attribute != null) {

        DataType dt = attribute.getDataType();
        // log.info("DATATYPE IS " + dt);

        List<Validation> vl = dt.getValidationList();

        if ((vl != null) && (!vl.isEmpty())) {

          Validation val = vl.get(0);

          if ((val.getSelectionBaseEntityGroupList() != null)
              && (!val.getSelectionBaseEntityGroupList().isEmpty())) {

            String groupCode = val.getSelectionBaseEntityGroupList().get(0);

            JsonObject searchBe =
                VertxUtils.readCachedJson(
                    userToken.getRealm(), "SBE_" + groupCode, userToken.getToken());

            if ("ok".equalsIgnoreCase(searchBe.getString("status"))) {

              /* This is for dynamically generated items */
              SearchEntity sbe =
                  JsonUtils.fromJson(searchBe.getString("value"), SearchEntity.class);
              sbe.addColumn("PRI_NAME", "Name");
              sbe.setPageSize(1500); // for old dropdown
              if (dropdownTarget != null) {
                sbe.setDropdownTarget(dropdownTarget);
              }
              dropDownUtils.setSearch(sbe);
              QDataBaseEntityMessage resultsMsg =
                  dropDownUtils.sendSearchResultsUsingAltSearch(
                      groupCode, "LNK_CORE", "ITEMS", userToken, true, false);
              if (cache) {
                qBulkMessage.add(resultsMsg);
              }

            } else {

              // Check if already in cache
              QDataBaseEntityMessage qdb = null;

              JsonObject json =
                  VertxUtils.readCachedJson(
                      userToken.getRealm(), "QDB_" + groupCode, userToken.getToken());

              if ("null".equals(json.getString("value"))) {

                log.error(
                    val.getCode()
                        + " groupCode has Illegal Group Code : ["
                        + groupCode
                        + "] dataType=["
                        + dt
                        + "] for attributeCode:["
                        + attributeCode
                        + "]");

              } else if ("ok".equalsIgnoreCase(json.getString("status"))) {

                qdb = JsonUtils.fromJson(json.getString("value"), QDataBaseEntityMessage.class);

                if (cache) {
                  qBulkMessage.add(qdb);
                } else {
                  qdb.setToken(userToken.getToken());
                  VertxUtils.writeMsg("webcmds", JsonUtils.toJson(qdb));
                }

              } else {

                dropDownUtils
                    .setNewSearch("Dropdown", "Fetch Dropdown Items")
                    .setSourceCode(groupCode)
                    .setPageStart(0)
                    .setPageSize(10000);

                qdb =
                    dropDownUtils.sendSearchResults(
                        groupCode, "LNK_CORE", "ITEMS", userToken, true, cache);
                if (cache) {
                  qBulkMessage.add(qdb);
                }
                VertxUtils.writeCachedJson(
                    userToken.getRealm(),
                    "QDB_" + groupCode,
                    JsonUtils.toJson(qdb),
                    userToken.getToken());
              }
            }
          }
        }
      }

    } catch (Exception e) {

      e.printStackTrace();
    }
    return qBulkMessage;
  }

  public static QBulkMessage sendDefSelectionItems(
      BaseEntity[] arrayItems,
      BaseEntity defBe,
      String attributeCode,
      GennyToken userToken,
      GennyToken serviceToken,
      Boolean cache,
      String dropdownTarget,
      String groupCode,
      String questionCode) {
    QBulkMessage qBulkMessage = new QBulkMessage();
    log.info("Sending dropdown items for " + groupCode + " and question " + questionCode);
    try {

      Optional<EntityAttribute> searchAtt = defBe.findEntityAttribute("SER_" + attributeCode);
      if (searchAtt.isPresent()) {

        QDataBaseEntityMessage msg =
            new QDataBaseEntityMessage(
                arrayItems, groupCode, "LINK", Long.decode(arrayItems.length + ""));
        // TODO this needs to include the attribbute code
        msg.setParentCode(groupCode);
        msg.setQuestionCode(questionCode);
        msg.setToken(userToken.getToken());
        msg.setLinkCode("LNK_CORE");
        msg.setLinkValue("ITEMS");
        msg.setReplace(true);
        msg.setData_type("BaseEntity");
        msg.setDelete(false);
        msg.setShouldDeleteLinkedBaseEntities(false);
        msg.setTotal(Long.valueOf(arrayItems.length));
        msg.setOption(MsgOption.EXEC);
        msg.setMsg_type("DATA_MSG");
        // TODO handle tags
        msg.setReturnCount(Long.valueOf(arrayItems.length));

        qBulkMessage.add(msg);
        if (!cache) {
          VertxUtils.writeMsg("webcmds", JsonUtils.toJson(msg));
        }
      }

    } catch (Exception e) {

      e.printStackTrace();
    }
    return qBulkMessage;
  }
}
