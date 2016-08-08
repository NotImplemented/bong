public interface ICrawlerConsumer {

    public void OnPageDownload(String url, String contents);
}
