package life.genny.utils;

import java.io.Serializable;

import life.genny.models.GennyToken;

public class CallingProcessToken implements Serializable {
	GennyToken gennyToken;
	Long processId;
	String code;
	/**
	 * @param gennyToken
	 * @param processId
	 * @param code
	 */
	public CallingProcessToken(GennyToken gennyToken, Long processId, String code) {
		this.gennyToken = gennyToken;
		this.processId = processId;
		this.code = code;
	}
	@Override
	public String toString() {
		return "CallingProcessToken [gennyToken=" + gennyToken + ", processId=" + processId + ", code=" + code + "]";
	}
	/**
	 * @return the gennyToken
	 */
	public GennyToken getGennyToken() {
		return gennyToken;
	}
	/**
	 * @param gennyToken the gennyToken to set
	 */
	public void setGennyToken(GennyToken gennyToken) {
		this.gennyToken = gennyToken;
	}
	/**
	 * @return the processId
	 */
	public Long getProcessId() {
		return processId;
	}
	/**
	 * @param processId the processId to set
	 */
	public void setProcessId(Long processId) {
		this.processId = processId;
	}
	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}
	/**
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}
	
	
	
}
