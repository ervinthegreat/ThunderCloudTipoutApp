package tipout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One day's tipout sheet: the date, everyone's name written once, and each tipout in order. */
public final class Shift {
	public final String date;
	private final List<String> team;
	private final List<Tipout> tipouts = new ArrayList<>();

	public Shift(String date, List<String> team) {
		this.date = date;
		this.team = Collections.unmodifiableList(TipoutCalculator.validateNames(team));
	}

	public List<String> getTeam() {return team;}
	public List<Tipout> getTipouts() {return Collections.unmodifiableList(tipouts);}

	/** Names not starred at any tipout so far, in the order they were written. */
	public List<String> getRemaining() {
		List<String> remaining = new ArrayList<>(team);
		for (Tipout t : tipouts) removeOnceEach(remaining, t.starred);
		return remaining;
	}

	/**
	 * Removes one entry per starred name, so starring one of two Coreys leaves the other.
	 * Returns false if a starred name has no remaining entry left to remove.
	 */
	static boolean removeOnceEach(List<String> names, List<String> starred) {
		for (String name : starred) {
			if (!names.remove(name)) return false;
		}
		return true;
	}

	/** Records a tipout. The report must be a running total for the day, at least the previous report. */
	public void addTipout(Tipout tipout) {
		List<String> remaining = getRemaining();
		if (remaining.isEmpty()) throw new IllegalStateException("everyone has already been paid out");
		if (tipout.starred.isEmpty()) throw new IllegalArgumentException("a tipout must star at least one person");
		if (!removeOnceEach(remaining, tipout.starred)) {
			throw new IllegalArgumentException("starred someone who isn't on the remaining list");
		}
		ReportSnapshot previous = tipouts.isEmpty() ? ReportSnapshot.ZERO : tipouts.get(tipouts.size() - 1).report;
		ReportSnapshot delta = tipout.report.minus(previous);
		if (delta.cashTips < 0 || delta.creditCardTips < 0 || delta.uberEats < 0) {
			throw new IllegalArgumentException("report is lower than the previous report");
		}
		tipouts.add(tipout);
	}

	/** Final tipout of the day: stars everyone still remaining. */
	public void addFinalTipout(String time, ReportSnapshot report) {
		addTipout(new Tipout(time, report, getRemaining()));
	}
}
