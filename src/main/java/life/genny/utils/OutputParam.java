package life.genny.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.annotations.Expose;


public class OutputParam implements Serializable,Comparable<Object> {
	  /**
	 * This class is used by RuleFlowGroupWorkItemHandler to store the output result of the Rule Task.
	 */
	private static final long serialVersionUID = 1L;
	@Expose
	private Object result;
	@Expose
	private Integer level=0;
	@Expose 
	private String askSourceCode;
	@Expose 
	private String askTargetCode;
	@Expose
	private String typeOfResult="ERROR";  // FRAME_CODE or LIFECYCLE
	@Expose
	private String resultCode = "DUMMY";
	@Expose
	private String targetCode = "DUMMY";
	@Expose
	private Long taskId = -1L;
	@Expose
	private List<Long> longList = new ArrayList<Long>();

	@Expose
	private Map<String,String> attributeTargetCodeMap = new ConcurrentHashMap<String,String>();

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
	
	/**
	 * @return the askSourceCode
	 */
	public String getAskSourceCode() {
		return askSourceCode;
	}

	/**
	 * @param askSourceCode the askSourceCode to set
	 */
	public void setAskSourceCode(String askSourceCode) {
		this.askSourceCode = askSourceCode;
	}

	/**
	 * @return the askTargteCode
	 */
	public String getAskTargetCode() {
		return askTargetCode;
	}

	/**
	 * @param askTargteCode the askTargteCode to set
	 */
	public void setAskTargetCode(String askTargetCode) {
		this.askTargetCode = askTargetCode;
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

//	public Object getResult() {
//		return result;
//	}
//	
//	public void setResult(Object result) {
//		this.result = result;
//	}

	
	
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

	
	
	/**
	 * @return the taskId
	 */
	public Long getTaskId() {
		return taskId;
	}

	/**
	 * @param taskId the taskId to set
	 */
	public void setTaskId(Long taskId) {
		if (taskId == null) {
			taskId = -1L;
		}
		this.taskId = taskId;
	}

	
	
	
	/**
	 * @return the attributeTargetCodeMap
	 */
	public Map<String, String> getAttributeTargetCodeMap() {
		return attributeTargetCodeMap;
	}

	/**
	 * @param attributeTargetCodeMap the attributeTargetCodeMap to set
	 */
	public void setAttributeTargetCodeMap(Map<String, String> attributeTargetCodeMap) {
		this.attributeTargetCodeMap = attributeTargetCodeMap;
	}

	
	
	
	/**
	 * @return the longList
	 */
	public List<Long> getLongList() {
		return longList;
	}

	/**
	 * @param longList the longList to set
	 */
	public void setLongList(List<Long> longList) {
		this.longList = longList;
	}

	@Override
	public String toString() {
		return "OutputParam [typeOfResult=" + typeOfResult + ", resultCode=" + resultCode + ":target="+this.getTargetCode()+": lvl="+level+":"+attributeTargetCodeMap+"]";
	}







	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((level == null) ? 0 : level.hashCode());
		result = prime * result + ((resultCode == null) ? 0 : resultCode.hashCode());
		result = prime * result + ((targetCode == null) ? 0 : targetCode.hashCode());
		result = prime * result + ((typeOfResult == null) ? 0 : typeOfResult.hashCode());
	//	result = prime * result + ((taskId == null) ? 0 : taskId.hashCode());
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
//		if (taskId == null) {
//			if (other.taskId != null)
//				return false;
//		} else if (!taskId.equals(other.taskId))
//			return false;
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
//					if (this.taskId.compareTo(other.getTaskId())==0) {
						return 0;
//					} else {
//						return this.taskId.compareTo(other.getTaskId());
//					}
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