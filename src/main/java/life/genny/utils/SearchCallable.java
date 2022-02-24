package life.genny.utils;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import life.genny.qwanda.datatype.CapabilityMode;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;

public class SearchCallable implements Callable<QBulkMessage> {

    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    QBulkMessage ret = new QBulkMessage();
    TableUtils tableUtils;
    private SearchEntity searchBE;
    private BaseEntityUtils beUtils;
    Boolean cache = false;
    String filterCode = null;
    String filterValue = null;
    Boolean replace = true;

    public SearchCallable(TableUtils tableUtils, SearchEntity searchBE, BaseEntityUtils beUtils) {
        this(tableUtils, searchBE, beUtils, false);
    }

    public SearchCallable(TableUtils tableUtils, SearchEntity searchBE, BaseEntityUtils beUtils, String filterCode,
            String filterValue) {
        this(tableUtils, searchBE, beUtils, false, filterCode, filterValue);
    }

    public SearchCallable(TableUtils tableUtils, SearchEntity searchBE, BaseEntityUtils beUtils, Boolean cache) {
        this.tableUtils = tableUtils;
        this.beUtils = beUtils;
        this.searchBE = searchBE;
        this.cache = cache;
    }

    public SearchCallable(TableUtils tableUtils, SearchEntity searchBE, BaseEntityUtils beUtils, Boolean cache,
            String filterCode, String filterValue) {
        this.tableUtils = tableUtils;
        this.beUtils = beUtils;
        this.searchBE = searchBE;
        this.cache = cache;
        this.filterCode = filterCode;
        this.filterValue = filterValue;
    }

    public SearchCallable(TableUtils tableUtils, SearchEntity searchBE, BaseEntityUtils beUtils, Boolean cache,
            String filterCode, String filterValue, Boolean replace) {
        this.tableUtils = tableUtils;
        this.beUtils = beUtils;
        this.searchBE = searchBE;
        this.cache = cache;
        this.filterCode = filterCode;
        this.filterValue = filterValue;
        this.replace = replace;
    }

    public QBulkMessage call() {

        log.info("Starting Search! " + searchBE.getCode());

        QBulkMessage qbm1 = null;
        Boolean noCachePresent = true;
        Boolean usingCache = searchBE.is("SCH_CACHABLE");      
        Integer pageStart = -1;
        
        // Check if a filter is being used
        final String templateSearchCode  = searchBE.getCode().replaceFirst("_"+beUtils.getGennyToken().getSessionCode().toUpperCase(), "");
        
        CapabilityUtils capabilityUtils = new CapabilityUtils(beUtils);
        Boolean isAllowedToUseCache = capabilityUtils.hasCapability("USE_CACHE", CapabilityMode.VIEW);
        
        String wildcard = searchBE.getValueAsString("SCH_WILDCARD");
        if (!StringUtils.isBlank(wildcard) || !isAllowedToUseCache) {
        	usingCache = false;
        }
        
        usingCache = false;
        noCachePresent = true;
        if (usingCache) {
        	 
        	  pageStart = searchBE.getValue("SCH_PAGE_START",0);
        	  log.info("Fetching Table Search from Cache with pageStart = "+pageStart);
        	  if (pageStart == 0) {
        		  // only do caching if the searchsession matches the original
         		  qbm1 = VertxUtils.getObject(beUtils.getGennyToken().getRealm(), "SPEEDUP", templateSearchCode,
                     QBulkMessage.class);
         		  
        		  if (qbm1==null) {
        			  noCachePresent = true;
        		  }else {
       		
        			  if (qbm1.getMessages()[0].getParentCode()!=null) {
        				  qbm1.getMessages()[0].setParentCode(templateSearchCode+"_"+beUtils.getGennyToken().getSessionCode().toUpperCase());  // fix to current session
        			  } else {
        				 Arrays.stream(qbm1.getMessages()[0].getItems()).forEach(i -> i.setCode(templateSearchCode+"_"+beUtils.getGennyToken().getSessionCode().toUpperCase()));        				  
        			  }
        			  if (qbm1.getMessages().length>0) {
               			  if (qbm1.getMessages()[1].getParentCode()!=null) {
            				  qbm1.getMessages()[1].setParentCode(templateSearchCode+"_"+beUtils.getGennyToken().getSessionCode().toUpperCase());  // fix to current session
            			  } else {
             				 Arrays.stream(qbm1.getMessages()[1].getItems()).forEach(i -> i.setCode(templateSearchCode+"_"+beUtils.getGennyToken().getSessionCode().toUpperCase()));        				  
           			  }
             			  }
        			  qbm1.setToken(beUtils.getGennyToken().getToken());  // update with latest user token
        			  noCachePresent = false;
        		  }
        	  }
        }

        if (noCachePresent) {
            qbm1 = tableUtils.performSearch(searchBE, null, filterCode, filterValue, cache,
                    replace);
            if ((pageStart == 0)&&usingCache) {
            	VertxUtils.putObject(beUtils.getGennyToken().getRealm(), "SPEEDUP", templateSearchCode, qbm1,
                    beUtils.getGennyToken().getToken());
            }
        } 
        ret.add(qbm1);

        log.info("Finished "+(usingCache?"Using cache ":"with no caching ")+" - Search with " + qbm1.getMessages().length + " items ");
        return ret;
    }
}
