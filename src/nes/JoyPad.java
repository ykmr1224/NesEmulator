package nes;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

import debug.Debug;

public class JoyPad {
	boolean[] buttonState = new boolean[8];
	
	public static final int BUTTON_A = 0;
	public static final int BUTTON_B = 1;
	public static final int BUTTON_SELECT = 2;
	public static final int BUTTON_START = 3;
	public static final int BUTTON_UP = 4;
	public static final int BUTTON_DOWN = 5;
	public static final int BUTTON_LEFT = 6;
	public static final int BUTTON_RIGHT = 7;
	
	public void setState(int button, boolean state){
		buttonState[button] = state;
	}
	
	int count = 0;
	public void readReset(){
		count = 0;
	}
	public int read(){
		return buttonState[(count++)%8]?1:0;
	}
	
	
	Map<Integer, Integer> keyMap = new HashMap<Integer, Integer>();
	{
		keyMap.put(KeyEvent.VK_Z, BUTTON_A);
		keyMap.put(KeyEvent.VK_X, BUTTON_B);
		keyMap.put(KeyEvent.VK_SPACE, BUTTON_SELECT);
		keyMap.put(KeyEvent.VK_ENTER, BUTTON_START);
		keyMap.put(KeyEvent.VK_UP, BUTTON_UP);
		keyMap.put(KeyEvent.VK_DOWN, BUTTON_DOWN);
		keyMap.put(KeyEvent.VK_LEFT, BUTTON_LEFT);
		keyMap.put(KeyEvent.VK_RIGHT, BUTTON_RIGHT);
	}
	
	public KeyListener getListener(){
		return new JoyListener();
	}
	
	public class JoyListener implements KeyListener{
		public void keyPressed(KeyEvent e) {
			Integer key = keyMap.get(e.getKeyCode());
			if(key == null) return;
			setState(key, true);
			Debug.println("Key Press: " + key);
		}

		public void keyReleased(KeyEvent e) {
			Integer key = keyMap.get(e.getKeyCode());
			if(key == null) return;
			setState(key, false);
			Debug.println("Key Release: " + key);
		}

		public void keyTyped(KeyEvent e) {
		}
	}
}
