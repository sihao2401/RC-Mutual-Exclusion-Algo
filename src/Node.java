import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.distribution.*;

public class Node {
    public int totalNodeNum;
    public int myNodeID;
    public int myPort;
    private int interRequestDelay;
    private int csExecutionTime;
    public int numRequest;
    private List<Integer> outGoingNeighbor = new ArrayList<>();
    public Map<Integer, String[]> nodeInfoMap;
    private ConcurrentHashMap<Integer, Boolean> nodeKeys;
    public ConcurrentLinkedQueue<Message> deferredRequests = new ConcurrentLinkedQueue<>();
    public AtomicInteger maxTimestamp = new AtomicInteger(0);
    public AtomicInteger timestamp = new AtomicInteger(0);
    public volatile boolean inCS = false;
    public volatile boolean hasRequested = false;
    private ExponentialDistribution delayDistribution;
    private ExponentialDistribution csDistribution;
    public String path;
    public Tester tester;


    public void readConfig(String configPath) throws UnknownHostException {
        this.path = configPath.substring(0, configPath.lastIndexOf("/"));
        tester = new Tester(this, path);
        InetAddress localHostInfo = InetAddress.getLocalHost();
        String myHostName = localHostInfo.getHostName().split("\\.")[0];
        System.out.println("myHostName is " + myHostName);
        BufferedReader in = null;
        this.nodeInfoMap = new HashMap<>();
        List<String[]> words = new ArrayList<>();
        try {
            String str = null;
            in = new BufferedReader(new FileReader(configPath));
            while ((str = in.readLine()) != null) {
                if (str.startsWith("#") || str.length() == 0) {
                    continue;
                }
                String removedSignal = str.trim();
                if (str.indexOf("#") != -1) {
                    removedSignal = removedSignal.substring(0, str.indexOf("#"));
                }
                words.add(removedSignal.split(" "));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.totalNodeNum = Integer.parseInt(words.get(0)[0]);
        this.interRequestDelay = Integer.parseInt(words.get(0)[1]);
        this.csExecutionTime = Integer.parseInt(words.get(0)[2]);
        this.delayDistribution = new ExponentialDistribution(this.interRequestDelay);
        this.csDistribution = new ExponentialDistribution(this.csExecutionTime);
        this.numRequest = Integer.parseInt(words.get(0)[3]);
        for (int i = 1; i <= totalNodeNum; i++) {
            String[] s = words.get(i);
            int id = Integer.parseInt(s[0]);
            if (s[1].equals(myHostName)) {
                this.myNodeID = id;
                this.myPort = Integer.parseInt(s[2]);
            } else {
                this.outGoingNeighbor.add(id);
            }
            String[] info = new String[2];
            info[0] = s[1] + ".utdallas.edu";
            info[1] = s[2];
            nodeInfoMap.put(id, info);
        }
    }

    public void listen() {
        Listener listener = new Listener(this);
        Thread listener_thread = new Thread(listener);
        listener_thread.start();
    }

//    public void addOneTimestamp() {
//        this.maxTimestamp.addAndGet(1);
//    }

    public void sendReplyMsg(int dstID) {
//        addOneTimestamp();
        Message replyMsg = new Message(myNodeID, "REPLY", timestamp.get());
        String[] host = nodeInfoMap.get(dstID);
        sendMsg(replyMsg, host[0], Integer.parseInt(host[1]));
        tester.outputFile("ReplyCount" + "-" + myNodeID);
        System.out.println("[Send]" + " Send reply to " + dstID + " " + this.timestamp);
        sendKey(dstID);
    }

    public void sendRequestMsg(int dstID) {
        Message requestMsg = new Message(myNodeID, "REQUEST", timestamp.get());
        String[] host = nodeInfoMap.get(dstID);
        sendMsg(requestMsg, host[0], Integer.parseInt(host[1]));
        tester.outputFile("RequestCount" + "-" + myNodeID);
        System.out.println("[Send]" + " Send request to " + dstID + " " + this.timestamp);

    }

    public synchronized void sendRequest() {
        this.hasRequested = true;
//        this.addOneTimestamp();
        List<Integer> needKeys = requiredKey();
        if (needKeys != null) {
            for (int nodeId : needKeys) {
                sendRequestMsg(nodeId);
            }
        }
    }

    public void sendTerminate() {
        for (int dstID : outGoingNeighbor) {
            Message terminateMsg = new Message(myNodeID, "TERMINATE", timestamp.get());
            String[] host = nodeInfoMap.get(dstID);
            sendMsg(terminateMsg, host[0], Integer.parseInt(host[1]));
        }
    }

    public void sendMsg(Message msg, String hostaddr, int port) {
        Socket randSocket;
        while (true) {
            try {
                randSocket = new Socket(hostaddr, port);
                break;
            } catch (IOException e) {
                System.out.println("connect refused");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                e.printStackTrace();
            }
        }
        try {
            OutputStream outToServer = randSocket.getOutputStream();
            ObjectOutputStream outStream = new ObjectOutputStream(outToServer);
            outStream.writeObject(msg);
            outToServer.close();
            outStream.close();
            randSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initKeys() {
        this.nodeKeys = new ConcurrentHashMap<>();
        for (int i = 0; i < this.totalNodeNum; i++) {
            if (i < this.myNodeID) {
                nodeKeys.put(i, false);
            } else if (i > this.myNodeID) {
                nodeKeys.put(i, true);
            }
        }
    }

    public void printKey() {
        System.out.println("Node Key is : ");
        for (Map.Entry<Integer, Boolean> entries : nodeKeys.entrySet()) {
            System.out.println(entries.getKey() + " has key : " + entries.getValue());
        }
    }

    public synchronized List<Integer> requiredKey() {
        List<Integer> keys = new ArrayList<>();
        for (int nodeId : this.nodeKeys.keySet()) {
            if (!this.nodeKeys.get(nodeId)) {
                keys.add(nodeId);
            }
        }
        return keys.size() == 0 ? null : keys;
    }


    public void sendKey(int nodeId) {
        this.nodeKeys.put(nodeId, false);
    }

    public void receiveKey(int nodeId) {
        this.nodeKeys.put(nodeId, true);
    }

    public void executeCS() {
        try {
            Thread.sleep((int) this.csDistribution.sample());
            System.out.println("Node : " + this.myNodeID + " executed Critical Section ");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void enterCS() {
//        if (requiredKey() == null) {
            inCS = true;
            System.out.println(" Critical Section execution started at : " + System.currentTimeMillis());
            tester.outputFile("CSStart-" + myNodeID + "-" + System.currentTimeMillis());
            executeCS();
            System.out.println(" Critical Section execution finished at :: " + System.currentTimeMillis());
            tester.outputFile("CSEnd-" + myNodeID + "-" + System.currentTimeMillis());
            leaveCS();
            inCS = false;
//        }
    }

    public void leaveCS() {
        List<Integer> nodeIDNeedKey = new ArrayList<>();
        for (Message message : this.deferredRequests) {
            nodeIDNeedKey.add(message.getFromID());
        }
        System.out.print("nodeIDNeedKey: ");
        for (int nodeID : nodeIDNeedKey) {
            System.out.print(" " + nodeID + " ");
            sendReplyMsg(nodeID);
        }
        System.out.println();
        this.deferredRequests.clear();
        this.hasRequested = false;
        this.timestamp.set(this.maxTimestamp.get());
    }

    public static void main(String[] args) {
        Node node = new Node();
        String path = args[0];
        try {
            node.readConfig(path);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        System.out.println("My Node ID is : " + node.myNodeID);
        node.listen();
        node.initKeys();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int i = 0;
        while (i < node.numRequest) {
            node.tester.outputFile("RequestSent-" + node.myNodeID + "-" + System.currentTimeMillis());
            System.out.println("RequestSent-"  + node.myNodeID + "-" + System.currentTimeMillis());
            node.sendRequest();
            while (node.hasRequested) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                node.enterCS();
            }
            try {
                Thread.sleep((int) node.delayDistribution.sample());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i++;
        }
        node.sendTerminate();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
