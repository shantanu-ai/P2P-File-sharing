package com.UFL;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


class OwnerProcess extends Thread {
    protected ObjectOutputStream object_output_stream;
    protected ObjectInputStream input_stream;

    public String peer_name = this.getName();

    protected HashMap<Integer, byte[]> current_block;

    protected String file_name = "";

    public void transferMessageToPeer(Object msg) throws IOException {
        this.object_output_stream.writeObject(msg);
        this.performFlush();
    }

    public void transferMessageToPeer(int msg) throws IOException {
        this.object_output_stream.writeInt(msg);
        this.performFlush();
    }

    public void performFlush() throws IOException {
        this.object_output_stream.flush();
        this.object_output_stream.reset();
    }

    public void initiateBlockPerFile(HashMap<Integer, byte[]> block) {
        this.current_block = block;
    }

    public void initiateNameForCurrentFile(String file_name) {
        this.file_name = file_name;
    }

    public void initiateCurrentNetwork(Socket socket,
                                       ObjectOutputStream _object_output_stream,
                                       ObjectInputStream _input_stream) {
        this.object_output_stream = _object_output_stream;
        this.input_stream = _input_stream;
        System.out.println("[" + peer_name + "] connection established from " + socket.getPort());
    }

    protected int clientId = -1;

    private void replyToIndividualPeerRequest(String message, int x)
            throws IOException,
            ClassNotFoundException {
        switch (message) {
            case "LIST":
                // transmit all the block
                performListOperation();
                break;
            case "NAME":
                // transmit file name
                transferMessageToPeer((Object) this.file_name);
                break;
            case "REQUEST":
                // Read block number as first input
                performRequestOperation(x);
                break;
            case "DATA":
                x = this.input_stream.readInt();
                byte[] chunk = (byte[]) this.input_stream.readObject();
                break;
            case "REGISTER":
                performRegisterOperation();
                break;
            case "PEER":
                performPeerOperation();
                break;
            case "CLOSE":
                this.performCloseOperation();
                return;

        }
    }

    private static int generateIdPerPeer() {
        int i = 0;
        while (i < 100) {
            if (FileOwner.config_peer.containsKey(i) &&
                    (!FileOwner.list_peer.containsKey(i))) {
                FileOwner.list_peer.put(i, (FileOwner.config_peer.get(i)).getPort());
                return i;
            }
            i++;
        }
        return -1;
    }

    private void performListOperation() throws IOException {
        ArrayList<Integer> arrayList = new ArrayList<Integer>(this.current_block.size());
        for (int x = 0; x < this.current_block.size(); x++) {
            if (this.current_block.containsKey(x)) {
                arrayList.add(x);
            }
        }
        transferMessageToPeer(arrayList);
    }

    private void performPeerOperation()
            throws IOException {
        System.out.print("[Owner] Peer list:");
        int peer_id = input_stream.readInt();
        for (int _peer : FileOwner.list_peer.keySet()) {
            System.out.print(_peer + " ");
        }
        System.out.println();
        System.out.println("[Owner] Transmit Upload/Download peers");
        transferMessageToPeer(FileOwner.list_peer);
        transferMessageToPeer((Object) (FileOwner.config_peer.get(peer_id)).getDownLoadId());
        transferMessageToPeer((Object) (FileOwner.config_peer.get(peer_id)).getUploadId());
    }

    private void performCloseOperation()
            throws IOException {
        object_output_stream.close();
        input_stream.close();
        FileOwner.list_peer.remove(clientId);
    }

    private void performRequestOperation(int x)
            throws IOException {
        x = this.input_stream.readInt();
        transferMessageToPeer(x);
        transferMessageToPeer(this.current_block.get(x));
    }

    private void performRegisterOperation()
            throws IOException {
        int peer = this.generateIdPerPeer();
        System.out.println(peer);
        System.out.println(FileOwner.list_peer);
        int port = FileOwner.list_peer.get(peer);
        transferMessageToPeer(peer);
        transferMessageToPeer(port);
    }

    private String initiate_run(){
        System.out.println("[Owner] Owner is up and running:");
        Object input_from_peer;
        while (true) {
            try {
                input_from_peer = this.input_stream.readObject();
                break;
            } catch (Exception Ex) {

            }
        }
        String msg = (String) input_from_peer;
        System.out.println("[Owner] gets message (" + msg + ") from " + clientId);
        return msg;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String message = initiate_run();
                System.out.println("[Owner] gets message (" + message + ") from " + clientId);
                int x = -1;
                replyToIndividualPeerRequest(message, x);
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                System.out.println("Error Occurred");
                FileOwner.list_peer.remove(clientId);
                return;
            }
        }
    }
}

public class FileOwner {
    private String file_name = "";
    private int owner_port = 0;
    private ServerSocket owner_skt;
    public static HashMap<Integer, PeerConfigModel> config_peer = new HashMap<Integer, PeerConfigModel>();
    public static HashMap<Integer, Integer> list_peer = new HashMap<Integer, Integer>();

    // Peer configs
    public final int FILE_MAX_SIZE = 102400;
    public static HashMap<Integer, byte[]> file_block_list = new HashMap<Integer, byte[]>();
    public static String peerName = "File Owner";

    private void createConfig(int id, int peerPort, int downloadPort) {
        if (this.config_peer.isEmpty()) {
            this.config_peer.put(id, new PeerConfigModel(peerPort, 6, 0));
            this.config_peer.put(6, new PeerConfigModel(downloadPort, 0, id));
        } else {
            int d_id = 0;
            for (Map.Entry<Integer, PeerConfigModel> entry : config_peer.entrySet()) {
                if (entry.getValue().getPort() == downloadPort) {
                    d_id = entry.getKey();
                    break;
                }
            }
            if (!this.config_peer.containsKey(id)) {
                this.config_peer.put(id, new PeerConfigModel(peerPort, d_id, 0));
            } else {
                PeerConfigModel pcm = this.config_peer.get(id);
                this.config_peer.put(id, new PeerConfigModel(pcm.getPort(), d_id, pcm.getUploadId()));
            }
            PeerConfigModel pcm = this.config_peer.get(d_id);
            this.config_peer.put(d_id, new PeerConfigModel(pcm.getPort(), pcm.getDownLoadId(), id));
        }
    }

    public FileOwner(int _owner_port, String _file_name) {
        if (_file_name != null && new File(_file_name).exists()) {
            this.file_name = _file_name;
        }
        this.owner_port = _owner_port;
        try {
            owner_skt = new ServerSocket(this.owner_port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        // Init file chunk list
        this.divideFileIntoChunks();
    }

    public void initiateOwner() {
        try {
            while (true) {
                System.out.println(this.peerName + " is waiting for peers...");
                Socket o_skt = owner_skt.accept();
                ObjectOutputStream objectOutputStream;
                ObjectInputStream inputStream;
                objectOutputStream = new ObjectOutputStream(o_skt.getOutputStream());
                inputStream = new ObjectInputStream(o_skt.getInputStream());
                String message_from_client = (String) inputStream.readObject();
                if (message_from_client.toUpperCase().startsWith("CONFIG")) {
                    String[] split_string = message_from_client.split("\\s+");
                    int id = Integer.parseInt(split_string[1]);
                    int peer_port = Integer.parseInt(split_string[2]);
                    int download_port = Integer.parseInt(split_string[3]);
                    createConfig(id, peer_port, download_port);
                    this.printConfig();
                }
                OwnerProcess op = new OwnerProcess();
                op.initiateBlockPerFile(file_block_list);
                op.initiateNameForCurrentFile(this.file_name);
                op.initiateCurrentNetwork(o_skt, objectOutputStream, inputStream);
                op.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void printConfig() {
        for (int name : config_peer.keySet()) {
            PeerConfigModel peerConfigModel = config_peer.get(name);
            System.out.println(name + " " + peerConfigModel.getPort() + " " +
                    peerConfigModel.getDownLoadId() + " " +
                    peerConfigModel.getUploadId());
        }
    }

    private void divideFileIntoChunks() {
        try {
            File sepDir = new File("OwnerDir");
            if (!sepDir.exists()) {
                sepDir.mkdir();
            }
            // Memory fetch
            FileInputStream fileInputStream = new FileInputStream(this.file_name);
            byte[] buffer = new byte[this.FILE_MAX_SIZE];
            int id = 0;
            int character;
            while ((character = fileInputStream.read(buffer)) != -1) {
                perFormFileChunks(buffer, character, id);
                buffer = new byte[this.FILE_MAX_SIZE];
                id++;
            }
            System.out.println("[Owner] Total " + id + " Blocks");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void perFormFileChunks(byte[] buffer, int character, int id) throws IOException {
        byte[] bChunk = Arrays.copyOfRange(buffer, 0, character);
        file_block_list.put(id, bChunk);
        System.out.println("Block --> " + id + " = " + character + " bytes");
        FileOutputStream fso = new FileOutputStream("OwnerDir/" + id, false);
        fso.write(bChunk);
        fso.flush();
        fso.close();
    }

    private static void testConfig() {
//        createConfig(2, 9001, 9005);
//        createConfig(3, 9002, 9001);
//        createConfig(4, 9003, 9002);
//        createConfig(5, 9004, 9003);
//        createConfig(6, 9005, 9004);

        for (int name : config_peer.keySet()) {
            PeerConfigModel peerConfigModel = config_peer.get(name);
            System.out.println(name + " " + peerConfigModel.getPort() + " " +
                    peerConfigModel.getDownLoadId() + " " +
                    peerConfigModel.getUploadId());
        }
    }

    public static void main(String[] args) {
        int owner_port = 0;
        String file_name = "/Users/shantanughosh/Desktop/Shantanu_MS/Fall 19/CN/Projects/P2p_Final/GitHub/Bittorrent-CN/TCP-connection-1.pdf";
        if (args.length > 0) {
            owner_port = Integer.parseInt(args[0]);
            // file_name = args[1];
        }
        System.out.println("Owner Port: " + owner_port + " File Name: " + file_name);
        new FileOwner(owner_port, file_name).initiateOwner();
    }
}
