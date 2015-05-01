package com.m32s.java.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MultiPortEcho {
	private int port = 8005;
	private ByteBuffer echoBuffer = ByteBuffer.allocate(1024);

	public MultiPortEcho() throws IOException {
		configure_selector();
	}

	private void configure_selector() throws IOException {
		// Create a new selector
		Selector selector = Selector.open();

		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ServerSocket ss = ssc.socket();
		InetSocketAddress address = new InetSocketAddress(port);
		ss.bind(address);

		SelectionKey key = ssc.register(selector, SelectionKey.OP_ACCEPT);

		System.out.println("Going to listen on " + port);

		while (true) {
			int num = selector.select();

			if (num == 0)
				continue;
			
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			Iterator<SelectionKey> it = selectedKeys.iterator();

			while (it.hasNext()) {
				key = it.next();

				if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
					// Accept the new connection
					ssc = (ServerSocketChannel) key
							.channel();
					SocketChannel sc = ssc.accept();
					sc.configureBlocking(false);

					// Add the new connection to the selector
					SelectionKey newKey = sc.register(selector,
							SelectionKey.OP_READ);
					System.out.println("Got connection from " + sc);
				} else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
					// Read the data
					SocketChannel sc = (SocketChannel) key.channel();

					// Echo data
					int bytesEchoed = 0;
					while (true) {
						echoBuffer.clear();

						int number_of_bytes = sc.read(echoBuffer);

						if (number_of_bytes <= 0) {
							sc.close();
							break;
						}

						echoBuffer.flip();

						sc.write(echoBuffer);
						bytesEchoed += number_of_bytes;
					}

					System.out.println("Echoed " + bytesEchoed + " from " + sc);
				}
				it.remove();
			}
		}
	}

	static public void main(String args[]) throws Exception {
		new MultiPortEcho();
	}
}