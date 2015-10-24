package debug;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTabbedPane;

public class MemoryViewer extends JPanel implements AdjustmentListener{
	byte[] mem;
	int offset = 0;
	int lines;
	
	static JFrame frame;
	static JTabbedPane tabbed;
	
	static void addViewer(String title, MemoryViewer mv, JScrollBar bar){
		if(frame == null){
			frame = new JFrame("MemoryViewer");
			frame.add(tabbed = new JTabbedPane());
			frame.setBounds(600, 0, 450, 400);
			frame.setVisible(true);
		}
		JPanel temp = new JPanel();
		temp.setLayout(new BorderLayout());
		temp.add(mv);
		temp.add(bar, BorderLayout.EAST);
		tabbed.add(title, temp);
	}
	
	JScrollBar scroll;
	
	public MemoryViewer(String title, byte[] mem) {
		this.mem = mem;
		lines = mem.length/16;
		scroll = new JScrollBar(JScrollBar.VERTICAL, 0, Math.min(16, lines), 0, lines);
		scroll.addAdjustmentListener(this);
		addViewer(title, this, scroll);
	}
	
	Font font = new Font("Monospaced", Font.PLAIN, 12);
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.setColor(Color.black);
		g.setFont(font);
		
		int h = this.getHeight();
		int fontH = 16;
		int lines = h/fontH;
		for(int i=0; i<lines; i++){
			StringBuffer buff = new StringBuffer();
			int addr = (offset+i)<<4;
			buff.append(Debug.hex4(addr));
			buff.append("|");
			for(int j=0; j<16 && addr+j<mem.length; j++){
				buff.append(Debug.hex2(mem[addr+j]));
				buff.append(j%4==3?"|":" ");
			}
			if((offset+i)% 16 == 0){
				g.drawLine(0, fontH*i+4, this.getWidth(), fontH*i+4);
			}
			g.drawString(buff.toString(), 0, fontH*i+fontH);
		}
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
		int val = e.getValue();
		offset = val;
		repaint();
	}

	static byte[] test = new byte[0x4000];
	public static void main(String[] args) {
		for(int i=0; i<0x4000; i++){
			test[i] = (byte)i;
		}
		new MemoryViewer("Test", test);
	}
}
