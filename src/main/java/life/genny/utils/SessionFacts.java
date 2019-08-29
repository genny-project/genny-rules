package life.genny.utils;
import java.io.Serializable;

import life.genny.models.GennyToken;


public class SessionFacts implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	GennyToken serviceToken;
	GennyToken userToken;
	Object message;
	/**
	 * @param gennyToken
	 * @param userToken
	 * @param message
	 */
	public SessionFacts(GennyToken serviceToken, GennyToken userToken, Object message) {
		this.serviceToken = serviceToken;
		this.userToken = userToken;
		this.message = message;
	}
	@Override
	public String toString() {
		return "SessionFacts [serviceToken=" + serviceToken + ", userToken=" + userToken + ", message=" + message + "]";
	}
	/**
	 * @return the serviceToken
	 */
	public GennyToken getServiceToken() {
		return serviceToken;
	}
	/**
	 * @param serviceToken the serviceToken to set
	 */
	public void setServiceToken(GennyToken serviceToken) {
		this.serviceToken = serviceToken;
	}
	/**
	 * @return the userToken
	 */
	public GennyToken getUserToken() {
		return userToken;
	}
	/**
	 * @param userToken the userToken to set
	 */
	public void setUserToken(GennyToken userToken) {
		this.userToken = userToken;
	}
	/**
	 * @return the message
	 */
	public Object getMessage() {
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(Object message) {
		this.message = message;
	}
	

}
