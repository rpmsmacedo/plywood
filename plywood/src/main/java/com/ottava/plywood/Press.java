package com.ottava.plywood;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

/**
 * Created by rmacedo on 14-08-2017.
 */

@SuppressWarnings({ "WeakerAccess", "unused" }) // Public API.
public class Press extends Timber.DebugTree {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");

	static {
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
		TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private final int priority;
	private final String folder;
	private final String prefix;
	private final ExecutorService executorService;

	/**
	 *
	 *
	 * @param priority minimum priority to accept logs
	 * @param folder folder where log files will be created
	 * @param prefix prefix for log file name: <prefix><yyyy-MM-dd>.log
	 * @param executorService log serialization
	 */
	public Press(int priority, @NonNull String folder, @Nullable String prefix, @Nullable ExecutorService executorService) {
		this.priority = priority;
		this.folder = folder;
		this.prefix = prefix == null ? "" : prefix;
		this.executorService = executorService == null ? Executors.newSingleThreadExecutor() : executorService;
	}

	@Override
	protected boolean isLoggable(String tag, int priority) {
		return priority > this.priority;
	}

	@Override
	protected void log(final int priority, @NonNull final String tag, @NonNull final String message, @NonNull final Throwable t) {
		if (!isLoggable(tag, priority)) {
			return;
		}

		executorService.submit(new Runnable() {
			@Override
			public void run() {
				if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					return;
				}

				File dir = new File(Environment.getExternalStorageDirectory() + File.separator + folder);
				if (!dir.exists()) {
					dir.mkdirs();
				}

				Long currMillis = System.currentTimeMillis();
				String date = DATE_FORMAT.format(currMillis);
				String filename = prefix + date + ".log";
				File file = new File(dir, filename);
				if (!file.exists()) {
					try {
						if (!file.createNewFile()) {
							return;
						}
					} catch (Exception e) {
						Log.e(tag, "logToSdCard error", e);
						return;
					}
				}

				try (FileOutputStream fos = new FileOutputStream(file, true);
				     PrintWriter printWriter = new PrintWriter(fos);) {

					printWriter.println(ply(currMillis, priority, tag, message));
					printWriter.flush();
					fos.flush();

				} catch (IOException e) {
					Log.e(tag, "logToSdCard error", e);
				}
			}
		});
	}

	@NonNull
	private String ply(Long currMillis, int priority, String tag, String message) {
		StringBuilder line = new StringBuilder();
		line.append(TIME_FORMAT.format(currMillis)).append(' ');
		switch (priority) {
			case Log.VERBOSE:
				line.append("V/");
				break;
			case Log.DEBUG:
				line.append("D/");
				break;
			case Log.INFO:
				line.append("I/");
				break;
			case Log.WARN:
				line.append("W/");
				break;
			case Log.ERROR:
				line.append("E/");
				break;
			case Log.ASSERT:
				line.append("A/");
				break;
		}
		line.append(tag).append(": ");
		line.append(message);
		return line.toString();
	}
}
