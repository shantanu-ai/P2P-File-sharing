import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.lang.*;

class ClientSocket extends Thread {

    protected int peer_id;
    protected Socket socket;
    protected ObjectOutputStream objectOutputStream;
    protected ObjectInputStream objectInputStream;

    public String peerName = this.getName();

    protected HashMap<Integer, byte[]> current_block;

    public ClientSocket(int _peer_id, HashMap<Integer, byte[]> _list_block_file) {
        this.peer_id = _peer_id;
        this.current_block = _list_block_file;
    }

    private void saveChunkFile(int x, byte[] chunk) {
        try {
            FileOutputStream fileOutputStream =
                    new FileOutputStream("PEER" + this.peer_id + "Dir/" + x, false);
            fileOutputStream.write(chunk);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void intialiseSocket(Socket socket) {
        this.socket = socket;
        System.out.println("[" + peerName + "] get connected from " + socket.getPort());
        try {
            objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
            objectInputStream = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void transmitToPeer(Object message) throws IOException {
        objectOutputStream.writeObject(message);
        objectOutputStream.flush();
        objectOutputStream.reset();
    }

    public void transmitToPeer(int message) throws IOException {
        objectOutputStream.writeInt(message);
        objectOutputStream.flush();
        objectOutputStream.reset();
    }

    @Override
    public void run() {
        while (true) {
            try {
                String msg = printCommands();
                int id = -1;
                switch (msg) {
                    case "LIST":
                        performListOperation();
                        break;
                    case "REQUEST":
                        requestChunk(id);
                        break;
                    case "ASK":
                        askChunk(id);
                        break;
                    case "DATA":
                        saveRecievedData(id);
                        break;
                    case "CLOSE":
                        objectOutputStream.close();
                        objectInputStream.close();
                        return;
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                System.out.println("[" + this.getName() + "]: Session ended.");
                return;
            }
        }
    }

    private void saveRecievedData(int id) throws IOException, ClassNotFoundException {
        id = this.objectInputStream.readInt();
        byte[] chunk = (byte[]) this.objectInputStream.readObject();
        if (!this.current_block.containsKey(id)) {
            current_block.put(id, chunk);
            System.out.println("Received Chunk #" + id);
            saveChunkFile(id, chunk);
        }
    }

    private void askChunk(int id) throws IOException {
        if (this.current_block.containsKey(this.objectInputStream.readInt())) {
            transmitToPeer(1);
        } else {
            transmitToPeer(0);
        }
    }

    private void requestChunk(int id) throws IOException {
        id = this.objectInputStream.readInt();
        // Send that chunk
        transmitToPeer(id);
        transmitToPeer(this.current_block.get(id));
    }

    private void performListOperation() throws IOException {
        ArrayList<Integer> q = new ArrayList<Integer>(this.current_block.size());
        for (Integer key : this.current_block.keySet()) {
            q.add(key);
        }
        transmitToPeer(q);
    }

    private String printCommands() throws IOException, ClassNotFoundException {
        System.out.println("[" + this.peerName + "] Peer is listening command:");
        Object msgObj = this.objectInputStream.readObject();
        String msg = (String) msgObj;
        System.out.println("[" + this.peerName + "] Received message (" + msg + ")");
        return msg;
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
        String message_to_owner = "Config 5 " + peer_port + " " + _download_port;
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

            ClientSocket peer = new ClientSocket(Peer.peer_id, list_block_file);
            Socket p = null;
            try {
                System.out.println("Peer is listening at Port " + peer_skt.getLocalPort());
                p = peer_skt.accept();
                initiatePeer(peer, p);
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

    public void executeRun()
            throws IOException,
            InterruptedException,
            ClassNotFoundException {

        System.out.println("==================");
        Thread.sleep(10000);
        System.out.println("Establishing upload...");
        System.out.println("Establishing download...");

        Socket skt_up = new Socket("localhost", port_UL);
        ObjectOutputStream objectOutputStreamUp = new ObjectOutputStream(skt_up.getOutputStream());

        Socket skt_dwn = new Socket("localhost", port_DL);
        ObjectOutputStream objectOutputStreamDwn = new ObjectOutputStream(skt_dwn.getOutputStream());
        ObjectInputStream objectInputStreamDwn = new ObjectInputStream(skt_dwn.getInputStream());

        System.out.println("Connection Established!");
        while (!checkChunk()) {
            processChunk(objectOutputStreamUp, objectOutputStreamDwn, objectInputStreamDwn);
            Thread.sleep(1000);
        }
    }

    private void processChunk(ObjectOutputStream objectOutputStreamUp,
                              ObjectOutputStream objectOutputStreamDwn,
                              ObjectInputStream objectInputStreamDwn)
            throws IOException, ClassNotFoundException {
        System.out.println("Received blocks from peer");
        TransmitMessageToOwner("LIST", objectOutputStreamDwn);
        ArrayList<Integer> chunks = (ArrayList<Integer>) objectInputStreamDwn.readObject();
        for (int i = 0; i < chunks.size(); i++) {
            int q = chunks.get(i);
            if (Peer.list_block_file.containsKey(q)) {
                System.out.print(q + "=>" + q + "\t");
            } else {
                System.out.print(q + "=> NEW\t");
            }
        }
        System.out.println();
        sendBlocksToPeers(objectOutputStreamDwn, objectInputStreamDwn);
        pushFileBlock(objectOutputStreamUp);
    }

    private void pushFileBlock(ObjectOutputStream objectOutputStreamUp)
            throws IOException {
        System.out.println("[" + Peer.peer_name + "] completed pulling from neighbor...");
        System.out.println("Initiated pushing block list...");
        for (Integer block_index : Peer.block_indx) {
            int q = block_index;
            if (!Peer.list_block_file.containsKey(q)) {
                continue;
            }
            System.out.print(q + " ");
            TransmitMessageToOwner("DATA", objectOutputStreamUp);
            TransmitMessageToOwner(q, objectOutputStreamUp);
            transmitData(objectOutputStreamUp, Peer.list_block_file.get(q));
        }
        System.out.println();
        System.out.println("[" + Peer.peer_name + "] completed pushing!! sleep 1sec.");
    }

    private void sendBlocksToPeers(ObjectOutputStream objectOutputStreamDwn,
                                   ObjectInputStream objectInputStreamDwn)
            throws IOException, ClassNotFoundException {
        for (int i = 0; i < Peer.block_indx.size(); i++) {
            int q = Peer.block_indx.get(i);
            if (Peer.list_block_file.containsKey(q)) {
                continue;
            }

            System.out.println("[" + Peer.peer_name + "] REQUEST PEER" + peer_DL + " Chunk #" + q);
            TransmitMessageToOwner("ASK", objectOutputStreamDwn);
            TransmitMessageToOwner(q, objectOutputStreamDwn);
            if (objectInputStreamDwn.readInt() == 1) { //Means peer has that chunk
                TransmitMessageToOwner("REQUEST", objectOutputStreamDwn);
                TransmitMessageToOwner(q, objectOutputStreamDwn);
                int x = objectInputStreamDwn.readInt();
                byte[] chunk = (byte[]) objectInputStreamDwn.readObject();
                Peer.list_block_file.put(x, chunk);
                System.out.println("Received Chunk #" +
                        block_indx.get(i) + " from Peer " + peer_DL);
            } else {
                System.out.println("[" + Peer.peer_name + "] PEER" +
                        peer_DL + " doesn't have Chunk #" + q);
            }
        }
    }

    private void initalize() {
        try {
            Socket socket = new Socket("localhost", server_port);
            ObjectOutputStream oStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream iStream = new ObjectInputStream(socket.getInputStream());

            getBootStrap(oStream, iStream);
            createBriefForEntireProcess(0);

            Random rand = new Random();

            TransmitMessageToOwner("NAME", oStream);
            merge_file_name = getMergeFileName(iStream);

            System.out.println("Output file is " + merge_file_name);

            getUploadDownloadNeighbor(oStream, iStream);

            System.out.println("[" + peer_name + "] 's Upload Neighbor " + peer_UL + ":" + port_UL);
            System.out.println("[" + peer_name + "] Download Neighbor " + peer_DL + ":" + port_DL);

            (new Thread() {
                @Override
                public void run() {
                    try {
                        executeRun();
                    } catch (IOException | ClassNotFoundException | InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }).start();

            while (peer_self_port < 0) {
                Thread.sleep(500);
            }
            setConnectionToPeer();

        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }

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
