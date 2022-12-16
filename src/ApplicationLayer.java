import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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

import org.jnetpcap.PcapIf;


public class ApplicationLayer extends JFrame implements BaseLayer {

   public int nUpperLayerCount = 0;
   public String pLayerName = null;
   public BaseLayer p_UnderLayer = null;
   public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

   public static LayerManager m_LayerMgr = new LayerManager();

   private JTextField ChattingWrite;
   private JTextField dstIpWrite;
   
   private JTextField proxyDeviceWrite;
   private JTextField proxyIpWrite;
   private JTextField proxyMacWrite;
   
   private JTextField routerDstWrite;
   private JTextField routerSubnetMaskWrite;
   private JTextField routerGatewayWrite;
   private JTextField routerInterfaceWrite;
   
   
   Container contentPane;

   JTextArea routerTableArea;
   JTextArea srcMacAddress;
   JTextArea srcIpAddress;
   JTextArea cacheArea;
   JTextArea proxyArpArea;
   JTextArea fileArea;
   JTextArea ChattingArea;
   

   JLabel lblsrc;
   JLabel lbldst;
   JLabel dstIpLabel;
   JLabel proxyDevice;
   JLabel proxyIp;
   JLabel proxyMac;
   
   JLabel routerDst;
   JLabel routerSubnetMask;
   JLabel routerGateway;
   JLabel routerInterface;
   

   JButton Setting_Button;
   
   JButton Chat_send_Button;
   JButton File_send_Button;
   JButton openFileButton;
   JButton itemDeleteButton;
   JButton allDeleteButton;
   JButton dstIpSendButton;
   JButton proxyAddButton;
   JButton proxyDeleteButton;
   JButton routerAddButton;
   JButton routerRemoveButton;
   
   JCheckBox up;
   JCheckBox gateway;
   JCheckBox host;
   
   static JComboBox<String> NICComboBox;
   static JComboBox<String> NICComboBox2;

   int adapterNum1 = 0;
   int adapterNum2 = 0;
   String port;
   byte[] final_srcIP, final_dstIP, final_srcMac;
   String Text;
   JProgressBar progressBar;

   File file;
   
   private ArrayList<ArrayList<byte[]>> cacheTable = new ArrayList<ArrayList<byte[]>>();
   public static RoutingTable routingTable = new RoutingTable();
   
   public String srcIP1 = new String();
   public String srcMac1 = new String();
   public String srcIP2 = new String();
   public String srcMac2 = new String();
   
   public static void main(String[] args) {

      m_LayerMgr.AddLayer(new NILayer("NI"));
      m_LayerMgr.AddLayer(new NILayer("NI2"));
      m_LayerMgr.AddLayer(new EthernetLayer("Ethernet"));
      m_LayerMgr.AddLayer(new EthernetLayer("Ethernet2"));
      m_LayerMgr.AddLayer(new ARPLayer("ARP"));
      m_LayerMgr.AddLayer(new ARPLayer("ARP2"));
      m_LayerMgr.AddLayer(new IPLayer("IP"));
      m_LayerMgr.AddLayer(new IPLayer("IP2"));
      m_LayerMgr.AddLayer(new ApplicationLayer("GUI"));
      
      /* GUI를 최상단에 두고 두 갈래로 하위 레이어 연결하기 */
      m_LayerMgr.ConnectLayers(" NI ( *Ethernet ( *ARP ( *IP ( *GUI ) ) +IP ( *GUI ) ) )");
      m_LayerMgr.ConnectLayers(" NI2 ( *Ethernet2 ( *ARP2 ( *IP2 ( *GUI ) ) +IP2 ( *GUI ) ) )");
      
      /* 서로에게 접근할 수 있도록 연결해주고 동일한 routing table을 set */
      ((IPLayer) m_LayerMgr.GetLayer("IP")).setAnotherPortSet (((IPLayer) m_LayerMgr.GetLayer("IP2")));
      ((IPLayer) m_LayerMgr.GetLayer("IP2")).setAnotherPortSet (((IPLayer) m_LayerMgr.GetLayer("IP")));
      ((IPLayer) m_LayerMgr.GetLayer("IP")).setRouter(routingTable);
      ((IPLayer) m_LayerMgr.GetLayer("IP2")).setRouter(routingTable);
   }

   class setAddressListener implements ActionListener {
      @Override
      public void actionPerformed(ActionEvent e) {

         if (e.getSource() == Setting_Button) {
        	 /* Reset 눌렀을 때 */
            if (Setting_Button.getText() == "Reset") {
               srcMacAddress.setText("");
               srcIpAddress.setText("");
               Setting_Button.setText("Setting");
               srcMacAddress.setEnabled(true);
               srcIpAddress.setEnabled(true);
            } 
            /* 각각 자신의 MAC과 자신의IP 주소 세팅 */
            else {
            	
            	byte[] myMacAddr1 = new byte[6];
                byte[] myIPAddr1 = new byte[4];
                
                /* -을 제거하여 저장 */
                String[] split_srcMac1 = srcMac1.split("-");
                for (int i = 0; i < 6; i++) {
                   myMacAddr1[i] = (byte) Integer.parseInt(split_srcMac1[i], 16);
                }

                /* .을 제거하여 저장 */
                String[] split_srcIP1 = srcIP1.split("\\.");
                for (int i = 0; i < 4; i++) {
                   myIPAddr1[i] = (byte) Integer.parseInt(split_srcIP1[i]);
                }
                
                byte[] myMacAddr2 = new byte[6];
                byte[] myIPAddr2 = new byte[4];
                
                /* -을 제거하여 저장 */
                String[] split_srcMac2 = srcMac2.split("-");
                for (int i = 0; i < 6; i++) {
                   myMacAddr2[i] = (byte) Integer.parseInt(split_srcMac2[i], 16);
                }

                /* .을 제거하여 저장 */
                String[] split_srcIP2 = srcIP2.split("\\.");
                for (int i = 0; i < 4; i++) {
                   myIPAddr2[i] = (byte) Integer.parseInt(split_srcIP2[i]);
                }
        
               System.out.println("Setting");
                
               /* ARP 프레임 헤더에 각각 IP 주소와 Mac 주소 저장 */
               ((ARPLayer) m_LayerMgr.GetLayer("ARP")).SetIpSrcAddress(myIPAddr1);
               ((ARPLayer) m_LayerMgr.GetLayer("ARP2")).SetIpSrcAddress(myIPAddr2);
               ((ARPLayer) m_LayerMgr.GetLayer("ARP")).SetArpSrcAddress(myMacAddr1);
               ((ARPLayer) m_LayerMgr.GetLayer("ARP2")).SetArpSrcAddress(myMacAddr2);
               
               /* IPLayer 헤더에 IP 주소와 Port 번호 부여 */ 
               ((IPLayer) m_LayerMgr.GetLayer("IP")).SetIpSrcAddress(myIPAddr1);
               ((IPLayer) m_LayerMgr.GetLayer("IP2")).SetIpSrcAddress(myIPAddr2);
               ((IPLayer) m_LayerMgr.GetLayer("IP")).setPort(1);
               ((IPLayer) m_LayerMgr.GetLayer("IP2")).setPort(2);
              
               /* 이더넷, NI 헤더에 IP 주소 저장 */
               ((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).SetEnetSrcAddress(myMacAddr1);
               ((EthernetLayer) m_LayerMgr.GetLayer("Ethernet2")).SetEnetSrcAddress(myMacAddr2);
               ((NILayer) m_LayerMgr.GetLayer("NI")).SetAdapterNumber(adapterNum1);
               ((NILayer) m_LayerMgr.GetLayer("NI2")).SetAdapterNumber(adapterNum2);
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
         if(e.getSource() == allDeleteButton){
        	 cacheArea.setText(null);
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
         
         /* File send 버튼 눌렀을 때 */ 
         if (e.getSource() == File_send_Button) {
            ((FileAppLayer) m_LayerMgr.GetLayer("FileApp")).setAndStartSendFile();
            File_send_Button.setEnabled(false);
         }
         
         /* send 버튼 눌렀을 때 */
         if (e.getSource() == Chat_send_Button) {
//             if (Setting_Button.getText() == "Reset") { 
//                String input = ChattingWrite.getText();           // 채팅창에 입력된 텍스트 저장
//                ChattingArea.append("[SEND] : " + input + "\n");  // 성공하면 입력값 출력
//                byte[] bytes = input.getBytes(); 				  // 입력된 메시지를 바이트로 저장
//                
//                // 채팅창에 입력된 메시지를 chatAppLayer로 보냄
//                ((ChatAppLayer)m_LayerMgr.GetLayer("ChatApp")).Send(bytes, bytes.length);
//                
//                ChattingWrite.setText("");  // 채팅 입력란 비우기
//             } 
//             else {
//                JOptionPane.showMessageDialog(null, "Address Setting Error!.");  // 주소 설정 에러
//                return;
//             }
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
//             if (Setting_Button.getText() == "Reset") {
//                try {
//                   File_send_Button.setEnabled(false);  // 전송 버튼 클릭 후에는 전송 버튼 비활성화
//                   
//                   /* 
//                    * 파일창에 입력된 메시지를 FileAppLayer로 보냄
//                    * 이 때, 실시간 progressbar 변경 위해 생성한 메소드인 sendFile() 호출
//                    */
//                   ((FileAppLayer)m_LayerMgr.GetLayer("FileApp")).sendFile();
//                } catch (Exception e1) {
//                   // TODO Auto-generated catch block
//                   e1.printStackTrace();
//                }
//             }
//             else {
//                JOptionPane.showMessageDialog(null, "Address Setting Error!.");  // 주소 설정 에러
//                return;
//             }
          }
      
         // ARP send
         if (e.getSource() == dstIpSendButton) {
            if (dstIpSendButton.getText() == "Send") {
               String dstIP = dstIpWrite.getText();
               /* 입력한IP 주소*/
               cacheArea.append(dstIP);
               cacheArea.append("  ??-??-??-??-??-??");
               cacheArea.append("  Incomplete" + "\n");
               
               /* 입력한 IP 주소 상태 테이블에 표시*/
               byte[] dstIPAddress = new byte[4];
               String[] byte_dstIP = dstIP.split("\\.");
               for (int i = 0; i < 4; i++) {
                  dstIPAddress[i] = (byte) Integer.parseInt(byte_dstIP[i], 10);
               }
               
               final_dstIP = dstIPAddress;
              // ((TCPLayer) m_LayerMgr.GetLayer("TCP")).ARPSend(final_srcIP, final_dstIP); 
            }
         }
         
         /* routing table delete 버튼 */
        if (e.getSource() == routerRemoveButton) {
            System.out.println(">> routing table show 삭제");
            routerTableArea.setText(null);
            
            ((IPLayer) m_LayerMgr.GetLayer("IP")).removeRoutingTable();
          }
        /* routing table에 entry 추가 */
        if (e.getSource() == routerAddButton) {
        	String routerDst = routerDstWrite.getText();
        	String routerSubnetMask = routerSubnetMaskWrite.getText();
        	String routerGateway = routerGatewayWrite.getText();
        	String upIsSelected = "0";
        	String gatewayIsSelected = "0";
        	String hostIsSelected = "0";
        	String flag="";
        	
        	/* flag 확인 */
        	if (up.isSelected()) {
        		upIsSelected = "1";
        		flag+="U";
        	}
        	if (gateway.isSelected()) {
        		gatewayIsSelected = "1";
        		flag+="G";
        	}
        	if (host.isSelected()) {
        		hostIsSelected = "1";
        		flag+="H";
        	}
        	
        	port = routerInterfaceWrite.getText();
        	
        	routerTableArea.append(routerDst+"      ");
        	routerTableArea.append(routerSubnetMask+"      ");
        	routerTableArea.append(routerGateway+"      ");
        	routerTableArea.append(flag+"      ");
        	routerTableArea.append(port + "\n");
        	
        	byte[] dstIPAddress = new byte[4];
            String[] byte_dstIP = routerDst.split("\\.");
            for (int i = 0; i < 4; i++) {
               dstIPAddress[i] = (byte) Integer.parseInt(byte_dstIP[i], 10);
            }
            
            byte[] subnetMaskAddress = new byte[4];
            String[] byte_mask = routerSubnetMask.split("\\.");
            for (int i = 0; i < 4; i++) {
            	subnetMaskAddress[i] = (byte) Integer.parseInt(byte_mask[i], 10);
            }
            
            byte[] gatewayAddress = new byte[4];
            if (routerGateway.equals("*")){
            	gatewayAddress[0] = (byte) Integer.parseInt("-1", 10);
            }
            else {
            	String[] byte_gate = routerSubnetMask.split("\\.");
            	 for (int i = 0; i < 4; i++) {
                 	subnetMaskAddress[i] = (byte) Integer.parseInt(byte_gate[i], 10);
                 }
            }
            
            byte[] flagAddress = new byte[3];
            flagAddress[0] = (byte) Integer.parseInt(upIsSelected,10);
            flagAddress[1] = (byte) Integer.parseInt(gatewayIsSelected,10);
            flagAddress[2] = (byte) Integer.parseInt(hostIsSelected,10);
            
            byte[] interfaceAddress = new byte[1];
            interfaceAddress[0] =  (byte) Integer.parseInt(port,10);
            /* 순서에 맞게 entry 배열을 설정하여 routing table에 추가 */
            ((IPLayer) m_LayerMgr.GetLayer("IP")).addRoutingTable(dstIPAddress, subnetMaskAddress, gatewayAddress, flagAddress, interfaceAddress);
        
        }
       
      }

   }

   public ApplicationLayer(String pName) {
      pLayerName = pName;

      setTitle("Router Table");
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setBounds(250, 0, 1200, 800);
      contentPane = new JPanel();
      ((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
      setContentPane(contentPane);
      contentPane.setLayout(null);

      // ARP Cache panel
      JPanel arpCachePanel = new JPanel();
      arpCachePanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "ARP Cache",
            TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      arpCachePanel.setBounds(10, 5, 370, 371);
      contentPane.add(arpCachePanel);
      arpCachePanel.setLayout(null);

      JPanel arpCacheEditorPanel = new JPanel();
      arpCacheEditorPanel.setBounds(10, 15, 350, 230);
      arpCachePanel.add(arpCacheEditorPanel);
      arpCacheEditorPanel.setLayout(null);

      cacheArea = new JTextArea();
      cacheArea.setEditable(false);
      cacheArea.setBounds(0, 0, 350, 220);
      arpCacheEditorPanel.add(cacheArea);// chatting panel 

      itemDeleteButton = new JButton("Delete Item");
      itemDeleteButton.setBounds(70, 250, 100, 30);

      allDeleteButton = new JButton("Delete All");
      allDeleteButton.setBounds(200, 250, 100, 30);
      allDeleteButton.addActionListener(new setAddressListener());
      
      /* Delete button actionListener */
      arpCachePanel.add(itemDeleteButton);
      arpCachePanel.add(allDeleteButton);
      

      dstIpLabel = new JLabel("IP 주소");
      dstIpLabel.setBounds(15, 300, 100, 20);
      arpCachePanel.add(dstIpLabel);

      dstIpWrite = new JTextField();
      dstIpWrite.setBounds(70, 300, 200, 20);// 249
      arpCachePanel.add(dstIpWrite);
      dstIpWrite.setColumns(10);// target IP write panel 
      dstIpSendButton = new JButton("Send");
      dstIpSendButton.addActionListener(new setAddressListener());
      dstIpSendButton.setBounds(285, 300, 70, 20);
      arpCachePanel.add(dstIpSendButton);
     
      // routing table panel
      JPanel routePannel = new JPanel();
      routePannel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Routing Table",
            TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      routePannel.setBounds(10, 380, 360, 260);
      contentPane.add(routePannel);
      routePannel.setLayout(null);

      JPanel routerTableEditorPanel = new JPanel();// write panel
      routerTableEditorPanel.setBounds(10, 15, 340, 235);
      routePannel.add(routerTableEditorPanel);
      routerTableEditorPanel.setLayout(null);

      routerTableArea = new JTextArea();
      routerTableArea.setEditable(false);
      routerTableArea.setBounds(0, 0, 340, 190);
      routerTableEditorPanel.add(routerTableArea);// routing show edit

      routerTableArea.setLayout(null);
      
      //router show table에서의 remove button
      routerRemoveButton = new JButton("Remove");
      routerRemoveButton.setBounds(120, 205, 100, 30);
      
      routerRemoveButton.addActionListener(new setAddressListener());
      routerTableEditorPanel.add(routerRemoveButton);

      // router add panel
      JPanel routerAddPanel = new JPanel();// router add panel
      routerAddPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Add Routing Table",
            TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      routerAddPanel.setBounds(380, 380, 350, 250);
      
      routerAddPanel.setLayout(null);

      routerDst = new JLabel("Destination");
      routerDst.setBounds(20, 40, 80, 20);
      routerAddPanel.add(routerDst);

      routerSubnetMask = new JLabel("Netmask");
      routerSubnetMask.setBounds(20, 70, 80, 20);
      routerAddPanel.add(routerSubnetMask);
      
      routerGateway = new JLabel("Gateway");
      routerGateway.setBounds(20, 100, 80, 20);
      routerAddPanel.add(routerGateway);
      
      routerGateway = new JLabel("Flag");
      routerGateway.setBounds(20, 130, 80, 20);
      routerAddPanel.add(routerGateway);
      
      routerInterface = new JLabel("Interface");
      routerInterface.setBounds(20, 160, 80, 20);
      routerAddPanel.add(routerInterface);
      
      routerDstWrite = new JTextField();
      routerDstWrite.setBounds(100, 40, 200, 20);
      routerAddPanel.add(routerDstWrite);

      routerSubnetMaskWrite = new JTextField();
      routerSubnetMaskWrite.setBounds(100, 70, 200, 20);
      routerAddPanel.add(routerSubnetMaskWrite);

      routerGatewayWrite = new JTextField();
      routerGatewayWrite.setBounds(100, 100, 200, 20);
      routerAddPanel.add(routerGatewayWrite);
      
      routerInterfaceWrite = new JTextField();
      routerInterfaceWrite.setBounds(100, 160, 200, 20);
      routerAddPanel.add(routerInterfaceWrite);
      
      routerAddButton = new JButton("Add");
      routerAddButton.setBounds(130, 200, 70, 30);
      
      routerAddButton.addActionListener(new setAddressListener());
      
      up = new JCheckBox("up",true);
      up.setBounds(100, 130, 50, 20);
	  gateway = new JCheckBox("gateway");
	  gateway.setBounds(150, 130, 80, 20);
	  host = new JCheckBox("host",true);
	  host.setBounds(230, 130, 100, 20);
		
	  routerAddPanel.add(up);
	  routerAddPanel.add(gateway);
	  routerAddPanel.add(host);
	  routerAddPanel.add(routerAddButton);
	  
	  contentPane.add(routerAddPanel);
      setVisible(true);
      
      /* Setting panel */
      JPanel settingPanel = new JPanel();
      settingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Setting",
            TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      settingPanel.setBounds(380, 5, 350, 370);
      contentPane.add(settingPanel);
      settingPanel.setLayout(null);

      JLabel NICLabel = new JLabel("NIC1 Select");
      NICLabel.setBounds(30, 20, 300, 20);
      settingPanel.add(NICLabel);
      
      JLabel NICLabel2 = new JLabel("NIC2 Select");
      NICLabel2.setBounds(30, 100, 300, 20);
      settingPanel.add(NICLabel2);
      
      NICComboBox = new JComboBox();
      NICComboBox.setBounds(30, 49, 300, 20);
      settingPanel.add(NICComboBox);
      
      NICComboBox2 = new JComboBox();
      NICComboBox2.setBounds(30, 129, 300, 20);
      settingPanel.add(NICComboBox2);
      
      Setting_Button = new JButton("Setting");// setting
      Setting_Button.setBounds(80, 180, 200, 40);
      Setting_Button.addActionListener(new setAddressListener());
      JPanel settingBtnPannel = new JPanel();
      settingBtnPannel.setBounds(290, 129, 150, 20);
      settingPanel.add(Setting_Button); // setting
      
      contentPane.add(settingBtnPannel);
      
      for (int i = 0; ((NILayer) m_LayerMgr.GetLayer("NI")).getAdapterList().size() > i; i++) {
         NICComboBox.addItem(((NILayer) m_LayerMgr.GetLayer("NI")).GetAdapterObject(i).getDescription());
      }
      for (int i = 0; ((NILayer) m_LayerMgr.GetLayer("NI2")).getAdapterList().size() > i; i++) {
          NICComboBox2.addItem(((NILayer) m_LayerMgr.GetLayer("NI2")).GetAdapterObject(i).getDescription());
       }
      
      NICComboBox.addActionListener(new ActionListener() { 

          @Override
          public void actionPerformed(ActionEvent e) {
             // TODO Auto-generated method stub

             adapterNum1 = NICComboBox.getSelectedIndex();
             try {
            	 srcMac1 = getMyMacAddr(((NILayer) m_LayerMgr.GetLayer("NI")).GetAdapterObject(0).getHardwareAddress());
                 byte[] srcIPAddr1 = ((((NILayer) m_LayerMgr.GetLayer("NI")).GetAdapterObject(0)
              		   .getAddresses()).get(0)).getAddr().getData();
                 final StringBuilder IPbuf1 = new StringBuilder();
                 for (byte b: srcIPAddr1) {
              	   if (IPbuf1.length()!=0)
              		   IPbuf1.append(".");
              	   IPbuf1.append(b & 0xff);
                 }
                 srcIP1 = IPbuf1.toString();
             } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
             }
          }
       });
      
      NICComboBox2.addActionListener(new ActionListener() { // Event Listener

          @Override
          public void actionPerformed(ActionEvent e) {
             // TODO Auto-generated method stub
             adapterNum2 = NICComboBox2.getSelectedIndex();
             try {
             	srcMac2= getMyMacAddr(((NILayer) m_LayerMgr.GetLayer("NI2")).m_pAdapterList.get(1).getHardwareAddress());
             	byte[] srcIPAddr2 = ((((NILayer) m_LayerMgr.GetLayer("NI2")).m_pAdapterList.get(1)
              		   .getAddresses()).get(0)).getAddr().getData();
                 final StringBuilder IPbuf2 = new StringBuilder();
                 for (byte b: srcIPAddr2) {
              	   if (IPbuf2.length()!=0)
              		 IPbuf2.append(".");
              	 IPbuf2.append(b & 0xff);
               }
               srcIP2 = IPbuf2.toString();
             } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
             }
          }
       });
      
      /* proxy panel */
      JPanel proxyArpPanel = new JPanel();
      proxyArpPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Proxy ARP Entry",
            TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      proxyArpPanel.setBounds(750, 370, 350, 340);  
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
      
      /* chatting panel*/
      JPanel chattingPanel = new JPanel();
      chattingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "chatting",
            TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      chattingPanel.setBounds(750, 110, 370, 250);
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
      fileTransferPanel.setBounds(750, 10, 370, 90);
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
         routerTableArea.append("[RECV] : " + Text + "\n");
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
      // nUpperLayerCount++;
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

   // Cache table 세팅 
   public void setArpCache(ArrayList<ArrayList<byte[]>> cacheTable) {
      this.cacheTable = cacheTable;
      cacheArea.setText("");
   
      System.out.println(">> ARP Cache table setting");

      for(int i=0; i<cacheTable.size(); i++) {
         byte[] ip_byte = cacheTable.get(i).get(0);
         byte[] mac_byte = cacheTable.get(i).get(1);
         byte[] status_byte = cacheTable.get(i).get(2);
         
         String ip_Byte1 = Integer.toString(Byte.toUnsignedInt(ip_byte[0]));
         String ip_Byte2 = Integer.toString(Byte.toUnsignedInt(ip_byte[1]));
         String ip_Byte3 = Integer.toString(Byte.toUnsignedInt(ip_byte[2]));
         String ip_Byte4 = Integer.toString(Byte.toUnsignedInt(ip_byte[3]));
         
         String mac_Byte1 = String.format("%02X", mac_byte[0]);
         String mac_Byte2 = String.format("%02X", mac_byte[1]);
         String mac_Byte3 = String.format("%02X", mac_byte[2]);
         String mac_Byte4 = String.format("%02X", mac_byte[3]);
         String mac_Byte5 = String.format("%02X", mac_byte[4]);
         String mac_Byte6 = String.format("%02X", mac_byte[5]);
         
         cacheArea.append(ip_Byte1+"."+ip_Byte2+"."+ip_Byte3+"."+ip_Byte4);
         cacheArea.append("  "+mac_Byte1+"-"+mac_Byte2+"-"+mac_Byte3+"-"+mac_Byte4+"-"+mac_Byte5+"-"+mac_Byte6);
         System.out.println(ip_Byte1+"."+ip_Byte2+"."+ip_Byte3+"."+ip_Byte4);
         System.out.println("  "+mac_Byte1+"-"+mac_Byte2+"-"+mac_Byte3+"-"+mac_Byte4+"-"+mac_Byte5+"-"+mac_Byte6);

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