package com.abstratt.pluginutils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeSorter {
	public interface NodeHandler<T> {
		Collection<T> next(T node);
	}

	static class Counter {
		int count = 0;

		@Override
		public String toString() {
			return "" + count;
		}
	}

	public static <N> List<N> sort(Collection<N> toSort,
	        NodeHandler<N> nodeHandler) {
		if (toSort.size() < 2)
			return new ArrayList<N>(toSort);
		Map<N, Counter> predecessorCounts = new HashMap<N, Counter>(
		        toSort.size());
		for (N vertex : toSort)
			predecessorCounts.put(vertex, new Counter());
		for (N vertex : toSort) {
			Collection<N> successors = nodeHandler.next(vertex);
			for (N successor : successors) {
				Counter predecessorCount = predecessorCounts.get(successor);
				if (predecessorCount != null)
					predecessorCount.count++;
			}
		}
		List<N> sorted = new ArrayList<N>(toSort.size());
		for (int i = 0; i < toSort.size(); i++)
			for (Map.Entry<N, Counter> it : predecessorCounts.entrySet())
				if (it.getValue().count == 0) {
					it.getValue().count = -1;
					sorted.add(it.getKey());
					if (sorted.size() == toSort.size())
						return sorted;
					for (N successor : nodeHandler.next(it.getKey())) {
						Counter predecessorCount = predecessorCounts
						        .get(successor);
						if (predecessorCount != null)
							predecessorCount.count--;
					}
				}
		throw new IllegalArgumentException("Cycle found");
	}
}
