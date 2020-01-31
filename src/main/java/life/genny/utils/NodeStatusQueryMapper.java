package life.genny.utils;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.dashbuilder.dataset.DataSet;
import org.jbpm.kie.services.impl.query.mapper.AbstractQueryMapper;
import org.jbpm.services.api.query.QueryResultMapper;

import life.genny.model.NodeStatus;

/**
 * Dedicated mapper that transforms DataSet to NodeStatusDesc.
 *
 */
public class NodeStatusQueryMapper extends AbstractQueryMapper<NodeStatus> implements QueryResultMapper<List<NodeStatus>> {
    
    private static final long serialVersionUID = 5935133069234626714L;

    /**
     * Dedicated for ServiceLoader to create instance, use <code>get()</code> method instead 
     */
    public NodeStatusQueryMapper() {
    }
    
    /**
     * Default access to get instance of the mapper
     * @return
     */
    public static NodeStatusQueryMapper get() {
        return new NodeStatusQueryMapper();
    }

    @Override
    public List<NodeStatus> map(Object result) {
        if (result instanceof DataSet) {
            DataSet dataSetResult = (DataSet) result;
            List<NodeStatus> mappedResult = new ArrayList<NodeStatus>();
            
            if (dataSetResult != null) {
                
                for (int i = 0; i < dataSetResult.getRowCount(); i++) {
                    NodeStatus pi = buildInstance(dataSetResult, i);
                    mappedResult.add(pi);
                
                }
            }
            
            return mappedResult;
        }
        
        throw new IllegalArgumentException("Unsupported result for mapping " + result);
    }
    
    protected NodeStatus buildInstance(DataSet dataSetResult, int index) {
        NodeStatus pi = new life.genny.model.NodeStatus(
                getColumnLongValue(dataSetResult, "id", index),
                getColumnDateValue(dataSetResult, "date", index),
                getColumnStringValue(dataSetResult, "nodeId", index),
                getColumnStringValue(dataSetResult, "nodeName", index),
                getColumnStringValue(dataSetResult, "processId", index),
                getColumnLongValue(dataSetResult, "processInstanceId", index),
                getColumnStringValue(dataSetResult, "realm", index),
                getColumnStringValue(dataSetResult, "status", index),
                getColumnStringValue(dataSetResult, "userCode", index),
                getColumnStringValue(dataSetResult, "workflowStage", index), 
                getColumnStringValue(dataSetResult, "workflowBeCode", index)
                );

         return pi;
    }

    @Override
    public String getName() {
        return "NodeStatus";
    }

    @Override
    public Class<?> getType() {
        return NodeStatus.class;
    }

    @Override
    public QueryResultMapper<List<NodeStatus>> forColumnMapping(Map<String, String> columnMapping) {
        return new NodeStatusQueryMapper();
    }

}
