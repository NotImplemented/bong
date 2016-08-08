import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

// Looks really shitty.
public class Parser {

    private static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    private StringBuffer lexeme = new StringBuffer();
    private StringBuffer head   = new StringBuffer();
    private Boolean insideTag   = false;
    private Boolean insideHead  = false;
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

                if (tagName.replaceAll("^/", "") == "HEAD")
                    insideHead = false;
            }
            else {

                if (tagName == "HEAD")
                    insideHead = true;
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
        if (l < 4 || l > 12)
            return;

        words.add(lexeme.toLowerCase().replaceAll("[^A-Za-z0-9]", ""));
    }

    private void parse(String text) {

        for(int i = 0; i < text.length(); ++i) {

            char c = text.charAt(i);

            switch (c) {

                case '<':
                    insideTag = true;

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
                    if (insideHead)
                        this.head.append(c);

                    break;
            }
        }
    }
}
