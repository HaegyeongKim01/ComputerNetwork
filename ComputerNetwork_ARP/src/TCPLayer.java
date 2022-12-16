import java.util.ArrayList;

public class TCPLayer implements BaseLayer{
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	private class _TCP_HEADER { // tcp layer의 header
		byte[] tcp_sport;	// source port
		byte[] tcp_dport;	// destination port
		byte[] tcp_seq;		// sequence number
		byte[] tcp_ack;		// acknowledge sequence
		byte[] tcp_offset;	// no use
		byte[] tcp_flag;	// control flag
		byte[] tcp_window;	// no use
		byte[] tcp_cksum;	// check sum
		byte[] tcp_urgptr;	// no use
		byte[] padding;		
		byte[] tcp_data;

		public _TCP_HEADER() { // header 생성자
			this.tcp_sport = new byte[2];	// tcp_sport 2byte
			this.tcp_dport = new byte[2];	// tcp_dport 2byte
			this.tcp_seq = new byte[4];		// tcp_seq 4byte
			this.tcp_ack = new byte[4];		// tcp_ack 4byte
			this.tcp_offset = new byte[1];	// tcp_offset 1byte
			this.tcp_flag = new byte[1];	// tcp_flag 1byte
			this.tcp_window = new byte[2];	// tcp_window 2byte
			this.tcp_cksum = new byte[2];	// tcp_cksum 2byte
			this.tcp_urgptr = new byte[2];	// tcp_urgptr 2byte
			this.padding = new byte[4]; 	// padding 4byte
			this.tcp_data = null;
		}
	}
	
	_TCP_HEADER m_sHeader = new _TCP_HEADER(); // header 객체 생성
	
	public TCPLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		ResetHeader();
	}
	
	public void ResetHeader() {
		for(int i = 0 ; i < 2; i++) {
			m_sHeader.tcp_sport[i] = (byte) 0x00;
			m_sHeader.tcp_dport[i] = (byte) 0x00;
			m_sHeader.tcp_window[i] = (byte) 0x00;
			m_sHeader.tcp_cksum[i] = (byte) 0x00;
			m_sHeader.tcp_urgptr[i] = (byte) 0x00;
		}
		for(int i = 0 ; i < 4; i++) {
			m_sHeader.tcp_seq[i] = (byte) 0x00;
			m_sHeader.tcp_ack[i] = (byte) 0x00;
			m_sHeader.padding[i] = (byte) 0x00;
		}
		m_sHeader.tcp_offset[0] = (byte) 0x00;
		m_sHeader.tcp_flag[0] = (byte) 0x00;
		m_sHeader.tcp_data = null;
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
	
	public void ARPSend(byte[] src, byte[] dst) { // arp 전송 메소드
		((IPLayer) this.GetUnderLayer()).ARPSend(src, dst); // 받은 송수신지 주소를 ip layer로 전달
	}

	
}