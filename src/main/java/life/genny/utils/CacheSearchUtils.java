package life.genny.utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import life.genny.jbpm.customworkitemhandlers.ShowFrame;
import life.genny.models.Frame3;
import life.genny.models.GennyToken;
import life.genny.models.TableData;
import life.genny.models.Theme;
import life.genny.models.ThemeAttribute;
import life.genny.qwanda.Answer;
import life.genny.qwanda.Ask;
import life.genny.qwanda.Context;
import life.genny.qwanda.ContextList;
import life.genny.qwanda.ContextType;
import life.genny.qwanda.Question;
import life.genny.qwanda.VisualControlType;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.validation.Validation;
import life.genny.qwanda.validation.ValidationList;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;

public class CacheSearchUtils extends TableUtils {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


	BaseEntityUtils beUtils = null;
	
	
	

	public CacheSearchUtils(BaseEntityUtils beUtils) {
		super(beUtils);
	}


	static public long searchTable(BaseEntityUtils beUtils, SearchEntity searchBE, Boolean cache) {
		long starttime = System.currentTimeMillis();
		
			// Strip the searchBE of all attributes (so we just get the baseentity code
		
		Set<EntityAttribute> nonColumnAttributes = searchBE.getBaseEntityAttributes()
				  .stream()
				  .filter(c -> !c.getAttributeCode().startsWith("C"))
				  .collect(Collectors.toSet());
		
		searchBE.setBaseEntityAttributes(nonColumnAttributes);
		
		long s1time = System.currentTimeMillis();
		/* get current search */
		TableUtils tableUtils = new CacheSearchUtils(beUtils);

		long s2time = System.currentTimeMillis();

		ExecutorService WORKER_THREAD_POOL = Executors.newFixedThreadPool(10);
		CompletionService<QBulkMessage> service = new ExecutorCompletionService<>(WORKER_THREAD_POOL);

		TableFrameCallable tfc = new TableFrameCallable(beUtils, cache);
		SearchCallable sc = new SearchCallable(tableUtils, searchBE, beUtils, cache);

		List<Callable<QBulkMessage>> callables = Arrays.asList(tfc, sc);

		QBulkMessage aggregatedMessages = new QBulkMessage();

		long startProcessingTime = System.currentTimeMillis();
		long totalProcessingTime;

		if (GennySettings.useConcurrencyMsgs) {
			for (Callable<QBulkMessage> callable : callables) {
				service.submit(callable);
			}
			try {
				Future<QBulkMessage> future = service.take();
				QBulkMessage firstThreadResponse = future.get();
				aggregatedMessages.add(firstThreadResponse);
				totalProcessingTime = System.currentTimeMillis() - startProcessingTime;

				/*
				 * assertTrue("First response should be from the fast thread",
				 * "fast thread".equals(firstThreadResponse.getData_type()));
				 * assertTrue(totalProcessingTime >= 100 && totalProcessingTime < 1000);
				 */
				System.out.println("Thread finished after: " + totalProcessingTime + " milliseconds");

				future = service.take();
				QBulkMessage secondThreadResponse = future.get();
				aggregatedMessages.add(secondThreadResponse);
				System.out.println("2nd Thread finished after: " + totalProcessingTime + " milliseconds");
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}

			WORKER_THREAD_POOL.shutdown();
			try {
				if (!WORKER_THREAD_POOL.awaitTermination(90, TimeUnit.SECONDS)) {
					WORKER_THREAD_POOL.shutdownNow();
				}
			} catch (InterruptedException ex) {
				WORKER_THREAD_POOL.shutdownNow();
				Thread.currentThread().interrupt();
			}
		} else {
			aggregatedMessages.add(tfc.call());
			aggregatedMessages.add(sc.call());

		}
		totalProcessingTime = System.currentTimeMillis() - startProcessingTime;
		System.out.println("All threads finished after: " + totalProcessingTime + " milliseconds");
		aggregatedMessages.setToken(beUtils.getGennyToken().getToken());

		if (cache) {
			System.out.println("Cache is enabled ! Sending Qbulk message with QDataBaseEntityMessage and QDataAskMessage !!!");
			String json = JsonUtils.toJson(aggregatedMessages);
			VertxUtils.writeMsg("webcmds", json);
		}

		/* update(output); */
		long endtime = System.currentTimeMillis();
		System.out.println("init setup took " + (s1time - starttime) + " ms");
		System.out.println("search session setup took " + (s2time - s1time) + " ms");
		return (endtime - starttime);
	}
	
}
