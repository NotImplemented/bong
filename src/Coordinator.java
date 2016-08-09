import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class Coordinator {

    private static final String crawlCommand = "crawl";
    private static final int crawlAmount = 4096;
    private static final String queryCommand = "query";
    private static final int queryAmount = 8;
    private static final String browseCommand = "browse";
    private static final String infoCommand = "info";
    private static final String exitCommand = "exit";

    private static final Map<Crawler, Set<String>> crawlers = new HashMap<Crawler, Set<String>>();
    private static final Map<Crawler, Integer> limits = new HashMap<Crawler, Integer>();
    private static final Map<String, Crawler> pages = new HashMap<String, Crawler>();

    public static void displayHint() {

        System.out.println("Available commands are:");
        System.out.println("\tcrawl <list of roots> [<amount of pages> = " + crawlAmount +"]");
        System.out.println("\tquery <query> [<display only N top> = " + queryAmount + "]");
        System.out.println("\tinfo");
        System.out.println("\tbrowse [<url regular expression>]");
        System.out.println("\texit");
    }

    public static void main(String[] args) throws IOException {

        displayHint();

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        while (true) {

            System.out.print("\n>");
            String line = input.readLine();
            String[] splitted = line.split("\\s+");

            if (splitted.length >= 1) {

                String command = splitted[0].toLowerCase();

                if (command.equals(crawlCommand)) {

                    ArrayList<String> roots = new ArrayList<String>();
                    int amount = crawlAmount;
                    for (int i = 1; i < splitted.length; ++i) {

                        if (i == splitted.length - 1 && tryParseInt(splitted[i])) {
                            amount = Integer.parseInt(splitted[i]);
                            continue;
                        }

                        if (validURL(splitted[i]))
                            roots.add(splitted[i]);
                        else if (validURL("http://" + splitted[i]))
                            roots.add("http://" + splitted[i]);
                    }

                    crawl(roots, amount);
                    System.out.println("Crawling has started. Querying is available immediately.");

                }
                else if (command.equals(queryCommand)) {

                    StringBuilder query = new StringBuilder();
                    int amount = queryAmount;

                    for(int i = 1; i < splitted.length; ++i) {

                        if (tryParseInt(splitted[i]))
                            amount = Integer.parseInt(splitted[i]);
                        else if (query.length() > 0)
                            query.append(" " + splitted[i]);
                        else
                            query.append(splitted[i]);
                    }

                    List<Pair<String, Double>> result = find(query.toString(), amount);

                    System.out.println("Query \"" + query.toString() + "\"" +  " (displaying top " + amount + ")" + ":");
                    System.out.println("\n");

                    for (int i = 0; i < result.size(); ++i) {

                        String url = result.get(i).getKey();
                        String title = Index.getTitle(url);
                        System.out.println("\t" + url + " - " + title + " = " + String.format("%.4f", result.get(i).getValue()));
                    }

                }
                else if (command.equals(browseCommand)) {

                    // TODO: Remove the feature or add amount and output next items.
                    int amount = Integer.MAX_VALUE;
                    List<Pair<String, List<String>>> result = browse();

                    System.out.println("Index:");
                    System.out.println("\n");

                    for (int i = 0; i < result.size(); ++i)
                        System.out.println("\t" + result.get(i).getKey() + " -> " + "{" + commaSeparated(result.get(i).getValue()) + "}" );
                }
                else if (command.equals(exitCommand)) {

                    break;
                }
                else if (command.equals(infoCommand)) {

                    System.out.println("Index contains " + Index.getSize() + " pages.");
                    System.out.println("\n");
                }
            }
        }

        for(Crawler crawler : crawlers.keySet())
            crawler.stop();

        Parser.stop();
    }

    private static void crawl(ArrayList<String> roots, int amount) {

        synchronized (crawlers) {

            Crawler crawler = new Crawler();
            limits.put(crawler, amount);
            crawler.addCallback(Coordinator::onPageDownload);

            for (String root : roots) {

                pages.put(root, crawler);
                crawler.add(root);
            }

            crawler.start();
        }
    }

    private static void onPageDownload(Crawler crawler, String url, String content) {

        synchronized (crawlers) {

            Set<String> values = crawlers.get(crawler);
            if (values == null) {

                values = new HashSet<String>();
                values.add(url);
                crawlers.put(crawler, values);
            }
            else
                values.add(url);

            Integer limit = limits.get(crawler);
            if (values.size() >= limit) {
                crawler.stop();

                System.out.println("Crawling completed. Crawled " + values.size() + " pages.");
            }
        }

        Parser.parse(url, content, Coordinator::onPageParse);
    }

    private static boolean resourceLink(String link) {

        // TODO: crack url properly
        String[] extentions = {"css", "js", "png", "jpg", "gif", "ico"};

        for (String extention : extentions)
            if (link.endsWith("." + extention))
                return true;

        return false;
    }

    private static void onPageParse(String url, String text, Parser parser) {

        Index.append(url, parser.getTitle(), parser.getWords());

        synchronized (crawlers) {

            Crawler crawler = pages.get(url);
            ArrayList<String> links = parser.getLinks();
            for(String link : links) {

                if (link.startsWith("http:")) {

                    if (link.endsWith("/"))
                        link = link.replaceAll("/*$", "");

                    if (!resourceLink(link) && !pages.containsKey(link)) {

                        pages.put(link, crawler);
                        crawler.add(link);
                    }
                }
                else if (link.startsWith("/")){

                    String local_link = url + link;

                    if (local_link.endsWith("/"))
                        local_link = local_link.replaceAll("/*$", "");

                    if (!resourceLink(local_link) && !pages.containsKey(local_link)) {

                        pages.put(local_link, crawler);
                        crawler.add(local_link);
                    }
                }
            }
        }
    }

    private static List<Pair<String, Double>> find(String query, int amount) {

        return Index.query(query, amount);
    }

    private static List<Pair<String, List<String>>> browse() {

        return Index.browse();
    }

    private static boolean validURL(String address) {

        URL url = null;

        try {

            url = new URL(address);
        }
        catch (MalformedURLException e) {

            return false;
        }

        try {

            url.toURI();
        }
        catch (URISyntaxException e) {

            return false;
        }

        return true;
    }

    private static String commaSeparated(List<String> values) {

        StringBuilder builder = new StringBuilder();

        for(String word : values) {

            if (builder.length() == 0)
                builder.append(word);
            else
                builder.append(", " + word);
        }

        return builder.toString();
    }

    private static boolean tryParseInt(String value) {

        try {

            Integer.parseInt(value);

            return true;
        }
        catch (NumberFormatException e) {

            return false;
        }
    }
}