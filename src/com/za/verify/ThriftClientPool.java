package com.za.verify;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public class ThriftClientPool<T extends TServiceClient> {

	Function<TTransport, T> clientFac;
	List<ServerInfo> serverInfos;
	GenericObjectPool<ThriftClient<T>> pool;

	public ThriftClientPool(Function<TTransport, T> clientFac, List<ServerInfo> serverInfos) throws Exception {
		this.clientFac = clientFac;
		this.serverInfos = serverInfos;
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxIdle(5);
		config.setMinIdle(3);
		config.setMaxTotal(10);
		pool = new GenericObjectPool<>(new ThriftClientFactory<ThriftClient<T>>(), config);

	}

	public ThriftClient<T> getClient() throws NoSuchElementException, IllegalStateException, Exception {
		return pool.borrowObject();
	}

	class ThriftClientFactory<X> extends BasePooledObjectFactory<X> {
		AtomicInteger get = new AtomicInteger(0);

		@Override
		public X create() throws Exception {
			get.getAndIncrement();
			TTransport transport = geTTransport(serverInfos.get(0));
			transport.open();

			X client = (X) new ThriftClient<>(pool, clientFac.apply(transport), serverInfos.get(0));
			return client;
		}

		@Override
		public PooledObject<X> wrap(X obj) {
			// TODO Auto-generated method stub
			return new DefaultPooledObject<>(obj);
		}

		@Override
		public boolean validateObject(org.apache.commons.pool2.PooledObject<X> p) {
			return true;
		};

		@Override

		public void activateObject(org.apache.commons.pool2.PooledObject<X> p) throws Exception {

			((ThriftClient<TServiceClient>) p.getObject()).openClient();
		};

		@Override
		public void destroyObject(org.apache.commons.pool2.PooledObject<X> p) throws Exception {
			((ThriftClient<TServiceClient>) p.getObject()).closeClient();
		};

		@Override
		public void passivateObject(org.apache.commons.pool2.PooledObject<X> p) throws Exception {

		};

		private TTransport geTTransport(ServerInfo serverInfo) {
			if (serverInfos == null) {
				throw new NullPointerException();
			}

			TTransport transport;
			transport = new TFramedTransport(new TSocket(serverInfo.getHost(), serverInfo.getPort()));

			return transport;
		}

	}

}
