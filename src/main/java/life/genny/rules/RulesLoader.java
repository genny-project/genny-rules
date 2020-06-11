package life.genny.rules;

import com.google.common.io.Files;
import com.google.gson.reflect.TypeToken;

import es.usc.citius.hipster.graph.GraphBuilder;
import es.usc.citius.hipster.graph.HipsterDirectedGraph;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import life.genny.jbpm.customworkitemhandlers.*;
import life.genny.model.NodeStatus;
import life.genny.model.SessionPid;
import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.datatype.Allowed;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.entity.User;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.*;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.rules.listeners.GennyAgendaEventListener;
import life.genny.rules.listeners.GennyRuleTimingListener;
import life.genny.rules.listeners.JbpmInitListener;
import life.genny.rules.listeners.NodeStatusLog;

import life.genny.utils.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.Logger;
import org.drools.compiler.compiler.DrlParser;
import org.drools.compiler.compiler.DroolsParserException;
import org.drools.compiler.lang.descr.AttributeDescr;
import org.drools.compiler.lang.descr.RuleDescr;
import org.drools.core.impl.EnvironmentFactory;
import org.jbpm.executor.ExecutorServiceFactory;
import org.jbpm.executor.impl.ExecutorImpl;
import org.jbpm.executor.impl.ExecutorServiceImpl;
import org.jbpm.kie.services.impl.query.SqlQueryDefinition;
import org.jbpm.process.audit.AbstractAuditLogger;
import org.jbpm.runtime.manager.impl.DefaultRegisterableItemsFactory;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.query.QueryAlreadyRegisteredException;
import org.jbpm.services.api.query.QueryService;
import org.jbpm.services.api.query.model.QueryParam;
import org.jbpm.services.api.utils.KieServiceConfigurator;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.executor.ExecutorService;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.*;
import org.kie.api.runtime.conf.TimedRuleExecutionOption;
import org.kie.api.runtime.manager.*;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskData;
import org.kie.internal.identity.IdentityProvider;
import org.kie.internal.query.QueryContext;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.task.api.UserGroupCallback;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RulesLoader {
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	static String RESOURCE_PATH = "src/main/resources/life/genny/rules/";

	public static Map<String, KieBase> kieBaseCache = new ConcurrentHashMap<String, KieBase>();;
	static {
		setKieBaseCache(new ConcurrentHashMap<String, KieBase>());
	}

	public static KieServices ks = KieServices.Factory.get();

	public static Set<String> realms = new ConcurrentHashSet<String>();
	public static Set<String> userRoles = null;
	public static Map<String, User> usersSession = new ConcurrentHashMap<String, User>();
	public static boolean RUNTIME_MANAGER_ON = true;
	public static RuntimeEnvironment runtimeEnvironment;
	public static RuntimeEnvironmentBuilder runtimeEnvironmentBuilder;
	public static RuntimeManager runtimeManager; // THIS IS THREADSAFE - KEEP ALIVE ALWAYS
	public static Environment env; // drools persistence
	public static EntityManagerFactory emf = null;

	private static ExecutorService executorService;
	// private static TaskService taskService;
	protected static ProcessService processService;
	protected UserTaskService userTaskService;

	public static final Map<String, KieSession> kieSessionMap = new ConcurrentHashMap<>();
	public static final Map<String, TaskService> taskServiceMap = new ConcurrentHashMap<>();

	private static RuntimeDataService rds;

	public static Set<String> frameCodes = new TreeSet<String>();
	public static Set<String> themeCodes = new TreeSet<String>();

	static KieSessionConfiguration ksconf = null;

	public static List<String> activeRealms = new ArrayList<String>();

	public static Boolean rulesChanged = !GennySettings.detectRuleChanges; // If detectRule Changes is false then ALWAYS
																			// assume rules changed

	public static Boolean persistRules = GennySettings.persistRules;

	public static Boolean gNotReady = false;

	// public static Boolean rulesChanged = true;
	private final String debugStr = "DEBUG,";

	public static void shutdown() {
		runtimeManager.close();
	}

	public static void addRules(final String rulesDir, List<Tuple3<String, String, String>> newrules) {
		List<Tuple3<String, String, String>> rules = processFileRealmsFromApi(realms);
		rules.addAll(newrules);
		// realms = getRealms(rules);
		realms.stream().forEach(System.out::println);
		realms.remove("genny");
		setupKieRules("genny", rules); // run genny rules first
		for (String realm : realms) {
			setupKieRules(realm, rules);
		}
	}

	/**
	 * @param rulesDir
	 */
	public static Boolean loadRules(final String rulesDir) {

		log.info("Loading Rules and workflows!!!");

		// Create a simple weighted directed graph with Hipster where
		// vertices are Strings and edge values are just Strings

		GraphBuilder<String, String> gb = GraphBuilder.<String, String>create();

		List<String> activeRealms = new ArrayList<String>();
		JsonObject ar = VertxUtils.readCachedJson(GennySettings.GENNY_REALM, "REALMS");
		String ars = ar.getString("value");

		if (ars == null) {
			try {
				ars = QwandaUtils.apiGet(GennySettings.qwandaServiceUrl + "/utils/realms", "NOTREQUIRED");
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Type listType = new TypeToken<List<String>>() {
		}.getType();
		ars = ars.replaceAll("\\\"", "\"");
		activeRealms = JsonUtils.fromJson(ars, listType);
		if (activeRealms == null) {
			realms = new HashSet<>();
			log.error("NO ACTIVE REALMS");
		} else {
			realms = new HashSet<>(activeRealms);
		}

		List<Tuple3<String, String, String>> rules = null;

		if (GennySettings.useApiRules) {
			rules = processFileRealmsFromApi(realms);
		} else {
			rules = processFileRealmsFromFiles("genny", rulesDir, realms);
		}
		log.info("LOADED ALL RULES " + rules.size());
		realms.stream().forEach(System.out::println);
		realms.remove("genny");

		List<String> uninitialisedThemes = new ArrayList<String>();
		List<String> uninitialisedFrames = new ArrayList<String>();

		for (String realm : activeRealms) {
			log.info("LOADING " + realm + " RULES");
			Integer rulesCount = setupKieRules(realm, rules);
			log.info("Rules Count for " + realm + " = " + rulesCount);
			// check if rules need to be initialised
			// Check if rules have been initialised
			List<String> realmUninitialisedThemes = returnUninitialisedThemes(realm);
			List<String> realmUninitialisedFrames = returnUninitialisedFrames(realm);

			if (realmUninitialisedThemes == null) {
				rulesChanged = true;
			} else if (!realmUninitialisedThemes.isEmpty()) {
				rulesChanged = true;
				realmUninitialisedThemes.addAll(realmUninitialisedThemes);
			}
			if (realmUninitialisedFrames == null) {
				rulesChanged = true;
			} else if (!realmUninitialisedFrames.isEmpty()) {
				rulesChanged = true;
				realmUninitialisedFrames.addAll(realmUninitialisedFrames);
			}

		}

		// set up kie conf
		ksconf = KieServices.Factory.get().newKieSessionConfiguration();
		ksconf.setOption(TimedRuleExecutionOption.YES);

		return rulesChanged;
	}

	/**
	 * @param rulesDir
	 * @return
	 */
	public void triggerStartupRules(final String rulesDir) {
		log.info("Triggering Startup Rules for all realms");
		for (String realm : realms) {
			triggerStartupRules(realm, rulesDir);
		}
		log.info("Startup Rules Triggered");
		try {

			Files.touch(new File("/tmp/ready"));
		} catch (IOException e) {
			log.info("Could not save readiness file");
		}

	}

	static public List<Tuple3<String, String, String>> processFileRealmsFromApi(Set<String> activeRealms) {
		List<Tuple3<String, String, String>> rules = new ArrayList<Tuple3<String, String, String>>();

		for (String realm : activeRealms) {

			JsonObject tokenObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM, "TOKEN" + realm.toUpperCase());
			String sToken = tokenObj.getString("value");
			GennyToken serviceToken = new GennyToken("PER_SERVICE", sToken);

			if ((serviceToken == null) || ("DUMMY".equalsIgnoreCase(serviceToken.getToken()))) {
				log.error("NO SERVICE TOKEN FOR " + realm + " IN CACHE");
				return null;
			}

			// Fetch all the rules from the api
			SearchEntity searchBE = new SearchEntity("SBE_RULES", "Rules")
					.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "RUL_%")
					/* .addFilter("PRI_BRANCH", SearchEntity.StringFilter.LIKE,branch) */
					.addColumn("PRI_KIE_TEXT", "Text").addColumn("PRI_FILENAME", "Filename").setPageStart(0)
					.setPageSize(4000);

			searchBE.setRealm(serviceToken.getRealm());

			String jsonSearchBE = JsonUtils.toJson(searchBE);
			/* System.out.println(jsonSearchBE); */
			String resultJson;

			try {
				resultJson = QwandaUtils.apiPostEntity(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/search",
						jsonSearchBE, serviceToken.getToken());
				QDataBaseEntityMessage resultMsg = JsonUtils.fromJson(resultJson, QDataBaseEntityMessage.class);
				for (BaseEntity ruleBe : resultMsg.getItems()) {
					String filename = ruleBe.getValueAsString("PRI_FILENAME");
					String ruleText = ruleBe.getValueAsString("PRI_KIE_TEXT");
					Tuple3<String, String, String> rule = (Tuple.of(realm, filename, ruleText));
					rules.add(rule);
					log.info("Imported rule from API : " + filename);
				}
			} catch (Exception e) {
				log.error("Could not fetch Rules from API");

			}
		}

		return rules;
	}

	static public List<Tuple3<String, String, String>> processFileRealmsFromFiles(final String realm,
			String inputFileStrs, Set<String> activeRealms) {
		List<Tuple3<String, String, String>> rules = new ArrayList<Tuple3<String, String, String>>();

		String[] inputFileStrArray = inputFileStrs.split(";,:"); // allow multiple rules dirs

		for (String inputFileStr : inputFileStrArray) {

			// log.info("InputFileStr=" + inputFileStr);
			File file = new File(inputFileStr);
			String fileName = inputFileStr.replaceFirst(".*/(\\w+).*", "$1");

			String fileNameExt = inputFileStr.replaceFirst(".*/\\w+\\.(.*)", "$1");
			if (!file.isFile()) { // DIRECTORY
				if (!fileName.startsWith("XX")) {
					String localRealm = realm;
					if (fileName.startsWith("prj_") || fileName.startsWith("PRJ_")) {
						String fileprj = fileName.substring("prj_".length());
						if ((activeRealms.stream().anyMatch(fileprj::equals))
								|| ("prj_genny".equalsIgnoreCase(fileName))) {
							localRealm = fileName.substring("prj_".length()).toLowerCase(); // extract realm name
							log.info("LocalRealm changed to " + localRealm);
						} else {
							continue;
						}
					}
					List<String> filesList = null;

					if (Vertx.currentContext() != null) {
						filesList = Vertx.currentContext().owner().fileSystem().readDirBlocking(inputFileStr);
					} else {
						final File folder = new File(inputFileStr);
						final File[] listOfFiles = folder.listFiles();
						filesList = new ArrayList<String>();
						if (listOfFiles != null) {
							for (File f : listOfFiles) {
								filesList.add(f.getAbsolutePath());
							}
						} else {
							log.info("No files to load from " + inputFileStrs);
						}
					}

					for (final String dirFileStr : filesList) {
						List<Tuple3<String, String, String>> childRules = processFileRealmsFromFiles(localRealm,
								dirFileStr, realms); // use
						// directory
						// name
						// as
						// rulegroup
						rules.addAll(childRules);
					}
				}

			} else {
				String nonVertxFileText = null;
				Buffer buf = null;
				if (Vertx.currentContext() != null) {
					buf = Vertx.currentContext().owner().fileSystem().readFileBlocking(inputFileStr);
				} else {
					try {
						nonVertxFileText = getFileAsText(inputFileStr);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {
					if ((!fileName.startsWith("XX")) && (fileNameExt.equalsIgnoreCase("drl"))) { // ignore files that
																									// start
																									// with XX
						String ruleText = null;
						if (Vertx.currentContext() != null) {
							ruleText = buf.toString();
						} else {
							ruleText = nonVertxFileText;
						}

						Tuple3<String, String, String> rule = (Tuple.of(realm, fileName + "." + fileNameExt, ruleText));
						String filerule = inputFileStr.substring(inputFileStr.indexOf("/rules/"));
						log.info("(" + realm + ") Loading in DRL Rule:" + rule._1 + " of " + inputFileStr);
						rules.add(rule);
					} else if ((!fileName.startsWith("XX")) && (fileNameExt.equalsIgnoreCase("bpmn"))) { // ignore files
																											// that
																											// start
																											// with XX
						String bpmnText = null;
						if (Vertx.currentContext() != null) {
							bpmnText = buf.toString();
						} else {
							bpmnText = nonVertxFileText;
						}

						Tuple3<String, String, String> bpmn = (Tuple.of(realm, fileName + "." + fileNameExt, bpmnText));
						log.info(realm + " Loading in BPMN:" + bpmn._1 + " of " + inputFileStr);
						rules.add(bpmn);
					} else if ((!fileName.startsWith("XX")) && (fileNameExt.equalsIgnoreCase("xls"))) { // ignore files
																										// that
																										// start with XX
						String xlsText = null;
						if (Vertx.currentContext() != null) {
							xlsText = buf.toString();
						} else {
							xlsText = nonVertxFileText;
						}

						Tuple3<String, String, String> xls = (Tuple.of(realm, fileName + "." + fileNameExt, xlsText));
						log.info(realm + " Loading in XLS:" + xls._1 + " of " + inputFileStr);
						rules.add(xls);
					}

				} catch (final DecodeException dE) {

				}

			}
		}
		return rules;
	}

	private static String getFileAsText(final String inputFilePath) throws IOException {
		File file = new File(inputFilePath);
		final BufferedReader in = new BufferedReader(new FileReader(file));
		String ret = "";
		String line = null;
		while ((line = in.readLine()) != null) {
			ret += line;
		}
		in.close();

		return ret;
	}

	public static Set<String> getRealms(final List<Tuple3<String, String, String>> rules) {
		Set<String> realms = new HashSet<String>();

		for (Tuple3<String, String, String> rule : rules) {
			String realm = rule._1;
			realms.add(realm);
		}
		return realms;
	}

	public static Integer setupKieRules(final String realm, final List<Tuple3<String, String, String>> rules) {
		Integer count = 0;
		try {
			// load up the knowledge base
			if (ks == null) {
				log.error("ks is NULL");
				ks = KieServices.Factory.get();
			} else {
				ks = null; // clear
				ks = KieServices.Factory.get();
			}

			final KieFileSystem kfs = ks.newKieFileSystem();

			// Write each rule into it's realm cache
			for (final Tuple3<String, String, String> rule : rules) {
				if (writeRulesIntoKieFileSystem(realm, rules, kfs, rule)) {
					count++;
				}
			}
			log.info("Creating RulesGraph");
			FrameUtils2.rulesGraph = FrameUtils2.graphBuilder.createDirectedGraph();

			if (rulesChanged) {
				log.info("Theme and Frame Rules CHANGED. RUNNING init frames...");
			} else {
				log.info("Theme and Frame Rules DID NOT CHANGE. NOT RUNNING init frames...");
			}

			final KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();
			if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
				log.info(kieBuilder.getResults().toString());
			}

			ReleaseId releaseId = kieBuilder.getKieModule().getReleaseId();
			log.info("kieBuilder kieModule getReleaseId = " + releaseId);
			final KieContainer kContainer = ks.newKieContainer(releaseId);
			final KieBaseConfiguration kbconf = ks.newKieBaseConfiguration();
			kbconf.setProperty("name", realm);
			final KieBase kbase = kContainer.newKieBase(kbconf);

			if (RUNTIME_MANAGER_ON) {
				// This is needed to get around the user permissions : TODO!!!!! fix
				System.setProperty("org.kie.server.bypass.auth.user", "true");

				/* Using Runtime Environment */
				runtimeEnvironmentBuilder = RuntimeEnvironmentBuilder.Factory.get().newDefaultBuilder();

				if (GennySettings.useExecutor) { // default to true if no system env set
					log.info("USING EXECUTOR!");
					if (GennySettings.useJMS) {
						log.info("USING JMS!");
						System.setProperty("org.kie.executor.jms", "true");
					} else {
						log.info("NOT USING JMS!");
						System.setProperty("org.kie.executor.jms", "false");
					}
					// String executorQueueName = "KIE.SERVER.EXECUTOR";
					String executorQueueName = "queue/KIE.SERVER.EXECUTOR";
					// build executor service
					executorService = ExecutorServiceFactory.newExecutorService(emf);
					executorService.setInterval(3);
					executorService.setRetries(3);
					executorService.setThreadPoolSize(10);
					executorService.setTimeunit(TimeUnit.valueOf("SECONDS"));

					((ExecutorImpl) ((ExecutorServiceImpl) executorService).getExecutor())
							.setQueueName(executorQueueName);

					executorService.init();

//					Map<String, Object> jmsProps = new HashMap<String, Object>();
//					jmsProps.put("jbpm.audit.jms.transacted", true);
//					jmsProps.put("jbpm.audit.jms.connection.factory", factory);
//					jmsProps.put("jbpm.audit.jms.queue", queue);
//					AbstractAuditLogger auditLogger = AuditLoggerFactory.newInstance(Type.JMS, ksession, jmsProps);
					// ksession.addProcessEventListener(auditLogger);

					runtimeEnvironment = runtimeEnvironmentBuilder.knowledgeBase(kbase)

							.entityManagerFactory(emf).addEnvironmentEntry("ExecutorService", executorService)
							.registerableItemsFactory(new DefaultRegisterableItemsFactory() {
								@Override
								public Map<String, WorkItemHandler> getWorkItemHandlers(RuntimeEngine runtime) {

									Map<String, WorkItemHandler> handlers = super.getWorkItemHandlers(runtime);
									// handlers.put("async", new AsyncWorkItemHandler(executorService,
									// "org.jbpm.executor.commands.PrintOutCommand"));
									Map<String, WorkItemHandler> gennyHandlers = getHandlers(runtime, null);
									for (String handlerKey : gennyHandlers.keySet()) {
										handlers.put(handlerKey, gennyHandlers.get(handlerKey));
									}
									return handlers;
								}

								@Override
								public List<ProcessEventListener> getProcessEventListeners(RuntimeEngine runtime) {
									List<ProcessEventListener> listeners = super.getProcessEventListeners(runtime);
									// listeners.add(countDownListener);
									return listeners;
								}

							}).userGroupCallback(new UserGroupCallback() {
								public List<String> getGroupsForUser(String userId) {
									List<String> result = new ArrayList<String>();
									if ("sales-rep".equals(userId)) {
										result.add("sales");
									} else if ("john".equals(userId)) {
										result.add("PM");
									}
									return result;
								}

								public boolean existsUser(String arg0) {
									return true;
								}

								public boolean existsGroup(String arg0) {
									return true;
								}
							})

							.get();
				} else {
					log.info("NOT USING EXECUTOR!");
					runtimeEnvironment = runtimeEnvironmentBuilder.knowledgeBase(kbase).entityManagerFactory(emf).get();
				}

				// <property name="org.kie.executor.jms.queue"
				// value="queue/KIE.SERVER.EXECUTOR"/>

				if (runtimeManager != null) {
					log.info("Closing active runtime Manager Id = " + runtimeManager.getIdentifier());
					runtimeManager.close();
				}

				if (GennySettings.useSingleton) { // TODO
					log.info("Creating Singleton runtimeManager for " + realm);
					runtimeManager = RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(runtimeEnvironment,
							realm); // TODO
					log.info("Created Singleton runtimeManager for " + realm);

				} else {
					runtimeManager = RuntimeManagerFactory.Factory.get().newPerRequestRuntimeManager(runtimeEnvironment,
							realm);
					log.info("Created Per request strategy runtimeManager for " + realm);
				}
			}

			log.info("Put rules KieBase into Custom Cache");
			if (getKieBaseCache().containsKey(realm)) {
				getKieBaseCache().remove(realm);
				log.info(realm + " removed");
			}
			getKieBaseCache().put(realm, kbase);
			log.info(realm + " rules installed\n");

			/*
			 * getting Runtime Engine from RuntimeManager each instance of Engine handles
			 * one KieSession
			 */
			RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(EmptyContext.get());

			TaskService taskService = runtimeEngine.getTaskService();
			synchronized (taskServiceMap) {
				taskServiceMap.put(realm, taskService);
			}

			/* For using ProcessInstanceIdContext */
			// RuntimeEngine runtimeEngine =
			// runtimeManager.getRuntimeEngine(ProcessInstanceIdContext.get());

			/* Getting KieSession */
			KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();
			ksconf.setProperty("name", realm);

			if (GennySettings.useSingleton) { // TODO
				KieSession kieSession = runtimeEngine.getKieSession();
				kieSessionMap.put(realm, kieSession);

				// JPAWorkingMemoryDbLogger logger = new JPAWorkingMemoryDbLog;ger(kieSession);
				AbstractAuditLogger logger = new NodeStatusLog(kieSession);

				// addHandlers(kieSession);
				kieSession.addEventListener(logger);
				kieSession.addEventListener(new GennyAgendaEventListener());

				kieSession.getWorkItemManager().registerWorkItemHandler("AskQuestionTask",
						new AskQuestionTaskWorkItemHandler(RulesLoader.class, kieSession, taskService));
				kieSession.getWorkItemManager().registerWorkItemHandler("ProcessAnswers",
						new ProcessAnswersWorkItemHandler(RulesLoader.class, kieSession.getEnvironment(), taskService));
				kieSession.getWorkItemManager().registerWorkItemHandler("CheckTasks",
						new CheckTasksWorkItemHandler(RulesLoader.class, kieSession, taskService));

				kieSession.getEnvironment().set("Autoclaim", "true"); // for JBPM
			}

			// Set up taskService
//			userTaskService = runtimeEngine.
//	        TaskServiceSession taskSession = taskService.createSession();

		} catch (final Throwable t) {
			t.printStackTrace();
		}
		return count;
	}

	/**
	 * @param realm
	 * @param rules
	 * @param kfs
	 * @param rule
	 */
	private static boolean writeRulesIntoKieFileSystem(final String realm,
			final List<Tuple3<String, String, String>> rules, final KieFileSystem kfs,
			final Tuple3<String, String, String> rule) {
		boolean ret = false;

		if (rule._1.equalsIgnoreCase("genny") || rule._1.equalsIgnoreCase(realm)) {
			if ((rule._1.equalsIgnoreCase("genny")) && (!"genny".equalsIgnoreCase(realm))) {
				String filename = rule._2;
				for (Tuple3<String, String, String> ruleCheck : rules) { // look for rules that are not genny rules
					String realmCheck = ruleCheck._1;
					if (realmCheck.equals(realm)) {

						String filenameCheck = ruleCheck._2;
						if (filenameCheck.equalsIgnoreCase(filename)) {
							log.info("Ditching the genny rule because higher rule overrides:" + rule._1 + " : "
									+ rule._2);
							return false; // do not save this genny rule as there is a proper realm rule with same name
						}
					}

				}
			}
			if (rule._2.endsWith(".drl")) {
				if (rule._3 == null) {
					log.error(rule + " has null file text");

				} else {
					final String inMemoryDrlFileName = RESOURCE_PATH + rule._2;
					Resource rs = ks.getResources().newReaderResource(new StringReader(rule._3));
					kfs.write(inMemoryDrlFileName, rs.setResourceType(ResourceType.DRL));
					DrlParser parser = new DrlParser();
					try {
						if (parser.parse(rs).getRules().size() > 1) {
							log.error("ERROR!! " + rule._2 + " has more than one rule in it!");
						}
						if (parser.parse(rs).getRules().size() == 0) {
							log.error("ERROR!! " + rule._2 + " has NO Rules in it");
						} else {
							RuleDescr ruleObj = null;
							try {

								ruleObj = parser.parse(rs).getRules().get(0);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							processRule(realm, ruleObj, rule);
						}
					} catch (NullPointerException e) {
						log.error("Error with the rules:: " + rule._2 + " -> " + e.getLocalizedMessage());

					}

					catch (DroolsParserException e) {
						log.error("BAD RULE : " + rule._2 + " -> " + e.getLocalizedMessage());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else if (rule._2.endsWith(".bpmn")) {
				// final String inMemoryDrlFileName = "src/main/resources/" + rule._2;
				final String inMemoryDrlFileName = RESOURCE_PATH + rule._2;
				Resource rs = ks.getResources().newReaderResource(new StringReader(rule._3));
				kfs.write(inMemoryDrlFileName, rs.setResourceType(ResourceType.BPMN2));
//				processJbpm(realm, rule);
//				DrlParser parser = new DrlParser();
//				try {
//					RuleDescr ruleObj = parser.parse(rs).getRules().get(0);
//					processRule(realm, ruleObj, rule);
//				} catch (NullPointerException e) {
//					log.error("Error with the rules:: " + rule._2 + " -> " + e.getLocalizedMessage());
//				}
//				catch (DroolsParserException e) {
//					log.error("BAD RULE : " + rule._2 + " -> " + e.getLocalizedMessage());
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}

			} else if (rule._2.endsWith(".xls")) {
				final String inMemoryDrlFileName = RESOURCE_PATH + rule._2;

			} else {
				final String inMemoryDrlFileName = RESOURCE_PATH + rule._2;
				kfs.write(inMemoryDrlFileName, ks.getResources().newReaderResource(new StringReader(rule._3))
						.setResourceType(ResourceType.DRL));
			}
			ret = true;
		}

		return ret;
	}

	private KieSession createNewKieSession(SessionFacts facts, boolean isInitEvent) {
		String realm = facts.getServiceToken().getRealm();

		RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(EmptyContext.get());
		log.info(debugStr + runtimeEngine.toString());

		KieSession kieSession = runtimeEngine.getKieSession();
		log.info(debugStr + kieSession.toString());

		// update taskSerice map
		TaskService taskService = runtimeEngine.getTaskService();
		synchronized (taskServiceMap) {
			taskServiceMap.put(realm, taskService);
			// userToken is null when application startup
			if (!isInitEvent) {
				taskServiceMap.put(facts.getUserToken().getSessionCode(), taskService);
			}
		}
		// JPAWorkingMemoryDbLogger logger = new JPAWorkingMemoryDbLogger(kieSession);
		// AbstractAuditLogger logger = new NodeStatusLog(kieSession);
		AbstractAuditLogger logger = new NodeStatusLog(emf, env);
//				 addHandlers(kieSession);
		kieSession.addEventListener(new GennyRuleTimingListener());
		kieSession.addEventListener(logger);
		kieSession.addEventListener(new GennyAgendaEventListener());
		kieSession.addEventListener(new JbpmInitListener(facts.getServiceToken()));

		kieSession.getWorkItemManager().registerWorkItemHandler("AskQuestionTask",
				new AskQuestionTaskWorkItemHandler(RulesLoader.class, kieSession, taskService));

		kieSession.getWorkItemManager().registerWorkItemHandler("ProcessAnswers",
				new ProcessAnswersWorkItemHandler(RulesLoader.class, kieSession.getEnvironment(), taskService)); // the
																													// env
																													// should
																													// be
																													// the
																													// same
																													// for
																													// all
																													// kieSessions

		kieSession.getWorkItemManager().registerWorkItemHandler("CheckTasks",
				new CheckTasksWorkItemHandler(RulesLoader.class, kieSession, taskService));

		kieSession.getEnvironment().set("Autoclaim", "true"); // for JBPM

		/* set up a global */
		QBulkMessage payload = new QBulkMessage();
		kieSession.setGlobal("payload", payload);

		return kieSession;
	}

	// @Transactional(dontRollbackOn = {
	// org.drools.persistence.jta.JtaTransactionManager.class })
	private KieSession getKieSesion(SessionFacts facts, boolean isInitEvent) {
		KieSession kieSession = null;
		String sessionCode = facts.getServiceToken().getRealm();

		if (!isInitEvent) {
			sessionCode = facts.getUserToken().getSessionCode();
		}

		if (RUNTIME_MANAGER_ON) {
			if (GennySettings.useSingleton) { // TODO
				kieSession = kieSessionMap.get(facts.getServiceToken().getRealm());
				// map to current sessionCode
				kieSessionMap.put(sessionCode, kieSession);
				kieSession.addEventListener(new JbpmInitListener(facts.getServiceToken()));

				log.info("Using Runtime engine in Singleton Strategy ::::::: Stateful with kieSession id="
						+ kieSession.getIdentifier());
			} else {
				kieSession = createNewKieSession(facts, isInitEvent);
				if (kieSessionMap.get(sessionCode) == null) {
					// map to current sessionCode
					kieSessionMap.put(sessionCode, kieSession);
					log.info("Create new KieSession:" + kieSession.getIdentifier());
				} else {
					kieSessionMap.replace(sessionCode, kieSession);
					log.info(debugStr + "Replace with new KieSession:" + kieSession.getIdentifier());
				}
				log.info("Using Runtime engine in Per Request Strategy ::::::: Stateful with kieSession id="
						+ kieSession.getIdentifier());
			}
		}
		return kieSession;
	}

	private void processQEventMessageEvent(SessionFacts facts, long processId, KieSession kieSession) {
		((QEventMessage) facts.getMessage()).setToken(facts.getUserToken().getToken());

		String msg_code = ((QEventMessage) facts.getMessage()).getData().getCode();
		String bridgeSourceAddress = ((QEventMessage) facts.getMessage()).getSourceAddress();

		// Save an associated Bridge IP to the session
		log.info("saving bridge ip to cache associted with session " + facts.getUserToken().getSessionCode());
		VertxUtils.writeCachedJson(facts.getUserToken().getRealm(), facts.getUserToken().getSessionCode(),
				bridgeSourceAddress, facts.getUserToken().getToken());

		log.info("incoming EVENT" + " message from " + bridgeSourceAddress + ": " + facts.getUserToken().getRealm()
				+ ": " + facts.getUserToken().getSessionCode() + ": " + facts.getUserToken().getUserCode() + "   "
				+ msg_code + " to pid " + processId);

		// kieSession.signalEvent("EV_"+session_state, facts);

		// HACK!!
		if (msg_code.equals("QUE_SUBMIT")) {
			Answer dataAnswer = new Answer(facts.getUserToken().getUserCode(), facts.getUserToken().getUserCode(),
					"PRI_SUBMIT", "QUE_SUBMIT");
//									String ad = "{\"street_number\":\"63\",\"street_name\":\"Fakenham Road\",\"suburb\":\"Ashburton\",\"state\":\"Victoria\",\"country\":\"AU\",\"postal_code\":\"3147\",\"full_address\":\"64 Fakenham Rd, Ashburton VIC 3147, Australia\",\"latitude\":-37.863208,\"longitude\":145.092359,\"street_address\":\"64 Fakenham Road\"}";
//									dataAnswer = new Answer(facts.getUserToken().getUserCode(),
//											facts.getUserToken().getUserCode(), "PRI_ADDRESS_JSON", ad);
			dataAnswer.setChangeEvent(false);
			QDataAnswerMessage dataMsg = new QDataAnswerMessage(dataAnswer);
			SessionFacts sessionFactsData = new SessionFacts(facts.getServiceToken(), facts.getUserToken(), dataMsg);
			log.info("SignalEvent -> QUE_SUBMIT event to 'data' for " + facts.getUserToken().getUserCode() + ":"
					+ processId);
			kieSession.signalEvent("data", sessionFactsData, processId);
		} else if (msg_code.equals("QUE_CANCEL")) {
			Answer dataAnswer = new Answer(facts.getUserToken().getUserCode(), facts.getUserToken().getUserCode(),
					"PRI_SUBMIT", "QUE_CANCEL");
			dataAnswer.setChangeEvent(false);
			QDataAnswerMessage dataMsg = new QDataAnswerMessage(dataAnswer);
			SessionFacts sessionFactsData = new SessionFacts(facts.getServiceToken(), facts.getUserToken(), dataMsg);
			log.info("SignalEvent -> QUE_CANCEL event to 'data' for " + facts.getUserToken().getUserCode() + ":"
					+ processId);
			kieSession.signalEvent("data", sessionFactsData, processId);
		} else {
			log.info("SignalEvent -> 'event' for " + facts.getUserToken().getUserCode() + ":" + processId);
			try {
				kieSession.signalEvent("event", facts, processId);
			} catch (Exception e) {
				log.error("Bad Session Error for process Id " + processId + " and userCode "
						+ facts.getUserToken().getUserCode());

			}
		}
	}

	private void processQDataMessageEvent(SessionFacts facts, long processId, KieSession kieSession) {
		((QDataMessage) facts.getMessage()).setToken(facts.getUserToken().getToken());

		String msg_code = ((QDataMessage) facts.getMessage()).getData_type();
		String bridgeSourceAddress = ((QDataMessage) facts.getMessage()).getSourceAddress();

		// Save an associated Bridge IP to the session
		log.info("saving bridge ip to cache associted with session " + facts.getUserToken().getSessionCode());
		VertxUtils.writeCachedJson(facts.getUserToken().getRealm(), facts.getUserToken().getSessionCode(),
				bridgeSourceAddress, facts.getUserToken().getToken());
		log.info("incoming DATA" + " message from " + bridgeSourceAddress + ": " + facts.getUserToken().getRealm() + ":"
				+ facts.getUserToken().getSessionCode() + ":" + facts.getUserToken().getUserCode() + "   " + msg_code
				+ " to pid " + processId);

		// kieSession.signalEvent("DT_"+session_state, facts);
		log.info("SignalEvent -> 'data' for " + facts.getUserToken().getUserCode() + ":" + processId);
		try {
			if (facts.getMessage() == null) {
				log.error("facts.getMessage() is NULL");
			} else if (facts.getUserToken() == null) {
				log.error("facts.getUserToken() is NULL");
			} else if (facts.getServiceToken() == null) {
				log.error("facts.getServiceToken() is NULL");
//			} else if (processId == null) {
//				log.error("processId is NULL");
			} else {
				kieSession.signalEvent("data", facts, processId);
			}
		} catch (Exception e) {
			log.error("Error in data signal :" + facts + ":" + e.getLocalizedMessage());
		}
	}

	private void processAuthInitEvent(SessionFacts facts, KieSession kieSession) {
		((QEventMessage) facts.getMessage()).getData().setValue("NEW_SESSION");
		String bridgeSourceAddress = ((QEventMessage) facts.getMessage()).getSourceAddress();

		log.info("incoming  AUTH_INIT message from " + bridgeSourceAddress + ": " + facts.getUserToken().getRealm()
				+ ":" + facts.getUserToken().getSessionCode() + ":" + facts.getUserToken().getUserCode() + "   "
				+ "AUTH_INIT" + " to NEW SESSION");
		/* sending New Session Signal */
		kieSession.signalEvent("newSession", facts);
	}

	private void sendEventThroughUserSession(SessionFacts facts, KieSession kieSession) throws InterruptedException {
		log.info("Setting up Capabilities and Alloweds");
		String bridgeSourceAddress = "";
		GennyToken serviceToken = facts.getServiceToken();

		BaseEntityUtils beUtils = new BaseEntityUtils(facts.getUserToken());
		beUtils.setServiceToken(facts.getServiceToken());
		log.info("BaseEntity created");

		FactHandle beUtilsHandle = kieSession.insert(beUtils);
		FactHandle capabilityUtilsHandle = null;
		List<FactHandle> allowables = new ArrayList<FactHandle>();

		CapabilityUtils capabilityUtils = new CapabilityUtils(beUtils);

		/*
		 * log.info("CapabilityUtils created , now processing");
		 * capabilityUtils.process(); log.info("CapabilitysUtils processed ");
		 */

		BaseEntity user = beUtils.getBaseEntityByCode(facts.getUserToken().getUserCode());
		if (user != null) {
			log.info("User:" + user.getCode() + "fetched.");

			List<Allowed> allowable = CapabilityUtils.generateAlloweds(facts.getUserToken(), user);
			log.info(allowable.size() + " Alloweds generated ");

			capabilityUtilsHandle = kieSession.insert(capabilityUtils);

			log.info("Adding Allowed to kiesession");
			// get each capability from each Role and add to allowables
			for (Allowed allow : allowable) {
				allowables.add(kieSession.insert(allow));
			}
		}

		Long processId = null;
		String session_state = facts.getUserToken().getSessionCode();
		int processState = -1;

		log.info("Looking up ProcessId by session " + session_state);
		Optional<Long> processIdBysessionId = getProcessIdBysessionId(serviceToken.getRealm(), session_state);

		if (processIdBysessionId.isPresent()) {
			processId = processIdBysessionId.get();
			processState = kieSession.getProcessInstance(processId).getState();

//			while(processState != ProcessInstance.STATE_COMPLETED) {
//				log.warn("Current process:" + processId + " not completed, state is:" + processState + ", wait 1 second.");
//				processState = kieSession.getProcessInstance(processId).getState();
//				TimeUnit.SECONDS.sleep(1);
//			}

			/* If the message is QEventMessage then send in to event channel */
			if (facts.getMessage() instanceof QEventMessage) {
				processQEventMessageEvent(facts, processId, kieSession);
			} else if (facts.getMessage() instanceof QDataMessage) {
				/* If the message is data message then send in to data channel */
				processQDataMessageEvent(facts, processId, kieSession);
			}
		} else {
			if (facts.getMessage() instanceof QEventMessage
					&& ((QEventMessage) facts.getMessage()).getData().getCode().equals("AUTH_INIT")) {
				/* If the message is QeventMessage and the Event Message is AuthInit */
				processAuthInitEvent(facts, kieSession);
			} else {
				log.error("NO EXISTING SESSION AND NOT AUTH_INIT");
			}
		}
		// Cleanup facts
		kieSession.delete(beUtilsHandle);
		if (capabilityUtilsHandle != null) {
			kieSession.delete(capabilityUtilsHandle);
			for (FactHandle allow : allowables) {
				kieSession.delete(allow);
			}
		}
	}

	public void executeStatefulForIintEvent(final List<Tuple2<String, Object>> globals, SessionFacts facts) {
		int rulesFired = 0;
		GennyToken serviceToken = facts.getServiceToken();

		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();

		if (getKieBaseCache().get(serviceToken.getRealm()) == null) {
			log.error("The realm  kieBaseCache is null, not loaded " + serviceToken.getRealm());
			return;
		}

		KieSession kieSession = getKieSesion(facts, true);

		try {
			tx.begin();
			log.info("initProject Events! with facts=" + facts);
			kieSession.signalEvent("initProject", facts);
		} catch (NullPointerException e) {
			log.error("Null pointer Exception thrown in workflow/rules");
		} catch (final Throwable t) {
			log.error(t.getLocalizedMessage());
		} finally {
			log.info("Finished Message Handling - Fired " + rulesFired + " rules for " + facts.getUserToken());
			// commit
			tx.commit();
			em.close();
		}
	}

	public synchronized void executeStateful(final List<Tuple2<String, Object>> globals, SessionFacts facts)
			throws InterruptedException {
		
		TimeUnit.SECONDS.sleep(2);
		int rulesFired = 0;
		GennyToken serviceToken = facts.getServiceToken();

		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();

		if (getKieBaseCache().get(serviceToken.getRealm()) == null) {
			log.error("The realm  kieBaseCache is null, not loaded " + serviceToken.getRealm());
			return;
		}

		String sessionCode = facts.getUserToken().getSessionCode();
		// get new kieSession
		KieSession kieSession = getKieSesion(facts, false);
//		Collection<ProcessInstance> processInstances = kieSession.getProcessInstances();
//		for (ProcessInstance p : processInstances) {
//			log.info(debugStr + "KiesessionID:" +  kieSession.getIdentifier() + ",ProcessID:" + p.getProcessId()
//					+ ",ID:" + p.getId()
//					+ ",State:" + p.getState()
//					+ ",ProcessName:" + p.getProcessName()
//					+ ",ParentID" + p.getParentProcessInstanceId());
//		}

		try {
			tx.begin();
			/* If userToken is not null then send the event through user Session */
			if (facts.getUserToken() != null) {
				sendEventThroughUserSession(facts, kieSession);
			} else if (((QEventMessage) facts.getMessage()).getData().getCode().equals("INIT_STARTUP")) {
				/* When usertoken is null */
				/* Running init_project workflow */
				log.info("initProject Events! with facts=" + facts);
				kieSession.signalEvent("initProject", facts);
			} else {
				log.info("Invalid Events coming in");
			}
			// rulesFired = kieSession.fireAllRules();
		} catch (NullPointerException e) {
			log.error("Null pointer Exception thrown in workflow/rules");
		} catch (final Throwable t) {
			log.error(t.getLocalizedMessage());
		} finally {
			log.info("Finished Message Handling - Fired " + rulesFired + " rules for " + facts.getUserToken());
			// commit
			if (tx.isActive()) {
				tx.commit();
			}
			if (em.isOpen())
				em.close();
			// runtimeManager.disposeRuntimeEngine(runtimeEngine);
		}
	}
//		else {
//
//			// StatefulKnowledgeSession kieSession = null;
//		/*	synchronized (kieSession) */{
//				try {
//					tx.begin();
//
//					if (getKieBaseCache().get(facts.getServiceToken().getRealm()) == null) {
//						log.error("The realm  kieBaseCache is null, not loaded " + facts.getServiceToken().getRealm());
//						return;
//					}
//
//					KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();
//
//					kieSession = JPAKnowledgeService.newStatefulKnowledgeSession(
//							getKieBaseCache().get(facts.getServiceToken().getRealm()), ksconf, env);
//
//					JPAWorkingMemoryDbLogger logger = new JPAWorkingMemoryDbLogger(kieSession);
//
//					// addHandlers(kieSession);
//
//					kieSession.addEventListener(new GennyAgendaEventListener());
//					kieSession.addEventListener(new JbpmInitListener(facts.getServiceToken()));
//
//					/* If userToken is not null then send the event through user Session */
//					if (facts.getUserToken() != null) {
//
//						String session_state = facts.getUserToken().getSessionCode();
//						Long processId = null;
//
//						Optional<Long> processIdBysessionId = getProcessIdBysessionId(session_state);
//
//						/* Check if the process already exist or not is there */
//						boolean hasProcessIdBySessionId = processIdBysessionId.isPresent();
//
//						if (hasProcessIdBySessionId) {
//
//							processId = processIdBysessionId.get();
//
//							/* If the message is QEventMessage then send in to event channel */
//							if (facts.getMessage() instanceof QEventMessage) {
//
//								((QEventMessage) facts.getMessage()).setToken(facts.getUserToken().getToken());
//
//								msg_code = ((QEventMessage) facts.getMessage()).getData().getCode();
//								bridgeSourceAddress = ((QEventMessage) facts.getMessage()).getSourceAddress();
//
//								log.info("incoming EVENT" + " message from " + bridgeSourceAddress + ": "
//										+ facts.getUserToken().getRealm() + ":" + facts.getUserToken().getSessionCode()
//										+ ":" + facts.getUserToken().getUserCode() + "   " + msg_code + " to pid "
//										+ processId);
//
//								kieSession.signalEvent("event", facts, processId);
//							}
//
//							/* If the message is data message then send in to data channel */
//							else if (facts.getMessage() instanceof QDataMessage) {
//
//								((QDataMessage) facts.getMessage()).setToken(facts.getUserToken().getToken());
//
//								msg_code = ((QDataMessage) facts.getMessage()).getData_type();
//								bridgeSourceAddress = ((QDataMessage) facts.getMessage()).getSourceAddress();
//
//								log.info("incoming DATA" + " message from " + bridgeSourceAddress + ": "
//										+ facts.getUserToken().getRealm() + ":" + facts.getUserToken().getSessionCode()
//										+ ":" + facts.getUserToken().getUserCode() + "   " + msg_code + " to pid "
//										+ processId);
//
//								kieSession.signalEvent("data", facts, processId);
//							}
//
//						} else {
//
//							/* If the message is QeventMessage and the Event Message is AuthInit */
//							if (facts.getMessage() instanceof QEventMessage
//									&& ((QEventMessage) facts.getMessage()).getData().getCode().equals("AUTH_INIT")) {
//
//								((QEventMessage) facts.getMessage()).getData().setValue("NEW_SESSION");
//								bridgeSourceAddress = ((QEventMessage) facts.getMessage()).getSourceAddress();
//
//								log.info("incoming  message from " + bridgeSourceAddress + ": "
//										+ facts.getUserToken().getRealm() + ":" + facts.getUserToken().getSessionCode()
//										+ ":" + facts.getUserToken().getUserCode() + "   " + msg_code
//										+ " to NEW SESSION");
//
//								/* sending New Session Signal */
//								log.info("Message is event Authinit");
//								kieSession.signalEvent("newSession", facts);
//
//							} else {
//								log.error("NO EXISTING SESSION AND NOT AUTH_INIT");
//
//							}
//
//						}
//					} /* When usertoken is null */
//					else if (((QEventMessage) facts.getMessage()).getData().getCode().equals("INIT_STARTUP")) {
//
//						/* Running init_project workflow */
//						kieSession.startProcess("init_project");
//					} else {
//						log.info("Invalid Events coming in");
//					}
//					// rulesFired = kieSession.fireAllRules();
//				} catch (final Throwable t) {
//					log.error(t.getLocalizedMessage());
//					;
//				} finally {
//					log.info("Finished Message Handling - Fired " + rulesFired + " rules for " + facts.getUserToken()
//							+ ":" + facts.getUserToken().getSessionCode());
//					// commit
//
//					tx.commit();
//					em.close();
//					// kieSession.dispose();
//
//				}
//			}
//		}

	public static Map<String, Object> executeStateless(final List<Tuple2<String, Object>> globals,
			final List<Object> facts, final GennyToken serviceToken, final GennyToken userToken) {
		Map<String, Object> results = new HashMap<String, Object>();

		StatefulKnowledgeSession kieSession = null;
		int rulesFired = 0;
		QEventMessage eventMsg = null;
		QDataMessage dataMsg = null;
		String msg_code = "";
		String msg_type = "";
		GennyToken gToken = serviceToken;
		String bridgeSourceAddress = "";

		log.info("EXECUTING STATELESS HANDLER");

		try {

			if (getKieBaseCache().get(serviceToken.getRealm()) == null) {
				log.error("The realm  kieBaseCache is null, not loaded " + serviceToken.getRealm());
				results.put("status", "ERROR");
				results.put("value", "The realm  kieBaseCache is null, not loaded " + serviceToken.getRealm());

				return results;
			}

			KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();

			kieSession = (StatefulKnowledgeSession) getKieBaseCache().get(serviceToken.getRealm()).newKieSession(ksconf,
					env);
//			KieSession kieSession = kieSessionMap.get(serviceToken.getRealm());

			kieSession.addEventListener(new GennyAgendaEventListener());
			kieSession.addEventListener(new JbpmInitListener(serviceToken));

			for (final Object fact : facts) {
				kieSession.insert(fact);
				if (fact instanceof QEventMessage) {
					eventMsg = (QEventMessage) fact;
					if (userToken != null) {
						eventMsg.setToken(userToken.getToken());
					} else {
						eventMsg.setToken(serviceToken.getToken());
					}
					msg_code = eventMsg.getData().getCode();
					msg_type = "EVENT";
					bridgeSourceAddress = eventMsg.getSourceAddress();
				}
				if (fact instanceof QDataMessage) {
					dataMsg = (QDataMessage) fact;
					dataMsg.setToken(userToken.getToken());
					msg_code = dataMsg.getData_type();
					msg_type = "DATA";
					bridgeSourceAddress = dataMsg.getSourceAddress();
				}

			}

			BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
			CapabilityUtils capabilityUtils = new CapabilityUtils(beUtils);

			rulesFired = kieSession.fireAllRules();

		} catch (final Throwable t) {
			log.error(t.getLocalizedMessage());
			results.put("status", "ERROR");
			results.put("value", t.getLocalizedMessage());
		} finally {
			log.info("Finished Stateless Message Handling (" + msg_code + ") - Fired " + rulesFired + " rules for "
					+ userToken.getUserCode() + ":" + userToken.getSessionCode());
			kieSession.dispose();
			results.put("status", "OK");

		}
		return results;
	}

	public static Map<String, KieBase> getKieBaseCache() {
		return kieBaseCache;
	}

	public static void setKieBaseCache(Map<String, KieBase> kieBaseCache) {
		RulesLoader.kieBaseCache = kieBaseCache;

	}

	public static List<Tuple2<String, Object>> getStandardGlobals() {
		List<Tuple2<String, Object>> globals = new ArrayList<Tuple2<String, Object>>();
		String RESET = "\u001B[0m";
		String RED = "\u001B[31m";
		String GREEN = "\u001B[32m";
		String YELLOW = "\u001B[33m";
		String BLUE = "\u001B[34m";
		String PURPLE = "\u001B[35m";
		String CYAN = "\u001B[36m";
		String WHITE = "\u001B[37m";
		String BOLD = "\u001b[1m";

		globals.add(Tuple.of("LOG_RESET", RESET));
		globals.add(Tuple.of("LOG_RED", RED));
		globals.add(Tuple.of("LOG_GREEN", GREEN));
		globals.add(Tuple.of("LOG_YELLOW", YELLOW));
		globals.add(Tuple.of("LOG_BLUE", BLUE));
		globals.add(Tuple.of("LOG_PURPLE", PURPLE));
		globals.add(Tuple.of("LOG_CYAN", CYAN));
		globals.add(Tuple.of("LOG_WHITE", WHITE));
		globals.add(Tuple.of("LOG_BOLD", BOLD));
		globals.add(Tuple.of("REACT_APP_QWANDA_API_URL", GennySettings.qwandaServiceUrl));
		return globals;
	}

	public void initMsg(final String msgType, String realm, final Object msg) {

		log.info("INIT MSG with Stateful");
		// Service Token
		JsonObject tokenObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM, "TOKEN" + realm.toUpperCase());
		String serviceToken = tokenObj.getString("value");

		if ((serviceToken == null) || ("DUMMY".equalsIgnoreCase(serviceToken))) {
			log.error("NO SERVICE TOKEN FOR " + realm + " IN CACHE");
			return;
		}

		GennyToken gennyServiceToken = new GennyToken("PER_SERVICE", serviceToken);

		List<Tuple2<String, Object>> globals = RulesLoader.getStandardGlobals();

		SessionFacts facts = new SessionFacts(gennyServiceToken, null, msg);

		try {
			executeStatefulForIintEvent(globals, facts);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void processMsg(final Object msg, final String token) {

		GennyToken userToken = new GennyToken("userToken", token);

		// Service Token
		String serviceTokenStr = VertxUtils.getObject(userToken.getRealm(), "CACHE", "SERVICE_TOKEN", String.class);
		if (serviceTokenStr == null) {
			log.error("SERVICE TOKEN FETCHED FROM CACHE IS NULL");
		} else {
			GennyToken serviceToken = new GennyToken("PER_SERVICE", serviceTokenStr);

			List<Tuple2<String, Object>> globals = new ArrayList<Tuple2<String, Object>>();

			try {
				if ((msg instanceof QEventAttributeValueChangeMessage) || (msg instanceof QEventLinkChangeMessage)) {
					log.info("Executing Stateless for " + msg);

					List<Object> facts = new ArrayList<Object>();
					facts.add(msg);
					facts.add(userToken);
					facts.add(serviceToken);
					// SessionFacts facts = new SessionFacts(serviceToken, userToken, msg);
					// RulesLoader.executeStateful(globals, facts);
					RulesLoader.executeStateless(globals, facts, serviceToken, userToken);
				} else {

					SessionFacts facts = new SessionFacts(serviceToken, userToken, msg);
					executeStateful(globals, facts);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public static Map<File, ResourceType> getKieResources() {
		return new HashMap<File, ResourceType>(); // TODO
	}

	private static Map<String, WorkItemHandler> getHandlers(RuntimeEngine runtime, KieSession kieSession) {
		Map<String, WorkItemHandler> handlers = new HashMap<String, WorkItemHandler>();
		// log.info("Register SendSignal kiesession");
		handlers.put("SendSignal", new SendSignalWorkItemHandler(RulesLoader.class, runtime));
		handlers.put("SendSignalByWorkflowBeCode", new SendSignalToWorkflowWorkItemHandler(RulesLoader.class, runtime));

		handlers.put("Awesome", new AwesomeHandler());
		handlers.put("GetProcessesUsingVariable", new GetProcessesUsingVariable());
		handlers.put("Notification", new NotificationWorkItemHandler());
		handlers.put("ShowAllForms", new ShowAllFormsHandler());
		handlers.put("ShowFrame", new ShowFrame());
		handlers.put("ShowFrames", new ShowFrames());
		handlers.put("Print", new PrintWorkItemHandler());
		handlers.put("ShowFrameWithContextList", new ShowFrameWIthContextList());
		handlers.put("RuleFlowGroup", new RuleFlowGroupWorkItemHandler());
		handlers.put("ThrowSignalProcess", new ThrowSignalProcessWorkItemHandler(runtime));
//			handlers.put("AskQuestionTask",
//					new AskQuestionTaskWorkItemHandler(RulesLoader.class,runtime));
//			handlers.put("ProcessAnswers",
//					new ProcessAnswersWorkItemHandler(RulesLoader.class,runtime));

		handlers.put("ProcessTaskId", new ProcessTaskIdWorkItemHandler(RulesLoader.class, runtime));

		handlers.put("ThrowSignal", new ThrowSignalWorkItemHandler(RulesLoader.class, runtime));
		// handlers.put("JMSSendTask", new JMSSendTaskWorkItemHandler());

		return handlers;
	}

	/**
	 * @param rulesDir
	 */
	public static void loadRules(final String realm, final String rulesDir) {

		log.info("Loading Rules and workflows!!! for realm " + realm);
		List<String> reloadRealms = new ArrayList<String>();
		reloadRealms.add(realm);
		realms = new HashSet<>(reloadRealms);

		List<Tuple3<String, String, String>> rules = null;
		if (GennySettings.useApiRules) {
			rules = processFileRealmsFromApi(realms);
		} else {
			rules = processFileRealmsFromFiles("genny", rulesDir, realms);
		}

		log.info("LOADED ALL RULES " + rules.size());

		if (rules.size() > 0) {
			gNotReady = true;

			realms.stream().forEach(System.out::println);
			realms.remove("genny");

			log.info("LOADING " + realm + " RULES");
			Integer rulesCount = setupKieRules(realm, rules);
			log.info("Rules Count for " + realm + " = " + rulesCount);

			// set up kie conf
			if (ksconf == null) {
				ksconf = KieServices.Factory.get().newKieSessionConfiguration();
				ksconf.setOption(TimedRuleExecutionOption.YES);
			}
		} else {
			log.error("NO RULES LOADED FROM API");
			gNotReady = false;
		}

	}

	/**
	 * @param rulesDir
	 * @return
	 */
	public void triggerStartupRules(final String realm, final String rulesDir) {
		log.info("Triggering Startup Rules for all " + realm);
		QEventMessage msg = new QEventMessage("EVT_MSG", "INIT_STARTUP");
		msg.getData().setValue((rulesChanged) ? "RULES_CHANGED" : "NO_RULES_CHANGED");
		initMsg("Event:INIT_STARTUP", realm, msg);
		// rulesChanged = false;

		// Now check if all Themes and Frames got created and display errors if
		// missing...
		JsonObject tokenObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM, "TOKEN" + realm.toUpperCase());
		String serviceToken = tokenObj.getString("value");

		if ("DUMMY".equalsIgnoreCase(serviceToken)) {
			log.error("NO SERVICE TOKEN FOR " + realm + " IN CACHE");
			return;
		}

//		for (String themeCode : themeCodes) {
//			Theme theme = VertxUtils.getObject(realm, "", themeCode,
//					Theme.class, serviceToken);			
//			if (theme==null) {
//				log.error(themeCode+" NOT IN CACHE -> NOT CREATED");
//			}
//		}
//		for (String frameCode : frameCodes) {
//			QDataBaseEntityMessage TARGET_FRM_MSG = VertxUtils.getObject(realm, "",
//					frameCode + "_MSG", QDataBaseEntityMessage.class, serviceToken);
//			if (TARGET_FRM_MSG==null) {
//				log.error(frameCode+" NOT IN CACHE -> NOT CREATED");
//			}
//		}

	}

//	public static Optional<Long> getProcessIdBysessionId2(String sessionId) {
//		// Do pagination here
//		QueryContext ctx = new QueryContext(0, 100);
//		Collection<ProcessInstanceDesc> instances = queryService.query("getAllProcessInstances",
//				ProcessInstanceQueryMapper.get(), ctx, QueryParam.equalsTo("value", sessionId));
//
//		return instances.stream().map(d -> d.getId()).findFirst();
//
//	}
	public static Optional<Long> getProcessIdBysessionId(String realm, String sessionId) {
		// Do pagination here
		QueryContext ctx = new QueryContext(0, 100);
		try {
			Collection<SessionPid> instances = queryService.query("getAllSessionPids", SessionPidQueryMapper.get(), ctx,
					QueryParam.equalsTo("sessionCode", sessionId)/* ,QueryParam.equalsTo("realm", realm) */);

			return instances.stream().map(d -> d.getProcessInstanceId()).findFirst();
		} catch (Exception e) {
			log.warn("No pid found for sessionCode=" + sessionId);
		}
		return Optional.empty();

	}

	public static Optional<Long> getProcessIdByWorkflowBeCode(String realm, String workflowBeCode) {
		// Do pagination here
		QueryContext ctx = new QueryContext(0, 100);
		Collection<NodeStatus> instances = queryService.query("getAllNodeStatuses2", NodeStatusQueryMapper.get(), ctx,
				QueryParam.equalsTo("workflowBeCode", workflowBeCode), QueryParam.equalsTo("realm", realm));
		return instances.stream().map(d -> d.getId()).findFirst();

	}

	public static List<String> getWorkflowBeCodeByWorkflowStage(String realm, String workflowStage) {
		// Do pagination here
		QueryContext ctx = new QueryContext(0, 100);
		Collection<NodeStatus> instances = queryService.query("getAllNodeStatuses2", NodeStatusQueryMapper.get(), ctx,
				QueryParam.equalsTo("workflowStage", workflowStage), QueryParam.equalsTo("realm", realm));
		return instances.stream().map(d -> d.getWorkflowBeCode()).collect(Collectors.toList());

	}

	private static QueryService queryService;
	private static KieServiceConfigurator serviceConfigurator;

	protected static void configureServices() {
		serviceConfigurator = ServiceLoader.load(KieServiceConfigurator.class).iterator().next();

		IdentityProvider identityProvider = new IdentityProvider() {

			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return "";
			}

			@Override
			public List<String> getRoles() {
				// TODO Auto-generated method stub
				return new ArrayList<String>();
			}

			@Override
			public boolean hasRole(String role) {
				// TODO Auto-generated method stub
				return true;
			}
		};

		UserGroupCallback userGroupCallback = new UserGroupCallback() {

			@Override
			public boolean existsUser(String userId) {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public boolean existsGroup(String groupId) {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public List<String> getGroupsForUser(String userId) {
				// TODO Auto-generated method stub
				return new ArrayList<String>();
			}
		};

		serviceConfigurator.configureServices("genny-persistence-jbpm-jpa", identityProvider, userGroupCallback);
		queryService = serviceConfigurator.getQueryService();
		processService = serviceConfigurator.getProcessService();
	}

	public static void init() {
		log.info("Setting up Persistence");

		try {
			emf = Persistence.createEntityManagerFactory("genny-persistence-jbpm-jpa");
			env = EnvironmentFactory.newEnvironment(); // KnowledgeBaseFactory.newEnvironment();
			env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
		} catch (Exception e) {
			log.warn("No persistence enabled, are you running wildfly-rulesservice?");
		}

//		QueryDefinitionEntity qde = new QueryDefinitionEntity();
		configureServices();
		SqlQueryDefinition query = new SqlQueryDefinition("getAllProcessInstances", "java:jboss/datasources/gennyDS");
		query.setExpression("select * from VariableInstanceLog");
		try {
			queryService.registerQuery(query);
		} catch (QueryAlreadyRegisteredException e) {
			log.warn(query.getName() + " is already registered");
		}

		SqlQueryDefinition query3 = new SqlQueryDefinition("getAllSessionPids", "java:jboss/datasources/gennyDS");
		query3.setExpression("select * from session_pid");
		try {
			queryService.registerQuery(query3);
		} catch (QueryAlreadyRegisteredException e) {
			log.warn(query3.getName() + " is already registered");
		}

		SqlQueryDefinition query2 = new SqlQueryDefinition("getAllNodeStatuses2", "java:jboss/datasources/gennyDS");
		query2.setExpression("select  * from nodestatus");

		try {
			queryService.registerQuery(query2);
		} catch (QueryAlreadyRegisteredException e) {
			log.warn(query2.getName() + " is already registered");
		}

		log.info("Finished init");
	}

	public static Optional<Long> getProcessIdBySessionId(String realm, String sessionId) {
		// TODO Auto-generated method stub
		return RulesLoader.getProcessIdBysessionId(realm, sessionId);
	}

	private static Boolean processRule(String realm, RuleDescr rule, Tuple3<String, String, String> ruleTuple) {
		Boolean ret = false;
		String filename = ruleTuple._2;
		String ruleText = ruleTuple._3;
		Pattern p = Pattern.compile("(FRM_[A-Z0-9_-]+|THM_[A-Z0-9_-]+)");

		// If Rule is a theme or Frame
		String ruleName = filename.replaceAll("\\.[^.]*$", "");
		String ruleCode = "RUL_" + ruleName;

		if (ruleCode.startsWith("RUL_FRM_") || ruleCode.startsWith("RUL_THM")) {
			// Parse rule text to identify child rules
			Set<String> children = new HashSet<String>();
			Matcher m = p.matcher(ruleText);
			while (m.find()) {
				String child = m.group();
				children.add(child);

			}
			if (!children.isEmpty()) {
				for (String child : children) {
					FrameUtils2.graphBuilder.connect(ruleName).to(child).withEdge("PARENT");
					FrameUtils2.graphBuilder.connect(child).to(ruleName).withEdge("CHILD");
					log.info("Rule : " + ruleName + " --- child -> " + child);
				}
			}

		}

		if (ruleCode.startsWith("RUL_FRM_")) {
			frameCodes.add(filename.replaceAll("\\.[^.]*$", ""));
			FrameUtils2.ruleFires.put(realm + ":" + filename.replaceAll("\\.[^.]*$", ""), false); // check if actually

		}
		if (ruleCode.startsWith("RUL_THM_")) {
			themeCodes.add(filename.replaceAll("\\.[^.]*$", ""));
			FrameUtils2.ruleFires.put(realm + ":" + filename.replaceAll("\\.[^.]*$", ""), false); // check if actuall
																									// fires
		}

		if (!persistRules) {
			return false;
		}
		// Determine what rules have changed via their hash .... and if so then clear
		// their cache and db entries
		Map<String, String> realmTokenMap = new HashMap<String, String>();
		Map<String, BaseEntityUtils> realmBeUtilsMap = new HashMap<String, BaseEntityUtils>();
		Integer hashcode = ruleText.hashCode();
		if (realmTokenMap.get(realm) == null) {
			JsonObject tokenObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM, "TOKEN" + realm.toUpperCase());
			String token = tokenObj.getString("value");
			realmTokenMap.put(realm, token);
		}
		// get kie type
		String ext = filename.substring(filename.lastIndexOf(".") + 1);
		String kieType = ext.toUpperCase();

		// Get rule filename

		// get existing rule from cache

		BaseEntity existingRuleBe = VertxUtils.readFromDDT(realm, ruleCode, true, realmTokenMap.get(realm));
		Integer existingHashCode = 0;
		if (existingRuleBe != null) {
			existingHashCode = existingRuleBe.getValue("PRI_HASHCODE", -1);
		}

		BaseEntityUtils beUtils = null;
		if (realmBeUtilsMap.get(realm) == null) {
			beUtils = new BaseEntityUtils(new GennyToken(realmTokenMap.get(realm)));
			realmBeUtilsMap.put(realm, beUtils);
		} else {
			beUtils = realmBeUtilsMap.get(realm);
		}
		if (existingRuleBe == null) {
			if ("FRM_QUE_GRP_PLACED_GRP".contentEquals(ruleCode)) {
				log.info("got to here:");
			}
			existingRuleBe = beUtils.create(ruleCode, rule.getName());
		}

		if ((!hashcode.equals(existingHashCode)) && (existingRuleBe != null)) {
			log.info("Hashcode for rule " + realm + ":" + filename + " = " + hashcode + " existing hashcode="
					+ existingHashCode + "  match = " + (hashcode.equals(existingHashCode) ? "TRUE" : "FALSE ****"));

			// If any rules do not match then set the rulesChanged flag
			// but only for theme and frame rules
			if (filename.startsWith("THM_") || filename.startsWith("FRM_")) {
				rulesChanged = true;
			}

			// create the rule Baseentity
			try {
				Attribute hashcodeAttribute = RulesUtils.getAttribute("PRI_HASHCODE", realmTokenMap.get(realm));
				existingRuleBe.setValue(hashcodeAttribute, hashcode);
				Attribute filenameAttribute = RulesUtils.getAttribute("PRI_FILENAME", realmTokenMap.get(realm));
				existingRuleBe.setValue(filenameAttribute, filename);
				existingRuleBe.setValue(RulesUtils.getAttribute("PRI_KIE_TYPE", realmTokenMap.get(realm)), kieType);
				existingRuleBe.setValue(RulesUtils.getAttribute("PRI_KIE_TEXT", realmTokenMap.get(realm)), ruleText);
				existingRuleBe.setValue(RulesUtils.getAttribute("PRI_KIE_NAME", realmTokenMap.get(realm)),
						rule.getName());
				if (rule.getAttributes().get("ruleflow-group") != null) {
					AttributeDescr attD = rule.getAttributes().get("ruleflow-group");
					String ruleflowgroup = attD.getValue();
					existingRuleBe.setValue(RulesUtils.getAttribute("PRI_KIE_RULE_GROUP", realmTokenMap.get(realm)),
							ruleflowgroup);
				}
				if (rule.getAttributes().get("no-loop") != null) {
					AttributeDescr attD = rule.getAttributes().get("no-loop");
					String noloop = attD.getValue();
					existingRuleBe.setValue(RulesUtils.getAttribute("PRI_KIE_RULE_NOLOOP", realmTokenMap.get(realm)),
							noloop);
				}
				if (rule.getAttributes().get("salience") != null) {
					AttributeDescr attD = rule.getAttributes().get("salience");
					String salience = attD.getValue();
					existingRuleBe.setValue(RulesUtils.getAttribute("PRI_KIE_RULE_SALIENCE", realmTokenMap.get(realm)),
							salience);
				}

			} catch (BadDataException e) {
				log.error("Bad data");
			}

			beUtils.saveBaseEntityAttributes(existingRuleBe);
			// now if the rule is a theme or frame rule then clear the cache of the output
			// of those rules, the MSG and ASK
			// the logic is that a rule can skip loading and generating the cached item if
			// it already has a cached item
			if (existingRuleBe.getCode().startsWith("RUL_THM_") || existingRuleBe.getCode().startsWith("RUL_FRM_")
					|| existingRuleBe.getCode().startsWith("SBE_")) {
				VertxUtils.writeCachedJson(realm, existingRuleBe.getCode(), null, realmTokenMap.get(realm));

			}
			ret = true;
		}
		return ret;
	}

	private static Boolean processJbpm(String realm, Tuple3<String, String, String> ruleTuple) {
		Boolean ret = false;
		Map<String, String> realmTokenMap = new HashMap<String, String>();
		Map<String, BaseEntityUtils> realmBeUtilsMap = new HashMap<String, BaseEntityUtils>();
		String name = ruleTuple._2.replaceAll("\\.[^.]*$", "");
		String filename = ruleTuple._2;
		String ruleText = ruleTuple._3;
		Integer hashcode = ruleText.hashCode();
		if (realmTokenMap.get(realm) == null) {
			JsonObject tokenObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM, "TOKEN" + realm.toUpperCase());
			String token = tokenObj.getString("value");
			realmTokenMap.put(realm, token);
		}

		if ("init_project.bpmn".equals(filename)) {
			log.info("DETECTED INIT_PROJECT");
		}

		// get kie type
		String ext = filename.substring(filename.lastIndexOf(".") + 1);
		String kieType = ext.toUpperCase();

		// Get rule filename
		String ruleCode = "BPM_" + filename.replaceAll("\\.[^.]*$", "");

		// get existing rule from cache

		BaseEntity existingRuleBe = VertxUtils.readFromDDT(realm, ruleCode, true, realmTokenMap.get(realm));
		Integer existingHashCode = 0;
		if (existingRuleBe != null) {
			existingHashCode = existingRuleBe.getValue("PRI_HASHCODE", -1);
		}

		BaseEntityUtils beUtils = null;
		if (realmBeUtilsMap.get(realm) == null) {
			beUtils = new BaseEntityUtils(new GennyToken(realmTokenMap.get(realm)));
			realmBeUtilsMap.put(realm, beUtils);
		} else {
			beUtils = realmBeUtilsMap.get(realm);
		}
		if (existingRuleBe == null) {
			existingRuleBe = beUtils.create(ruleCode, name);
		}

		if ((!hashcode.equals(existingHashCode)) && (existingRuleBe != null)) {
			log.info("Hashcode for rule " + realm + ":" + filename + " = " + hashcode + " existing hashcode="
					+ existingHashCode + "  match = " + (hashcode.equals(existingHashCode) ? "TRUE" : "FALSE ****"));

			// create the rule Baseentity
			try {
				Attribute hashcodeAttribute = RulesUtils.getAttribute("PRI_HASHCODE", realmTokenMap.get(realm));
				existingRuleBe.setValue(hashcodeAttribute, hashcode);
				Attribute filenameAttribute = RulesUtils.getAttribute("PRI_FILENAME", realmTokenMap.get(realm));
				existingRuleBe.setValue(filenameAttribute, filename);
				existingRuleBe.setValue(RulesUtils.getAttribute("PRI_KIE_TYPE", realmTokenMap.get(realm)), kieType);
				existingRuleBe.setValue(RulesUtils.getAttribute("PRI_KIE_TEXT", realmTokenMap.get(realm)), ruleText);
				existingRuleBe.setValue(RulesUtils.getAttribute("PRI_KIE_NAME", realmTokenMap.get(realm)), name);
			} catch (ClassCastException ee) {
				log.error("ClassCastException?!? realm=" + realm + " realmTokenMap.get(realm)="
						+ realmTokenMap.get(realm));
			} catch (BadDataException e) {
				log.error("Bad data");
			}

			beUtils.saveBaseEntityAttributes(existingRuleBe);
			ret = true;
		}
		return ret;
	}

	/**
	 * @return the rds
	 */
	public RuntimeDataService getRds() {
		return rds;
	}

	/**
	 * @param rds the rds to set
	 */
	public void setRds(RuntimeDataService rds) {
		this.rds = rds;
	}

	public static OutputParam loadOutputFromTask(GennyToken userToken, Long taskId) {
		OutputParam output = new OutputParam();
		TaskService taskService = taskServiceMap.get(userToken.getSessionCode());
		Map<String, Object> params = new HashMap<String, Object>();
		// Do Task Operations
		if (taskService == null) {
			log.error("TaskService is null");
			return output;
		}

		Task task = taskService.getTaskById(taskId);

		// Look at the task and simply set the output
		TaskData taskData = task.getTaskData();
		String code = task.getFormName();
		output.setFormCode(code, "FRM_CONTENT");
		output.setTypeOfResult("FORMCODE");

		return output;
	}

	static public List<String> returnUninitialisedThemes(String realm) {
		List<String> uninitialisedThemes = new ArrayList<String>();
		JsonObject tokenObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM, "TOKEN" + realm.toUpperCase());
		String sToken = tokenObj.getString("value");
		GennyToken serviceToken = new GennyToken("PER_SERVICE", sToken);

		if ((serviceToken == null) || ("DUMMY".equalsIgnoreCase(serviceToken.getToken()))) {
			log.error("NO SERVICE TOKEN FOR " + realm + " IN CACHE");
			return null; // TODO throw exception
		}

		// Fetch all the uninitilised theme rules from the api
		SearchEntity searchBE = new SearchEntity("SBE_RULES_FIRED", "Have Rules been initialised")
				.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "RUL_THM_%")
				.addFilter("PRI_POJO", SearchEntity.StringFilter.EQUAL, "EMPTY").addColumn("PRI_FILENAME", "Filename")
				.setPageStart(0).setPageSize(4000);

		searchBE.setRealm(serviceToken.getRealm());

		String jsonSearchBE = JsonUtils.toJson(searchBE);
		String resultJson;

		try {
			resultJson = QwandaUtils.apiPostEntity(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/search",
					jsonSearchBE, serviceToken.getToken());
			QDataBaseEntityMessage resultMsg = JsonUtils.fromJson(resultJson, QDataBaseEntityMessage.class);

			if (resultMsg.getItems() != null) {
				for (BaseEntity ruleBe : resultMsg.getItems()) {
					String filename = ruleBe.getValueAsString("PRI_FILENAME");
					log.info("############ RULE THEME  : " + filename + "   HAS NOT BEEN INITIALISED");
					uninitialisedThemes.add(ruleBe.getCode());
				}
			}
		} catch (Exception e) {
			log.error("Could not fetch Rules from API");

		}
		return uninitialisedThemes;
	}

	static public List<String> returnUninitialisedFrames(String realm) {
		List<String> uninitialisedFrames = new ArrayList<String>();
		JsonObject tokenObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM, "TOKEN" + realm.toUpperCase());
		String sToken = tokenObj.getString("value");
		GennyToken serviceToken = new GennyToken("PER_SERVICE", sToken);

		if ((serviceToken == null) || ("DUMMY".equalsIgnoreCase(serviceToken.getToken()))) {
			log.error("NO SERVICE TOKEN FOR " + realm + " IN CACHE");
			return null; // TODO throw exception
		}

		// Fetch all the uninitilised theme rules from the api
		SearchEntity searchBE = new SearchEntity("SBE_RULES_FIRED", "Have Rules been initialised")
				.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "RUL_FRM_%")
				.addFilter("PRI_POJO", SearchEntity.StringFilter.EQUAL, "EMPTY")
				.addFilter("PRI_ASKS", SearchEntity.StringFilter.EQUAL, "EMPTY")
				.addFilter("PRI_MSG", SearchEntity.StringFilter.EQUAL, "EMPTY").addColumn("PRI_FILENAME", "Filename")
				.setPageStart(0).setPageSize(4000);

		searchBE.setRealm(serviceToken.getRealm());

		String jsonSearchBE = JsonUtils.toJson(searchBE);
		String resultJson;

		try {
			resultJson = QwandaUtils.apiPostEntity(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/search",
					jsonSearchBE, serviceToken.getToken());
			QDataBaseEntityMessage resultMsg = JsonUtils.fromJson(resultJson, QDataBaseEntityMessage.class);

			if (resultMsg.getItems() != null) {
				for (BaseEntity ruleBe : resultMsg.getItems()) {
					String filename = ruleBe.getValueAsString("PRI_FILENAME");
					log.info("############ RULE FRAME  : " + filename + "   HAS NOT BEEN INITIALISED");
					uninitialisedFrames.add(ruleBe.getCode());
				}
			}
		} catch (Exception e) {
			log.error("Could not fetch Rules from API");

		}
		return uninitialisedFrames;
	}

}
