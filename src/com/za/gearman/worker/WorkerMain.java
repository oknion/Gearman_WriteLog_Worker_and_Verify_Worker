package com.za.gearman.worker;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.gearman.Gearman;
import org.gearman.GearmanFunction;
import org.gearman.GearmanFunctionCallback;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;

public class WorkerMain {
	public static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager
			.getLogger(WorkerMain.class.getName());
	private static String WRITELOG_FUNC = "writelog";
	private static final String DELIMITER = " ";

	static AtomicInteger count = new AtomicInteger();

	public static void main(String[] args) {
		Properties props = new Properties();
		// Timer to print tracking performance
		Timer timer = new Timer();
		timer.schedule(new TrackVerifyWorker(), 1000, 1000);

		try {
			Gearman gearman = Gearman.createGearman();
			FileInputStream in = new FileInputStream("prop.properties");
			props.load(in);
			in.close();
			System.out.println(
					"Start writelog gearmanworker:" + props.getProperty("gmhost") + ":" + props.getProperty("gmport"));
			GearmanServer server = gearman.createGearmanServer(props.getProperty("gmhost"),
					Integer.parseInt(props.getProperty("gmport")));
			GearmanWorker worker = gearman.createGearmanWorker();
			worker.addServer(server);
			worker.setMaximumConcurrency(Integer.parseInt(props.getProperty("maxConcurrent")));
			worker.addFunction(WRITELOG_FUNC, new GearmanFunction() {
				@Override
				public byte[] work(String arg0, byte[] bytes, GearmanFunctionCallback arg2) throws Exception {
					count.getAndIncrement();
					ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
					ObjectInputStream ois = new ObjectInputStream(bais);
					List<?> logData = (List<?>) ois.readObject();
					if (logData == null) {
						return null;
					}
					String log = "";
					for (Object val : logData) {
						log += replaceSpaceWithHyphen((String) val) + DELIMITER;
					}
					LOGGER.error(log);
					return null;

				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static String replaceSpaceWithHyphen(String value) {
		if (value == null || !value.contains(" ")) {
			return value;
		}
		return value.trim().replaceAll(" ", "-");
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
}
