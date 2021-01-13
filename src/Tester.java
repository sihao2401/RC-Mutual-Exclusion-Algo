import java.io.*;
import java.math.BigInteger;
import java.util.*;

public class Tester {

    Node node;
    String path;
    int totalNumber;
    BufferedReader in = null;

    public Tester() {
        try {
            String str = null;
            in = new BufferedReader(new FileReader("config.txt"));
            str = in.readLine();
            this.totalNumber = Integer.parseInt(str.substring(0,str.indexOf(" ")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Tester(Node node, String path) {
        this.node = node;
        this.path = path;
    }

    public void testMutualExclusion() throws IOException {
        int replyCount = 0;
        int requestCount = 0;
        long startPoint = Long.MAX_VALUE;
        long endPoint = Long.MIN_VALUE;
        List<long[]> timeStamps = new ArrayList<>();
        List<Long> responseTimes = new ArrayList<>();
        // changed the value
        for (int i = 0; i < totalNumber; i++) {
            //CSStart CSEnd  RequestSent
            File file = new File(  "result." + i + ".out");
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
            List<Long> startList = new ArrayList<>();
            List<Long> endList = new ArrayList<>();
            List<Long> requestSentList = new ArrayList<>();
            String curLine = null;
            while ((curLine = br.readLine()) != null) {
                String[] line = curLine.split("-");
                if (line[0].equals("ReplyCount")) {
                    replyCount++;
                } else if (line[0].equals("RequestCount")) {
                    requestCount++;
                } else if (line[0].equals("CSStart")) {
                    startList.add(Long.parseLong(line[2]));
                } else if (line[0].equals("CSEnd")) {
                    endList.add(Long.parseLong(line[2]));
                    endPoint = Math.max(endPoint, Long.parseLong(line[2]));
                } else if (line[0].equals("RequestSent")) {
                    requestSentList.add(Long.parseLong(line[2]));
                    startPoint = Math.min(startPoint, Long.parseLong(line[2]));
                }
            }
            for (int j = 0; j <Math.min(requestSentList.size(), endList.size()) ; j++) {
                responseTimes.add(endList.get(j) - requestSentList.get(j));
                System.out.println();
            }
            for (int j = 0; j < startList.size(); j++) {
                long[] timestamp = new long[2];
                timestamp[0] = startList.get(j);
                timestamp[1] = endList.get(j);
                timeStamps.add(timestamp);
            }
        }

        long totalResponse = 0l;
        for (Long time : responseTimes) {
            totalResponse += time;
        }
        if (checkMutualExclusion(timeStamps)){
            System.out.println("Success ! No two processes accessed Critical Section simultaneously");
        }else{
            System.out.println("Wrong ! Mutual Exclusion broken");
        }

        int totalMessage = replyCount + requestCount;
        System.out.println("Message Complexity is : " + totalMessage);

        BigInteger divided = new BigInteger(String.valueOf(responseTimes.size()));

        System.out.println("Average ResponseTime is : " + totalResponse / responseTimes.size());
        System.out.println("Total ResponseTime is : " + totalResponse);

        double systemThroughout = (double) requestCount / (endPoint - startPoint);
        System.out.println("System Throughout is : " + systemThroughout);

//        removeFile();
    }

    public boolean checkMutualExclusion(List<long[]> timestamps) {
        Collections.sort(timestamps, new Comparator<long[]>() {
            @Override
            public int compare(long[] o1, long[] o2) {
                if(o1[0] < o2[0]){
                    return -1;
                }else{
                    return 1;
                }
            }
        });
        for(int i = 1; i< timestamps.size(); i++){
            if(timestamps.get(i-1)[1]>timestamps.get(i)[0]){
                return false;
            }
        }
        return true;
    }


    public void outputFile(String content) {
        try {
            File file = new File(this.path + "/result." + node.myNodeID + ".out");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file, true);
            fileWriter.write(content);
            fileWriter.write("\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Tester tester = new Tester();
        try {
            tester.testMutualExclusion();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
