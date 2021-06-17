package life.genny.rules.listeners;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.AgendaGroupPoppedEvent;
import org.kie.api.event.rule.AgendaGroupPushedEvent;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.MatchCancelledEvent;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.event.rule.RuleFlowGroupActivatedEvent;
import org.kie.api.event.rule.RuleFlowGroupDeactivatedEvent;

public class GennyRuleTimingListener implements  AgendaEventListener {
	
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


	static Map<String,Long> ruleStartTime = new ConcurrentHashMap<String,Long>();
	static Map<String,Long> ruleTotalTime = new ConcurrentHashMap<String,Long>();
	static Map<String,Long> ruleAverageTime = new ConcurrentHashMap<String,Long>();
	static Map<String,Long> ruleCount = new ConcurrentHashMap<String,Long>();
	
	
	static public void showLogs(String filter)
	{
		showAverageLog(filter);
		showTotalLog(filter);
		showCountLog(filter);
	}
	
	static public void clearLogs()
	{
		ruleStartTime = new ConcurrentHashMap<String,Long>();
		ruleTotalTime = new ConcurrentHashMap<String,Long>();
		ruleAverageTime = new ConcurrentHashMap<String,Long>();
		ruleCount = new ConcurrentHashMap<String,Long>();

	}
	
	static public void showAverageLog(String filter)
	{
		// sort by by Average time decreasing
		
		Map<String, Long> sortedMap = ruleAverageTime.entrySet().stream()
                .sorted(Collections.reverseOrder(Entry.comparingByValue()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		
		log.info("**********************************************");
		log.info("Rule Average Timing Table\n");
		for (String key : sortedMap.keySet()) {
			if (filter.startsWith("count:")) {
				String[] str = filter.split(":");
				Long count = Long.parseLong(str[1]);
				Long actualcount = ruleCount.get(key);
				if (actualcount >= count) {
					log.info(String.format("%50s %8d ms",key,sortedMap.get(key)));
				}
			} else {
				log.info(String.format("%50s %8d ms",key,sortedMap.get(key)));
			}
		}
	}

	static public void showTotalLog(String filter)
	{
		// sort by by Average time decreasing
		
		Map<String, Long> sortedMap = ruleTotalTime.entrySet().stream()
                .sorted(Collections.reverseOrder(Entry.comparingByValue()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		log.info("**********************************************");		
		log.info("Rule Total Timing Table\n");
		for (String key : sortedMap.keySet()) {
			if (filter.startsWith("count:")) {
				String[] str = filter.split(":");
				Long count = Long.parseLong(str[1]);
				Long actualcount = ruleCount.get(key);
				if (actualcount >= count) {
					log.info(String.format("%50s %8d ms",key,sortedMap.get(key)));
				}
			} else {
				log.info(String.format("%50s %8d ms",key,sortedMap.get(key)));
			}
		}
	}
	
	static public void showCountLog(String filter)
	{
		// sort by by Average time decreasing
		
		Map<String, Long> sortedMap = ruleCount.entrySet().stream()
                .sorted(Collections.reverseOrder(Entry.comparingByValue()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		log.info("**********************************************");		
		log.info("Rule Count Table\n");
		for (String key : sortedMap.keySet()) {
			if (filter.startsWith("count:")) {
				String[] str = filter.split(":");
				Long count = Long.parseLong(str[1]);
				Long actualcount = ruleCount.get(key);
				if (actualcount >= count) {
					log.info(String.format("%50s %8d ",key,sortedMap.get(key)));
				}
			} else {
				log.info(String.format("%50s %8d ",key,sortedMap.get(key)));
			}
		}
	}
	
	@Override
	public void matchCreated(MatchCreatedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void matchCancelled(MatchCancelledEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeMatchFired(BeforeMatchFiredEvent event) {
		ruleStartTime.put(event.getMatch().getRule().getName(), System.nanoTime());
		
	}

	@Override
	public void afterMatchFired(AfterMatchFiredEvent event) {
		Long difftime = (System.nanoTime() - ruleStartTime.get(event.getMatch().getRule().getName()))/1000000L;
		Long updatedTime = (ruleTotalTime.get(event.getMatch().getRule().getName())==null?0L:ruleTotalTime.get(event.getMatch().getRule().getName())) + difftime;
		ruleTotalTime.put(event.getMatch().getRule().getName(),updatedTime );
		Long count = (ruleCount.get(event.getMatch().getRule().getName())==null?0L:ruleCount.get(event.getMatch().getRule().getName())) + 1;
		ruleCount.put(event.getMatch().getRule().getName(),count );
		Long average = updatedTime / count;  // will be no zero
		ruleAverageTime.put(event.getMatch().getRule().getName(),average );
	}

	@Override
	public void agendaGroupPopped(AgendaGroupPoppedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void agendaGroupPushed(AgendaGroupPushedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
		// TODO Auto-generated method stub
		
	}

}
