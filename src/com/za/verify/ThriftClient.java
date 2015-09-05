package com.za.verify;

import java.io.IOException;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.TServiceClient;

public class ThriftClient<T extends TServiceClient> implements AutoCloseable {
	private GenericObjectPool<ThriftClient<T>> pool;
	private T client;
	private ServerInfo serverInfo;
	private boolean finish;

	public ThriftClient(GenericObjectPool<ThriftClient<T>> pool, T client, ServerInfo serverInfo) {
		this.pool = pool;
		// TODO Auto-generated constructor stub
		this.client = client;
		this.serverInfo = serverInfo;

	}

	@Override
	public void close() throws IOException {
		try {
			if (finish) {
				finish = false;
				pool.returnObject(this);
			} else {
				closeClient();
				pool.invalidateObject(this);
			}
		} catch (Exception e) {
			e.printStackTrace();
			closeClient();
			pool.returnObject(this);
		}
	}

	void closeClient() {
		ThriftUtil.closeClient(this.client);
	}

	void openClient() {
		ThriftUtil.openClient(this.client);
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		closeClient();
	}

	public void finish() {
		this.finish = true;
	}

	public T iFace() {
		return client;
	}

	public ServerInfo getServerInfo() {
		return serverInfo;
	}

}