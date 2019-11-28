import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.lang.*;

class ClientSocket extends Thread {

    protected int peerId;
    protected Socket socket;
    protected ObjectOutputStream oStream;
    protected ObjectInputStream iStream;

    public String peerName = this.getName();

    protected HashMap<Integer, byte[]> workingChunk;

    protected String fileName = "Test.dat";

    public void InitiateFileBlock(HashMap<Integer, byte[]> fileChunk) {
        this.workingChunk = fileChunk;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void intialiseSocket(Socket socket) {
        this.socket = socket;
        System.out.println("[" + peerName + "] get connected from " + socket.getPort());
        try {
            oStream = new ObjectOutputStream(this.socket.getOutputStream());
            iStream = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(Object message) throws IOException {
        oStream.writeObject(message);
        oStream.flush();
        oStream.reset();
    }

    public void send(int message) throws IOException {
        oStream.writeInt(message);
        oStream.flush();
        oStream.reset();
    }

    public void initiatePeerId(int id) {
        this.peerId = id;
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("[" + this.peerName + "] Peer is listening command:");
                Object payload = this.iStream.readObject();
                assert (payload instanceof String);
                String message = (String) payload;
                System.out.println("[" + this.peerName + "] Received message (" + message + ")");
                int p = -1;
                switch (message) {
                    case "LIST":
                        // Send chunk list
                        ArrayList<Integer> q = new ArrayList<Integer>(this.workingChunk.size());
                        for (Integer key : this.workingChunk.keySet()) {
                            q.add(key);
                        }
                        send(q);
                        break;
                    case "REQUEST":
                        // Read first int as chunk number
                        p = this.iStream.readInt();
                        // Send that chunk
                        send(p);
                        send(this.workingChunk.get(p));
                        break;
                    case "ASK":
                        p = this.iStream.readInt();
                        if (this.workingChunk.containsKey(p)) {
                            send(1);
                        } else {
                            send(0);
                        }
                        break;
                    case "DATA":
                        // Read first int as chunk number
                        p = this.iStream.readInt();
                        // Save received data
                        byte[] chunk = (byte[]) this.iStream.readObject();
                        if (!this.workingChunk.containsKey(p)) {
                            workingChunk.put(p, chunk);
                            System.out.println("Received Chunk #" + p);
                            saveChunkFile(p, chunk);
                        }
                        break;
                    case "CLOSE":
                        // Close stream and exit thread
                        oStream.close();
                        iStream.close();
                        return;
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                System.out.println("[" + this.getName() + "]: Session ended.");
                return;
            }
        }
    }

    private void saveChunkFile(int x, byte[] chunk) {
        try {
            FileOutputStream fso = new FileOutputStream("PEER" + this.peerId + "Dir/" + x, false);
            fso.write(chunk);
            fso.flush();
            fso.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class Peer extends Thread {
    int server_port = 0;
    int peer_port = 0;
    int _download_port = 0;
    static int peer_id = -1;
    private int port_DL = -1;
    private int peer_DL = -1;
    private int port_UL = -1;
    private int peer_UL = -1;
    public ServerSocket peer_skt;

    public static int peer_self_port = -2;
    public static String peer_name = "";
    public final static int MAX_PEER = 5;

    public static String merge_file_name = "merge.dat";

    public static HashMap<Integer, byte[]> list_block_file = new HashMap<Integer, byte[]>();
    public static ArrayList<Integer> block_indx = new ArrayList<Integer>();
    public static HashMap<Integer, Integer> peer_list = new HashMap<Integer, Integer>();

    public Peer(int _serverPort, int _peer_port, int _client_port) {
        this.server_port = _serverPort;
        this.peer_port = _peer_port;
        this._download_port = _client_port;
    }

    private void getInitialChunksFromServer(ObjectOutputStream oStream, ObjectInputStream iStream)
            throws IOException, ClassNotFoundException {
        int last = (int) (1.0 * block_indx.size() / MAX_PEER * ((peer_id % MAX_PEER) + 1));
        int begin = (int) (1.0 * block_indx.size() / MAX_PEER * (peer_id % MAX_PEER));

        for (int i = begin; i < last; i++) {
            TransmitMessageToOwner("REQUEST", oStream);
            TransmitMessageToOwner(block_indx.get(i), oStream);
            int f = iStream.readInt();
            byte[] chunk = (byte[]) iStream.readObject();
            list_block_file.put(f, chunk);
            System.out.println("Received Block #" + block_indx.get(i) + " from owner");
            saveChunkFile(f, chunk);
        }
    }

    private void getChuckList(ObjectOutputStream oStream, ObjectInputStream iStream)
            throws IOException, ClassNotFoundException {
        TransmitMessageToOwner("LIST", oStream);
        block_indx = (ArrayList<Integer>) iStream.readObject();
    }

    private void getBootStrap(ObjectOutputStream oStream, ObjectInputStream iStream)
            throws IOException, ClassNotFoundException {
        String message_to_owner = "Config 3 " + peer_port + " " + _download_port;
        TransmitMessageToOwner(message_to_owner, oStream);
        TransmitMessageToOwner("REGISTER", oStream);
        peer_id = iStream.readInt();
        peer_self_port = iStream.readInt();
        peer_name = "PEER" + peer_id;
        System.out.println(peer_id);
        // Create a peerDir to save file chunk from server
        File peerDir = new File(peer_name + "Dir");
        if (!peerDir.exists()) {
            peerDir.mkdir();
        }
        getChuckList(oStream, iStream);
        getInitialChunksFromServer(oStream, iStream);
    }

    public static void TransmitMessageToOwner(String msg,
                                              ObjectOutputStream stream)
            throws IOException {
        stream.writeObject(msg);
        stream.flush();
        stream.reset();
    }

    public static void TransmitMessageToOwner(int value,
                                              ObjectOutputStream stream)
            throws IOException {
        stream.writeInt(value);
        stream.flush();
    }

    public static void transmitData(ObjectOutputStream stream,
                                    byte[] message)
            throws IOException {
        stream.writeObject(message);
        stream.flush();
        stream.reset();
    }

    private void performAfterCheckChunk() throws IOException {
        File file = new File(merge_file_name);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fs = new FileOutputStream(file);
        for (int i = 0; i < block_indx.size(); i++) {
            saveChunkFile(i, list_block_file.get(block_indx.get(i)));
            fs.write(list_block_file.get(block_indx.get(i)));
        }
        fs.flush();
        fs.close();
    }

    private void saveChunkFile(int file_i, byte[] chunk) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(peer_name + "Dir/" + file_i,
                    false);
            fileOutputStream.write(chunk);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkChunk() {
        for (int key : block_indx) {
            if (!list_block_file.containsKey(key)) {
                return false;
            }
        }
        try {
            performAfterCheckChunk();
            System.out.println("Congo!! [" + peer_name + "] has completed downloading the file!!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    private void initiatePeer(ClientSocket peer, Socket _socket) {
        peer.intialiseSocket(_socket);
        peer.initiatePeerId(Peer.peer_id);
        peer.InitiateFileBlock(list_block_file);
        peer.start();
    }

    private void createBriefForEntireProcess(int ch) {
        if (true) {
            try {
                FileOutputStream fileOutputStream =
                        new FileOutputStream(peer_name + "Dir/summary.txt", false);
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < Peer.block_indx.size(); i++) {
                    int q = Peer.block_indx.get(i);
                    if (Peer.list_block_file.containsKey(q)) {
                        stringBuilder.append(q);
                        stringBuilder.append(" ");
                    }
                }
                fileOutputStream.write(stringBuilder.toString().getBytes());
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setConnectionToPeer() throws IOException {
        peer_skt = new ServerSocket(this.peer_self_port);
        while (true) {
            ClientSocket peer = new ClientSocket();
            try {
                System.out.println("Peer is listening at Port " + peer_skt.getLocalPort());
                initiatePeer(peer, peer_skt.accept());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void getUploadDownloadNeighbor(ObjectOutputStream oStream, ObjectInputStream iStream)
            throws InterruptedException, IOException, ClassNotFoundException {
        do {

            TransmitMessageToOwner("PEER", oStream);
            TransmitMessageToOwner(peer_id, oStream);
            this.peer_list = (HashMap<Integer, Integer>) iStream.readObject();

            System.out.println("[" + this.peer_name + "] Ask bootstrap server for neighbors:");
            this.peer_DL = (int) iStream.readObject();
            this.peer_UL = (int) iStream.readObject();
            this.port_DL = peer_list.containsKey(peer_DL) ? this.peer_list.get(this.peer_DL) : 0;
            this.port_UL = peer_list.containsKey(peer_UL) ? this.peer_list.get(this.peer_UL) : 0;
            Thread.sleep(1000);
        } while (this.port_DL <= 0 || this.port_UL <= 0);

    }

    public String getMergeFileName(ObjectInputStream iStream)
            throws IOException, ClassNotFoundException {
        String base_name = new File((String) iStream.readObject()).
                getName();
        return base_name.substring(0, base_name.lastIndexOf('.')) + "-peer-" + peer_id + "."
                + base_name.substring(base_name.lastIndexOf('.') + 1);
    }

    private void pushChunkToPeer(ObjectOutputStream objectOutputStreamUp)
            throws IOException, InterruptedException {
        for (Integer block_index : Peer.block_indx) {
            int bid = block_index;
            if (!Peer.list_block_file.containsKey(bid)) {
                continue;
            }
            System.out.print(bid + " ");
            TransmitMessageToOwner("DATA", objectOutputStreamUp);
            TransmitMessageToOwner(bid, objectOutputStreamUp);
            transmitData(objectOutputStreamUp, Peer.list_block_file.get(bid));
        }
        System.out.println();
        System.out.println("[" + Peer.peer_name + "] completed uploading to Peer!!");
        Thread.sleep(1000);
    }

    private void printBlockListFromPeer(ObjectInputStream iDownStream)
            throws IOException, ClassNotFoundException {
        ArrayList<Integer> c = (ArrayList<Integer>) iDownStream.readObject();
        for (int i = 0; i < c.size(); i++) {
            int q = c.get(i);
            if (Peer.list_block_file.containsKey(q)) {
                System.out.print(q + "=>" + q + "\t");
            } else {
                System.out.print(q + "=> NEW\t");
            }
        }
    }

    private void processBlockIndex(ObjectInputStream objectInputStream,
                                   ObjectOutputStream objectOutputStreamDown)
            throws IOException, ClassNotFoundException {
        for (int x = 0; x < Peer.block_indx.size(); x++) {
            int index = Peer.block_indx.get(x);
            if (Peer.list_block_file.containsKey(index)) {
                continue;
            }
            System.out.println("[" + Peer.peer_name + "] Request PEER" +
                    peer_DL + " block #" + index);
            TransmitMessageToOwner("ASK", objectOutputStreamDown);
            TransmitMessageToOwner(index, objectOutputStreamDown);
            if (objectInputStream.readInt() == 1) {
                getChunkFromThatPeer(objectInputStream, objectOutputStreamDown, index, x);

            } else {
                System.out.println("[" + Peer.peer_name + "] PEER"
                        + peer_DL + " doesn't have block #" + index);
            }
        }
    }

    private void getChunkFromThatPeer(ObjectInputStream objectInputStream,
                                      ObjectOutputStream objectOutputStreamDown, int index, int i)
            throws IOException, ClassNotFoundException {
        TransmitMessageToOwner("REQUEST", objectOutputStreamDown);
        TransmitMessageToOwner(index, objectOutputStreamDown);
        int x = objectInputStream.readInt();
        byte[] chunk = (byte[]) objectInputStream.readObject();
        Peer.list_block_file.put(x, chunk);
        System.out.println("Received Chunk #" + block_indx.get(i) +
                " from Peer " + peer_DL);
    }


    public void executeRun()
            throws IOException,
            InterruptedException,
            ClassNotFoundException {
        Socket upSock = new Socket("localhost", port_UL);
        ObjectOutputStream objectOutputStreamUp = new ObjectOutputStream(upSock.getOutputStream());

        Socket downSock = new Socket("localhost", port_DL);
        ObjectOutputStream objectOutputStreamDown = new ObjectOutputStream(downSock.getOutputStream());
        ObjectInputStream objectInputStream = new ObjectInputStream (downSock.getInputStream());

        System.out.println("==================");
        Thread.sleep(10000);
        System.out.println("Initiating upload connection...");
        System.out.println("Initiating download connection...");
        System.out.println("Connection Established!!");
        while (!checkChunk()) {
            System.out.println("Received Block list from neighbor");
            TransmitMessageToOwner("LIST", objectOutputStreamDown);
            printBlockListFromPeer(objectInputStream);
            System.out.println();
            processBlockIndex(objectInputStream, objectOutputStreamDown);
            System.out.println("[" + Peer.peer_name + "] Finished pulling...");
            System.out.println("Start pushing chunk list...");
            pushChunkToPeer(objectOutputStreamUp);
        }
    }

    private void initalize(){

    }

//    private void initialize(){
//        try {
//            //Step 1: Bootstrap. Register and get peer id
//            Socket s = new Socket("localhost", server_port);
//            ObjectOutputStream oStream = new ObjectOutputStream(s.getOutputStream());
//            TransmitMessageToOwner("REGISTER", oStream);
//            ObjectInputStream iStream = new ObjectInputStream(s.getInputStream());
//            peer_id = iStream.readInt();
//            port = iStream.readInt();
//            peerName = "PEER" + peerId;
//            System.out.println(peerId);
//            // Create a peerDir to save file chunk from server
//            File peerDir = new File(peerName + "Dir");
//            if(!peerDir.exists()) {
//                peerDir.mkdir();
//            }
//            //Step 2: Get chunk list
//            TransmitMessageToOwner(oStream, "LIST");
//            chunkIndex = (ArrayList<Integer>) iStream.readObject();
//            //Step 3: Get initial chunks from server;
//            int startIndex = (int)(1.0 * chunkIndex.size() / TOTAL_PEERS * (peerId % TOTAL_PEERS));
//            int endIndex = (int)(1.0 * chunkIndex.size() / TOTAL_PEERS * ((peerId  % TOTAL_PEERS) + 1));
//            for(int i = startIndex; i < endIndex; i++) {
//                TransmitMessageToOwner(oStream, "REQUEST");
//                TransmitMessageToOwner(oStream, chunkIndex.get(i));
//                int x = iStream.readInt();
//                byte[] chunk = (byte[]) iStream.readObject();
//                chunkList.put(x, chunk);
//                System.out.println("Received Chunk #" + chunkIndex.get(i) + " from server");
//                saveChunkFile(x, chunk);
//            }
//            CreateSummaryFile(0);
//            Random rand = new Random();
//            // Step 3-1: Get filename
//            writeMessage(oStream, "NAME");
//            String filePath = (String) iStream.readObject();
//            String basename = new File(filePath).getName();
//            String extension = basename.substring(basename.lastIndexOf('.') + 1);
//            String fileRoot = basename.substring(0, basename.lastIndexOf('.'));
//            mergeFileName = fileRoot + "-peer-" + peerId + "." + extension;
//            System.out.println("Output file is " + mergeFileName);
//            //Step 4: Get a upload neighbor and download neighbor
//            do {
//                TransmitMessageToOwner(oStream, "PEER");
//                TransmitMessageToOwner(oStream, peerId);
//                peerList = (HashMap<Integer, Integer>) iStream.readObject();
//
//                System.out.println("[" + peerName + "] Ask bootstrap server for neighbors:");
//                downloadPeer = (int) iStream.readObject();
//                uploadPeer = (int) iStream.readObject();
//                downloadPort = peerList.containsKey(downloadPeer) ? peerList.get(downloadPeer) : 0;
//                uploadPort = peerList.containsKey(uploadPeer) ? peerList.get(uploadPeer) : 0;
//                Thread.sleep(1000);
//            } while (this.downloadPort <= 0 || this.uploadPort <= 0);
//            System.out.println("[" + peerName + "] Uploading to " + uploadPeer + ":" + uploadPort);
//            System.out.println("[" + peerName + "] Downloading from " + downloadPeer + ":" + downloadPort);
//
//            (new Thread() {
//                @Override
//                public void run() {
//                    try {
//                        System.out.println("==================");
//                        Thread.sleep(10000);
//                        System.out.println("Making upload connection...");
//                        Socket upSock = new Socket("localhost", uploadPort);
//                        ObjectOutputStream oUpStream = new ObjectOutputStream(upSock.getOutputStream());
//                        System.out.println("Making download connection...");
//                        Socket downSock = new Socket("localhost", downloadPort);
//                        ObjectOutputStream oDownStream = new ObjectOutputStream(downSock.getOutputStream());
//                        ObjectInputStream iDownStream = new ObjectInputStream(downSock.getInputStream());
//                        System.out.println("Connection Made!");
//                        while (!checkChunk()) {
//                            System.out.println("Got chunk list from neighbor");
//                            TransmitMessageToOwner(oDownStream, "LIST");
//                            ArrayList<Integer> c = (ArrayList<Integer>) iDownStream.readObject();
//                            for (int i =0 ; i < c.size(); i++ ) {
//                                int q = c.get(i);
//                                if(Client.chunkList.containsKey(q)) {
//                                    System.out.print(q + "=>" + q + "\t");
//                                } else {
//                                    System.out.print(q + "=> NEW\t");
//                                }
//                            }
//                            System.out.println();
//                            for (int i = 0; i < Client.chunkIndex.size(); i++) {
//                                int q = Client.chunkIndex.get(i);
//                                if (Client.chunkList.containsKey(q)) {
//                                    continue;
//                                }
//
//                                System.out.println("[" + Client.peerName + "] Ask PEER" + downloadPeer + " Chunk #" + q);
//                                TransmitMessageToOwner(oDownStream, "ASK");
//                                TransmitMessageToOwner(oDownStream, q);
//                                if (iDownStream.readInt() == 1) { //Means peer has that chunk
//                                    TransmitMessageToOwner(oDownStream, "REQUEST");
//                                    TransmitMessageToOwner(oDownStream, q);
//                                    int x = iDownStream.readInt();
//                                    byte[] chunk = (byte[]) iDownStream.readObject();
//                                    Client.chunkList.put(x, chunk);
//                                    System.out.println("Received Chunk #" + chunkIndex.get(i) + " from Peer " + downloadPeer);
//                                } else {
//                                    System.out.println("[" + Client.peerName + "] PEER" + downloadPeer + " doesn't have Chunk #" + q);
//                                }
//                            }
//                            System.out.println("[" + Client.peerName + "] Finished pulling...");
//                            System.out.println("Start pushing chunk list...");
//                            for (Integer aChunkIndex : Client.chunkIndex) {
//                                int q = aChunkIndex;
//                                if (!Client.chunkList.containsKey(q)) {
//                                    continue;
//                                }
//                                System.out.print(q + " ");
//                                TransmitMessageToOwner(oUpStream, "DATA");
//                                TransmitMessageToOwner(oUpStream, q);
//                                TransmitMessageToOwner(oUpStream, Client.chunkList.get(q));
//                            }
//                            System.out.println();
//                            System.out.println("[" + Client.peerName + "] Finished pushing, sleep 1sec.");
//                            Thread.sleep(1000);
//                        }
//                    } catch (IOException | ClassNotFoundException | InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                }
//            }).start();
//
//            while(port < 0) {
//                Thread.sleep(500);
//            }
//
//            clientServerSocket = new ServerSocket(this.port);
//
//            while (true) {
//
//                ClientSocket localDaemon = new ClientSocket();
//                Socket socket = null;
//                try {
//                    System.out.println("Peer is listening at Port " + clientServerSocket.getLocalPort());
//
//                    socket = clientServerSocket.accept();
//                    localDaemon.setSocket(socket);
//                    localDaemon.setPeerId(Client.peerId);
//                    localDaemon.setFileChunk(chunkList);
//                    localDaemon.start();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        } catch (IOException | ClassNotFoundException | InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

//    private void initalize() {
//        try {
//            Socket socket = new Socket("localhost", server_port);
//            ObjectOutputStream oStream = new ObjectOutputStream(socket.getOutputStream());
//            ObjectInputStream iStream = new ObjectInputStream(socket.getInputStream());
//
//            getBootStrap(oStream, iStream);
//            createBriefForEntireProcess(0);
//
//            Random rand = new Random();
//
//            TransmitMessageToOwner("NAME", oStream);
//            merge_file_name = getMergeFileName(iStream);
//
//            System.out.println("Output file is " + merge_file_name);
//
//            getUploadDownloadNeighbor(oStream, iStream);
//
//            System.out.println("[" + peer_name + "] 's Upload Neighbor " + peer_UL + ":" + port_UL);
//            System.out.println("[" + peer_name + "] Download Neighbor " + peer_DL + ":" + port_DL);
//
//            (new Thread() {
//                @Override
//                public void run() {
//                    try {
//                        System.out.println("==================");
//                        Thread.sleep(10000);
//                        System.out.println("Making upload connection...");
//                        Socket upSock = new Socket("localhost", port_UL);
//                        ObjectOutputStream oUpStream = new ObjectOutputStream(upSock.getOutputStream());
//                        System.out.println("Making download connection...");
//                        Socket downSock = new Socket("localhost", port_DL);
//                        ObjectOutputStream oDownStream = new ObjectOutputStream(downSock.getOutputStream());
//                        ObjectInputStream iDownStream = new ObjectInputStream(downSock.getInputStream());
//                        System.out.println("Connection Made!");
//                        while (!checkChunk()) {
//                            System.out.println("Got chunk list from neighbor");
//                            TransmitMessageToOwner("LIST", oDownStream);
//                            ArrayList<Integer> c = (ArrayList<Integer>) iDownStream.readObject();
//                            for (int i =0 ; i < c.size(); i++ ) {
//                                int q = c.get(i);
//                                if(Peer.list_block_file.containsKey(q)) {
//                                    System.out.print(q + "=>" + q + "\t");
//                                } else {
//                                    System.out.print(q + "=> NEW\t");
//                                }
//                            }
//                            System.out.println();
//                            for (int i = 0; i < Peer.block_indx.size(); i++) {
//                                int q = Peer.block_indx.get(i);
//                                if (Peer.list_block_file.containsKey(q)) {
//                                    continue;
//                                }
//
//                                System.out.println("[" + Peer.peer_name + "] Ask PEER" + peer_DL + " Chunk #" + q);
//                                TransmitMessageToOwner("ASK", oDownStream);
//                                TransmitMessageToOwner(q, oDownStream);
//                                if (iDownStream.readInt() == 1) { //Means peer has that chunk
//                                    TransmitMessageToOwner( "REQUEST", oDownStream);
//                                    TransmitMessageToOwner(q, oDownStream);
//                                    int x = iDownStream.readInt();
//                                    byte[] chunk = (byte[]) iDownStream.readObject();
//                                    Peer.list_block_file.put(x, chunk);
//                                    System.out.println("Received Chunk #" +
//                                            block_indx.get(i) + " from Peer " + peer_DL);
//                                } else {
//                                    System.out.println("[" + Peer.peer_name + "] PEER" +
//                                            peer_DL + " doesn't have Chunk #" + q);
//                                }
//                            }
//                            System.out.println("[" + Peer.peer_name + "] Finished pulling...");
//                            System.out.println("Start pushing chunk list...");
//                            for (Integer aChunkIndex : Peer.block_indx) {
//                                int q = aChunkIndex;
//                                if (!Peer.list_block_file.containsKey(q)) {
//                                    continue;
//                                }
//                                System.out.print(q + " ");
//                                TransmitMessageToOwner("DATA", oUpStream);
//                                TransmitMessageToOwner(q, oUpStream);
//                                transmitData(oUpStream, Peer.list_block_file.get(q));
//                            }
//                            System.out.println();
//                            System.out.println("[" + Peer.peer_name + "] Finished pushing, sleep 1sec.");
//                            Thread.sleep(1000);
//                        }
//                    } catch (IOException | ClassNotFoundException | InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                }
////                public void run() {
////                    try {
////                        executeRun();
////                    } catch (IOException | InterruptedException | ClassNotFoundException e) {
////                        e.printStackTrace();
////                    }
////
////                }
//            }).start();
//
//            while (peer_self_port < 0) {
//                Thread.sleep(500);
//            }
//            peer_skt = new ServerSocket(this.peer_port);
//
//            while (true) {
//
//                ClientSocket localDaemon = new ClientSocket();
//                Socket skt = null;
//                try {
//                    System.out.println("Peer is listening at Port " + peer_skt.getLocalPort());
//
//                    skt = peer_skt.accept();
//                    localDaemon.intialiseSocket(skt);
//                    localDaemon.initiatePeerId(Peer.peer_id);
//                    localDaemon.InitiateFileBlock(list_block_file);
//                    localDaemon.start();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        } catch (IOException | ClassNotFoundException | InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) {
        int _server_port = 0;
        int _peer_port = 0;
        int _download_port = 0;

        if (args.length > 0) {
            _server_port = Integer.parseInt(args[0]);
            _peer_port = Integer.parseInt(args[1]);
            _download_port = Integer.parseInt(args[2]);
        }
        new Peer(_server_port, _peer_port, _download_port).initalize();
    }
}
