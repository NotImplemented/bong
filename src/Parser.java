import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

// Looks really shitty.
public class Parser {

    private static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    private StringBuffer lexeme = new StringBuffer();
    private StringBuffer title   = new StringBuffer();
    private Boolean insideTag   = false;
    private Boolean insideTitle = false;
    private String tagName      = "";
    private String tagAttribute = "";

    private ArrayList<String> words = new ArrayList<>();
    private ArrayList<String> links = new ArrayList<>();

    public ArrayList<String> getWords() {

        return words;
    }
    public ArrayList<String> getLinks() {

        return links;
    }
    public String getTitle()
    {
        String[] splitted = title.toString().split("[^a-zA-Z0-9\\-]");
        if (splitted.length > 0)
            return splitted[0];

        return "";
    }

    public static void parse(String url, String text, IParserConsumer consumer) {

        executor.execute(new Runnable() {
            @Override
            public void run() {

                Parser parser = new Parser();
                parser.parse(text);

                consumer.onPageParse(url, text, parser);
            }
        });
    }

    private void handleTag(String tagName, String tagAttribute) {

        if (tagAttribute == "") {

            if (tagName.startsWith("/")) {

                if (tagName.replaceAll("^/", "").toLowerCase().equals("title"))
                    insideTitle = false;
            }
            else {

                if (tagName.toLowerCase().equals("title"))
                    insideTitle = true;
            }
        }
        else if (tagAttribute.startsWith("href")) {

            String link = tagAttribute.replaceAll("^href=\"", "").replaceAll("\"$", "");
            links.add(link);
        }
    }

    private void handleLexeme(String lexeme) {

        lexeme = lexeme.toLowerCase().replaceAll("[^A-Za-z0-9]", "");
        int l = lexeme.length();
        if (l <= 2 || l >= 14)
            return;

        words.add(lexeme);
    }

    private void parse(String text) {

        for(int i = 0; i < text.length(); ++i) {

            char c = text.charAt(i);

            switch (c) {

                case '<':
                    insideTag = true;

                    if (insideTitle)
                        insideTitle = false;

                    tagName = tagAttribute = "";
                    lexeme.setLength(0);

                    break;

                case '>':
                    insideTag = false;

                    if (tagName.length() > 0) {

                        tagAttribute = lexeme.toString();
                        handleTag(tagName, tagAttribute);
                    }
                    else {

                        tagName = lexeme.toString();
                        handleTag(tagName, "");
                    }

                    tagName = tagAttribute = "";
                    lexeme.setLength(0);

                    break;

                case ' ':

                    if (insideTag) {

                        if (tagName.length() > 0) {

                            tagAttribute = lexeme.toString();
                            handleTag(tagName, tagAttribute);
                            tagAttribute = "";
                        }
                        else {

                            tagName = lexeme.toString();
                            handleTag(tagName, "");
                        }
                    }
                    else {

                        handleLexeme(lexeme.toString());
                    }

                    lexeme.setLength(0);

                    break;

                default:

                    lexeme.append(c);
                    if (insideTitle)
                        title.append(c);

                    break;
            }
        }
    }

    public static void stop() {

        executor.shutdown();
    }
}
