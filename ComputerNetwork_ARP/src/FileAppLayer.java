import java.io.*;
import java.util.ArrayList;

/* 파일 데이터 송수신을 위한 계층 */
public class FileAppLayer implements BaseLayer {
    private int count = 0;
    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
    private String fileName;         // 파일 이름
    private int receivedLength = 0;  // 수신한 데이터의 크기
    private int targetLength = 0;    // 수신해야하는 파일의 총 크기

    private File file;                       // 저장할 파일
    private ArrayList<byte[]> fileByteList;  // 수신한 파일 프레임(정렬 전)
    private ArrayList<byte[]> fileSortList;  // 수신한 파일을 정렬 하는데 사용하는 리스트
    private ArrayList<Boolean> ackChk =new ArrayList<Boolean>();   // ACK 저장할 리스트

    public FileAppLayer(String pName) {
        // TODO Auto-generated constructor stub
        pLayerName = pName;
        fileByteList = new ArrayList();
        ackChk.add(true);
    }

    /* File layer에서 사용되는 프레임 구조체 */
    public class _FAPP_HEADER {
        byte[] fapp_totlen;  // 파일 전체 길이
        byte[] fapp_type;    // 파일 단편화 정보 저장, 0x00 -> 단편화 X, 0x01 -> 단편화 첫부분, 0x02 -> 단편화 마지막
        byte fapp_msg_type;  // fileInfo인지 파일 데이터인지 구분, 0 -> fileInfo, 1 -> 파일 데이터
        byte fapp_unused;
        byte[] fapp_seq_num; // 단편화 조각 순서
        byte[] fapp_data;    // 파일 데이터

        /* 프레임 생성자 */
        public _FAPP_HEADER() {
            this.fapp_totlen = new byte[4];   // FileAppLayer의 헤더는 크기와 구성이 약간 다름 
            this.fapp_type = new byte[2];     // 파일 단편화 정보
            this.fapp_msg_type = 0x00;        // 파일 정보인지 데이터인지를 구분
            this.fapp_unused = 0x00;
            this.fapp_seq_num = new byte[4];  // 몇번째 단편화 조각인지를 구분
            this.fapp_data = null;            // 파일 데이터
        }
    }

    _FAPP_HEADER m_sHeader = new _FAPP_HEADER();  // 프레임 생성

    /* 헤더의 단편화 정보를 설정 */
    private void setFragmentation(int type) {
    	/* 처음 */
    	if(type == 0) {
            m_sHeader.fapp_type[0] = (byte) 0x0;
            m_sHeader.fapp_type[1] = (byte) 0x0;
        }
    	/* 중간 */
        else if(type == 1) {
            m_sHeader.fapp_type[0] = (byte) 0x0;
            m_sHeader.fapp_type[1] = (byte) 0x1;
        }
    	/* 끝 */
        else if(type == 2) {
            m_sHeader.fapp_type[0] = (byte) 0x0;
            m_sHeader.fapp_type[1] = (byte) 0x2;
        }
    }

    /* 파일 정보인지, 파일 데이터인지 구분해주는 fapp_msg_type 값을 설정 */
    public void setFileMsgType(int type) {
        m_sHeader.fapp_msg_type = (byte) type;
    }

    /* 파일 이름 설정자 */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    // 파일 크기 설정자
    public void setFileSize(int fileSize) {
        m_sHeader.fapp_totlen[0] = (byte)(0xff&(fileSize >> 24));
        m_sHeader.fapp_totlen[1] = (byte)(0xff&(fileSize >> 16));
        m_sHeader.fapp_totlen[2] = (byte)(0xff&(fileSize >> 8));
        m_sHeader.fapp_totlen[3] = (byte)(0xff & fileSize);
    }

    /* 몇 번째 Frame인지 계산(Frame은 0번부터 시작) */
    public int calcSeqNum(byte[] input) {
        int seqNum = 0;
        seqNum += (input[8] & 0xff) << 24;
        seqNum += (input[9] & 0xff) << 16;
        seqNum += (input[10] & 0xff) << 8;
        seqNum += (input[11] & 0xff);

        return seqNum;
    }

    /* 파일의 총 크기를 계산 */
    public int calcFileFullLength(byte[] input) {
        int fullLength = 0;
        fullLength += (input[0] & 0xff) << 24;
        fullLength += (input[1] & 0xff) << 16;
        fullLength += (input[2] & 0xff) << 8;
        fullLength += (input[3] & 0xff);
        return fullLength;
    }

    /* 파일 정보 전송 */
    public boolean fileInfoSend(byte[] input, int length) {
        this.setFileMsgType(0);    // 파일 정보 송신임을 나타냄
        this.Send(input, length);  // 파일 정보 전송

        return true;
    }

    /* 프레임을 다 받았는지 확인 후, 모두 정확히 수신했으면 정렬을 진행 */
    public boolean sortFileList(int lastFrameNumber) {
        /* 모든 프레임을 받지 못한 경우 */
        if((fileByteList.size() - 1 != lastFrameNumber) || (receivedLength != targetLength)) {
            ((ARPDlg)this.GetUpperLayer(0)).ChattingArea.append("파일 수신 실패\n"); // 수신 실패 메시지
            return false;
        }
        /*
         * 모든 프레임을 받은 경우
         * ArrayList에 SeqNum을 Index로 가지도록 삽입하여 정렬 진행
         */
        fileSortList = new ArrayList();  // 정렬된 파일 데이터를 저장할 리스트
        
        for(int checkSeqNum = 0; checkSeqNum < (lastFrameNumber + 1); ++checkSeqNum) {
            byte[] checkByteArray = fileByteList.remove(0);     // 받은 파일 리스트에서 순차적으로 byte array 가져오기
            int arraySeqNum = this.calcSeqNum(checkByteArray);  // 가져온 byte array가 몇번째 프레임인지 계산
            fileSortList.add(arraySeqNum, checkByteArray);      // 정렬 리스트의 계산한 프레임 순서에 가져온 array 삽입
        }

        return true;
    }
    
    /* thread를 생성하며 인자로 전달할 객체 클래스 */
    class fileTransfer implements Runnable {
       public fileTransfer() {}
       
       /* thread 실행시 수행할 메소드 */
       @Override
       public void run() {
          setAndStartSendFile();
       }
    }
    
    public void sendFile() { 
       fileTransfer temp = new fileTransfer();
       Thread fileTransferThread = new Thread(temp);  // Runnable형 인자를 받아 thread 생성
       fileTransferThread.start();  // thread 실행
    }
    
    /* 파일 전송을 수행할 메소드 */
    public void setAndStartSendFile() {
        ARPDlg upperLayer = (ARPDlg) this.GetUpperLayer(0);
        File sendFile = upperLayer.getFile();  // 하위 계층으로 전달해야할 파일
        int sendTotalLength;                   // 보내야하는 총 크기
        int sendedLength;                      // 현재 보낸 크기
        this.resetSeqNum();                    // 파일 프레임 순서를 나타내는 fapp_seq_num 초기화

        try (FileInputStream fileInputStream = new FileInputStream(sendFile)) {  // 파일스트림을 가져 옴
            sendedLength = 0; // 전송된 데이터 길이 초기화
            
            // 파일스트림을 읽어 올 버퍼스트림
            BufferedInputStream fileReader = new BufferedInputStream(fileInputStream);
            sendTotalLength = (int)sendFile.length();  // 전송해야 할 파일 길이
            this.setFileSize(sendTotalLength);         // 헤더에 fapp_totlen 설정
            byte[] sendData =new byte[1448];           // 전송할 파일 데이터를 저장할 배열, 파일 데이터는 1448단위로 전송
            
            // progressbar의 최댓값을 파일 전체 길이로 설정
            ((ARPDlg)this.GetUpperLayer(0)).progressBar.setMaximum(sendTotalLength);
            
            /* 단편화 없이 전송 */
            if(sendTotalLength <= 1448) {
                setFragmentation(0);    // 파일 정보, 단편화 X
                this.setFileMsgType(0); // 파일 정보임을 나타냄
                this.fileInfoSend(sendFile.getName().getBytes(), sendFile.getName().getBytes().length); // 파일 정보 전송

                /* 파일 데이터 전송 */
                this.setFileMsgType(1);                // 파일 데이터임을 나타냄 + 여전히 setFragmentation(0)
                fileReader.read(sendData);             // 버퍼에서 1448만큼 데이터를 읽어옴
                this.Send(sendData, sendData.length);  // 데이터 전송
                sendedLength += sendData.length;       // 보낸 데이터 길이에 전송한 데이터 길이 더함
                ((ARPDlg)this.GetUpperLayer(0)).progressBar.setValue(sendedLength); // progressbar 현재 진행값 업데이트
            }
            /* 파일 길이가 1448보다 클때 단편화 필요 */
            else {
                sendedLength = 0;
                
                /* 파일 정보 전송 */
                this.setFragmentation(0);  // 파일 정보
                this.setFileMsgType(0);    // 파일 정보임을 나타냄
                this.fileInfoSend(sendFile.getName().getBytes(), sendFile.getName().getBytes().length);

                /* 파일 데이터 전송 */
                this.setFileMsgType(1);    // 파일 데이터임을 나타냄 
                this.setFragmentation(1);  // 단편화 중간부분으로 변경
                
                /* 중간 데이터이면 */
                while(fileReader.read(sendData) != -1 && (sendedLength + 1448 < sendTotalLength)) {
                    this.Send(sendData, 1448); // 1448크기만큼 전송
                    
                    try {
                        Thread.sleep(4);  // 잠시 전송을 기다림
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    sendedLength += 1448;  // 보낸 데이터 크기 증가
                    this.increaseSeqNum(); // 단편화 조각 순서값 증가
                    ((ARPDlg)this.GetUpperLayer(0)).progressBar.setValue(sendedLength); // progressbar 현재 진행값 업데이트
                }

                byte[] getRealDataFrame = new byte[sendTotalLength - sendedLength];  // 마지막 데이터를 담을 배열
                this.setFragmentation(2);  // 단편화 마지막으로 변경
                fileReader.read(sendData); // 남은 데이터 모두 읽어오기
                
                // getRealDataFrame에 마지막 파일 데이터 담기
                for(int index = 0; index < getRealDataFrame.length; ++index) {
                    getRealDataFrame[index] = sendData[index];
                }

                this.Send(getRealDataFrame, getRealDataFrame.length);
                sendedLength += getRealDataFrame.length;  // 보낸 데이터 크기 증가
                count = 0;
                ((ARPDlg)this.GetUpperLayer(0)).progressBar.setValue(sendedLength);  // progressbar 현재 진행값 업데이트
            }
            
            fileInputStream.close();  // 파일 스트림 닫기
            fileReader.close();       // 버퍼 스트림 닫기
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /* FileApp의 Header를 제거 */
    private byte[] RemoveCappHeader(byte[] input, int length) {
        byte[] buf = new byte[length - 12];  // 프레임 길이에서 헤더 길이만큼 제거한 배열
        
        for(int dataIndex = 0; dataIndex < length - 12; ++dataIndex)
            buf[dataIndex] = input[12 + dataIndex];  // 헤더를 제거한 데이터만 buf에 저장

        return buf;
    }
    
    /* ACK를 체크하고 ACK가 들어올 때까지 대기 */
    public void waitACK() {
       while(ackChk.size()<=0 ){
          try{
             Thread.sleep(10);
          }catch(InterruptedException e){
             e.printStackTrace();
          }
       }
       ackChk.remove(0);
    }

    /* 데이터를 수신 처리 */
    public synchronized boolean Receive(byte[] input) {
        byte[] data;
        
        if(input==null){ 
           ackChk.add(true);  // ACK 수신
           return true;
        }

        /* 파일의 정보를 받은 경우, 파일 이름과 길이 정보 */
        if(checkReceiveFileInfo(input)) {
            data = RemoveCappHeader(input, input.length);  // Header 없애기
            String fileName = new String(data);            // 파일 이름 저장
            fileName = fileName.trim();                    // 공백 제거
            targetLength = calcFileFullLength(input);      // 받아야 하는 파일 총 크기 초기화
            file = new File("./" + fileName);              // 받는 경로, 현재 경로

            /* 수신 측 Progressbar 초기화 */
            ((ARPDlg)this.GetUpperLayer(0)).progressBar.setMinimum(0);
            ((ARPDlg)this.GetUpperLayer(0)).progressBar.setMaximum(targetLength);
            ((ARPDlg)this.GetUpperLayer(0)).progressBar.setValue(0);

            receivedLength = 0;  // 받은 파일 크기 초기화
        }
        else {
            /* 단편화를 하지 않은 데이터를 받은 경우 */
            if (checkNoFragmentation(input)) {
                data = RemoveCappHeader(input, input.length);     // 헤더 없애기
                fileByteList.add(this.calcSeqNum(input), data);   // 받은 프레임 seqnum과 받은 파일 데이터 저장
                
                try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    fileOutputStream.write(fileByteList.get(0));  // 저장할 파일 스트림을 열어 받은 데이터를 작성
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            /* 단편화를 진행한 데이터를 받은 경우 */
            else {
                /* 데이터 프레임 수신 */
                fileByteList.add(input);  // 마지막 프레임을 수신하고, 헤더의 seqnum을 확인하여 정렬해야 하므로 헤더 제거 없이 프레임 그대로 저장
                receivedLength += (input.length - 12);  // 받은 데이터 길이 증가, 헤더의 길이는 제외

                /* 마지막 프레임 수신 */
                if(checkLastDataFrame(input)) {
                    int lastFrameNumber = this.calcSeqNum(input);  // 마지막 프레임의 seqnum 확인

                    /* 프레임을 모두 받았으면 */
                    if(sortFileList(lastFrameNumber)) {
                        try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        	// 저장할 파일 스트림을 열어 총 프레임 개수만큼 반복하며
                            for (int frameCount = 0; frameCount < (lastFrameNumber + 1); ++frameCount) {
                            	// 헤더를 제거
                                data = RemoveCappHeader(fileSortList.get(frameCount), fileSortList.get(frameCount).length);
                                fileOutputStream.write(data);  // 파일 스트림에 데이터 작성
                            }
                            
                            ((ARPDlg)this.GetUpperLayer(0)).ChattingArea.append("파일 수신 및 생성 완료\n");  // 수신 완료 메시지
                            fileByteList = new ArrayList();
                        } catch (FileNotFoundException e) {
                            ((ARPDlg)this.GetUpperLayer(0)).ChattingArea.append("파일 수신 실패\n");
                            e.printStackTrace();
                        } catch (IOException e) {
                            ((ARPDlg)this.GetUpperLayer(0)).ChattingArea.append("파일 수신 실패\n");
                            e.printStackTrace();
                        }
                    }
                }
                ((ARPDlg)this.GetUpperLayer(0)).progressBar.setValue(receivedLength);  // Progressbar 갱신
            }
        }
        
        ((EthernetLayer)this.GetUnderLayer()).fileSend(null, 0);  // ACK 송신
        return true;
    }

    /* seqnum을 초기화 */
    public void resetSeqNum() {
        this.m_sHeader.fapp_seq_num[0] = (byte)0x0;
        this.m_sHeader.fapp_seq_num[1] = (byte)0x0;
        this.m_sHeader.fapp_seq_num[2] = (byte)0x0;
        this.m_sHeader.fapp_seq_num[3] = (byte)0x0;
    }

    /* Frame 번호 증가 함수(Send시 Frame 번호 값 변경) */
    public void increaseSeqNum() {
        if((this.m_sHeader.fapp_seq_num[3] & 0xff) < 255)
            ++this.m_sHeader.fapp_seq_num[3];
        else if((this.m_sHeader.fapp_seq_num[2] & 0xff) < 255) {
            ++this.m_sHeader.fapp_seq_num[2];
            this.m_sHeader.fapp_seq_num[3] = 0;
        } else if((this.m_sHeader.fapp_seq_num[1] & 0xff) < 255) {
            ++this.m_sHeader.fapp_seq_num[1];
            this.m_sHeader.fapp_seq_num[2] = 0;
            this.m_sHeader.fapp_seq_num[3] = 0;
        } else if((this.m_sHeader.fapp_seq_num[0] & 0xff) < 255) {
            ++this.m_sHeader.fapp_seq_num[0];
            this.m_sHeader.fapp_seq_num[1] = 0;
            this.m_sHeader.fapp_seq_num[2] = 0;
            this.m_sHeader.fapp_seq_num[3] = 0;
        }
    }

    /* 데이터 전송 함수 */
    public boolean Send(byte[] input, int length) {
        byte[] bytes = this.ObjToByte(m_sHeader, input, length);  // 헤더와 데이터를 합친 프레임을
        waitACK();
        ((EthernetLayer)this.GetUnderLayer()).fileSend(bytes, length + 12);  // 하위 계층으로 전달
        
        return true;
    }

    /* 받은 헤더와 데이터를 바이트 프레임 배열에 넣어줌 */
    private byte[] ObjToByte(_FAPP_HEADER m_sHeader, byte[] input, int length) {
       byte[] buf = new byte[length + 12];  // header 길이 + 데이터 길이
       
        // totlen 4bytes
        buf[0] = m_sHeader.fapp_totlen[0];
        buf[1] = m_sHeader.fapp_totlen[1];
        buf[2] = m_sHeader.fapp_totlen[2];
        buf[3] = m_sHeader.fapp_totlen[3];
        
        // type 2bytes
        buf[4] = m_sHeader.fapp_type[0];
        buf[5] = m_sHeader.fapp_type[1];
        
        // msg_type 1bytes
        buf[6] = m_sHeader.fapp_msg_type;
        
        // unused 1bytes
        buf[7] = m_sHeader.fapp_unused;
        
        // seq_num 4bytes
        buf[8] = m_sHeader.fapp_seq_num[0];
        buf[9] = m_sHeader.fapp_seq_num[1];
        buf[10] = m_sHeader.fapp_seq_num[2];
        buf[11] = m_sHeader.fapp_seq_num[3];

        for(int dataIndex = 0; dataIndex < length; ++dataIndex)
            buf[12 + dataIndex] = input[dataIndex];

        return buf;
    }
    
    /* 받은 데이터가 파일 정보인지 확인 */
    public boolean checkReceiveFileInfo(byte[] input) {
        if(input[6] == (byte)0x00)
            return true;

        return false;
    }
    
    /* 마지막 Frame인지 확인 */
    public boolean checkLastDataFrame(byte[] input) {
        if(input[4] == (byte) 0x0 && input[5] == (byte)0x0)
            return true;
        else if(input[4] == (byte) 0x0 && input[5] == (byte)0x2)
            return true;
        else
            return false;
    }

    /* File 데이터가 단편화를 진행하지 않았는지 검사 */
    public boolean checkNoFragmentation(byte[] input) {
        if(input[4] == (byte) 0x00 && input[5] == (byte)0x0)
            return true;

        return false;
    }

    @Override
    public String GetLayerName() {
        return pLayerName;
    }

    @Override
    public BaseLayer GetUnderLayer() {
        if(p_UnderLayer == null)
            return null;
        return p_UnderLayer;
    }

    @Override
    public BaseLayer GetUpperLayer(int nindex) {
        if(nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
            return null;
        return p_aUpperLayer.get(nindex);
    }

    @Override
    public void SetUnderLayer(BaseLayer pUnderLayer) {
        if(pUnderLayer == null)
            return;
        this.p_UnderLayer = pUnderLayer;
    }

    @Override
    public void SetUpperLayer(BaseLayer pUpperLayer) {
        if(pUpperLayer == null)
            return;
        this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
    }

    @Override
    public void SetUpperUnderLayer(BaseLayer pUULayer) {
        this.SetUpperLayer(pUULayer);
        pUULayer.SetUnderLayer(this);
    }
}