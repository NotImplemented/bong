@FunctionalInterface
public interface ICrawlerConsumer {

    public void onPageDownload(Crawler crawler, String url, String contents);
}
