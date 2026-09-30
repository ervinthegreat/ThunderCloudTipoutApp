package tipout;

import java.util.Arrays;
import java.util.List;

/** Runs worked examples through the calculator and compares against hand-calculated sheets. */
public class TipoutCalculatorCheck {
	private static int failures = 0;

	public static void main(String[] args) {
		moneyParsingAndFormatting();
		fourPersonDayWithTwoEarlyTipouts();
		twoPeopleStarredAtOnce();
		rejectsMistakes();
		standaloneMatchesFullShift();
		namesAreTheHeadcount();
		starringOneOfTwoSameNames();
		System.out.println(failures == 0 ? "ALL CHECKS PASSED" : failures + " CHECK(S) FAILED");
		if (failures > 0) System.exit(1);
	}

	private static void moneyParsingAndFormatting() {
		expect("parse 123.45", 12345, Money.parse("123.45"));
		expect("parse $1,234.5", 123450, Money.parse("$1,234.5"));
		expect("parse 80", 8000, Money.parse("80"));
		expect("parse .5", 50, Money.parse(".5"));
		expectText("format", "$1,234.56", Money.format(123456));
		expectText("format small", "$0.07", Money.format(7));
	}

	private static ReportSnapshot report(String cash, String credit, String uber) {
		return new ReportSnapshot(Money.parse(cash), Money.parse(credit), Money.parse(uber));
	}

	/**
	 * Ana, Ben, Cal, Dee.
	 * 1:00 PM Ana starred.  Report cash 100, card 400, uber 1000.  Team 4.
	 *   cash 25.00, card 100.00, uber 1000 x 0.0739 / 4 = 18.475 -> 18.48.  Take 143.48, total 143.48.
	 * 2:30 PM Ben starred.  Report cash 190, card 700, uber 1600.  Period 90 / 300 / 600.  Team 3.
	 *   cash 30.00, card 100.00, uber 600 x 0.0739 / 3 = 14.78.  Take 144.78, total 288.26.
	 * 3:00 PM final (Cal, Dee).  Report cash 250, card 900, uber 2000.  Period 60 / 200 / 400.  Team 2.
	 *   cash 30.00, card 100.00, uber 400 x 0.0739 / 2 = 14.78.  Take 144.78, total 433.04.
	 */
	private static void fourPersonDayWithTwoEarlyTipouts() {
		Shift shift = new Shift("2026-09-29", Arrays.asList("Ana", "Ben", "Cal", "Dee"));
		shift.addTipout(new Tipout("1:00 PM", report("100", "400", "1000"), Arrays.asList("Ana")));
		shift.addTipout(new Tipout("2:30 PM", report("190", "700", "1600"), Arrays.asList("Ben")));
		shift.addFinalTipout("3:00 PM", report("250", "900", "2000"));
		List<TipoutCalculator.Result> r = TipoutCalculator.calculate(shift);

		expect("tipouts", 3, r.size());
		expectResult("tipout 1", r.get(0), 4, "25.00", "100.00", "18.48", "143.48", "143.48");
		expectResult("tipout 2", r.get(1), 3, "30.00", "100.00", "14.78", "144.78", "288.26");
		expectResult("final", r.get(2), 2, "30.00", "100.00", "14.78", "144.78", "433.04");
		expectText("final starred", "[Cal, Dee]", r.get(2).tipout.starred.toString());
		expectText("remaining after final", "[]", shift.getRemaining().toString());
	}

	/** Five people, two win the draw at 12:45. Report card 500, uber 0, cash 0. Team 5 -> 100.00 each. */
	private static void twoPeopleStarredAtOnce() {
		Shift shift = new Shift("2026-09-29", Arrays.asList("A", "B", "C", "D", "E"));
		shift.addTipout(new Tipout("12:45 PM", report("0", "500", "0"), Arrays.asList("B", "D")));
		expectText("remaining", "[A, C, E]", shift.getRemaining().toString());
		shift.addFinalTipout("3:00 PM", report("0", "800", "0"));
		List<TipoutCalculator.Result> r = TipoutCalculator.calculate(shift);
		expectResult("draw", r.get(0), 5, "0.00", "100.00", "0.00", "100.00", "100.00");
		expectResult("final", r.get(1), 3, "0.00", "100.00", "0.00", "100.00", "200.00");
	}

	private static void rejectsMistakes() {
		Shift shift = new Shift("2026-09-29", Arrays.asList("Ana", "Ben", "Cal"));
		shift.addTipout(new Tipout("1:00 PM", report("100", "400", "1000"), Arrays.asList("Ana")));
		expectThrows("report lower than previous", () ->
				shift.addTipout(new Tipout("2:00 PM", report("100", "300", "1000"), Arrays.asList("Ben"))));
		expectThrows("starring someone already gone", () ->
				shift.addTipout(new Tipout("2:00 PM", report("100", "500", "1000"), Arrays.asList("Ana"))));
		expectThrows("starring nobody", () ->
				shift.addTipout(new Tipout("2:00 PM", report("100", "500", "1000"), Arrays.asList())));
		expect("mistakes didn't get recorded", 1, shift.getTipouts().size());
	}

	/** Same day as fourPersonDayWithTwoEarlyTipouts, but typed in from the paper sheet one tipout at a time. */
	private static void standaloneMatchesFullShift() {
		TipoutCalculator.Result first = TipoutCalculator.calculate(Arrays.asList("Ana", "Ben", "Cal", "Dee"),
				report("100", "400", "1000"), ReportSnapshot.ZERO, 0);
		expectResult("standalone 1", first, 4, "25.00", "100.00", "18.48", "143.48", "143.48");
		TipoutCalculator.Result second = TipoutCalculator.calculate(Arrays.asList("Ben", "Cal", "Dee"),
				report("190", "700", "1600"), report("100", "400", "1000"), Money.parse("143.48"));
		expectResult("standalone 2", second, 3, "30.00", "100.00", "14.78", "144.78", "288.26");
		TipoutCalculator.Result last = TipoutCalculator.calculate(Arrays.asList("Cal", "Dee"),
				report("250", "900", "2000"), report("190", "700", "1600"), Money.parse("288.26"));
		expectResult("standalone final", last, 2, "30.00", "100.00", "14.78", "144.78", "433.04");
		expectText("standalone names kept", "[Cal, Dee]", last.present.toString());
		expectThrows("standalone lower report", () -> TipoutCalculator.calculate(Arrays.asList("Cal", "Dee"),
				report("100", "300", "1000"), report("100", "400", "1000"), 0));
	}

	/** The headcount comes from names, so bad lists are refused rather than miscounted. */
	private static void namesAreTheHeadcount() {
		ReportSnapshot r = report("0", "100", "0");
		expectThrows("no names", () -> TipoutCalculator.calculate(Arrays.asList(), r, ReportSnapshot.ZERO, 0));
		expectThrows("blank name", () -> TipoutCalculator.calculate(Arrays.asList("Ana", "  "), r, ReportSnapshot.ZERO, 0));
		TipoutCalculator.Result ok = TipoutCalculator.calculate(Arrays.asList(" Anthony ", "Ana"), r, ReportSnapshot.ZERO, 0);
		expectText("names trimmed", "[Anthony, Ana]", ok.present.toString());
		expect("headcount from names", 2, ok.teamSize);
		TipoutCalculator.Result twins = TipoutCalculator.calculate(
				Arrays.asList("Corey", "Corey", "Rachael", "Rachael"), r, ReportSnapshot.ZERO, 0);
		expect("repeated names are separate people", 4, twins.teamSize);
		expect("repeated names split four ways", Money.parse("25.00"), twins.creditCardShare);
	}

	/** Starring one of two Coreys must leave the other Corey on the list. */
	private static void starringOneOfTwoSameNames() {
		Shift shift = new Shift("2026-09-29", Arrays.asList("Corey", "Vince", "Corey"));
		shift.addTipout(new Tipout("1:00 PM", report("0", "300", "0"), Arrays.asList("Corey")));
		expectText("one Corey left", "[Vince, Corey]", shift.getRemaining().toString());
		shift.addTipout(new Tipout("2:00 PM", report("0", "500", "0"), Arrays.asList("Corey")));
		expectText("both Coreys gone", "[Vince]", shift.getRemaining().toString());
		expectThrows("no third Corey", () ->
				shift.addTipout(new Tipout("2:30 PM", report("0", "600", "0"), Arrays.asList("Corey"))));
		List<TipoutCalculator.Result> r = TipoutCalculator.calculate(shift);
		expect("first split three ways", 3, r.get(0).teamSize);
		expect("second split two ways", 2, r.get(1).teamSize);
	}

	private static void expectResult(String label, TipoutCalculator.Result r, int teamSize,
			String cash, String credit, String uber, String take, String total) {
		expect(label + " team size", teamSize, r.teamSize);
		expect(label + " cash", Money.parse(cash), r.cashShare);
		expect(label + " card", Money.parse(credit), r.creditCardShare);
		expect(label + " uber", Money.parse(uber), r.uberEatsShare);
		expect(label + " this take", Money.parse(take), r.thisTake);
		expect(label + " total so far", Money.parse(total), r.totalSoFar);
	}

	private static void expect(String label, long expected, long actual) {
		if (expected != actual) fail(label + ": expected " + expected + " got " + actual);
	}

	private static void expectText(String label, String expected, String actual) {
		if (!expected.equals(actual)) fail(label + ": expected " + expected + " got " + actual);
	}

	private static void expectThrows(String label, Runnable action) {
		try {
			action.run();
			fail(label + ": expected an error, none thrown");
		} catch (RuntimeException expected) {
			// rejected as intended
		}
	}

	private static void fail(String message) {
		failures++;
		System.out.println("FAIL " + message);
	}
}
