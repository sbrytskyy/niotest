package com.m32s.java.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

public class NewSmsSimulator extends Thread implements IServer {
	
	private static Logger logger = Logger.getLogger(NewSmsSimulator.class);
	
	private static final int PORT = 2775;

	private ByteBuffer readBuffer = ByteBuffer.allocate(ISmppConstants.MAX_PACKET_SIZE);
	
	private ServerDataWorker worker = new ServerDataWorker();
	
	private Selector selector;

	// A list of ChangeRequest instances
	private CopyOnWriteArrayList<ChangeRequest> changeRequests = new CopyOnWriteArrayList<ChangeRequest>();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private ConcurrentHashMap<SocketChannel, CopyOnWriteArrayList<ByteBuffer>> pendingData = new ConcurrentHashMap<SocketChannel, CopyOnWriteArrayList<ByteBuffer>>();
	  
	private static NewSmsSimulator instance;

	private NewSmsSimulator() {
	}
	
	public static NewSmsSimulator getInstance() {
		if (instance == null) {
			instance = new NewSmsSimulator();
			try {
				instance.init();
			} catch (IOException e) {
				throw new ExceptionInInitializerError(e);
			}
		}
		logger.debug("Simulator initialized succesfully.");
		return instance;
	}

	public void init() throws IOException {
//		worker.start();

		selector = Selector.open();
		
		ServerSocketChannel ssChannel = ServerSocketChannel.open();
		ssChannel.configureBlocking(false);

		SocketAddress address = new InetSocketAddress(PORT);
		ssChannel.bind(address);

		ssChannel.register(selector, SelectionKey.OP_ACCEPT);
		
		this.setDaemon(true);
	}
	
	public void run() {
		while (true) {
			try {
				// Process any pending changes
				Iterator<ChangeRequest> changes = changeRequests.iterator();
				while (changes.hasNext()) {
					ChangeRequest change = changes.next();
					switch (change.getType()) {
					case ChangeRequest.CHANGEOPS:
						SelectionKey key = change.getSocket().keyFor(selector);
						key.interestOps(change.getOps());
					}
				}
				changeRequests.clear();
				
				logger.debug("Waiting for selector.select()...");
				int select = selector.select();
				logger.debug("The number of keys, whose ready-operation sets were updated = " + select);
				
				Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
				
				while (selectedKeys.hasNext()) {
					SelectionKey key = selectedKeys.next();
					selectedKeys.remove();

			          if (!key.isValid()) {
			            continue;
			          }
			          
			          if (key.isAcceptable()) {
						accept(key);
					} else if (key.isReadable()) {
						read(key);
					} else if (key.isWritable()) {
						write(key);
					}
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private void accept(SelectionKey key) throws IOException,
			ClosedChannelException {
		
		logger.debug("OP_ACCEPT: " + key);
		ServerSocketChannel channel = (ServerSocketChannel) key.channel();
		channel.configureBlocking(false);

		SocketChannel accept = channel.accept();
		accept.configureBlocking(false);

		accept.register(selector, SelectionKey.OP_READ);
	}

	private void read(SelectionKey key) throws IOException {
		logger.debug("OP_READ: " + key);

		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		readBuffer.clear();

		int numRead;
		try {
			numRead = socketChannel.read(readBuffer);
		} catch (IOException e) {
			logger.error(e.getMessage());
			key.cancel();
			socketChannel.close();
			worker.invalidateSocket(socketChannel);
			return;
		}
		
		if (numRead == -1) {
			key.cancel();
			socketChannel.close();
			worker.invalidateSocket(socketChannel);
			//throw new IOException("Socket closed");
			logger.warn("Socket closed");
			return;
		}
		
		worker.processData(this, socketChannel, this.readBuffer.array(), numRead); 
	}

	private void write(SelectionKey key) throws IOException {
		logger.debug("OP_WRITE: " + key);
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		CopyOnWriteArrayList<ByteBuffer> queue = pendingData.get(socketChannel);

		// Write until there's not more data ...
		while (!queue.isEmpty()) {
			ByteBuffer buf = queue.get(0);
			if (buf != null) {
				socketChannel.write(buf);
				if (buf.remaining() > 0) {
					// ... or the socket's buffer fills up
					logger.warn("OP_WRITE: the socket's buffer fills up!");
					break;
				}
			} else {
				logger.warn("OP_WRITE: " + key + "; empty data!");
			}
			queue.remove(0);
		}

		if (queue.isEmpty()) {
			// We wrote away all data, so we're no longer interested
			// in writing on this socket. Switch back to waiting for
			// data.
			key.interestOps(SelectionKey.OP_READ);
		}
	}
	
	@Override
	public void send(SocketChannel socket, byte[] data) {
		// Indicate we want the interest ops set changed
		changeRequests.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

		// And queue the data we want written
		CopyOnWriteArrayList<ByteBuffer> queue = pendingData.get(socket);
		if (queue == null) {
			queue = new CopyOnWriteArrayList<ByteBuffer>();
			pendingData.put(socket, queue);
		}
		queue.add(ByteBuffer.wrap(data));
		logger.debug("[TO SEND] socket: " + socket + ", queue size: " + queue.size());

		// Finally, wake up our selecting thread so it can make the required
		// changes
		selector.wakeup();
	}
	
	public static void main(String[] args) {
		logger.info("Starting SMSC simulator. Cores count: " + Runtime.getRuntime().availableProcessors());
		
		NewSmsSimulator smsSimulator = NewSmsSimulator.getInstance();
		smsSimulator.start();

		try {
			smsSimulator.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
