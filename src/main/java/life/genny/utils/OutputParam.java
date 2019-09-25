package life.genny.utils;

import java.io.Serializable;


public class OutputParam implements Serializable,Comparable {
	  /**
	 * This class is used by RuleFlowGroupWorkItemHandler to store the output result of the Rule Task.
	 */
	private static final long serialVersionUID = 1L;
	private Object result;
	private Integer level=0;

	private String typeOfResult="ERROR";  // FRAME_CODE or LIFECYCLE
	private String resultCode = null;
	private String targetCode = null;

	public OutputParam()
	{
		
	}
	
	public OutputParam(final String typeOfResult, final String resultCode, final String targetCode)
	{
		this.resultCode = resultCode;
		this.typeOfResult = typeOfResult;
		this.targetCode = targetCode;
		this.level = 0;
	}
	
	public OutputParam(final String typeOfResult, final String resultCode)
	{
		this.resultCode = resultCode;
		this.typeOfResult = typeOfResult;
		this.level = 0;
	}
	
	public void setFormCode( final String formCode, final String targetCode) {
		this.resultCode = formCode;
		this.typeOfResult = "FORMCODE";
		this.targetCode = targetCode;
		this.level = 0;
	}
	
	public void setLifeCycle( final String workflowId, final String targetCode) {
		this.resultCode = workflowId;
		this.typeOfResult = "LIFECYCLE";
		this.targetCode = targetCode;
		this.level = 0;
	}
	

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

	
	
	/**
	 * @return the level
	 */
	public Integer getLevel() {
		return level;
	}

	/**
	 * @param level the level to set
	 */
	public void setLevel(Integer level) {
		this.level = level;
	}

	@Override
	public String toString() {
		return "OutputParam [typeOfResult=" + typeOfResult + ", resultCode=" + resultCode + ":target="+this.getTargetCode()+": lvl="+level+"]";
	}







	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((level == null) ? 0 : level.hashCode());
		result = prime * result + ((resultCode == null) ? 0 : resultCode.hashCode());
		result = prime * result + ((targetCode == null) ? 0 : targetCode.hashCode());
		result = prime * result + ((typeOfResult == null) ? 0 : typeOfResult.hashCode());
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
		OutputParam other = (OutputParam) obj;
		if (level == null) {
			if (other.level != null)
				return false;
		} else if (!level.equals(other.level))
			return false;
		if (resultCode == null) {
			if (other.resultCode != null)
				return false;
		} else if (!resultCode.equals(other.resultCode))
			return false;
		if (targetCode == null) {
			if (other.targetCode != null)
				return false;
		} else if (!targetCode.equals(other.targetCode))
			return false;
		if (typeOfResult == null) {
			if (other.typeOfResult != null)
				return false;
		} else if (!typeOfResult.equals(other.typeOfResult))
			return false;
		return true;
	}

	@Override
	public int compareTo(Object o) {
		if (this == o)
			return 0;
		if (o == null)
			return -1;
		if (getClass() != o.getClass())
			return -1;
		OutputParam other = (OutputParam) o;
		// TODO Auto-generated method stub
		if (this.resultCode.compareTo(other.getResultCode())==0) {
			if (this.targetCode.compareTo(other.getTargetCode())==0) {
				if (this.level.compareTo(other.getLevel())==0) {
					return 0;
				} else {
					return this.level.compareTo(other.getLevel());
				}
			} else {
				return this.targetCode.compareTo(other.getTargetCode());
			}
		} else {
			return this.resultCode.compareTo(other.getResultCode());
		}
	}


	 
	
}