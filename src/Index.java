import javafx.util.Pair;

import java.util.*;

public class Index {

    private static HashMap<String, ArrayList<String>> index = new HashMap<>();
    //private static HashMap<String, String> names = new HashMap<>();

    public static void append(String url, ArrayList<String> words) {

        synchronized (index) {

            index.put(url, words);
        }
    }

    public static ArrayList<Pair<String, Double>> query(String text) {

        ArrayList<Pair<String, Double>> result = new ArrayList<>();

        synchronized (index) {

            String[] splitted = text.split("\\s+");

            for (Map.Entry<String, ArrayList<String>> entry : index.entrySet()) {

                String key = entry.getKey();
                ArrayList<String> values = entry.getValue();
                result.add(new Pair(key, 1.0));
            }

            // Remove it.
            for (String word : splitted) {

                double tf = tf(word);

                for (int i = 0; i < result.size(); ++i) {

                    double itf = 1 + itf(word, index.get(result.get(i).getKey()));

                    if (!Double.isNaN(tf / itf) && tf > 0) {

                        Double d = result.get(i).getValue();
                        d *= itf / tf;

                        result.set(i, new Pair(result.get(i).getKey(), d));
                    }
                    else {

                        result.set(i, new Pair(result.get(i).getKey(), 0.0));
                    }
                }
            }

            Collections.sort(result, new Comparator<Pair<String, Double>>() {
                @Override
                public int compare(Pair<String, Double> apple, Pair<String, Double> banana) {

                    return banana.getValue().compareTo(apple.getValue());
                }
            });

            return result;
        }
    }

    private static int itf(String word, ArrayList<String> values) {

        int result = 0;

        for(String w : values)
            if (word.equals(w))
                result++;

        return result;
    }

    private static int tf(String word) {

        int result = 0;
        for (Map.Entry<String, ArrayList<String>> entry : index.entrySet()) {
            //String key = entry.getKey();
            ArrayList<String> values = entry.getValue();

            for(String w : values)
                if (word.equals(w))
                    result++;
        }

        return result;
    }
}
