package com.example.iPalControlInterface;

import android.robot.motion.RobotMotion;
import android.util.Log;

public class MoveMotorCommand implements Runnable{
	private RobotMotion _robot;
	private int _motorId;
	private int _destAngle;
	private int _duration;
	private int _flag;
	
	private static long _prevCommandNanotime = -1;
	
	public MoveMotorCommand(RobotMotion robot, String motorName, int destAngle, int duration, int flag) {
		this._robot = robot;
		motorName = motorName.toUpperCase();
		Integer motorId = MotorIdDict.MOTOR_ID_MAP.get(motorName);
		if (motorId == null) {
			_motorId = Integer.MIN_VALUE;
			Log.e("Network", "Motor named " + motorName + " doesn't exist.");
		} else {
			_motorId = motorId;
		}
		this._destAngle = destAngle;
		this._duration = duration;
		this._flag = flag;
	}

	@Override
	public void run() {
		if (_motorId == Integer.MIN_VALUE || _destAngle == Integer.MIN_VALUE) {
			return;
		}
		Log.d("Network", "Moving motor id " + _motorId + " to " + _destAngle + " degrees.");
		_robot.runMotor(_motorId, _destAngle, _duration, _flag);
		if (_prevCommandNanotime == -1) {
			_prevCommandNanotime = System.nanoTime();
		} else {
			long timeElapsed = System.nanoTime() - _prevCommandNanotime;
			_prevCommandNanotime = System.nanoTime();
			Log.d("Network", "Time since last command: " + (timeElapsed / 1000000) + "ms " + (timeElapsed % 1000000 / 1000) + "us " + (timeElapsed % 1000) + "ns");
		}
	}
}
