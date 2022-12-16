import java.util.ArrayList;

public class IPLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	
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
		byte[] ip_data; 	// variable length data

		/* 헤더 생성자 */
		public _IP_HEADER() {
			this.ip_src = new _IP_ADDR(); 	// 송신지 주소가 될 _IP_ADDR
			this.ip_dst = new _IP_ADDR();	// 수신지 주소가 될 _IP_ADDR 
			this.ip_len = new byte[2];		// ip_len 2byte
			this.ip_id = new byte[2]; 		// ip_id 2byte
			this.ip_fragoff = new byte[2]; 	// ip_fragoff 2byte
			this.ip_cksum = new byte[2]; 	// ip_cksum 2byte
			this.ip_data = null;
		}
	}

	_IP_HEADER m_sHeader = new _IP_HEADER();  // 헤더 객체 생성

	public IPLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		ResetHeader();
	}
	
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
		this.SetIpDstAddress(dst);  // 수신지 IP 주소 설정
		this.SetIpSrcAddress(src);  // 송신지 IP 주소 설정
		((ARPLayer) this.GetUnderLayer()).ARPSend(src, dst); // ARPLayer로 전달
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