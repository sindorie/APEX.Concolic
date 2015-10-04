package components;

import java.util.Comparator;
import java.util.List;

public class ListSZComparator implements Comparator<List<EventSummaryPair>>{
	@Override
	public int compare(List<EventSummaryPair> o1, List<EventSummaryPair> o2) {
		return o1.size() - o2.size();
	}
}
