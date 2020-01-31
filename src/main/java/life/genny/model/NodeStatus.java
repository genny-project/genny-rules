package life.genny.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.jbpm.process.audit.event.AuditEvent;

import com.google.gson.annotations.Expose;

/**
* @author Adam Crow
* @author Rahul Samaranayake
* */

@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)

@Table(name = "nodestatus", indexes = {@Index(name = "IDX_NStat_nodeId", columnList = "nodeId"),
        @Index(name = "IDX_NStat_realm", columnList = "realm"),
        @Index(name = "IDX_NStat_wfs", columnList = "workflowStage"),
        @Index(name = "IDX_NStat_wfc", columnList = "workflowBeCode")})
//@SequenceGenerator(name="nodeStatusIdSeq", sequenceName="NODE_STATUS_LOG_ID_SEQ", allocationSize=1)


@Entity
public class NodeStatus implements Serializable, AuditEvent, org.kie.api.runtime.manager.audit.NodeInstanceLog {
	
	/**
	 * 
	 */
	
	private static final long serialVersionUID = 910l;
	
    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "log_date")
    private Date date;
	
    @Expose
	private String userCode;
    @Expose
	private String nodeName;
    @Expose
	private String nodeId;
    @Expose
	private String status;
    @Expose
	private String realm;
    @Expose
	private Long processInstanceId;
    @Expose
	private String processId;
    @Expose
    @Column(nullable=true)
	private String workflowStage;
    @Expose
    @Column(nullable=true)
	private String workflowBeCode;
    
	
	private NodeStatus()
	{
		// used by hibernate
	}
	
	/**
	 * @param userCode
	 * @param nodeName
	 * @param nodeId
	 * @param realm
	 * @param processInstanceId
	 * @param processId
	 */
	
	public NodeStatus(String userCode, String nodeName, String nodeId, String realm, Long processInstanceId,
			String processId, String workflowStage, String workflowBeCode) {

		this.id = processInstanceId;
		this.userCode = userCode;
		this.nodeName = nodeName;
		this.nodeId = nodeId;
		this.realm = realm;
		this.processInstanceId = processInstanceId;
		this.processId = processId;
		this.workflowStage = workflowStage;
		this.workflowBeCode = workflowBeCode;
	}
	
	public NodeStatus(Long id, 
    	      Date date,
    	      String nodeId,
    	      String nodeName,
    	      String processId,
    	      Long processInstanceId,
    	      String realm,
    	      String status,
    	      String userCode,
    	      String workflowStage,
    	      String workflowBeCode
    	      ) {

		
		this.id = id;
		this.date = date;
		this.nodeId = nodeId;
		this.nodeName = nodeName;
		this.processId = processId;
		this.processInstanceId = processInstanceId;
		this.realm = realm;
		this.status = status;
		this.userCode = userCode;
		this.workflowStage = workflowStage;
		this.workflowBeCode = workflowBeCode;
	}
	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the userCode
	 */
	public String getUserCode() {
		return userCode;
	}

	/**
	 * @return the nodeName
	 */
	public String getNodeName() {
		return nodeName;
	}

	/**
	 * @return the nodeId
	 */
	public String getNodeId() {
		return nodeId;
	}

	/**
	 * @return the realm
	 */
	public String getRealm() {
		return realm;
	}

	/**
	 * @return the processInstanceId
	 */
	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	/**
	 * @return the processId
	 */
	public String getProcessId() {
		return processId;
	}

	/**
	 * @return the workflowCode
	 */
	public String getWorkflowStage() {
		return workflowStage;
	}
	
	/**
	 * @return the workflowCode
	 */
	public void setWorkflowStage(String workFlowStage) {
		this.workflowStage = workFlowStage;
	}
	
	/**
	 * @return the workflowBeCode
	 */
	public String getWorkflowBeCode() {
		return workflowBeCode;
	}
	
	/**
	 * @return the workflowBeCode
	 */
	public void setWorkflowBeCode(String workflowBeCode) {
		this.workflowBeCode = workflowBeCode;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "NodeStatus [" + (userCode != null ? "userCode=" + userCode + ", " : "")
				+ (nodeId != null ? "nodeId=" + nodeId + ", " : "") + (status != null ? "status=" + status + ", " : "")
				+ (realm != null ? "realm=" + realm + ", " : "") + (processId != null ? "processId=" + processId : "")
				+ (workflowStage != null ? "workflowStage=" + workflowStage : "")
				+ (workflowBeCode != null ? "workflowBeCode=" + workflowBeCode : "")
				+ "]";
	}

	
	


	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		result = prime * result + ((processId == null) ? 0 : processId.hashCode());
		result = prime * result + ((processInstanceId == null) ? 0 : processInstanceId.hashCode());
		result = prime * result + ((realm == null) ? 0 : realm.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((userCode == null) ? 0 : userCode.hashCode());
		result = prime * result + ((workflowStage == null) ? 0 : workflowStage.hashCode());
		result = prime * result + ((workflowBeCode == null) ? 0 : workflowBeCode.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeStatus other = (NodeStatus) obj;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		if (processId == null) {
			if (other.processId != null)
				return false;
		} else if (!processId.equals(other.processId))
			return false;
		if (processInstanceId == null) {
			if (other.processInstanceId != null)
				return false;
		} else if (!processInstanceId.equals(other.processInstanceId))
			return false;
		if (realm == null) {
			if (other.realm != null)
				return false;
		} else if (!realm.equals(other.realm))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (userCode == null) {
			if (other.userCode != null)
				return false;
		} else if (!userCode.equals(other.userCode))
			return false;
		if (workflowStage == null) {
			if (other.workflowStage != null)
				return false;
		} else if (!workflowStage.equals(other.workflowStage))
			return false;
		if (workflowBeCode == null) {
			if (other.workflowBeCode != null)
				return false;
		} else if (!workflowBeCode.equals(other.workflowBeCode))
			return false;
		return true;
	}



	@Override
	public String getExternalId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNodeInstanceId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getWorkItemId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getConnection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNodeType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getType() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
