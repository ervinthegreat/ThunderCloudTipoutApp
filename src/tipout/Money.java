package tipout;

/** Money is always held as whole cents in a long, so sums never drift the way floating point does. */
public final class Money {
	private Money() {}

	/** Parses "123.45", "$1,234.5", "80" into cents. Throws NumberFormatException on bad input. */
	public static long parse(String text) {
		String s = text.trim().replace("$", "").replace(",", "");
		if (s.isEmpty()) throw new NumberFormatException("empty amount");
		boolean negative = s.startsWith("-");
		if (negative) s = s.substring(1);
		int dot = s.indexOf('.');
		String whole = dot < 0 ? s : s.substring(0, dot);
		String frac = dot < 0 ? "" : s.substring(dot + 1);
		if (frac.length() > 2) throw new NumberFormatException("more than 2 decimal places: " + text);
		while (frac.length() < 2) frac += "0";
		long cents = Long.parseLong(whole.isEmpty() ? "0" : whole) * 100 + Long.parseLong(frac);
		return negative ? -cents : cents;
	}

	/** Formats cents as "$1,234.56". */
	public static String format(long cents) {
		boolean negative = cents < 0;
		long abs = Math.abs(cents);
		String whole = Long.toString(abs / 100);
		StringBuilder grouped = new StringBuilder();
		for (int i = 0; i < whole.length(); i++) {
			if (i > 0 && (whole.length() - i) % 3 == 0) grouped.append(',');
			grouped.append(whole.charAt(i));
		}
		long frac = abs % 100;
		return (negative ? "-$" : "$") + grouped + "." + (frac < 10 ? "0" : "") + frac;
	}

	/** numerator / denominator rounded to the nearest cent, halves rounded up. Both inputs non-negative. */
	static long divideRounded(long numerator, long denominator) {
		return (numerator * 2 + denominator) / (denominator * 2);
	}
}
