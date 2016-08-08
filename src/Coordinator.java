import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Coordinator {

    private static final String crawlCommand = "crawl";
    private static final String queryCommand = "query";
    private static final String exitCommand = "exitCommand";

    private static final Map<Crawler, Set<String>> crawlers = new HashMap<Crawler, Set<String>>();
    private static final Map<Crawler, Integer> limits = new HashMap<Crawler, Integer>();
    private static final Map<String, Crawler> pages = new HashMap<String, Crawler>();

    public static void displayHint() {

        System.out.println("Available commands are:");
        System.out.println("\n");
        System.out.println("\tcrawl <list of roots> [<amount of pages>]");
        System.out.println("\tquery <query>");
        System.out.println("\texit");
    }

    public static void main(String[] args) throws IOException {

        displayHint();

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        while (true) {

            System.out.print("\n>");
            String line = input.readLine();
            String[] splitted = line.split("\\s+");

            if (splitted.length > 1) {

                String command = splitted[0];

                if (command.equals(crawlCommand)) {

                    ArrayList<String> roots = new ArrayList<String>();
                    int amount = Integer.MAX_VALUE;
                    for (int i = 1; i < splitted.length; ++i) {

                        if (splitted[i].startsWith("http"))
                            roots.add(splitted[i]);
                        else
                            amount = Integer.parseInt(splitted[i]);
                    }

                    crawl(roots, amount);
                    System.out.println("Crawling started. Querying is available immediately.");

                } else if (command.equals(queryCommand)) {

                    String query = line.substring(line.indexOf(' ') + 1);
                    ArrayList<Pair<String, Double>> result = find(query);

                    System.out.println("Query: \"" + query + "\": ");
                    System.out.println("\n");

                    for (int i = 0; i < result.size(); ++i)
                        System.out.println("\t" + result.get(i).getKey() + " -> " + String.format("%.4f", result.get(i).getValue()));
                }
                else if (command.equals(exitCommand)) {

                    break;
                }
            }
        }
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


    private static void onPageParse(String url, String text, Parser parser) {

        Index.append(url, parser.getWords());

        synchronized (crawlers) {

            Crawler crawler = pages.get(url);
            ArrayList<String> links = parser.getLinks();
            for(String link : links) {

                if (link.startsWith("http:")) {

                    if (!pages.containsKey(link)) {

                        pages.put(link, crawler);
                        crawler.add(link);
                    }
                }
                else if (link.startsWith("/")){

                    String local = url + link;
                    if (!pages.containsKey(local)) {

                        pages.put(local, crawler);
                        crawler.add(local);
                    }
                }
            }
        }
    }

    private static ArrayList<Pair<String, Double>> find(String query) {

        return Index.query(query);
    }
}