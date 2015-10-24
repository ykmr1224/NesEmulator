package util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class PropertyViewer extends JPanel{
	JList list;
	MyListModel model;
	
	class MyListModel extends AbstractListModel{
		List<String> key = new LinkedList<String>();
		List<String> data = new LinkedList<String>();
		
		public void set(String k, String d){
			synchronized(this){
				int i=0;
				for(;i<key.size();i++){
					int c = k.compareTo(key.get(i));
					if(c == 0){
						data.set(i, d);
						fireContentsChanged(this, i, i);
						return;
					}else if(c < 0){
						break;
					}
				}
				key.add(i, k);
				data.add(i, d);
				fireIntervalAdded(this, i, i);
			}
		}
		
		public void remove(String k){
			synchronized(this){
				for(int i=0; i<key.size(); i++){
					if(k.equals(key.get(i))){
						data.remove(i);
						key.remove(i);
						fireIntervalRemoved(this, i, i);
						break;
					}
				}
			}
		}
		
		public void clear(){
			synchronized(this){
				int size = key.size();
				data.clear();
				key.clear();
				fireIntervalRemoved(this, 0, size-1);
			}
		}

		public Object getElementAt(int index) {
			synchronized(this){
				if(key.size()>index)
					return key.get(index) + " : " + data.get(index);
				else
					return "***";
			}
		}

		public int getSize() {
			synchronized(this){
				return key.size();
			}
		}
	}
	
	public PropertyViewer() {
		model = new MyListModel();
		list = new JList(model);
		this.setLayout(new BorderLayout());
		this.add(new JScrollPane(list));
	}
	
	public void set(String key, String value){
		model.set(key, value);
	}
	public void remove(String key){
		model.remove(key);
	}
	public void clear(){
		model.clear();
	}

	@Override
	public Dimension getPreferredSize() {
		return super.getPreferredSize();
	}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		final PropertyViewer propv = new PropertyViewer();
		frame.getContentPane().add(propv);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		propv.set("a", "0x1111");
		propv.set("b", "0x2222");
		
		new Thread(new Runnable(){
			public void run() {
				for(int i=0; i<100; i++){
					propv.set("a", Integer.toHexString(i));
					propv.set("b", Integer.toHexString(i*2));
					propv.remove("a"+(i-1));
					propv.set("a"+i, Integer.toHexString(i*3));
					propv.set("b"+i, Integer.toHexString(i*8));
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
}
