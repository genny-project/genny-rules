package life.genny.model;

import java.io.Serializable;
import java.util.TreeSet;

import life.genny.utils.OutputParam;

public class OutputParamTreeSet implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	TreeSet<OutputParam> tree;
	OutputParam last = null;
	
	public OutputParamTreeSet()
	{
		tree = new TreeSet<OutputParam>();
		OutputParam root = new OutputParam("FRM_ROOT",null);
		root.setLevel(0);
		tree.add(root);
	}

	public OutputParamTreeSet(final String rootFrameCode)
	{
		tree = new TreeSet<OutputParam>();
		OutputParam root = new OutputParam(rootFrameCode,"FRM_ROOT");
		root.setLevel(0);
		tree.add(root);
	}
	
	/**
	 * @return the tree
	 */
	public TreeSet<OutputParam> getTree() {
		return tree;
	}



	/**
	 * @param tree the tree to set
	 */
	public void setTree(TreeSet<OutputParam> tree) {
		this.tree = tree;
	}



	/**
	 * @return the last
	 */
	public OutputParam getLast() {
		return last;
	}



	public void add(OutputParam outputParam)
	{
		// Consolidate output Params so that redundant replaced frames are replaced
		// find the targetCode
		OutputParam target = tree.ceiling(outputParam);
		Integer level = target.getLevel();
		outputParam.setLevel(level+1); 
		this.tree.add(outputParam);
		last = outputParam;
	}
}
