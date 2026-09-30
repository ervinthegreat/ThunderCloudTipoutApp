package tipout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The handwritten procedure:
 * - Each tipout covers the period since the previous report (this report minus the last one).
 * - Team size is everyone not starred at an earlier tipout.
 * - Cash and credit card tips are divided by team size.
 * - Uber Eats is multiplied by 0.0739, then divided by team size.
 * - "Total so far" adds this take to every earlier take. The list only shrinks, so everyone present
 *   has worked every earlier period and shares the same running total.
 * Each share is rounded to the nearest cent; totals are sums of those rounded shares.
 */
public final class TipoutCalculator {
	/** 0.0739 as an exact fraction, so Uber Eats math stays in whole cents. */
	public static final long UBER_RATE_NUMERATOR = 739;
	public static final long UBER_RATE_DENOMINATOR = 10_000;

	/** The written line for one tipout. All money in cents, per person. */
	public static final class Result {
		public final Tipout tipout;
		public final ReportSnapshot periodSales;
		public final List<String> present;
		public final int teamSize;
		public final long cashShare;
		public final long creditCardShare;
		public final long uberEatsShare;
		public final long thisTake;
		public final long totalSoFar;

		Result(Tipout tipout, ReportSnapshot periodSales, List<String> present,
				long cashShare, long creditCardShare, long uberEatsShare, long totalSoFar) {
			this.tipout = tipout;
			this.periodSales = periodSales;
			this.present = Collections.unmodifiableList(present);
			this.teamSize = present.size();
			this.cashShare = cashShare;
			this.creditCardShare = creditCardShare;
			this.uberEatsShare = uberEatsShare;
			this.thisTake = cashShare + creditCardShare + uberEatsShare;
			this.totalSoFar = totalSoFar;
		}
	}

	/**
	 * One tipout from values copied off the paper sheet, with no stored history.
	 * Team size is the number of names, so the headcount can't be guessed.
	 * For the first tipout pass ReportSnapshot.ZERO and 0 for the previous report and total.
	 */
	public static Result calculate(List<String> names, ReportSnapshot currentReport,
			ReportSnapshot previousReport, long previousTotal) {
		List<String> team = validateNames(names);
		int teamSize = team.size();
		ReportSnapshot period = currentReport.minus(previousReport);
		if (period.cashTips < 0 || period.creditCardTips < 0 || period.uberEats < 0) {
			throw new IllegalArgumentException("report is lower than the previous report");
		}
		long cash = Money.divideRounded(period.cashTips, teamSize);
		long credit = Money.divideRounded(period.creditCardTips, teamSize);
		long uber = Money.divideRounded(period.uberEats * UBER_RATE_NUMERATOR, UBER_RATE_DENOMINATOR * teamSize);
		return new Result(null, period, team, cash, credit, uber, previousTotal + cash + credit + uber);
	}

	/** Trimmed names; rejects an empty list and blank names. Repeated names are separate people. */
	public static List<String> validateNames(List<String> names) {
		List<String> team = new ArrayList<>();
		for (String raw : names) {
			String name = raw == null ? "" : raw.trim();
			if (name.isEmpty()) throw new IllegalArgumentException("every person needs a name");
			team.add(name);
		}
		if (team.isEmpty()) throw new IllegalArgumentException("enter at least one name");
		return team;
	}

	public static List<Result> calculate(Shift shift) {
		List<Result> results = new ArrayList<>();
		List<String> present = new ArrayList<>(shift.getTeam());
		ReportSnapshot previous = ReportSnapshot.ZERO;
		long totalSoFar = 0;
		for (Tipout tipout : shift.getTipouts()) {
			ReportSnapshot period = tipout.report.minus(previous);
			long size = present.size();
			long cash = Money.divideRounded(period.cashTips, size);
			long credit = Money.divideRounded(period.creditCardTips, size);
			long uber = Money.divideRounded(period.uberEats * UBER_RATE_NUMERATOR, UBER_RATE_DENOMINATOR * size);
			totalSoFar += cash + credit + uber;
			results.add(new Result(tipout, period, new ArrayList<>(present), cash, credit, uber, totalSoFar));
			Shift.removeOnceEach(present, tipout.starred);
			previous = tipout.report;
		}
		return results;
	}
}
