package com.nomade;

public class Node {
    public int index;
	public double value;
	public Node next;

	public static long SIZE = 16;

	public void swapIndex() {
		int temp = this.index;
        this.index = this.next.index;
        this.next.index = temp;
	}

	public void swapIndex(Node node) {
		int temp = this.index;
        this.index = node.index;
        node.index = temp;
	}

    public void swapValue() {
        double temp = this.value;
        this.value = this.next.value;
        this.next.value = temp;
    }

	public void swapValue(Node node) {
		double temp = this.value;
        this.value = node.value;
        node.value = temp;
	}
}
