package com.plexobject.mock.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.apache.log4j.Logger;


public class FileUtils {
	private static final Logger LOGGER = Logger.getLogger(FileUtils.class);

	private static final File DIR = new File(System.getProperty("MOCK_FOLDER",
			"."));
	private static final Random random = new Random();
	private final static String SKIP_PATTERN = System
			.getProperty(
					"skip.pattern",
					"(<authToken>[0-9a-z\\-]+<.authToken>|<twoFactorChallenge>[0-9a-z\\-]+<.twoFactorChallenge>)");

	private static String normalize(String name) {
		return name.replaceAll("[\\&\\/\\?:;,\\s]", "_");
	}

	public static void write(byte[] data, String fileName) throws IOException {
		DIR.mkdirs();
		File path = new File(DIR, normalize(fileName));
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(path));
		out.write(data);
		out.close();
	}

	public static byte[] read(InputStream in) throws IOException {
		byte[] buffer = new byte[8192];
		ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
		int len;
		while ((len = in.read(buffer)) != -1) {
			out.write(buffer, 0, len);
		}
		in.close();
		out.close();
		return out.toByteArray();
	}

	public static byte[] read(String fileName) throws IOException {
		DIR.mkdirs();
		File path = new File(DIR, fileName);
		if (!path.exists() || !path.canRead()) {
			throw new FileNotFoundException("Could not read " + path);
		}
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(
				path));
		return read(in);
	}

	public static void writeObject(Object obj, String name) {
		DIR.mkdirs();
		final String fileName = normalize(name);

		File path = new File(DIR, fileName + (System.currentTimeMillis() % 10));
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(new BufferedOutputStream(
					new FileOutputStream(path)));
			out.writeObject(obj);
			out.close();
		} catch (IOException e) {
			System.err.println("Failed to write filename " + fileName);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private static int random(int max) {
		return Math.abs(Math.abs(random.nextInt()) % max);
	}

	public static Object readObject(final String name) {
		DIR.mkdirs();
		final String fileName = normalize(name);
		File[] files = DIR.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(fileName);
			}
		});

		// File path = new File(DIR, fileName);

		File path = files != null && files.length > 0 ? files[random(files.length)]
				: null;

		if (path == null || !path.exists() || !path.canRead()) {
			LOGGER.info("Path " + path + " for name " + name + " not found on "
					+ DIR);
			return null;
		}

		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new BufferedInputStream(
					new FileInputStream(path)));
			return in.readObject();
		} catch (IOException e) {
			System.err.println("Failed to read filename " + fileName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

	public static String hash(final String text) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] bytes = text.toLowerCase().replace(SKIP_PATTERN, "")
					.getBytes();
			md.update(bytes, 0, bytes.length);
			final byte[] sha1hash = md.digest();
			return convertToHex(sha1hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Failed to create hash for " + text, e);
		}
	}

	private static String convertToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9)) {
					buf.append((char) ('0' + halfbyte));
				} else {
					buf.append((char) ('a' + (halfbyte - 10)));
				}
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

}
