package com.example.iPalControlInterface;

import java.util.Arrays;
import java.util.TreeMap;

import android.annotation.TargetApi;
import android.robot.motion.RobotMotion;
import android.util.Log;

@TargetApi(19)
public class State {
	int[] _rightArmAngles;
	int[] _leftArmAngles;
	
	public State() {
		this._rightArmAngles = new int[]{0,0,0,0,0};
		this._leftArmAngles = new int[]{0,0,0,0,0};
	}
	
	public State(int[] right, int[] left) {
		this._rightArmAngles = right;
		this._leftArmAngles = left;
	}
	
	/**
	 * Moves ROBOT to THIS state within DURATION milliseconds.
	 * All motor movements are executed at the same time.
	 * 
	 * @param robot The robot to be moved.
	 * @param duration The duration of each motor's motion.
	 * @param motor_flag 0=ease-stop; 1=sharp-stop; 2=no-stop
	 * @return returned status from robot.run command
	 */
	public int setRobotToState(RobotMotion robot, int duration, int motor_flag) {
		byte[] pdata = new byte[10 * _rightArmAngles.length + 10 * _leftArmAngles.length];
		for (int i = 0 ; i < _rightArmAngles.length; i++) {
			pdata [i * 10 + 0] = (byte) 0xF1; 	// constant
			pdata [i * 10 + 1] = (byte) 0xBE;  	// constant
			pdata [i * 10 + 2] = (byte) 0; 		// placeholder
			pdata [i * 10 + 3] = MotorIdDict.MOTOR_ID_MAP.get(MotorIdDict.RIGHT_ARM_JOINT_NAMES[i]).byteValue();
												// motor ID
			pdata [i * 10 + 4] = (byte) (_rightArmAngles[i] & 0xFF);
												// motor angle: least significant half
			pdata [i * 10 + 5] = (byte) ((_rightArmAngles[i] & 0xFF00) >> 8);
												// motor angle: most significant half
			pdata [i * 10 + 6] = (byte) motor_flag; 		// flag: 0=ease-stop; 1=sharp-stop; 2=no-stop
			pdata [i * 10 + 7] = (byte) (duration / 100); 		// duration in 100ms unit (e.g. 10 means 1 second: 10 * 100ms)
			pdata [i * 10 + 8] = (byte) 0;		// placeholder
			pdata [i * 10 + 9] = (byte) 0;		// placeholder
		}
		for (int i = 0 ; i < _leftArmAngles.length; i++) {
			pdata [i * 10 + 50] = (byte) 0xF1; 	// constant
			pdata [i * 10 + 51] = (byte) 0xBE;  // constant
			pdata [i * 10 + 52] = (byte) 0; 	// placeholder
			pdata [i * 10 + 53] = MotorIdDict.MOTOR_ID_MAP.get(MotorIdDict.LEFT_ARM_JOINT_NAMES[i]).byteValue();
												// motor ID
			pdata [i * 10 + 54] = (byte) (_leftArmAngles[i] & 0xFF);
												// motor angle: least significant half
			pdata [i * 10 + 55] = (byte) ((_leftArmAngles[i] & 0xFF00) >> 8);
												// motor angle: most significant half
			pdata [i * 10 + 56] = (byte) motor_flag; 
												// flag: 0=ease-stop; 1=sharp-stop; 2=no-stop
			pdata [i * 10 + 57] = (byte) (duration / 100); 
												// duration in 100ms unit (e.g. 10 means 1 second: 10 * 100ms)
			pdata [i * 10 + 58] = (byte) 0;		// placeholder
			pdata [i * 10 + 59] = (byte) 0;		// placeholder
		}
		return robot.run(pdata, pdata.length, 0, 1);
												// robot.run duration:
												//		0 = use separate motor durations in PDATA
												// robot.run flag:
												// 		0 = add to motor control buffer;
												// 		1 = execute immediately no waiting
	}
	
	/**
	 * Sets the iPal robot to a given joint state, STATE. The joints are moved in an order specified by ORDER.
	 * Each joint will take DURATION amount of milliseconds to execute.
	 * 
	 * @param state The target state. State consists of a set of left and right arm angles.
	 * @param order The order the joint movement should be executed in. ORDER should have a length equal to
	 * 				the number of joints in a state, k. ORDER must have all integers in [0, k).
	 * @param duration The number of milliseconds allowed for each joint movement. Note: the actual task may take
	 * 				longer than DURATION to execute.
	 * 
	 * First 5 elements of ORDER is for right arm; the last 5 elements of ORDER is for left arm.
	 */
	public void setRobotToStateInOrder (RobotMotion robot, int[] order, int duration, int flag) {
		if (order == null) {
			order = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
		}

		TreeMap<Integer, MoveMotorCommand> commandSequence = new TreeMap<Integer, MoveMotorCommand>();
		if (order.length != ServerMainActivity.NUM_MOTORS) {
			Log.e("Network", "Set state failed! Length of ORDER isn't " + ServerMainActivity.NUM_MOTORS);
			return;
		}
		if (this._rightArmAngles != null) {
			for (int i = 0; i < order.length / 2; i++) {
				commandSequence.put(order[i], new MoveMotorCommand(robot,
						MotorIdDict.RIGHT_ARM_JOINT_NAMES[i],
						this._rightArmAngles[i],
						duration,
						flag));
			}
		}
		if (this._leftArmAngles != null) {
			for (int i = order.length / 2 + 1; i < order.length; i++) {
				commandSequence.put(order[i], new MoveMotorCommand(robot,
						MotorIdDict.LEFT_ARM_JOINT_NAMES[i - (order.length / 2 + 1)],
						this._leftArmAngles[i - (order.length / 2 + 1)],
						duration,
						flag));
			}
		}

		for (Integer cmd_i : commandSequence.navigableKeySet()) {
			commandSequence.get(cmd_i).run();
		}
	}
	
	@Override
	public String toString() {
		return "{right arm angels: " + Arrays.toString(_rightArmAngles) + 
				", left arm angels: " + Arrays.toString(_leftArmAngles) + "}";
	}
}
