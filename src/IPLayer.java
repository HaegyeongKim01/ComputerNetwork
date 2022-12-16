import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class IPLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	public IPLayer anotherPort;
	public RoutingTable routingTable;
	public int port;
   
	private class _IP_ADDR {
		private byte[] addr = new byte[4];  // IP 주소 4bytes

		public _IP_ADDR() {
			// _IP_ADDR() 생성 시 모두 bit 0으로 초기화
			for (int i = 0; i < 4; i++) {
				this.addr[i] = (byte) 0x00;
			}		
		}
	}

   /* IPLayer의 헤더 */
	private class _IP_HEADER {
		byte ip_verlen;		// IP 버전 ex) IPv4, IPv6
		byte ip_tos; 		// type of service
		byte[] ip_len; 		// total packet length
		byte[] ip_id; 		// datagram id
		byte[] ip_fragoff; 	// fragment offset
		byte ip_ttl; 		// time to live in gateway hops
		byte ip_proto; 		// IP protocol
		byte[] ip_cksum; 	// header checksum
		_IP_ADDR ip_src; 	// IP address of source
		_IP_ADDR ip_dst; 	// IP address of destination
		byte[] data; 	// variable length data

		/* 헤더 생성자 */
		public _IP_HEADER() {
			this.ip_src = new _IP_ADDR(); 	// 송신지 주소가 될 _IP_ADDR
			this.ip_dst = new _IP_ADDR();	// 수신지 주소가 될 _IP_ADDR 
			this.ip_len = new byte[2];		// ip_len 2byte
			this.ip_id = new byte[2]; 		// ip_id 2byte
			this.ip_fragoff = new byte[2]; 	// ip_fragoff 2byte
			this.ip_cksum = new byte[2]; 	// ip_cksum 2byte
			this.data = null;
		}
	}

	_IP_HEADER m_sHeader = new _IP_HEADER();  // 헤더 객체 생성

	public IPLayer(String pName) {
		pLayerName = pName;
		ResetHeader();
	}

	/* 헤더 초기화 */
	public void ResetHeader() {
		m_sHeader = new _IP_HEADER();
	}

	/* 수신지의 IP 주소를 반환 */
	public _IP_ADDR GetIPDstAddress() {
		return m_sHeader.ip_dst;
	}

	/* 송신지의 IP 주소를 반환 */
	public _IP_ADDR GetIPSrcAddress() {
		return m_sHeader.ip_src;
	}

	/* 수신지의 IP 주소를 설정 */
	public void SetIpDstAddress(byte[] input) {
		for (int i = 0; i < 4; i++) {
			m_sHeader.ip_dst.addr[i] = input[i];
		}
	}

	/* 송신지의 IP 주소를 설정 */
	public void SetIpSrcAddress(byte[] input) {
		for (int i = 0; i < 4; i++) {
			m_sHeader.ip_src.addr[i] = input[i];
		}
	}

	/* ARP 전송 메소드 */
	public void ARPSend(byte[] src, byte[] dst) {
		this.SetIpSrcAddress(src);  // 수신지 IP 주소 설정
		this.SetIpDstAddress(dst);  // 송신지 IP 주소 설정
		((ARPLayer) this.GetUnderLayer()).ARPSend(src, dst);  // ARPLayer로 전달
	}
    
	public void copyHeader(_IP_HEADER header, byte[] dstIP) { 
		//다른 Port로 현재 헤더의 정보를 옮긴다.
		m_sHeader.ip_verlen = header.ip_verlen;
		m_sHeader.ip_tos = header.ip_tos;
		m_sHeader.ip_ttl = header.ip_ttl;
		m_sHeader.ip_proto = header.ip_proto;
	
		m_sHeader.ip_len = header.ip_len;
		m_sHeader.ip_id = header.ip_id;
		m_sHeader.ip_fragoff = header.ip_fragoff;
		m_sHeader.ip_cksum = header.ip_cksum;
	  
		m_sHeader.ip_dst.addr = dstIP;
	  
		m_sHeader.data = header.data;
	}
    
	public void inputToHeader(byte[] input, byte[] dstIP) {
		//input의 알맞은 인덱스를 찾아 현재 헤더에 정보를 넣는다.
		m_sHeader.ip_verlen = input[0];
		m_sHeader.ip_tos = input[1];
		m_sHeader.ip_ttl = input[8];
		m_sHeader.ip_proto = input[9];

		m_sHeader.ip_len = this.intToByte2(input.length);
		
		for (int i = 0; i < 2; i++) {
			m_sHeader.ip_id[i] = input[4 + i];
			m_sHeader.ip_fragoff[i] = input[6 + i];
        	m_sHeader.ip_cksum[i] = input[10 + i];
		}
      
		this.m_sHeader.ip_dst.addr = dstIP;

		this.m_sHeader.data = new byte[input.length-20];
		for (int i = 20; i < input.length; i++) {
			m_sHeader.data[i-20] = input[i];
		}
	}

	/* 패킷 송신 */
	public boolean Send() {
		byte[] bytes = ObjToByte(m_sHeader);
		int length = byte2ToInt(m_sHeader.ip_len[0], m_sHeader.ip_len[1]);
		//EthernetLayer의 Send함수를 호출하여 패킷 전송 (Ping)
		this.GetUnderLayer().GetUnderLayer().Send(bytes, length);
		
		return true;
	}
   
	public void addRoutingTable(byte[] dst, byte[] subnet, byte[] gateway, byte[] flag, byte[] _interface) {
		this.routingTable.add(dst, subnet, gateway, flag, _interface);
	}

	public void removeRoutingTable() {
		this.routingTable.remove();
	}

	public void setRouter(RoutingTable routingtable) {
		this.routingTable = routingtable;
	}

	public void setAnotherPortSet(IPLayer ipLayer) {
		this.anotherPort = ipLayer;
	}
   
	public byte[] ObjToByte(_IP_HEADER header) {
		int length = byte2ToInt(header.ip_len[0], header.ip_len[1]);
		//현재 헤더 속 길이를 나타내는 인덱스를 찾아 변수에 저장
		byte[] buf = new byte[length];
		//헤더 길이만큼의 byte형 buf를 생성한다.
		
		//알맞은 위치에 헤더 정보를 옮긴다.
		buf[0] = header.ip_verlen;
		buf[1] = header.ip_tos;
		buf[8] = header.ip_ttl;
		buf[9] = header.ip_proto;
		
		for (int i = 0; i < 2; i++) {
			buf[2 + i] = header.ip_len[i];
			buf[4 + i] = header.ip_id[i];
			buf[6 + i] = header.ip_fragoff[i];
			buf[10 + i] = header.ip_cksum[i];
		}
		
		for(int i =0; i < 4 ; i++) {
			buf[12+i] = header.ip_src.addr[i];
			buf[16+i] = header.ip_dst.addr[i];
		}   
		
		for (int i = 20; i < length; i++) {
			buf[i] = header.data[i-20];
		}
		
		return buf;
	}
	
	/* 패킷 수신 */
	public boolean Receive(byte[] input) {
		byte[] srcIP = this.m_sHeader.ip_src.addr;
		byte[] dstIP = new byte[4];
		System.arraycopy(input, 16, dstIP, 0, 4);
		int index = this.routingTable.matchEntry(dstIP);
		//routingTable에서 해당 Entry를 찾아 배열 정보를 저장한다.
		ArrayList<byte[]> inform = routingTable.getEntry(index);
      
		byte[] flag = inform.get(3);
      
		if (flag[0] == 1 & flag[1] == 0 & flag[2] == 0) { //flag = U
			System.out.println("flag : U");
			this.inputToHeader(input, dstIP);
			int hasIP = ((ARPLayer) this.GetUnderLayer()).hasIpInCacheTable(srcIP, dstIP);
			
			//IP가 없다
			if (hasIP == -1) {
				System.out.println("flag U, ARP Request");
				((ARPLayer) this.GetUnderLayer()).ARPSend(srcIP, dstIP);
			}
			//IP가 있다면 hasIP에 ARP 캐시 테이블에 해당 IP, MAC이 있는 인덱스를 저장
			else {
				System.out.println("flag U, 바로 SEND");
				//해당 인덱스에 해당하는 MAC 주소 받아와 변수에 저장, 이를 Ethernet Layer의 목적지 주소로 설정
				byte[] mac = ((ARPLayer) this.GetUnderLayer()).getMacInCacheTable(hasIP);
				((EthernetLayer) this.GetUnderLayer().GetUnderLayer()).SetEnetDstAddress(mac);
				
				//현재 port의 번호와 나가야 할 port 번호가 같다면 바로 Send 호출 (현재 port에서 send)
				if(Byte.toUnsignedInt(inform.get(4)[0]) == this.port) {
					this.Send();
				}
				//현재 port 번호와 나가야 할 port 번호가 다르다면 해당 port(interface)로 이동하여 해당 Send 호출 (다른 port에서 send)
				else {
					//다른 port에 목적지 주소와 헤더 정보를 옮긴다.
					this.anotherPort.copyHeader(this.m_sHeader, dstIP);
					((EthernetLayer) this.anotherPort.GetUnderLayer().GetUnderLayer()).SetEnetDstAddress(mac);
					this.anotherPort.Send();
				}
			}
			
			return true;
		}
		
		else if (flag[0] == 1 & flag[1] == 1 & flag[2] == 0) { // UG
			this.inputToHeader(input, inform.get(2));
			int hasIP = ((ARPLayer) this.GetUnderLayer()).hasIpInCacheTable(srcIP, inform.get(2));
			//목적지 IP가 아닌 gateway 주소를 타겟으로 설정하여 ARP 테이블을 확인한다.
			
			if (hasIP == -1) {
				((ARPLayer) this.GetUnderLayer()).ARPSend(srcIP, dstIP);
			}
			else {
				byte[] mac = ((ARPLayer) this.GetUnderLayer())
							.getMacInCacheTable(hasIP);
				((EthernetLayer) this.GetUnderLayer().GetUnderLayer()).SetEnetDstAddress(mac);
				
				if(Byte.toUnsignedInt(inform.get(4)[0]) == this.port) {
					this.Send();
				}
				else {
					this.anotherPort.copyHeader(this.m_sHeader, inform.get(2));
					((EthernetLayer) this.anotherPort.GetUnderLayer().GetUnderLayer()).SetEnetDstAddress(mac);
					this.anotherPort.Send();
				}
			}	
			return true;
		}		
		return false;
	}
   

	private byte[] intToByte2(int value) {
		byte[] temp = new byte[2];
        temp[0] |= (byte) ((value & 0xFF00) >> 8);
        temp[1] |= (byte) (value & 0xFF);

        return temp;
    }
	
   
	public void setPort(int portNum) {
		this.port = portNum;
	}
   
    private int byte2ToInt(byte value1, byte value2) {
        return (int)((value1 << 8) | (value2));
    }

    @Override
    public String GetLayerName() {
    	return pLayerName;
    }

    @Override
    public BaseLayer GetUnderLayer() {
    	if (p_UnderLayer == null)
    		return null;
      
    	return p_UnderLayer;
    }

    @Override
    public BaseLayer GetUpperLayer(int nindex) {
    	if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
    		return null;
      
    	return p_aUpperLayer.get(nindex);
    }

    @Override
    public void SetUnderLayer(BaseLayer pUnderLayer) {
    	if (pUnderLayer == null)
    		return;
      
    	this.p_UnderLayer = pUnderLayer;
    }

    @Override
    public void SetUpperLayer(BaseLayer pUpperLayer) {
    	if (pUpperLayer == null)
    		return;
      
    	this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
    }

    @Override
    public void SetUpperUnderLayer(BaseLayer pUULayer) {
    	this.SetUpperLayer(pUULayer);
    	pUULayer.SetUnderLayer(this);
    }
}