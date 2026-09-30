package ui;

/** Local date and time as the platform formats them, for writing on the tipout sheet. */
public interface ClockSource {
	/** e.g. "Tue, Sep 29, 2026" */
	String date();

	/** e.g. "8:16 PM" */
	String time();
}
