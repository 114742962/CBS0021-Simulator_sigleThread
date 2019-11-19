/**
* @Title: SendThread.java
* @Package wp.cbs0021.com
* @Description: TODO(用一句话描述该文件做什么)
* @author Administrator
* @date 2019年10月31日
* @version V1.0
*/
package wp.cbs0021.com;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @ProjectName:  [CBS0021_Simulator] 
 * @Package:      [wp.cbs0021.com.SendThread.java]  
 * @ClassName:    [SendThread]   
 * @Description:  [模拟CBS0021行为的线程]   
 * @Author:       [桂亚君]   
 * @CreateDate:   [2019年10月31日 下午8:21:43]   
 * @UpdateUser:   [桂亚君]   
 * @UpdateDate:   [2019年10月31日 下午8:21:43]   
 * @UpdateRemark: [将波形类型的枚举改为字符串数组]  
 * @Version:      [v1.2]
 */

public class SendThread implements Runnable {
    /** 默认连接服务端的IP地址，将会显示到界面的ServerIP文本框中 */
    public static String serverIP = "127.0.0.1";
    /** 默认连接服务端的端口号，将会显示到界面的ServerPort文本框中 */
    public static int TCPPort = 6668;
    /** TCP连接的socket */
    public Socket socket = null;
    /** TCP连接的sockets */
    public List<Socket> sockets = new ArrayList<>();
    /** 包大小，用来指定每次传输波形时的包数据的大小*/
    public static int packageSize = 1024;
    /** 输入流，用来获取socket的输入流  */
    private InputStream inputStream = null;
    /** 输出流，用来获取socket的输出流  */
    private OutputStream outputStream = null;
    /** 表示CBS启始的modbus地址 */
    private int startChannel = 1;
    /** 表示CBS最后一个的modbus地址 */
    private int endChannel = 10;
    /** 表示当前所发送的CBS的地址 */
    private int rightNowChannel = 0;
    /** 每次发送后未收到应答的次数，超过三次置0 */
    private int time = 1;
    /** 表示CBS线程开启的变量 */
    private boolean start = false;
    /** 主函数所在的类的实例化变量，方便获取到实例的函数或者变量值  */
    private MainLoop main = null;
    /** 表示波形编号的量  */
    private int waveNumber = 1;
    /** 表示波形相位的量 */
    private int stepForPosition = 0;
    /** 表示波形类型的量 */
    private int stepForType = 0;
    /** 上一次接收到波形包数据的回复的时间 */
    private long lastReseivePackageTime = 0;
    /** 存放jar包的当前路径 */
    private String jarpath = null;   
    /** 存放波形数据包 */
    private List<byte[]> wavePackages = new ArrayList<>();
    /** 存放波形文件File对象 */
    private File file = null;
    /** 存放波形文件的大小 */
    private int fileLength = 0;
    /** 存放波形数据包个数 */
    private int packages = 0;
    /** 存放多个TCP连接 */
    /** 表示波形相位的枚举 */
    private enum wavePosition {
        _A___,
        __B__,
        ___C_
    }
    /** 表示波形类型的枚举 */
    private String[] waveType  = new String[] {
        "O__.nmo",
        "C__.nmo",
        "E__.neo",
        "CO_.nmo",
        "OCO.nmo"
    };
    
    SendThread(MainLoop main) {
        this.main = main;
    }
    
    /**
    * @Description: 创建一个新的实例 SendThread.
    * @param channel CBS的地址
    * @param main 创建本实例的主函数的所在的类的实例
     */
    SendThread(int startChannel, int endChannel, MainLoop main) {
        this(main);
        this.startChannel = startChannel;
        this.endChannel = endChannel;
        this.start = true;
    }
    
    @Override
    public void run() {
         connect();
    }
    
    public void init() {
        // 获取到jar包所在的路径并找到波形文件的路径
        jarpath = main.getClass().getProtectionDomain().getCodeSource().getLocation()
            .getPath() + "wave/20191017100056_00002_A___O__.nmo";
        file = new File(jarpath);
        // 获取波形文件的大小
        fileLength = (int)file.length();
        // 获取报个数
        packages = fileLength / packageSize + 1;
        // 将当前发送的CBS地址初始化
        rightNowChannel = startChannel;
        
        initWavePackages();
    }
    
    /**
    * @Title: initWavePackages
    * @Description: 初始化波形发送的数据包并存放到List
    * @param     参数 
    * @return void    返回类型
    * @throws
     */
    public void initWavePackages() {
        try {
            FileInputStream fis = new FileInputStream(file);
            // 最后一包的长度
            int mod = fileLength % packageSize;
            byte[] packageLenthByteArray = new byte[packageSize];
            // 组织每包的数据并添加到List中
            for (int i=1; i<packages + 1; i++) {
                if (false == start) {
                    break;
                }
                // 数据包的指令
                byte[] order = new byte[2];
                order[0] = (byte)0x54;
                order[1] = (byte)0x54;
                // 数据包的帧号
                byte[] frameNumber = new byte[2];
                frameNumber[0] = (byte)(i >> 8);
                frameNumber[1] = (byte)i; 
        
                if (i != (packages)) {
                    // 每次从波形文件中读取数据并装入packageLenthByteArray中，长度为packageSize
                    fis.read(packageLenthByteArray);
                    // 创建一个字节数组的输出流
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(packageSize + 6);
                    // 写入包指令
                    baos.write(order);
                    // 写入帧号
                    baos.write(frameNumber);
                    // 算出包大小
                    byte[] packetSize = new byte[2];
                    packetSize[0] = (byte)(packageSize >> 8);
                    packetSize[1] = (byte)packageSize;
                    // 写入包大小
                    baos.write(packetSize);
                    // 写入包数据
                    baos.write(packageLenthByteArray);
                    // 将字节数组输出流转化为字节数组
                    byte[] byteOfPackge = baos.toByteArray();
                    // 存放包数据
                    wavePackages.add(byteOfPackge);
                    
                } else {
                    // 组织最后一包数据
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(packageSize + 6);
                    fis.read(packageLenthByteArray);
                    baos.write(order);
                    baos.write(frameNumber);
                    byte[] packetSize = new byte[2];
                    packetSize[0] = (byte)(mod >> 8);
                    packetSize[1] = (byte)mod;
                    baos.write(packetSize);
                    baos.write(packageLenthByteArray, 0, mod);
                    byte[] byteOfPackge = baos.toByteArray();
                    wavePackages.add(byteOfPackge);
                }
            }
            
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
    * @Title: connect
    * @Description: 模拟CBS的行为发送数据
    * @param     参数 
    * @return void    返回类型
    * @throws
     */
    public void connect() {
        
        init();
        
        if (serverIP != null) {
            try {
                for (int i=startChannel; i<=endChannel; i++) {
                    sockets.add(new Socket(serverIP, TCPPort));
System.out.println(sockets.size());
                }
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    
        try {
            // 判断是否暂停
            while (start) {
                socket = sockets.get(rightNowChannel - startChannel);
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                // 发送CBS地址
                if (start) {
                    sendCBSAddress(rightNowChannel);
                    waitMoment(100);
                }
                // 发送CBS实时信息
                if (start && rightNowChannel == startChannel) {
                    sendRealInformation();
                    waitMoment(100);
                }
                // 发送CBS波形名称
                if (start) {
                    sendWaveName();
                    waitMoment(100);
                }
                // 发送波形文件
                if (start) {
                    sendWave();
                }
                
                if (rightNowChannel == endChannel) {
                    rightNowChannel = startChannel;
                } else {
                    rightNowChannel ++; 
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    
    /**
    * @Title: sendCBSAddress
    * @Description: 实现CBS地址的发送方法
    * @param @param outputStream    参数 
    * @return void    返回类型
    * @throws
     */
    public void sendCBSAddress(int channel) {
        byte[] address = new byte[4];
        address[0]=(byte)0xA0;
        address[1]=(byte)0xA0;
        address[2]=(byte)(channel>>8);
        address[3]=(byte)channel;
        
        try {
            outputStream = socket.getOutputStream();
            outputStream.write(address);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    /**
    * @Title: sendWaveName
    * @Description: 实现CBS波形名称的发送方法
    * @param @param outputStream  
    * @param @param inputStream    参数 
    * @return void    返回类型
    * @throws
     */
    public void sendWaveName() {
        // 用来组织包数据的，最后将转化为一个字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        byte[] order = new byte[2];
        order[0]=(byte)0x53;
        order[1]=(byte)0x53;
        byte[] fileSize = new byte[4];
        fileSize[0]=(byte)(fileLength >> 24);
        fileSize[1]=(byte)(fileLength >> 16); 
        fileSize[2]=(byte)(fileLength >> 8); 
        fileSize[3]=(byte)fileLength; 
        byte[] packetSize = new byte[2];
        packetSize[0]=(byte)(packageSize >> 8);
        packetSize[1]=(byte)packageSize;        
        byte[] packetNumber = new byte[2];
        packetNumber[0]=(byte)(packages >> 8);
        packetNumber[1]=(byte)packages;        
        
        String waveName = getWaveName();
        
        try {
            // 协议类型
            dos.write(order);
            // 波形名称
            dos.writeBytes(waveName);
            // 文件总大小
            dos.write(fileSize);
            // 单个包大小
            dos.write(packetSize);
            // 包个数
            dos.write(packetNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] waveNamePackage = baos.toByteArray();
        try {
            // 将数据包发出
            outputStream.write(waveNamePackage);
            byte[] byteOn = new byte[4];
            byte[] byteOff = new byte[4];
            byteOff[0]=(byte)0x53;
            byteOff[1]=(byte)0x53; 
            byteOff[2]=(byte)0xAA; 
            byteOff[3]=(byte)0x55; 
            inputStream.read(byteOn);
            
            // 未收到应答时重发，重发3次后将time置1
            while (byteOn[0] == byteOn[2] && time <= 3) {
                System.out.println("missing WaveNamePackage and resentWaveName!");
                outputStream.write(waveNamePackage);
                inputStream.read(byteOn);
                time ++;
            }
            
            if (time != 1) {
                time = 1;
            }
            
//            printHexString(byteOn);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
    * @Title: sendWave
    * @Description: 发送包数据，并在控制台打印出时间，在软件界面打印出回复内容
    * @param @param outputStream
    * @param @param inputStream    参数 
    * @return void    返回类型
    * @throws
     */
    public void sendWaveAndPrint() {
        long sendTime = 0;
        long receiveTime = 0;
        
        try {
            long sendToLastReseive = 0;
            OutputStream outputStream = socket.getOutputStream();
            // 依次发送每包数据
            for (int i=1; i<packages + 1; i++) {
                if (false == start) {
                    break;
                }
                byte[] packet = wavePackages.get(i - 1);
                outputStream.write(packet);
                // 一次写出所有缓冲
                outputStream.flush();
                sendTime = System.currentTimeMillis();
                // 算出从上次接收到发出新一包数据的时间差
                if (i != 1) { 
                    sendToLastReseive = sendTime - lastReseivePackageTime;
                }
                // 接收服务端的回复
                byte[] byteOn = new byte[6];
                inputStream.read(byteOn);
                    
                // 未收到应答时重发，重发3次后将time置1
                while (byteOn[0] == byteOn[2] && time <= 3) {
                    System.out.println("missing!WavePackage:" + i + "resentWavePackage:" + i);
                    outputStream.write(packet);
                    inputStream.read(byteOn);
                    time ++;
                }
                
                if (time != 1) {
                    time = 1;
                }
                
                // 记录成功接收完成的时间
                receiveTime = System.currentTimeMillis();
                // 打印接收端的回复
                printHexString(byteOn);
                // 更新上一次接收成功的时间，用于计算接收到发送耗时
                lastReseivePackageTime = receiveTime;
                // 计算发送到接收的耗时
                long sendToReseive = receiveTime - sendTime;
System.out.println("CBS: " + rightNowChannel + " PKG: " + i + "STOR:" + (sendToReseive) + "ms "
    + "LRTOS:" + (sendToLastReseive) + "ms");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
    * @Title: sendWave
    * @Description: 高速度的发送波形数据包，不做过多的处理
    * @param     参数 
    * @return void    返回类型
    * @throws
    */
    public void sendWave() {
      long sendTime = 0;
      long receiveTime = 0;
      long sendToReseive = 0;
      
      try {
          long sendToLastReseive = 0;
          OutputStream outputStream = socket.getOutputStream();
          // 依次发送每包数据
          for (int i=1; i<packages + 1; i++) {
              if (false == start) {
                  break;
              }
              byte[] packet = wavePackages.get(i - 1);
              outputStream.write(packet);
              // 一次写出所有缓冲
              outputStream.flush();
              // 记录发送时间
              sendTime = System.currentTimeMillis();
              // 算出从上次接收到发出新一包数据的时间差
              if (i != 1) { 
                  sendToLastReseive = sendTime - lastReseivePackageTime;
              }
              // 存放回复的数据
              byte[] byteOn = new byte[6];
              inputStream.read(byteOn);
              // 记录成功接收完成的时间
              receiveTime = System.currentTimeMillis();
              // 更新上一次接收成功的时间，用于计算接收到发送耗时
              lastReseivePackageTime = receiveTime;
              // 计算发送到接收的耗时
              sendToReseive = receiveTime - sendTime;
              
              main.textArea.append("CBS: " + rightNowChannel + "\tPKG: " + i + "\tSTOR:" + (sendToReseive) + "ms " 
                  + "\tLRTOS:" + (sendToLastReseive) + "ms" + "\n");
              
              if (main.textArea.getColumns() > 5000) {
                  main.textArea.setText("");
              }
          }
      } catch (IOException e) {
          e.printStackTrace();
      }
    }        
    /**
    * @Title: sendRealInformation
    * @Description: 发送实时信息
    * @param @param outputStream
    * @param @param inputStream    参数 
    * @return void    返回类型
    * @throws
     */
    public void sendRealInformation() {
        // 获取时间戳
        int rightNow = (int) System.currentTimeMillis();
        // 数据包的指令
        byte[] order = new byte[2];
        order[0] = (byte)0x56;
        order[1] = (byte)0x56;
        // 已有文件个数
        byte[] haveFileCount = new byte[2];
        haveFileCount[0] = (byte)(1024 >> 8);
        haveFileCount[1] = (byte)1024; 
        // 未发送文件个数
        byte[] UnreadFileCount = new byte[2];
        UnreadFileCount[0] = (byte)(24 >> 8);
        UnreadFileCount[1] = (byte)24; 
        // 未发送文件个数
        byte[] deviceTime = new byte[4];
        deviceTime[0] = (byte)(rightNow >> 24);
        deviceTime[1] = (byte)(rightNow >> 16); 
        deviceTime[2] = (byte)(rightNow >> 8); 
        deviceTime[3] = (byte)rightNow; 
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(10);
        DataInputStream dis = null;
        // 写入包指令
        try {
            baos.write(order);
            // 写入文件个数
            baos.write(haveFileCount);
            // 写入未发送文件个数
            baos.write(UnreadFileCount);
            // 写入包数据
            baos.write(deviceTime);
            // 将字节数组输出流转化为字节数组
            byte[] byteOfPackge = baos.toByteArray();
            // 发出包数据
            outputStream.write(byteOfPackge);
            // 一次写出所有缓冲
            outputStream.flush();
            
            // 接收服务端的回复
            byte[] reseive = new byte[6];
            inputStream.read(reseive);
            // 打印接收端的回复
            dis = new DataInputStream(new ByteArrayInputStream(reseive));
            dis.readByte();
            dis.readByte();
            long serverTime = (long)(dis.readInt());
            serverTime *= 1000;
            Date date = new Date(serverTime);
            SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");
            String serverTimeToString = format.format(date);
            main.getTime.setText(serverTimeToString);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                dis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
    * @Title: printHexString
    * @Description: 将收的的数据转化为16进制字符串并打印到界面
    * @param @param b    参数 
    * @return void    返回类型
    * @throws
     */
    public void printHexString(byte[] b) {
        String hexString = "";
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String time = sdf.format(date);
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            hexString += (" " + hex.toUpperCase());
        }
//        main.textArea.append(time + "-" + "CBSAdress:" + channel + ": " + hexString + "\n");
        System.out.println(time + "-" + "CBSAdress:" + rightNowChannel + ": " + hexString + "\n");
        if (main.textArea.getColumns() > 5000) {
            main.textArea.setText("");
        }
    }   
    
    /**
    * @Title: waveNumber
    * @Description: 获取到波形的编号，3位数，用于波形名称的拼接，前面是waveNumberID
    * @param @param waveNumber   1-999
    * @param @return    参数 
    * @return String    返回类型
    * @throws
     */
    public String waveNumber(int waveNumber) {
        DecimalFormat decimalFormat = new DecimalFormat("000");
        String str = decimalFormat.format(waveNumber);
        return str;
    }
    
    /**
    * @Title: waveNumberID
    * @Description: 获取到设备的地址编号，用于拼接成波形的编号，方便判断是哪个设备发出的波形
    * @param @param channel
    * @param @return    参数 
    * @return String    返回类型
    * @throws
     */
    public String waveNumberID(int channel) {
        DecimalFormat decimalFormat = new DecimalFormat("00");
        String str = decimalFormat.format(channel);
        return str;
    }
    
    /**
    * @Title: getWaveName
    * @Description: 拼接波形名称的方法
    * @param @return    参数 
    * @return String    返回类型
    * @throws
     */
    public String getWaveName() {
        wavePosition[] waveposition = wavePosition.values();
        String position = waveposition[stepForPosition].toString();
        String type = waveType[stepForType];
        
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String waveName = sdf.format(date) + "_" + waveNumberID(rightNowChannel) + waveNumber(waveNumber)
            + position + type;
        
        if (stepForPosition == waveposition.length - 1) {
            stepForPosition = 0;
            
            if (stepForType == waveType.length - 1) {
                stepForType = 0;
            } else {
                stepForType ++;
            }
        } else {
            stepForPosition ++;
        }
        
        if (waveNumber > 998) {
            waveNumber = 0;
        } else {
            waveNumber ++;
        }
        
        return waveName;
    }
    
    /**
    * @Title: waitMoment
    * @Description: 将线程的休息进行封装
    * @param @param millisecond    参数 
    * @return void    返回类型
    * @throws
     */
    public void waitMoment(int millisecond) {
        try {
            Thread.sleep(millisecond);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }
}
