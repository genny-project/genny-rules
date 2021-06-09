package life.genny.utils;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dashbuilder.dataset.DataSet;
import org.jbpm.kie.services.impl.query.mapper.AbstractQueryMapper;
import org.jbpm.services.api.query.QueryResultMapper;

import life.genny.model.SessionPid;

/**
 * Dedicated mapper that transforms DataSet to NodeStatusDesc.
 *
 */
public class SessionPidQueryMapper extends AbstractQueryMapper<SessionPid> implements QueryResultMapper<List<SessionPid>> {
    
    private static final long serialVersionUID = 5935133069234626714L;

    /**
     * Dedicated for ServiceLoader to create instance, use <code>get()</code> method instead 
     */
    public SessionPidQueryMapper() {
    }
    
    /**
     * Default access to get instance of the mapper
     * @return
     */
    public static SessionPidQueryMapper get() {
        return new SessionPidQueryMapper();
    }

    @Override
    public List<SessionPid> map(Object result) {
        if (result instanceof DataSet) {
            DataSet dataSetResult = (DataSet) result;
            List<SessionPid> mappedResult = new ArrayList<SessionPid>();
            
            if (dataSetResult != null) {
                
                for (int i = 0; i < dataSetResult.getRowCount(); i++) {
                    SessionPid pi = buildInstance(dataSetResult, i);
                    mappedResult.add(pi);
                
                }
            }
            
            return mappedResult;
        }
        
        throw new IllegalArgumentException("Unsupported result for mapping " + result);
    }
    
    protected SessionPid buildInstance(DataSet dataSetResult, int index) {
        SessionPid pi = new life.genny.model.SessionPid(
        		getColumnStringValue(dataSetResult, "usercode", index),
                getColumnLongValue(dataSetResult, "id", index),
                getColumnDateValue(dataSetResult, "log_date", index),
                getColumnStringValue(dataSetResult, "realm", index),
                getColumnStringValue(dataSetResult, "sessioncode", index),              
                getColumnLongValue(dataSetResult, "pid", index)
                
                 );

         return pi;
    }

    @Override
    public String getName() {
        return "SessionPid";
    }

    @Override
    public Class<?> getType() {
        return SessionPid.class;
    }

    @Override
    public QueryResultMapper<List<SessionPid>> forColumnMapping(Map<String, String> columnMapping) {
        return new SessionPidQueryMapper();
    }

}
