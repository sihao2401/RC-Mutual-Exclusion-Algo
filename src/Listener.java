import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Listener implements Runnable {
    private Node node;
    private ServerSocket serverSocket;
    private volatile int terminateCount;

    public Listener(Node node) {
        this.node = node;
        this.terminateCount = 0;
        try {
            serverSocket = new ServerSocket(node.myPort);
            System.out.println(node.myNodeID + " Server is listening on port " + node.myPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (this.terminateCount != node.totalNodeNum - 1) {
            try {
                Socket server = this.serverSocket.accept();
                ObjectInputStream in = new ObjectInputStream(server.getInputStream());
                Message msg = (Message) in.readObject();
                int newVal = Math.max(node.maxTimestamp.get(), msg.getTimestamp())+1;
                node.maxTimestamp.set(newVal);
                System.out.println("[Received Content]"+ msg);
                receivedMessage(msg);
            } catch (SocketTimeoutException s) {
                System.out.println("Socket timed out!");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    public void receivedMessage(Message message){
        if(message.getType().equals("REQUEST")){
            if(node.inCS){
                node.deferredRequests.add(message);
                System.out.println("[Received]" + " Node in Critical Section, defer message");
            }else if(!node.hasRequested){
                node.sendReplyMsg(message.getFromID());
                System.out.println("[Received]" + " No Pending Request, Send Reply");
            }else if(node.timestamp.get() < message.getTimestamp()){
                node.deferredRequests.add(message);
                System.out.println("[Received]" + " This ["+node.timestamp.get()+"] request time is smaller than ["+message.getTimestamp()+"] message timestamp, defer message");
            }else if(node.timestamp.get() > message.getTimestamp()){
                node.sendReplyMsg(message.getFromID());
                node.sendRequestMsg(message.getFromID());
                System.out.println("[Received]" + " This ["+node.timestamp.get()+"] request time is bigger than ["+message.getTimestamp()+"] message timestamp, send two messages");
            }else if(node.timestamp.get() == message.getTimestamp()){
                if(node.myNodeID > message.getFromID()){
                    node.deferredRequests.add(message);
                    System.out.println("[Received]" + " This request id is bigger than ["+message.getTimestamp()+"] message timestamp, defer message");
                }else if (node.myNodeID < message.getFromID()){
                    node.sendReplyMsg(message.getFromID());
                    node.sendRequestMsg(message.getFromID());
                    System.out.println("[Received]" + " This request id is smaller than ["+message.getTimestamp()+"] timestamp, send two messages");
                }
            }
        }else if(message.getType().equals("REPLY")){
            node.receiveKey(message.getFromID());
            System.out.println("[Received]" + " Received Reply, Check CS");
            System.out.println("Before CS:");
            node.printKey();
            node.enterCS();
            System.out.println("Leave CS:");
            node.printKey();
        }else if(message.getType().equals("TERMINATE")){
            this.terminateCount++;
        }
    }
}
