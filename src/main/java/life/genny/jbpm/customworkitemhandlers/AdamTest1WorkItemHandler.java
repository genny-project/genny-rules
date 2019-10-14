package life.genny.jbpm.customworkitemhandlers;


import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import org.jbpm.process.workitem.core.AbstractLogOrThrowWorkItemHandler;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.runtime.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AdamTest1WorkItemHandler extends AbstractLogOrThrowWorkItemHandler implements Cacheable {

 
    private static Logger logger = LoggerFactory.getLogger(AdamTest1WorkItemHandler.class);

     private KieSession ksession;
    private ClassLoader classLoader;
  

    /**
     * Default constructor -  ksession
     */
    public AdamTest1WorkItemHandler() {
        this.ksession = null;
    }

    /**
     * Used when no authentication is required
     * @param ksession - kie session
     */
    public AdamTest1WorkItemHandler(KieSession ksession) {
        this(ksession, null);
    }

  

    /**
     * Used when no authentication is required
     * @param ksession - kie session
     * @param classloader - classloader to use
     */
    public AdamTest1WorkItemHandler(KieSession ksession,
                                     ClassLoader classloader) {
        this.ksession = ksession;
        this.classLoader = classloader;
        System.out.println("AdamTest1WIH Constructor");
    }

 

    
    /**
     * Used when no authentication is required
     * @param handlingProcessId - process id to handle exception
     * @param handlingStrategy - strategy to be applied after handling exception process is completed
     * @param ksession - kie session
     */
    public AdamTest1WorkItemHandler(String handlingProcessId,
                                     String handlingStrategy,
                                     KieSession ksession) {
        this(ksession, null);
        this.handlingProcessId = handlingProcessId;
        this.handlingStrategy = handlingStrategy;
    }

 

    /**
     * Used when no authentication is required
     * @param handlingProcessId - process id to handle exception
     * @param handlingStrategy - strategy to be applied after handling exception process is completed
     * @param ksession - kie session
     * @param classloader - classloader to use
     */
    public AdamTest1WorkItemHandler(String handlingProcessId,
                                     String handlingStrategy,
                                     KieSession ksession,
                                     ClassLoader classloader) {
        this(ksession, classloader);
        this.handlingProcessId = handlingProcessId;
        this.handlingStrategy = handlingStrategy;
    }



    public void executeWorkItem(WorkItem workItem,
                                final WorkItemManager manager) {

        System.out.println("AdamTest1WIH entering executeWorkItem");

        // since JaxWsDynamicClientFactory will change the TCCL we need to restore it after creating client
        ClassLoader origClassloader = Thread.currentThread().getContextClassLoader();

        Object[] parameters = null;
        String interfaceRef = (String) workItem.getParameter("Interface");
        String operationRef = (String) workItem.getParameter("Operation");
        String endpointAddress = (String) workItem.getParameter("Endpoint");
        if (workItem.getParameter("Parameter") instanceof Object[]) {
            parameters = (Object[]) workItem.getParameter("Parameter");
        } else if (workItem.getParameter("Parameter") != null && workItem.getParameter("Parameter").getClass().isArray()) {
            int length = Array.getLength(workItem.getParameter("Parameter"));
            parameters = new Object[length];
            for (int i = 0; i < length; i++) {
                parameters[i] = Array.get(workItem.getParameter("Parameter"),
                                          i);
            }
        } else {
            parameters = new Object[]{workItem.getParameter("Parameter")};
        }

        String modeParam = (String) workItem.getParameter("Mode");

        try {
 
                    Object[] result = null;

                    Map<String, Object> output = new HashMap<String, Object>();

                    if (result == null || result.length == 0) {
                        output.put("Result",
                                   null);
                    } else {
                        output.put("Result",
                                   result[0]);
                    }
                    logger.debug("Received sync response {} completeing work item {}",
                                 result,
                                 workItem.getId());
                    manager.completeWorkItem(workItem.getId(),
                                             output);
     
        } catch (Exception e) {
            handleException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(origClassloader);
        }
    }

 
    public void abortWorkItem(WorkItem workItem,
                              WorkItemManager manager) {
        // Do nothing, cannot be aborted
    }

    private ClassLoader getInternalClassLoader() {
        if (this.classLoader != null) {
            return this.classLoader;
        }

        return Thread.currentThread().getContextClassLoader();
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    protected String nonNull(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

 

}