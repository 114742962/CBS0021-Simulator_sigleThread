/**
* @Title: MainLoop.java
* @Package wp.cbs0021.com
* @Description: TODO(用一句话描述该文件做什么)
* @author Administrator
* @date 2019年10月31日
* @version V1.0
*/
package wp.cbs0021.com;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;

/**  
 * @ProjectName:  [CBS0021_Simulator] 
 * @Package:      [wp.cbs0021.com.MainLoop.java]  
 * @ClassName:    [MainLoop]   
 * @Description:  [模拟器的主界面]   
 * @Author:       [Guiyajun]   
 * @CreateDate:   [2019年10月31日 上午9:52:04]   
 * @UpdateUser:   [Guiyajun]   
 * @UpdateDate:   [2019年10月31日 上午9:52:04]   
 * @UpdateRemark: [说明本次修改内容]  
 * @Version:      [v1.0]
 */
public class MainLoop extends Frame {
    
    /** CBS起始地址 */
    public static int startCBSAddress = 1;
    /** CBS结束地址 */
    public static int stopCBSAddress = 10;
    /** 存放CBS线程的实例 */
    public List<SendThread> sendThreads = new ArrayList<>();
    /** 存放线程池的实例  */
    ThreadPoolService threadPoolService = null;
    /** 打印文本的区域  */
    public TextArea textArea = null;
    /** 工具的标题和版本号 */
    private String title = "CBS0021-Simulator_V1.6_20191119";
    /** 获取服务器的时间  */
    TextField getTime = new TextField("1970年01月01日 00点00分00秒", 25);
    /** 发送数据的线程  */
    SendThread sendThread = null;
    /**
     * @Fields field:field:{todo}(用一句话描述这个变量表示什么)
     */
    private static final long serialVersionUID = 1L;
    
    public static void main(String[] args) {
        MainLoop main = new MainLoop();
        main.launchFrame();

    }
    
    public void launchFrame() {
        setBounds(400, 200, 455, 480);
        setTitle(title);
        setResizable(true);
        setLayout(new FlowLayout());
        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        textArea = new TextArea();
        textArea.setSize(440, 450);
        TextField serverIP = new TextField("127.0.0.1",12);
        TextField ServerPort = new TextField("6668", 5);
        TextField StartCBSAddress = new TextField("1", 5);
        TextField StopCBSAddress = new TextField("10", 5);
        TextField CBSCount = new TextField("10", 5);
        CBSCount.setEditable(false);
        TextField PackageSize = new TextField("1024", 5);
        add(textArea);
        add(startButton);
        add(stopButton);
        add(new Label("ServerIP:"));
        add(serverIP);
        add(new Label("ServerPort:"));
        add(ServerPort);
        add(new Label("StartCBSAddress:"));
        add(StartCBSAddress);
        add(new Label("StopCBSAddress:"));
        add(StopCBSAddress);
        add(new Label("CBSCount:"));
        add(CBSCount);
        add(new Label("PackageSize:"));
        add(PackageSize);
        add(new Label("GetTime:"));
        add(getTime);
        setVisible(true);
        
        // 窗口关闭
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                System.exit(0);
          }
        });
        
        // CBS地址其实输入框，根据填入的值计算出CBS个数
        StartCBSAddress.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                int startCBSAddress = Integer.parseInt(StartCBSAddress.getText().trim());
                int stopCBSAddress = Integer.parseInt(StopCBSAddress.getText().trim());
                String cbsCounts = "" + (stopCBSAddress - startCBSAddress + 1);
                CBSCount.setText(cbsCounts);                     
            }
        });
        
        // CBS地址其实输入框，根据填入的值计算出CBS个数
        StopCBSAddress.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                int startCBSAddress = Integer.parseInt(StartCBSAddress.getText().trim());
                int stopCBSAddress = Integer.parseInt(StopCBSAddress.getText().trim());
                String cbsCounts = "" + (stopCBSAddress - startCBSAddress + 1);
                CBSCount.setText(cbsCounts);                     
            }
        });
        
        // 暂停所有线程
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                sendThread.setStart(false);
            }
        });
         
        // 开始发送按钮
        startButton.addActionListener(new ActionListener() {
            
            public void init() {
                threadPoolService = null;
            }
            
            public void actionPerformed(ActionEvent e) {
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                
                init();
                ThreadPoolService.getInstance();
                threadPoolService = new ThreadPoolService();
                
                String ip = serverIP.getText().trim();
                int serverPort = Integer.parseInt(ServerPort.getText().trim());
                int packageSize = Integer.parseInt(PackageSize.getText().trim());
                int startCBSAddress = Integer.parseInt(StartCBSAddress.getText().trim());
                int stopCBSAddress = Integer.parseInt(StopCBSAddress.getText().trim());
                String cbsCounts = "" + (stopCBSAddress - startCBSAddress + 1);
                CBSCount.setText(cbsCounts);
                SendThread.serverIP = ip;
                SendThread.TCPPort = serverPort;
                SendThread.packageSize = packageSize;
                // 启动线程池
                if (startCBSAddress <= stopCBSAddress) {
                    sendThread = new SendThread(startCBSAddress,  stopCBSAddress, MainLoop.this);
                    threadPoolService.execute(sendThread);
                }
            }
        });
    }
}