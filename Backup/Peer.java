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
        String message_to_owner = "Config 2 " + peer_port + " " + _download_port;
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
        ObjectOutputStream objectOutputStreamUp = new ObjectOutputStream
                (new Socket("localhost",
                        port_UL).getOutputStream());
        ObjectOutputStream objectOutputStreamDown = new ObjectOutputStream
                (new Socket("localhost",
                        port_DL).getOutputStream());
        ObjectInputStream objectInputStream = new ObjectInputStream
                (new Socket("localhost", port_DL)
                        .getInputStream());

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
                    } catch (IOException | InterruptedException | ClassNotFoundException e) {
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
