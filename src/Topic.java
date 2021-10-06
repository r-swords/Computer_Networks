import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

public class Topic {
    private ArrayList<ArrayList<InetSocketAddress>> subTopics;
    private ArrayList<InetSocketAddress> allSubscribers;
    HashMap<String, Integer> topNumbers;

    Topic(){
        subTopics = new ArrayList<>();
        allSubscribers = new ArrayList<>();
        topNumbers = new HashMap<>();
    }

    public void addTopic(String name){
        topNumbers.put(name, subTopics.size());
        ArrayList<InetSocketAddress> newList = new ArrayList<>();
        subTopics.add(newList);
    }

    public ArrayList<InetSocketAddress> getSubscriberList(int topicNo){
        return subTopics.get(topicNo);
    }

    public boolean addSubscriber(String name, InetSocketAddress newSub){
        if(topNumbers.containsKey(name)) {
            int topicNo = topNumbers.get(name);
            ArrayList<InetSocketAddress> subscribers = subTopics.get(topicNo);
            subscribers.add(newSub);
            subTopics.set(topicNo, subscribers);
            allSubscribers.add(newSub);
            return true;
        }
        else return false;
    }

    public ArrayList<InetSocketAddress> getAll(){
        return allSubscribers;
    }
}
