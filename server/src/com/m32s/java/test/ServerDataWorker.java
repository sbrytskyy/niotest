package com.m32s.java.test;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

public class ServerDataWorker extends Thread {

	private static Logger logger = Logger.getLogger(ServerDataWorker.class);

	private final ExecutorService execService = Executors.newFixedThreadPool(32);
	private LinkedBlockingQueue<ServerDataEvent> eventsQueue = new LinkedBlockingQueue<ServerDataEvent>();

	class ServerDataEvent {

		private IServer server;
		private SocketChannel socket;
		private byte[] dataCopy;

		public ServerDataEvent(IServer server, SocketChannel socket, byte[] dataCopy) {
			this.server = server;
			this.socket = socket;
			this.dataCopy = dataCopy;
		}

		public IServer getServer() {
			return server;
		}

		public SocketChannel getSocket() {
			return socket;
		}

		public byte[] getDataCopy() {
			return dataCopy;
		}

	}
	
	public void processData(IServer server, SocketChannel socket, byte[] data,
			int count) {

		logger.debug("Bytes count to process = " + count);
		
		byte[] dataCopy = new byte[count];
		System.arraycopy(data, 0, dataCopy, 0, count);

		eventsQueue.add(new ServerDataEvent(server, socket, dataCopy));
	}

	public void run() {
		ServerDataEvent dataEvent;

		while (true) {
			try {
				dataEvent = eventsQueue.take();

				SmppProcessor sp = new SmppProcessor(dataEvent.getSocket(), dataEvent.getServer(), dataEvent.getDataCopy());
				execService.execute(sp);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public void invalidateSocket(SocketChannel socket) {
		SmppProcessor.invalidateSocket(socket);
	}
}
