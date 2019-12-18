//package com.UFL;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.lang.*;

/**
 * Class containing all the string constants.
 */
class Constants {
    public static final String LIST = "LIST";
    public static final String REQUEST = "REQUEST";
    public static final String ASK = "ASK";
    public static final String DATA = "DATA";
    public static final String CLOSE = "CLOSE";
    public static final String PEER = "PEER";
    public static final String DIR = "Dir/";
    public static final String BRACE_OPEN = "[";
    public static final String BRACE_CLOSE = "] get connected from ";
    public static final String SESSION_ENDED = "]: Session ended.";
    public static final String RECIEVED_CHUNK = "Received Chunk #";
    public static final String IS_LISTENING = "] Peer is listening command:";
    public static final String BRACKET_CLOSE = ")";
    public static final String RECEIVED_MESSAGE = "] Received message (";
    public static final String RECIEVED_BLOCK = "Received Block #";
    public static final String FROM_OWNER = " from owner";
    public static final String CONFIG = "Config 6 ";
    public static final String SPACE = " ";
    public static final String DIR_1 = "Dir";
    public static final String CONGO = "CONGO !! [";
    public static final String DOWNLOAD_COMPLETION = "] has completed downloading the file!!";
    public static final String SUMMARY_FILE = "Dir/summary.txt";
    public static final String PEER_LISTENING = "Peer is listening at Port ";
    public static final String BOOTSTRAP_FINAL = "] Asking for upload/download neighbor:";
    public static final String ESTABLISHING_UPLOAD = "Establishing upload...";
    public static final String ESTABLISHING_DOWNLOAD = "Establishing download...";
    public static final String ESTABLISHED_CONNECTION = "Connection established";
    public static final String LOCALHOST = "localhost";
    public static final String RECEIVED_BLOCKS = "Received blocks(download neighbor) from peer(port): ";
    public static final String COMPLETED_PULLING = "] completed pulling from neighbor...";
    public static final String INITIATED_PUSHING = "Initiated pushing block list...";
    public static final String NAME = "NAME";
    public static final String COMPLETED_PUSHING = "] completed pushing!! sleep 1sec.";
    public static final String REQUEST_PEER = "] REQUEST PEER";
    public static final String CHUNK = " Chunk #";
    public static final String RECEIVED_CHUNK = "Received Chunk #";
    public static final String FROM_PEER = " from Peer ";
    public static final String NOT_HAVE_CHUNK = " doesn't have Chunk #";
    public static final String CLOSE_PEER = "] PEER";
    public static final String OUT_PUT_FILE = "Output file is ";
    public static final String UPLOAD_NEIGHBOUR = "] 's Upload Neighbor ";
    public static final String DOWNLOAD_NEIGHBOUR = "] 's download Neighbor ";
    public static final String COLON = ":";
    public static final String REGISTER = "REGISTER";
    public static final String DOWNLOAD_NEIGHBOR_ID = "Current Download neighbor(ID) Peer: ";
    public static final String UPLOAD_NEIGHBOR_ID = "Current Upload neighbor(ID) Peer: ";
}

/**
 * This class creates a client socket that spawns new threads for each neighbor.
 */
class ClientSocket extends Thread {

    protected int peer_id;
    protected Socket socket;
    protected ObjectOutputStream objectOutputStream;
    protected ObjectInputStream objectInputStream;

    public String peerName = this.getName();

    protected HashMap<Integer, byte[]> current_block;

    /**
     * Creates a new client socket for each neighbor
     *
     * @param _peer_id         peer id
     * @param _list_block_file list of file chunks
     */
    public ClientSocket(int _peer_id, HashMap<Integer, byte[]> _list_block_file) {
        this.peer_id = _peer_id;
        this.peerName = Integer.toString(this.peer_id);
        this.current_block = _list_block_file;
    }

    /**
     * Saves file chunk.
     *
     * @param x     peer id
     * @param chunk file chunk
     */
    private void saveChunkFile(int x, byte[] chunk) {
        try {
            FileOutputStream fileOutputStream =
                    new FileOutputStream(Constants.PEER
                            + this.peer_id + Constants.DIR + x, false);
            fileOutputStream.write(chunk);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialise peer socket.
     *
     * @param socket peer socket
     */
    public void intialiseSocket(Socket socket) {
        this.socket = socket;
        System.out.println(Constants.BRACE_OPEN + peerName +
                Constants.BRACE_CLOSE + socket.getPort());
        try {
            objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
            objectInputStream = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Communicates with neighbor peer for transferring messages.
     *
     * @param message message to be transferred
     * @throws IOException if an I/O error occurs while reading stream header
     */
    public void transmitToPeer(Object message) throws IOException {
        objectOutputStream.writeObject(message);
        objectOutputStream.flush();
        objectOutputStream.reset();
    }

    /**
     * Communicates with neighbor peer for transferring messages.
     *
     * @param message message to be transferred
     * @throws IOException if an I/O error occurs while reading stream header
     */
    public void transmitToPeer(int message) throws IOException {
        objectOutputStream.writeInt(message);
        objectOutputStream.flush();
        objectOutputStream.reset();
    }

    /**
     * If this thread was constructed using a separate
     * {@code Runnable} run object, then that
     * {@code Runnable} object's {@code run} method is called;
     * otherwise, this method does nothing and returns.
     * Subclasses of {@code Thread} should override this method.
     */
    @Override
    public void run() {
        while (true) {
            try {
                String msg = printCommands();
                int id = -1;
                switch (msg) {
                    case Constants.LIST:
                        performListOperation();
                        break;
                    case Constants.REQUEST:
                        requestChunk(id);
                        break;
                    case Constants.ASK:
                        askChunk(id);
                        break;
                    case Constants.DATA:
                        saveRecievedData(id);
                        break;
                    case Constants.CLOSE:
                        objectOutputStream.close();
                        objectInputStream.close();
                        return;
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                System.out.println(Constants.BRACE_OPEN + this.getName() +
                        Constants.SESSION_ENDED);
                return;
            }
        }
    }

    /**
     * Saves the received file chunks.
     *
     * @param id chunk id
     * @throws IOException            if an I/O error occurs while reading stream header
     * @throws ClassNotFoundException Class of a serialized object cannot be found
     */
    private void saveRecievedData(int id) throws IOException, ClassNotFoundException {
        id = this.objectInputStream.readInt();
        byte[] chunk = (byte[]) this.objectInputStream.readObject();
        if (!this.current_block.containsKey(id)) {
            current_block.put(id, chunk);
            System.out.println(Constants.RECIEVED_CHUNK + id);
            saveChunkFile(id, chunk);
        }
    }

    /**
     * Asks chunk from neighbors.
     *
     * @param id chunk id
     * @throws IOException if an I/O error occurs while reading stream header
     */
    private void askChunk(int id) throws IOException {
        if (this.current_block.containsKey(this.objectInputStream.readInt())) {
            transmitToPeer(1);
        } else {
            transmitToPeer(0);
        }
    }

    /**
     * Requests chunk from neighbors.
     *
     * @param id chunk id
     * @throws IOException if an I/O error occurs while reading stream header
     */
    private void requestChunk(int id) throws IOException {
        id = this.objectInputStream.readInt();
        // Send that chunk
        transmitToPeer(id);
        transmitToPeer(this.current_block.get(id));
    }

    /**
     * Lists all the blocks.
     *
     * @throws IOException if an I/O error occurs while reading stream header
     */
    private void performListOperation() throws IOException {
        ArrayList<Integer> q = new ArrayList<Integer>(this.current_block.size());
        for (Integer key : this.current_block.keySet()) {
            q.add(key);
        }
        transmitToPeer(q);
    }

    /**
     * Prints the commands in the console.
     *
     * @return print command
     * @throws IOException            if an I/O error occurs while reading stream header
     * @throws ClassNotFoundException Class of a serialized object cannot be found
     */
    private String printCommands() throws IOException, ClassNotFoundException {
        System.out.println(Constants.BRACE_OPEN + this.peerName + Constants.IS_LISTENING);
        Object msgObj = this.objectInputStream.readObject();
        String msg = (String) msgObj;
        System.out.println(Constants.BRACE_OPEN + this.peerName + Constants.RECEIVED_MESSAGE
                + msg + Constants.BRACKET_CLOSE);
        return msg;
    }
}

/**
 * Creates a Peer that communicates with the file owner and other peers.
 */
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

    public static String merge_file_name = "";

    public static HashMap<Integer, byte[]> list_block_file = new HashMap<Integer, byte[]>();
    public static ArrayList<Integer> block_indx = new ArrayList<Integer>();
    public static HashMap<Integer, Integer> peer_list = new HashMap<Integer, Integer>();

    /**
     * Creates a new peer.
     *
     * @param _serverPort  port of the server
     * @param _peer_port   port of the peer
     * @param _client_port port of the client
     */
    public Peer(int _serverPort, int _peer_port, int _client_port) {
        this.server_port = _serverPort;
        this.peer_port = _peer_port;
        this._download_port = _client_port;
    }

    /**
     * Gets the initial chunks from the server.
     *
     * @param oStream ObjectOutputStream that writes to the specified OutputStream
     * @param iStream ObjectInputStream deserializes primitive data and objects previously
     *                written using an ObjectOutputStream
     * @throws IOException            if an I/O error occurs while reading stream header
     * @throws ClassNotFoundException Class of a serialized object cannot be found
     */
    private void getInitialChunksFromServer(ObjectOutputStream oStream, ObjectInputStream iStream)
            throws IOException, ClassNotFoundException {
        int last = (int) (1.0 * block_indx.size()
                / MAX_PEER * ((peer_id % MAX_PEER) + 1));
        int begin = (int) (1.0 * block_indx.size()
                / MAX_PEER * (peer_id % MAX_PEER));

        for (int i = begin; i < last; i++) {
            TransmitMessageToOwner(Constants.REQUEST, oStream);
            TransmitMessageToOwner(block_indx.get(i), oStream);
            int f = iStream.readInt();
            byte[] chunk = (byte[]) iStream.readObject();
            list_block_file.put(f, chunk);
            System.out.println(Constants.RECIEVED_BLOCK + block_indx.get(i) +
                    Constants.FROM_OWNER);
            saveChunkFile(f, chunk);
        }
    }

    /**
     * Gets the lists of chunks.
     *
     * @param oStream ObjectOutputStream that writes to the specified OutputStream
     * @param iStream ObjectInputStream deserializes primitive data and objects previously
     *                written using an ObjectOutputStream
     * @throws IOException            if an I/O error occurs while reading stream header
     * @throws ClassNotFoundException Class of a serialized object cannot be found
     */
    private void getChuckList(ObjectOutputStream oStream, ObjectInputStream iStream)
            throws IOException, ClassNotFoundException {
        TransmitMessageToOwner(Constants.LIST, oStream);
        block_indx = (ArrayList<Integer>) iStream.readObject();
    }

    /**
     * Bootstraps with the server.
     *
     * @param oStream ObjectOutputStream that writes to the specified OutputStream
     * @param iStream ObjectInputStream deserializes primitive data and objects previously
     *                written using an ObjectOutputStream
     * @throws IOException            if an I/O error occurs while reading stream header
     * @throws ClassNotFoundException Class of a serialized object cannot be found
     */
    private void getBootStrap(ObjectOutputStream oStream, ObjectInputStream iStream)
            throws IOException, ClassNotFoundException {
        String message_to_owner = Constants.CONFIG + peer_port + Constants.SPACE + _download_port;
        TransmitMessageToOwner(message_to_owner, oStream);
        TransmitMessageToOwner(Constants.REGISTER, oStream);
        peer_id = iStream.readInt();
        peer_self_port = iStream.readInt();
        peer_name = Constants.PEER + peer_id;
        System.out.println(peer_id);
        File peerDir = new File(peer_name + Constants.DIR_1);
        if (!peerDir.exists()) {
            peerDir.mkdir();
        }
        getChuckList(oStream, iStream);
        getInitialChunksFromServer(oStream, iStream);
    }

    /**
     * Gets the upload and download neighbor from the file owner
     *
     * @param oStream ObjectOutputStream that writes to the specified OutputStream
     * @param iStream ObjectInputStream deserializes primitive data and objects previously
     *                written using an ObjectOutputStream
     * @throws InterruptedException   * Constructs an InterruptedException with the specified detail message.
     * @throws IOException            if an I/O error occurs while reading stream header
     * @throws ClassNotFoundException Class of a serialized object cannot be found
     */
    public void getUploadDownloadNeighbor(ObjectOutputStream oStream, ObjectInputStream iStream)
            throws InterruptedException, IOException, ClassNotFoundException {
        do {

            TransmitMessageToOwner(Constants.PEER, oStream);
            TransmitMessageToOwner(peer_id, oStream);
            TransmitMessageToOwner(peer_port, oStream);
            TransmitMessageToOwner(_download_port, oStream);
            this.peer_list = (HashMap<Integer, Integer>) iStream.readObject();

            System.out.println(Constants.BRACE_OPEN + this.peer_name + Constants.BOOTSTRAP_FINAL);
            this.peer_DL = (int) iStream.readObject();
            this.peer_UL = (int) iStream.readObject();
            System.out.println(this.peer_DL);
            System.out.println(this.peer_UL);
            this.port_DL = peer_list.containsKey(peer_DL) ? this.peer_list.get(this.peer_DL) : 0;
            this.port_UL = peer_list.containsKey(peer_UL) ? this.peer_list.get(this.peer_UL) : 0;
            printNeighborStatus();
//            System.out.println(Constants.DOWNLOAD_NEIGHBOR_ID + this.peer_DL);
//            System.out.println(Constants.UPLOAD_NEIGHBOR_ID + this.peer_UL);
            Thread.sleep(1000);
        } while (this.port_DL <= 0 || this.port_UL <= 0);

    }

    /**
     * Prints the neighbors status.
     */
    private void printNeighborStatus() {
        if (port_UL == 0) {
            System.out.println("Upload Neighbor is still not up");
        } else {
            System.out.println(Constants.UPLOAD_NEIGHBOR_ID +
                    this.peer_UL + " is up and running!!");
        }

        if (peer_DL == 0) {
            System.out.println("Download Neighbor is still not up");
        } else {
            System.out.println(Constants.DOWNLOAD_NEIGHBOR_ID +
                    this.peer_DL + " is up and running!!");
        }
    }

    /**
     * Communicates with file owner for transferring messages.
     *
     * @param msg    transmits the message
     * @param stream ObjectOutputStream that writes to the specified OutputStream
     * @throws IOException if an I/O error occurs while reading stream header
     */
    public static void TransmitMessageToOwner(String msg,
                                              ObjectOutputStream stream)
            throws IOException {
        stream.writeObject(msg);
        stream.flush();
        stream.reset();
    }

    /**
     * Communicates with file owner for transferring messages.
     *
     * @param value  transmits the message
     * @param stream ObjectOutputStream that writes to the specified OutputStream
     * @throws IOException if an I/O error occurs while reading stream header
     */
    public static void TransmitMessageToOwner(int value,
                                              ObjectOutputStream stream)
            throws IOException {
        stream.writeInt(value);
        stream.flush();
    }

    /**
     * Communicates with file owner for transferring messages.
     *
     * @param stream  ObjectOutputStream that writes to the specified OutputStream
     * @param message transmits the message
     * @throws IOException
     */
    public static void transmitData(ObjectOutputStream stream,
                                    byte[] message)
            throws IOException {
        stream.writeObject(message);
        stream.flush();
        stream.reset();
    }

    /**
     * Performs check whether the chunk exists.
     *
     * @throws IOException if an I/O error occurs while reading stream header
     */
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

    /**
     * Saves the file chunk in the disk
     *
     * @param file_i file index
     * @param chunk  file chunk
     */
    private void saveChunkFile(int file_i, byte[] chunk) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(peer_name + Constants.DIR + file_i,
                    false);
            fileOutputStream.write(chunk);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks the particular chunk
     *
     * @return boolean value whether that chunk exists or not
     */
    public boolean checkChunk() {
        for (int key : block_indx) {
            if (!list_block_file.containsKey(key)) {
                return false;
            }
        }
        try {
            performAfterCheckChunk();
            System.out.println(Constants.CONGO + peer_name + Constants.DOWNLOAD_COMPLETION);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Creates a new peer thread.
     *
     * @param peer    peer soket
     * @param _socket socket connection
     */
    private void initiatePeer(ClientSocket peer, Socket _socket) {
        peer.intialiseSocket(_socket);
        peer.start();
    }

    /**
     * Creates a summary file.
     *
     * @param ch characters
     */
    private void createBriefForEntireProcess(int ch) {
        if (true) {
            try {
                FileOutputStream fileOutputStream =
                        new FileOutputStream(peer_name + Constants.SUMMARY_FILE, false);
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < Peer.block_indx.size(); i++) {
                    int q = Peer.block_indx.get(i);
                    if (Peer.list_block_file.containsKey(q)) {
                        stringBuilder.append(q);
                        stringBuilder.append(Constants.SPACE);
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

    /**
     * Sets the connection.
     *
     * @throws IOException if an I/O error occurs while reading stream header
     */
    private void setConnectionToPeer() throws IOException {
        peer_skt = new ServerSocket(this.peer_self_port);
        while (true) {

            ClientSocket peer = new ClientSocket(Peer.peer_id, list_block_file);
            Socket p = null;
            try {
                System.out.println(Constants.PEER_LISTENING + peer_skt.getLocalPort());
                p = peer_skt.accept();
                initiatePeer(peer, p);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets the file name of the merged file.
     *
     * @param iStream deserializes primitive data and objects previously
     *                written using an ObjectOutputStream
     * @return the merged file name
     * @throws IOException            if an I/O error occurs while reading stream header
     * @throws ClassNotFoundException Class of a serialized object cannot be found
     */
    public String getMergeFileName(ObjectInputStream iStream)
            throws IOException, ClassNotFoundException {
        String base_name = new File((String) iStream.readObject()).
                getName();
        return base_name.substring(0, base_name.lastIndexOf('.')) + "-peer-" + peer_id + "."
                + base_name.substring(base_name.lastIndexOf('.') + 1);
    }

    /**
     * Sets ObjectOutputStream and ObjectInputStream
     *
     * @throws IOException            if an I/O error occurs while reading stream header
     * @throws InterruptedException   Constructs an InterruptedException with the specified detail message
     * @throws ClassNotFoundException Class of a serialized object cannot be found
     */
    public void executeRun()
            throws IOException,
            InterruptedException,
            ClassNotFoundException {

        System.out.println("==================");
        Thread.sleep(10000);
        System.out.println(Constants.ESTABLISHING_UPLOAD);
        System.out.println(Constants.ESTABLISHING_DOWNLOAD);

        Socket skt_up = new Socket(Constants.LOCALHOST, port_UL);
        ObjectOutputStream objectOutputStreamUp = new ObjectOutputStream(skt_up.getOutputStream());

        Socket skt_dwn = new Socket(Constants.LOCALHOST, port_DL);
        ObjectOutputStream objectOutputStreamDwn = new ObjectOutputStream(skt_dwn.getOutputStream());
        ObjectInputStream objectInputStreamDwn = new ObjectInputStream(skt_dwn.getInputStream());

        System.out.println(Constants.ESTABLISHED_CONNECTION);
        while (!checkChunk()) {
            processChunk(objectOutputStreamUp, objectOutputStreamDwn, objectInputStreamDwn);
            Thread.sleep(1000);
        }
    }

    /**
     * Processes each chunk
     *
     * @param objectOutputStreamUp  ObjectOutputStream that writes to the specified OutputStream
     * @param objectOutputStreamDwn ObjectOutputStream that writes to the specified OutputStream
     * @param objectInputStreamDwn  deserializes primitive data and objects previously
     *                              written using an ObjectOutputStream
     * @throws IOException            if an I/O error occurs while reading stream header
     * @throws ClassNotFoundException Class of a serialized object cannot be found
     */
    private void processChunk(ObjectOutputStream objectOutputStreamUp,
                              ObjectOutputStream objectOutputStreamDwn,
                              ObjectInputStream objectInputStreamDwn)
            throws IOException, ClassNotFoundException {
        System.out.println(Constants.RECEIVED_BLOCKS + port_DL);
        TransmitMessageToOwner(Constants.LIST, objectOutputStreamDwn);
        ArrayList<Integer> chunks = (ArrayList<Integer>) objectInputStreamDwn.readObject();
        for (int i = 0; i < chunks.size(); i++) {
            int q = chunks.get(i);
            if (Peer.list_block_file.containsKey(q)) System.out.print(q + ":=" + q + "\t");
            else System.out.print(q + ":= Downloaded\t");

        }
        System.out.println();
        sendBlocksToPeers(objectOutputStreamDwn, objectInputStreamDwn);
        pushFileBlock(objectOutputStreamUp);
    }

    /**
     * Uploads file blocks to upload neighbor
     *
     * @param objectOutputStreamUp ObjectOutputStream that writes to the specified OutputStream
     * @throws IOException if an I/O error occurs while reading stream header
     */
    private void pushFileBlock(ObjectOutputStream objectOutputStreamUp)
            throws IOException {
        System.out.println(Constants.BRACE_OPEN + Peer.peer_name +
                Constants.COMPLETED_PULLING);
        System.out.println(Constants.INITIATED_PUSHING
                + " (upload neighbor) to Peer(port): " + port_UL);
        for (Integer block_index : Peer.block_indx) {
            int q = block_index;
            if (!Peer.list_block_file.containsKey(q)) {
                continue;
            }
            System.out.print(q + Constants.SPACE);
            TransmitMessageToOwner(Constants.DATA, objectOutputStreamUp);
            TransmitMessageToOwner(q, objectOutputStreamUp);
            transmitData(objectOutputStreamUp, Peer.list_block_file.get(q));
        }
        System.out.println();
        System.out.println(Constants.BRACE_OPEN + Peer.peer_name +
                Constants.COMPLETED_PUSHING);
    }

    /**
     * Transmits file chunks to neighbors
     *
     * @param objectOutputStreamDwn ObjectOutputStream that writes to the specified OutputStream
     * @param objectInputStreamDwn  deserializes primitive data and objects previously
     *                              written using an ObjectOutputStream
     * @throws IOException            if an I/O error occurs while reading stream header
     * @throws ClassNotFoundException Class of a serialized object cannot be found
     */
    private void sendBlocksToPeers(ObjectOutputStream objectOutputStreamDwn,
                                   ObjectInputStream objectInputStreamDwn)
            throws IOException, ClassNotFoundException {
        for (int i = 0; i < Peer.block_indx.size(); i++) {
            int q = Peer.block_indx.get(i);
            if (Peer.list_block_file.containsKey(q)) {
                continue;
            }

            System.out.println(Constants.BRACE_OPEN + Peer.peer_name + Constants.REQUEST_PEER + peer_DL
                    + Constants.CHUNK + q);
            TransmitMessageToOwner(Constants.ASK, objectOutputStreamDwn);
            TransmitMessageToOwner(q, objectOutputStreamDwn);
            if (objectInputStreamDwn.readInt() == 1) { //Means peer has that chunk
                TransmitMessageToOwner(Constants.REQUEST, objectOutputStreamDwn);
                TransmitMessageToOwner(q, objectOutputStreamDwn);
                int x = objectInputStreamDwn.readInt();
                byte[] chunk = (byte[]) objectInputStreamDwn.readObject();
                Peer.list_block_file.put(x, chunk);
                System.out.println(Constants.RECEIVED_CHUNK +
                        block_indx.get(i) + Constants.FROM_PEER + peer_DL);
            } else {
                System.out.println(Constants.BRACE_OPEN + Peer.peer_name + Constants.CLOSE_PEER +
                        peer_DL + Constants.NOT_HAVE_CHUNK + q);
            }
        }
    }

    /**
     * Initialises each peer
     */
    private void initalize() {
        try {
            Socket socket = new Socket(Constants.LOCALHOST, server_port);
            ObjectOutputStream oStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream iStream = new ObjectInputStream(socket.getInputStream());

            getBootStrap(oStream, iStream);
            createBriefForEntireProcess(0);

            Random rand = new Random();

            TransmitMessageToOwner(Constants.NAME, oStream);
            merge_file_name = getMergeFileName(iStream);

            System.out.println(Constants.OUT_PUT_FILE + merge_file_name);

            getUploadDownloadNeighbor(oStream, iStream);

            System.out.println(Constants.BRACE_OPEN + peer_name + Constants.UPLOAD_NEIGHBOUR + peer_UL
                    + Constants.COLON + port_UL);
            System.out.println(Constants.BRACE_OPEN + peer_name +
                    Constants.DOWNLOAD_NEIGHBOUR + peer_DL +
                    Constants.COLON + port_DL);

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

    /**
     * Main method
     *
     * @param args: <File owner port> <Peer port> <Download neighbor port>
     */
    public static void main(String[] args) {
        int _server_port = 0;
        int _peer_port = 0;
        int _download_port = 0;
        try {
            if (args.length == 3) {
                _server_port = Integer.parseInt(args[0]);
                _peer_port = Integer.parseInt(args[1]);
                _download_port = Integer.parseInt(args[2]);
                if (_peer_port == _download_port) {
                    System.out.println("FileOwner only distributes the chunks, so peer port and download port should be " +
                            "different as peer can only download from other peers, not from file owner..");
                } else {
                    new Peer(_server_port, _peer_port, _download_port).initalize();
                }
            } else {
                System.out.println("Argument length should be 3");
            }
        } catch (Exception ex) {
            System.out.println("Give Proper Input");
        }
    }
}
