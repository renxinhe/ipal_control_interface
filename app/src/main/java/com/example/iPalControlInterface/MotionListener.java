package com.example.iPalControlInterface;

import android.robot.motion.RobotMotion;

import android.util.Log;

public class MotionListener implements RobotMotion.Listener{
	
	public MotionListener() {
		Log.d("Network", "Listener initialized");
	}

	@Override
	public void onCompleted(int session_id, int result) {
		Log.d("Network", "session id: " + session_id);
		Log.d("Network", "result: " + result);
		
		// Write motion start time
		long motionEndTime = System.nanoTime();
		Log.d("Network", "motion end time: " + motionEndTime);
		ServerService.motionEndTime = motionEndTime;
		ServerService.resSema.release();
	}

	@Override
	public void onStatusChanged(int status) {
		Log.d("Network", "status: " + status);
	}

}
