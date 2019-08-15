package life.genny.utils;

import java.io.Serializable;


public class OutputParam implements Serializable{
	  /**
	 * This class is used by RuleFlowGroupWorkItemHandler to store the output result of the Rule Task.
	 */
	private static final long serialVersionUID = 1L;
	private Object result;
	private String typeOfResult="NONE";  // FRAME_CODE or LIFECYCLE
	private String resultCode = null;
	private String targetCode = null;


	/**
	 * @return the resultCode
	 */
	public String getResultCode() {
		return resultCode;
	}

	/**
	 * @return the targetCode
	 */
	public String getTargetCode() {
		return targetCode;
	}

	/**
	 * @param targetCode the targetCode to set
	 */
	public void setTargetCode(String targetCode) {
		this.targetCode = targetCode;
	}

	/**
	 * @param resultCode the resultCode to set
	 */
	public void setResultCode(String resultCode) {
		this.resultCode = resultCode;
	}

	/**
	 * @return the typeOfResult
	 */
	public String getTypeOfResult() {
		return typeOfResult;
	}

	/**
	 * @param typeOfResult the typeOfResult to set
	 */
	public void setTypeOfResult(String typeOfResult) {
		this.typeOfResult = typeOfResult;
	}

	public Object getResult() {
		return result;
	}
	
	public void setResult(Object result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return "OutputParam [typeOfResult=" + typeOfResult + ", resultCode=" + resultCode + "]";
	}
	 
	
}