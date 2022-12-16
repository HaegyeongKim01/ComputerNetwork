import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;


public class ARPDlg extends JFrame implements BaseLayer {
   public int nUpperLayerCount = 0;
   public String pLayerName = null;
   public BaseLayer p_UnderLayer = null;
   public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

   public static LayerManager m_LayerMgr = new LayerManager();

   private JTextField ChattingWrite;
   private JTextField PathWrite;
   private JTextField dstIpWrite;
   private JTextField proxyDeviceWrite;
   private JTextField proxyIpWrite;
   private JTextField proxyMacWrite;
   private JTextField GARPAddressWrite;

   Container contentPane;

   JTextArea ChattingArea;
   JTextArea fileArea;
   JTextArea srcMacAddress;
   JTextArea srcIPAddress;
   JTextArea cacheArea;
   JTextArea proxyArpArea;

   JLabel lblsrc;
   JLabel lbldst;
   JLabel dstIpLabel;
   JLabel proxyDevice;
   JLabel proxyIp;
   JLabel proxyMac;
   JLabel GARPAddress;

   JButton Setting_Button;
   JButton Chat_send_Button;
   JButton File_send_Button;
   JButton openFileButton;
   JButton itemDeleteButton;
   JButton allDeleteButton;
   JButton dstIpSendButton;
   JButton proxyAddButton;
   JButton proxyDeleteButton;
   JButton GARPSendButton;

   static JComboBox<String> NICComboBox;

   int adapterNumber = 0;
   String Text;
   byte[] final_srcIP, final_dstIP, final_srcMac;
   JProgressBar progressBar;

   File file;
   
   private ArrayList<ArrayList<byte[]>> cacheTable = new ArrayList<ArrayList<byte[]>>();
   
   public static void main(String[] args) {
      m_LayerMgr.AddLayer(new NILayer("NI"));
      m_LayerMgr.AddLayer(new EthernetLayer("Ethernet"));
      m_LayerMgr.AddLayer(new ARPLayer("ARP"));
      m_LayerMgr.AddLayer(new IPLayer("IP"));
      m_LayerMgr.AddLayer(new TCPLayer("TCP"));
      // m_LayerMgr.AddLayer(new ChatAppLayer("ChatApp"));
      // m_LayerMgr.AddLayer(new FileAppLayer("FileApp"));
      m_LayerMgr.AddLayer(new ARPDlg("GUI"));
      m_LayerMgr.ConnectLayers(" NI ( *Ethernet ( *ARP ( *IP ( *TCP ( *GUI ) ) ) ) )");
   }

   class setAddressListener implements ActionListener {
      @Override
      public void actionPerformed(ActionEvent e) {
         if (e.getSource() == Setting_Button) {
        	/* Reset 눌렀을 때 */
            if (Setting_Button.getText() == "Reset") {
               srcMacAddress.setText("");
               srcIPAddress.setText("");
               Setting_Button.setText("Setting");
               srcMacAddress.setEnabled(true);
               srcIPAddress.setEnabled(true);
               
            }
            /* 자신의 MAC과 IP 주소 세팅 */
            else {
               byte[] myMacAddr = new byte[6];
               byte[] myIPAddr = new byte[4];

               String srcMac = srcMacAddress.getText();
               String srcIP = srcIPAddress.getText();
               /* 입력한 값 읽기 */

               /* -을 제거하여 저장 */
               String[] split_srcMac = srcMac.split("-");
               for (int i = 0; i < 6; i++) {
                  myMacAddr[i] = (byte) Integer.parseInt(split_srcMac[i], 16);
               }

               /* .을 제거하여 저장 */
               String[] split_srcIP = srcIP.split("\\.");
               for (int i = 0; i < 4; i++) {
                  myIPAddr[i] = (byte) Integer.parseInt(split_srcIP[i]);
               }

               final_srcIP = myIPAddr;
               final_srcMac = myMacAddr;
               
               /* 수신지 MAC 주소는 ff-ff-ff-ff-ff-ff */
               String[] dstMac = {"ff","ff","ff","ff","ff","ff"};
               byte[] mydstMacAddr = new byte[6];
               
               for (int i = 0; i < 6; i++) {
                  mydstMacAddr[i] = (byte) Integer.parseInt(dstMac[i], 16);
               }
               
               /* 이더넷 헤더에 송수신지 MAC 주소 저장 */              
               ((EthernetLayer)m_LayerMgr.GetLayer("Ethernet")).SetEnetSrcAddress(myMacAddr);
               ((EthernetLayer)m_LayerMgr.GetLayer("Ethernet")).SetEnetDstAddress(mydstMacAddr);
               
               /* ARP 프레임에 입력한 송신지 MAC, IP 주소와 수신지 MAC 주소 저장 */
               ((ARPLayer)m_LayerMgr.GetLayer("ARP")).SetArpSrcAddress(myMacAddr);
               ((ARPLayer)m_LayerMgr.GetLayer("ARP")).SetArpDstAddress(mydstMacAddr);
               ((ARPLayer)m_LayerMgr.GetLayer("ARP")).SetIpSrcAddress(myIPAddr);
               
               ((NILayer) m_LayerMgr.GetLayer("NI")).SetAdapterNumber(adapterNumber);

               Setting_Button.setText("Reset");
               srcMacAddress.setEnabled(false);
               srcIPAddress.setEnabled(false);
            }
         }


         /* 
          * ARP Table
          * Send 버튼 눌렀을 때
          */
         if (e.getSource() == dstIpSendButton) {
            if (dstIpSendButton.getText() == "Send") {
               String dstIP = dstIpWrite.getText();
               /* 입력한 IP 주소 */
               cacheArea.append(dstIP);
               cacheArea.append("  ??-??-??-??-??-??");
               cacheArea.append("  Incomplete" + "\n");
               
               /* 입력한 IP 주소 상태 테이블에 표시 */
               byte[] dstmyIPAddr = new byte[4];
               String[] byte_dstIP = dstIP.split("\\.");
               for (int i = 0; i < 4; i++) {
                  dstmyIPAddr[i] = (byte) Integer.parseInt(byte_dstIP[i], 10);
               }
               
               final_dstIP = dstmyIPAddr;  // .을 제거한 IP 주소 배열 저장

               // 이전에 저장한 송신지 IP 주소와 ARP 테이블에서 send한 목적지 IP 주소를 TCP 레이어의 ARPSend 인자로 보냄
               ((TCPLayer) m_LayerMgr.GetLayer("TCP")).ARPSend(final_srcIP, final_dstIP);
            }
         }

         /* 
          * Proxy ARP Table
          * Add 버튼 눌렀을 때
          */
         if (e.getSource() == proxyAddButton) {
            if (proxyAddButton.getText() == "Add") {
               String proxyDevice = proxyDeviceWrite.getText();  // 프록시 디바이스 이름
               String proxyIP = proxyIpWrite.getText();          // 추가할 IP
               String proxyMac = proxyMacWrite.getText();        // 추가할 MAC
               
               /* 한 줄에 추가 */
               proxyArpArea.append(proxyDevice);
               proxyArpArea.append("  " + proxyIP);
               proxyArpArea.append("  " + proxyMac + "\n");
               
               byte[] proxyDeviceByte = new byte[1];
               byte[] proxyIpByte = new byte[4];
               byte[] proxyMacByte = new byte[6];
               
               /* .을 제거하여 저장 */
               String[] split_proxyIP = proxyIP.split("\\.");
               for (int i = 0; i < 4; i++) {
                  proxyIpByte[i] = (byte) Integer.parseInt(split_proxyIP[i], 10);
               }
               
               /* -을 제거하여 저장 */
               String[] split_proxyMac = proxyMac.split("-");
               for (int i = 0; i < 6; i++) {
                  proxyMacByte[i] = (byte) Integer.parseInt(split_proxyMac[i], 16);
               }
               
               proxyDeviceByte[0] = (byte)Integer.parseInt("1");
               ((ARPLayer)m_LayerMgr.GetLayer("ARP")).addProxyTable(proxyDeviceByte, proxyIpByte, proxyMacByte);
            }   
         }
         
         /* GARP */
         if (e.getSource() == GARPSendButton) {
        	System.out.println("GARP Send!");
            String garp = GARPAddressWrite.getText();  // 변경할 MAC 주소
            byte[] garpByte = new byte[6];
            
            /* -을 제거하여 저장 */
            String[] split_garp = garp.split("-");
            for (int i = 0; i < 6; i++) {
               garpByte[i] = (byte) Integer.parseInt(split_garp[i], 16);
            }
            
            /* 수신지 MAC 주소는 ff-ff-ff-ff-ff-ff */
            String[] dstMac = {"ff","ff","ff","ff","ff","ff"};
            byte[] mydstMacAddr = new byte[6];
            
            for (int i = 0; i < 6; i++) {
               mydstMacAddr[i] = (byte) Integer.parseInt(dstMac[i], 16);
            }
            
            // 이더넷 수신지 : 브로드캐스트 주소
            ((EthernetLayer)m_LayerMgr.GetLayer("Ethernet")).SetEnetDstAddress(mydstMacAddr);
            
            ((ARPLayer)m_LayerMgr.GetLayer("ARP")).SetArpSrcAddress(garpByte);        // 송신지 MAC 주소를 GARP
            ((ARPLayer)m_LayerMgr.GetLayer("ARP")).SetArpDstAddress(mydstMacAddr);    // 수신지 MAC 주소 모름
            
            ((ARPLayer)m_LayerMgr.GetLayer("ARP")).ARPSend(final_srcIP, final_srcIP); // 송수신지 IP 둘 다 자신
         }
         
         /* Chatting send */ 
         if (e.getSource() == Chat_send_Button) {
            if (Setting_Button.getText() == "Reset") {
               for (int i = 0; i < 10; i++) {
                  String input = ChattingWrite.getText();
                  ChattingArea.append("[SEND] : " + input + "\n");
                  byte[] bytes = input.getBytes();
                  m_LayerMgr.GetLayer("ChatApp").Send(bytes, bytes.length);
                  
                  if (m_LayerMgr.GetLayer("GUI").Receive()) {
                     input = Text;
                     ChattingArea.append("[RECV] : " + input + "\n");
                     continue;
                  }
                  break;
               }
            }
            else {
               JOptionPane.showMessageDialog(null, "Address Configuration Error");
            }
         }
         if (e.getSource() == openFileButton) {
            FileNameExtensionFilter filter = new FileNameExtensionFilter("txt", "txt");
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(filter);
            int ret = chooser.showOpenDialog(null);
            
            if (ret == JFileChooser.APPROVE_OPTION) {
               String filePath = chooser.getSelectedFile().getPath();
               fileArea.setText(filePath);
               File_send_Button.setEnabled(true);
               file = chooser.getSelectedFile();
            }
         }
         /* File send */ 
         if (e.getSource() == File_send_Button) {
            ((FileAppLayer) m_LayerMgr.GetLayer("FileApp")).setAndStartSendFile();
            File_send_Button.setEnabled(false);
         }
         
         /* send 버튼 눌렀을 때 */
         if (e.getSource() == Chat_send_Button) {
             if (Setting_Button.getText() == "Reset") { 
                String input = ChattingWrite.getText();           // 채팅창에 입력된 텍스트 저장
                ChattingArea.append("[SEND] : " + input + "\n");  // 성공하면 입력값 출력
                byte[] bytes = input.getBytes(); 				  // 입력된 메시지를 바이트로 저장
                
                // 채팅창에 입력된 메시지를 chatAppLayer로 보냄
                ((ChatAppLayer)m_LayerMgr.GetLayer("ChatApp")).Send(bytes, bytes.length);
                
                ChattingWrite.setText("");  // 채팅 입력란 비우기
             } 
             else {
                JOptionPane.showMessageDialog(null, "Address Setting Error!.");  // 주소 설정 에러
                return;
             }
          }
          /* 파일 선택 버튼 */
          else if (e.getSource() == openFileButton) {
             JFileChooser fileChooser = new JFileChooser();      // 파일 선택 객체 생성
             fileChooser.setCurrentDirectory(new File("C:\\"));  // 파일 창의 기본 경로를 C:\\에서 시작
             int result = fileChooser.showOpenDialog(null);      // 창 띄우기
             
             /* 선택 없이 취소 눌렀을 경우 */
             if(result != JFileChooser.APPROVE_OPTION) {
                JOptionPane.showMessageDialog(null, "파일 선택 오류");
                return;
             }
             /* 파일을 선택했을 경우 */
             else {
//                object = fileChooser.getSelectedFile();  // 대상 파일을 선택한 파일로 변경
//                path.setText(fileChooser.getSelectedFile().getPath().toString());  // 경로 텍스트 필드
                File_send_Button.setEnabled(true);       // 파일 전송 버튼 클릭 가능하도록 변경
             }
          }
          /* 파일 전송 버튼 눌렀을 때 */
          else if (e.getSource() == File_send_Button) {
             if (Setting_Button.getText() == "Reset") {
                try {
                   File_send_Button.setEnabled(false);  // 전송 버튼 클릭 후에는 전송 버튼 비활성화
                   
                   /* 
                    * 파일창에 입력된 메시지를 FileAppLayer로 보냄
                    * 이 때, 실시간 progressbar 변경 위해 생성한 메소드인 sendFile() 호출
                    */
                   ((FileAppLayer)m_LayerMgr.GetLayer("FileApp")).sendFile();
                } catch (Exception e1) {
                   // TODO Auto-generated catch block
                   e1.printStackTrace();
                }
             }
             else {
                JOptionPane.showMessageDialog(null, "Address Setting Error!.");  // 주소 설정 에러
                return;
             }
          }
      }
   }

   public ARPDlg(String pName) {
      pLayerName = pName;

      setTitle("TestARP");
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setBounds(250, 250, 755, 750);
      contentPane = new JPanel();
      ((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
      setContentPane(contentPane);
      contentPane.setLayout(null);

      /* ARP Cache panel */
      JPanel arpCachePanel = new JPanel();
      arpCachePanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "ARP Cache",
            TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      arpCachePanel.setBounds(10, 5, 370, 340);
      contentPane.add(arpCachePanel);
      arpCachePanel.setLayout(null);

      JPanel arpCacheEditorPanel = new JPanel();
      arpCacheEditorPanel.setBounds(10, 15, 350, 220);
      arpCachePanel.add(arpCacheEditorPanel);
      arpCacheEditorPanel.setLayout(null);

      cacheArea = new JTextArea();
      cacheArea.setEditable(false);
      cacheArea.setBounds(0, 0, 350, 220);
      arpCacheEditorPanel.add(cacheArea);

      itemDeleteButton = new JButton("Item Delete");
      itemDeleteButton.setBounds(70, 250, 100, 30);

      allDeleteButton = new JButton("All Delete");
      allDeleteButton.setBounds(200, 250, 100, 30);
      
      arpCachePanel.add(itemDeleteButton);
      arpCachePanel.add(allDeleteButton);

      dstIpLabel = new JLabel("IP 주소");
      dstIpLabel.setBounds(15, 300, 100, 20);
      arpCachePanel.add(dstIpLabel);

      dstIpWrite = new JTextField();
      dstIpWrite.setBounds(70, 300, 200, 25);
      arpCachePanel.add(dstIpWrite);
      dstIpWrite.setColumns(10);
      dstIpSendButton = new JButton("Send");
      dstIpSendButton.addActionListener(new setAddressListener());
      dstIpSendButton.setBounds(285, 300, 70, 25);
      arpCachePanel.add(dstIpSendButton);

      
      JPanel proxyArpPanel = new JPanel();
      proxyArpPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Proxy ARP Entry",
            TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      proxyArpPanel.setBounds(380, 5, 350, 340);  
      contentPane.add(proxyArpPanel);
      proxyArpPanel.setLayout(null);

      JPanel proxyEditorPanel = new JPanel();
      proxyEditorPanel.setBounds(5, 15, 330, 150);
      proxyArpPanel.add(proxyEditorPanel);
      proxyEditorPanel.setLayout(null);

      proxyArpArea = new JTextArea();
      proxyArpArea.setEditable(false);
      proxyArpArea.setBounds(5, 5, 420, 150);
      proxyEditorPanel.add(proxyArpArea);

      JPanel proxyInputPanel = new JPanel();
    
      proxyInputPanel.setBounds(10, 180, 320, 150);
      proxyInputPanel.setLayout(null);
      proxyArpPanel.add(proxyInputPanel);
      proxyDevice = new JLabel("Device");
      proxyDevice.setBounds(20, 10, 60, 20);
      proxyInputPanel.add(proxyDevice);

      proxyIp = new JLabel("IP 주소");
      proxyIp.setBounds(20, 40, 60, 20);
      proxyInputPanel.add(proxyIp);

      proxyMac = new JLabel("MAC 주소");
      proxyMac.setBounds(20, 70, 60, 20);
      proxyInputPanel.add(proxyMac);

      proxyDeviceWrite = new JTextField();
      proxyDeviceWrite.setBounds(100, 10, 200, 20);
      proxyInputPanel.add(proxyDeviceWrite);

      proxyIpWrite = new JTextField();
      proxyIpWrite.setBounds(100, 40, 200, 20);
      proxyInputPanel.add(proxyIpWrite);

      proxyMacWrite = new JTextField();
      proxyMacWrite.setBounds(100, 70, 200, 20);
      proxyInputPanel.add(proxyMacWrite);

      proxyAddButton = new JButton("Add"); 
      proxyAddButton.setBounds(40, 110, 100, 25);  
      proxyDeleteButton = new JButton("Delete");
      proxyDeleteButton.setBounds(180, 110, 100, 25);
      proxyInputPanel.add(proxyAddButton);
      proxyInputPanel.add(proxyDeleteButton);
      
      proxyAddButton.addActionListener(new setAddressListener());
      proxyDeleteButton.addActionListener(new setAddressListener());
      

      /* gratuitous panel */
      JPanel gratuitousPanel = new JPanel();
      gratuitousPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Gratuitous ARP",
            TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      gratuitousPanel.setBounds(380, 610, 350, 90);
      contentPane.add(gratuitousPanel);
      gratuitousPanel.setLayout(null);

      GARPAddress = new JLabel("H/W 주소");
      GARPAddress.setBounds(10, 40, 100, 25);
      GARPAddressWrite = new JTextField();
      GARPAddressWrite.setBounds(70, 40, 190, 25);
      GARPSendButton = new JButton("Send");
      GARPSendButton.setBounds(270, 40, 70, 25);
      gratuitousPanel.add(GARPAddress);
      gratuitousPanel.add(GARPAddressWrite);
      gratuitousPanel.add(GARPSendButton);
      
      GARPSendButton.addActionListener(new setAddressListener());
      
      /* setting panel */
      JPanel settingPanel = new JPanel();
      settingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Setting",
            TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      settingPanel.setBounds(380, 360, 350, 250);
      contentPane.add(settingPanel);
      settingPanel.setLayout(null);

      JPanel sourceAddressPanel = new JPanel();
      sourceAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
      sourceAddressPanel.setBounds(30, 96, 170, 20);
      settingPanel.add(sourceAddressPanel);
      sourceAddressPanel.setLayout(null);

      lblsrc = new JLabel("MAC Address");
      lblsrc.setBounds(30, 75, 170, 20);
      settingPanel.add(lblsrc);

      srcMacAddress = new JTextArea();
      srcMacAddress.setBounds(2, 2, 170, 20);
      sourceAddressPanel.add(srcMacAddress);

      JPanel IpPanel = new JPanel();
      IpPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
      IpPanel.setBounds(30, 150, 170, 20);
      settingPanel.add(IpPanel);
      IpPanel.setLayout(null);

      lbldst = new JLabel("IP Address");
      lbldst.setBounds(30, 125, 190, 20);
      settingPanel.add(lbldst);

      srcIPAddress = new JTextArea();
      srcIPAddress.setBounds(2, 2, 170, 20);
      IpPanel.add(srcIPAddress);

      JLabel NICLabel = new JLabel("NIC 선택");
      NICLabel.setBounds(30, 20, 170, 20);
      settingPanel.add(NICLabel);

      NICComboBox = new JComboBox();
      NICComboBox.setBounds(30, 49, 170, 20);
      settingPanel.add(NICComboBox);

      for (int i = 0; ((NILayer) m_LayerMgr.GetLayer("NI")).getAdapterList().size() > i; i++) {
         NICComboBox.addItem(((NILayer) m_LayerMgr.GetLayer("NI")).GetAdapterObject(i).getDescription());
      }

      NICComboBox.addActionListener(new ActionListener() { 
         @Override
         public void actionPerformed(ActionEvent e) {
            adapterNumber = NICComboBox.getSelectedIndex();
            
            try {
               srcMacAddress.setText("");
               srcMacAddress.append(getMyMacAddr(((NILayer) m_LayerMgr.GetLayer("NI"))
                     .GetAdapterObject(adapterNumber).getHardwareAddress()));
            } catch (IOException e1) {
               e1.printStackTrace();
            }
         }
      });

      try {
         srcMacAddress.append(getMyMacAddr(
               ((NILayer) m_LayerMgr.GetLayer("NI")).GetAdapterObject(adapterNumber).getHardwareAddress()));
      } catch (IOException e1) {
         e1.printStackTrace();
      };
      
      JPanel chattingPanel = new JPanel();
      chattingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "chatting",
            TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      chattingPanel.setBounds(10, 360, 370, 250);
      contentPane.add(chattingPanel);
      chattingPanel.setLayout(null);

      JPanel chattingEditorPanel = new JPanel();
      chattingEditorPanel.setBounds(10, 15, 340, 160);
      chattingPanel.add(chattingEditorPanel);
      chattingEditorPanel.setLayout(null);

      ChattingArea = new JTextArea();
      ChattingArea.setEditable(false);
      ChattingArea.setBounds(0, 0, 340, 210);
      chattingEditorPanel.add(ChattingArea);

      JPanel chattingInputPanel = new JPanel();
      chattingInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
      chattingInputPanel.setBounds(10, 190, 230, 25);
      chattingPanel.add(chattingInputPanel);
      chattingInputPanel.setLayout(null);

      ChattingWrite = new JTextField();
      ChattingWrite.setBounds(2, 2, 230, 20);
      chattingInputPanel.add(ChattingWrite);
      ChattingWrite.setColumns(10);

      /* file panel */
      JPanel fileTransferPanel = new JPanel();
      fileTransferPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "file transfer",
            TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      fileTransferPanel.setBounds(10, 610, 370, 90);
      contentPane.add(fileTransferPanel);
      fileTransferPanel.setLayout(null);

      JPanel fileEditorPanel = new JPanel();
      fileEditorPanel.setBounds(10, 20, 330, 60);
      fileTransferPanel.add(fileEditorPanel);
      fileEditorPanel.setLayout(null);

      fileArea = new JTextArea();
      fileArea.setEditable(false);
      fileArea.setBounds(0, 5, 250, 20);
      fileEditorPanel.add(fileArea);

      openFileButton = new JButton("File...");
      openFileButton.setBounds(260, 5, 70, 20);
      openFileButton.addActionListener(new setAddressListener());
      fileEditorPanel.add(openFileButton);

      this.progressBar = new JProgressBar(0, 100);
      this.progressBar.setBounds(0, 40, 250, 20);
      this.progressBar.setStringPainted(true);
      fileEditorPanel.add(this.progressBar);

      File_send_Button = new JButton("Send");
      File_send_Button.setBounds(260, 40, 70, 20);
      fileEditorPanel.add(File_send_Button);
      File_send_Button.addActionListener(new setAddressListener());
      File_send_Button.setEnabled(false);

      Setting_Button = new JButton("Setting");
      Setting_Button.setBounds(80, 180, 100, 20);
      Setting_Button.addActionListener(new setAddressListener());
      settingPanel.add(Setting_Button);// setting

      Chat_send_Button = new JButton("Send");     
      Chat_send_Button.setBounds(270, 190, 80, 25);
      Chat_send_Button.addActionListener(new setAddressListener());
      chattingPanel.add(Chat_send_Button);

      setVisible(true);

   }

   public File getFile() {
      return this.file;
   }

   public String getMyMacAddr(byte[] myMacAddrbyte) {
      String myMacAddr = "";
      for (int i = 0; i < 6; i++) {
         myMacAddr += String.format("%02X%s", myMacAddrbyte[i], (i < myMacAddr.length() - 1) ? "" : "");
         if (i != 5) {
            myMacAddr += "-";
         }
      }

      return myMacAddr;
   }

   public boolean Receive(byte[] input) {
      if (input != null) {
         byte[] data = input;
         Text = new String(data);
         ChattingArea.append("[RECV] : " + Text + "\n");
         return false;
      }
      return false;
   }

   @Override
   public void SetUnderLayer(BaseLayer pUnderLayer) {
      // TODO Auto-generated method stub
      if (pUnderLayer == null)
         return;
      this.p_UnderLayer = pUnderLayer;
   }

   @Override
   public void SetUpperLayer(BaseLayer pUpperLayer) {
      // TODO Auto-generated method stub
      if (pUpperLayer == null)
         return;
      this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
   }

   @Override
   public String GetLayerName() {
      // TODO Auto-generated method stub
      return pLayerName;
   }

   @Override
   public BaseLayer GetUnderLayer() {
      // TODO Auto-generated method stub
      if (p_UnderLayer == null)
         return null;
      return p_UnderLayer;
   }

   @Override
   public BaseLayer GetUpperLayer(int nindex) {
      // TODO Auto-generated method stub
      if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
         return null;
      return p_aUpperLayer.get(nindex);
   }

   @Override
   public void SetUpperUnderLayer(BaseLayer pUULayer) {
      this.SetUpperLayer(pUULayer);
      pUULayer.SetUnderLayer(this);

   }

   /*
    * cache table setting
    * ip , ethernet , status(0,1)
    */
   public void setArpCache(ArrayList<ArrayList<byte[]>> cacheTable) {
      this.cacheTable = cacheTable;
      cacheArea.setText("");

      System.out.println("ARP Cache Setting!");

      for(int i=0; i<cacheTable.size(); i++) {
          byte[] ip_byte = cacheTable.get(i).get(0);
          byte[] mac_byte = cacheTable.get(i).get(1);
          byte[] status_byte = cacheTable.get(i).get(2);
          
          String ipByte1 = Integer.toString(Byte.toUnsignedInt(ip_byte[0]));
          String ipByte2 = Integer.toString(Byte.toUnsignedInt(ip_byte[1]));
          String ipByte3 = Integer.toString(Byte.toUnsignedInt(ip_byte[2]));
          String ipByte4 = Integer.toString(Byte.toUnsignedInt(ip_byte[3]));
          
          String macByte1 = String.format("%02X", mac_byte[0]);
          String macByte2 = String.format("%02X", mac_byte[1]);
          String macByte3 = String.format("%02X", mac_byte[2]);
          String macByte4 = String.format("%02X", mac_byte[3]);
          String macByte5 = String.format("%02X", mac_byte[4]);
          String macByte6 = String.format("%02X", mac_byte[5]);
          
          cacheArea.append(ipByte1+"."+ipByte2+"."+ipByte3+"."+ipByte4);
          cacheArea.append("  "+macByte1+"-"+macByte2+"-"+macByte3+"-"+macByte4+"-"+macByte5+"-"+macByte6);
          

         if (byte2ToInt(status_byte[0], status_byte[1])==1) {
            cacheArea.append("  complete" + "\n");
         }
         else {
            cacheArea.append("  Incomplete" + "\n");
         } 
      }    
   }
   
   private int byte2ToInt(byte value1, byte value2) {
        return (int)((value1 << 8) | (value2));
   }
}
