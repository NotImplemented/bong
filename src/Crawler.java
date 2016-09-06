import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Crawler {

    private static ThreadPoolExecutor executor = null;
    private ArrayList<ICrawlerConsumer> consumers = new ArrayList<>();
    private AtomicReference<Thread> t = null;
    private final int threads;

    public Crawler(int threads) {

        this.threads = threads;
    }

    public void start() {

        if (executor != null)
            return;

        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
    }

    private void crawl(final Crawler crawler, final String url) {

        executor.execute(new Runnable() {
            @Override
            public void run() {

                try {

                    URL resource = new URL(url);
                    HttpURLConnection connection;
                    String location;

                    while (true) {

                        connection = (HttpURLConnection) resource.openConnection();
                        if (connection == null)
                            break;

                        connection.setInstanceFollowRedirects(false);
                        switch (connection.getResponseCode()) {
                            case HttpURLConnection.HTTP_MOVED_PERM:
                            case HttpURLConnection.HTTP_MOVED_TEMP:
                                location = connection.getHeaderField("Location");

                                //resource = new URL(resource, location);
                                URL next = new URL(resource, location);
                                resource = new URL(next.toExternalForm());

                                continue;
                        }

                        break;
                    }

                    if (connection == null)
                        return;

                    if (crawler.stopped())
                        return;

                    InputStream input = connection.getInputStream();
                    StringBuffer builder = new StringBuffer();

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                        String line;

                        while(true) {

                            line = reader.readLine();

                            if (line == null || line.length() == 0)
                                break;

                            builder.append(line);
                        }
                    }

                    if (builder.length() > 0) {

                        for (ICrawlerConsumer consumer : consumers)
                            consumer.onPageDownload(crawler, url, builder.toString());
                    }
                }
                catch (MalformedURLException e) {

                    e.printStackTrace();
                }
                catch (IOException e) {

                    // Suppress this from flooding console.
                }
            }

        });
    }

    public void stop() {

        executor.getQueue().clear();
        executor.shutdown();

    }

    public boolean stopped() {

        return executor.isTerminating() || executor.isTerminated();
    }

    public void add(String url) {

        crawl(this, url);
    }

    public void addCallback(ICrawlerConsumer consumer) {

        consumers.add(consumer);
    }
}
