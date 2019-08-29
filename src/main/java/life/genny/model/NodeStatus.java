package life.genny.model;

import java.io.Serializable;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

/**
* @author Adam Crow
* @author Rahul Samaranayake
* */

@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)

@Table(name = "nodestatus", 
indexes = {
        @Index(columnList = "nodeId", name =  "code_idx"),
        @Index(columnList = "processId", name =  "code_idx"),
        @Index(columnList = "realm", name = "code_idx")
    }
)

@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class NodeStatus implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	
    @Id
    @GeneratedValue
    private int id;
	
	private String userCode;
	private String nodeName;
	private String nodeId;
	private String status;
	private String realm;
	private Long processInstanceId;
	private String processId;
	private String workflowCode;
	
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
			String processId, String workflowCode) {

		this.userCode = userCode;
		this.nodeName = nodeName;
		this.nodeId = nodeId;
		this.realm = realm;
		this.processInstanceId = processInstanceId;
		this.processId = processId;
		this.workflowCode = workflowCode;
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
	public String getWorkflowCode() {
		return workflowCode;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "NodeStatus [" + (userCode != null ? "userCode=" + userCode + ", " : "")
				+ (nodeId != null ? "nodeId=" + nodeId + ", " : "") + (status != null ? "status=" + status + ", " : "")
				+ (realm != null ? "realm=" + realm + ", " : "") + (processId != null ? "processId=" + processId : "")
				+ (workflowCode != null ? "workflowCode=" + workflowCode : "")
				+ "]";
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
		result = prime * result + ((workflowCode == null) ? 0 : workflowCode.hashCode());
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
		if (workflowCode == null) {
			if (other.workflowCode != null)
				return false;
		} else if (!workflowCode.equals(other.workflowCode))
			return false;
		return true;
	}
	
}
