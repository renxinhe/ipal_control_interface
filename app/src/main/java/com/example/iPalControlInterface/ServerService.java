package com.example.iPalControlInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.robot.motion.RobotMotion;
import android.util.Log;

@TargetApi(19)
public class ServerService extends Service {
	final static int NUM_MOTORS = 10;

	private int _motion;
	private int _player;
	private volatile ServerSocket _serverSocket;
	private volatile State _curState;
	
	RobotMotion _robot = new RobotMotion();
	private RobotMotion.Listener _robotMotionListener;
	
	static volatile Semaphore resSema;
	static volatile long motionStartTime;
	static volatile long motionEndTime;

	@Override
	public void onCreate() {
		Log.d("Network", "Server service created");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		_robotMotionListener = new MotionListener();
		_robot.setListener(_robotMotionListener);

		new Thread(new SocketServerThread(this)).start();
		Log.d("Network", "Server service starting");
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		Log.d("Network", "Killing server...");
		try {
			if (_serverSocket != null) {
				_serverSocket.close();
			}
		} catch (IOException e) {
			LogStackTrace(e);
		}
	}

	static void LogStackTrace(Exception e) {
		String errMsg = e.toString() + "\n";
		for (StackTraceElement ste : e.getStackTrace()) {
			errMsg += "\t" + ste.toString() + "\n";
		}
		Log.e("Network", errMsg);
	}
	
	static void LogStackTrace(Exception e, String msg) {
		String errMsg = msg + "\n" + e.toString() + "\n";
		for (StackTraceElement ste : e.getStackTrace()) {
			errMsg += "\t" + ste.toString() + "\n";
		}
		Log.e("Network", errMsg);
	}
	
	class SocketServerThread implements Runnable {
		private Context _context;

		public SocketServerThread(Context context) {
			this._context = context;
			_curState = new State();
		}

		@Override
		public void run() {
			// Create socket server
			Log.d("IP", "Starting socket server");
			try {
				_serverSocket = new ServerSocket(38888);
				Log.d("Network", "Server Address: " + _serverSocket.getInetAddress() + ":" + _serverSocket.getLocalPort());
				Log.d("Network", "Listening for connection on port %d ....\n" + _serverSocket.getLocalPort());
				while (true) {
					Socket socket = null;
					try {
						socket = _serverSocket.accept();
						Log.d("Network", "Client socket: "
								+ socket.getInetAddress().toString());
						SocketClientThread clientThread = new SocketClientThread(
								socket, this._context);
						new Thread(clientThread).start();

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				LogStackTrace(e);
			}
		}
	}

	class SocketClientThread implements Runnable {

		private Socket _clientSocket;
		private Context _context;
		private String _reqBody;
		private String _resMsg;
		private HashMap<String, String> _paramArgs;

		public SocketClientThread(Socket clientSocket, Context act) {
			this._clientSocket = clientSocket;
			this._context = act;
			this._resMsg = "iPal request received.";
			this._paramArgs = new HashMap<String, String>();
		}

		@Override
		public void run() {
			BufferedReader in;
			String inputLine;
			_reqBody = "";
			try {
				in = new BufferedReader(new InputStreamReader(
						_clientSocket.getInputStream()));
				while (!(inputLine = in.readLine()).equals("")) {
					if (!_reqBody.equals("")) {
						_reqBody += "\t\t";
					}
					_reqBody += inputLine + "\n";
					System.out.println(inputLine);
					Log.d("Network", "Data read: " + inputLine);
				}

				handleRequest();

				String httpResponse = "HTTP/1.1 200 OK\r\n\r\n" + _resMsg;
				_clientSocket.getOutputStream().write(
						httpResponse.getBytes("UTF-8"));
				Log.d("Network", "Writing response to " + _clientSocket);
			} catch (IOException e) {
				LogStackTrace(e);
			} finally {
				try {
					if (_clientSocket != null) {
						_clientSocket.close();
					}
				} catch (IOException e) {
					LogStackTrace(e);
				}
			}
		}

		/**
		 * Dispatcher method that calls the more specific handlers for client
		 * requests.
		 */
		void handleRequest() {
			// Read input
			String requestType = _reqBody.substring(0, 3);
			String queryString = _reqBody.trim().split("\n")[0].split(" ")[1]
					.replaceAll("^\\/\\?", "");
			if (requestType.equals("GET")) {
				String[] queries = queryString.split("&");
				for (String query : queries) {
					String[] queryArgs = query.split("=", 2);
					_paramArgs.put(queryArgs[0], queryArgs[1]);
				}
			} else {
				Log.e("Network", "Only GET requests are supported");
				return;
			}

			// Process input
			String mode = _paramArgs.get("mode");
			if (mode == null) {
				_resMsg = "Error: Missing MODE parameter.";
				Log.e("Network", "Missing MODE parameter.");
				return;
			}
			try {
				if (mode.equals("single")) {
					handleSingleMotorRequest();
				} else if (mode.equals("state")) {
					handleStateRequest();
				} else if (mode.equals("relative_state")) {
					handleRelativeStateRequest();
				}
			} catch (NullPointerException e) {
				_resMsg = "Error: NullPointerException in handler. Did you forget some required parameter?\n";
				_resMsg += e.toString() + "\n";
				for (StackTraceElement ste : e.getStackTrace()) {
					_resMsg += "\t" + ste.toString() + "\n";
				}
				Log.e("Network", _resMsg);
			} catch (NumberFormatException e) {
				_resMsg = "Error: NumberFormatException in handler. Did you have non-digit characters in the input?\n";
				_resMsg += e.toString() + "\n";
				for (StackTraceElement ste : e.getStackTrace()) {
					_resMsg += "\t" + ste.toString() + "\n";
				}
				Log.e("Network", _resMsg);
			} catch (IOException e) {
				_resMsg = "Error: IOException in handler.\n";
				_resMsg += e.toString() + "\n";
				for (StackTraceElement ste : e.getStackTrace()) {
					_resMsg += "\t" + ste.toString() + "\n";
				}
				Log.e("Network", _resMsg);
			} catch (InterruptedException e) {
				_resMsg = "Error: InterruptedException in handler.\n";
				_resMsg += e.toString() + "\n";
				for (StackTraceElement ste : e.getStackTrace()) {
					_resMsg += "\t" + ste.toString() + "\n";
				}
				Log.e("Network", _resMsg);
			}
		}

		/**
		 * Handler for requests to set a single motor's position. MODE =
		 * "single"
		 * 
		 * Required GET query parameters: "motor": the String name of the target
		 * motor "angle": the target joint angle "duration": the number of
		 * milliseconds it takes to move the joint. (>100)
		 * @throws IOException 
		 * @throws InterruptedException 
		 */
		void handleSingleMotorRequest() throws IOException, InterruptedException {
			try {
				String motorName = _paramArgs.get("motor").toUpperCase();
				int motorId = MotorIdDict.MOTOR_ID_MAP.get(motorName);
				String angleStr = _paramArgs.get("angle");
				int angle = Integer.parseInt(angleStr);
				String durationStr = _paramArgs.get("duration");
				int duration = Integer.parseInt(durationStr);

				// Write motion start time
				motionStartTime = System.nanoTime();
				Log.d("Network", "motion start time: " + motionStartTime);
				
				_robot.runMotor(motorId, angle, duration, 1);
				resSema = new Semaphore(0);
				resSema.acquire();
				
				long motionDiffNanotime = motionEndTime - motionStartTime;
				_resMsg = "Single motor mode: motor " + motorName + " set to "
						+ angle + " degrees. Motion finished in " + motionDiffNanotime / 1000000.0 + " ms.";
			} catch (NullPointerException e) {
				LogStackTrace(e);
				Log.e("Network", _paramArgs.toString());
			}
		}

		/**
		 * Handler for setState requests. Sets the iPal robot to a given joint
		 * state. MODE = "state"
		 * 
		 * Required GET query parameters: "leftArm": a length 5 comma separated
		 * int list of target joint angles. "rightArm": a length 5 comma
		 * separated int list of target joint angles. "order": a length 10 comma
		 * separated int list that specifies the position of each joint in the
		 * sequence of moveMotor commands. "duration": the number of
		 * milliseconds it takes to move the joint. (>100)
		 * 
		 * The 5 joints are arm rotation, arm swing, forearm rotation, forearm
		 * swing, and wrist rotation, in that order.
		 */
		void handleStateRequest() {
			String rightArmStr = _paramArgs.get("rightArm");
			int[] rightArmAngles;
			if (rightArmStr == null) {
				rightArmAngles = null;
			} else {
				String[] rightArmAnglesStr = rightArmStr.split(",");
				rightArmAngles = new int[5];
				for (int i = 0; i < 5; i++) {
					rightArmAngles[i] = Integer.parseInt(rightArmAnglesStr[i]);
				}
			}
			Log.d("Network",
					"Right arm angles: " + Arrays.toString(rightArmAngles));

			String leftArmStr = _paramArgs.get("leftArm");
			int[] leftArmAngles;
			if (leftArmStr == null) {
				leftArmAngles = null;
			} else {
				String[] leftArmAnglesStr = leftArmStr.split(",");
				leftArmAngles = new int[5];
				for (int i = 0; i < 5; i++) {
					leftArmAngles[i] = Integer.parseInt(leftArmAnglesStr[i]);
				}
			}
			Log.d("Network",
					"Left arm angles: " + Arrays.toString(leftArmAngles));

			State newState = new State(rightArmAngles, leftArmAngles);

			String orderStr = _paramArgs.get("order");
			int[] orders;
			if (orderStr == null) {
				orders = null;
			} else {
				String[] ordersStr = orderStr.split(",");
				orders = new int[10];
				for (int i = 0; i < 10; i++) {
					orders[i] = Integer.parseInt(ordersStr[i]);
				}
			}

			String durationStr = _paramArgs.get("duration");
			int duration = Integer.parseInt(durationStr);

			if (orders == null) {
				newState.setRobotToState(_robot, duration, 1);
			} else {
				newState.setRobotToStateInOrder(_robot, orders, duration, 1);
			}

			_resMsg = "State mode: state set to " + newState.toString();
			_curState = newState;

		}

		/**
		 * Handler for setRelativeState requests. Sets the iPal robot to a given
		 * relative joint state. MODE = "relative_state"
		 * 
		 * Required GET query parameters: "leftArm": a length 5 comma separated
		 * int list of target joint angles. "rightArm": a legnth 5 comma
		 * separated int list of target joint angles. "order": a length 10 comma
		 * separated int list that specifies the position of each joint in the
		 * sequence of moveMotor commands. "duration": the number of
		 * milliseconds it takes to move the joint. (>100)
		 * 
		 * The 5 joints are arm rotation, arm swing, forearm rotation, forearm
		 * swing, and wrist rotation, in that order.
		 */
		void handleRelativeStateRequest() {

			String rightArmStr = _paramArgs.get("rightArm");
			if (rightArmStr != null) {
				String rightArmParams = "";
				String[] rightArmAnglesStr = rightArmStr.split(",");
				for (int i = 0; i < 5; i++) {
					rightArmParams += Integer.parseInt(rightArmAnglesStr[i])
							+ _curState._rightArmAngles[i];
					if (i < 4) {
						rightArmParams += ",";
					}
				}
				_paramArgs.put("rightArm", rightArmParams);
			}

			String leftArmStr = _paramArgs.get("leftArm");
			if (leftArmStr != null) {
				String leftArmParams = "";
				String[] leftArmAnglesStr = leftArmStr.split(",");
				for (int i = 0; i < 5; i++) {
					leftArmParams += Integer.parseInt(leftArmAnglesStr[i])
							+ _curState._leftArmAngles[i];
					if (i < 4) {
						leftArmParams += ",";
					}
				}
				_paramArgs.put("leftArm", leftArmParams);
			}
			Log.d("Network", _paramArgs.toString());
			_paramArgs.put("mode", "state");
			handleStateRequest();

		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d("Network", "Intent package " + intent.getPackage());
		return null;
	}
}
