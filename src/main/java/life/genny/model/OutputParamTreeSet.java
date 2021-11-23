package life.genny.model;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.logging.log4j.Logger;

import com.github.javaparser.utils.Log;

import life.genny.qwanda.utils.OutputParam;

public class OutputParamTreeSet implements Serializable {
	
	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ConcurrentSkipListSet<OutputParam> tree;
//	TreeSet<OutputParam> tree;
	OutputParam last = null;

	public OutputParamTreeSet() {
		tree = new ConcurrentSkipListSet<OutputParam>();
	//	tree = new TreeSet<OutputParam>();
		OutputParam root = new OutputParam();
		root.setFormCode("FRM_APP", "FRM_ROOT");
		root.setLevel(0);
		add(root);
	}

	public OutputParamTreeSet(final String rootFrameCode) {
		tree = new ConcurrentSkipListSet<OutputParam>();
		//tree = new TreeSet<OutputParam>();
		OutputParam root = new OutputParam();
		root.setFormCode(rootFrameCode, "FRM_ROOT");
		root.setLevel(0);
		add(root);
	}

	/**
	 * @return the tree
	 */
	public ConcurrentSkipListSet<OutputParam> getTree2() {
		return tree;
	}

	/**
	 * @param tree the tree to set
	 */
	public void setTree(ConcurrentSkipListSet<OutputParam> tree) {
		this.tree = tree;
	}

	/**
	 * @return the last
	 */
	public OutputParam getLast() {
		return last;
	}

	public void add(OutputParam outputParam) {
		// Consolidate output Params so that redundant replaced frames are replaced
		// find the targetCode
		if (outputParam != null) {
			if (!outputParam.getResultCode().equals(outputParam.getTargetCode())) {
				if (outputParam.getResultCode().equals("NONE")) {
					return; // ignore
				}
				if (!this.tree.contains(outputParam)) {
				
				// find the OutputParam in the tree that contains the targetframe as the result
				Optional<OutputParam> optParent = tree.parallelStream()
						.filter(x -> x.getResultCode().equals(outputParam.getTargetCode())).findFirst();
				
				// Found the target !
				if (optParent.isPresent()) {
					OutputParam parent = optParent.get();
					
					// Now check if any existing items already point to that parent and remove them.
					Optional<OutputParam> optExisting = tree.parallelStream()
							.filter(x -> (x.getResultCode().equals(outputParam.getResultCode()) && (x.getTargetCode().equals(outputParam.getTargetCode()))))
							.findFirst();
				
						outputParam.setLevel(parent.getLevel() + 1);
						if (optExisting.isPresent()) {
							this.tree.remove(optExisting.get());
						}
						this.tree.add(outputParam); // this needs to replace any existing treesets that have the same
													// target code
		
				} else { // NO TARGET EXISTS IN THE DOM
					// Now check if any existing items already point to that parent and remove them.
					for (OutputParam x : tree.toArray(new OutputParam[0])) {
						if (x == null) {
							log.info("An outputparam is null!");
						}
					}
					if (outputParam != null ) {
					Optional<OutputParam> optExisting = tree.parallelStream()
							.filter(x -> x!=null)
							.filter(x -> x.getTargetCode()!=null)
							.filter(x ->  x.getTargetCode().equals(outputParam.getTargetCode()))
							.findFirst();				
						if (optExisting.isPresent()) {
							this.tree.remove(optExisting.get());
						}
						this.tree.add(outputParam); // this needs to replace any existing treesets that have the same
													// target code
					}
				}
					}
				last = outputParam;

			}
		}
	}

	@Override
	public String toString() {
		return "OutputParamTreeSet [tree=" + tree + ", last=" + last + "]";
	}
	
	
}
