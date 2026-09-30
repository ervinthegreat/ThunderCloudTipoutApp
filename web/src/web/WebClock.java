package web;

import org.teavm.jso.JSBody;

import ui.ClockSource;

/** The phone's local date and time. */
public class WebClock implements ClockSource {
	@Override
	public String date() {
		return localDate();
	}

	@Override
	public String time() {
		return localTime();
	}

	@JSBody(script = "return new Date().toLocaleDateString('en-US',"
			+ " {weekday: 'short', month: 'short', day: 'numeric', year: 'numeric'});")
	private static native String localDate();

	@JSBody(script = "return new Date().toLocaleTimeString('en-US', {hour: 'numeric', minute: '2-digit'});")
	private static native String localTime();
}
