import java.util.ArrayList;

public class ARPLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	public ArrayList<ArrayList<byte[]>> cacheTable = new ArrayList<ArrayList<byte[]>>();
	public ArrayList<ArrayList<byte[]>> proxyCacheTable = new ArrayList<ArrayList<byte[]>>();
	private static LayerManager m_LayerMgr = new LayerManager();
	public int state = 0;

	private class _IP_ADDR {
		private byte[] addr = new byte[4];

		public _IP_ADDR() {
			for(int i=0; i<4;i++){
				this.addr[i] = (byte) 0x00;
			}
		}
	}

	private class _ETHERNET_ADDR {		
		private byte[] addr = new byte[6];

		public _ETHERNET_ADDR() {
			for (int i=0 ; i<6; i++){
				this.addr[i] = (byte) 0x00;
			}
		}
	}

	/* ARP Frame */
	private class ARP_FRAME {
		_ETHERNET_ADDR sender_mac;
		_ETHERNET_ADDR target_mac;
		_IP_ADDR sender_ip;
		_IP_ADDR target_ip;
		byte[] opcode;  // opcode를 통해 request인지 reply인지 구별 가능
		byte hardsize;				
		byte protsize;				
		byte[] hardtype;			
		byte[] prottype;			

		ARP_FRAME() {
			this.sender_mac = new _ETHERNET_ADDR();	 // 6byte
			this.target_mac = new _ETHERNET_ADDR();	 // 6byte
			this.sender_ip = new _IP_ADDR();		 // 4byte
			this.target_ip = new _IP_ADDR();		 // 4byte
			this.opcode = new byte[2];			
			this.hardsize = 6;					
			this.protsize = 4;					
			this.hardtype = new byte[2];		     // 2byte
			this.prottype = new byte[2];		     // 2byte
			this.opcode = intToByte2(1); 		     // default ==> 1
		}
	}

	ARP_FRAME frame = new ARP_FRAME();  // frame 객체 선언 

	/* Constructor */
	public ARPLayer(String pName) {
		pLayerName = pName;
		ResetFrame();
	}

	/* Frame reset */
	public void ResetFrame() {
		frame = new ARP_FRAME();
	}

	/* int를 byte로 변환 */
	private byte[] intToByte2(int value) {
		byte[] temp = new byte[2];
		temp[0] |= (byte) ((value & 0xFF00) >> 8);
		temp[1] |= (byte) (value & 0xFF);

		return temp;
	}

	/* byte를 int로 변환 */
	private int byte2ToInt(byte value1, byte value2) {
		return (int) ((value1 << 8) | (value2));
	}

	/*
	 * obj를 byte로 변환
	 * frame -> byte data로 return
	 */
	public byte[] ObjToByte(ARP_FRAME frame, int length) {
		byte[] buf = new byte[28];

		for (int i = 0; i < 2; i++) {
			buf[i] = frame.hardtype[i];
			buf[i + 2] = frame.prottype[i];
			buf[i + 6] = frame.opcode[i];
		}

		buf[4] = frame.hardsize;
		buf[5] = frame.protsize;

		for (int i = 0; i < 6; i++) {
			buf[i + 8] = frame.sender_mac.addr[i];
			buf[i + 18] = frame.target_mac.addr[i];
		}
		for (int i = 0; i < 4; i++) {
			buf[i + 14] = frame.sender_ip.addr[i];
			buf[i + 24] = frame.target_ip.addr[i];
		}
		return buf;  // 이더넷으로 보낼 data 순서에 맞게 합치기
	}

	public boolean ARPSend(byte[] ip_src, byte[] ip_dst) {
		System.out.println("ARP Send!");
		
		frame.prottype = intToByte2(0x0800);
		
		this.SetIpSrcAddress(ip_src);	// srcAddress setter
		this.SetIpDstAddress(ip_dst);	// ipDstAddress setter
		
		byte[] bytes = ObjToByte(frame, 28);
		((EthernetLayer) this.GetUnderLayer()).ARPSend(bytes, 28);
		
		return false;
	}

	/*
	 * -- Cache Table Setting -- 
     * ProxyCacheTable Dialog == UI 에서 Proxy get
	 * input으로 들어온 src_arp의 IP와 MAC주소 가져와서 cache table에 존재하는지 확인 -> 없다면 put
     * 이미 존재하는 IP라면 table의 MAC주소와 target_arp를 확인해서 틀리면 바꿈 -> GARP
     */
	public boolean addCacheTable(byte[] input){
	      ArrayList<byte[]> cache = new ArrayList<byte[]>();
	      
	      byte[] ip_save = new byte[4];
	      
	      for(int i=0; i<4; i++) {   
	         ip_save[i] = input[14+i];  // input의 src IP address buffer 임시 저장
	      }
	      
	      byte[] mac_buf = new byte[6];
	      
	      for(int i=0; i<6; i++) {   
	         mac_buf[i] = input[i+8];  // input의 src MAC address buffer 임시 저장
	      }
	      
	      boolean hasIP = false;
	      
	      for(int i=0; i<cacheTable.size(); i++) {
	    	 /* cacheTable에 IP address EXIST */
	         if(java.util.Arrays.equals(ip_save, cacheTable.get(i).get(0))) {
	            hasIP = true;
	            /* cacheTable에 저장된 MAC address가 sender_mac과 다른 경우 */
	            if(!java.util.Arrays.equals(mac_buf, cacheTable.get(i).get(1))) {
	               cacheTable.get(i).set(1, mac_buf);  // GARP
	            }
	         }
	      }
	      
	      /* cacheTable에 IP address NOT EXIST */
	      if(hasIP == false) {
	         cache.add(ip_save);         // cache[0]에 IP address put
	         cache.add(mac_buf);        // cache[1]에 MAC address put
	         cache.add(intToByte2(1));  // cache[2]에 Complete put. 1이면 complete.
	         cacheTable.add(cache);
	      }
	      ((ARPDlg)ARPDlg.m_LayerMgr.GetLayer("GUI")).setArpCache(cacheTable);
	      
	      return true;
	   }

	/* Proxy Table에 채우는 함수 */
	public boolean addProxyTable(byte[] interNum, byte[] proxy_ip, byte[] proxy_mac) {
		ArrayList<byte[]> proxy = new ArrayList<byte[]>();

		proxy.add(interNum);   // proxy[0]에 interface number put
		proxy.add(proxy_ip);   // proxy[1]에 IP address put
		proxy.add(proxy_mac);  // proxy[2]에 MAC address put

		proxyCacheTable.add(proxy);

		
		System.out.println("Proxy Add Success");
		return true;
	}

	/* 목적지가 자신의 주소인지 확인 */
	public boolean isItMine(byte[] input) {
		for (int i = 0; i < 4; i++) {
			if (frame.sender_ip.addr[i] == input[i])
				continue;
			else {
				System.out.println("dst is NOT me");
				return false;
			}
		}
		return true;
	}

	/* Proxy Table의 IP와 dst의 IP가 같은지 확인  */
	public boolean ProxyCheck(byte[] dst_ip) {
		for (int i = 0; i < proxyCacheTable.size(); i++) {
			boolean flag = true;
			ArrayList<byte[]> proxy = proxyCacheTable.get(i);
			
			for (int j = 0; j < 4; j++) {
				if (proxy.get(1)[j] == dst_ip[j])
					continue;
				else
					flag = false;
			}
			if (flag == true) {
				return true;
			}
		}
		return false;
	}

	/* ARP Receive */
	public boolean ARPReceive(byte[] input) {
		int ARP_Request = byte2ToInt(input[6], input[7]);  // ARP Opcode

		/*
		 * -- ARP Request --
		 * 목적지(dst)가 broadcast인 경우.
		 * 각 host는 table에 지금 ARP 요청 보낸 host(Sender)의 IP와 MAC 저장 후
		 * ARP message에 있는 target IP 보고, 목적지가 자신인지 확인
		 * 목적지 아니면 drop, 맞으면 ARP message에 있는 target MAC에 자신의 MAC 주소 put
		 * ARP reply message를 sender에게 보내기 위해 ARP message에 있는 sender의 주소와 target의 주소 swap
		 * ARP reply위해 opcode ==> 2
		 */
		if (ARP_Request == 1) {
			System.out.println("<ARP Request> Receive");
			
			addCacheTable(input);         // CacheTable에 추가
			
			byte[] ip_save = new byte[4];  // ARP 수신지 IP 저장
			for(int i = 0; i < 4; i++) {	
				ip_save[i] = input[24 + i];
			}
			
			System.out.println("수신한 ARP Request의 목적지 IP 끝 : " + Byte.toUnsignedInt(ip_save[3]));
			System.out.println("내 IP 끝 : " + Byte.toUnsignedInt(frame.sender_ip.addr[3]));
			
			byte[] send_ip_b = new byte[4];
			System.arraycopy(input, 24, send_ip_b, 0, 4);
			
			byte[] target_ip_b = new byte[4];
			System.arraycopy(input, 14, target_ip_b, 0, 4);
			
			// 자신의 목적지
			if(isItMine(ip_save)) {
				System.out.println("ARP Request's dst is me!");

				// target IP 바꿈
				for(int i = 0; i < 4; i++) {
					frame.target_ip.addr[i] = input[14+i];
				}
				
				for(int i = 0; i < 6; i++) {
					frame.target_mac.addr[i] = input[8+i];
				}
				
				frame.opcode = intToByte2(2);		
		
				ARPSend(send_ip_b, target_ip_b);
				frame.opcode = intToByte2(1);
				
			}
			/* 목적지가 자신이 아닌 경우 == Proxy ARP */
			else {
				System.out.println("Proxy Check");
				
				boolean check = ProxyCheck(ip_save);
				
				/*
				 * 만약 Proxy Table에 target의 MAC 주소 있으면 target의 MAC 주소 채움
				 * Proxy Table에 있으면 Dlg로 Table 보내줌
				 * sender의 주소와 target의 주소 swap
				 * opcode 2로 변경
				 */
				if(check==true) {
					frame.opcode = intToByte2(2);
			
					ARPSend(send_ip_b, target_ip_b);
					frame.opcode = intToByte2(1);
					
					((ARPDlg) ARPDlg.m_LayerMgr.GetLayer("GUI")).setArpCache(cacheTable);
				}
			}
			return true;
		}
		/* 
		 * ARP Reply
		 * ARP_Request == opcode
		 */
		else if (ARP_Request == 2) {
			System.out.println("<ARP Reply> Receive");
			
			byte[] ip_save = new byte[4];
			for(int i = 0; i < 4; i++) {	
				ip_save[i] = input[24+i];
			}
			
			System.out.println("수신한 ARP Request의 목적지 IP 끝 : " + Byte.toUnsignedInt(ip_save[2])+ "." + Byte.toUnsignedInt(ip_save[3]));
			System.out.println("내 IP 끝 : " + Byte.toUnsignedInt(frame.sender_ip.addr[2]) + "." + Byte.toUnsignedInt(frame.sender_ip.addr[3]));
			
			byte[] send_ip_b = new byte[4];
			System.arraycopy(input, 24, send_ip_b, 0, 4);
			
			byte[] target_ip_b = new byte[4];
			System.arraycopy(input, 14, target_ip_b, 0, 4);
			
			/* 나 자신이 목적지 */
			if(isItMine(ip_save)) {
				System.out.println("ARP Reply's dst is me!");

				/*
				 * sender의 ARP Layer가 받음.
				 * ARP message target의 MAC보고 sender는 table 채움
				 * IP, MAC 변수에 setting -> Dlg에서 get해서 화면에 출력
				 */
				addCacheTable(input);
			
				return true;
			}
		}
		return false;
	}

	public _ETHERNET_ADDR GetArpSrcAddress() {
		return frame.sender_mac;
	}

	public _ETHERNET_ADDR GetArpDstAddress() {
		return frame.target_mac;
	}

	public void SetArpSrcAddress(byte[] input) {
		for (int i = 0; i < 6; i++) {
			frame.sender_mac.addr[i] = input[i];
		}
	}

	public void SetArpDstAddress(byte[] input) {
		for (int i = 0; i < 6; i++) {
			frame.target_mac.addr[i] = input[i];
		}
	}

	public _IP_ADDR GetIpSrcAddress() {
		return frame.sender_ip;
	}

	public _IP_ADDR GetIpDstAddress() {
		return frame.target_ip;
	}

	public void SetIpSrcAddress(byte[] input) {
		for (int i = 0; i < 4; i++) {
			frame.sender_ip.addr[i] = input[i];
		}
	}

	public void SetIpDstAddress(byte[] input) {
		for (int i = 0; i < 4; i++) {
			frame.target_ip.addr[i] = input[i];
		}
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
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		// TODO Auto-generated method stub
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}

	public ArrayList<ArrayList<byte[]>> getCacheTable() {
		return this.cacheTable;
	}

	public int getState() {
		return this.state;
	}
}