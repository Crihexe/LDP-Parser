package com.crihexe.ldp;

import java.util.Collection;

import com.scalified.tree.TreeNode;
import com.scalified.tree.multinode.ArrayMultiTreeNode;

public class LDPNode extends ArrayMultiTreeNode<String> {

	public LDPNode(String data) {
		super(data == null ? "" : data);
	}
	
	public LDPNode add(LDPNode node) {
		super.add(node);
		return node;
	}
	
	public void generate() {
		System.out.println(super.toString());
		Collection<? extends TreeNode<String>> subtrees = super.subtrees();
		for(TreeNode<String> node : subtrees)
			System.out.println(node.data());
		System.out.println();
		rec(this);
	}// System.out.print((node.isLeaf()?"":"(") + node.data() + " ");
	
	private void rec(TreeNode<String> node) {
		boolean par = !node.isLeaf();
		if(par) System.out.print("(");
		Collection<? extends TreeNode<String>> subtrees = node.subtrees();
		System.out.print(node.data());
		if(subtrees.size() > 0) {
			System.out.print(" ");
			for(TreeNode<String> child : subtrees)
				rec(child);
		}
		if(par) System.out.print(")");
	}
	
	public static LDPNode node(String data) {
		return new LDPNode(data);
	}
	
	public static LDPNode node(String data, String...children) {
		LDPNode node = node(data);
		for(String child : children)
			node.add(node(child));
		return node;
	}
	
	public static LDPNode node(String data, LDPNode...children) {
		LDPNode node = node(data);
		for(LDPNode child : children)
			node.add(child);
		return node;
	}
	
	public static LDPNode node(String data, Object...children) {
		LDPNode node = node(data);
		for(Object child : children) {
			if(child instanceof String)
				node.add(node((String)child));
			else if(child instanceof LDPNode)
				node.add((LDPNode)child);
			else
				System.err.println("Not a String or LDPNode! Value: " + child);
		}
		return node;
	}
	
}
