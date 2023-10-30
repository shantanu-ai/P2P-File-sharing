# P2P File Sharing
1. no always-on server
2. arbitrary end systems directly communicate
3. peers request service from other peers, provide service in return to other peers
     - self scalability – new peers bring new service capacity, as well as new service demands
4. peers are intermittently connected and change IP addresses
    - complex management
<img src="https://github.com/Shantanu48114860/P2P-File-sharing/blob/master/images/p2p_pic.png" width="250" height="300">

# Programming Environment
  Java
  
# Requirement
Requirement is specified in the [Project2.pdf](https://github.com/Shantanu48114860/P2P-File-sharing/blob/master/Project2.pdf) 
and [Project2.pptx](https://github.com/Shantanu48114860/P2P-File-sharing/blob/master/Project2.pptx) file.

# Compile
<b>File owner</b> <br/>
cd Peer1_Owner <br/>
javac FileOwner.java <br/>

<b>Peer2</b> <br/>
cd Peer2 <br/>
javac Peer.java <br/>

<b>Peer3</b> <br/>
cd Peer3 <br/>
javac Peer2.java <br/>

<b>Peer4</b> <br/>
cd Peer4 <br/>
javac Peer.java <br/>

<b>Peer5</b> <br/>
cd Peer5 <br/>
javac Peer5.java <br/>

<b>Peer6</b> <br/>
cd Peer6 <br/>
javac Peer6.java 
 
# How to run
1. Start the file owner process, giving a listening port.
2. Start five peer processes as:
<b>file owner, peer itself, download neighbor(another peer’s port) </b>
3. Each peer connects to the server’s listening port. The latter creates a new thread to upload one or several file chunks to the peer, while its main thread goes back to
listening for new peers.
4. After receiving chunk(s) from the file owner, the peer stores them as separate file(s)
and creates a summary file, listing the IDs of the chunks it has.
5. The peer then proceeds with two new threads, with one thread listening to its upload
neighbor to which it will upload file chunks, and the other thread connecting to its
download neighbor.
6. The peer requests for the chunk ID list from the download neighbor, compares with
its own to find the missing ones, and randomly requests a missing chunk from the neighbor. In the meantime, it sends its own chunk ID list to its upload neighbor, and upon request uploads chunks to the neighbor.
7. After a peer has all file chunks, it combines them for a single file.
8. A peer MUST output its activity to its console whenever it receives a chunk, sends a chunk, receives a chunk ID list, sends out a chunk ID list, requests for chunks, or
receives such a request.

**A demo video can be found at [here](https://youtu.be/yf0M3uXAPNc)**

<img src="https://github.com/Shantanu48114860/P2P-File-sharing/blob/master/Demo_Startup.png" width="1000" height="750">

# Post Completion:
After completion the file will be downloaded to each of the peer. Plus inside each Peer on Peer<id>Dir folder will be created where all the file chunks and a summary file which contains the chunk ids that the peer got from the owner will be present.
A detailed demo video can be found as <b>demo.mp4</b> file.
<img src="https://github.com/Shantanu48114860/P2P-File-sharing/blob/master/Demo_Complete.png" width="1000" height="750">
     



