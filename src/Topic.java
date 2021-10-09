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
            if(!subscribers.contains(newSub)) subscribers.add(newSub);
            subTopics.set(topicNo, subscribers);
            if(!allSubscribers.contains(newSub)) allSubscribers.add(newSub);
            return true;
        }
        else return false;
    }

    public void addSubscriber(InetSocketAddress newSub) {
        for(ArrayList<InetSocketAddress> i : subTopics){
            if(!i.contains(newSub))i.add(newSub);
        }
        if(!allSubscribers.contains(newSub))allSubscribers.add(newSub);
    }

    public void removeSubscriber(String name, InetSocketAddress removeSubscriber){
        if(topNumbers.containsKey(name)){
            int topNo = topNumbers.get(name);
            ArrayList<InetSocketAddress> subscribers = subTopics.get(topNo);
            subscribers.remove(removeSubscriber);
            subTopics.set(topNo, subscribers);
        }
    }

    public void removeSubscriber(InetSocketAddress removeSubscriber) {
        for(ArrayList<InetSocketAddress> i : subTopics){
            i.remove(removeSubscriber);
        }
        allSubscribers.remove(removeSubscriber);
    }

    public ArrayList<InetSocketAddress> getAll(){
        return allSubscribers;
    }
}
