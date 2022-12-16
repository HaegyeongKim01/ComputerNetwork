import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class RoutingTable {
	public ArrayList<ArrayList<byte[]>> routingTable = new ArrayList<ArrayList<byte[]>>(); // routing table array list
	
	public void add(byte[] dst, byte[] subnet, byte[] gateway, byte[] flag, byte[] _interface) { // routing table entry 추가
		ArrayList<byte[]> entry = new ArrayList<byte[]>();
		entry.add(dst);
		entry.add(subnet);
		entry.add(gateway);
		entry.add(flag);
		entry.add(_interface);
		this.routingTable.add(entry);
	}
	public void remove() { // routing table entry 삭제
		routingTable = new ArrayList<ArrayList<byte[]>>();
	}
	public ArrayList<byte[]> getEntry(int idx) { // 일치하는 routing entry 반환
		return this.routingTable.get(idx);
	}
	
	public int size() { // routing table의 size
		return this.routingTable.size();
	}
	
	public byte[] subnetmasking(byte[] dst_ip, byte[] subnet) { // subnetmasking을 수행하는 함수
	      byte[] network_address = new byte[4]; // masking 결과 값을 저장할 배열
	      for(int i = 0; i < 4; i++) {
	         network_address[i] = (byte) (dst_ip[i] & subnet[i]); // 주소와 subnet을 & 연산
	      }
	      return network_address;
	   }
	
	public int matchEntry(byte[] dst_ip) { // ip 주소에 해당하는 routing entry의 index를 반환
		int matchIdx = this.routingTable.size() - 1;
		byte[] matchIp = this.routingTable.get(this.routingTable.size() - 1).get(0);
		
		for(int i = 0; i<this.routingTable.size() - 1; i++) {
			ArrayList<byte[]> temp = this.routingTable.get(i); // routing table에서 entry 가져오기
			byte[] result_ip = this.subnetmasking(dst_ip, temp.get(1)); //routing table에서 subnetmask를 가져와 dst_ip와 masking
			
			if(Arrays.equals(temp.get(0), result_ip)) { // masking 결과가 routing table entry의 dst와 같으면
				if(ByteBuffer.wrap(matchIp).getInt() < ByteBuffer.wrap(result_ip).getInt()) {
					matchIdx = i; // 매칭되는 index를 i로 설정
					matchIp = result_ip; // ip 주소를 masking 결과 주소로 설정
				}
			}
		}
		return matchIdx;
	}
}