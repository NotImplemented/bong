@FunctionalInterface
public interface IParserConsumer {

    public void onPageParse(String url, String text, Parser parser);
}