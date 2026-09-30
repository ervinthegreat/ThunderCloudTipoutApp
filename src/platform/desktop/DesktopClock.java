package platform.desktop;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import ui.ClockSource;

public class DesktopClock implements ClockSource {
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

	@Override
	public String date() {return LocalDateTime.now().format(DATE);}

	@Override
	public String time() {return LocalDateTime.now().format(TIME);}
}
