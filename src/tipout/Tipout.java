package tipout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One tipout: when it happened, the report printed for it, and who was starred (went home) at it. */
public final class Tipout {
	/** Bookkeeping only, e.g. "1:05 PM"; it doesn't affect the math. */
	public final String time;
	public final ReportSnapshot report;
	public final List<String> starred;

	public Tipout(String time, ReportSnapshot report, List<String> starred) {
		this.time = time;
		this.report = report;
		this.starred = Collections.unmodifiableList(new ArrayList<>(starred));
	}
}
