package app;

import java.util.ArrayList;
import java.util.List;

import tipout.Money;
import tipout.ReportSnapshot;
import tipout.TipoutCalculator;
import ui.ClockSource;
import ui.TextField;
import ui.TextInputHost;
import ui.UiCanvas;
import ui.UiLayer;

/**
 * The tipout screens, one step at a time, mirroring the paper procedure.
 * Nothing is remembered: the paper sheet is the record, and "New tipout" clears everything.
 */
public class TipApp implements UiLayer {

	private enum Step { NAMES, FIRST_OR_LATER, REPORT, PREVIOUS, RESULT }

	private static final int DIM = 0x99000000;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int MUTED = 0xFFB9B9C9;
	private static final int ACCENT = 0xFFFFD21F;
	private static final int FIELD_BG = 0xE6181822;
	private static final int FIELD_BORDER = 0xFF4A4A5E;
	private static final int ERROR = 0xFFFF7070;
	private static final int DARK_TEXT = 0xFF161616;
	private static final int DIVIDER = 0x55FFFFFF;

	/** Leaves room for the iPhone notch and status bar. */
	private static final float TOP = 60;
	private static final float MARGIN = 20;
	private static final float FIELD_H = 48;
	private static final float BUTTON_H = 52;
	private static final int MAX_NAMES = 12;

	private final TextInputHost input;
	private final ClockSource clock;

	private Step step;
	private final List<TextField> names = new ArrayList<>();
	private final TextField cash = new TextField(TextField.Kind.MONEY);
	private final TextField credit = new TextField(TextField.Kind.MONEY);
	private final TextField uber = new TextField(TextField.Kind.MONEY);
	private final TextField prevCash = new TextField(TextField.Kind.MONEY);
	private final TextField prevCredit = new TextField(TextField.Kind.MONEY);
	private final TextField prevUber = new TextField(TextField.Kind.MONEY);
	private final TextField prevTotal = new TextField(TextField.Kind.MONEY);
	private boolean firstTipout;
	/** Start over takes two taps so a stray tap mid-rush can't erase the tipout. */
	private boolean confirmStartOver;
	private final Runnable startOver = this::startOverTapped;
	private String error;
	private TipoutCalculator.Result result;
	private String resultDate, resultTime;

	/** Rebuilt every paint; taps are hit-tested against the most recent layout. */
	private final List<TextField> visibleFields = new ArrayList<>();
	private final List<Button> buttons = new ArrayList<>();

	private static final class Button {
		final float x, y, w, h;
		final Runnable action;

		Button(float x, float y, float w, float h, Runnable action) {
			this.x = x; this.y = y; this.w = w; this.h = h;
			this.action = action;
		}
	}

	public TipApp(TextInputHost input, ClockSource clock) {
		this.input = input;
		this.clock = clock;
		chain(cash, credit);
		chain(credit, uber);
		uber.onEnter = this::reportNext;
		chain(prevCash, prevCredit);
		chain(prevCredit, prevUber);
		chain(prevUber, prevTotal);
		prevTotal.onEnter = this::calculate;
		reset();
	}

	private void chain(TextField from, TextField to) {
		from.onEnter = () -> input.focus(to);
	}

	private void reset() {
		names.clear();
		addNameRow();
		for (TextField f : new TextField[] {cash, credit, uber, prevCash, prevCredit, prevUber, prevTotal}) f.text = "";
		firstTipout = true;
		result = null;
		error = null;
		confirmStartOver = false;
		step = Step.NAMES;
	}

	private void startOverTapped() {
		if (!confirmStartOver) {
			confirmStartOver = true;
			return;
		}
		input.blurAll();
		reset();
	}

	private boolean hasAnythingToErase() {
		if (step != Step.NAMES || names.size() > 1) return true;
		return !names.get(0).text.trim().isEmpty();
	}

	private void go(Step next) {
		input.blurAll();
		error = null;
		step = next;
	}

	// ---------------------------------------------------------------- names

	private TextField addNameRow() {
		TextField f = new TextField(TextField.Kind.NAME);
		f.onEnter = () -> nameEnter(f);
		names.add(f);
		return f;
	}

	/** Return on a name moves to the next line, adding one when on the last filled line. */
	private void nameEnter(TextField f) {
		int i = names.indexOf(f);
		if (i < names.size() - 1) {
			input.focus(names.get(i + 1));
		} else if (!f.text.trim().isEmpty() && names.size() < MAX_NAMES) {
			TextField next = addNameRow();
			next.x = f.x; next.w = f.w; next.h = f.h;
			next.y = f.y + f.h + rowGap();
			input.focus(next);
		} else {
			input.blurAll();
		}
	}

	private float rowHeight() {return names.size() > 8 ? 40 : FIELD_H;}
	private float rowGap() {return names.size() > 8 ? 6 : 10;}

	private void namesNext() {
		for (int i = 0; i < names.size(); i++) {
			if (names.get(i).text.trim().isEmpty()) {
				error = "Line " + (i + 1) + " is empty. Type a name or remove the line.";
				return;
			}
		}
		go(Step.FIRST_OR_LATER);
	}

	private List<String> nameList() {
		List<String> list = new ArrayList<>();
		for (TextField f : names) list.add(f.text);
		return TipoutCalculator.validateNames(list);
	}

	// ---------------------------------------------------------------- money

	private static long parseMoney(TextField f, String label) {
		String t = f.text.trim();
		if (t.isEmpty()) throw new IllegalArgumentException("Enter " + label + " (0 if none).");
		long cents;
		try {
			cents = Money.parse(t);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(capitalize(label) + " isn't a valid amount.");
		}
		if (cents < 0) throw new IllegalArgumentException(capitalize(label) + " can't be negative.");
		return cents;
	}

	private ReportSnapshot currentReport() {
		return new ReportSnapshot(parseMoney(cash, "cash tips"), parseMoney(credit, "credit card tips"),
				parseMoney(uber, "Uber Eats"));
	}

	private void reportNext() {
		try {
			currentReport();
		} catch (IllegalArgumentException e) {
			error = e.getMessage();
			return;
		}
		if (firstTipout) calculate();
		else go(Step.PREVIOUS);
	}

	private void calculate() {
		try {
			ReportSnapshot current = currentReport();
			ReportSnapshot previous = ReportSnapshot.ZERO;
			long previousTotal = 0;
			if (!firstTipout) {
				previous = new ReportSnapshot(parseMoney(prevCash, "last report cash tips"),
						parseMoney(prevCredit, "last report credit card tips"),
						parseMoney(prevUber, "last report Uber Eats"));
				previousTotal = parseMoney(prevTotal, "last total take");
			}
			result = TipoutCalculator.calculate(nameList(), current, previous, previousTotal);
		} catch (IllegalArgumentException e) {
			String message = e.getMessage();
			if (message != null && message.contains("lower than the previous report")) {
				message = "A current report total is lower than the last report. Check both reports.";
			}
			error = capitalize(message);
			return;
		}
		resultDate = clock.date();
		resultTime = clock.time();
		go(Step.RESULT);
	}

	private static String capitalize(String s) {
		if (s == null || s.isEmpty()) return "Something went wrong.";
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	// ---------------------------------------------------------------- input

	@Override
	public void tap(float x, float y) {
		for (TextField f : visibleFields) {
			if (f.contains(x, y)) {
				confirmStartOver = false;
				input.focus(f);
				return;
			}
		}
		input.blurAll();
		for (Button b : buttons) {
			if (x >= b.x && x < b.x + b.w && y >= b.y && y < b.y + b.h) {
				if (b.action != startOver) confirmStartOver = false;
				b.action.run();
				return;
			}
		}
		confirmStartOver = false;
	}

	// ---------------------------------------------------------------- painting

	@Override
	public void paint(UiCanvas c, float width, float height) {
		visibleFields.clear();
		buttons.clear();
		c.fillRect(0, 0, width, height, DIM);
		float x = MARGIN, w = width - 2 * MARGIN;
		switch (step) {
			case NAMES: paintNames(c, x, w); break;
			case FIRST_OR_LATER: paintFirstOrLater(c, x, w); break;
			case REPORT: paintReport(c, x, w); break;
			case PREVIOUS: paintPrevious(c, x, w); break;
			case RESULT: paintResult(c, x, w); break;
		}
		if (hasAnythingToErase()) paintStartOver(c, x, w, height);
		input.sync(visibleFields);
	}

	/** Pinned above the iPhone home indicator; erases every name and number after a confirming tap. */
	private void paintStartOver(UiCanvas c, float x, float w, float height) {
		float h = 44, y = height - 34 - h;
		buttons.add(new Button(x, y, w, h, startOver));
		if (confirmStartOver) {
			c.fillRoundRect(x, y, w, h, 12, 0x55FF3030);
			c.strokeRoundRect(x, y, w, h, 12, 2f, ERROR);
			c.drawText("Tap again to erase everything", x + w * 0.5f, y + h * 0.5f + 6, 17, true, TEXT,
					UiCanvas.ALIGN_CENTER);
		} else {
			c.strokeRoundRect(x, y, w, h, 12, 1.5f, 0x55FFFFFF);
			c.drawText("Start over", x + w * 0.5f, y + h * 0.5f + 6, 17, false, MUTED, UiCanvas.ALIGN_CENTER);
		}
	}

	private void paintNames(UiCanvas c, float x, float w) {
		float y = title(c, "Who's working?", x, TOP);
		y = paragraph(c, "Type every name on this tipout, one per line. The headcount comes from this list.", x, y, w);
		y += 10;
		float rowH = rowHeight(), gap = rowGap();
		boolean removable = names.size() > 1;
		for (int i = 0; i < names.size(); i++) {
			TextField f = names.get(i);
			c.drawText((i + 1) + ".", x, y + rowH * 0.5f + 6, 16, false, MUTED, UiCanvas.ALIGN_LEFT);
			float fieldX = x + 30;
			float fieldW = w - 30 - (removable ? rowH + 8 : 0);
			layoutField(c, f, fieldX, y, fieldW, rowH, "Name");
			if (removable) {
				TextField row = f;
				button(c, "\u00D7", x + w - rowH, y, rowH, rowH, false, () -> {
					names.remove(row);
					error = null;
				});
			}
			y += rowH + gap;
		}
		if (names.size() < MAX_NAMES) {
			button(c, "+ Add person", x + 30, y, w - 30, 44, false, () -> {
				TextField added = addNameRow();
				input.focus(added);
			});
			y += 44 + 14;
		}
		c.drawText(names.size() + (names.size() == 1 ? " person" : " people"), x, y + 22, 22, true, ACCENT,
				UiCanvas.ALIGN_LEFT);
		y += 36;
		y = errorText(c, x, y, w);
		button(c, "Next", x, y + 6, w, BUTTON_H, true, this::namesNext);
	}

	private void paintFirstOrLater(UiCanvas c, float x, float w) {
		float y = title(c, "First tipout today?", x, TOP);
		y = paragraph(c, "If there was an earlier tipout, you'll copy its numbers from the sheet.", x, y, w);
		y += 16;
		button(c, "Yes, first tipout", x, y, w, BUTTON_H, true, () -> {
			firstTipout = true;
			go(Step.REPORT);
		});
		y += BUTTON_H + 14;
		button(c, "No, there was an earlier one", x, y, w, BUTTON_H, false, () -> {
			firstTipout = false;
			go(Step.REPORT);
		});
		y += BUTTON_H + 30;
		button(c, "Back", x, y, w, 44, false, () -> go(Step.NAMES));
	}

	private void paintReport(UiCanvas c, float x, float w) {
		float y = title(c, "Current report", x, TOP);
		y = paragraph(c, "Totals on the sales report printed right now.", x, y, w);
		y += 8;
		y = labeledField(c, cash, "Cash tips", x, y, w);
		y = labeledField(c, credit, "Credit card tips", x, y, w);
		y = labeledField(c, uber, "Uber Eats", x, y, w);
		y = errorText(c, x, y + 4, w);
		backAndNext(c, x, y + 6, w, firstTipout ? "Calculate" : "Next", () -> go(Step.FIRST_OR_LATER),
				this::reportNext);
	}

	private void paintPrevious(UiCanvas c, float x, float w) {
		float y = title(c, "Last tipout", x, TOP);
		y = paragraph(c, "Copy these from the sheet.", x, y, w);
		y += 8;
		y = labeledField(c, prevCash, "Last report: cash tips", x, y, w);
		y = labeledField(c, prevCredit, "Last report: credit card tips", x, y, w);
		y = labeledField(c, prevUber, "Last report: Uber Eats", x, y, w);
		y = labeledField(c, prevTotal, "Last total take (per person)", x, y, w);
		y = errorText(c, x, y + 4, w);
		backAndNext(c, x, y + 6, w, "Calculate", () -> go(Step.REPORT), this::calculate);
	}

	private void paintResult(UiCanvas c, float x, float w) {
		TipoutCalculator.Result r = result;
		float y = title(c, "Write this down", x, TOP);
		c.drawText(resultDate + "   " + resultTime, x, y + 20, 18, true, TEXT, UiCanvas.ALIGN_LEFT);
		y += 34;
		y = paragraph(c, r.teamSize + (r.teamSize == 1 ? " person: " : " people: ") + String.join(", ", r.present),
				x, y, w);
		y += 6;
		c.fillRect(x, y, w, 1, DIVIDER);
		y += 16;
		String n = Integer.toString(r.teamSize);
		String period = firstTipout ? "" : " since last report";
		c.drawText("Each person gets" + period, x, y + 14, 15, false, MUTED, UiCanvas.ALIGN_LEFT);
		y += 28;
		y = amountRow(c, "Cash tips", r.cashShare, Money.format(r.periodSales.cashTips) + " \u00F7 " + n, x, y, w);
		y = amountRow(c, "Credit card tips", r.creditCardShare,
				Money.format(r.periodSales.creditCardTips) + " \u00F7 " + n, x, y, w);
		y = amountRow(c, "Uber Eats", r.uberEatsShare,
				Money.format(r.periodSales.uberEats) + " \u00D7 0.0739 \u00F7 " + n, x, y, w);
		c.fillRect(x, y, w, 1, DIVIDER);
		y += 14;
		c.drawText("This take", x, y + 30, 22, true, TEXT, UiCanvas.ALIGN_LEFT);
		c.drawText(Money.format(r.thisTake), x + w, y + 30, 30, true, ACCENT, UiCanvas.ALIGN_RIGHT);
		y += 46;
		c.drawText("Total so far", x, y + 30, 22, true, TEXT, UiCanvas.ALIGN_LEFT);
		c.drawText(Money.format(r.totalSoFar), x + w, y + 30, 30, true, TEXT, UiCanvas.ALIGN_RIGHT);
		y += 64;
		backAndNext(c, x, y, w, "New tipout", () -> go(firstTipout ? Step.REPORT : Step.PREVIOUS), () -> {
			input.blurAll();
			reset();
		});
	}

	// ---------------------------------------------------------------- widgets

	private float title(UiCanvas c, String text, float x, float y) {
		c.drawText(text, x, y + 30, 30, true, TEXT, UiCanvas.ALIGN_LEFT);
		return y + 46;
	}

	private float paragraph(UiCanvas c, String text, float x, float y, float w) {
		return wrapText(c, text, x, y, w, false, MUTED);
	}

	private float errorText(UiCanvas c, float x, float y, float w) {
		return error == null ? y : wrapText(c, error, x, y, w, true, ERROR) + 6;
	}

	/** Word-wrapped 16px text starting at top y; returns the y below the last line. */
	private float wrapText(UiCanvas c, String text, float x, float y, float w, boolean bold, int argb) {
		final float size = 16, lineH = 22;
		String line = "";
		for (String word : text.split(" ")) {
			String candidate = line.isEmpty() ? word : line + " " + word;
			if (!line.isEmpty() && c.measureText(candidate, size, bold) > w) {
				c.drawText(line, x, y + 16, size, bold, argb, UiCanvas.ALIGN_LEFT);
				y += lineH;
				line = word;
			} else {
				line = candidate;
			}
		}
		if (!line.isEmpty()) {
			c.drawText(line, x, y + 16, size, bold, argb, UiCanvas.ALIGN_LEFT);
			y += lineH;
		}
		return y;
	}

	private float labeledField(UiCanvas c, TextField f, String label, float x, float y, float w) {
		c.drawText(label, x, y + 15, 15, false, MUTED, UiCanvas.ALIGN_LEFT);
		layoutField(c, f, x, y + 22, w, FIELD_H, "0.00");
		return y + 22 + FIELD_H + 14;
	}

	private void layoutField(UiCanvas c, TextField f, float x, float y, float w, float h, String placeholder) {
		f.x = x; f.y = y; f.w = w; f.h = h;
		visibleFields.add(f);
		c.fillRoundRect(x, y, w, h, 10, FIELD_BG);
		c.strokeRoundRect(x, y, w, h, 10, f.focused ? 2.5f : 1.5f, f.focused ? ACCENT : FIELD_BORDER);
		float textX = x + 14;
		float baseline = y + h * 0.5f + 7;
		if (f.kind == TextField.Kind.MONEY) {
			c.drawText("$", textX, baseline, 20, true, MUTED, UiCanvas.ALIGN_LEFT);
			textX += c.measureText("$", 20, true) + 6;
		}
		if (f.text.isEmpty() && !f.focused) {
			c.drawText(placeholder, textX, baseline, 20, false, 0xFF6C6C80, UiCanvas.ALIGN_LEFT);
		} else {
			c.drawText(f.text, textX, baseline, 20, false, TEXT, UiCanvas.ALIGN_LEFT);
		}
		if (f.focused && (System.currentTimeMillis() / 500) % 2 == 0) {
			float caretX = textX + c.measureText(f.text, 20, false) + 1;
			c.fillRect(caretX, y + 12, 2, h - 24, ACCENT);
		}
	}

	private void button(UiCanvas c, String label, float x, float y, float w, float h, boolean primary, Runnable action) {
		buttons.add(new Button(x, y, w, h, action));
		if (primary) {
			c.fillRoundRect(x, y, w, h, 12, ACCENT);
			c.drawText(label, x + w * 0.5f, y + h * 0.5f + 7, 19, true, DARK_TEXT, UiCanvas.ALIGN_CENTER);
		} else {
			c.fillRoundRect(x, y, w, h, 12, 0x33FFFFFF);
			c.strokeRoundRect(x, y, w, h, 12, 1.5f, 0x88FFFFFF);
			c.drawText(label, x + w * 0.5f, y + h * 0.5f + 7, 18, true, TEXT, UiCanvas.ALIGN_CENTER);
		}
	}

	private void backAndNext(UiCanvas c, float x, float y, float w, String nextLabel, Runnable back, Runnable next) {
		float backW = w * 0.34f;
		button(c, "Back", x, y, backW, BUTTON_H, false, back);
		button(c, nextLabel, x + backW + 12, y, w - backW - 12, BUTTON_H, true, next);
	}

	private float amountRow(UiCanvas c, String label, long cents, String math, float x, float y, float w) {
		c.drawText(label, x, y + 20, 19, true, TEXT, UiCanvas.ALIGN_LEFT);
		c.drawText(Money.format(cents), x + w, y + 20, 22, true, TEXT, UiCanvas.ALIGN_RIGHT);
		c.drawText(math, x, y + 42, 14, false, MUTED, UiCanvas.ALIGN_LEFT);
		return y + 58;
	}
}
