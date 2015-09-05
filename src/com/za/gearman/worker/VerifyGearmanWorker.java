package com.za.gearman.worker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransport;
import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanFunction;
import org.gearman.GearmanFunctionCallback;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;

import com.vng.fresher.zcryptographer.ZCryptographer;
import com.za.verify.Function;
import com.za.verify.ServerInfo;
import com.za.verify.ThriftClientPool;
import com.za.verify.VerifyAppDomainService;
import com.za.verify.VerifyAppDomainService.Client;
import com.za.ziptocity.ZIpToCity;

public class VerifyGearmanWorker {
	public static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager
			.getLogger(WorkerMain.class.getName());
	private static String VERIFY_FUNC_NAME = "VerifyFnc";
	private static String WRITELOG_FUNCTION_NAME = "writelog";
	private static final String DELIMITER = " ";

	static AtomicInteger count = new AtomicInteger();

	static SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");
	// Thirft client pool
	static ThriftClientPool<Client> thriftClientPool;

	private static boolean verify(List<String> request) {
		if (request == null || request.size() != 24)
			return false;

		String id = ZCryptographer.tranform(request.get(0));
		String[] ids = id.split("-");
		// Check ZA
		if (!"ZA".equals(ids[0])) {
			return false;
		}
		try {
			if ((df.parse(ids[1]).compareTo(new Date()) >= 0)) {
				return false;
			}

		} catch (Exception e) {
			// LOGGER.warn(e.getMessage());

			return false;
		}
		request.set(0, id);
		// try (ThriftClient<Client> thriftClient =
		// thriftClientPool.getClient()) {
		// return
		// thriftClient.iFace().verifyAppDomain(request.get(RequestParams.idsite),
		// request.get(RequestParams.domain),
		// request.get(RequestParams._id_visit));
		// } catch (Exception e) {
		// e.printStackTrace();
		// return false;
		// }
		return true;
	}

	public static void main(String[] args) throws Exception {
		// Thrift Client Func
		Function<TTransport, VerifyAppDomainService.Client> funtion = new Function<TTransport, VerifyAppDomainService.Client>() {

			@Override
			public Client apply(TTransport t) {
				return new VerifyAppDomainService.Client(new TBinaryProtocol(t));
			}
		};

		// Load properties
		// BasicConfigurator.configure();
		Properties props = new Properties();
		FileInputStream in = new FileInputStream("prop.properties");
		props.load(in);
		in.close();
		// Timer to print tracking performance
		Timer timer = new Timer();
		timer.schedule(new TrackVerifyWorker(), 1000, 1000);
		// Thrift
		ServerInfo thriftServerinfo = new ServerInfo(props.getProperty("thrifthost"),
				Integer.parseInt(props.getProperty("thriftport")));
		List<ServerInfo> thriftServerinfos = new ArrayList<>();
		thriftServerinfos.add(thriftServerinfo);
		thriftClientPool = new ThriftClientPool<>(funtion, thriftServerinfos);

		try {
			final Gearman gearman = Gearman.createGearman();

			System.out.println(
					"Start gearman verify worker " + props.getProperty("gmhost") + props.getProperty("gmport"));
			final GearmanServer server = gearman.createGearmanServer(props.getProperty("gmhost"),
					Integer.parseInt(props.getProperty("gmport")));
			GearmanWorker worker = gearman.createGearmanWorker();
			worker.addServer(server);
			worker.setMaximumConcurrency(Integer.parseInt(props.getProperty("maxConcurrent")));
			final GearmanClient client = gearman.createGearmanClient();
			client.addServer(server);

			//

			worker.addFunction(VERIFY_FUNC_NAME, new GearmanFunction() {

				@Override
				public byte[] work(String arg0, byte[] bytes, GearmanFunctionCallback arg2) throws Exception {
					List<String> logData = byte2object(bytes);
					count.getAndIncrement();
					if (verify(logData)) {
						try {
							String[] urlref = UrlReferalUtil.referalParse(logData.get(5));
							logData.remove(logData.size() - 1);
							logData.set(5, urlref[0]); // set ref type
							logData.set(4, urlref[1]); // set ref url
							logData.set(16, ZIpToCity.getInstance().ipToCity(logData.get(16)));
						} catch (FileNotFoundException e1) {
							LOGGER.warn("Could not load dbcity.");
						}

						client.submitBackgroundJob(WRITELOG_FUNCTION_NAME, objectToBytes(logData));
					}
					return null;
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static byte[] objectToBytes(Object obj) throws IOException {
		ByteArrayOutputStream bais = new ByteArrayOutputStream();
		ObjectOutput ois = new ObjectOutputStream(bais);
		ois.writeObject(obj);
		return bais.toByteArray();
	}

	static class TrackVerifyWorker extends TimerTask {
		int lastValue = 0;

		@Override
		public void run() {
			System.out.println("Total request:" + count.get());
			System.out.println("Total in last SECOND:" + (count.get() - lastValue));
			lastValue = count.get();
		}
	}

	public static List<String> byte2object(byte[] bytes) throws ClassNotFoundException, IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
		List<String> obj = new ArrayList<>();
		obj.addAll((List<String>) ois.readObject());

		return obj;
	}
}
