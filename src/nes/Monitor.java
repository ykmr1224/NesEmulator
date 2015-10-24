package nes;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;


public class Monitor extends JPanel{
	BufferedImage image;
	Graphics g;
	int width = 256;
	int height = 240;
	boolean painting = false;
	
	Color[] colors = new Color[64];
	{
		colors[0]=new Color(109,109,109);
		colors[1]=new Color(0,39,159);
		colors[2]=new Color(12,0,172);
		colors[3]=new Color(60,0,140);
		colors[4]=new Color(171,0,75);
		colors[5]=new Color(221,0,5);
		colors[6]=new Color(203,0,0);
		colors[7]=new Color(143,0,0);
		colors[8]=new Color(83,30,0);
		colors[9]=new Color(0,53,0);
		colors[10]=new Color(0,57,0);
		colors[11]=new Color(0,55,29);
		colors[12]=new Color(0,47,86);
		colors[13]=new Color(0,0,0);
		colors[14]=new Color(0,0,0);
		colors[15]=new Color(0,0,0);
		colors[16]=new Color(188,188,188);
		colors[17]=new Color(0,95,255);
		colors[18]=new Color(0,56,255);
		colors[19]=new Color(131,4,255);
		colors[20]=new Color(255,0,173);
		colors[21]=new Color(255,0,50);
		colors[22]=new Color(255,0,0);
		colors[23]=new Color(250,12,0);
		colors[24]=new Color(210,73,0);
		colors[25]=new Color(0,112,0);
		colors[26]=new Color(0,129,0);
		colors[27]=new Color(0,122,61);
		colors[28]=new Color(0,137,202);
		colors[29]=new Color(20,20,20);
		colors[30]=new Color(4,4,4);
		colors[31]=new Color(4,4,4);
		colors[32]=new Color(255,255,255);
		colors[33]=new Color(0,210,255);
		colors[34]=new Color(57,144,255);
		colors[35]=new Color(229,98,255);
		colors[36]=new Color(255,0,251);
		colors[37]=new Color(255,63,119);
		colors[38]=new Color(255,111,0);
		colors[39]=new Color(255,136,0);
		colors[40]=new Color(255,175,0);
		colors[41]=new Color(103,227,0);
		colors[42]=new Color(0,245,0);
		colors[43]=new Color(0,244,140);
		colors[44]=new Color(0,255,255);
		colors[45]=new Color(74,74,74);
		colors[46]=new Color(5,5,5);
		colors[47]=new Color(5,5,5);
		colors[48]=new Color(255,255,255);
		colors[49]=new Color(100,254,255);
		colors[50]=new Color(137,233,255);
		colors[51]=new Color(227,151,237);
		colors[52]=new Color(255,144,255);
		colors[53]=new Color(255,141,162);
		colors[54]=new Color(255,200,153);
		colors[55]=new Color(255,236,136);
		colors[56]=new Color(255,247,121);
		colors[57]=new Color(199,230,116);
		colors[58]=new Color(110,237,152);
		colors[59]=new Color(100,242,209);
		colors[60]=new Color(68,255,253);
		colors[61]=new Color(214,214,214);
		colors[62]=new Color(9,9,9);
		colors[63]=new Color(9,9,9);
	}
	
	boolean dbgClicked = false;
	
	public Monitor(){
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
		g = image.getGraphics();
		
		this.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				dbgClicked = true;
				Monitor.this.requestFocus();
			}
		});
	}
	
	public void clear(){
		g.setColor(Color.black);
		g.fillRect(0, 0, width, height);
	}
	
	public void setPixel(int x, int y, int color){
		g.setColor(colors[color]);
		g.fillRect(x, y, 1, 1);
	}
	
	public void refresh(){
		if(!painting){
			painting = true;
			repaint();
		}
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.drawImage(image, 0, 0, frame.getContentPane().getWidth(), frame.getContentPane().getHeight(), 0, 0, width, height, null);
		painting = false;
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(width, height);
	}
	
	JFrame frame = null;
	public void showAsFrame(){
		if(frame == null){
			frame = new JFrame("NES Test");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().add(this);
			frame.pack();
			frame.setVisible(true);
		}
	}
}
