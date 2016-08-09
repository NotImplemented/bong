import javafx.util.Pair;

import java.util.*;

public class Index {

    private static HashMap<String, ArrayList<String>> index = new HashMap<>();
    private static HashMap<String, HashMap<String, Integer>> index_frequency = new HashMap<>();
    private static HashMap<String, Integer> frequency_global = new HashMap<>();
    private static HashMap<String, String> titles = new HashMap<>();

    public static void append(String url, String head, ArrayList<String> words) {

        HashMap<String, Integer> frequency = new HashMap<String, Integer>();
        for(String word : words) {

            if (!frequency.containsKey(word))
                frequency.put(word, 1);
            else
                frequency.replace(word, frequency.get(word)+1);
        }

        synchronized (titles) {
            titles.put(url, head);
        }

        synchronized (index) {
            index.put(url, words);
        }

        synchronized (index_frequency) {
            index_frequency.put(url, frequency);
        }

        synchronized (frequency_global) {

            for(String word : words) {

                if (!frequency_global.containsKey(word))
                    frequency_global.put(word, 1);
                else
                    frequency_global.replace(word, frequency.get(word)+1);
            }
        }
    }

    public static List<Pair<String, List<String>>> browse() {

        List<Pair<String, List<String>>> result = new ArrayList<>();

        synchronized (index) {

            for (Map.Entry<String, ArrayList<String>> entry : index.entrySet()) {

                result.add(new Pair<>(entry.getKey(), entry.getValue()));
            }

            return result;
        }
    }

    public static List<Pair<String, Double>> query(String text, int amount) {

        text = text.toLowerCase();

        List<Pair<String, Double>> result = new ArrayList<>();

        String[] splitted = text.split("\\s+");

        synchronized (index) {

            int N = index.size();

            for (Map.Entry<String, ArrayList<String>> entry : index.entrySet()) {

                double relevance = 0;
                String url = entry.getKey();

                for (String word : splitted) {

                    double tf = term_frequency(word, url);
                    double idf = document_frequency(word);

                    double d = tf * Math.log(N / (idf + 1));

                    relevance += d;
                }

                result.add(new Pair<>(url, relevance));
            }
        }

        // TODO: Apply selection algorithm generally or use heap to select top.
        Collections.sort(result, new Comparator<Pair<String, Double>>() {
            @Override
            public int compare(Pair<String, Double> apple, Pair<String, Double> banana) {

                return banana.getValue().compareTo(apple.getValue());
            }
        });

        if (result.size() > amount) {
            result = result.subList(0, amount-1);
        }

        return result;

    }

    public static String getTitle(String url) {

        synchronized (titles) {

            return titles.get(url);
        }
    }

    public static int getSize() {

        synchronized (index) {

            return index.size();
        }
    }

    private static int document_frequency(String word) {

        synchronized (index) {

            Integer frequency = frequency_global.get(word);
            if (frequency == null)
                return 0;

            return frequency;
        }
    }

    private static int term_frequency(String word, String url) {

        synchronized (index) {

            HashMap<String, Integer> frequency_info = index_frequency.get(url);
            if (frequency_info == null)
                return 0;

            Integer frequency = frequency_info.get(word);
            if (frequency == null)
                return 0;

            return frequency;
        }
    }
}
