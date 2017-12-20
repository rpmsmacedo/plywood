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

	private final String folder;
	private final String prefix;
	private final ExecutorService executorService;

	public Press(@NonNull String folder, @Nullable String prefix, @Nullable ExecutorService executorService) {
		if (folder == null)
			throw new IllegalArgumentException("Missing destination folder");

		this.folder = folder;
		this.prefix = prefix == null ? "" : prefix;
		this.executorService = executorService == null ? Executors.newSingleThreadExecutor() : executorService;
	}

	@Override
	protected void log(final int priority, final String tag, final String message, final Throwable t) {
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
				String filename = date + ".log";
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

				FileOutputStream fos = null;
				PrintWriter printWriter = null;
				try {
					fos = new FileOutputStream(file, true);
					printWriter = new PrintWriter(fos);

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

					printWriter.println(line.toString());

					printWriter.flush();
					fos.flush();

				} catch (Exception e) {
					Log.e(tag, "logToSdCard error", e);
				} finally {
					if (fos != null) {
						try {
							fos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					if (printWriter != null) {
						printWriter.close();
					}
				}
			}
		});
	}
}
