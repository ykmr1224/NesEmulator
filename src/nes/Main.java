package nes;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;

public class Main {
	static NESContext context = new NESContext();
	
	static class Clockmeter{
		long start;
		int cycle;
		int clockCount;
		int count;
		float speed;
		int waitEvery;
		boolean faster; //faster than clock or not

		/**
		 * @param cycle clocks per sec
		 * @param speed cpu will execute cycle*speed clocks
		 */
		public Clockmeter(int cycle, float speed){
			this.cycle = cycle;
			this.speed = speed;
			this.waitEvery = Math.max(1, (int)(cycle*speed/100));
			this.faster = false;
			reset();
		}
		public void reset(){
			this.start = System.currentTimeMillis();
			this.clockCount = 0;
			this.count = 0;
		}
		public void changeSpeed(float speed){
			this.speed = speed;
			this.waitEvery = Math.max(1, (int)(cycle*speed/100));
		}
		private void cpuwait(){
			if(++count % waitEvery == 0){// wait once a 1000 times
				long diff = start + (long)(clockCount*1000/(speed*cycle)) - System.currentTimeMillis();
				this.faster = diff >= 0;
				if(diff>10){
					try {
						Thread.sleep(diff);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		public void add(int clock){
			clockCount += clock;
			cpuwait();
			if(clockCount>cycle){
				int clcps = clockCount*1000/(int)(System.currentTimeMillis()-start);
				System.out.println("Clocks/Sec = "+clcps);
				reset();
			}
		}
	}
	
	public static void boot(NESFile file){
		context.nes = file;
		context.monitor = new Monitor();
		context.joypad = new JoyPad();
		context.monitor.addKeyListener(context.joypad.getListener());
		context.memory = new Memory(context);
		context.cpu = new CPU(context.memory);
		context.ppu = new PPU(context.cpu, context.monitor);
		
		if(context.nes.CHRROM.length > 0)
			context.ppu.loadCHRROM(context.nes.CHRROM[0], 0);
		for(int i=0; i<2; i++){
			context.memory.loadPRGRAM(i, context.nes.PRGROM[i%context.nes.PRGROMCount]);
		}
		context.ppu.setMirroring(context.nes.mirror);
		context.memory.showDebugWindow();
		context.cpu.reset();
		context.ppu.showDebugWindow();
		context.monitor.showAsFrame();

		
		// CPU thread
		Thread th = new Thread(new Runnable(){
			public void run() {
				Clockmeter meter = new Clockmeter(1790000, 1.0f);
				while(true){
					int clc = context.cpu.exec();
					meter.add(clc); // it will wait if its too fast
				}
			}
		});
		th.start();
		
		// display thread
		Thread dispth = new Thread(new Runnable(){
			public void run() {
				Clockmeter meter = new Clockmeter(50, 1.0f);
				while(true){
					if(!context.cpu.debugger.suspend){
						context.ppu.render(true);
						meter.add(1);
					}else{
						meter.reset();
					}
				}
			}
		});
		dispth.start();
	}
	
	private static File chooseFile(String path){
		JFileChooser ch = new JFileChooser(path);
		int res = ch.showOpenDialog(null);
		if(res == JFileChooser.APPROVE_OPTION){
			return ch.getSelectedFile();
		}else{
			return null;
		}
	}
	
	public static void main(String[] args) throws IOException {
//		NESFile file = new NESFile(new File("/Users/yukimori/Downloads/nesmas/nesmas.nes"));
//		NESFile file = new NESFile(new File("/Users/yukimori/Downloads/blargg_ppu_tests_2005.09.15b/palette_ram.nes"));
//		NESFile file = new NESFile(new File("/Users/yukimori/Downloads/blargg_ppu_tests_2005.09.15b/power_up_palette.nes"));
//		NESFile file = new NESFile(new File("/Users/yukimori/Downloads/blargg_ppu_tests_2005.09.15b/sprite_ram.nes"));
//		NESFile file = new NESFile(new File("/Users/yukimori/Downloads/blargg_ppu_tests_2005.09.15b/vram_access.nes"));
//		NESFile file = new NESFile(new File("/Users/yukimori/Downloads/blargg_ppu_tests_2005.09.15b/vbl_clear_time.nes"));
		NESFile file = new NESFile(chooseFile("/Users/yukimori/develop/old/nes/"));
//		NESFile file = new NESFile(new File("/Users/yukimori/develop/old/nes/IceClimber.nes"));
		if(file != null){
			file.load();
			boot(file);
		}
	}
}
