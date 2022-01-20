package com.sanger.overlapSnifflesPindel;

import java.util.*;
import java.io.*;
import java.util.stream.*;
import java.util.zip.*;
import java.nio.file.*;


public class GZIPFiles {
	public static Stream<String> lines(Path path) {
		InputStream fileIs = null;
		BufferedInputStream bufferedIs = null;
		GZIPInputStream gzipIs = null;
		try {
			fileIs = Files.newInputStream(path);
			bufferedIs = new BufferedInputStream(fileIs, 65535);
			gzipIs = new GZIPInputStream(bufferedIs);
		} catch (IOException e) {
			closeSafely(gzipIs);
			closeSafely(bufferedIs);
			closeSafely(fileIs);
			throw new UncheckedIOException(e);
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(gzipIs));
		return reader.lines().onClose(() -> closeSafely(reader));
	}

	private static void closeSafely(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
	        // Ignore
			}
		}
	}
}