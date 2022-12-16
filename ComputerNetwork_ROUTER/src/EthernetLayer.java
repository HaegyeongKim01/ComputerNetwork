import java.util.ArrayList;

/* 송신자 주소, 목적지 주소, MAC 주소 관리 */
public class EthernetLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	
	public EthernetLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		ResetHeader();
	}
	
	public void ResetHeader() {
		m_sHeader = new _ETHERNET_HEADER();
	}
	
	private class _ETHERNET_ADDR {
		private byte[] addr = new byte[6];  // 6bytes 크기의 16진수형의 주소를 받음

		public _ETHERNET_ADDR() {
			this.addr[0] = (byte) 0x00;
			this.addr[1] = (byte) 0x00;
			this.addr[2] = (byte) 0x00;
			this.addr[3] = (byte) 0x00;
			this.addr[4] = (byte) 0x00;
			this.addr[5] = (byte) 0x00;
		}
	}

	/* 헤더 설정 */
	private class _ETHERNET_HEADER {
		_ETHERNET_ADDR enet_dstaddr;  // 목적지 MAC 주소
		_ETHERNET_ADDR enet_srcaddr;  // 송신지 MAC 주소
		byte[] enet_type;
		byte[] enet_data;

		public _ETHERNET_HEADER() {
			this.enet_dstaddr = new _ETHERNET_ADDR();
			this.enet_srcaddr = new _ETHERNET_ADDR();
			this.enet_type = new byte[2];
			this.enet_data = null;
		}
	}

	_ETHERNET_HEADER m_sHeader = new _ETHERNET_HEADER();

	public byte[] ObjToByte(_ETHERNET_HEADER Header, byte[] input, int length) {
		byte[] buf = new byte[length + 14];  // 헤더와 데이터를 byte 배열로 이어줌
		
		for(int i = 0; i < 6; i++) {
			buf[i] = Header.enet_dstaddr.addr[i];    // 목적지 MAC 주소
			buf[i+6] = Header.enet_srcaddr.addr[i];  // 송신지 MAC 주소
		}			
		
		buf[12] = Header.enet_type[0];
		buf[13] = Header.enet_type[1];
		
		for (int i = 0; i < length; i++)
			buf[14 + i] = input[i];  // input(이더넷 헤더를 제외한 나머지 데이터)을 헤더 뒤에 붙임

		return buf;
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
        return (int)((value1 << 8) | (value2));
    }
    
	public boolean Send(byte[] input, int length) {
		//Ping
		m_sHeader.enet_type = intToByte2(0x0800);
		m_sHeader.enet_data = input;
		byte[] bytes = ObjToByte(m_sHeader, input, length);
		this.GetUnderLayer().Send(bytes, length + 14);

		return true;
	}
	
	public boolean fileSend(byte[] input, int length) {
		m_sHeader.enet_type = intToByte2(0x2090);
		byte[] bytes = ObjToByte(m_sHeader, input, length);
		this.GetUnderLayer().Send(bytes, length + 14);
		
		return true;
	}
	
	/*
	 * ARP 전송
	 * input : ARP 메시지, length : ARP 길이
	 */
	public boolean ARPSend(byte[] input, int length) { 
		m_sHeader.enet_type = intToByte2(0x0806);            // ARP type = 0x0806
		m_sHeader.enet_data = input;                         // data = input
		byte[] bytes = ObjToByte(m_sHeader, input, length);  // 헤더와 연결하여 배열로 저장
		this.GetUnderLayer().Send(bytes, length+14);         // 하위 계층으로 전송
		return true;
	}

	/* 헤더 제거 후 data만을 반환 */
	public byte[] RemoveEthernetHeader(byte[] input, int length) {
		byte[] data = new byte[length - 14];

		System.arraycopy(input, 14, data, 0, length - 14);
		
		return data;
	}
	
	public boolean Receive(byte[] input) {
		byte[] data;
		
		int temp_type = byte2ToInt(input[12], input[13]);
		
		/* Chatting */
		if(temp_type == Integer.decode("0x2080")) {
			System.out.println("Chat Receive!");
			if(chkAddr(input) || (isBroadcast(input)) || !isMyPacket(input)) {
				data = RemoveEthernetHeader(input, input.length);
				this.GetUpperLayer(0).Receive(data);
				return true;
			}
		}
		/* File */
		else if(temp_type == Integer.decode("0x2090")) {
			System.out.println("File Receive!");
			if(chkAddr(input) || (isBroadcast(input)) || !isMyPacket(input)) {
				data = RemoveEthernetHeader(input, input.length);
				this.GetUpperLayer(1).Receive(data);
				return true;
			}
		}
		/* ARP */
		else if(temp_type == Integer.decode("0x0806")) {
			System.out.println("ARP Receive!");
			this.ARPReceive(input);  // ARP 수신 함수 호출
		}
		
		/* Ping */
		else if(temp_type == Integer.decode("0x0800")) {
			System.out.println("Ping Receive!");
			if(chkAddr(input) || (isBroadcast(input)) || !isMyPacket(input)) {
				data = RemoveEthernetHeader(input, input.length);
				((IPLayer) this.GetUpperLayer(1)).Receive(data);
				return true;
			}
		}
		
		return false; 
	}
	
	public boolean ARPReceive(byte[] input) {
		byte[] data;
			
		if(!isMyPacket(input) || (chkAddr(input) || (isBroadcast(input)))) {
			data = RemoveEthernetHeader(input, input.length);
			((ARPLayer) this.GetUpperLayer(0)).ARPReceive(data);
			return true;
		}
	
		return false;
	}
	
	/* 패킷 송신지와 자신의 MAC 주소가 같은지 확인 */
	public boolean isMyPacket(byte[] input) {
		for(int i = 0; i < 6; i++)
	         if(m_sHeader.enet_srcaddr.addr[i] != input[6 + i])
	            return false;
		
	    return true;
	}

	/* 타겟 MAC 주소가 브로드캐스트인지 확인 */
	public boolean isBroadcast(byte[] input) {
		for (int i = 0; i < 6; i++)
			if (input[i] != 0xff)
				return false;
		
		return true;
	}
	
	/* 자신의 MAC 주소가 패킷의 타겟 MAC 주소와 동일한지 확인 */
	private boolean chkAddr(byte[] input) {
		for(int i = 0; i< 6; i++)
			if(m_sHeader.enet_srcaddr.addr[i] != input[i])
				return false;
		
		return true;
	}
	
	public _ETHERNET_ADDR GetEnetDstAddress() {
		return m_sHeader.enet_dstaddr;
	}
	
	public _ETHERNET_ADDR GetEnetSrcAddress() {
		return m_sHeader.enet_srcaddr;
	}
	
	public void SetEnetType(byte[] input) {
		for (int i = 0; i < 2; i++) {
			m_sHeader.enet_type[i] = input[i];
		}
	}
	
	public void SetEnetSrcAddress(byte[] input) {
		for (int i = 0; i < 6; i++) {
			m_sHeader.enet_srcaddr.addr[i] = input[i];
		}
	}
	
	public void SetEnetDstAddress(byte[] input) {
		for (int i = 0; i < 6; i++) {
			m_sHeader.enet_dstaddr.addr[i] = input[i];
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
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}
}