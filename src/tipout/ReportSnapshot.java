package tipout;

/** The three running totals from one printed sales report, in cents. */
public final class ReportSnapshot {
	public static final ReportSnapshot ZERO = new ReportSnapshot(0, 0, 0);

	public final long cashTips;
	public final long creditCardTips;
	public final long uberEats;

	public ReportSnapshot(long cashTips, long creditCardTips, long uberEats) {
		this.cashTips = cashTips;
		this.creditCardTips = creditCardTips;
		this.uberEats = uberEats;
	}

	/** What came in between an earlier report and this one. */
	public ReportSnapshot minus(ReportSnapshot earlier) {
		return new ReportSnapshot(cashTips - earlier.cashTips,
				creditCardTips - earlier.creditCardTips,
				uberEats - earlier.uberEats);
	}
}
