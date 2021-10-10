import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

public class Topic {
    private ArrayList<InetSocketAddress> allSubscribers;
    private HashMap<String, ArrayList<InetSocketAddress>> subTopics;

    Topic(){
        subTopics = new HashMap<>();
        allSubscribers = new ArrayList<>();
    }

    public void addTopic(String name){
        ArrayList<InetSocketAddress> newList = new ArrayList<>();
        subTopics.put(name, newList);
    }

    public ArrayList<InetSocketAddress> getSubscriberList(String name){
        ArrayList<InetSocketAddress> newList = subTopics.getOrDefault(name, null);
        return subTopics.getOrDefault(name, null);
    }

    public boolean addSubscriber(String name, InetSocketAddress newSub){
        if(subTopics.containsKey(name)) {
            ArrayList<InetSocketAddress> subscribers = subTopics.get(name);
            if(!subscribers.contains(newSub)) subscribers.add(newSub);
            subTopics.put(name, subscribers);
            if(!allSubscribers.contains(newSub)) allSubscribers.add(newSub);
            return true;
        }
        else return false;
    }

    public void addSubscriber(InetSocketAddress newSub) {
        for(ArrayList<InetSocketAddress> i : subTopics.values()){
            if(!i.contains(newSub))i.add(newSub);
        }
        if(!allSubscribers.contains(newSub))allSubscribers.add(newSub);
    }

    public void removeSubscriber(String name, InetSocketAddress removeSubscriber){
        if(subTopics.containsKey(name)){
            ArrayList<InetSocketAddress> subscribers = subTopics.get(name);
            subscribers.remove(removeSubscriber);
            subTopics.put(name, subscribers);
        }
    }

    public void removeSubscriber(InetSocketAddress removeSubscriber) {
        for(ArrayList<InetSocketAddress> i : subTopics.values()){
            i.remove(removeSubscriber);
        }
        allSubscribers.remove(removeSubscriber);
    }

    public ArrayList<InetSocketAddress> getAll(){
        return allSubscribers;
    }
}