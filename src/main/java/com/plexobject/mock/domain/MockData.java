package com.plexobject.mock.domain;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import com.plexobject.mock.util.FileUtils;

/**
 * This is a helper class for creating mock data
 * 
 * @author shahzad bhatti
 *
 */

public class MockData {
    private static final String CONTENT_TYPE = "Content-Type:";

    private static final String CONTENT_DISPOSITION = "Content-Disposition:";

    private static final String SEPARATOR = "--------------------------";

    public static final boolean[] BOOLEANS = new boolean[] { true, false };

    public static final String[] CITIES = new String[] { "Paris", "London",
            "Chicago", "Karachi", "Tokyo", "Lagos", "Delhi", "Shanghai",
            "Mexico City", "Cairo", "Beijing", "Dhaka", "Osaka", "Buenos Aires",
            "Chongqing", "Istanbul", "Kolkata", "Manila", "Rio de Janeiro",
            "Tianjin", "Kinshasa", "Guangzhou", "Los Angeles", "Moscow",
            "Shenzhen", "Lahore", "Bangalore", "Paris", "Bogotá", "Jakarta",
            "Chennai", "Lima", "Bangkok", "Seoul", "Nagoya", "Hyderabad",
            "Chengdu", "Nanjing", "Wuhan", "Ho Chi Minh City", "Luanda",
            "Ahmedabad", "Kuala Lumpur", "Xi'an", "Hong Kong", "Dongguan",
            "Hangzhou", "Foshan", "Shenyang", "Riyadh", "Baghdad", "Santiago",
            "Surat", "Madrid", "Suzhou", "Washington, D.C.", "New York City",
            "Pune", "Harbin", "Houston", "Dallas", "Toronto", "Dar es Salaam",
            "Miami", "Belo Horizonte", "Singapore", "Philadelphia", "Atlanta",
            "Fukuoka", "Khartoum", "Barcelona", "Johannesburg", "Tehran",
            "Saint Petersburg", "Qingdao", "Dalian", "Yangon", "Alexandria",
            "Jinan", "Guadalajara" };

    public static final String[] ANDROID_MODELS = new String[] { "Nexus One",
            "Nexus S", "Nexus 4", "Nexus 5", "Nexus 6", "Nexus 5X", "Nexus 6P",
            "Nexus 7", "Nexus 10", "Nexus 9", "Nexus Q", };

    public static final String[] ANDROID_VERSIONS = new String[] { "4.0",
            "4.0.1", "4.0.2", "4.0.3", "4.0.4", "4.1", "4.1.1", "4.1.2", "4.2",
            "4.2.1", "4.2.2", "4.3", "4.3.1", "4.4", "4.4.1", "4.4.2", "4.4.3",
            "4.4.4", "4.4W", "4.4W.1", "4.4W.2", "5.0", "5.0.1", "5.0.2", "5.1",
            "5.1.1", "6.0", "6.0.1", "7.0", "7.1", "7.1.1", "7.1.2", "8.0",
            "8.1.0", "9.0", "10.0", };

    public static final String[] IOS_MODELS = new String[] { "iPhone 3G",
            "iPhone 3GS", "iPhone 4", "iPhone 4S", "iPhone 5", "iPhone 5C",
            "iPhone 5S", "iPhone 6", "iPhone 6 Plus", "iPhone 6S",
            "iPhone 6S Plus", "iPhone SE", "iPhone 7", "iPhone 7 Plus",
            "iPhone 8", "iPhone 8 Plus", "iPhone X", "iPhone XS",
            "iPhone XS Max", "iPhone XR", "iPhone 11", "iPhone 11 Pro",
            "iPhone 11 Pro Max", "iPhone SE", };

    public static final String[] IOS_VERSIONS = new String[] { "5.0", "5.1",
            "6", "6.0", "6.1", "7", "7.0", "7.1", "8", "8.0", "8.1", "8.2",
            "8.3", "8.4", "9", "9.0", "9.1", "9.2", "9.3", "10", "10.0", "10.1",
            "10.2", "10.3", "11", "11.0", "11.1", "11.2", "11.3", "11.4", "12",
            "12.0", "12.1", "12.2", "12.3", "12.4", "13", "13.0", "13.1",
            "13.2", "13.3", "13.4", "13.5", "13.6", "13.7", "14", "14.0", };

    public static final String[] DOMAINS = new String[] { "@gmail.com",
            "@yahoo.com", "@mail.com", "@outlook.com", "@bitvaulet.com",
            "@xyz.com", "@abc.com" };

    public static final String[] NAMES = new String[] { "Aaron", "Abigail",
            "Adam", "Alan", "Albert", "Alexander", "Alexis", "Alice", "Amanda",
            "Amber", "Amy", "Andrea", "Andrew", "Angela", "Ann", "Anna",
            "Anthony", "Arthur", "Ashley", "Austin", "Barbara", "Benjamin",
            "Betty", "Beverly", "Billy", "Bobby", "Bradley", "Brandon",
            "Brenda", "Brian", "Brittany", "Bruce", "Bryan", "Carl", "Carol",
            "Carolyn", "Catherine", "Charles", "Cheryl", "Christian",
            "Christina", "Christine", "Christopher", "Cynthia", "Daniel",
            "Danielle", "David", "Deborah", "Debra", "Denise", "Dennis",
            "Diana", "Diane", "Donald", "Donna", "Doris", "Dorothy", "Douglas",
            "Dylan", "Edward", "Elizabeth", "Emily", "Emma", "Eric", "Ethan",
            "Eugene", "Evelyn", "Frances", "Frank", "Gabriel", "Gary", "George",
            "Gerald", "Gloria", "Grace", "Gregory", "Hannah", "Harold",
            "Heather", "Helen", "Henry", "Jack", "Jacob", "Jacqueline", "James",
            "Jane", "Janet", "Janice", "Jason", "Jean", "Jeffrey", "Jennifer",
            "Jeremy", "Jerry", "Jesse", "Jessica", "Joan", "Joe", "John",
            "Johnny", "Jonathan", "Jordan", "Jose", "Joseph", "Joshua", "Joyce",
            "Juan", "Judith", "Judy", "Julia", "Julie", "Justin", "Karen",
            "Katherine", "Kathleen", "Kathryn", "Kayla", "Keith", "Kelly",
            "Kenneth", "Kevin", "Kimberly", "Kyle", "Larry", "Laura", "Lauren",
            "Lawrence", "Linda", "Lisa", "Logan", "Lori", "Louis", "Madison",
            "Margaret", "Maria", "Marie", "Marilyn", "Mark", "Martha", "Mary",
            "Matthew", "Megan", "Melissa", "Michael", "Michelle", "Nancy",
            "Natalie", "Nathan", "Nicholas", "Nicole", "Noah", "Olivia",
            "Pamela", "Patricia", "Patrick", "Paul", "Peter", "Philip",
            "Rachel", "Ralph", "Randy", "Raymond", "Rebecca", "Richard",
            "Robert", "Roger", "Ronald", "Rose", "Roy", "Russell", "Ruth",
            "Ryan", "Samantha", "Samuel", "Sandra", "Sara", "Sarah", "Scott",
            "Sean", "Sharon", "Shirley", "Sophia", "Stephanie", "Stephen",
            "Steven", "Susan", "Teresa", "Terry", "Theresa", "Thomas",
            "Timothy", "Tyler", "Victoria", "Vincent", "Virginia", "Walter",
            "Wayne", "William", "Willie", "Zachary", };

    private Map<String, List<String>> textFiles = new HashMap<>();
    private Configuration config;

    public MockData(Configuration config) {
        this.config = config;
    }

    public int readLineInt(String name) throws IOException {
        return readLineInt(name, 0);
    }

    public int readLineInt(String name, long seed) throws IOException {
        String line = readLine(name, seed);
        if (line == null) {
            return 0;
        }
        return Integer.parseInt(line);
    }

    public String readLine(String name) throws IOException {
        return readLine(name, 0);
    }

    public String readLine(String name, long seed) throws IOException {
        synchronized (name.intern()) {
            List<String> lines = textFiles.get(name);
            if (lines == null) {
                File file = config.find(name);
                if (file == null) {
                    throw new IOException("Could not find " + name);
                }
                lines = FileUtils.readLines(file);
                // removing Content-Disposition / Content-Type line
                Iterator<String> it = lines.iterator();
                while (it.hasNext()) {
                    String next = it.next().trim();
                    if (next.isEmpty() || next.startsWith(SEPARATOR)
                            || next.startsWith(CONTENT_DISPOSITION)
                            || next.startsWith(CONTENT_TYPE)) {
                        it.remove();
                    }
                }
                textFiles.put(name, lines);
            }
            if (lines.size() == 0) {
                throw new IOException("Empty file fo " + name);
            }
            Random random = new Random();
            if (seed > 0) {
                random.setSeed(seed);
            }
            return lines.get(random.nextInt(lines.size()));
        }
    }

    public String uuid() {
        return UUID.randomUUID().toString();
    }

    public String uuid(String prefix, long n) {
        prefix = prefix.toLowerCase();
        if (prefix.length() > 10) {
            prefix = prefix.substring(0, 10);
        } else if (prefix.length() < 10) {
            while (prefix.length() < 10) {
                prefix = prefix + "0";
            }
        }
        prefix = prefix.replaceAll("[^0-9a-f]", "0");
        return prefix + "-0000-0000-0000-" + String.format("%12X", n + 100000)
                .replaceAll(" ", "0").toLowerCase();
    }

    public String name() {
        return name(0);
    }

    public String name(long seed) {
        Random random = new Random();
        if (seed > 0) {
            random.setSeed(seed);
        }
        return NAMES[random.nextInt(NAMES.length)];
    }

    public String string() {
        return string(20);
    }

    public String date() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        return df.format(new Date());
    }

    public boolean bool() {
        return bool(0);
    }
    public boolean bool(long seed) {
        Random random = new Random();
        if (seed > 0) {
            random.setSeed(seed);
        }
        return BOOLEANS[random.nextInt(BOOLEANS.length)];
    }

    public String androidModel() {
        return androidModel(0);
    }

    public String androidModel(long seed) {
        Random random = new Random();
        if (seed > 0) {
            random.setSeed(seed);
        }
        return ANDROID_MODELS[random.nextInt(ANDROID_MODELS.length)];
    }

    public String iosModel() {
        return iosModel(0);
    }

    public String iosModel(long seed) {
        Random random = new Random();
        if (seed > 0) {
            random.setSeed(seed);
        }
        return IOS_MODELS[random.nextInt(IOS_MODELS.length)];
    }

    public String androidVersion() {
        return androidVersion(0);
    }

    public String androidVersion(long seed) {
        Random random = new Random();
        if (seed > 0) {
            random.setSeed(seed);
        }
        return ANDROID_VERSIONS[random.nextInt(ANDROID_VERSIONS.length)];
    }

    public String iosVersion() {
        return iosVersion(0);
    }

    public String iosVersion(long seed) {
        Random random = new Random();
        if (seed > 0) {
            random.setSeed(seed);
        }
        return IOS_VERSIONS[random.nextInt(IOS_VERSIONS.length)];
    }

    public String email() {
        return email(0);
    }

    public String email(long seed) {
        Random random = new Random();
        if (seed > 0) {
            random.setSeed(seed);
        }
        String domain = DOMAINS[random.nextInt(DOMAINS.length)];
        return string(20) + domain;
    }

    public String string(int length) {
        return string(length, 0);
    }

    public String string(int length, long seed) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
        if (seed > 0) {
            random.setSeed(seed);
        }

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(length).collect(StringBuilder::new,
                        StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public String city(int max) {
        return city(max, 0);
    }

    public String city(int max, long seed) {
        Random random = new Random();
        if (seed > 0) {
            random.setSeed(seed);
        }
        return CITIES[random.nextInt(Math.min(max, CITIES.length))];
    }

    public String dollars() {
        return "$" + number(100, 10000);
    }

    public String percent() {
        return number(0, 100) + " %";
    }

    public int number() {
        return number(5000, 50000);
    }

    public int number(int min, int max) {
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
}
