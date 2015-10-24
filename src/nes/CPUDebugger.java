package nes;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import debug.Debug;
import util.PropertyViewer;


public class CPUDebugger {
	boolean suspend = false;
	PropertyViewer pv;
	JTextArea historyTxt;
	DebugPanel panel;
	
	class DebugPanel extends JPanel implements ActionListener{
		JToggleButton stopButton;
		JButton skip1Button;
		JButton skip10Button;
		JButton skip100Button;
		JToggleButton addrButton;
		JTextField addrText;
		JToggleButton opeButton;
		JTextField opeText;
		
		public DebugPanel() {
			this.setLayout(null);
			pv = new PropertyViewer();
			pv.setBounds(10, 10, 200, 300);
			this.add(pv);
			stopButton = new JToggleButton("Suspend");
			stopButton.addActionListener(this);
			stopButton.setBounds(10, 310, 100, 20);
			this.add(stopButton);
			skip1Button = new JButton(">");
			skip1Button.setEnabled(false);
			skip1Button.addActionListener(this);
			skip1Button.setBounds(110, 310, 30, 20);
			this.add(skip1Button);
			skip10Button = new JButton(">>");
			skip10Button.setEnabled(false);
			skip10Button.addActionListener(this);
			skip10Button.setBounds(140, 310, 30, 20);
			this.add(skip10Button);
			skip100Button = new JButton(">>>");
			skip100Button.setEnabled(false);
			skip100Button.addActionListener(this);
			skip100Button.setBounds(170, 310, 40, 20);
			this.add(skip100Button);
			addrButton = new JToggleButton("Addr");
			addrButton.setBounds(220, 10, 40, 20);
			addrButton.addActionListener(this);
			this.add(addrButton);
			addrText = new JTextField();
			addrText.setBounds(260, 10, 60, 20);
			this.add(addrText);
			opeButton = new JToggleButton("Ope");
			opeButton.setBounds(220, 30, 40, 20);
			opeButton.addActionListener(this);
			this.add(opeButton);
			opeText = new JTextField();
			opeText.setBounds(260, 30, 60, 20);
			this.add(opeText);
			historyTxt = new JTextArea();
			historyTxt.setEnabled(false);
			historyTxt.setFont(new Font("Monospaced", Font.PLAIN, 12));
			JScrollPane sc = new JScrollPane(historyTxt);
			sc.setBounds(220, 60, 370, 270);
			this.add(sc);
		}
		
		@Override
		public Dimension getPreferredSize() {
			return new Dimension(600, 400);
		}
		
		public void suspend(){
			if(!stopButton.isSelected())
				stopButton.doClick();
		}

		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == stopButton){
				suspend = stopButton.isSelected();
				skip1Button.setEnabled(suspend);
				skip10Button.setEnabled(suspend);
				skip100Button.setEnabled(suspend);
			}else if(e.getSource() == skip1Button){
				skipCount = 1;
			}else if(e.getSource() == skip10Button){
				skipCount = 10;
			}else if(e.getSource() == skip100Button){
				skipCount = 100;
			}else if(e.getSource() == addrButton){
				if(addrButton.isSelected()){
					String txt = addrText.getText();
					if(txt.length() > 0)
						hookAddr = Integer.parseInt(txt, 16);
					addrText.setEnabled(false);
				}else{
					hookAddr = 0;
					addrText.setEnabled(true);
				}
			}else if(e.getSource() == opeButton){
				if(opeButton.isSelected()){
					String txt = opeText.getText();
					if(txt.length() > 0)
						hookOpe = Integer.parseInt(opeText.getText(), 16);
					opeText.setEnabled(true);
				}else{
					hookOpe = -1;
					opeText.setEnabled(true);
				}
			}
		}
	}
	
	int hookAddr = 0;
	int hookOpe = -1;
	
	int skipCount = 0;

	class HistoryItem{
		int pc;
		CPU.Operand ope;
		byte[] data;
		int a;
		int x, y;
		int s = 0xff;
		String st;
		HistoryItem(int pc, CPU.Operand ope, byte[] data, int a, int x, int y, int s, String st){
			this.data = data;
			this.ope = ope;
			this.a=a; this.x=x; this.y=y; this.pc=pc; this.s=s; this.st=st;
		}
	}
	int nItems = 10000;
	List<HistoryItem> history = new LinkedList<HistoryItem>();

	String createHistory(CPU cpu){
		StringBuffer buff = new StringBuffer();
		for(int i=0; i<history.size(); i++){
			HistoryItem item = history.get(i);
			int pc = item.pc;
			buff.append(Debug.hex4(pc));
			buff.append(":");
			CPU.Operand ope = item.ope;
			byte[] data = cpu.dbgFetch(pc, 3);
			if(ope != null){
				buff.append("A:"+Debug.hex4(item.a)+" X:"+Debug.hex2(item.x)+" Y:"+Debug.hex2(item.y)+" S:"+Debug.hex2(item.s)+" ST:"+item.st);
				buff.append(" "+ope.instStr + " " + ope.addr.toString(data[1], data[2]));
			}
			buff.append("\n");
		}
		return buff.toString();
	}
	
	public void debug(CPU cpu){
		history.add(new HistoryItem(cpu.pc, cpu.dbgToOpe(cpu.pc), cpu.dbgFetch(cpu.pc, 3), cpu.a, cpu.x, cpu.y, cpu.s, cpu.statusStr()));
		while(history.size()>nItems) history.remove(0);
		if(cpu.pc == hookAddr || (0xff&cpu.mem.data[cpu.pc]) == hookOpe){
			suspend = true;
			panel.suspend();
			skipCount = 0;
		}
		if(cpu.dbgToOpe(cpu.pc) == null){suspend = true;skipCount=0; panel.suspend();}
		if(!suspend) return;
		if(--skipCount>0){ return; }//skip
		skipCount=0;
		
		pv.set("a ", Debug.hex2(cpu.a));
		pv.set("st", cpu.statusStr());
		pv.set("x ", Debug.hex2(cpu.x));
		pv.set("y ", Debug.hex2(cpu.y));
		pv.set("pc", Debug.hex4(cpu.pc));
		pv.set("sp", Debug.hex4(cpu.s));
		CPU.Operand ope = cpu.dbgToOpe(cpu.pc);
		byte[] data = cpu.dbgFetch(cpu.pc, 3);
		if(ope!= null){
			pv.set("next", ope.instStr + " " + ope.addr.toString(data[1], data[2]));
		}
		historyTxt.setText(createHistory(cpu));
		
		while(suspend && skipCount == 0){
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
		}
	}
	
	public CPUDebugger() {
		JFrame frame = new JFrame("CPUDebugger");
		frame.setBounds(200, 200, 300, 200);
		frame.add(panel = new DebugPanel());
		frame.pack();
		frame.setVisible(true);
		panel.suspend();
	}
	
	public static void main(String[] args) {
		new CPUDebugger();
	}
}
