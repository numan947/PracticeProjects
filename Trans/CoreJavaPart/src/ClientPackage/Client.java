package ClientPackage;

import java.io.*;

/**
 * Created by numan947 on 10/26/16.
 **/
public class Client {
    //buffer related
    private int DEFAULT_BUFFER_SIZE=12000;
    private int BUFFER_SIZE=36000;

    //server & network related
    private int port=46043;
    private String serverAddress=null;
    private NetworkUtil util=null;

    //file transfer related
    private String[]fileAddresses=null;
    private String[]fileNames=null;
    private File selectedFile=null;
    private BufferedInputStream fbuff=null;
    private byte[]buff=null;
    private char separator=File.separatorChar;
    private String encoding="UTF-8";


    public Client(String serverAddress,String[]fileAddresses) {

        this.serverAddress=serverAddress;
        this.fileAddresses=fileAddresses;
        this.util=new NetworkUtil(serverAddress,port);
        this.buff=new byte[DEFAULT_BUFFER_SIZE];

    }

    public Client() {
        util=new NetworkUtil(serverAddress,port);
        buff=new byte[DEFAULT_BUFFER_SIZE];
    }


    private int getBuff(byte[]buff)
    {
        try {
            return fbuff.read(buff);
        } catch (IOException e) {
            System.out.println("Exception In ClientPackage.Client.getBuff "+e.getMessage());
        }
        return 0;
    }

    private void generateNames()
    {
        fileNames=new String[fileAddresses.length];
        int cnt=0;
        for(String i: fileAddresses){
            fileNames[cnt++]=i.substring(i.lastIndexOf(this.separator)+1);
        }
    }



    public void processAndSend()
    {
        long totalSizetoSend=0;
        long ct=System.currentTimeMillis();
        int totalRead;
        try {
            this.generateNames();

            //how many file to send and response from server
            totalRead=util.readBuff(buff);
            if(totalRead!=-1) {
                System.out.println(new String(buff, 0, totalRead, encoding));

                util.writeBuff(("" + fileAddresses.length).getBytes(encoding));
                totalRead = util.readBuff(buff);

                if (totalRead != -1) {
                    System.out.println(new String(buff, 0, totalRead, encoding));

                    for (int i = 0; i < fileAddresses.length; i++) {
                        try {
                            System.out.println("Sending File " + (i + 1) + "...");
                            selectedFile = new File(fileAddresses[i]);
                            fbuff = new BufferedInputStream(new FileInputStream(selectedFile),BUFFER_SIZE);
                            totalSizetoSend+=selectedFile.length();
                            //send file name & size
                            util.writeBuff((fileNames[i] + "$$$$" + selectedFile.length()).getBytes(encoding));

                            //response from server
                            totalRead = util.readBuff(buff);
                            if (totalRead != -1) {
                                String msg = new String(buff, 0, totalRead, encoding);

                                System.out.println(msg);

                                int tmpCt=0;
                                while ((totalRead = getBuff(buff)) > -1) {
                                    tmpCt+=totalRead;
                                    util.writeBuff(buff, 0, totalRead);
                                    if(tmpCt>=DEFAULT_BUFFER_SIZE){
                                        util.flushStream();
                                        tmpCt=0;
                                    }
                                }
                                util.flushStream();
                                //util.writeBuff("$$$$".getBytes(encoding));

                                //response from server
                                totalRead = util.readBuff(buff);
                                if (totalRead != -1) {
                                    msg = new String(buff, 0, totalRead, encoding);

                                    System.out.println(msg);

                                    System.out.println("File " + i + " sent in stream");
                                    System.out.println("Time taken "+(System.currentTimeMillis()-ct)/1000);
                                    ct=System.currentTimeMillis();
                                }
                            }
                        } catch (FileNotFoundException | UnsupportedEncodingException e) {
                            System.out.println("Exception In ClientPackage.Client.processAndSend, iteration: " + i + " " + e.getMessage());
                        }
                    }
                }
                totalRead = util.readBuff(buff);
                if (totalRead != -1) System.out.println(new String(buff, 0, totalRead, encoding));
            }
        } catch (UnsupportedEncodingException ee) {
            System.out.println("Exception In ClientPackage.Client.processAndSend "+ee.getMessage());
        }
        util.closeAll();
        System.out.println("total time taken to send file: "+(System.currentTimeMillis()-ct)/1000.0);
        System.out.println("total sent in MB: "+(totalSizetoSend/(1024*1024)));
    }
}
