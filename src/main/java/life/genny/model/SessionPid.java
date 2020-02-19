package life.genny.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
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
* */

@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)

@Table(name = "session_pid", indexes = {@Index(name = "IDX_NStat_SessionCode", columnList = "sessionCode")
		})
//@SequenceGenerator(name="sessionPidIdSeq", sequenceName="SESSION_PID_ID_SEQ", allocationSize=1)


@Entity
public class SessionPid implements Serializable/*, AuditEvent, org.kie.api.runtime.manager.audit.NodeInstanceLog */ {
	
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private long id;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "log_date")
    private Date date;
	
    @Expose
    @Column(name = "sessioncode")
	private String sessionCode;
    @Expose
 	private String realm;
  
    @Expose
    @Column(name = "pid")
	private Long processInstanceId;
 	
	private SessionPid()
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
	
	public SessionPid(long id,Date date,String realm,String sessionCode,Long processInstanceId) {

		this.id = id;
		this.date = date;
		this.sessionCode = sessionCode;
		this.processInstanceId = processInstanceId;
		this.realm = realm;
	}
	
	public SessionPid(String realm,String sessionCode,Long processInstanceId) {

		this.date = new Date();
		this.sessionCode = sessionCode;
		this.processInstanceId = processInstanceId;
		this.realm = realm;
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @param date the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * @return the sessionCode
	 */
	public String getSessionCode() {
		return sessionCode;
	}

	/**
	 * @param sessionCode the sessionCode to set
	 */
	public void setSessionCode(String sessionCode) {
		this.sessionCode = sessionCode;
	}

	/**
	 * @return the realm
	 */
	public String getRealm() {
		return realm;
	}

	/**
	 * @param realm the realm to set
	 */
	public void setRealm(String realm) {
		this.realm = realm;
	}

	/**
	 * @return the processInstanceId
	 */
	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	/**
	 * @param processInstanceId the processInstanceId to set
	 */
	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((processInstanceId == null) ? 0 : processInstanceId.hashCode());
		result = prime * result + ((realm == null) ? 0 : realm.hashCode());
		result = prime * result + ((sessionCode == null) ? 0 : sessionCode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SessionPid other = (SessionPid) obj;
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
		if (sessionCode == null) {
			if (other.sessionCode != null)
				return false;
		} else if (!sessionCode.equals(other.sessionCode))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SessionPid [" + (date != null ? "date=" + date + ", " : "")
				+ (sessionCode != null ? "sessionCode=" + sessionCode + ", " : "")
				+ (realm != null ? "realm=" + realm + ", " : "")
				+ (processInstanceId != null ? "processInstanceId=" + processInstanceId : "") + "]";
	}


	
}
