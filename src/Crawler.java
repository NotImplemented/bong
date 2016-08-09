import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Crawler {

    private BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private AtomicReference<Thread> t = new AtomicReference<>();
    private ArrayList<ICrawlerConsumer> consumers = new ArrayList<>();

    public void start() {

        if (t.get() != null)
            return;

        t = new AtomicReference<>(new Thread(this::workingThread));
        t.get().start();
    }

    private void workingThread() {

        while (true) {

            try {

                String url = queue.take();
                String result = crawl(url);

                if (result.length() > 0) {

                    for(ICrawlerConsumer consumer : consumers)
                        consumer.onPageDownload(this, url, result);
                }
            }
            catch (InterruptedException e) {

                break;
            }
            catch (Exception e) {

                e.printStackTrace();
                break;
            }
        }

    }

    private String crawl(String url) {

        URL resource, current, next;
        HttpURLConnection connection = null;
        String location;

        while (true)
        {
            try {

                resource = new URL(url);
                connection = (HttpURLConnection)resource.openConnection();
                if (connection == null)
                    break;

                connection.setInstanceFollowRedirects(false);
                switch (connection.getResponseCode())
                {
                    case HttpURLConnection.HTTP_MOVED_PERM:
                    case HttpURLConnection.HTTP_MOVED_TEMP:
                        location = connection.getHeaderField("Location");
                        current = new URL(url);
                        next = new URL(current, location);
                        url = next.toExternalForm();
                        continue;
                }
            }
            catch (MalformedURLException e) {

                //e.printStackTrace();
            }
            catch (IOException e) {

                //e.printStackTrace();
                // Suppress this from flooding console.
            }

            break;
        }

        if (connection == null)
            return "";

        try {

            InputStream input = connection.getInputStream();
            StringBuffer builder = new StringBuffer();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                String line = "";

                while(true) {

                    line = reader.readLine();

                    if (line == null || line.length() == 0)
                        break;

                    builder.append(line);
                }
            }

            return builder.toString();
        }
        catch (IOException e) {

            //e.printStackTrace();
        }

        return "";
    }

    public void stop() {

        if (t.get() == null)
            return;

        t.get().interrupt();
    }

    public void add(String url) {

        queue.add(url);
    }

    public void addCallback(ICrawlerConsumer consumer) {

        consumers.add(consumer);
    }
}
