# :book: ComputerNetwork

### Java로 Router 구현  
virtual machine 3개로 프로젝트를 진행하였다 HOST1, HOST2, ROUTER
 <img src="https://user-images.githubusercontent.com/110768149/208080043-6a0852e7-865c-4a17-97c1-539f0c206591.png"/><br><br>
 <img width = "70%" src="https://user-images.githubusercontent.com/110768149/208080035-7306f668-f1a9-44b7-838e-b93116ea8ac5.png"/><br><br>
 <img src="https://user-images.githubusercontent.com/110768149/208080048-3eb0a401-32f0-4414-a2a3-0cec4895923a.png"/><br><hr>
 
 <h3> :bulb: Class 설명</h3>
<b>Class “ApplicationLayer”</b><br>
최상위 계층이며 GUI 코드가 구현된 클래스로 화면에 ARP Cache table과 Proxy Entry 설정창, 채팅 및 파일 전송 창, Routing Table item 추가 창, Routing Table을 보여준다. 두개의 NI Layer, Ethernet Layer, ARP Layer, IP Layer를 연결한다. 각 버튼을 눌렀을 때 하위레이어의 함수를 호출해 데이터를 전달한다.

<b>Class “RoutingTable”</b><br>
ArrayList 형태의 Routing Table 객체를 갖는다. routing table 요소를 추가하고 삭제하는 함수와 subnet masking을 수행하는 함수, 파라미터로 전달받은 ip 주소에 해당하는 item을 돌려주는 함수를 구현한다.

<b>Class “ChatAppLayer”</b><br>
응용 계층으로, 채팅 데이터를 주고받는다. Send() 함수를 이용하여 데이터를 송신하고, waitACK() 함수를 통해 Stop&Wait 프로토콜을 따른다. 길이와 타입을 헤더에 담고 길이에 따라 단편화 여부를 결정한다. 1456 바이트 이상일 경우 데이터를 단편화하여 순서를 설정한 뒤 차례대로 전송한다. 조각을 전송할 때마다 ACK를 확인하여 앞선 조각이 정상적으로 도착하였는지 확인한다. 수신은 Receive() 함수를 이용한다. 아무 데이터가 없으면 ACK에 해당하므로 ACK 도착을 알리고, 이외의 경우 단편화 여부를 확인하여 데이터를 수신한다. 이때 헤더를 제거한 뒤 순서대로 정렬하여 상위 계층으로 보낸다. 데이터 수신이 완료되면 ACK를 전송하여 정상적으로 수신했음을 알린다.

<b>Class “FileAppLayer”</b><br>
ChatAppLayer와 같은 응용 계층에 위치하며 파일 데이터를 주고받는다. 파일의 길이를 측정하여 1448 바이트 이상이면 단편화 후 전송한다. 이때, 전송 현황에 따른 progress bar가 실시간으로 증가하게끔 쓰레드를 생성하여 전송 함수 setAndStartSendFile()을 호출한다. 조각 하나를 전송 완료할 때마다 progress bar를 업데이트하여 실시간 진행률을 확인한다. 수신을 위한 Receive() 함수 역시 조각을 수신할 때마다 progress bar를 업데이트한다. 단편화 여부를 확인한 뒤 헤더를 제거하고 순서대로 정렬하여 상위 계층으로 보낸다.

<b>Class “IPLayer”</b><br>
네트워크 계층에 해당하며 routing을 수행한다 IP 헤더에 필요한 정보(Destination IP, Source IP 등)를 담아서 송신한다. routing table을 보며 해당하는 entry를 찾아 다음 경로를 설정한다.

<b>Class “ARPLayer”</b><br>
전달받은 메시지를 이용하여 테이블에 IP와 MAC 주소를 mapping하여 기록한다. ARP request가 broadcasting일 경우, 테이블에 ARP request를 보낸 sender의 IP와 MAC 주소를 저장하고 목적지 주소를 확인해 자신의 주소가 아니면 버리고, 맞으면 ARP message의 target Ethernet address에 자신의 MAC 주소를 넣고 reply한다.

<b>Class “EthernetLayer”</b><br>
데이터 링크 계층에 해당한다. 데이터를 보내기 위해 송수신 주소를 헤더에 저장하고 상위 계층의 종류에 따라 type을 구분한다. ACK를 사용하는 채팅 데이터 송신 함수 Send()에서는 ACK, 브로드캐스트, 채팅 데이터의 type을 구분하여 헤더에 저장하고 하위 계층으로 보낸다. 파일 데이터 송신 함수 fileSend() 역시 정해진 type을 지정하여 하위 계층으로 보낸다. 수신 시에는 ACK 여부를 확인하여 ACK일 경우 상위 계층에 알린다. 데이터일 경우 브로드캐스트인지, 수신지가 자신과 동일한지 확인한다. 정상적인 데이터라면 헤더를 제거하고 타입에 따라 해당하는 상위 계층에 전송한다. 이후 ACK를 보내어 수신 완료를 알린다.

<b>Class “NILayer”</b><br>
물리 계층으로, 쓰레드를 생성하여 파일과 채팅 전송이 동시에 이루어지도록 한다. 네트워크 어댑터의 sendPacket()을 통해 패킷을 전송하고 실패한 경우 에러 메시지를 출력한다.
