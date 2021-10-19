import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

public class Topic {
    private ArrayList<InetSocketAddress> allSubscribers;
    private HashMap<String, ArrayList<InetSocketAddress>> subTopics;
    private HashMap<String, ArrayList<String>> groups;

    Topic(){
        subTopics = new HashMap<>();
        allSubscribers = new ArrayList<>();
        groups = new HashMap<>();
    }

    public void addGroup(String name){
        ArrayList<String> newList = new ArrayList<>();
        groups.put(name, newList);
    }

    public boolean addSubtopicToGroup(String subName, String groupName){
        if(groups.containsKey(groupName) && subTopics.containsKey(subName)) {
            ArrayList<String> list = groups.get(groupName);
            list.add(subName);
            groups.put(groupName, list);
            return true;
        }
        else return false;
    }

    public void addTopic(String name){
        ArrayList<InetSocketAddress> newList = new ArrayList<>();
        subTopics.put(name, newList);
    }

    public ArrayList<String> getGroupList(String groupName) {
        return groups.getOrDefault(groupName, null);
    }

    public ArrayList<InetSocketAddress> getSubscriberList(String name){
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

    public boolean subscribeToGroup(String name, InetSocketAddress newSub){
        if(groups.containsKey(name)){
            ArrayList<String> subtopics = groups.get(name);
            for(String i: subtopics) {
                ArrayList<InetSocketAddress> subList = getSubscriberList(i);
                subList.add(newSub);
                subTopics.put(i, subList);
            }
            return true;
        }
        return false;
    }

    public void unsubGroup(String name, InetSocketAddress sub){
        if(groups.containsKey(name)){
            ArrayList<String> subtopics = groups.get(name);
            for(String i: subtopics){
                ArrayList<InetSocketAddress> list = getSubscriberList(i);
                list.remove(sub);
                subTopics.put(i, list);
            }
        }
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