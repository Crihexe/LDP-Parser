package com.crihexe.ldp;

import com.scalified.tree.TreeNode;

public interface LDPTraversalAction<T extends TreeNode> {
	
	void prePerform(T node);
	void postPerform(T node);
	
	boolean isCompleted();
	
}
