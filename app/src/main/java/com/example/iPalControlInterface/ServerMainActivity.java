package com.example.iPalControlInterface;

import java.net.*;
import java.util.*;

import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.view.Menu;

import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

// new imports


@TargetApi(19)
public class ServerMainActivity extends Activity {
	final static int NUM_MOTORS = 10;
	final static int PORT = 38888;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server_main_activity);

		startService(new Intent(this, ServerService.class));
		Log.d("Network", "Main activity created");
		TextView tbPort = (TextView) this.findViewById(R.id.textPort);
		tbPort.setText("Server running on " +  getIPAddress(true) + ":" + PORT);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.simple_server_main, menu);
		return true;
	}

	public void onStop() {
		super.onStop();
		Log.d("Network", "Killing server...");
	}

	/**
	 * Get IP address from first non-localhost interface
	 * http://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
	 *
	 * @param ipv4  true=return ipv4, false=return ipv6
	 * @return  address or empty string
	 */
	private static String getIPAddress(boolean useIPv4) {
		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress();
						//boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						boolean isIPv4 = sAddr.indexOf(':')<0;

						if (useIPv4) {
							if (isIPv4)
								return sAddr;
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
								return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
							}
						}
					}
				}
			}
		} catch (Exception ex) { } // for now eat exceptions
		return "";
	}
}
